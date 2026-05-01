package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行状态枚举
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    RUNNING("RUNNING", "运行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String description;

    public static ExecutionStatus fromCode(String code) {
        for (ExecutionStatus status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ExecutionStatus: " + code);
    }
}
