-- ============================================
-- ETL数据同步调度系统 - V2.0 升级脚本
-- 数据库: MySQL 8.0
-- 版本: 1.0.0 → 2.0.0
-- ============================================

USE etl_system;

-- ============================================
-- 1. 现有表新增字段
-- ============================================

-- etl_sync_task 新增字段（MySQL不支持ADD COLUMN IF NOT EXISTS，使用存储过程安全添加）
DROP PROCEDURE IF EXISTS etl_add_column;
DELIMITER $$
CREATE PROCEDURE etl_add_column(IN p_table VARCHAR(100), IN p_column VARCHAR(100), IN p_definition VARCHAR(500))
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

CALL etl_add_column('etl_sync_task', 'priority', 'INT DEFAULT 5 COMMENT ''任务优先级(1-10)'' AFTER retry_interval');
CALL etl_add_column('etl_sync_task', 'incremental_type', 'VARCHAR(20) DEFAULT ''TIMESTAMP'' COMMENT ''增量策略类型: TIMESTAMP/AUTO_INCREMENT/BINLOG'' AFTER incremental_value');
CALL etl_add_column('etl_sync_task', 'feature_flags', 'JSON COMMENT ''功能开关(JSON)'' AFTER incremental_type');

CALL etl_add_column('etl_task_execution', 'error_code', 'VARCHAR(20) COMMENT ''错误码'' AFTER error_message');
CALL etl_add_column('etl_task_execution', 'error_category', 'VARCHAR(30) COMMENT ''错误分类'' AFTER error_code');
CALL etl_add_column('etl_task_execution', 'error_detail', 'JSON COMMENT ''结构化错误详情'' AFTER error_category');
CALL etl_add_column('etl_task_execution', 'failure_phase', 'VARCHAR(20) COMMENT ''失败阶段: EXTRACT/TRANSFORM/LOAD'' AFTER error_detail');
CALL etl_add_column('etl_task_execution', 'affected_table', 'VARCHAR(200) COMMENT ''受影响的表'' AFTER failure_phase');
CALL etl_add_column('etl_task_execution', 'retry_attempt', 'INT DEFAULT 0 COMMENT ''当前重试次数'' AFTER affected_table');

DROP PROCEDURE IF EXISTS etl_add_column;

-- ============================================
-- 2. 新增表
-- ============================================

-- 数据血缘关系表
CREATE TABLE IF NOT EXISTS etl_data_lineage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    execution_id BIGINT,
    source_ds_id BIGINT NOT NULL,
    source_table VARCHAR(200) NOT NULL,
    target_ds_id BIGINT NOT NULL,
    target_table VARCHAR(200) NOT NULL,
    column_mapping JSON,
    row_count BIGINT DEFAULT 0,
    sync_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_source (source_ds_id, source_table),
    INDEX idx_target (target_ds_id, target_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘关系表';

-- 任务依赖关系表
CREATE TABLE IF NOT EXISTS etl_task_dependency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    depends_on_task_id BIGINT NOT NULL,
    dependency_type VARCHAR(20) DEFAULT 'FINISH' COMMENT '依赖类型: FINISH/SUCCESS',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dependency (task_id, depends_on_task_id),
    INDEX idx_depends_on (depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖关系表';

-- 任务每日统计摘要表
CREATE TABLE IF NOT EXISTS etl_task_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    summary_date DATE NOT NULL,
    total_executions INT DEFAULT 0,
    success_executions INT DEFAULT 0,
    failed_executions INT DEFAULT 0,
    avg_duration BIGINT DEFAULT 0,
    total_rows BIGINT DEFAULT 0,
    success_rows BIGINT DEFAULT 0,
    failed_rows BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_date (task_id, summary_date),
    INDEX idx_summary_date (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务每日统计摘要表';

-- 失败任务归档表
CREATE TABLE IF NOT EXISTS etl_failed_task_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_execution_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    task_name VARCHAR(200),
    source_ds_name VARCHAR(100),
    target_ds_name VARCHAR(100),
    error_code VARCHAR(20),
    error_category VARCHAR(30),
    error_message TEXT,
    error_detail JSON,
    failure_phase VARCHAR(20),
    affected_table VARCHAR(200),
    retry_attempt INT,
    total_rows BIGINT,
    processed_rows BIGINT,
    start_time DATETIME,
    end_time DATETIME,
    archived_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_error_code (error_code),
    INDEX idx_archived_at (archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失败任务归档表';

-- ============================================
-- 3. 新增系统配置数据
-- ============================================

INSERT IGNORE INTO etl_system_config (config_group, config_key, config_value, config_type, description) VALUES
-- 引擎配置
('ENGINE', 'batch.size', '1000', 'INT', '默认批量处理大小'),
('ENGINE', 'parallel.threads', '4', 'INT', '默认并行线程数'),
('ENGINE', 'retry.times', '3', 'INT', '默认重试次数'),
('ENGINE', 'retry.interval.seconds', '60', 'INT', '默认重试间隔(秒)'),
('ENGINE', 'query.timeout.seconds', '60', 'INT', '查询超时时间(秒)'),
('ENGINE', 'feature.binlog_incremental.enabled', 'false', 'BOOLEAN', '启用Binlog增量策略'),
('ENGINE', 'feature.desensitize.enabled', 'false', 'BOOLEAN', '启用数据脱敏'),
('ENGINE', 'feature.data_lineage.enabled', 'false', 'BOOLEAN', '启用数据血缘'),
('ENGINE', 'feature.stream_load_doris.enabled', 'false', 'BOOLEAN', '启用Doris Stream Load'),
-- 系统配置
('SYSTEM', 'sync.core.pool.size', '4', 'INT', '同步核心线程数'),
('SYSTEM', 'sync.max.pool.size', '16', 'INT', '同步最大线程数'),
('SYSTEM', 'sync.queue.capacity', '100', 'INT', '任务队列容量'),
('SYSTEM', 'sync.keep.alive.seconds', '60', 'INT', '空闲线程存活时间(秒)'),
('SYSTEM', 'global.max.concurrency', '10', 'INT', '全局最大并发任务数'),
('SYSTEM', 'cache.metadata.ttl.seconds', '3600', 'INT', '元数据缓存TTL(秒)'),
('SYSTEM', 'cache.metadata.max.size', '1000', 'INT', '元数据缓存最大条目数'),
-- 数据源配置
('DATASOURCE', 'connection.timeout.ms', '30000', 'INT', '连接超时时间(毫秒)'),
('DATASOURCE', 'pool.max.size', '10', 'INT', '连接池最大连接数'),
('DATASOURCE', 'pool.min.idle', '2', 'INT', '连接池最小空闲连接数'),
('DATASOURCE', 'pool.max.lifetime.ms', '1800000', 'INT', '连接最大存活时间(毫秒)'),
('DATASOURCE', 'pool.leak.detection.ms', '60000', 'INT', '连接泄漏检测阈值(毫秒)'),
-- 监控配置
('MONITOR', 'log.retention.days', '30', 'INT', '日志保留天数'),
('MONITOR', 'execution.retention.days', '90', 'INT', '执行记录保留天数'),
('MONITOR', 'log.batch.size', '50', 'INT', '日志批量写入大小'),
('MONITOR', 'log.flush.interval.ms', '3000', 'INT', '日志刷新间隔(毫秒)'),
('MONITOR', 'monitor.overview.cache.ttl.seconds', '30', 'INT', '系统概览缓存TTL(秒)'),
('MONITOR', 'monitor.trend.cache.ttl.seconds', '300', 'INT', '执行趋势缓存TTL(秒)');
