-- 停车场数据库初始化脚本

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `order-car`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `order-car`;

-- 创建停车记录表
CREATE TABLE IF NOT EXISTS `parking_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plate_num` VARCHAR(20) NOT NULL COMMENT '车牌号',
  `plate_color` VARCHAR(20) COMMENT '车牌颜色',
  `car_logo` VARCHAR(50) COMMENT '车辆品牌',
  `car_color` VARCHAR(20) COMMENT '车辆颜色',
  `vehicle_type` VARCHAR(20) COMMENT '车辆类型',
  `device_sn` VARCHAR(50) COMMENT '设备序列号',
  `entry_time` DATETIME NOT NULL COMMENT '进场时间',
  `exit_time` DATETIME COMMENT '出场时间',
  `parking_minutes` BIGINT COMMENT '停车时长（分钟）',
  `is_completed` BOOLEAN DEFAULT FALSE COMMENT '是否已完成（出场）',
  `created_at` DATETIME COMMENT '创建时间',
  `updated_at` DATETIME COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plate_num` (`plate_num`),
  KEY `idx_device_sn` (`device_sn`),
  KEY `idx_is_completed` (`is_completed`),
  KEY `idx_entry_time` (`entry_time`),
  KEY `idx_exit_time` (`exit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='停车记录表';

-- 创建索引用于查询性能优化
CREATE INDEX IF NOT EXISTS `idx_plate_num_completed` ON `parking_records` (`plate_num`, `is_completed`);
CREATE INDEX IF NOT EXISTS `idx_entry_exit_time` ON `parking_records` (`entry_time`, `exit_time`);
