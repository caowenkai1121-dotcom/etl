-- 任务执行实例表
CREATE TABLE IF NOT EXISTS etl_task_execution_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20),
    trigger_type VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    duration BIGINT,
    row_count BIGINT,
    error_msg TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
);

-- 节点执行记录表
CREATE TABLE IF NOT EXISTS etl_node_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id BIGINT NOT NULL,
    node_id VARCHAR(50) NOT NULL,
    node_name VARCHAR(200),
    status VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    input_rows BIGINT,
    output_rows BIGINT,
    error_rows BIGINT,
    log_content TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_execution_id (execution_id)
);
