package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CDC操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum CdcEventType {

    INSERT("INSERT", "插入"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String description;

    public static CdcEventType fromCode(String code) {
        for (CdcEventType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CdcEventType: " + code);
    }
}
