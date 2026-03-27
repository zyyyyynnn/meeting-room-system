package com.example.mrs.reservation;

import com.example.mrs.common.ApiResponse;
import com.example.mrs.reservation.dto.ReservationDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@Tag(name = "预约")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
  private final ReservationService reservationService;
  private final NotificationService notificationService;
  private final AuditLogService auditLogService;

  public ReservationController(ReservationService reservationService,
                               NotificationService notificationService,
                               AuditLogService auditLogService) {
    this.reservationService = reservationService;
    this.notificationService = notificationService;
    this.auditLogService = auditLogService;
  }

  @PostMapping
  public ApiResponse<ReservationDtos.ReservationResp> create(@Valid @RequestBody ReservationDtos.CreateReq req) {
    return ApiResponse.ok(reservationService.create(req));
  }

  @PostMapping("/batch/weekly")
  public ApiResponse<List<ReservationDtos.ReservationResp>> createBatchWeekly(
      @Valid @RequestBody ReservationDtos.BatchCreateReq req) {
    return ApiResponse.ok(reservationService.createBatchWeekly(req));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<Void> cancel(@PathVariable long id) {
    reservationService.cancel(id);
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteMine(@PathVariable long id) {
    reservationService.deleteMine(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/mine/recent")
  public ApiResponse<List<ReservationDtos.ReservationResp>> myRecent() {
    return ApiResponse.ok(reservationService.myRecent());
  }

  @GetMapping("/calendar")
  public ApiResponse<List<ReservationDtos.ReservationResp>> calendar(@RequestParam long roomId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
    return ApiResponse.ok(reservationService.calendar(roomId, start, end));
  }

  @GetMapping("/occupancy/day")
  public ApiResponse<ReservationDtos.RoomDayOccupancyResp> occupancyDay(
      @RequestParam long roomId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
    return ApiResponse.ok(reservationService.roomDayOccupancy(roomId, day));
  }

  @GetMapping("/suggestions")
  public ApiResponse<ReservationDtos.ConflictSuggestionResp> suggestions(
      @RequestParam long roomId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
    return ApiResponse.ok(reservationService.suggestAlternatives(roomId, start, end));
  }

  @GetMapping("/notifications")
  public ApiResponse<List<String>> notifications() {
    return ApiResponse.ok(notificationService.myNotifications());
  }

  @GetMapping("/audit/mine")
  public ApiResponse<List<String>> myAuditLogs() {
    return ApiResponse.ok(auditLogService.myLogs());
  }
}
