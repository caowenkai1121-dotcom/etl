package com.etl.engine;

import com.etl.common.domain.SyncPipelineContext;

/**
 * 同步引擎接口
 */
public interface SyncEngine {

    /**
     * 执行同步
     */
    void sync(SyncPipelineContext context) throws Exception;

    /**
     * 停止同步
     */
    void stop();

    /**
     * 获取同步进度
     */
    int getProgress();

    /**
     * 是否正在运行
     */
    boolean isRunning();
}
