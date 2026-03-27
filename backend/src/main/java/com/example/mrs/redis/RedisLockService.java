package com.example.mrs.redis;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisLockService {
  private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
      Long.class
  );

  private final StringRedisTemplate stringRedisTemplate;

  public RedisLockService(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @SuppressWarnings("null")
  public String tryLock(String key, Duration ttl) {
    final String lockKey = Objects.requireNonNull(key, "key must not be null");
    final Duration lockTtl = Objects.requireNonNull(ttl, "ttl must not be null");

    final String token = UUID.randomUUID().toString();
    Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, lockTtl);
    return Boolean.TRUE.equals(ok) ? token : null;
  }

  public void unlock(String key, String token) {
    if (key == null || token == null) {
      return;
    }
    stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
  }
}
