package com.etl.engine.load;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackpressureController {
    private final int maxPendingSize;
    private final long checkIntervalMs;
    private volatile int pendingCount = 0;

    public BackpressureController(int maxPendingSize, long checkIntervalMs) {
        this.maxPendingSize = maxPendingSize;
        this.checkIntervalMs = checkIntervalMs;
    }

    public synchronized void beforeExtract() throws InterruptedException {
        while (pendingCount >= maxPendingSize) {
            log.warn("[Backpressure] 待处理数据量达到上限 {}, 暂停抽取", pendingCount);
            wait(checkIntervalMs);
        }
    }

    public synchronized void afterLoad(int count) {
        pendingCount -= count;
        if (pendingCount < maxPendingSize) {
            notifyAll();
        }
    }

    public synchronized void onExtract(int count) {
        pendingCount += count;
    }

    public int getPendingCount() { return pendingCount; }
}