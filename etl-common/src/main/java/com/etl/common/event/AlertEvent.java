package com.etl.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertEvent extends ApplicationEvent {
    private final Long ruleId;
    private final String alertType;
    private final String message;
    private final String severity;

    public AlertEvent(Object source, Long ruleId, String alertType, String message, String severity) {
        super(source);
        this.ruleId = ruleId;
        this.alertType = alertType;
        this.message = message;
        this.severity = severity;
    }
}
