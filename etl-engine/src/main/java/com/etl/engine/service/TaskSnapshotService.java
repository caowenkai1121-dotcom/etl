package com.etl.engine.service;

import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.mapper.SyncTaskMapper;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSnapshotService {
    private final SyncTaskMapper syncTaskMapper;
    private final JdbcTemplate jdbcTemplate;

    public void createSnapshot(Long taskId, Long executionId) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("[Snapshot] 任务不存在: {}", taskId);
            return;
        }
        String configJson = JSON.toJSONString(task);
        jdbcTemplate.update(
            "INSERT INTO etl_task_snapshot (task_id, execution_id, snapshot_config, create_time) VALUES (?, ?, ?, NOW())",
            taskId, executionId, configJson
        );
        log.info("[Snapshot] 任务快照已保存: taskId={}, executionId={}", taskId, executionId);
    }
}
