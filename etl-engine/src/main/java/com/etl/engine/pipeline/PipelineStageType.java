package com.etl.engine.pipeline;

/**
 * 流水线阶段类型
 */
public enum PipelineStageType {
    READ("数据读取"),
    TRANSFORM("数据转换"),
    FILTER("数据过滤"),
    VALIDATE("数据校验"),
    WRITE("数据写入");

    private final String description;

    PipelineStageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
