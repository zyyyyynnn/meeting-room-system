package com.example.mrs.stats;

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
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
  private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM/dd");
  private static final int BUSINESS_START_HOUR = 8;
  private static final int BUSINESS_END_HOUR = 18;
  private static final List<String> BLOCKED_NOTIFICATION_MARKERS = List.of(
      "示例数据已生成",
      "本周已补充预约",
      "界面联调",
      "运营看板、审批与趋势图层"
  );
  private static final List<String> MEETING_NOTIFICATION_MARKERS = List.of(
      "预约",
      "会议",
      "审批",
      "维护",
      "会议室",
      "房间",
      "参会"
  );

  private final MeetingRoomMapper meetingRoomMapper;
  private final SysUserMapper sysUserMapper;
  private final ReservationMapper reservationMapper;
  private final MeetingRoomService meetingRoomService;
  private final NotificationService notificationService;

  public StatsService(MeetingRoomMapper meetingRoomMapper,
                      SysUserMapper sysUserMapper,
                      ReservationMapper reservationMapper,
                      MeetingRoomService meetingRoomService,
                      NotificationService notificationService) {
    this.meetingRoomMapper = meetingRoomMapper;
    this.sysUserMapper = sysUserMapper;
    this.reservationMapper = reservationMapper;
    this.meetingRoomService = meetingRoomService;
    this.notificationService = notificationService;
  }

  public StatsDtos.OverviewResp overview() {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    LocalDate today = LocalDate.now();

    Long roomCount = meetingRoomMapper.selectCount(new LambdaQueryWrapper<MeetingRoomEntity>());
    Long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>());
    Long normalUserCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getRole, "USER"));
    Long adminUserCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getRole, "ADMIN"));
    Long superAdminUserCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getRole, "SUPER_ADMIN"));
    Long disabledUserCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getEnabled, false));

    Long todayReservationCount = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .ge(ReservationEntity::getStartTime, today.atStartOfDay())
        .lt(ReservationEntity::getStartTime, today.plusDays(1).atStartOfDay())
        .in(ReservationEntity::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.APPROVED.name()));
    Long myUpcomingCount = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getUserId, me.id())
        .ge(ReservationEntity::getStartTime, today.atStartOfDay())
        .in(ReservationEntity::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.APPROVED.name()));

    Long pendingApprovals = 0L;
    if (isAdminRole(me.role())) {
      pendingApprovals = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
          .eq(ReservationEntity::getStatus, ReservationStatus.PENDING.name()));
    }

    StatsDtos.UserBreakdown breakdown = new StatsDtos.UserBreakdown();
    breakdown.setNormalUsers(safeLong(normalUserCount));
    breakdown.setAdminUsers(safeLong(adminUserCount));
    breakdown.setSuperAdminUsers(safeLong(superAdminUserCount));
    breakdown.setDisabledUsers(safeLong(disabledUserCount));

    StatsDtos.OverviewResp resp = new StatsDtos.OverviewResp();
    resp.setTotalRooms(safeLong(roomCount));
    resp.setTotalUsers(safeLong(userCount));
    resp.setTodayReservations(safeLong(todayReservationCount));
    resp.setMyUpcomingReservations(safeLong(myUpcomingCount));
    resp.setPendingApprovals(safeLong(pendingApprovals));
    resp.setUserBreakdown(breakdown);
    return resp;
  }

  public StatsDtos.DashboardResp dashboard() {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    boolean adminView = isAdminRole(me.role());
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    List<RoomDtos.RoomResp> rooms = safeList(meetingRoomService.listAll());
    List<ReservationEntity> weekReservations = safeList(reservationMapper.selectList(new LambdaQueryWrapper<ReservationEntity>()
        .ge(ReservationEntity::getStartTime, today.atStartOfDay())
        .lt(ReservationEntity::getStartTime, today.plusDays(7).atStartOfDay())
        .in(ReservationEntity::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.APPROVED.name())
        .orderByAsc(ReservationEntity::getStartTime)));

    long pendingApprovals = adminView
        ? safeLong(reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
            .eq(ReservationEntity::getStatus, ReservationStatus.PENDING.name())))
        : 0L;
    int notificationCount = filterMeetingNotifications(notificationService.myNotifications()).size();

    Map<LocalDate, List<ReservationEntity>> reservationsByDay = weekReservations.stream()
        .filter(reservation -> reservation.getStartTime() != null)
        .collect(Collectors.groupingBy(reservation -> reservation.getStartTime().toLocalDate()));

    List<ReservationEntity> todayReservations = reservationsByDay.getOrDefault(today, Collections.emptyList());
    List<StatsDtos.DashboardHeatmapBucket> todayHeatmap = buildHeatmap(today, todayReservations, rooms.size());
    List<StatsDtos.DashboardTrendPoint> weeklyTrend = buildWeeklyTrend(today, reservationsByDay, rooms.size());

    long availableRoomsNow = rooms.stream().filter(room -> isRoomAvailableNow(room, now)).count();
    long maintenanceRoomsToday = rooms.stream().filter(room -> isMaintenanceToday(room, today, now)).count();
    int highLoadBuckets = (int) todayHeatmap.stream().filter(bucket -> "high".equals(bucket.getLoad())).count();
    int lowLoadBuckets = (int) todayHeatmap.stream().filter(bucket -> "low".equals(bucket.getLoad())).count();
    int peakOccupancy = todayHeatmap.stream().mapToInt(StatsDtos.DashboardHeatmapBucket::getOccupancyPercent).max().orElse(0);
    String peakWindow = todayHeatmap.stream()
        .sorted((left, right) -> Integer.compare(right.getOccupancyPercent(), left.getOccupancyPercent()))
        .map(StatsDtos.DashboardHeatmapBucket::getLabel)
        .findFirst()
        .orElse("今日较平稳");
    Map<Long, Long> roomLoadToday = todayReservations.stream()
        .collect(Collectors.groupingBy(ReservationEntity::getRoomId, Collectors.counting()));
    long highPressureRooms = roomLoadToday.values().stream().filter(count -> count >= 2).count();
    long easyRooms = rooms.stream()
        .filter(room -> "AVAILABLE".equals(normalizeStatus(room.getStatus())))
        .filter(room -> roomLoadToday.getOrDefault(room.getId(), 0L) == 0L)
        .filter(room -> !hasMaintenanceAt(room, today.atTime(12, 0)))
        .count();

    StatsDtos.DashboardResp resp = new StatsDtos.DashboardResp();
    resp.setAdminView(adminView);
    resp.setWelcome(buildWelcome(me));
    resp.setTaskSummary(adminView
        ? buildAdminTaskSummary(now, today, weekReservations, pendingApprovals, maintenanceRoomsToday, highLoadBuckets, notificationCount)
        : buildUserTaskSummary(me, now, weekReservations, notificationCount, maintenanceRoomsToday));
    resp.setResourceSnapshot(adminView
        ? buildAdminResourceSnapshot(rooms.size(), availableRoomsNow, todayReservations.size(), maintenanceRoomsToday, peakWindow, peakOccupancy)
        : buildUserResourceSnapshot(availableRoomsNow, todayReservations.size(), peakWindow, lowLoadBuckets, maintenanceRoomsToday));
    resp.setTodayHeatmap(todayHeatmap);
    resp.setWeeklyTrend(weeklyTrend);
    resp.setRiskDistribution(adminView
        ? buildAdminRiskDistribution(pendingApprovals, maintenanceRoomsToday, highLoadBuckets, highPressureRooms)
        : buildUserRiskDistribution(lowLoadBuckets, highLoadBuckets, maintenanceRoomsToday, easyRooms));
    resp.setQuickLinkContext(adminView ? buildAdminQuickLinks() : buildUserQuickLinks());
    return resp;
  }

  private StatsDtos.Welcome buildWelcome(JwtPrincipal me) {
    String roleLabel = roleLabel(me.role());
    StatsDtos.Welcome welcome = new StatsDtos.Welcome();
    welcome.setRoleLabel(roleLabel);
    welcome.setMessage("Welcome，" + me.username() + "！");
    return welcome;
  }

  private StatsDtos.DashboardTaskSummary buildAdminTaskSummary(LocalDateTime now,
                                                               LocalDate today,
                                                               List<ReservationEntity> weekReservations,
                                                               long pendingApprovals,
                                                               long maintenanceRoomsToday,
                                                               int highLoadBuckets,
                                                               int notificationCount) {
    long upcomingSoon = weekReservations.stream()
        .filter(reservation -> reservation.getStartTime() != null)
        .filter(reservation -> !reservation.getStartTime().isBefore(now))
        .filter(reservation -> reservation.getStartTime().isBefore(now.plusHours(2)))
        .count();

    StatsDtos.DashboardTaskSummary summary = new StatsDtos.DashboardTaskSummary();
    summary.setTitle("今日待办与异常");
    summary.setSubtitle("先处理待审批、维护异常和近时段会议，再进入下方图层判断整体资源压力。");
    summary.setItems(List.of(
        taskItem("pending-approvals", "待审批预约", pendingApprovals, "需要优先人工处理", "warning", "/admin/approvals", Map.of("focus", "pending")),
        taskItem("upcoming-soon", "即将开始会议", upcomingSoon, "未来 2 小时内即将开始", "accent", "/calendar", Map.of("focus", "today")),
        taskItem("maintenance-alerts", "维护异常", maintenanceRoomsToday, "今日受维护影响的会议室", "danger", "/rooms", Map.of("status", "MAINTENANCE")),
        taskItem("risk-slots", "高风险时段", highLoadBuckets, "今日需要重点留意的高峰时段", "info", "/rooms", Map.of("status", "AVAILABLE")),
        taskItem("notifications", "关键通知", notificationCount, "个人通知中心待查看", "neutral", "/dashboard", Map.of("panel", "notifications"))
    ));
    return summary;
  }

  private StatsDtos.DashboardTaskSummary buildUserTaskSummary(JwtPrincipal me,
                                                              LocalDateTime now,
                                                              List<ReservationEntity> weekReservations,
                                                              int notificationCount,
                                                              long maintenanceRoomsToday) {
    long myUpcoming = weekReservations.stream()
        .filter(reservation -> Objects.equals(reservation.getUserId(), me.id()))
        .filter(reservation -> reservation.getStartTime() != null && !reservation.getStartTime().isBefore(now))
        .count();
    long myPending = weekReservations.stream()
        .filter(reservation -> Objects.equals(reservation.getUserId(), me.id()))
        .filter(reservation -> ReservationStatus.PENDING.name().equals(reservation.getStatus()))
        .count();
    long myApproved = weekReservations.stream()
        .filter(reservation -> Objects.equals(reservation.getUserId(), me.id()))
        .filter(reservation -> ReservationStatus.APPROVED.name().equals(reservation.getStatus()))
        .count();

    StatsDtos.DashboardTaskSummary summary = new StatsDtos.DashboardTaskSummary();
    summary.setTitle("我的今日主控");
    summary.setSubtitle("先看自己的预约推进和提醒，再参考右侧资源态势决定何时、去哪里预约更顺。");
    summary.setItems(List.of(
        taskItem("my-upcoming", "我的待进行会议", myUpcoming, "近 7 天个人待进行会议", "accent", "/mine", Map.of("status", "APPROVED")),
        taskItem("my-pending", "我的待处理预约", myPending, "仍在等待审批的申请", "warning", "/mine", Map.of("status", "PENDING")),
        taskItem("my-approved", "我的已确认预约", myApproved, "已确认可执行的预约", "info", "/mine", Map.of("status", "APPROVED")),
        taskItem("notifications", "通知提醒", notificationCount, "审批、会议与系统消息提醒", "neutral", "/dashboard", Map.of("panel", "notifications")),
        taskItem("maintenance-alerts", "维护提示", maintenanceRoomsToday, "今日受维护影响的资源提醒", "danger", "/rooms", Map.of("status", "MAINTENANCE"))
    ));
    return summary;
  }

  private StatsDtos.DashboardResourceSnapshot buildAdminResourceSnapshot(int totalRooms,
                                                                         long availableRoomsNow,
                                                                         int todayReservations,
                                                                         long maintenanceRoomsToday,
                                                                         String peakWindow,
                                                                         int peakOccupancy) {
    StatsDtos.DashboardResourceSnapshot snapshot = new StatsDtos.DashboardResourceSnapshot();
    snapshot.setTitle("资源态势驾驶舱");
    snapshot.setSubtitle("围绕可用性、负载与维护影响快速判断今天的资源效率和风险。");
    snapshot.setMetrics(List.of(
        metric("available-now", "当前可用会议室", String.valueOf(availableRoomsNow), "可立即发起预约的资源数", "accent"),
        metric("today-total", "今日预约总量", String.valueOf(todayReservations), "今日 blocking 预约数", "info"),
        metric("maintenance-ratio", "维护占用", percent(maintenanceRoomsToday, totalRooms), "受维护影响的房间占比", "danger"),
        metric("peak-window", "高峰时段", peakWindow, "当前最紧张的预约窗口", "warning"),
        metric("pressure", "负载压力", peakOccupancy + "%", "按活跃房间占比估算", "neutral")
    ));
    return snapshot;
  }

  private StatsDtos.DashboardResourceSnapshot buildUserResourceSnapshot(long availableRoomsNow,
                                                                        int todayReservations,
                                                                        String peakWindow,
                                                                        int lowLoadBuckets,
                                                                        long maintenanceRoomsToday) {
    StatsDtos.DashboardResourceSnapshot snapshot = new StatsDtos.DashboardResourceSnapshot();
    snapshot.setTitle("轻量资源态势");
    snapshot.setSubtitle("保留少量全局信息，帮助判断何时预约更顺，不把首页变成管理页。");
    snapshot.setMetrics(List.of(
        metric("available-now", "当前可预约会议室", String.valueOf(availableRoomsNow), "此刻可尝试预约的资源数", "accent"),
        metric("today-total", "今日预约总量", String.valueOf(todayReservations), "帮助判断今天是否拥挤", "info"),
        metric("peak-window", "高峰提醒", peakWindow, "避开这个时间段更容易预约", "warning"),
        metric("quiet-slots", "较空闲时段", String.valueOf(lowLoadBuckets), "今日低压时段数量", "neutral"),
        metric("maintenance", "维护提醒", String.valueOf(maintenanceRoomsToday), "受维护影响的房间数量", "danger")
    ));
    return snapshot;
  }

  private List<StatsDtos.DashboardRiskDistributionItem> buildAdminRiskDistribution(long pendingApprovals,
                                                                                   long maintenanceRoomsToday,
                                                                                   int highLoadBuckets,
                                                                                   long highPressureRooms) {
    return List.of(
        riskItem("pending-pressure", "待审批压力", pendingApprovals, "待审批积压需要优先清理", "warning"),
        riskItem("maintenance-impact", "维护占用", maintenanceRoomsToday, "今日维护对资源池的直接影响", "danger"),
        riskItem("peak-slots", "高峰时段", highLoadBuckets, "今日活跃度最高的时段数", "info"),
        riskItem("pressure-rooms", "高压房间", highPressureRooms, "今日负载较高的会议室数量", "accent")
    );
  }

  private List<StatsDtos.DashboardRiskDistributionItem> buildUserRiskDistribution(int lowLoadBuckets,
                                                                                  int highLoadBuckets,
                                                                                  long maintenanceRoomsToday,
                                                                                  long easyRooms) {
    return List.of(
        riskItem("quiet-slots", "较空闲时段", lowLoadBuckets, "更适合尝试发起预约的时段", "accent"),
        riskItem("peak-warning", "高峰提醒", highLoadBuckets, "需要尽量避开的时段数量", "warning"),
        riskItem("maintenance-impact", "维护提醒", maintenanceRoomsToday, "今日有维护影响的房间数", "danger"),
        riskItem("easy-rooms", "易预约房间", easyRooms, "当前负载更轻的会议室数", "info")
    );
  }

  private List<StatsDtos.DashboardQuickLinkContext> buildAdminQuickLinks() {
    return List.of(
        quickLink("处理待审批", "直接查看待审批预约列表", "/admin/approvals", "warning", Map.of("focus", "pending")),
        quickLink("维护会议室", "快速进入维护中过滤结果", "/rooms", "danger", Map.of("status", "MAINTENANCE")),
        quickLink("查看可用资源", "查看当前可用会议室列表", "/rooms", "accent", Map.of("status", "AVAILABLE")),
        quickLink("进入会议预约", "回到日历页做时段判断", "/calendar", "info", Map.of("focus", "today")),
        quickLink("用户管理", "检查账号状态与角色结构", "/admin/users", "neutral", Map.of("scope", "all"))
    );
  }

  private List<StatsDtos.DashboardQuickLinkContext> buildUserQuickLinks() {
    return List.of(
        quickLink("发起预约", "从空闲窗口进入预约流", "/calendar", "accent", Map.of("suggest", "quiet")),
        quickLink("我的待处理", "查看待审批和已确认预约", "/mine", "warning", Map.of("status", "PENDING")),
        quickLink("查看可约房间", "只看当前可用的会议室", "/rooms", "info", Map.of("status", "AVAILABLE")),
        quickLink("通知中心", "查看审批与会议提醒", "/dashboard", "neutral", Map.of("panel", "notifications"))
    );
  }

  private List<StatsDtos.DashboardHeatmapBucket> buildHeatmap(LocalDate day,
                                                              List<ReservationEntity> reservations,
                                                              int totalRooms) {
    List<StatsDtos.DashboardHeatmapBucket> buckets = new ArrayList<>();
    for (int hour = BUSINESS_START_HOUR; hour < BUSINESS_END_HOUR; hour++) {
      LocalDateTime bucketStart = day.atTime(hour, 0);
      LocalDateTime bucketEnd = bucketStart.plusHours(1);
      Set<Long> activeRooms = new HashSet<>();
      int count = 0;
      for (ReservationEntity reservation : reservations) {
        if (!overlaps(reservation, bucketStart, bucketEnd)) {
          continue;
        }
        count++;
        if (reservation.getRoomId() != null) {
          activeRooms.add(reservation.getRoomId());
        }
      }

      int occupancy = totalRooms <= 0 ? 0 : (int) Math.round((activeRooms.size() * 100.0) / totalRooms);
      StatsDtos.DashboardHeatmapBucket bucket = new StatsDtos.DashboardHeatmapBucket();
      bucket.setLabel(String.format("%02d:00-%02d:00", hour, hour + 1));
      bucket.setReservationCount(count);
      bucket.setActiveRoomCount(activeRooms.size());
      bucket.setOccupancyPercent(occupancy);
      bucket.setLoad(loadTone(occupancy));
      buckets.add(bucket);
    }
    return buckets;
  }

  private List<StatsDtos.DashboardTrendPoint> buildWeeklyTrend(LocalDate startDay,
                                                               Map<LocalDate, List<ReservationEntity>> reservationsByDay,
                                                               int totalRooms) {
    List<StatsDtos.DashboardTrendPoint> trend = new ArrayList<>();
    for (int offset = 0; offset < 7; offset++) {
      LocalDate day = startDay.plusDays(offset);
      List<ReservationEntity> dayReservations = reservationsByDay.getOrDefault(day, Collections.emptyList());
      List<StatsDtos.DashboardHeatmapBucket> dayHeatmap = buildHeatmap(day, dayReservations, totalRooms);
      long pendingCount = dayReservations.stream()
          .filter(reservation -> ReservationStatus.PENDING.name().equals(reservation.getStatus()))
          .count();
      long riskCount = dayHeatmap.stream().filter(bucket -> "high".equals(bucket.getLoad())).count();

      StatsDtos.DashboardTrendPoint point = new StatsDtos.DashboardTrendPoint();
      point.setDay(day.toString());
      point.setLabel(day.format(DAY_LABEL));
      point.setReservationCount(dayReservations.size());
      point.setPendingCount(pendingCount);
      point.setRiskCount(riskCount);
      trend.add(point);
    }
    return trend;
  }

  private StatsDtos.DashboardTaskItem taskItem(String key,
                                               String label,
                                               long value,
                                               String detail,
                                               String tone,
                                               String to,
                                               Map<String, String> query) {
    StatsDtos.DashboardTaskItem item = new StatsDtos.DashboardTaskItem();
    item.setKey(key);
    item.setLabel(label);
    item.setValue(value);
    item.setDetail(detail);
    item.setTone(tone);
    item.setTo(to);
    item.setQuery(query);
    return item;
  }

  private StatsDtos.DashboardMetric metric(String key,
                                           String label,
                                           String value,
                                           String detail,
                                           String tone) {
    StatsDtos.DashboardMetric metric = new StatsDtos.DashboardMetric();
    metric.setKey(key);
    metric.setLabel(label);
    metric.setValue(value);
    metric.setDetail(detail);
    metric.setTone(tone);
    return metric;
  }

  private StatsDtos.DashboardRiskDistributionItem riskItem(String key,
                                                           String label,
                                                           long value,
                                                           String detail,
                                                           String tone) {
    StatsDtos.DashboardRiskDistributionItem item = new StatsDtos.DashboardRiskDistributionItem();
    item.setKey(key);
    item.setLabel(label);
    item.setValue(value);
    item.setDetail(detail);
    item.setTone(tone);
    return item;
  }

  private StatsDtos.DashboardQuickLinkContext quickLink(String label,
                                                        String description,
                                                        String to,
                                                        String tone,
                                                        Map<String, String> query) {
    StatsDtos.DashboardQuickLinkContext link = new StatsDtos.DashboardQuickLinkContext();
    link.setLabel(label);
    link.setDescription(description);
    link.setTo(to);
    link.setTone(tone);
    link.setQuery(query);
    return link;
  }

  private boolean isAdminRole(String role) {
    return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
  }

  private String roleLabel(String role) {
    if ("SUPER_ADMIN".equals(role)) return "超级管理员";
    if ("ADMIN".equals(role)) return "管理员";
    return "用户";
  }

  private long safeLong(Long value) {
    return value == null ? 0L : value;
  }

  private List<String> filterMeetingNotifications(List<String> notifications) {
    return safeList(notifications).stream()
        .filter(Objects::nonNull)
        .filter(this::isMeetingNotification)
        .toList();
  }

  private boolean isMeetingNotification(String content) {
    if (content.isBlank()) {
      return false;
    }
    for (String blockedMarker : BLOCKED_NOTIFICATION_MARKERS) {
      if (content.contains(blockedMarker)) {
        return false;
      }
    }
    for (String marker : MEETING_NOTIFICATION_MARKERS) {
      if (content.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  private <T> List<T> safeList(List<T> value) {
    return value == null ? Collections.emptyList() : value;
  }

  private boolean isRoomAvailableNow(RoomDtos.RoomResp room, LocalDateTime now) {
    return "AVAILABLE".equals(normalizeStatus(room.getStatus())) && !hasMaintenanceAt(room, now);
  }

  private boolean isMaintenanceToday(RoomDtos.RoomResp room, LocalDate today, LocalDateTime now) {
    return "MAINTENANCE".equals(normalizeStatus(room.getStatus()))
        || hasMaintenanceAt(room, now)
        || hasMaintenanceOnDay(room, today);
  }

  private boolean hasMaintenanceOnDay(RoomDtos.RoomResp room, LocalDate day) {
    LocalDateTime dayStart = day.atStartOfDay();
    LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
    return safeList(room.getMaintenanceSlots()).stream().anyMatch(slot -> slotOverlaps(slot, dayStart, dayEnd));
  }

  private boolean hasMaintenanceAt(RoomDtos.RoomResp room, LocalDateTime moment) {
    return safeList(room.getMaintenanceSlots()).stream().anyMatch(slot -> slotOverlaps(slot, moment, moment.plusMinutes(1)));
  }

  private boolean slotOverlaps(RoomDtos.MaintenanceSlot slot, LocalDateTime start, LocalDateTime end) {
    if (slot == null || slot.getStartTime() == null || slot.getEndTime() == null) {
      return false;
    }
    return start.isBefore(slot.getEndTime()) && end.isAfter(slot.getStartTime());
  }

  private boolean overlaps(ReservationEntity reservation, LocalDateTime start, LocalDateTime end) {
    if (reservation == null || reservation.getStartTime() == null || reservation.getEndTime() == null) {
      return false;
    }
    return reservation.getStartTime().isBefore(end) && reservation.getEndTime().isAfter(start);
  }

  private String percent(long numerator, long denominator) {
    if (denominator <= 0) return "0%";
    return (int) Math.round((numerator * 100.0) / denominator) + "%";
  }

  private String loadTone(int occupancyPercent) {
    if (occupancyPercent >= 70) return "high";
    if (occupancyPercent >= 30) return "medium";
    return "low";
  }

  private String normalizeStatus(String status) {
    if (status == null) return "AVAILABLE";
    return status.trim().toUpperCase();
  }
}
