package com.example.mrs.room;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.common.BizException;
import com.example.mrs.domain.ReservationStatus;
import com.example.mrs.entity.MeetingRoomEntity;
import com.example.mrs.entity.ReservationEntity;
import com.example.mrs.mapper.MeetingRoomMapper;
import com.example.mrs.mapper.ReservationMapper;
import com.example.mrs.redis.RedisKeys;
import com.example.mrs.reservation.ReservationBitmapService;
import com.example.mrs.room.dto.RoomDtos;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MeetingRoomService {
  private final MeetingRoomMapper roomMapper;
  private final ReservationMapper reservationMapper;
  private final ReservationBitmapService bitmapService;
  private final ObjectMapper objectMapper;
  private final StringRedisTemplate stringRedisTemplate;

  public MeetingRoomService(MeetingRoomMapper roomMapper,
                            ReservationMapper reservationMapper,
                            ReservationBitmapService bitmapService,
                            ObjectMapper objectMapper,
                            StringRedisTemplate stringRedisTemplate) {
    this.roomMapper = roomMapper;
    this.reservationMapper = reservationMapper;
    this.bitmapService = bitmapService;
    this.objectMapper = objectMapper;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public List<RoomDtos.RoomResp> listAll() {
    List<MeetingRoomEntity> entities = roomMapper.selectList(new LambdaQueryWrapper<MeetingRoomEntity>()
        .orderByAsc(MeetingRoomEntity::getId));

    LocalDate today = LocalDate.now();
    for (MeetingRoomEntity entity : entities) {
      bitmapService.warmupWeek(entity.getId(), today);
    }

    return entities.stream().map(this::toResp).collect(Collectors.toList());
  }

  public RoomDtos.RoomResp create(RoomDtos.RoomCreateReq req) {
    MeetingRoomEntity exists = roomMapper.selectOne(new LambdaQueryWrapper<MeetingRoomEntity>()
        .eq(MeetingRoomEntity::getName, req.getName())
        .last("limit 1"));
    if (exists != null) {
      throw BizException.badRequest("会议室名称已存在");
    }
    MeetingRoomEntity e = new MeetingRoomEntity();
    e.setName(req.getName());
    e.setCapacity(req.getCapacity());
    e.setEquipmentJson(writeEquipment(req.getEquipment()));
    e.setRequireApproval(Boolean.TRUE.equals(req.getRequireApproval()));
    roomMapper.insert(e);

    bitmapService.warmupWeek(e.getId(), LocalDate.now());
    return toResp(e);
  }

  public RoomDtos.RoomResp update(long id, RoomDtos.RoomUpdateReq req) {
    MeetingRoomEntity e = roomMapper.selectById(id);
    if (e == null) {
      throw BizException.notFound("会议室不存在");
    }
    MeetingRoomEntity conflict = roomMapper.selectOne(new LambdaQueryWrapper<MeetingRoomEntity>()
        .eq(MeetingRoomEntity::getName, req.getName())
        .ne(MeetingRoomEntity::getId, id)
        .last("limit 1"));
    if (conflict != null) {
      throw BizException.badRequest("会议室名称已存在");
    }
    e.setName(req.getName());
    e.setCapacity(req.getCapacity());
    e.setEquipmentJson(writeEquipment(req.getEquipment()));
    e.setRequireApproval(Boolean.TRUE.equals(req.getRequireApproval()));
    roomMapper.updateById(e);

    bitmapService.warmupWeek(id, LocalDate.now());
    return toResp(e);
  }

  public void delete(long id) {
    Long blockingCount = reservationMapper.selectCount(new LambdaQueryWrapper<ReservationEntity>()
        .eq(ReservationEntity::getRoomId, id)
        .in(ReservationEntity::getStatus,
            ReservationStatus.PENDING.name(),
            ReservationStatus.APPROVED.name()));
    if (blockingCount != null && blockingCount > 0) {
      throw BizException.badRequest("该会议室仍有待审批/已批准预约，不可删除");
    }
    roomMapper.deleteById(id);
  }

  public MeetingRoomEntity getById(long id) {
    MeetingRoomEntity e = roomMapper.selectById(id);
    if (e == null) {
      throw BizException.notFound("会议室不存在");
    }
    return e;
  }

  private RoomDtos.RoomResp toResp(MeetingRoomEntity e) {
    RoomDtos.RoomResp r = new RoomDtos.RoomResp();
    r.setId(e.getId());
    r.setName(e.getName());
    r.setCapacity(e.getCapacity());
    r.setEquipment(readEquipment(e.getEquipmentJson()));
    r.setRequireApproval(Boolean.TRUE.equals(e.getRequireApproval()));
    r.setStatus(getRoomStatus(e.getId()));
    r.setMaintenanceSlots(listMaintenances(e.getId()));
    return r;
  }

  private List<String> readEquipment(String json) {
    if (json == null || json.trim().isEmpty()) return Collections.emptyList();
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception ex) {
      return Collections.emptyList();
    }
  }

  private String writeEquipment(List<String> equipment) {
    if (equipment == null) return null;
    try {
      return objectMapper.writeValueAsString(equipment);
    } catch (Exception ex) {
      return null;
    }
  }

  public void updateRoomStatus(long roomId, String status) {
    getById(roomId);
    String normalized = (status == null ? "" : status.trim().toUpperCase());
    if (!("AVAILABLE".equals(normalized) || "MAINTENANCE".equals(normalized) || "DISABLED".equals(normalized))) {
      throw BizException.badRequest("会议室状态仅支持 AVAILABLE / MAINTENANCE / DISABLED");
    }
    stringRedisTemplate.opsForValue().set(RedisKeys.roomStatus(roomId), normalized);
  }

  public String getRoomStatus(long roomId) {
    String v = stringRedisTemplate.opsForValue().get(RedisKeys.roomStatus(roomId));
    if (v == null || v.isBlank()) return "AVAILABLE";
    return v;
  }

  public void addMaintenance(long roomId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
    getById(roomId);
    if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
      throw BizException.badRequest("维护时段非法");
    }
    RoomDtos.MaintenanceSlot slot = new RoomDtos.MaintenanceSlot();
    slot.setStartTime(startTime);
    slot.setEndTime(endTime);
    slot.setReason(reason == null ? "维护" : reason);
    try {
      String key = RedisKeys.roomMaintenances(roomId);
      stringRedisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(slot));
      stringRedisTemplate.opsForList().trim(key, -200, -1);
    } catch (Exception e) {
      throw BizException.badRequest("维护时段保存失败");
    }
  }

  public List<RoomDtos.MaintenanceSlot> listMaintenances(long roomId) {
    String key = RedisKeys.roomMaintenances(roomId);
    List<String> rows = stringRedisTemplate.opsForList().range(key, 0, -1);
    if (rows == null || rows.isEmpty()) return Collections.emptyList();
    List<RoomDtos.MaintenanceSlot> out = new ArrayList<>();
    for (String row : rows) {
      try {
        out.add(objectMapper.readValue(row, RoomDtos.MaintenanceSlot.class));
      } catch (Exception ignored) {
      }
    }
    return out;
  }
}
