package com.etl.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskStatusChangeEvent extends ApplicationEvent {
    private final Long taskId;
    private final String oldStatus;
    private final String newStatus;
    private final String reason;

    public TaskStatusChangeEvent(Object source, Long taskId, String oldStatus, String newStatus, String reason) {
        super(source);
        this.taskId = taskId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }
}
