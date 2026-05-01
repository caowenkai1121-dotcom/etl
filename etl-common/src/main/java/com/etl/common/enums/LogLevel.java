package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日志级别枚举
 */
@Getter
@AllArgsConstructor
public enum LogLevel {

    DEBUG("DEBUG", "调试"),
    INFO("INFO", "信息"),
    WARN("WARN", "警告"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String description;

    public static LogLevel fromCode(String code) {
        for (LogLevel level : values()) {
            if (level.getCode().equalsIgnoreCase(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown LogLevel: " + code);
    }
}
