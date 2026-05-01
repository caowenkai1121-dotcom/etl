package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 失败策略枚举
 */
@Getter
@AllArgsConstructor
public enum OnFailStrategy {
    SKIP_ROW("SKIP_ROW", "跳过该行"),
    DEFAULT_VALUE("DEFAULT_VALUE", "使用默认值"),
    ABORT("ABORT", "中止任务");

    private final String code;
    private final String description;

    public static OnFailStrategy fromCode(String code) {
        for (OnFailStrategy strategy : values()) {
            if (strategy.getCode().equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown OnFailStrategy: " + code);
    }
}
