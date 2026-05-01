-- ============================================
-- ETL系统 v6.0 升级 - 后端API和数据引擎增强
-- 版本: 6.0.0
-- 说明: 新增任务模板、调度预览、DAG快照、API文档增强等功能
-- ============================================

USE etl_system;

-- ============================================
-- 1. 任务模板表
-- ============================================
CREATE TABLE IF NOT EXISTS etl_task_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    category VARCHAR(50) COMMENT '模板分类: SYNC/TRANSFORM/WORKFLOW/API',
    description VARCHAR(500) COMMENT '模板描述',
    icon VARCHAR(100) COMMENT '模板图标',
    config JSON NOT NULL COMMENT '模板配置JSON(包含节点、连线、调度等完整配置)',
    tags JSON COMMENT '标签JSON数组',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统模板: 0-否, 1-是',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模板表';

-- ============================================
-- 2. DAG版本快照表
-- ============================================
CREATE TABLE IF NOT EXISTS etl_dag_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    version INT NOT NULL COMMENT '版本号',
    dag_config_id BIGINT COMMENT '对应的etl_dag_config记录ID',
    nodes_json MEDIUMTEXT COMMENT '完整节点配置JSON',
    edges_json MEDIUMTEXT COMMENT '完整连线配置JSON',
    viewport_json TEXT COMMENT '视口配置JSON',
    change_summary TEXT COMMENT '变更摘要',
    checksum VARCHAR(64) COMMENT '配置校验和(SHA256)',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_version (version),
    UNIQUE KEY uk_task_version (task_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DAG版本快照表';

-- ============================================
-- 3. 任务导入/导出记录表
-- ============================================
CREATE TABLE IF NOT EXISTS etl_task_transfer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_id VARCHAR(50) NOT NULL COMMENT '批次ID',
    direction VARCHAR(10) NOT NULL COMMENT '方向: IMPORT/EXPORT',
    task_id BIGINT COMMENT '关联任务ID(导出)/新创建任务ID(导入)',
    task_name VARCHAR(200) COMMENT '任务名称',
    config_json MEDIUMTEXT COMMENT '完整配置JSON',
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
    error_msg TEXT COMMENT '错误信息',
    created_by VARCHAR(50) COMMENT '操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务导入导出记录表';

-- ============================================
-- 4. 调度触发器配置表（扩展调度类型支持）
-- ============================================
CREATE TABLE IF NOT EXISTS etl_schedule_trigger_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    trigger_type VARCHAR(30) NOT NULL COMMENT '触发类型: CRON/API/EVENT/DEPENDENCY/MANUAL',
    trigger_name VARCHAR(100) COMMENT '触发器名称',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式(CRON类型)',
    api_url VARCHAR(500) COMMENT 'API回调地址(API类型)',
    api_method VARCHAR(10) DEFAULT 'POST' COMMENT 'API请求方法',
    api_headers JSON COMMENT 'API请求头',
    event_type VARCHAR(50) COMMENT '事件类型: TASK_COMPLETED/FILE_ARRIVAL/MESSAGE_RECEIVED',
    event_filter JSON COMMENT '事件过滤条件',
    dependency_config JSON COMMENT '依赖配置(DEPENDENCY类型)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    priority INT DEFAULT 5 COMMENT '优先级(1-10)',
    retry_on_failure TINYINT DEFAULT 1 COMMENT '失败是否重试',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    retry_interval INT DEFAULT 60 COMMENT '重试间隔(秒)',
    timeout INT DEFAULT 3600 COMMENT '超时时间(秒)',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_id (task_id),
    INDEX idx_trigger_type (trigger_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度触发器配置表';

-- ============================================
-- 5. 调度执行日志表（增强版）
-- ============================================
CREATE TABLE IF NOT EXISTS etl_schedule_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    trigger_type VARCHAR(30) COMMENT '触发类型',
    trigger_name VARCHAR(100) COMMENT '触发器名称',
    scheduled_time DATETIME COMMENT '计划执行时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    duration BIGINT COMMENT '执行时长(毫秒)',
    status VARCHAR(20) NOT NULL COMMENT '状态: FIRED/SCHEDULED/MISFIRED/COMPLETED/FAILED',
    result VARCHAR(20) COMMENT '执行结果: SUCCESS/FAILED/SKIPPED',
    error_msg TEXT COMMENT '错误信息',
    next_fire_time DATETIME COMMENT '下次执行时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_scheduled_time (scheduled_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度执行日志表';

-- ============================================
-- 6. API文档表
-- ============================================
CREATE TABLE IF NOT EXISTS etl_api_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    api_id BIGINT NOT NULL COMMENT '关联API服务ID',
    doc_title VARCHAR(200) COMMENT '文档标题',
    doc_content MEDIUMTEXT COMMENT '文档内容(Markdown)',
    request_example JSON COMMENT '请求示例',
    response_example JSON COMMENT '响应示例',
    error_codes JSON COMMENT '错误码说明',
    version INT DEFAULT 1 COMMENT '文档版本',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_api_id (api_id),
    INDEX idx_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API文档表';

-- ============================================
-- 7. 扩展etl_api_call_log表字段
-- ============================================
ALTER TABLE etl_api_call_log
    ADD COLUMN IF NOT EXISTS api_path VARCHAR(200) COMMENT 'API路径',
    ADD COLUMN IF NOT EXISTS response_body LONGTEXT COMMENT '响应体内容',
    ADD COLUMN IF NOT EXISTS user_id VARCHAR(50) COMMENT '调用用户ID',
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500) COMMENT 'User-Agent',
    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(100) COMMENT '链路追踪ID';

-- 创建API调用统计物化表索引
ALTER TABLE etl_api_call_log
    ADD INDEX IF NOT EXISTS idx_api_id_time (api_id, request_time),
    ADD INDEX IF NOT EXISTS idx_status_request (status, request_time);

-- ============================================
-- 8. 发布审批流配置表
-- ============================================
CREATE TABLE IF NOT EXISTS etl_publish_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    publish_id BIGINT NOT NULL COMMENT '发布记录ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    approver VARCHAR(50) NOT NULL COMMENT '审批人',
    approval_level INT DEFAULT 1 COMMENT '审批级别',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审批状态: PENDING/APPROVED/REJECTED',
    comment VARCHAR(500) COMMENT '审批意见',
    approved_at DATETIME COMMENT '审批时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_publish_id (publish_id),
    INDEX idx_task_id (task_id),
    INDEX idx_approver (approver)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布审批记录表';

-- ============================================
-- 9. 初始化系统任务模板数据
-- ============================================
INSERT INTO etl_task_template (name, category, description, config, tags, is_system, created_by) VALUES
('MySQL同步模板', 'SYNC', 'MySQL全量+增量同步基础模板', '{"nodes":[{"id":"source_1","type":"SOURCE","name":"源数据源","config":{"datasourceType":"MYSQL","tableMode":"single"}},{"id":"sync_1","type":"SYNC","name":"数据同步","config":{"syncMode":"FULL","batchSize":1000}},{"id":"target_1","type":"TARGET","name":"目标数据源","config":{"datasourceType":"MYSQL","writeMode":"OVERWRITE"}}],"edges":[{"id":"e1","source":"source_1","target":"sync_1"},{"id":"e2","source":"sync_1","target":"target_1"}],"schedule":{"type":"CRON","cron":"0 0 2 * * ?"}}', '["mysql","同步","基础"]', 1, 'system'),
('API数据服务模板', 'API', '将SQL查询发布为REST API', '{"nodes":[{"id":"api_1","type":"API","name":"API入口","config":{"method":"GET","authType":"TOKEN","rateLimit":100,"timeout":30}},{"id":"sql_1","type":"SQL","name":"SQL查询","config":{"datasourceType":"MYSQL","sqlTemplate":"SELECT * FROM table WHERE 1=1"}}],"edges":[{"id":"e1","source":"api_1","target":"sql_1"}]}', '["api","服务","查询"]', 1, 'system'),
('数据转换管道模板', 'TRANSFORM', '数据清洗+转换+聚合流水线', '{"nodes":[{"id":"input_1","type":"INPUT","name":"数据输入"},{"id":"filter_1","type":"FILTER","name":"数据过滤","config":{"condition":"status = 1"}},{"id":"transform_1","type":"TRANSFORM","name":"字段转换","config":{"mappings":[{"from":"old_name","to":"new_name"}]}},{"id":"output_1","type":"OUTPUT","name":"数据输出"}],"edges":[{"id":"e1","source":"input_1","target":"filter_1"},{"id":"e2","source":"filter_1","target":"transform_1"},{"id":"e3","source":"transform_1","target":"output_1"}]}', '["转换","管道","清洗"]', 1, 'system'),
('多表关联工作流模板', 'WORKFLOW', '多个表的同步和关联处理', '{"nodes":[{"id":"table_a","type":"SYNC","name":"同步表A","config":{"tableName":"table_a","syncMode":"INCREMENTAL"}},{"id":"table_b","type":"SYNC","name":"同步表B","config":{"tableName":"table_b","syncMode":"INCREMENTAL"}},{"id":"join_1","type":"JOIN","name":"关联表A和B","config":{"joinType":"LEFT","joinKey":"id"}},{"id":"output_1","type":"OUTPUT","name":"输出结果"}],"edges":[{"id":"e1","source":"table_a","target":"join_1"},{"id":"e2","source":"table_b","target":"join_1"},{"id":"e3","source":"join_1","target":"output_1"}]}', '["工作流","关联","多表"]', 1, 'system');

-- ============================================
-- 完成
-- ============================================
SELECT 'v6.0 升级完成 - 后端API和数据引擎增强' AS message;
