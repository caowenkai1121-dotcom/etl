package com.etl.engine.pipeline;

import java.util.List;
import java.util.Map;

/**
 * 数据写入阶段
 * 将处理后的数据写入目标端
 */
public interface StageWriter extends PipelineStage {

    /** 打开写入器 */
    void open(PipelineContext context) throws Exception;

    /** 写入一批数据 */
    void write(PipelineContext context, List<Map<String, Object>> data) throws Exception;

    /** 刷新缓冲区 */
    void flush() throws Exception;

    /** 关闭写入器 */
    void close() throws Exception;

    /** 获取已写入行数 */
    long getWrittenRows();

    @Override
    default PipelineStageType getType() {
        return PipelineStageType.WRITE;
    }
}
