package com.example.mrs.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.mapper.MeetingRoomMapper;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.reservation.NotificationService;
import com.example.mrs.room.MeetingRoomService;
import com.example.mrs.room.dto.RoomDtos;
import com.example.mrs.security.JwtPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {
  @Mock
  private MeetingRoomMapper meetingRoomMapper;
  @Mock
  private SysUserMapper sysUserMapper;
  @Mock
  private ReservationMapper reservationMapper;
  @Mock
  private MeetingRoomService meetingRoomService;
  @Mock
  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void dashboardReturnsAdminCommandCenterView() {
    authenticate(new JwtPrincipal(7L, "ops-admin", "ADMIN"));
    LocalDate today = LocalDate.now();
    LocalDateTime morning = today.atTime(9, 0);
    LocalDateTime midday = today.atTime(11, 0);

    when(meetingRoomService.listAll()).thenReturn(List.of(
        room(1L, "Atlas", "AVAILABLE"),
        room(2L, "Borealis", "MAINTENANCE"),
        room(3L, "Cosmos", "AVAILABLE")
    ));
    when(notificationService.myNotifications()).thenReturn(List.of(
        "2026-04-01 18:05 | 示例数据已生成，可直接查看运营看板、审批与趋势图层",
        "2026-04-01 18:05 | 预约已审批通过 rid=102",
        "2026-04-01 18:05 | 会议提醒：会议开始前 30 分钟请准备参会"
    ));
    when(reservationMapper.selectList(any())).thenReturn(List.of(
        reservation(101L, 7L, 1L, "APPROVED", morning, morning.plusHours(1)),
        reservation(102L, 9L, 3L, "PENDING", morning.plusMinutes(30), morning.plusHours(2)),
        reservation(103L, 12L, 1L, "APPROVED", midday, midday.plusHours(1)),
        reservation(104L, 7L, 1L, "PENDING", today.plusDays(1).atTime(10, 0), today.plusDays(1).atTime(11, 0))
    ));
    when(reservationMapper.selectCount(any())).thenReturn(2L);

    StatsService service = new StatsService(
        meetingRoomMapper,
        sysUserMapper,
        reservationMapper,
        meetingRoomService,
        notificationService
    );

    StatsDtos.DashboardResp resp = service.dashboard();

    assertThat(resp.isAdminView()).isTrue();
    assertThat(resp.getWelcome().getMessage()).isEqualTo("Welcome，ops-admin！");
    assertThat(resp.getTaskSummary().getTitle()).isEqualTo("今日待办与异常");
    assertThat(resp.getTaskSummary().getItems()).extracting(StatsDtos.DashboardTaskItem::getLabel)
        .contains("待审批预约", "维护异常", "关键通知");
    assertThat(resp.getTaskSummary().getItems().stream()
        .filter(item -> "notifications".equals(item.getKey()))
        .findFirst()
        .map(StatsDtos.DashboardTaskItem::getValue))
        .hasValue(2L);
    assertThat(resp.getResourceSnapshot().getMetrics()).extracting(StatsDtos.DashboardMetric::getLabel)
        .contains("当前可用会议室", "今日预约总量", "维护占用");
    assertThat(resp.getTodayHeatmap()).hasSize(10);
    assertThat(resp.getWeeklyTrend()).hasSize(7);
    assertThat(resp.getRiskDistribution()).extracting(StatsDtos.DashboardRiskDistributionItem::getLabel)
        .contains("待审批压力", "维护占用", "高峰时段");
    assertThat(resp.getQuickLinkContext()).extracting(StatsDtos.DashboardQuickLinkContext::getTo)
        .contains("/admin/approvals", "/rooms", "/calendar");
  }

  @Test
  void dashboardReturnsPersonalizedUserView() {
    authenticate(new JwtPrincipal(13L, "normal-user", "USER"));
    LocalDate today = LocalDate.now();

    when(meetingRoomService.listAll()).thenReturn(List.of(
        room(1L, "Atlas", "AVAILABLE"),
        room(2L, "Borealis", "AVAILABLE"),
        room(3L, "Cosmos", "MAINTENANCE")
    ));
    when(notificationService.myNotifications()).thenReturn(List.of(
        "2026-04-01 18:05 | 本周已补充预约、维护与通知样本，适合直接做界面联调",
        "2026-04-01 18:05 | 审批提醒：请留意审批结果通知"
    ));
    when(reservationMapper.selectList(any())).thenReturn(List.of(
        reservation(201L, 13L, 1L, "APPROVED", today.plusDays(1).atTime(9, 0), today.plusDays(1).atTime(10, 0)),
        reservation(202L, 13L, 2L, "PENDING", today.plusDays(2).atTime(14, 0), today.plusDays(2).atTime(15, 0)),
        reservation(203L, 21L, 1L, "APPROVED", today.atTime(10, 0), today.atTime(11, 0))
    ));

    StatsService service = new StatsService(
        meetingRoomMapper,
        sysUserMapper,
        reservationMapper,
        meetingRoomService,
        notificationService
    );

    StatsDtos.DashboardResp resp = service.dashboard();

    assertThat(resp.isAdminView()).isFalse();
    assertThat(resp.getWelcome().getMessage()).isEqualTo("Welcome，normal-user！");
    assertThat(resp.getTaskSummary().getTitle()).isEqualTo("我的今日主控");
    assertThat(resp.getTaskSummary().getItems()).extracting(StatsDtos.DashboardTaskItem::getLabel)
        .contains("我的待进行会议", "我的待处理预约", "通知提醒");
    assertThat(resp.getTaskSummary().getItems().stream()
        .filter(item -> "notifications".equals(item.getKey()))
        .findFirst()
        .map(StatsDtos.DashboardTaskItem::getValue))
        .hasValue(1L);
    assertThat(resp.getQuickLinkContext()).extracting(StatsDtos.DashboardQuickLinkContext::getTo)
        .contains("/calendar", "/mine", "/rooms", "/dashboard");
    assertThat(resp.getQuickLinkContext()).extracting(StatsDtos.DashboardQuickLinkContext::getTo)
        .doesNotContain("/admin/approvals", "/admin/users");
  }

  private void authenticate(JwtPrincipal principal) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "n/a", List.of())
    );
  }

  private RoomDtos.RoomResp room(Long id, String name, String status) {
    RoomDtos.RoomResp room = new RoomDtos.RoomResp();
    room.setId(id);
    room.setName(name);
    room.setCapacity(10);
    room.setEquipment(List.of("投影仪"));
    room.setRequireApproval(Boolean.TRUE);
    room.setStatus(status);
    room.setMaintenanceSlots(List.of());
    return room;
  }

  private ReservationEntity reservation(Long id, Long userId, Long roomId, String status, LocalDateTime start, LocalDateTime end) {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(id);
    reservation.setUserId(userId);
    reservation.setRoomId(roomId);
    reservation.setStatus(status);
    reservation.setStartTime(start);
    reservation.setEndTime(end);
    reservation.setReason("sync");
    return reservation;
  }
}
