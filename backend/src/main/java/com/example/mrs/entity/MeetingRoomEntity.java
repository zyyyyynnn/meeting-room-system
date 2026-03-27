package com.example.mrs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("meeting_room")
public class MeetingRoomEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String name;
  private Integer capacity;
  private String equipmentJson;
  private Boolean requireApproval;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

