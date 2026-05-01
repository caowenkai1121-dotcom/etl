package com.etl.engine.pipeline;

import com.etl.common.domain.SyncPipelineContext;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流水线上下文
 * 贯穿整个流水线执行过程，存储各阶段共享数据
 */
@Getter
@Setter
public class PipelineContext {

    /** 同步上下文（来自上层调用） */
    private SyncPipelineContext syncContext;

    /** 任务ID */
    private Long taskId;

    /** 执行ID */
    private Long executionId;

    /** 全链路追踪ID */
    private String traceId;

    /** 源表名 */
    private String sourceTable;

    /** 目标表名 */
    private String targetTable;

    /** 分片信息 */
    private Shard shard;

    /** 阶段间共享数据 */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /** 阶段执行统计 */
    private final Map<String, StageStats> stageStats = new ConcurrentHashMap<>();

    public PipelineContext(SyncPipelineContext syncContext) {
        this.syncContext = syncContext;
        this.taskId = syncContext.getTaskId();
        this.executionId = syncContext.getExecutionId();
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void recordStageStats(String stageName, long elapsedMs, long recordCount, boolean success) {
        stageStats.put(stageName, new StageStats(stageName, elapsedMs, recordCount, success));
    }

    @Getter
    public static class StageStats {
        private final String stageName;
        private final long elapsedMs;
        private final long recordCount;
        private final boolean success;

        public StageStats(String stageName, long elapsedMs, long recordCount, boolean success) {
            this.stageName = stageName;
            this.elapsedMs = elapsedMs;
            this.recordCount = recordCount;
            this.success = success;
        }
    }
}
