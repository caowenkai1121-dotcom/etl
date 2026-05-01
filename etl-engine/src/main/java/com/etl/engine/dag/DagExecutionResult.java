package com.etl.engine.dag;

import lombok.Data;

/**
 * DAG执行结果
 */
@Data
public class DagExecutionResult {
    private Long executionId;
    private Long taskId;
    private String status;
    private String errorMsg;
    private long duration;
    private int totalNodes;
    private int successNodes;
    private int failedNodes;
    private int skippedNodes;
    private int interruptedNodes;
}
