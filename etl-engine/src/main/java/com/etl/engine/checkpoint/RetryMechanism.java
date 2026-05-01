package com.etl.engine.checkpoint;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 重试机制
 * 支持指数退避重试，可配置最大重试次数和间隔
 */
@Slf4j
public class RetryMechanism {

    private final int maxRetries;
    private final long baseIntervalMs;
    private final long maxIntervalMs;
    private final double backoffMultiplier;

    public RetryMechanism(int maxRetries, long baseIntervalMs, long maxIntervalMs, double backoffMultiplier) {
        this.maxRetries = maxRetries;
        this.baseIntervalMs = baseIntervalMs;
        this.maxIntervalMs = maxIntervalMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    public RetryMechanism() {
        this(3, 1000, 60000, 2.0);
    }

    /**
     * 执行带重试的操作
     *
     * @param task      需要重试的任务
     * @param retryName 任务名称（日志用）
     * @return 执行结果
     */
    public <T> RetryResult<T> executeWithRetry(Callable<T> task, String retryName) {
        RetryResult<T> result = new RetryResult<>();
        long interval = baseIntervalMs;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                long startTime = System.currentTimeMillis();
                T value = task.call();
                long elapsed = System.currentTimeMillis() - startTime;

                result.setSuccess(true);
                result.setData(value);
                result.setAttempts(attempt);
                result.setTotalElapsedMs(elapsed);
                log.debug("重试任务[{}]第{}次尝试成功, 耗时={}ms", retryName, attempt, elapsed);
                return result;

            } catch (Exception e) {
                log.warn("重试任务[{}]第{}次尝试失败: {}", retryName, attempt, e.getMessage());

                if (attempt == maxRetries) {
                    result.setSuccess(false);
                    result.setAttempts(attempt);
                    result.setLastError(e);
                    result.setErrorMessage("重试" + maxRetries + "次后仍然失败: " + e.getMessage());
                    log.error("重试任务[{}]{}次均失败", retryName, maxRetries, e);
                    return result;
                }

                // 指数退避等待
                try {
                    long jitter = (long) (interval * 0.2 * Math.random());
                    TimeUnit.MILLISECONDS.sleep(interval + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    result.setSuccess(false);
                    result.setErrorMessage("重试被中断");
                    return result;
                }

                interval = (long) Math.min(interval * backoffMultiplier, maxIntervalMs);
            }
        }

        return result;
    }

    /**
     * 执行带重试的操作（不返回结果）
     */
    public RetryResult<Void> executeWithRetryRunnable(RetryRunnable task, String retryName) {
        return executeWithRetry(() -> {
            task.run();
            return null;
        }, retryName);
    }

    @FunctionalInterface
    public interface RetryRunnable {
        void run() throws Exception;
    }

    @lombok.Data
    public static class RetryResult<T> {
        private boolean success;
        private T data;
        private int attempts;
        private long totalElapsedMs;
        private Exception lastError;
        private String errorMessage;
    }
}
