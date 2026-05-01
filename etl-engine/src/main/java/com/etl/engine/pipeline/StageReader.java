package com.etl.engine.pipeline;

import java.util.List;
import java.util.Map;

/**
 * 数据读取阶段
 * 从源端读取数据，支持分片、游标、限速
 */
public interface StageReader extends PipelineStage {

    /** 打开读取器 */
    void open(PipelineContext context) throws Exception;

    /** 读取一批数据 */
    List<Map<String, Object>> read(PipelineContext context) throws Exception;

    /** 是否还有更多数据 */
    boolean hasNext(PipelineContext context) throws Exception;

    /** 关闭读取器 */
    void close() throws Exception;

    /** 获取已读取行数 */
    long getReadRows();

    @Override
    default PipelineStageType getType() {
        return PipelineStageType.READ;
    }
}
