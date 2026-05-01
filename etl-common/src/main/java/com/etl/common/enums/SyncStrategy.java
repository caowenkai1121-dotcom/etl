package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 同步策略枚举
 */
@Getter
@AllArgsConstructor
public enum SyncStrategy {

    OVERWRITE("OVERWRITE", "覆盖写入"),
    APPEND("APPEND", "追加写入"),
    UPDATE("UPDATE", "更新写入");

    private final String code;
    private final String description;

    public static SyncStrategy fromCode(String code) {
        for (SyncStrategy strategy : values()) {
            if (strategy.getCode().equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown SyncStrategy: " + code);
    }
}
