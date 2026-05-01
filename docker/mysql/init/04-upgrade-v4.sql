-- ============================================
-- V4 全面升级 - 新增表结构
-- ============================================

-- 转换流水线定义表
CREATE TABLE IF NOT EXISTS `etl_transform_pipeline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT '流水线名称',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `source_datasource_id` BIGINT DEFAULT NULL COMMENT '源数据源ID',
  `target_datasource_id` BIGINT DEFAULT NULL COMMENT '目标数据源ID',
  `source_table` VARCHAR(128) DEFAULT NULL COMMENT '源表名',
  `target_table` VARCHAR(128) DEFAULT NULL COMMENT '目标表名',
  `pipeline_config` JSON DEFAULT NULL COMMENT '流水线配置(节点和边的JSON)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换流水线定义';

-- 转换步骤配置表
CREATE TABLE IF NOT EXISTS `etl_transform_step` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pipeline_id` BIGINT NOT NULL COMMENT '流水线ID',
  `step_code` VARCHAR(64) NOT NULL COMMENT '步骤编码(唯一标识)',
  `step_name` VARCHAR(128) NOT NULL COMMENT '步骤名称',
  `rule_type` VARCHAR(64) NOT NULL COMMENT '规则类型',
  `rule_config` JSON DEFAULT NULL COMMENT '规则参数配置(JSON)',
  `step_order` INT NOT NULL DEFAULT 0 COMMENT '步骤顺序',
  `next_step_code` VARCHAR(64) DEFAULT NULL COMMENT '下一节点编码',
  `error_strategy` VARCHAR(32) NOT NULL DEFAULT 'SKIP' COMMENT '错误策略: SKIP/ABORT/ALERT',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_id` (`pipeline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换步骤配置';

-- DAG调度依赖表
CREATE TABLE IF NOT EXISTS `etl_schedule_dag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dag_name` VARCHAR(128) NOT NULL COMMENT 'DAG名称',
  `description` VARCHAR(512) DEFAULT NULL,
  `dag_config` JSON NOT NULL COMMENT 'DAG配置(节点和边)',
  `cron_expression` VARCHAR(64) DEFAULT NULL COMMENT '调度Cron表达式',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DAG调度依赖';

-- 数据质量规则表
CREATE TABLE IF NOT EXISTS `etl_quality_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` BIGINT DEFAULT NULL COMMENT '关联任务ID',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_dimension` VARCHAR(32) NOT NULL COMMENT '维度: COMPLETENESS/ACCURACY/CONSISTENCY/TIMELINESS',
  `rule_type` VARCHAR(64) NOT NULL COMMENT '规则类型',
  `rule_config` JSON NOT NULL COMMENT '规则配置',
  `severity` VARCHAR(16) NOT NULL DEFAULT 'WARN' COMMENT '严重度: INFO/WARN/ERROR/CRITICAL',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量规则';

-- 质量校验报告表
CREATE TABLE IF NOT EXISTS `etl_quality_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `execution_id` BIGINT DEFAULT NULL COMMENT '执行记录ID',
  `rule_id` BIGINT DEFAULT NULL COMMENT '规则ID',
  `quality_score` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '质量评分(0-100)',
  `total_count` BIGINT NOT NULL DEFAULT 0 COMMENT '总校验数',
  `pass_count` BIGINT NOT NULL DEFAULT 0 COMMENT '通过数',
  `fail_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败数',
  `fail_samples` JSON DEFAULT NULL COMMENT '失败样本',
  `report_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_report_time` (`report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量校验报告';

-- 操作审计日志表
CREATE TABLE IF NOT EXISTS `etl_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `operation` VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/EXECUTE',
  `module` VARCHAR(64) NOT NULL COMMENT '模块',
  `target_id` BIGINT DEFAULT NULL COMMENT '操作对象ID',
  `target_name` VARCHAR(128) DEFAULT NULL COMMENT '操作对象名称',
  `detail` TEXT DEFAULT NULL COMMENT '操作详情',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- 同步详细日志表
CREATE TABLE IF NOT EXISTS `etl_sync_log_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `log_id` BIGINT NOT NULL COMMENT '关联同步日志ID',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `step_code` VARCHAR(64) DEFAULT NULL COMMENT '步骤编码',
  `row_index` INT DEFAULT NULL COMMENT '行号',
  `source_value` JSON DEFAULT NULL COMMENT '转换前值',
  `target_value` JSON DEFAULT NULL COMMENT '转换后值',
  `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
  `is_error` TINYINT NOT NULL DEFAULT 0 COMMENT '是否异常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_id` (`log_id`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步详细日志';

-- 任务配置快照表
CREATE TABLE IF NOT EXISTS `etl_task_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `execution_id` BIGINT DEFAULT NULL COMMENT '执行记录ID',
  `snapshot_config` JSON NOT NULL COMMENT '任务配置快照',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务配置快照';

-- 修改告警规则表新增字段
-- 注意：alert_type字段已存在，无需重复添加
ALTER TABLE `etl_alert_rule` ADD COLUMN `cooldown_minutes` INT NOT NULL DEFAULT 5 COMMENT '告警收敛冷却时间(分钟)' AFTER `enabled`;
ALTER TABLE `etl_alert_rule` ADD COLUMN `escalation_config` JSON DEFAULT NULL COMMENT '升级策略配置' AFTER `cooldown_minutes`;
ALTER TABLE `etl_alert_rule` ADD COLUMN `notification_channels` JSON DEFAULT NULL COMMENT '通知渠道配置' AFTER `escalation_config`;
