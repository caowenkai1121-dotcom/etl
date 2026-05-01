-- ============================================
-- ETL数据同步调度系统 - 数据库表结构
-- 数据库: MySQL 8.0
-- 版本: 3.0.0 (Debezium CDC)
-- ============================================

CREATE DATABASE IF NOT EXISTS etl_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE etl_system;

-- ============================================
-- 1. 数据源配置表
-- ============================================
DROP TABLE IF EXISTS etl_datasource;
CREATE TABLE etl_datasource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    type VARCHAR(20) NOT NULL COMMENT '数据源类型: MYSQL/POSTGRESQL/DORIS/ORACLE/SQLSERVER/MONGODB/ELASTICSEARCH/CLICKHOUSE/HIVE/REDIS/KAFKA',
    host VARCHAR(100) NOT NULL COMMENT '主机地址',
    port INT NOT NULL COMMENT '端口号',
    database_name VARCHAR(100) NOT NULL COMMENT '数据库名称',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(500) NOT NULL COMMENT '密码(加密存储)',
    charset VARCHAR(20) DEFAULT 'utf8mb4' COMMENT '字符集',
    extra_config JSON COMMENT '扩展配置(JSON格式)',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    connection_test TINYINT DEFAULT 0 COMMENT '连接测试: 0-未测试, 1-成功, 2-失败',
    last_test_time DATETIME COMMENT '最后测试时间',
    remark VARCHAR(500) COMMENT '备注',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- ============================================
-- 2. 同步任务表
-- ============================================
DROP TABLE IF EXISTS etl_sync_task;
CREATE TABLE etl_sync_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '任务名称',
    description VARCHAR(500) COMMENT '任务描述',
    source_ds_id BIGINT NOT NULL COMMENT '源数据源ID',
    target_ds_id BIGINT NOT NULL COMMENT '目标数据源ID',
    sync_mode VARCHAR(20) NOT NULL COMMENT '同步模式: FULL/INCREMENTAL/CDC',
    sync_scope VARCHAR(20) NOT NULL COMMENT '同步范围: SINGLE_TABLE/MULTI_TABLE/FULL_DATABASE',
    table_config JSON NOT NULL COMMENT '表配置(JSON)',
    field_mapping JSON COMMENT '字段映射配置(JSON)',
    incremental_field VARCHAR(100) COMMENT '增量同步字段',
    incremental_value VARCHAR(200) COMMENT '增量同步起始值',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    sync_strategy VARCHAR(20) DEFAULT 'OVERWRITE' COMMENT '同步策略: OVERWRITE/APPEND/UPDATE',
    batch_size INT DEFAULT 1000 COMMENT '批量处理大小',
    parallel_threads INT DEFAULT 1 COMMENT '并行线程数',
    retry_times INT DEFAULT 3 COMMENT '失败重试次数',
    retry_interval INT DEFAULT 60 COMMENT '重试间隔(秒)',
    status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态: CREATED/RUNNING/PAUSED/STOPPED',
    last_sync_time DATETIME COMMENT '最后同步时间',
    next_sync_time DATETIME COMMENT '下次同步时间',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_sync_mode (sync_mode),
    INDEX idx_status (status),
    INDEX idx_source_ds (source_ds_id),
    INDEX idx_target_ds (target_ds_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务表';

-- ============================================
-- 3. 任务执行记录表
-- ============================================
DROP TABLE IF EXISTS etl_task_execution;
CREATE TABLE etl_task_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_no VARCHAR(50) NOT NULL COMMENT '执行编号',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发类型: MANUAL/SCHEDULED/CDC',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration BIGINT COMMENT '执行时长(毫秒)',
    status VARCHAR(20) NOT NULL COMMENT '状态: RUNNING/SUCCESS/FAILED/CANCELLED/SKIPPED',
    total_rows BIGINT DEFAULT 0 COMMENT '总行数',
    success_rows BIGINT DEFAULT 0 COMMENT '成功行数',
    failed_rows BIGINT DEFAULT 0 COMMENT '失败行数',
    skip_rows BIGINT DEFAULT 0 COMMENT '跳过行数',
    error_message TEXT COMMENT '错误信息',
    checkpoint VARCHAR(500) COMMENT '断点信息(JSON)',
    progress DECIMAL(5,2) DEFAULT 0 COMMENT '执行进度(%)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_execution_no (execution_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行记录表';

-- ============================================
-- 3.5. 任务依赖表
-- ============================================
DROP TABLE IF EXISTS etl_task_dependency;
CREATE TABLE etl_task_dependency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '当前任务ID',
    depends_on_task_id BIGINT NOT NULL COMMENT '依赖的任务ID',
    dependency_type VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '依赖类型: FINISH/SUCCESS',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_depends_on_task_id (depends_on_task_id),
    UNIQUE KEY uk_task_dependency (task_id, depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖关系表';

-- ============================================
-- 4. CDC同步位点表
-- ============================================
DROP TABLE IF EXISTS etl_cdc_position;
CREATE TABLE etl_cdc_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    source_ds_id BIGINT NOT NULL COMMENT '源数据源ID',
    table_name VARCHAR(200) NOT NULL COMMENT '表名',
    position_type VARCHAR(20) NOT NULL COMMENT '位点类型: BINLOG/WAL/KAFKA_OFFSET',
    position_value VARCHAR(500) NOT NULL COMMENT '位点值',
    binlog_file VARCHAR(100) COMMENT 'Binlog文件名(MySQL)',
    binlog_position BIGINT COMMENT 'Binlog位置(MySQL)',
    gtid VARCHAR(200) COMMENT 'GTID(MySQL)',
    lsn VARCHAR(200) COMMENT 'LSN(PostgreSQL)',
    extra TEXT COMMENT '扩展信息(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_table (task_id, table_name),
    INDEX idx_source_ds (source_ds_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDC同步位点表';

-- ============================================
-- 5. 同步日志表
-- ============================================
DROP TABLE IF EXISTS etl_sync_log;
CREATE TABLE etl_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_id BIGINT COMMENT '执行记录ID',
    log_level VARCHAR(20) NOT NULL COMMENT '日志级别: DEBUG/INFO/WARN/ERROR',
    log_type VARCHAR(50) COMMENT '日志类型: SYNC/MAPPING/ERROR/STATS',
    table_name VARCHAR(200) COMMENT '相关表名',
    message TEXT NOT NULL COMMENT '日志内容',
    stack_trace TEXT COMMENT '异常堆栈',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_execution_id (execution_id),
    INDEX idx_log_level (log_level),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步日志表';

-- ============================================
-- 6. 告警规则表
-- ============================================
DROP TABLE IF EXISTS etl_alert_rule;
CREATE TABLE etl_alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型',
    description VARCHAR(500) COMMENT '规则描述',
    condition_expr VARCHAR(1000) NOT NULL COMMENT '触发条件(JSON)',
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING' COMMENT '告警级别: INFO/WARNING/ERROR/CRITICAL',
    channels JSON NOT NULL COMMENT '通知渠道(JSON)',
    recipients JSON COMMENT '通知接收人(JSON)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    silence_minutes INT DEFAULT 30 COMMENT '静默时间(分钟)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_alert_type (alert_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- ============================================
-- 7. 告警记录表
-- ============================================
DROP TABLE IF EXISTS etl_alert_record;
CREATE TABLE etl_alert_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_id BIGINT COMMENT '规则ID',
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型',
    severity VARCHAR(20) NOT NULL COMMENT '告警级别',
    title VARCHAR(200) NOT NULL COMMENT '告警标题',
    content TEXT NOT NULL COMMENT '告警内容',
    source VARCHAR(100) COMMENT '告警来源',
    target_id BIGINT COMMENT '关联对象ID',
    target_name VARCHAR(200) COMMENT '关联对象名称',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/SENT/RESOLVED/IGNORED',
    sent_at DATETIME COMMENT '发送时间',
    resolved_at DATETIME COMMENT '解决时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ============================================
-- 8. CDC配置表 (Debezium)
-- ============================================
DROP TABLE IF EXISTS etl_cdc_config;
CREATE TABLE etl_cdc_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    datasource_id BIGINT NOT NULL COMMENT '关联数据源ID',

    -- Debezium连接器配置
    connector_name VARCHAR(100) NOT NULL COMMENT '连接器名称',
    connector_type VARCHAR(20) DEFAULT 'mysql' COMMENT '连接器类型(mysql/postgresql)',
    server_name VARCHAR(100) COMMENT '服务器名称(Topic前缀)',
    database_host VARCHAR(100) COMMENT '数据库主机',
    database_port INT COMMENT '数据库端口',
    kafka_topic_prefix VARCHAR(100) COMMENT 'Kafka Topic前缀',

    -- Canal兼容字段(保留用于迁移)
    canal_instance VARCHAR(100) COMMENT 'Canal实例名',
    canal_server_host VARCHAR(100) DEFAULT 'canal-server' COMMENT 'Canal服务器地址',
    canal_server_port INT DEFAULT 11111 COMMENT 'Canal服务器端口',
    master_address VARCHAR(200) COMMENT 'MySQL主库地址',
    db_username VARCHAR(100) COMMENT '数据库用户名',
    db_password VARCHAR(500) COMMENT '数据库密码(加密存储)',

    -- 同步过滤配置
    filter_regex VARCHAR(500) DEFAULT '.*\\..*' COMMENT '表过滤正则',
    filter_black_regex VARCHAR(500) COMMENT '黑名单过滤正则',
    kafka_topic VARCHAR(100) COMMENT 'Kafka Topic',

    -- 高级配置
    extra_config JSON COMMENT '扩展配置',

    -- 状态
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    sync_status VARCHAR(20) DEFAULT 'STOPPED' COMMENT '同步状态: STOPPED/RUNNING/ERROR',
    last_sync_time DATETIME COMMENT '最后同步时间',
    error_message TEXT COMMENT '错误信息',

    -- 元数据
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',

    UNIQUE KEY uk_connector_name (connector_name),
    INDEX idx_datasource (datasource_id),
    INDEX idx_status (status),
    INDEX idx_sync_status (sync_status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDC配置表';

-- ============================================
-- 9. 系统配置表
-- ============================================
DROP TABLE IF EXISTS etl_system_config;
CREATE TABLE etl_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_group VARCHAR(50) NOT NULL COMMENT '配置分组',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'STRING' COMMENT '配置类型: STRING/INT/BOOLEAN/JSON',
    description VARCHAR(200) COMMENT '配置说明',
    is_editable TINYINT DEFAULT 1 COMMENT '是否可编辑',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config (config_group, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 10. Quartz调度器标准表 (MySQL)
-- ============================================
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT(13) NULL,
    PREV_FIRE_TIME BIGINT(13) NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT(13) NOT NULL,
    END_TIME BIGINT(13) NULL,
    CALENDAR_NAME VARCHAR(190) NULL,
    MISFIRE_INSTR SMALLINT(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    REPEAT_COUNT BIGINT(7) NOT NULL,
    REPEAT_INTERVAL BIGINT(12) NOT NULL,
    TIMES_TRIGGERED BIGINT(10) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    FIRED_TIME BIGINT(13) NOT NULL,
    SCHED_TIME BIGINT(13) NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(190) NULL,
    JOB_GROUP VARCHAR(190) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
    CHECKIN_INTERVAL BIGINT(13) NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化数据
-- ============================================

-- 系统配置
INSERT INTO etl_system_config (config_group, config_key, config_value, config_type, description) VALUES
('SYSTEM', 'batch_size', '1000', 'INT', '默认批量处理大小'),
('SYSTEM', 'parallel_threads', '4', 'INT', '默认并行线程数'),
('SYSTEM', 'retry_times', '3', 'INT', '默认重试次数'),
('SYSTEM', 'retry_interval', '60', 'INT', '默认重试间隔(秒)'),
('SYSTEM', 'log_retention_days', '30', 'INT', '日志保留天数'),
('SYSTEM', 'execution_retention_days', '90', 'INT', '执行记录保留天数'),
('CDC', 'kafka_bootstrap_servers', 'kafka:9092', 'STRING', 'Kafka服务器地址'),
('CDC', 'kafka_consumer_group', 'etl-sync-group', 'STRING', 'Kafka消费组'),
('CDC', 'debezium_connect_url', 'http://debezium-connect:8083', 'STRING', 'Debezium Connect地址'),
('ALERT', 'enable_email_alert', 'false', 'BOOLEAN', '启用邮件告警'),
('ALERT', 'dingtalk_webhook', '', 'STRING', '钉钉Webhook地址'),
('ALERT', 'dingtalk_secret', '', 'STRING', '钉钉签名密钥');

-- 默认告警规则
INSERT INTO etl_alert_rule (name, alert_type, description, condition_expr, severity, channels, recipients, enabled, silence_minutes) VALUES
('任务失败告警', 'TASK_FAILED', '任务执行失败时触发', '{"event":"task_failed"}', 'ERROR', '["EMAIL","DINGTALK"]', '[]', 1, 30),
('同步延迟告警', 'SYNC_DELAY', '同步延迟超过阈值时触发', '{"delay_minutes":30}', 'WARNING', '["EMAIL","DINGTALK"]', '[]', 1, 60),
('连接异常告警', 'CONNECTION_ERROR', '数据源连接异常时触发', '{"event":"connection_error"}', 'CRITICAL', '["EMAIL","DINGTALK"]', '[]', 1, 10),
('系统错误告警', 'SYSTEM_ERROR', '系统运行异常时触发', '{"event":"system_error"}', 'CRITICAL', '["EMAIL","DINGTALK"]', '[]', 1, 10);
