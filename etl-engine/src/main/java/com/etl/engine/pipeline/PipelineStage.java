package com.etl.engine.pipeline;

/**
 * 流水线阶段基接口
 * 所有流水线阶段的通用契约
 */
public interface PipelineStage {

    /** 阶段名称 */
    String getName();

    /** 阶段类型 */
    PipelineStageType getType();

    /** 初始化 */
    default void init(PipelineContext context) throws Exception {}

    /** 销毁 */
    default void destroy() throws Exception {}
}
