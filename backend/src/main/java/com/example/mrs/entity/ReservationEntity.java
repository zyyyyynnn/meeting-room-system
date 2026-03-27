package com.example.mrs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("reservation")
public class ReservationEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private Long roomId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;
  private String reason;
  private String adminComment;
  private Long approvedBy;
  private LocalDateTime approvedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

