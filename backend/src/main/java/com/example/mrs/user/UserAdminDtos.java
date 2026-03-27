package com.example.mrs.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

public class UserAdminDtos {

  @Data
  public static class UserResp {
    private Long id;
    private String username;
    private String role;
    private Boolean enabled;
  }

  @Data
  public static class UpdateRoleReq {
    @NotNull(message = "角色不能为空")
    @Schema(description = "角色", example = "ADMIN")
    private String role;
  }

  @Data
  public static class UpdateEnabledReq {
    @NotNull(message = "启用状态不能为空")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
  }
}
