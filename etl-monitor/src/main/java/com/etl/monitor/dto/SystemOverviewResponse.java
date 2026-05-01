package com.etl.monitor.dto;

import lombok.Data;

/**
 * 系统概览响应
 */
@Data
public class SystemOverviewResponse {

    private int todayExecutions;
    private int todaySuccess;
    private int todayFailed;
    private int runningTasks;
    private long todayTotalRows;
    private double todaySuccessRate;
    private int alertCount;
    private long weekTotalRows;
    private long weekSuccessRows;
    private long avgDailyRows;
}
