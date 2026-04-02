package com.example.mrs.reservation;

import com.example.mrs.redis.RedisKeys;
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final List<String> BLOCKED_MARKERS = List.of(
      "示例数据已生成",
      "本周已补充预约",
      "界面联调",
      "运营看板、审批与趋势图层"
  );
  private static final List<String> MEETING_MARKERS = List.of(
      "预约",
      "会议",
      "审批",
      "维护",
      "会议室",
      "房间",
      "参会"
  );
  private final StringRedisTemplate redis;

  public NotificationService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void pushToUser(long userId, String content) {
    String key = RedisKeys.userNotifications(userId);
    String line = LocalDateTime.now().format(FMT) + " | " + content;
    redis.opsForList().leftPush(key, line);
    redis.opsForList().trim(key, 0, 49);
  }

  public List<String> myNotifications() {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    String key = RedisKeys.userNotifications(me.id());
    List<String> notifications = redis.opsForList().range(key, 0, 49);
    if (notifications == null || notifications.isEmpty()) {
      return List.of();
    }
    return notifications.stream()
        .filter(Objects::nonNull)
        .filter(this::shouldDisplay)
        .toList();
  }

  private boolean shouldDisplay(String content) {
    if (content.isBlank()) {
      return false;
    }
    for (String blockedMarker : BLOCKED_MARKERS) {
      if (content.contains(blockedMarker)) {
        return false;
      }
    }
    for (String meetingMarker : MEETING_MARKERS) {
      if (content.contains(meetingMarker)) {
        return true;
      }
    }
    return false;
  }
}
