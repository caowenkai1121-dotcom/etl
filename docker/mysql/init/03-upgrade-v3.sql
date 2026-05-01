-- ============================================
-- ETL数据同步调度系统 - V3.0 升级脚本
-- 版本: 2.0.0 → 3.0.0
-- 新增: Pipeline流水线、ETL转换增强、日志体系升级
-- ============================================

USE etl_system;

-- ============================================
-- 1. 现有表新增字段
-- ============================================

-- etl_sync_log 新增字段（全链路追踪 + 阶段统计）
DROP PROCEDURE IF EXISTS etl_add_column_v3;
DELIMITER $$
CREATE PROCEDURE etl_add_column_v3(IN p_table VARCHAR(100), IN p_column VARCHAR(100), IN p_definition VARCHAR(500))
BEGIN
    DECLARE col_count INT;
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column;
    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL etl_add_column_v3('etl_sync_log', 'trace_id', 'VARCHAR(32) COMMENT ''全链路追踪ID'' AFTER execution_id');
CALL etl_add_column_v3('etl_sync_log', 'stage_name', 'VARCHAR(50) COMMENT ''Pipeline阶段名'' AFTER log_type');
CALL etl_add_column_v3('etl_sync_log', 'transform_rule_id', 'BIGINT COMMENT ''关联转换规则ID'' AFTER stage_name');
CALL etl_add_column_v3('etl_sync_log', 'record_count', 'INT DEFAULT 0 COMMENT ''影响记录数'' AFTER message');
CALL etl_add_column_v3('etl_sync_log', 'elapsed_ms', 'BIGINT DEFAULT 0 COMMENT ''阶段耗时(毫秒)'' AFTER record_count');
CALL etl_add_column_v3('etl_sync_log', 'detail_json', 'JSON COMMENT ''转换详情(JSON)'' AFTER elapsed_ms');

-- etl_sync_task 新增字段（Pipeline相关）
CALL etl_add_column_v3('etl_sync_task', 'pipeline_config', 'JSON COMMENT ''Pipeline流水线配置'' AFTER field_mapping');
CALL etl_add_column_v3('etl_sync_task', 'transform_pipeline_id', 'BIGINT COMMENT ''关联转换流水线ID'' AFTER pipeline_config');
CALL etl_add_column_v3('etl_sync_task', 'quality_rule_ids', 'JSON COMMENT ''关联数据质量规则ID列表'' AFTER transform_pipeline_id');
CALL etl_add_column_v3('etl_sync_task', 'shard_config', 'JSON COMMENT ''分片配置(JSON)'' AFTER transform_pipeline_id');

-- etl_task_execution 新增字段（分片 + 校验信息）
CALL etl_add_column_v3('etl_task_execution', 'trace_id', 'VARCHAR(32) COMMENT ''全链路追踪ID'' AFTER execution_no');
CALL etl_add_column_v3('etl_task_execution', 'shard_total', 'INT DEFAULT 1 COMMENT ''总分片数'' AFTER skip_rows');
CALL etl_add_column_v3('etl_task_execution', 'shard_success', 'INT DEFAULT 0 COMMENT ''成功分片数'' AFTER shard_total');
CALL etl_add_column_v3('etl_task_execution', 'validation_status', 'VARCHAR(20) COMMENT ''数据校验状态: PENDING/PASSED/FAILED/SKIPPED'' AFTER shard_success');

DROP PROCEDURE IF EXISTS etl_add_column_v3;

-- ============================================
-- 2. 新增表
-- ============================================

-- 2.1 转换流水线定义表
CREATE TABLE IF NOT EXISTS etl_transform_pipeline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '流水线名称',
    description VARCHAR(500) COMMENT '流水线描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换流水线定义表';

-- 2.2 转换阶段定义表
CREATE TABLE IF NOT EXISTS etl_transform_stage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    pipeline_id BIGINT NOT NULL COMMENT '所属流水线ID',
    stage_name VARCHAR(100) NOT NULL COMMENT '阶段名称',
    stage_order INT NOT NULL COMMENT '阶段顺序',
    stage_type VARCHAR(30) NOT NULL COMMENT '阶段类型: CLEAN/TRANSFORM/ENRICH/VALIDATE/ROUTE',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    stop_on_error TINYINT DEFAULT 0 COMMENT '遇错是否停止',
    description VARCHAR(500) COMMENT '阶段描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_pipeline_id (pipeline_id),
    INDEX idx_stage_order (stage_order),
    FOREIGN KEY (pipeline_id) REFERENCES etl_transform_pipeline(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换阶段定义表';

-- 2.3 转换规则详情表
CREATE TABLE IF NOT EXISTS etl_transform_rule_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    stage_id BIGINT NOT NULL COMMENT '所属阶段ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(30) NOT NULL COMMENT '规则类型: VALUE_MAP/FORMAT_CONVERT/EXPRESSION/FIELD_CONCAT/DEFAULT_VALUE/TRIM/UPPER_CASE/LOWER_CASE/REGEX_REPLACE/FIELD_RENAME/FIELD_ADD/FIELD_REMOVE/FIELD_SPLIT/FILTER/SCRIPT/DICT_LOOKUP/API_CALL',
    sort_order INT DEFAULT 0 COMMENT '规则顺序',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    stop_on_error TINYINT DEFAULT 0 COMMENT '遇错是否停止',

    -- 源字段配置
    source_field VARCHAR(200) COMMENT '源字段名',
    target_field VARCHAR(200) COMMENT '目标字段名',

    -- 转换参数(JSON)
    rule_config JSON COMMENT '规则参数(JSON, 根据type不同包含不同参数)',

    -- 过滤条件
    filter_expression VARCHAR(500) COMMENT '过滤表达式',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_stage_id (stage_id),
    INDEX idx_rule_type (rule_type),
    FOREIGN KEY (stage_id) REFERENCES etl_transform_stage(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换规则详情表';

-- 2.4 转换日志表
CREATE TABLE IF NOT EXISTS etl_transform_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_id BIGINT NOT NULL COMMENT '执行ID',
    trace_id VARCHAR(32) COMMENT '全链路追踪ID',
    stage_id BIGINT COMMENT '转换阶段ID',
    rule_id BIGINT COMMENT '转换规则ID',
    rule_name VARCHAR(100) COMMENT '规则名称',
    rule_type VARCHAR(30) COMMENT '规则类型',
    table_name VARCHAR(200) COMMENT '表名',
    source_value TEXT COMMENT '转换前值(JSON)',
    target_value TEXT COMMENT '转换后值(JSON)',
    status VARCHAR(10) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED/SKIPPED',
    error_message TEXT COMMENT '错误信息',
    elapsed_ms BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
    record_count INT DEFAULT 0 COMMENT '影响记录数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_execution (execution_id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_rule_type (rule_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换日志表';

-- 2.5 数据质量日志表
CREATE TABLE IF NOT EXISTS etl_quality_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_id BIGINT NOT NULL COMMENT '执行ID',
    trace_id VARCHAR(32) COMMENT '全链路追踪ID',
    rule_name VARCHAR(100) COMMENT '质量规则名称',
    rule_type VARCHAR(30) COMMENT '规则类型: NOT_NULL/UNIQUE/DATA_TYPE/RANGE_VALUE/REFERENCE/CUSTOM',
    table_name VARCHAR(200) COMMENT '表名',
    field_name VARCHAR(200) COMMENT '字段名',
    field_value TEXT COMMENT '字段值',
    expected_value TEXT COMMENT '期望值',
    actual_value TEXT COMMENT '实际值',
    severity VARCHAR(10) DEFAULT 'WARNING' COMMENT '严重级别: INFO/WARNING/ERROR',
    status VARCHAR(10) DEFAULT 'OPEN' COMMENT '状态: OPEN/IGNORED/FIXED',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_execution (execution_id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_severity (severity),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量日志表';

-- 2.6 操作审计日志表
CREATE TABLE IF NOT EXISTS etl_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operator VARCHAR(50) COMMENT '操作人',
    operation_type VARCHAR(30) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/EXECUTE/STOP/LOGIN/LOGOUT',
    target_type VARCHAR(30) COMMENT '操作对象类型: DATASOURCE/TASK/CDC_CONFIG/TRANSFORM/ALERT/CONFIG',
    target_id BIGINT COMMENT '操作对象ID',
    target_name VARCHAR(200) COMMENT '操作对象名称',
    detail JSON COMMENT '操作详情(JSON)',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    status VARCHAR(10) DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_operator (operator),
    INDEX idx_operation_type (operation_type),
    INDEX idx_target_type (target_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- 2.7 数据校验任务表
CREATE TABLE IF NOT EXISTS etl_data_validation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_id BIGINT COMMENT '执行ID',
    source_table VARCHAR(200) NOT NULL COMMENT '源表名',
    target_table VARCHAR(200) NOT NULL COMMENT '目标表名',
    source_ds_id BIGINT NOT NULL COMMENT '源数据源ID',
    target_ds_id BIGINT NOT NULL COMMENT '目标数据源ID',
    validation_type VARCHAR(20) NOT NULL COMMENT '校验类型: COUNT/SAMPLE/FULL/CHECKSUM',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/PASSED/FAILED/ERROR',

    -- 行数校验
    source_count BIGINT DEFAULT 0 COMMENT '源表行数',
    target_count BIGINT DEFAULT 0 COMMENT '目标表行数',
    count_match TINYINT DEFAULT 0 COMMENT '行数是否匹配',

    -- 抽样校验
    sample_rate DECIMAL(5,4) DEFAULT 0.1000 COMMENT '抽样率',
    sample_size INT DEFAULT 0 COMMENT '抽样数量',
    match_count INT DEFAULT 0 COMMENT '匹配数量',
    mismatch_count INT DEFAULT 0 COMMENT '不匹配数量',

    -- 主键校验
    missing_key_count INT DEFAULT 0 COMMENT '缺失主键数',
    extra_key_count INT DEFAULT 0 COMMENT '多余主键数',

    -- 校验结果
    passed TINYINT DEFAULT 0 COMMENT '是否通过',
    summary TEXT COMMENT '校验摘要',
    error_message TEXT COMMENT '错误信息',
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task (task_id),
    INDEX idx_execution (execution_id),
    INDEX idx_status (status),
    INDEX idx_validation_type (validation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据校验任务表';

-- 2.8 校验明细表
CREATE TABLE IF NOT EXISTS etl_data_validation_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    validation_id BIGINT NOT NULL COMMENT '校验任务ID',
    row_key VARCHAR(500) COMMENT '主键值',
    field_name VARCHAR(200) COMMENT '字段名',
    source_value TEXT COMMENT '源值',
    target_value TEXT COMMENT '目标值',
    difference_type VARCHAR(20) COMMENT '差异类型: MISSING/EXTRA/MISMATCH',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_validation (validation_id),
    FOREIGN KEY (validation_id) REFERENCES etl_data_validation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据校验明细表';

-- 2.9 字典映射表
CREATE TABLE IF NOT EXISTS etl_dict_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_code VARCHAR(50) NOT NULL COMMENT '字典编码',
    source_value VARCHAR(200) NOT NULL COMMENT '源值',
    target_value VARCHAR(200) NOT NULL COMMENT '目标值',
    description VARCHAR(500) COMMENT '描述',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dict_code (dict_code),
    UNIQUE KEY uk_dict_mapping (dict_code, source_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典映射表';

-- 2.10 脚本模板表
CREATE TABLE IF NOT EXISTS etl_script_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description VARCHAR(500) COMMENT '模板描述',
    script_language VARCHAR(20) NOT NULL DEFAULT 'GROOVY' COMMENT '脚本语言: GROOVY/JAVASCRIPT',
    script_content TEXT NOT NULL COMMENT '脚本内容',
    params_definition JSON COMMENT '参数定义(JSON)',
    return_type VARCHAR(50) COMMENT '返回类型',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_language (script_language),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脚本模板表';

-- ============================================
-- 3. 新增系统配置
-- ============================================

INSERT IGNORE INTO etl_system_config (config_group, config_key, config_value, config_type, description) VALUES
-- ETL转换配置
('ETL', 'transform.script.timeout.ms', '5000', 'INT', '脚本执行超时(毫秒)'),
('ETL', 'transform.engine.max.concurrent', '4', 'INT', '转换引擎最大并发数'),
('ETL', 'transform.default.batch.size', '1000', 'INT', '默认转换批处理大小'),
-- 数据质量配置
('QUALITY', 'quality.check.default.sample.rate', '0.1', 'STRING', '默认数据质量抽样率'),
('QUALITY', 'quality.check.max.sample.rows', '10000', 'INT', '最大抽样行数'),
('QUALITY', 'quality.rule.severity.default', 'WARNING', 'STRING', '质量规则默认严重级别'),
-- 日志配置
('LOG', 'log.retention.transform.days', '15', 'INT', '转换日志保留天数'),
('LOG', 'log.retention.quality.days', '30', 'INT', '质量日志保留天数'),
('LOG', 'log.retention.audit.days', '180', 'INT', '审计日志保留天数'),
('LOG', 'log.index.enabled', 'true', 'BOOLEAN', '启用日志索引'),
-- 审计配置
('AUDIT', 'audit.enabled', 'true', 'BOOLEAN', '启用操作审计'),
('AUDIT', 'audit.log.console', 'false', 'BOOLEAN', '审计日志输出到控制台'),
-- 分片配置
('SHARD', 'shard.default.count', '4', 'INT', '默认分片数'),
('SHARD', 'shard.max.concurrency', '8', 'INT', '分片最大并发数'),
('SHARD', 'shard.min.partition.rows', '100000', 'INT', '单分片最小行数');
