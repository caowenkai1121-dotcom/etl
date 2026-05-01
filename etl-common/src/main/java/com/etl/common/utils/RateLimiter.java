package com.etl.common.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限流器
 * 使用AtomicLong和volatile实现线程安全
 * 不依赖任何第三方库
 */
public class RateLimiter {

    private final int maxTokens; // 最大令牌数
    private final long refillIntervalMs; // 令牌填充间隔（毫秒）
    private final int refillAmount; // 每次填充的令牌数

    private final AtomicLong availableTokens; // 可用令牌数
    private volatile long lastRefillTimestamp; // 上次填充时间戳

    /**
     * 构造器
     */
    public RateLimiter(int maxTokens, long refillIntervalMs, int refillAmount) {
        if (maxTokens <= 0 || refillIntervalMs <= 0 || refillAmount <= 0) {
            throw new IllegalArgumentException("参数必须大于0");
        }
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.refillAmount = refillAmount;
        this.availableTokens = new AtomicLong(maxTokens);
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    /**
     * 尝试获取1个令牌（非阻塞）
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * 尝试获取指定数量的令牌（非阻塞）
     */
    public boolean tryAcquire(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("获取的令牌数必须大于0");
        }

        refillTokens();

        long currentTokens = availableTokens.get();
        if (currentTokens >= permits) {
            return availableTokens.compareAndSet(currentTokens, currentTokens - permits);
        }

        return false;
    }

    /**
     * 获取1个令牌（阻塞等待）
     */
    public void acquire() {
        acquire(1);
    }

    /**
     * 获取指定数量的令牌（阻塞等待）
     */
    public void acquire(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("获取的令牌数必须大于0");
        }

        while (true) {
            refillTokens();

            long currentTokens = availableTokens.get();
            if (currentTokens >= permits) {
                if (availableTokens.compareAndSet(currentTokens, currentTokens - permits)) {
                    return;
                }
            } else {
                // 计算需要等待的时间
                long tokensNeeded = permits - currentTokens;
                long waitTime = calculateWaitTime(tokensNeeded);
                sleep(waitTime);
            }
        }
    }

    /**
     * 填充令牌
     * 使用同步块确保线程安全
     */
    private synchronized void refillTokens() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - lastRefillTimestamp;

        if (timeElapsed >= refillIntervalMs) {
            long refillTimes = timeElapsed / refillIntervalMs;
            long newTokens = refillTimes * refillAmount;

            if (newTokens > 0) {
                lastRefillTimestamp = now - (timeElapsed % refillIntervalMs);
                availableTokens.updateAndGet(current -> {
                    long result = current + newTokens;
                    return Math.min(result, maxTokens);
                });
            }
        }
    }

    /**
     * 计算需要等待的时间
     */
    private long calculateWaitTime(long tokensNeeded) {
        long tokensPerInterval = refillAmount;
        long intervalsNeeded = (tokensNeeded + tokensPerInterval - 1) / tokensPerInterval;
        return intervalsNeeded * refillIntervalMs;
    }

    /**
     * 获取可用令牌数
     */
    public long getAvailableTokens() {
        refillTokens();
        return availableTokens.get();
    }

    /**
     * 线程睡眠
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
