package com.example.mrs.room;

import com.example.mrs.common.ApiResponse;
import com.example.mrs.room.dto.RoomDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "会议室")
@RestController
@RequestMapping("/api/rooms")
public class MeetingRoomController {
  private final MeetingRoomService roomService;

  public MeetingRoomController(MeetingRoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping
  public ApiResponse<List<RoomDtos.RoomResp>> list() {
    return ApiResponse.ok(roomService.listAll());
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PostMapping
  public ApiResponse<RoomDtos.RoomResp> create(@Valid @RequestBody RoomDtos.RoomCreateReq req) {
    return ApiResponse.ok(roomService.create(req));
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PutMapping("/{id}")
  public ApiResponse<RoomDtos.RoomResp> update(@PathVariable long id, @Valid @RequestBody RoomDtos.RoomUpdateReq req) {
    return ApiResponse.ok(roomService.update(id, req));
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    roomService.delete(id);
    return ApiResponse.ok(null);
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PostMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable long id, @Valid @RequestBody RoomDtos.RoomStatusReq req) {
    roomService.updateRoomStatus(id, req.getStatus());
    return ApiResponse.ok(null);
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PostMapping("/{id}/maintenances")
  public ApiResponse<Void> addMaintenance(@PathVariable long id, @Valid @RequestBody RoomDtos.MaintenanceCreateReq req) {
    roomService.addMaintenance(id, req.getStartTime(), req.getEndTime(), req.getReason());
    return ApiResponse.ok(null);
  }
}
