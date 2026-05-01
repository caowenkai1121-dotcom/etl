package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警类型枚举
 */
@Getter
@AllArgsConstructor
public enum AlertType {
    TASK_FAILED("TASK_FAILED", "任务失败"),
    SYNC_DELAY("SYNC_DELAY", "同步延迟"),
    CONNECTION_ERROR("CONNECTION_ERROR", "连接错误"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统错误"),
    THRESHOLD_EXCEEDED("THRESHOLD_EXCEEDED", "阈值超限");

    private final String code;
    private final String description;

    public static AlertType fromCode(String code) {
        for (AlertType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AlertType: " + code);
    }
}
