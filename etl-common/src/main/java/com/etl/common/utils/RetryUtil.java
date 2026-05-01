package com.etl.common.utils;

import com.etl.common.exception.EtlException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 重试工具类
 * 提供指数退避重试机制
 */
@Slf4j
public class RetryUtil {

    private RetryUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 最大退避间隔（5分钟）
     */
    private static final long MAX_BACKOFF_MS = 300000L;

    /**
     * 执行有返回值的任务并自动重试
     * 使用指数退避策略
     */
    public static <T> T executeWithRetry(Supplier<T> supplier, int maxRetries, long baseIntervalMs) {
        return executeWithRetry(supplier, maxRetries, baseIntervalMs, e -> true);
    }

    /**
     * 执行有返回值的任务并自动重试
     * 使用指数退避策略和自定义重试判断条件
     */
    public static <T> T executeWithRetry(Supplier<T> supplier, int maxRetries, long baseIntervalMs,
                                         Predicate<Exception> retryPredicate) {
        int attempt = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (Exception e) {
                attempt++;
                log.warn("任务执行失败，第{}次尝试，异常: {}", attempt, e.getMessage());

                if (attempt >= maxRetries || !retryPredicate.test(e)) {
                    log.error("任务执行失败，已达到最大重试次数{}", maxRetries);
                    throw new EtlException("TASK_RETRY_FAILED", "任务重试失败: " + e.getMessage(), e);
                }

                long delay = calculateBackoffDelay(attempt, baseIntervalMs);
                log.info("等待{}ms后进行第{}次重试", delay, attempt + 1);
                sleep(delay);
            }
        }
    }

    /**
     * 执行无返回值的任务并自动重试
     * 使用指数退避策略
     */
    public static void executeWithRetry(Runnable runnable, int maxRetries, long baseIntervalMs) {
        executeWithRetry(() -> {
            runnable.run();
            return null;
        }, maxRetries, baseIntervalMs);
    }

    /**
     * 计算退避延迟
     * 公式：min(baseIntervalMs * 2^attempt, MAX_BACKOFF_MS)
     */
    private static long calculateBackoffDelay(int attempt, long baseIntervalMs) {
        long delay = baseIntervalMs * (1L << attempt);
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    /**
     * 线程睡眠
     */
    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("睡眠被中断", e);
        }
    }
}
