package com.example.mrs.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.domain.ReservationStatus;
import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.redis.RedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 会议室空闲状态缓存：
 * - 以“天”为单位缓存 48 个半小时位图：bit=1 表示该半小时被占用
 * - 同步缓存时间片列表，便于前端/诊断快速查看占用区间
 * - 用于快速冲突预检；最终仍以 DB 冲突检测为准
 */
@Service
public class ReservationBitmapService {
  private static final EnumSet<ReservationStatus> BLOCKING_STATUSES =
      EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);

  private static final Duration DAY_CACHE_TTL = Duration.ofDays(8);

  private final StringRedisTemplate stringRedisTemplate;
  private final ReservationMapper reservationMapper;
  private final ObjectMapper objectMapper;

  public ReservationBitmapService(StringRedisTemplate stringRedisTemplate,
                                  ReservationMapper reservationMapper,
                                  ObjectMapper objectMapper) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.reservationMapper = reservationMapper;
    this.objectMapper = objectMapper;
  }

  public boolean isSlotOccupied(long roomId, LocalDate day, int slotIndex) {
    String key = Objects.requireNonNull(RedisKeys.roomDayBitmap(roomId, day));
    Boolean bit = stringRedisTemplate.opsForValue().getBit(key, slotIndex);
    if (bit == null) {
      rebuildDay(roomId, day);
      bit = stringRedisTemplate.opsForValue().getBit(key, slotIndex);
    }
    return Boolean.TRUE.equals(bit);
  }

  public void warmupWeek(long roomId, LocalDate fromDay) {
    for (int i = 0; i < 7; i++) {
      rebuildDay(roomId, fromDay.plusDays(i));
    }
  }

  public void rebuildDay(long roomId, LocalDate day) {
    String bitmapKey = Objects.requireNonNull(RedisKeys.roomDayBitmap(roomId, day));
    String slicesKey = Objects.requireNonNull(RedisKeys.roomDaySlices(roomId, day));
    stringRedisTemplate.delete(List.of(bitmapKey, slicesKey));

    LocalDateTime dayStart = day.atStartOfDay();
    LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

    List<ReservationEntity> list = reservationMapper.selectList(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getRoomId, roomId)
        .in(ReservationEntity::getStatus, BLOCKING_STATUSES.stream().map(Enum::name).collect(Collectors.toList()))
        .lt(ReservationEntity::getStartTime, dayEnd)
        .gt(ReservationEntity::getEndTime, dayStart)
        .orderByAsc(ReservationEntity::getStartTime)
    );

    List<TimeSlice> slices = new ArrayList<>();
    for (ReservationEntity r : list) {
      LocalDateTime start = r.getStartTime().isBefore(dayStart) ? dayStart : r.getStartTime();
      LocalDateTime end = r.getEndTime().isAfter(dayEnd) ? dayEnd : r.getEndTime();

      int startSlot = toSlotIndex(start, false);
      int endSlot = toSlotIndex(end, true);
      for (int i = startSlot; i < Math.min(endSlot, 48); i++) {
        stringRedisTemplate.opsForValue().setBit(bitmapKey, i, true);
      }

      TimeSlice slice = new TimeSlice();
      slice.setReservationId(r.getId());
      slice.setStartTime(start);
      slice.setEndTime(end);
      slice.setStatus(r.getStatus());
      slices.add(slice);
    }

    try {
      String slicesJson = objectMapper.writeValueAsString(slices);
      stringRedisTemplate.opsForValue().set(slicesKey, slicesJson, DAY_CACHE_TTL);
    } catch (JsonProcessingException ignored) {
    }
    stringRedisTemplate.expire(bitmapKey, DAY_CACHE_TTL);
  }

  private int toSlotIndex(LocalDateTime t, boolean ceilEnd) {
    int half = t.getMinute() >= 30 ? 1 : 0;
    if (ceilEnd && t.getMinute() > 0 && t.getMinute() < 30) {
      half = 1;
    }
    if (ceilEnd && t.getMinute() > 30) {
      return t.getHour() * 2 + 2;
    }
    return t.getHour() * 2 + half;
  }

  public static class TimeSlice {
    private Long reservationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public Long getReservationId() {
      return reservationId;
    }

    public void setReservationId(Long reservationId) {
      this.reservationId = reservationId;
    }

    public LocalDateTime getStartTime() {
      return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
      this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
      return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
      this.endTime = endTime;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }
  }
}
