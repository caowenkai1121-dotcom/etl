package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 同步模式枚举
 */
@Getter
@AllArgsConstructor
public enum SyncMode {

    FULL("FULL", "全量同步"),
    INCREMENTAL("INCREMENTAL", "增量同步"),
    CDC("CDC", "实时CDC同步");

    private final String code;
    private final String description;

    public static SyncMode fromCode(String code) {
        for (SyncMode mode : values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown SyncMode: " + code);
    }
}
