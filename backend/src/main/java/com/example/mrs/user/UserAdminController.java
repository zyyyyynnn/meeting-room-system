package com.example.mrs.user;

import com.example.mrs.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户与权限管理")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class UserAdminController {
  private final UserAdminService userAdminService;

  public UserAdminController(UserAdminService userAdminService) {
    this.userAdminService = userAdminService;
  }

  @GetMapping
  public ApiResponse<List<UserAdminDtos.UserResp>> listUsers() {
    return ApiResponse.ok(userAdminService.listUsers());
  }

  @PostMapping("/{id}/role")
  public ApiResponse<Void> updateRole(@PathVariable long id, @Valid @RequestBody UserAdminDtos.UpdateRoleReq req) {
    userAdminService.updateRole(id, req.getRole());
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/enabled")
  public ApiResponse<Void> updateEnabled(@PathVariable long id, @Valid @RequestBody UserAdminDtos.UpdateEnabledReq req) {
    userAdminService.updateEnabled(id, req.getEnabled());
    return ApiResponse.ok(null);
  }
}
