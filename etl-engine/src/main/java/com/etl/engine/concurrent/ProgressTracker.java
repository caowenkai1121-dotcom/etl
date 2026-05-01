package com.etl.engine.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ProgressTracker {

    private final AtomicLong totalRows = new AtomicLong(0);
    private final AtomicLong processedRows = new AtomicLong(0);
    private final AtomicLong successRows = new AtomicLong(0);
    private final AtomicLong failedRows = new AtomicLong(0);

    // 滑动窗口速率计算
    private final Deque<long[]> rateWindow = new ArrayDeque<>();
    private static final int WINDOW_SIZE = 30; // 30秒窗口
    private volatile long startTimeMs;
    private final ReentrantLock windowLock = new ReentrantLock();

    public void initTotalRows(long count) { totalRows.set(count); }
    public void addTotalRows(long delta) { totalRows.addAndGet(delta); }
    public void addProcessedRows(long delta) { processedRows.addAndGet(delta); }
    public void addSuccessRows(long delta) { successRows.addAndGet(delta); }
    public void addFailedRows(long delta) { failedRows.addAndGet(delta); }

    public void start() { this.startTimeMs = System.currentTimeMillis(); }

    public BigDecimal getProgress() {
        long total = totalRows.get();
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(processedRows.get())
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    public double getRowsPerSecond() {
        if (startTimeMs <= 0) return 0;
        long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000;
        if (elapsed <= 0) return 0;
        return (double) processedRows.get() / elapsed;
    }

    public int getEstimatedRemainingSeconds() {
        double rate = getRowsPerSecond();
        if (rate <= 0) return -1;
        long remaining = totalRows.get() - processedRows.get();
        if (remaining <= 0) return 0;
        return (int) (remaining / rate);
    }

    public long getTotalRows() { return totalRows.get(); }
    public long getProcessedRows() { return processedRows.get(); }
    public long getSuccessRows() { return successRows.get(); }
    public long getFailedRows() { return failedRows.get(); }

    public void recordRateSample() {
        long now = System.currentTimeMillis();
        windowLock.lock();
        try {
            rateWindow.addLast(new long[]{now, processedRows.get()});
            // 移除超过窗口的样本
            while (rateWindow.size() > WINDOW_SIZE) {
                rateWindow.removeFirst();
            }
        } finally {
            windowLock.unlock();
        }
    }
}
