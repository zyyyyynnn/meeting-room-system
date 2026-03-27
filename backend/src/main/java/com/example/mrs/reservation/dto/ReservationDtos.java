package com.example.mrs.reservation.dto;

import com.example.mrs.reservation.ReservationBitmapService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public class ReservationDtos {

  @Data
  public static class CreateReq {
    @NotNull(message = "会议室不能为空")
    private Long roomId;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "预约原因/备注")
    private String reason;
  }

  @Data
  public static class BatchCreateReq {
    @NotNull(message = "会议室不能为空")
    private Long roomId;

    @NotNull(message = "首次开始时间不能为空")
    private LocalDateTime firstStartTime;

    @NotNull(message = "首次结束时间不能为空")
    private LocalDateTime firstEndTime;

    @NotNull(message = "周期次数不能为空")
    private Integer repeatWeeks;

    private String reason;
  }

  @Data
  public static class ApproveReq {
    @Schema(description = "管理员备注")
    private String adminComment;
  }

  @Data
  public static class RejectReq {
    @Schema(description = "驳回原因")
    private String adminComment;
  }

  @Data
  public static class ReservationResp {
    private Long id;
    private Long userId;
    private String username;
    private Long roomId;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String reason;
    private String adminComment;
    private Long approvedBy;
    private LocalDateTime approvedAt;
  }

  @Data
  public static class RoomDayOccupancyResp {
    private Long roomId;
    private LocalDateTime dayStart;
    private List<Integer> occupiedSlots;
    private List<ReservationBitmapService.TimeSlice> slices;
  }

  @Data
  public static class ConflictSuggestionResp {
    private String conflictMessage;
    private List<AlternativeSlot> alternatives;
  }

  @Data
  public static class AlternativeSlot {
    private Long roomId;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String tip;
  }
}
