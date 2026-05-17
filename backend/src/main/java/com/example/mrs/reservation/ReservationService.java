package com.example.mrs.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.common.BizException;
import com.example.mrs.config.ReservationRuleProps;
import com.example.mrs.domain.ReservationStatus;
import com.example.mrs.entity.MeetingRoomEntity;
import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.MeetingRoomMapper;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.redis.RedisKeys;
import com.example.mrs.redis.RedisLockService;
import com.example.mrs.reservation.dto.ReservationDtos;
import com.example.mrs.room.MeetingRoomService;
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
  private static final EnumSet<ReservationStatus> BLOCKING_STATUSES =
      EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);
  private static final int BUSINESS_START_SLOT = 16; // 08:00
  private static final int BUSINESS_END_SLOT = 36;   // 18:00
  private static final Duration USER_RECENT_CACHE_TTL = Duration.ofMinutes(10);

  private final ReservationMapper reservationMapper;
  private final SysUserMapper sysUserMapper;
  private final MeetingRoomMapper meetingRoomMapper;
  private final MeetingRoomService meetingRoomService;
  private final ReservationBitmapService bitmapService;
  private final RedisLockService lockService;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final ReservationRuleProps reservationRuleProps;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  public ReservationService(ReservationMapper reservationMapper,
                            SysUserMapper sysUserMapper,
                            MeetingRoomMapper meetingRoomMapper,
                            MeetingRoomService meetingRoomService,
                            ReservationBitmapService bitmapService,
                            RedisLockService lockService,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper,
                            ReservationRuleProps reservationRuleProps,
                            NotificationService notificationService,
                            AuditLogService auditLogService) {
    this.reservationMapper = reservationMapper;
    this.sysUserMapper = sysUserMapper;
    this.meetingRoomMapper = meetingRoomMapper;
    this.meetingRoomService = meetingRoomService;
    this.bitmapService = bitmapService;
    this.lockService = lockService;
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.reservationRuleProps = reservationRuleProps;
    this.notificationService = notificationService;
    this.auditLogService = auditLogService;
  }

  @Transactional
  public ReservationDtos.ReservationResp create(ReservationDtos.CreateReq req) {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    MeetingRoomEntity room = meetingRoomService.getById(req.getRoomId());

    validateTime(req.getStartTime(), req.getEndTime());
    LocalDate day = req.getStartTime().toLocalDate();

    String roomStatus = meetingRoomService.getRoomStatus(room.getId());
    if (!"AVAILABLE".equals(roomStatus)) {
      throw BizException.badRequest("会议室当前状态不可预约：" + roomStatus);
    }
    if (inMaintenance(room.getId(), req.getStartTime(), req.getEndTime())) {
      throw BizException.badRequest("该时段为会议室维护期，禁止预约");
    }

    int startSlot = toSlotIndex(req.getStartTime());
    int endSlot = toSlotIndex(req.getEndTime());
    for (int i = startSlot; i < endSlot; i++) {
      if (bitmapService.isSlotOccupied(room.getId(), day, i)) {
        throw BizException.badRequest("该时间段已被占用");
      }
    }

    Long sameDayCount = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getUserId, me.id())
        .ge(ReservationEntity::getStartTime, day.atStartOfDay())
        .lt(ReservationEntity::getStartTime, day.plusDays(1).atStartOfDay())
        .in(ReservationEntity::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.APPROVED.name()));
    if (sameDayCount != null && sameDayCount >= reservationRuleProps.getMaxReservationsPerUserPerDay()) {
      throw BizException.badRequest("单人单日预约次数上限为 " + reservationRuleProps.getMaxReservationsPerUserPerDay());
    }

    String lockKey = RedisKeys.roomDayLock(room.getId(), day);
    String token = lockService.tryLock(lockKey, Duration.ofSeconds(10));
    if (token == null) {
      throw BizException.badRequest("该会议室正在被预约，请稍后重试");
    }
    try {
      ensureNoConflict(room.getId(), req.getStartTime(), req.getEndTime(), null);

      ReservationEntity e = new ReservationEntity();
      e.setUserId(me.id());
      e.setRoomId(room.getId());
      e.setStartTime(req.getStartTime());
      e.setEndTime(req.getEndTime());
      e.setReason(req.getReason());
      boolean requireApproval = requiresApproval(room);
      e.setStatus(requireApproval ? ReservationStatus.PENDING.name() : ReservationStatus.APPROVED.name());
      reservationMapper.insert(e);

      bitmapService.rebuildDay(room.getId(), day);
      evictUserCache(me.id());

      String statusText = requireApproval ? "已提交预约申请，待管理员审批" : "预约成功";
      notificationService.pushToUser(me.id(), statusText + " | 会议室=" + room.getName());
      if (requireApproval) {
        notificationService.pushToUser(me.id(), "审批提醒：请留意审批结果通知");
      } else {
        notificationService.pushToUser(me.id(), "会议提醒：会议开始前 30 分钟请准备参会");
      }
      auditLogService.logForUser(me.id(), "创建预约 rid=" + e.getId() + " room=" + room.getName());

      return toResp(e, room.getName(), me.username());
    } finally {
      lockService.unlock(lockKey, token);
    }
  }

  @Transactional
  public List<ReservationDtos.ReservationResp> createBatchWeekly(ReservationDtos.BatchCreateReq req) {
    if (req.getRepeatWeeks() == null || req.getRepeatWeeks() < 1 || req.getRepeatWeeks() > 12) {
      throw BizException.badRequest("周期次数仅支持 1~12 周");
    }

    // 接口预留能力：任意一周创建失败时由事务整体回滚，避免只生成部分周期预约。
    List<ReservationDtos.ReservationResp> result = new ArrayList<>();
    for (int i = 0; i < req.getRepeatWeeks(); i++) {
      ReservationDtos.CreateReq one = new ReservationDtos.CreateReq();
      one.setRoomId(req.getRoomId());
      one.setStartTime(req.getFirstStartTime().plusWeeks(i));
      one.setEndTime(req.getFirstEndTime().plusWeeks(i));
      one.setReason((req.getReason() == null ? "周期预约" : req.getReason()) + " [第" + (i + 1) + "周]");
      result.add(create(one));
    }
    return result;
  }

  @Transactional
  public void cancel(long reservationId) {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    ReservationEntity e = reservationMapper.selectById(reservationId);
    if (e == null) throw BizException.notFound("预约不存在");
    if (!e.getUserId().equals(me.id())) throw BizException.forbidden("只能取消自己的预约");
    if (ReservationStatus.CANCELLED.name().equals(e.getStatus())) return;

    e.setStatus(ReservationStatus.CANCELLED.name());
    reservationMapper.updateById(e);
    bitmapService.rebuildDay(e.getRoomId(), e.getStartTime().toLocalDate());
    evictUserCache(me.id());
    notificationService.pushToUser(me.id(), "预约已取消 rid=" + e.getId());
    auditLogService.logForUser(me.id(), "取消预约 rid=" + e.getId());
  }

  @Transactional
  public void deleteMine(long reservationId) {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    ReservationEntity e = reservationMapper.selectById(reservationId);
    if (e == null) throw BizException.notFound("预约不存在");
    if (!e.getUserId().equals(me.id())) throw BizException.forbidden("只能删除自己的预约");

    reservationMapper.deleteById(reservationId);
    bitmapService.rebuildDay(e.getRoomId(), e.getStartTime().toLocalDate());
    evictUserCache(me.id());
    auditLogService.logForUser(me.id(), "删除预约 rid=" + reservationId);
  }

  public List<ReservationDtos.ReservationResp> myRecent() {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    String key = Objects.requireNonNull(RedisKeys.userRecentReservations(me.id()));
    String json = stringRedisTemplate.opsForValue().get(key);
    if (json != null && !json.trim().isEmpty()) {
      try {
        return objectMapper.readValue(json, new TypeReference<List<ReservationDtos.ReservationResp>>() {});
      } catch (Exception ignored) {
      }
    }

    LocalDateTime from = LocalDateTime.now().minusDays(30);
    List<ReservationEntity> list = reservationMapper.selectList(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getUserId, me.id())
        .ge(ReservationEntity::getStartTime, from)
        .orderByDesc(ReservationEntity::getStartTime));

    List<ReservationDtos.ReservationResp> resp = list.stream().map(e -> {
      String roomName = meetingRoomService.getById(e.getRoomId()).getName();
      return toResp(e, roomName, me.username());
    }).collect(Collectors.toList());

    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(resp), USER_RECENT_CACHE_TTL);
    } catch (Exception ignored) {
    }
    return resp;
  }

  public List<ReservationDtos.ReservationResp> calendar(long roomId, LocalDateTime start, LocalDateTime end) {
    if (!start.isBefore(end)) {
      throw BizException.badRequest("开始时间必须早于结束时间");
    }
    if (Duration.between(start, end).toDays() > 62) {
      throw BizException.badRequest("日历查询时间跨度不能超过 62 天");
    }

    MeetingRoomEntity room = meetingRoomService.getById(roomId);
    List<ReservationEntity> list = reservationMapper.selectList(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getRoomId, roomId)
        .lt(ReservationEntity::getStartTime, end)
        .gt(ReservationEntity::getEndTime, start)
        .orderByAsc(ReservationEntity::getStartTime));

    return list.stream().map(e -> {
      SysUserEntity u = sysUserMapper.selectById(e.getUserId());
      String username = u == null ? "" : u.getUsername();
      return toResp(e, room.getName(), username);
    }).collect(Collectors.toList());
  }

  public ReservationDtos.RoomDayOccupancyResp roomDayOccupancy(long roomId, LocalDate day) {
    meetingRoomService.getById(roomId);

    bitmapService.rebuildDay(roomId, day);

    String bitmapKey = RedisKeys.roomDayBitmap(roomId, day);
    String slicesKey = RedisKeys.roomDaySlices(roomId, day);

    List<Integer> occupied = new ArrayList<>();
    for (int i = BUSINESS_START_SLOT; i < BUSINESS_END_SLOT; i++) {
      Boolean bit = stringRedisTemplate.opsForValue().getBit(bitmapKey, i);
      if (Boolean.TRUE.equals(bit)) {
        occupied.add(i);
      }
    }

    List<ReservationBitmapService.TimeSlice> slices = new ArrayList<>();
    String slicesJson = stringRedisTemplate.opsForValue().get(slicesKey);
    if (slicesJson != null && !slicesJson.isBlank()) {
      try {
        slices = objectMapper.readValue(slicesJson, new TypeReference<List<ReservationBitmapService.TimeSlice>>() {});
      } catch (Exception ignored) {
      }
    }

    ReservationDtos.RoomDayOccupancyResp resp = new ReservationDtos.RoomDayOccupancyResp();
    resp.setRoomId(roomId);
    resp.setDayStart(day.atStartOfDay());
    resp.setOccupiedSlots(occupied);
    resp.setSlices(slices);
    return resp;
  }

  private void validateTime(LocalDateTime start, LocalDateTime end) {
    if (!start.isBefore(end)) throw BizException.badRequest("结束时间必须晚于开始时间");
    if (start.isBefore(LocalDateTime.now())) throw BizException.badRequest("开始时间不能早于当前时间");

    if (!(isHalfHourAligned(start) && isHalfHourAligned(end))) {
      throw BizException.badRequest("当前版本按“半小时”预约（分钟需为 00 或 30）");
    }

    if (!start.toLocalDate().equals(end.toLocalDate())) {
      throw BizException.badRequest("当前版本仅支持同一天内预约");
    }

    int startSlot = toSlotIndex(start);
    int endSlot = toSlotIndex(end);
    if (startSlot < BUSINESS_START_SLOT || endSlot > BUSINESS_END_SLOT) {
      throw BizException.badRequest("仅支持 08:00 - 18:00 时段预约");
    }

    Duration duration = Duration.between(start, end);
    if (duration.toMinutes() > reservationRuleProps.getMaxDurationMinutes()) {
      throw BizException.badRequest("单次预约时长不超过 " + reservationRuleProps.getMaxDurationMinutes() + " 分钟");
    }

    if (start.isAfter(LocalDateTime.now().plusDays(reservationRuleProps.getMaxAdvanceDays()))) {
      throw BizException.badRequest("仅支持预约未来 " + reservationRuleProps.getMaxAdvanceDays() + " 天内会议");
    }
  }

  private boolean isHalfHourAligned(LocalDateTime t) {
    return (t.getMinute() == 0 || t.getMinute() == 30) && t.getSecond() == 0 && t.getNano() == 0;
  }

  private int toSlotIndex(LocalDateTime t) {
    return t.getHour() * 2 + (t.getMinute() >= 30 ? 1 : 0);
  }

  private void ensureNoConflict(long roomId, LocalDateTime start, LocalDateTime end, Long excludeId) {
    LambdaQueryWrapper<ReservationEntity> q = new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getRoomId, roomId)
        .in(ReservationEntity::getStatus, BLOCKING_STATUSES.stream().map(Enum::name).collect(Collectors.toList()))
        .lt(ReservationEntity::getStartTime, end)
        .gt(ReservationEntity::getEndTime, start);
    if (excludeId != null) q.ne(ReservationEntity::getId, excludeId);
    Long cnt = reservationMapper.selectCount(q);
    if (cnt != null && cnt > 0) {
      throw BizException.badRequest("该时间段已被占用");
    }
  }

  private ReservationDtos.ReservationResp toResp(ReservationEntity e, String roomName, String username) {
    ReservationDtos.ReservationResp r = new ReservationDtos.ReservationResp();
    r.setId(e.getId());
    r.setUserId(e.getUserId());
    r.setUsername(username);
    r.setRoomId(e.getRoomId());
    r.setRoomName(roomName);
    r.setStartTime(e.getStartTime());
    r.setEndTime(e.getEndTime());
    r.setStatus(e.getStatus());
    r.setReason(e.getReason());
    r.setAdminComment(e.getAdminComment());
    r.setApprovedBy(e.getApprovedBy());
    r.setApprovedAt(e.getApprovedAt());
    return r;
  }

  private boolean requiresApproval(MeetingRoomEntity room) {
    if (room == null) return true;
    // 演示规则：A-101 作为重点会议室始终走审批，便于展示审批流程。
    if (room.getName() != null && room.getName().trim().equalsIgnoreCase("A-101")) {
      return true;
    }
    return !Boolean.FALSE.equals(room.getRequireApproval());
  }

  private void evictUserCache(long userId) {
    stringRedisTemplate.delete(Objects.requireNonNull(RedisKeys.userRecentReservations(userId)));
  }

  private boolean inMaintenance(long roomId, LocalDateTime start, LocalDateTime end) {
    List<com.example.mrs.room.dto.RoomDtos.MaintenanceSlot> slots = meetingRoomService.listMaintenances(roomId);
    for (com.example.mrs.room.dto.RoomDtos.MaintenanceSlot slot : slots) {
      if (slot.getStartTime() == null || slot.getEndTime() == null) continue;
      if (start.isBefore(slot.getEndTime()) && end.isAfter(slot.getStartTime())) {
        return true;
      }
    }
    return false;
  }

  public ReservationDtos.ConflictSuggestionResp suggestAlternatives(long roomId, LocalDateTime start, LocalDateTime end) {
    if (!start.isBefore(end)) throw BizException.badRequest("开始时间必须早于结束时间");
    int durationMinutes = (int) Duration.between(start, end).toMinutes();

    ReservationDtos.ConflictSuggestionResp resp = new ReservationDtos.ConflictSuggestionResp();
    resp.setAlternatives(new ArrayList<>());

    MeetingRoomEntity room = meetingRoomService.getById(roomId);
    String roomName = room.getName();
    boolean conflict = false;
    try {
      ensureNoConflict(roomId, start, end, null);
    } catch (BizException e) {
      conflict = true;
    }

    if (!conflict) {
      resp.setConflictMessage("当前时段可预约");
      return resp;
    }

    resp.setConflictMessage("当前时段与已有预约冲突，已为你推荐替代方案");

    // 推荐同会议室相邻时间段（±2小时内，半小时步进）
    int stepMinutes = 30;
    for (int i = 1; i <= 4; i++) {
      LocalDateTime newStart = start.plusMinutes(stepMinutes * i);
      LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);
      if (isSuggestionValid(roomId, newStart, newEnd)) {
        resp.getAlternatives().add(buildAlt(roomId, roomName, newStart, newEnd, "同会议室顺延"));
      }
      LocalDateTime prevStart = start.minusMinutes(stepMinutes * i);
      LocalDateTime prevEnd = prevStart.plusMinutes(durationMinutes);
      if (isSuggestionValid(roomId, prevStart, prevEnd)) {
        resp.getAlternatives().add(buildAlt(roomId, roomName, prevStart, prevEnd, "同会议室提前"));
      }
      if (resp.getAlternatives().size() >= 6) break;
    }

    // 推荐其他会议室同时间段
    List<MeetingRoomEntity> rooms = meetingRoomMapper.selectList(new LambdaQueryWrapper<MeetingRoomEntity>()
        .orderByAsc(MeetingRoomEntity::getId));
    for (MeetingRoomEntity r : rooms) {
      if (r.getId().equals(roomId)) continue;
      if (!"AVAILABLE".equals(meetingRoomService.getRoomStatus(r.getId()))) continue;
      if (inMaintenance(r.getId(), start, end)) continue;
      if (isSuggestionValid(r.getId(), start, end)) {
        resp.getAlternatives().add(buildAlt(r.getId(), r.getName(), start, end, "同时间段其他会议室"));
      }
      if (resp.getAlternatives().size() >= 10) break;
    }

    return resp;
  }

  private boolean isSuggestionValid(long roomId, LocalDateTime start, LocalDateTime end) {
    if (start.toLocalDate().isBefore(LocalDate.now())) return false;
    if (start.toLocalDate().isAfter(LocalDate.now().plusDays(reservationRuleProps.getMaxAdvanceDays()))) return false;
    if (start.getMinute() != 0 && start.getMinute() != 30) return false;
    if (end.getMinute() != 0 && end.getMinute() != 30) return false;
    int startSlot = toSlotIndex(start);
    int endSlot = toSlotIndex(end);
    if (startSlot < BUSINESS_START_SLOT || endSlot > BUSINESS_END_SLOT) return false;
    if (inMaintenance(roomId, start, end)) return false;

    try {
      ensureNoConflict(roomId, start, end, null);
      return true;
    } catch (BizException e) {
      return false;
    }
  }

  private ReservationDtos.AlternativeSlot buildAlt(long roomId, String roomName, LocalDateTime start, LocalDateTime end, String tip) {
    ReservationDtos.AlternativeSlot alt = new ReservationDtos.AlternativeSlot();
    alt.setRoomId(roomId);
    alt.setRoomName(roomName);
    alt.setStartTime(start);
    alt.setEndTime(end);
    alt.setTip(tip);
    return alt;
  }
}
