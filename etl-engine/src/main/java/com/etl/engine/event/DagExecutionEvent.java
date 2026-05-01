package com.etl.engine.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DAG执行事件
 * 用于跨模块推送执行状态变更
 */
@Getter
@Builder
public class DagExecutionEvent {

    public enum EventType {
        TASK_START, TASK_COMPLETE, TASK_FAILED,
        NODE_START, NODE_COMPLETE, NODE_FAILED, NODE_SKIP
    }

    private final EventType eventType;
    private final Long taskId;
    private final Long executionId;
    private final String nodeId;
    private final String nodeName;
    private final String status;
    private final String message;
    private final LocalDateTime timestamp;
}
