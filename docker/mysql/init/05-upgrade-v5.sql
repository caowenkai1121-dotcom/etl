-- ============================================
-- V5 升级 - 对标FineDataLink功能
-- 包含：可视化任务编排、数据服务API发布、任务版本发布管理
-- ============================================

-- ============================================
-- 阶段一：可视化任务编排器
-- ============================================

-- 文件夹表（树形结构管理）
CREATE TABLE IF NOT EXISTS `etl_folder` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '文件夹名称',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `folder_type` VARCHAR(20) NOT NULL DEFAULT 'WORKFLOW' COMMENT '文件夹类型: WORKFLOW/API/TASK',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件夹管理';

-- 工作流定义表
CREATE TABLE IF NOT EXISTS `etl_task_workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT '工作流名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `folder_id` BIGINT DEFAULT NULL COMMENT '所属文件夹ID',
  `workflow_json` JSON NOT NULL COMMENT '流程定义JSON(节点和边)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED',
  `publish_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发布状态: PENDING/PUBLISHED/UPDATED',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `cron_expression` VARCHAR(64) DEFAULT NULL COMMENT '调度Cron表达式',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义';

-- 工作流执行记录表
CREATE TABLE IF NOT EXISTS `etl_workflow_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_id` BIGINT NOT NULL COMMENT '工作流ID',
  `execution_no` VARCHAR(64) NOT NULL COMMENT '执行编号',
  `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发类型: MANUAL/SCHEDULE',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration` BIGINT DEFAULT NULL COMMENT '执行时长(毫秒)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/SUCCESS/FAILED/STOPPED',
  `current_node_id` VARCHAR(64) DEFAULT NULL COMMENT '当前执行节点ID',
  `total_nodes` INT NOT NULL DEFAULT 0 COMMENT '总节点数',
  `completed_nodes` INT NOT NULL DEFAULT 0 COMMENT '已完成节点数',
  `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_execution_no` (`execution_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流执行记录';

-- 工作流节点执行记录表
CREATE TABLE IF NOT EXISTS `etl_workflow_node_exec` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_execution_id` BIGINT NOT NULL COMMENT '工作流执行ID',
  `node_id` VARCHAR(64) NOT NULL COMMENT '节点ID',
  `node_name` VARCHAR(128) DEFAULT NULL COMMENT '节点名称',
  `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型: SYNC/TRANSFORM/SCRIPT/CONDITION/LOOP',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration` BIGINT DEFAULT NULL COMMENT '执行时长(毫秒)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED',
  `total_rows` BIGINT DEFAULT 0 COMMENT '总行数',
  `success_rows` BIGINT DEFAULT 0 COMMENT '成功行数',
  `failed_rows` BIGINT DEFAULT 0 COMMENT '失败行数',
  `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_execution_id` (`workflow_execution_id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点执行记录';

-- ============================================
-- 阶段二：数据服务API发布
-- ============================================

-- API服务定义表
CREATE TABLE IF NOT EXISTS `etl_api_service` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT 'API名称',
  `path` VARCHAR(200) NOT NULL COMMENT 'API路径',
  `method` VARCHAR(10) NOT NULL DEFAULT 'GET' COMMENT '请求方法: GET/POST',
  `datasource_id` BIGINT NOT NULL COMMENT '关联数据源ID',
  `sql_template` TEXT NOT NULL COMMENT 'SQL模板',
  `params_config` JSON DEFAULT NULL COMMENT '参数配置JSON',
  `auth_type` VARCHAR(20) NOT NULL DEFAULT 'TOKEN' COMMENT '认证方式: TOKEN/IP/SIGN/NONE',
  `auth_config` JSON DEFAULT NULL COMMENT '认证配置',
  `rate_limit` INT DEFAULT 100 COMMENT '限流(次/分钟)',
  `timeout` INT DEFAULT 30 COMMENT '超时时间(秒)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '状态: ONLINE/OFFLINE',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `folder_id` BIGINT DEFAULT NULL COMMENT '所属文件夹ID',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path_method` (`path`, `method`),
  KEY `idx_datasource_id` (`datasource_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API服务定义';

-- API调用日志表
CREATE TABLE IF NOT EXISTS `etl_api_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `api_id` BIGINT NOT NULL COMMENT 'API服务ID',
  `api_name` VARCHAR(128) DEFAULT NULL COMMENT 'API名称',
  `request_time` DATETIME NOT NULL COMMENT '请求时间',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_code` INT DEFAULT NULL COMMENT '响应状态码',
  `response_time` INT DEFAULT NULL COMMENT '响应时间(毫秒)',
  `response_rows` INT DEFAULT NULL COMMENT '返回行数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
  `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_api_id` (`api_id`),
  KEY `idx_request_time` (`request_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API调用日志';

-- API访问Token表
CREATE TABLE IF NOT EXISTS `etl_api_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token` VARCHAR(128) NOT NULL COMMENT 'Token值',
  `name` VARCHAR(128) NOT NULL COMMENT 'Token名称',
  `app_id` VARCHAR(64) DEFAULT NULL COMMENT '应用ID',
  `allowed_ips` VARCHAR(500) DEFAULT NULL COMMENT '允许IP列表(逗号分隔)',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问Token';

-- ============================================
-- 阶段四：任务版本与发布管理
-- ============================================

-- 任务发布记录表
CREATE TABLE IF NOT EXISTS `etl_task_publish` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `task_type` VARCHAR(20) NOT NULL DEFAULT 'WORKFLOW' COMMENT '任务类型: WORKFLOW/TASK/API',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `publish_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发布状态: PENDING/PUBLISHED/UPDATED',
  `published_by` VARCHAR(64) DEFAULT NULL COMMENT '发布人',
  `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
  `change_log` TEXT DEFAULT NULL COMMENT '变更说明',
  `snapshot_config` JSON DEFAULT NULL COMMENT '配置快照',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_publish_status` (`publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务发布记录';

-- 初始化根文件夹
INSERT INTO `etl_folder` (`name`, `parent_id`, `sort_order`, `folder_type`, `create_by`) VALUES
('全部工作流', 0, 0, 'WORKFLOW', 'system'),
('标准接口', 0, 0, 'API', 'system');

-- ============================================
-- 扩展现有表
-- ============================================

-- 扩展同步任务表，增加发布状态字段
ALTER TABLE `etl_sync_task` ADD COLUMN IF NOT EXISTS `folder_id` BIGINT DEFAULT NULL COMMENT '所属文件夹ID' AFTER `description`;
ALTER TABLE `etl_sync_task` ADD COLUMN IF NOT EXISTS `publish_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '发布状态' AFTER `status`;
ALTER TABLE `etl_sync_task` ADD COLUMN IF NOT EXISTS `version` INT DEFAULT 1 COMMENT '版本号' AFTER `publish_status`;
