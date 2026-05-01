package com.etl.engine.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流水线执行结果
 */
@Getter
@Setter
public class PipelineResult {

    private boolean success;
    private long totalRows;
    private long successRows;
    private long failedRows;
    private long totalElapsedMs;
    private String errorMessage;
    private String traceId;

    /** 各阶段统计 */
    private final Map<String, PipelineContext.StageStats> stageStats = new LinkedHashMap<>();

    /** 校验结果 */
    private ValidationResult validationResult;

    public void addStageStats(String stageName, PipelineContext.StageStats stats) {
        stageStats.put(stageName, stats);
    }

    /** 获取进度百分比 (0-100) */
    public int getProgress() {
        if (totalRows == 0) return 0;
        return (int) ((successRows + failedRows) * 100 / totalRows);
    }

    @Getter
    @Setter
    public static class ValidationResult {
        private boolean passed;
        private long sourceCount;
        private long targetCount;
        private boolean countMatch;
        private int sampleSize;
        private int matchCount;
        private int mismatchCount;
        private double sampleMatchRate;
        private int missingKeyCount;
        private int extraKeyCount;
        private String summary;
    }
}
