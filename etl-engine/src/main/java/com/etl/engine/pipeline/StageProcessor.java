package com.etl.engine.pipeline;

import java.util.List;
import java.util.Map;

/**
 * 数据处理阶段
 * 对读取的数据进行转换、过滤、校验等处理
 */
public interface StageProcessor extends PipelineStage {

    /** 处理一批数据 */
    List<Map<String, Object>> process(PipelineContext context, List<Map<String, Object>> data) throws Exception;

    @Override
    default PipelineStageType getType() {
        return PipelineStageType.TRANSFORM;
    }
}
