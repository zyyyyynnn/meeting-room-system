package com.example.mrs.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class RoomDtos {
  @Data
  public static class RoomCreateReq {
    @NotBlank(message = "会议室名称不能为空")
    @Size(max = 128, message = "会议室名称过长")
    private String name;

    @NotNull(message = "容量不能为空")
    @Min(value = 1, message = "容量必须大于 0")
    private Integer capacity;

    @Schema(description = "设备列表")
    private List<String> equipment;

    @Schema(description = "是否需要审批")
    private Boolean requireApproval;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class RoomUpdateReq extends RoomCreateReq {}

  @Data
  public static class RoomResp {
    private Long id;
    private String name;
    private Integer capacity;
    private List<String> equipment;
    private Boolean requireApproval;
    private String status;
    private List<MaintenanceSlot> maintenanceSlots;
  }

  @Data
  public static class MaintenanceSlot {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
  }

  @Data
  public static class RoomStatusReq {
    @NotBlank(message = "状态不能为空")
    private String status;
  }

  @Data
  public static class MaintenanceCreateReq {
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private String reason;
  }
}

