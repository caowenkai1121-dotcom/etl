package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 触发类型枚举
 */
@Getter
@AllArgsConstructor
public enum TriggerType {

    MANUAL("MANUAL", "手动触发"),
    SCHEDULED("SCHEDULED", "定时触发"),
    CDC("CDC", "CDC实时触发");

    private final String code;
    private final String description;

    public static TriggerType fromCode(String code) {
        for (TriggerType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TriggerType: " + code);
    }
}
