package com.etl.engine.extract;

import java.util.List;
import java.util.Map;

/**
 * 数据处理回调接口
 */
@FunctionalInterface
public interface DataHandler {

    /**
     * 处理一批数据
     *
     * @param batch 数据批次
     * @throws Exception 处理异常
     */
    void handle(List<Map<String, Object>> batch) throws Exception;
}
