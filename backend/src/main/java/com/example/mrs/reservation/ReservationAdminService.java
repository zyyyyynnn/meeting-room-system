package com.example.mrs.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.common.BizException;
import com.example.mrs.domain.ReservationStatus;
import com.example.mrs.domain.Role;
import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.redis.RedisKeys;
import com.example.mrs.reservation.dto.ReservationDtos;
import com.example.mrs.room.MeetingRoomService;
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationAdminService {
  private final ReservationMapper reservationMapper;
  private final SysUserMapper sysUserMapper;
  private final MeetingRoomService meetingRoomService;
  private final ReservationBitmapService bitmapService;
  private final StringRedisTemplate stringRedisTemplate;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  public ReservationAdminService(ReservationMapper reservationMapper,
                                 SysUserMapper sysUserMapper,
                                 MeetingRoomService meetingRoomService,
                                 ReservationBitmapService bitmapService,
                                 StringRedisTemplate stringRedisTemplate,
                                 NotificationService notificationService,
                                 AuditLogService auditLogService) {
    this.reservationMapper = reservationMapper;
    this.sysUserMapper = sysUserMapper;
    this.meetingRoomService = meetingRoomService;
    this.bitmapService = bitmapService;
    this.stringRedisTemplate = stringRedisTemplate;
    this.notificationService = notificationService;
    this.auditLogService = auditLogService;
  }

  public List<ReservationDtos.ReservationResp> pendingList() {
    List<ReservationEntity> list = reservationMapper.selectList(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getStatus, ReservationStatus.PENDING.name())
        .orderByAsc(ReservationEntity::getStartTime));

    return list.stream().map(this::toResp).collect(Collectors.toList());
  }

  public List<ReservationDtos.ReservationResp> recentReviewedList() {
    List<ReservationEntity> list = reservationMapper.selectList(new LambdaQueryWrapper<ReservationEntity>()
        .in(ReservationEntity::getStatus, ReservationStatus.APPROVED.name(), ReservationStatus.REJECTED.name())
        .orderByDesc(ReservationEntity::getApprovedAt)
        .last("limit 100"));

    return list.stream().map(this::toResp).collect(Collectors.toList());
  }

  @Transactional
  public void approve(long id, ReservationDtos.ApproveReq req) {
    JwtPrincipal admin = SecurityUtil.requirePrincipal();
    ReservationEntity e = reservationMapper.selectById(id);
    if (e == null) throw BizException.notFound("预约不存在");
    if (!ReservationStatus.PENDING.name().equals(e.getStatus())) {
      throw BizException.badRequest("仅待审批预约可批准");
    }

    Long cnt = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getRoomId, e.getRoomId())
        .in(ReservationEntity::getStatus,
            ReservationStatus.PENDING.name(),
            ReservationStatus.APPROVED.name())
        .lt(ReservationEntity::getStartTime, e.getEndTime())
        .gt(ReservationEntity::getEndTime, e.getStartTime())
        .ne(ReservationEntity::getId, e.getId()));
    if (cnt != null && cnt > 0) {
      throw BizException.badRequest("该时间段已被占用，无法审批通过");
    }

    e.setStatus(ReservationStatus.APPROVED.name());
    e.setAdminComment(req.getAdminComment());
    e.setApprovedBy(admin.id());
    e.setApprovedAt(LocalDateTime.now());
    reservationMapper.updateById(e);

    bitmapService.rebuildDay(e.getRoomId(), e.getStartTime().toLocalDate());
    evictUserCache(e.getUserId());
    notificationService.pushToUser(e.getUserId(), "预约已审批通过 rid=" + e.getId());
    notificationService.pushToUser(e.getUserId(), "会议提醒：会议开始前 30 分钟请准备参会");
    auditLogService.logForUser(e.getUserId(), "管理员通过预约 rid=" + e.getId() + " by=" + admin.username());
  }

  @Transactional
  public void reject(long id, ReservationDtos.RejectReq req) {
    JwtPrincipal admin = SecurityUtil.requirePrincipal();
    ReservationEntity e = reservationMapper.selectById(id);
    if (e == null) throw BizException.notFound("预约不存在");
    if (!ReservationStatus.PENDING.name().equals(e.getStatus())) {
      throw BizException.badRequest("仅待审批预约可驳回");
    }
    e.setStatus(ReservationStatus.REJECTED.name());
    e.setAdminComment(req.getAdminComment());
    e.setApprovedBy(admin.id());
    e.setApprovedAt(LocalDateTime.now());
    reservationMapper.updateById(e);

    bitmapService.rebuildDay(e.getRoomId(), e.getStartTime().toLocalDate());
    evictUserCache(e.getUserId());
    notificationService.pushToUser(e.getUserId(), "预约被驳回 rid=" + e.getId() + "，原因：" + (req.getAdminComment() == null ? "无" : req.getAdminComment()));
    auditLogService.logForUser(e.getUserId(), "管理员驳回预约 rid=" + e.getId() + " by=" + admin.username());
  }

  @Transactional
  public void revokeReviewed(long id) {
    JwtPrincipal operator = SecurityUtil.requirePrincipal();
    if (!Role.SUPER_ADMIN.name().equals(operator.role())) {
      throw BizException.forbidden("仅超级管理员可撤销审批结果");
    }

    ReservationEntity e = reservationMapper.selectById(id);
    if (e == null) throw BizException.notFound("预约不存在");
    if (!(ReservationStatus.APPROVED.name().equals(e.getStatus()) || ReservationStatus.REJECTED.name().equals(e.getStatus()))) {
      throw BizException.badRequest("仅已审批（通过/驳回）预约可撤销");
    }

    e.setStatus(ReservationStatus.PENDING.name());
    e.setAdminComment("[已撤销审批] " + (e.getAdminComment() == null ? "" : e.getAdminComment()));
    e.setApprovedBy(null);
    e.setApprovedAt(null);
    reservationMapper.updateById(e);

    bitmapService.rebuildDay(e.getRoomId(), e.getStartTime().toLocalDate());
    evictUserCache(e.getUserId());
    notificationService.pushToUser(e.getUserId(), "你的预约审批结果已被超级管理员撤销，当前状态：待审批 rid=" + e.getId());
    auditLogService.logForUser(e.getUserId(), "超级管理员撤销审批 rid=" + e.getId() + " by=" + operator.username());
  }

  private ReservationDtos.ReservationResp toResp(ReservationEntity e) {
    SysUserEntity u = sysUserMapper.selectById(e.getUserId());
    String username = u == null ? "" : u.getUsername();
    String roomName = meetingRoomService.getById(e.getRoomId()).getName();

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

  private void evictUserCache(long userId) {
    String key = RedisKeys.userRecentReservations(userId);
    if (key != null && !key.isBlank()) {
      stringRedisTemplate.delete(key);
    }
  }
}
