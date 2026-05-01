package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 失败阶段枚举
 */
@Getter
@AllArgsConstructor
public enum FailurePhase {
    EXTRACT("EXTRACT", "抽取阶段"),
    TRANSFORM("TRANSFORM", "转换阶段"),
    LOAD("LOAD", "加载阶段");

    private final String code;
    private final String description;

    public static FailurePhase fromCode(String code) {
        for (FailurePhase phase : values()) {
            if (phase.getCode().equalsIgnoreCase(code)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown FailurePhase: " + code);
    }
}
