package com.etl.datasource.connector;

import com.etl.common.domain.CdcEvent;

import java.util.function.Consumer;

/**
 * CDC 读取器接口
 */
public interface CdcReader {

    /**
     * 启动 CDC 读取
     */
    void start(Consumer<CdcEvent> eventConsumer);

    /**
     * 停止 CDC 读取
     */
    void stop();

    /**
     * 获取当前位置
     */
    String getPosition();

    /**
     * 是否正在运行
     */
    boolean isRunning();
}
