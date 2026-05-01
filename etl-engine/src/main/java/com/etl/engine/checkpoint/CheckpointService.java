package com.etl.engine.checkpoint;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.etl.engine.entity.EtlCdcPosition;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.CdcPositionMapper;
import com.etl.engine.mapper.TaskExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检查点服务
 * 用于断点续传和同步位置持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckpointService {

    private final CdcPositionMapper cdcPositionMapper;
    private final TaskExecutionMapper taskExecutionMapper;

    // 内存缓存，提高性能
    private final Map<Long, Checkpoint> checkpointCache = new ConcurrentHashMap<>();

    /**
     * 保存检查点
     *
     * @param taskId     任务ID
     * @param position   同步位置
     * @param extra      扩展信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveCheckpoint(Long taskId, String position, Map<String, Object> extra) {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setTaskId(taskId);
        checkpoint.setPosition(position);
        checkpoint.setExtra(extra);
        checkpoint.setUpdatedAt(LocalDateTime.now());

        // 更新内存缓存
        checkpointCache.put(taskId, checkpoint);

        // 持久化到数据库
        try {
            EtlCdcPosition entity = new EtlCdcPosition();
            entity.setTaskId(taskId);
            entity.setPositionValue(position);
            entity.setExtra(extra != null ? extra.toString() : null);
            entity.setUpdatedAt(LocalDateTime.now());

            // 先尝试更新，如果不存在则插入
            EtlCdcPosition existing = cdcPositionMapper.selectByTaskId(taskId);
            if (existing != null) {
                entity.setId(existing.getId());
                cdcPositionMapper.updateById(entity);
            } else {
                entity.setCreatedAt(LocalDateTime.now());
                cdcPositionMapper.insert(entity);
            }

            log.debug("检查点已保存: taskId={}, position={}", taskId, position);

        } catch (Exception e) {
            log.error("保存检查点失败: taskId={}", taskId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 获取检查点
     *
     * @param taskId 任务ID
     * @return 检查点信息
     */
    public Checkpoint getCheckpoint(Long taskId) {
        // 先从内存缓存获取
        Checkpoint cached = checkpointCache.get(taskId);
        if (cached != null) {
            return cached;
        }

        // 从数据库加载
        try {
            EtlCdcPosition entity = cdcPositionMapper.selectByTaskId(taskId);
            if (entity != null) {
                Checkpoint checkpoint = new Checkpoint();
                checkpoint.setTaskId(entity.getTaskId());
                checkpoint.setPosition(entity.getPositionValue());
                checkpoint.setExtra(entity.getExtra() != null ? Map.of("raw", entity.getExtra()) : null);
                checkpoint.setUpdatedAt(entity.getUpdatedAt());

                // 放入缓存
                checkpointCache.put(taskId, checkpoint);
                return checkpoint;
            }
        } catch (Exception e) {
            log.error("获取检查点失败: taskId={}", taskId, e);
        }

        return null;
    }

    /**
     * 获取同步位置
     *
     * @param taskId 任务ID
     * @return 同步位置
     */
    public String getPosition(Long taskId) {
        Checkpoint checkpoint = getCheckpoint(taskId);
        return checkpoint != null ? checkpoint.getPosition() : null;
    }

    /**
     * 清除检查点
     *
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearCheckpoint(Long taskId) {
        // 清除内存缓存
        checkpointCache.remove(taskId);

        // 清除数据库记录
        try {
            cdcPositionMapper.deleteByTaskId(taskId);
            log.info("检查点已清除: taskId={}", taskId);
        } catch (Exception e) {
            log.error("清除检查点失败: taskId={}", taskId, e);
        }
    }

    /**
     * 检查是否有检查点
     */
    public boolean hasCheckpoint(Long taskId) {
        return getCheckpoint(taskId) != null;
    }

    /**
     * 更新检查点位置（轻量级更新）
     */
    public void updatePosition(Long taskId, String newPosition) {
        Checkpoint checkpoint = checkpointCache.get(taskId);
        if (checkpoint != null) {
            checkpoint.setPosition(newPosition);
            checkpoint.setUpdatedAt(LocalDateTime.now());
        }

        // 持久化到数据库
        try {
            cdcPositionMapper.updatePositionByTaskId(taskId, newPosition);
        } catch (Exception e) {
            log.warn("更新检查点位置失败: taskId={}", taskId, e);
        }
    }

    /**
     * 获取所有过期的检查点
     *
     * @param hours 超时小时数
     * @return 过期的任务ID列表
     */
    public java.util.List<Long> getStaleCheckpoints(int hours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        return cdcPositionMapper.selectStaleTaskIds(threshold);
    }

    /**
     * 清理过期的检查点
     *
     * @param hours 超时小时数
     * @return 清理数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupStaleCheckpoints(int hours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        int count = cdcPositionMapper.deleteByUpdatedBefore(threshold);
        log.info("清理过期检查点: 数量={}", count);
        return count;
    }

    /**
     * 保存全量同步断点
     *
     * @param executionId 执行ID
     * @param tableName 表名
     * @param lastProcessedId 最后处理的ID
     * @param processedRows 已处理行数
     * @param totalRows 总行数
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveFullSyncCheckpoint(Long executionId, String tableName,
                                        Object lastProcessedId, long processedRows, long totalRows) {
        try {
            EtlTaskExecution execution = taskExecutionMapper.selectById(executionId);
            if (execution == null) {
                log.warn("执行记录不存在: executionId={}", executionId);
                return;
            }

            JSONObject checkpointJson;
            if (execution.getCheckpoint() != null) {
                checkpointJson = JSONUtil.parseObj(execution.getCheckpoint());
            } else {
                checkpointJson = new JSONObject();
                checkpointJson.set("tables", new JSONObject());
            }

            JSONObject tables = checkpointJson.getJSONObject("tables");
            JSONObject tableCheckpoint = new JSONObject();
            tableCheckpoint.set("lastProcessedId", lastProcessedId);
            tableCheckpoint.set("processedRows", processedRows);
            tableCheckpoint.set("totalRows", totalRows);
            tableCheckpoint.set("batchIndex", tables.containsKey(tableName) ?
                tables.getJSONObject(tableName).getInt("batchIndex", 0) + 1 : 0);
            tableCheckpoint.set("timestamp", LocalDateTime.now().toString());

            tables.set(tableName, tableCheckpoint);

            execution.setCheckpoint(checkpointJson.toString());
            taskExecutionMapper.updateById(execution);

            log.debug("保存全量同步断点: executionId={}, tableName={}, processedRows={}",
                executionId, tableName, processedRows);
        } catch (Exception e) {
            log.error("保存全量同步断点失败: executionId={}, tableName={}", executionId, tableName, e);
        }
    }

    /**
     * 获取全量同步断点
     *
     * @param executionId 执行ID
     * @param tableName 表名
     * @return 断点信息
     */
    public Map<String, Object> getFullSyncCheckpoint(Long executionId, String tableName) {
        try {
            EtlTaskExecution execution = taskExecutionMapper.selectById(executionId);
            if (execution == null || execution.getCheckpoint() == null) {
                return null;
            }

            JSONObject checkpointJson = JSONUtil.parseObj(execution.getCheckpoint());
            JSONObject tables = checkpointJson.getJSONObject("tables");
            if (tables == null || !tables.containsKey(tableName)) {
                return null;
            }

            JSONObject tableCheckpoint = tables.getJSONObject(tableName);
            Map<String, Object> result = new HashMap<>();
            result.put("lastProcessedId", tableCheckpoint.get("lastProcessedId"));
            result.put("processedRows", tableCheckpoint.getLong("processedRows"));
            result.put("totalRows", tableCheckpoint.getLong("totalRows"));
            result.put("batchIndex", tableCheckpoint.getInt("batchIndex"));
            result.put("timestamp", tableCheckpoint.getStr("timestamp"));

            return result;
        } catch (Exception e) {
            log.error("获取全量同步断点失败: executionId={}, tableName={}", executionId, tableName, e);
            return null;
        }
    }

    /**
     * 清除执行断点
     *
     * @param executionId 执行ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearExecutionCheckpoint(Long executionId) {
        try {
            EtlTaskExecution execution = taskExecutionMapper.selectById(executionId);
            if (execution != null) {
                execution.setCheckpoint(null);
                taskExecutionMapper.updateById(execution);
                log.info("清除执行断点: executionId={}", executionId);
            }
        } catch (Exception e) {
            log.error("清除执行断点失败: executionId={}", executionId, e);
        }
    }

    // ==================== 内部类 ====================

    @lombok.Data
    public static class Checkpoint {
        private Long taskId;
        private String position;
        private Map<String, Object> extra;
        private LocalDateTime updatedAt;
    }
}
