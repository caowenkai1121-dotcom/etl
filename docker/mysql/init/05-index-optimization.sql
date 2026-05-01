-- 数据库索引优化
-- 血缘关系表：按任务+时间查询
ALTER TABLE etl_data_lineage ADD INDEX idx_task_id_time (task_id, created_at DESC);
