package com.example.mrs.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.domain.ReservationStatus;
import com.example.mrs.entity.MeetingRoomEntity;
import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.MeetingRoomMapper;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.reservation.NotificationService;
import com.example.mrs.room.MeetingRoomService;
import com.example.mrs.room.dto.RoomDtos;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoDashboardDataRunner implements ApplicationRunner {
  private static final String DEMO_REASON_PREFIX = "[DEMO_DASHBOARD]";
  private static final int TARGET_ROOM_COUNT = 6;
  private static final int TARGET_RESERVATION_COUNT = 48;

  private static final List<String> DEMO_USERNAMES = List.of(
      "lin.ming",
      "zhou.qi",
      "chen.yu",
      "gu.xing",
      "song.nan",
      "xu.yan",
      "he.ning",
      "lu.an"
  );

  private static final List<RoomSeed> ROOM_SEEDS = List.of(
      new RoomSeed("北辰厅", 16, List.of("投影", "白板", "视频会议"), true),
      new RoomSeed("云岫厅", 12, List.of("双屏", "白板", "电话会议"), true),
      new RoomSeed("观海厅", 10, List.of("投影", "幕布", "扩音"), false),
      new RoomSeed("星图厅", 20, List.of("视频会议", "电子白板", "录播"), true),
      new RoomSeed("澄明厅", 8, List.of("白板", "电话会议"), false),
      new RoomSeed("森屿厅", 6, List.of("移动屏", "白板"), false)
  );

  private final SysUserMapper userMapper;
  private final MeetingRoomMapper roomMapper;
  private final ReservationMapper reservationMapper;
  private final PasswordEncoder passwordEncoder;
  private final MeetingRoomService meetingRoomService;
  private final NotificationService notificationService;

  @Value("${app.bootstrap.seed-demo-data-on-start:true}")
  private boolean seedDemoDataOnStart;

  @Value("${app.bootstrap.demo-user-password:demo123}")
  private String demoUserPassword;

  public DemoDashboardDataRunner(SysUserMapper userMapper,
                                 MeetingRoomMapper roomMapper,
                                 ReservationMapper reservationMapper,
                                 PasswordEncoder passwordEncoder,
                                 MeetingRoomService meetingRoomService,
                                 NotificationService notificationService) {
    this.userMapper = userMapper;
    this.roomMapper = roomMapper;
    this.reservationMapper = reservationMapper;
    this.passwordEncoder = passwordEncoder;
    this.meetingRoomService = meetingRoomService;
    this.notificationService = notificationService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!seedDemoDataOnStart) {
      return;
    }

    try {
      long roomCount = safeLong(roomMapper.selectCount(new LambdaQueryWrapper<>()));
      long reservationCount = safeLong(reservationMapper.selectCount(new LambdaQueryWrapper<>()));
      if (roomCount >= TARGET_ROOM_COUNT && reservationCount >= TARGET_RESERVATION_COUNT) {
        log.info("Demo dashboard seed skipped: current data is already dense enough");
        return;
      }

      List<SysUserEntity> demoUsers = ensureDemoUsers();
      List<MeetingRoomEntity> demoRooms = ensureDemoRooms();
      if (demoUsers.isEmpty() || demoRooms.isEmpty()) {
        return;
      }

      syncRoomStatusAndMaintenance(demoRooms);
      boolean insertedReservations = ensureDemoReservations(demoUsers, demoRooms);
      if (insertedReservations) {
        seedNotifications(demoUsers);
      }
    } catch (Exception ex) {
      log.warn("Demo dashboard seed skipped: {}", ex.getMessage());
    }
  }

  private List<SysUserEntity> ensureDemoUsers() {
    List<SysUserEntity> users = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (String username : DEMO_USERNAMES) {
      SysUserEntity exists = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
          .eq(SysUserEntity::getUsername, username)
          .last("limit 1"));
      if (exists == null) {
        SysUserEntity user = new SysUserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(demoUserPassword));
        user.setRole("USER");
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        exists = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
            .eq(SysUserEntity::getUsername, username)
            .last("limit 1"));
      }
      if (exists != null) {
        users.add(exists);
      }
    }
    return users;
  }

  private List<MeetingRoomEntity> ensureDemoRooms() {
    List<MeetingRoomEntity> rooms = new ArrayList<>();
    for (RoomSeed seed : ROOM_SEEDS) {
      MeetingRoomEntity exists = roomMapper.selectOne(new LambdaQueryWrapper<MeetingRoomEntity>()
          .eq(MeetingRoomEntity::getName, seed.name)
          .last("limit 1"));
      if (exists == null) {
        RoomDtos.RoomCreateReq req = new RoomDtos.RoomCreateReq();
        req.setName(seed.name);
        req.setCapacity(seed.capacity);
        req.setEquipment(seed.equipment);
        req.setRequireApproval(seed.requireApproval);
        meetingRoomService.create(req);
        exists = roomMapper.selectOne(new LambdaQueryWrapper<MeetingRoomEntity>()
            .eq(MeetingRoomEntity::getName, seed.name)
            .last("limit 1"));
      }
      if (exists != null) {
        rooms.add(exists);
      }
    }
    return rooms;
  }

  private void syncRoomStatusAndMaintenance(List<MeetingRoomEntity> rooms) {
    Map<String, MeetingRoomEntity> roomMap = rooms.stream()
        .collect(Collectors.toMap(MeetingRoomEntity::getName, room -> room, (left, right) -> left));

    setRoomStatus(roomMap.get("星图厅"), "MAINTENANCE");
    setRoomStatus(roomMap.get("森屿厅"), "DISABLED");
    setRoomStatus(roomMap.get("北辰厅"), "AVAILABLE");
    setRoomStatus(roomMap.get("云岫厅"), "AVAILABLE");
    setRoomStatus(roomMap.get("观海厅"), "AVAILABLE");
    setRoomStatus(roomMap.get("澄明厅"), "AVAILABLE");

    LocalDate today = LocalDate.now();
    ensureMaintenance(roomMap.get("星图厅"), today.atTime(13, 0), today.atTime(17, 30), "视频终端巡检");
    ensureMaintenance(roomMap.get("观海厅"), today.plusDays(1).atTime(9, 0), today.plusDays(1).atTime(12, 0), "投影幕布校准");
  }

  private boolean ensureDemoReservations(List<SysUserEntity> users, List<MeetingRoomEntity> rooms) {
    long demoReservationCount = safeLong(reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .like(ReservationEntity::getReason, DEMO_REASON_PREFIX)));
    if (demoReservationCount >= 70) {
      return false;
    }

    List<MeetingRoomEntity> schedulableRooms = rooms.stream()
        .filter(room -> !"森屿厅".equals(room.getName()))
        .collect(Collectors.toList());
    if (schedulableRooms.isEmpty()) {
      return false;
    }

    LocalDate today = LocalDate.now();
    Long reviewerId = resolveReviewerId();
    int[][] slots = {
        {9, 0, 60},
        {10, 30, 90},
        {13, 30, 60},
        {15, 0, 90},
        {16, 30, 60}
    };

    int inserted = 0;
    for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
      int laneCount = dayOffset == 0 ? 3 : 2;
      for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
        for (int lane = 0; lane < laneCount; lane++) {
          MeetingRoomEntity room = schedulableRooms.get((dayOffset + slotIndex + lane) % schedulableRooms.size());
          SysUserEntity user = users.get((dayOffset * 3 + slotIndex + lane) % users.size());
          LocalDateTime start = today.plusDays(dayOffset).atTime(slots[slotIndex][0], slots[slotIndex][1]);
          LocalDateTime end = start.plusMinutes(slots[slotIndex][2]);
          String reason = DEMO_REASON_PREFIX + " 调度样本-" + dayOffset + "-" + slotIndex + "-" + lane;
          if (reservationExists(reason)) {
            continue;
          }

          String status = pickStatus(dayOffset, slotIndex, lane);
          ReservationEntity reservation = new ReservationEntity();
          reservation.setUserId(user.getId());
          reservation.setRoomId(room.getId());
          reservation.setStartTime(start);
          reservation.setEndTime(end);
          reservation.setStatus(status);
          reservation.setReason(reason);
          reservation.setCreatedAt(start.minusDays(1));
          reservation.setUpdatedAt(start.minusHours(2));

          if (ReservationStatus.APPROVED.name().equals(status) || ReservationStatus.REJECTED.name().equals(status)) {
            reservation.setApprovedBy(reviewerId);
            reservation.setApprovedAt(start.minusHours(6));
            reservation.setAdminComment(ReservationStatus.APPROVED.name().equals(status) ? "示例数据自动批准" : "示例数据自动驳回");
          }

          reservationMapper.insert(reservation);
          inserted++;
        }
      }
    }

    log.info("Demo dashboard seed inserted {} reservations", inserted);
    return inserted > 0;
  }

  private void seedNotifications(List<SysUserEntity> demoUsers) {
    List<Long> targetUserIds = new ArrayList<>();
    targetUserIds.addAll(loadReviewerIds());
    targetUserIds.addAll(demoUsers.stream().limit(4).map(SysUserEntity::getId).filter(Objects::nonNull).collect(Collectors.toList()));

    for (Long userId : targetUserIds) {
      if (userId == null) {
        continue;
      }
      notificationService.pushToUser(userId, "预约节奏提醒：今日高峰集中在上午与午后时段，建议优先查看热度与维护影响");
      notificationService.pushToUser(userId, "会议室维护提醒：星图厅今日 13:00-17:30 视频终端巡检");
    }
  }

  private boolean reservationExists(String reason) {
    return safeLong(reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getReason, reason))) > 0;
  }

  private void setRoomStatus(MeetingRoomEntity room, String status) {
    if (room == null) {
      return;
    }
    meetingRoomService.updateRoomStatus(room.getId(), status);
  }

  private void ensureMaintenance(MeetingRoomEntity room, LocalDateTime start, LocalDateTime end, String reason) {
    if (room == null) {
      return;
    }
    boolean exists = meetingRoomService.listMaintenances(room.getId()).stream().anyMatch(slot ->
        Objects.equals(slot.getStartTime(), start)
            && Objects.equals(slot.getEndTime(), end)
            && Objects.equals(slot.getReason(), reason));
    if (!exists) {
      meetingRoomService.addMaintenance(room.getId(), start, end, reason);
    }
  }

  private Long resolveReviewerId() {
    SysUserEntity reviewer = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .in(SysUserEntity::getRole, "SUPER_ADMIN", "ADMIN")
        .orderByAsc(SysUserEntity::getId)
        .last("limit 1"));
    return reviewer == null ? null : reviewer.getId();
  }

  private List<Long> loadReviewerIds() {
    return userMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
            .in(SysUserEntity::getRole, "SUPER_ADMIN", "ADMIN")
            .orderByAsc(SysUserEntity::getId))
        .stream()
        .map(SysUserEntity::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String pickStatus(int dayOffset, int slotIndex, int lane) {
    int marker = dayOffset + slotIndex + lane;
    if (dayOffset == 0 && slotIndex < 2 && lane == 0) {
      return ReservationStatus.PENDING.name();
    }
    if (marker % 9 == 0) {
      return ReservationStatus.PENDING.name();
    }
    if (marker % 17 == 0) {
      return ReservationStatus.REJECTED.name();
    }
    if (marker % 19 == 0) {
      return ReservationStatus.CANCELLED.name();
    }
    return ReservationStatus.APPROVED.name();
  }

  private long safeLong(Long value) {
    return value == null ? 0L : value;
  }

  private static final class RoomSeed {
    private final String name;
    private final int capacity;
    private final List<String> equipment;
    private final boolean requireApproval;

    private RoomSeed(String name, int capacity, List<String> equipment, boolean requireApproval) {
      this.name = name;
      this.capacity = capacity;
      this.equipment = equipment;
      this.requireApproval = requireApproval;
    }
  }
}
