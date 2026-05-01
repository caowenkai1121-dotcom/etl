package com.etl.common.enums;

import lombok.Getter;

@Getter
public enum QualityDimension {
    COMPLETENESS("完整性"),
    ACCURACY("准确性"),
    CONSISTENCY("一致性"),
    TIMELINESS("时效性");

    private final String description;

    QualityDimension(String description) {
        this.description = description;
    }
}
