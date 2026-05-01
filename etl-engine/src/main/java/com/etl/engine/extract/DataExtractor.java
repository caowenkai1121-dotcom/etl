package com.etl.engine.extract;

import com.etl.common.domain.SyncPipelineContext;

/**
 * 数据抽取器接口
 */
public interface DataExtractor {

    /**
     * 抽取数据
     *
     * @param context 同步上下文
     * @param handler 数据处理回调
     * @throws Exception 抽取异常
     */
    void extract(SyncPipelineContext context, DataHandler handler) throws Exception;
}
