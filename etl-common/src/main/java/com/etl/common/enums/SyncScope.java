package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 同步范围枚举
 */
@Getter
@AllArgsConstructor
public enum SyncScope {

    SINGLE_TABLE("SINGLE_TABLE", "单表同步"),
    MULTI_TABLE("MULTI_TABLE", "多表同步"),
    FULL_DATABASE("FULL_DATABASE", "整库同步");

    private final String code;
    private final String description;

    public static SyncScope fromCode(String code) {
        for (SyncScope scope : values()) {
            if (scope.getCode().equalsIgnoreCase(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown SyncScope: " + code);
    }
}
