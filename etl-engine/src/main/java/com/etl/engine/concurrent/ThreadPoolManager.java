package com.etl.engine.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ThreadPoolManager {

    private final ThreadPoolExecutor syncExecutor;
    private final ThreadPoolExecutor scheduleExecutor;
    private final ThreadPoolExecutor logExecutor;
    private final Semaphore globalConcurrencyLimiter;

    public ThreadPoolManager() {
        // 同步任务线程池
        this.syncExecutor = new ThreadPoolExecutor(
            4, 16, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            namedThreadFactory("etl-sync"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 调度任务线程池
        this.scheduleExecutor = new ThreadPoolExecutor(
            2, 8, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            namedThreadFactory("etl-schedule"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 日志写入线程池（CallerRunsPolicy确保不丢日志）
        this.logExecutor = new ThreadPoolExecutor(
            1, 2, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            namedThreadFactory("etl-log"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 全局并发控制
        this.globalConcurrencyLimiter = new Semaphore(10);
    }

    public ThreadPoolExecutor getSyncExecutor() { return syncExecutor; }
    public ThreadPoolExecutor getScheduleExecutor() { return scheduleExecutor; }
    public ThreadPoolExecutor getLogExecutor() { return logExecutor; }

    public boolean tryAcquireConcurrency() { return globalConcurrencyLimiter.tryAcquire(); }
    public void acquireConcurrency() throws InterruptedException { globalConcurrencyLimiter.acquire(); }
    public void releaseConcurrency() { globalConcurrencyLimiter.release(); }

    public void adjustPoolSize(int coreSize, int maxSize) {
        syncExecutor.setCorePoolSize(coreSize);
        syncExecutor.setMaximumPoolSize(maxSize);
        log.info("调整同步线程池: core={}, max={}", coreSize, maxSize);
    }

    public Map<String, Object> getSyncPoolStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("corePoolSize", syncExecutor.getCorePoolSize());
        status.put("maximumPoolSize", syncExecutor.getMaximumPoolSize());
        status.put("activeCount", syncExecutor.getActiveCount());
        status.put("poolSize", syncExecutor.getPoolSize());
        status.put("queueSize", syncExecutor.getQueue().size());
        status.put("completedTaskCount", syncExecutor.getCompletedTaskCount());
        status.put("availablePermits", globalConcurrencyLimiter.availablePermits());
        return status;
    }

    public void shutdown() {
        syncExecutor.shutdown();
        scheduleExecutor.shutdown();
        logExecutor.shutdown();
        log.info("所有线程池已关闭");
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(0);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
