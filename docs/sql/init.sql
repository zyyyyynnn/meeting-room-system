-- meeting-room-system MySQL 初始化脚本。
-- 空数据库环境下，请先执行本脚本，再启动后端服务。

CREATE DATABASE IF NOT EXISTS meeting_room
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE meeting_room;

-- 如需完全重建，请先确认数据影响，再手动取消下列注释。
-- DROP TABLE IF EXISTS reservation;
-- DROP TABLE IF EXISTS meeting_room;
-- DROP TABLE IF EXISTS sys_user;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
  role VARCHAR(32) NOT NULL COMMENT '角色：USER / ADMIN / SUPER_ADMIN',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role (role),
  KEY idx_sys_user_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS meeting_room (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会议室主键',
  name VARCHAR(128) NOT NULL COMMENT '会议室名称',
  capacity INT NOT NULL COMMENT '容纳人数',
  equipment_json JSON NULL COMMENT '设备列表 JSON',
  require_approval TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否需要审批',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_meeting_room_name (name),
  KEY idx_meeting_room_capacity (capacity),
  KEY idx_meeting_room_require_approval (require_approval)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议室表';

CREATE TABLE IF NOT EXISTS reservation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约主键',
  user_id BIGINT NOT NULL COMMENT '预约用户 ID',
  room_id BIGINT NOT NULL COMMENT '会议室 ID',
  start_time DATETIME NOT NULL COMMENT '预约开始时间',
  end_time DATETIME NOT NULL COMMENT '预约结束时间',
  status VARCHAR(32) NOT NULL COMMENT '状态：PENDING / APPROVED / REJECTED / CANCELLED',
  reason VARCHAR(500) NULL COMMENT '预约原因/备注',
  admin_comment VARCHAR(500) NULL COMMENT '审批备注',
  approved_by BIGINT NULL COMMENT '审批人 ID',
  approved_at DATETIME NULL COMMENT '审批时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_reservation_room_time (room_id, start_time, end_time),
  KEY idx_reservation_user_time (user_id, start_time),
  KEY idx_reservation_status (status),
  KEY idx_reservation_approved_at (approved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议室预约表';
