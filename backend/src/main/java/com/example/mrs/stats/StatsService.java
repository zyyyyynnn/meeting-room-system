package com.example.mrs.stats;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.domain.ReservationStatus;
import com.example.mrs.entity.MeetingRoomEntity;
import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.MeetingRoomMapper;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
  private final MeetingRoomMapper meetingRoomMapper;
  private final SysUserMapper sysUserMapper;
  private final ReservationMapper reservationMapper;

  public StatsService(MeetingRoomMapper meetingRoomMapper,
                      SysUserMapper sysUserMapper,
                      ReservationMapper reservationMapper) {
    this.meetingRoomMapper = meetingRoomMapper;
    this.sysUserMapper = sysUserMapper;
    this.reservationMapper = reservationMapper;
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
    if ("ADMIN".equals(me.role()) || "SUPER_ADMIN".equals(me.role())) {
      pendingApprovals = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
          .eq(ReservationEntity::getStatus, ReservationStatus.PENDING.name()));
    }

    StatsDtos.UserBreakdown breakdown = new StatsDtos.UserBreakdown();
    breakdown.setNormalUsers(normalUserCount == null ? 0 : normalUserCount);
    breakdown.setAdminUsers(adminUserCount == null ? 0 : adminUserCount);
    breakdown.setSuperAdminUsers(superAdminUserCount == null ? 0 : superAdminUserCount);
    breakdown.setDisabledUsers(disabledUserCount == null ? 0 : disabledUserCount);

    StatsDtos.OverviewResp resp = new StatsDtos.OverviewResp();
    resp.setTotalRooms(roomCount == null ? 0 : roomCount);
    resp.setTotalUsers(userCount == null ? 0 : userCount);
    resp.setTodayReservations(todayReservationCount == null ? 0 : todayReservationCount);
    resp.setMyUpcomingReservations(myUpcomingCount == null ? 0 : myUpcomingCount);
    resp.setPendingApprovals(pendingApprovals == null ? 0 : pendingApprovals);
    resp.setUserBreakdown(breakdown);
    return resp;
  }
}
