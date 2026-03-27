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
public class AuditLogService {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final StringRedisTemplate redis;

  public AuditLogService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void logForUser(long userId, String action) {
    String line = LocalDateTime.now().format(FMT) + " | " + action;
    String userKey = RedisKeys.userAuditLogs(userId);
    redis.opsForList().leftPush(userKey, line);
    redis.opsForList().trim(userKey, 0, 199);

    String adminKey = RedisKeys.adminAuditLogs();
    redis.opsForList().leftPush(adminKey, "uid=" + userId + " | " + line);
    redis.opsForList().trim(adminKey, 0, 999);
  }

  public List<String> myLogs() {
    JwtPrincipal me = SecurityUtil.requirePrincipal();
    return redis.opsForList().range(RedisKeys.userAuditLogs(me.id()), 0, 99);
  }

  public List<String> adminLogs() {
    return redis.opsForList().range(RedisKeys.adminAuditLogs(), 0, 199);
  }
}
