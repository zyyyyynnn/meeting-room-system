package com.example.mrs.reservation;

import com.example.mrs.redis.RedisKeys;
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
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
    return redis.opsForList().range(key, 0, 49);
  }
}
