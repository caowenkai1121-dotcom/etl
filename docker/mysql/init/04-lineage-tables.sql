-- 数据血缘关系表
CREATE TABLE IF NOT EXISTS etl_data_lineage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    node_id VARCHAR(50) NOT NULL COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    node_type VARCHAR(50) COMMENT '节点类型',
    source_datasource_id BIGINT COMMENT '来源数据源ID',
    source_table VARCHAR(200) COMMENT '来源表',
    source_field VARCHAR(200) COMMENT '来源字段',
    target_datasource_id BIGINT COMMENT '目标数据源ID',
    target_table VARCHAR(200) COMMENT '目标表',
    target_field VARCHAR(200) COMMENT '目标字段',
    transform_logic VARCHAR(500) COMMENT '转换逻辑',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_node_id (node_id),
    INDEX idx_source (source_datasource_id, source_table),
    INDEX idx_target (target_datasource_id, target_table)
) COMMENT='数据血缘关系表';
