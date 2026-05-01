package com.etl.engine.dto;

import lombok.Data;

/**
 * 任务进度响应
 */
@Data
public class TaskProgressResponse {

    private Long taskId;
    private Long executionId;
    private String executionNo;
    private int progress;
    private Long totalRows;
    private Long successRows;
    private Long failedRows;
    private boolean running;
    private String status;
}
