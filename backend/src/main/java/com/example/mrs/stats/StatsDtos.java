package com.example.mrs.stats;

import java.util.List;
import java.util.Map;
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

  @Data
  public static class DashboardResp {
    private boolean adminView;
    private Welcome welcome;
    private DashboardTaskSummary taskSummary;
    private DashboardResourceSnapshot resourceSnapshot;
    private List<DashboardHeatmapBucket> todayHeatmap;
    private List<DashboardTrendPoint> weeklyTrend;
    private List<DashboardRiskDistributionItem> riskDistribution;
    private List<DashboardQuickLinkContext> quickLinkContext;
  }

  @Data
  public static class Welcome {
    private String roleLabel;
    private String message;
  }

  @Data
  public static class DashboardTaskSummary {
    private String title;
    private String subtitle;
    private List<DashboardTaskItem> items;
  }

  @Data
  public static class DashboardTaskItem {
    private String key;
    private String label;
    private long value;
    private String detail;
    private String tone;
    private String to;
    private Map<String, String> query;
  }

  @Data
  public static class DashboardResourceSnapshot {
    private String title;
    private String subtitle;
    private List<DashboardMetric> metrics;
  }

  @Data
  public static class DashboardMetric {
    private String key;
    private String label;
    private String value;
    private String detail;
    private String tone;
  }

  @Data
  public static class DashboardHeatmapBucket {
    private String label;
    private int reservationCount;
    private int activeRoomCount;
    private int occupancyPercent;
    private String load;
  }

  @Data
  public static class DashboardTrendPoint {
    private String day;
    private String label;
    private long reservationCount;
    private long pendingCount;
    private long riskCount;
  }

  @Data
  public static class DashboardRiskDistributionItem {
    private String key;
    private String label;
    private long value;
    private String detail;
    private String tone;
  }

  @Data
  public static class DashboardQuickLinkContext {
    private String label;
    private String description;
    private String to;
    private String tone;
    private Map<String, String> query;
  }
}
