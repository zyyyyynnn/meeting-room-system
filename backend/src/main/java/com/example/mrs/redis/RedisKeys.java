package com.example.mrs.redis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RedisKeys {
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

  public static String roomDayBitmap(long roomId, LocalDate day) {
    return "mrs:room:" + roomId + ":day:" + day.format(DAY) + ":hour-bitmap";
  }

  public static String roomDaySlices(long roomId, LocalDate day) {
    return "mrs:room:" + roomId + ":day:" + day.format(DAY) + ":slices";
  }

  public static String roomDayLock(long roomId, LocalDate day) {
    return "mrs:lock:room:" + roomId + ":day:" + day.format(DAY);
  }

  public static String userRecentReservations(long userId) {
    return "mrs:user:" + userId + ":reservations:last30d";
  }

  public static String userNotifications(long userId) {
    return "mrs:user:" + userId + ":notifications";
  }

  public static String userAuditLogs(long userId) {
    return "mrs:user:" + userId + ":audit";
  }

  public static String adminAuditLogs() {
    return "mrs:admin:audit";
  }

  public static String roomStatus(long roomId) {
    return "mrs:room:" + roomId + ":status";
  }

  public static String roomMaintenances(long roomId) {
    return "mrs:room:" + roomId + ":maintenances";
  }
}
