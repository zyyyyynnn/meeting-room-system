package com.example.mrs.stats;

import lombok.Data;

public class StatsDtos {
  @Data
  public static class OverviewResp {
    private long totalRooms;
    private long totalUsers;
    private long todayReservations;
    private long myUpcomingReservations;
    private long pendingApprovals;
    private UserBreakdown userBreakdown;
  }

  @Data
  public static class UserBreakdown {
    private long normalUsers;
    private long adminUsers;
    private long superAdminUsers;
    private long disabledUsers;
  }
}
