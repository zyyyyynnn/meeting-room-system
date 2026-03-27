package com.example.mrs.reservation;

import com.example.mrs.common.ApiResponse;
import com.example.mrs.reservation.dto.ReservationDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "预约审批（管理员）")
@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class ReservationAdminController {
  private final ReservationAdminService adminService;
  private final AuditLogService auditLogService;

  public ReservationAdminController(ReservationAdminService adminService, AuditLogService auditLogService) {
    this.adminService = adminService;
    this.auditLogService = auditLogService;
  }

  @GetMapping("/pending")
  public ApiResponse<List<ReservationDtos.ReservationResp>> pending() {
    return ApiResponse.ok(adminService.pendingList());
  }

  @GetMapping("/recent")
  public ApiResponse<List<ReservationDtos.ReservationResp>> recentReviewed() {
    return ApiResponse.ok(adminService.recentReviewedList());
  }

  @PostMapping("/{id}/approve")
  public ApiResponse<Void> approve(@PathVariable long id, @Valid @RequestBody ReservationDtos.ApproveReq req) {
    adminService.approve(id, req);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/reject")
  public ApiResponse<Void> reject(@PathVariable long id, @Valid @RequestBody ReservationDtos.RejectReq req) {
    adminService.reject(id, req);
    return ApiResponse.ok(null);
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PostMapping("/{id}/revoke")
  public ApiResponse<Void> revokeReviewed(@PathVariable long id) {
    adminService.revokeReviewed(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/audit")
  public ApiResponse<List<String>> auditLogs() {
    return ApiResponse.ok(auditLogService.adminLogs());
  }
}
