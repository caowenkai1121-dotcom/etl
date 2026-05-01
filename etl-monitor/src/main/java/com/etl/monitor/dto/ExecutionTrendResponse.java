package com.etl.monitor.dto;

import lombok.Data;

/**
 * 执行趋势响应
 */
@Data
public class ExecutionTrendResponse {

    private String date;
    private int total;
    private int success;
    private int failed;
    private long totalRows;
}
