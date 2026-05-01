package com.etl.monitor.log;

import com.etl.engine.entity.EtlSyncLog;
import com.etl.engine.mapper.SyncLogMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 异步日志写入器
 * 使用后台线程批量写入日志到数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncLogWriter {

    private final SyncLogMapper syncLogMapper;

    private LogQueue logQueue;
    private ExecutorService executorService;
    private volatile boolean running = false;

    // 配置参数
    private static final int QUEUE_CAPACITY = 10000;
    private static final int BATCH_SIZE = 50;
    private static final long FLUSH_INTERVAL_MS = 3000;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        this.logQueue = new LogQueue(QUEUE_CAPACITY);
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "async-log-writer");
            t.setDaemon(true);
            return t;
        });
        this.running = true;

        // 启动后台写入线程
        executorService.submit(this::writeLoop);

        log.info("异步日志写入器启动成功: queueCapacity={}, batchSize={}, flushInterval={}ms",
            QUEUE_CAPACITY, BATCH_SIZE, FLUSH_INTERVAL_MS);
    }

    /**
     * 关闭
     */
    @PreDestroy
    public void shutdown() {
        this.running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        // 最后一次刷新队列中的剩余日志
        flushRemaining();
        log.info("异步日志写入器已关闭");
    }

    /**
     * 写入循环
     */
    private void writeLoop() {
        while (running) {
            try {
                List<EtlSyncLog> batch = logQueue.drain(BATCH_SIZE);
                if (!batch.isEmpty()) {
                    try {
                        batch.forEach(syncLogMapper::insert);
                        log.debug("批量写入日志成功: count={}", batch.size());
                    } catch (Exception e) {
                        log.error("批量写入日志失败，尝试单条写入", e);
                        // 降级为单条写入
                        for (EtlSyncLog logEntry : batch) {
                            try {
                                syncLogMapper.insert(logEntry);
                            } catch (Exception ex) {
                                log.error("单条日志写入失败: {}", logEntry.getMessage(), ex);
                            }
                        }
                    }
                } else {
                    // 队列为空时等待一段时间
                    Thread.sleep(FLUSH_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("异步日志写入异常", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 刷新剩余日志
     */
    private void flushRemaining() {
        if (logQueue != null && !logQueue.isEmpty()) {
            List<EtlSyncLog> remaining = logQueue.drain(logQueue.size());
            if (!remaining.isEmpty()) {
                try {
                    remaining.forEach(syncLogMapper::insert);
                    log.info("刷新剩余日志成功: count={}", remaining.size());
                } catch (Exception e) {
                    log.error("刷新剩余日志失败，尝试单条写入", e);
                    for (EtlSyncLog logEntry : remaining) {
                        try {
                            syncLogMapper.insert(logEntry);
                        } catch (Exception ex) {
                            log.error("单条日志写入失败: {}", logEntry.getMessage(), ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * 异步写入日志
     *
     * @param logEntry 日志条目
     */
    public void asyncWrite(EtlSyncLog logEntry) {
        if (!running) {
            // 如果已关闭，降级为同步写入
            syncWrite(logEntry);
            return;
        }

        boolean success = logQueue.offer(logEntry);
        if (!success) {
            // 队列满时降级为同步写入
            log.warn("日志队列已满，降级为同步写入: level={}", logEntry.getLogLevel());
            syncWrite(logEntry);
        }
    }

    /**
     * 同步写入日志（降级方案）
     *
     * @param logEntry 日志条目
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncWrite(EtlSyncLog logEntry) {
        try {
            syncLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("同步写入日志失败", e);
        }
    }

    /**
     * 获取当前队列大小
     */
    public int getQueueSize() {
        return logQueue != null ? logQueue.size() : 0;
    }
}
