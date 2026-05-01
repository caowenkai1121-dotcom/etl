package com.etl.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatisticsResponse {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 总执行次数
     */
    private Integer totalExecutions;

    /**
     * 成功次数
     */
    private Integer successCount;

    /**
     * 失败次数
     */
    private Integer failedCount;

    /**
     * 取消次数
     */
    private Integer cancelledCount;

    /**
     * 成功率
     */
    private BigDecimal successRate;

    /**
     * 总同步行数
     */
    private Long totalRows;

    /**
     * 成功行数
     */
    private Long successRows;

    /**
     * 失败行数
     */
    private Long failedRows;

    /**
     * 总耗时(毫秒)
     */
    private Long totalDuration;

    /**
     * 平均耗时(毫秒)
     */
    private Long avgDuration;

    /**
     * 最大耗时(毫秒)
     */
    private Long maxDuration;

    /**
     * 最小耗时(毫秒)
     */
    private Long minDuration;

    /**
     * 最后成功时间
     */
    private LocalDateTime lastSuccessTime;

    /**
     * 最后失败时间
     */
    private LocalDateTime lastFailureTime;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecutionTime;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 同步模式
     */
    private String syncMode;

    /**
     * 格式化平均耗时
     */
    public String getFormattedAvgDuration() {
        if (avgDuration == null || avgDuration == 0) {
            return "-";
        }
        return formatDuration(avgDuration);
    }

    /**
     * 格式化总耗时
     */
    public String getFormattedTotalDuration() {
        if (totalDuration == null || totalDuration == 0) {
            return "-";
        }
        return formatDuration(totalDuration);
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}
