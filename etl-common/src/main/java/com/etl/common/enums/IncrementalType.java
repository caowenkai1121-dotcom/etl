package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 增量类型枚举
 */
@Getter
@AllArgsConstructor
public enum IncrementalType {
    TIMESTAMP("TIMESTAMP", "时间戳增量"),
    AUTO_INCREMENT("AUTO_INCREMENT", "自增ID增量"),
    BINLOG("BINLOG", "Binlog增量");

    private final String code;
    private final String description;

    public static IncrementalType fromCode(String code) {
        for (IncrementalType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return TIMESTAMP; // 默认返回TIMESTAMP保证向下兼容
    }
}
