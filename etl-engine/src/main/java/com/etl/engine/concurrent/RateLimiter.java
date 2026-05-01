package com.etl.engine.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 令牌桶限流器
 * 控制数据读取或写入的速率
 */
@Slf4j
public class RateLimiter {

    private final long maxPermits;       // 桶容量
    private final long refillIntervalMs; // 填充间隔(ms)
    private final long permitsPerRefill; // 每次填充数量
    private final AtomicLong availablePermits;
    private volatile long lastRefillTime;
    private final ReentrantLock lock = new ReentrantLock();

    public RateLimiter(long maxPermits, long permitsPerSecond) {
        this.maxPermits = maxPermits;
        this.availablePermits = new AtomicLong(maxPermits);
        this.refillIntervalMs = 1000;
        this.permitsPerRefill = permitsPerSecond;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * 获取一个令牌（阻塞直到可用）
     */
    public void acquire() {
        acquire(1);
    }

    /**
     * 获取指定数量的令牌（阻塞直到可用）
     */
    public void acquire(int permits) {
        while (true) {
            refill();
            long current = availablePermits.get();
            if (current >= permits) {
                if (availablePermits.compareAndSet(current, current - permits)) {
                    return;
                }
            } else {
                // 等待直到有足够令牌
                long waitMs = (permits - current) * refillIntervalMs / permitsPerRefill + 1;
                try {
                    TimeUnit.MILLISECONDS.sleep(Math.min(waitMs, 100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * 尝试获取令牌（非阻塞）
     */
    public boolean tryAcquire(int permits) {
        refill();
        long current = availablePermits.get();
        if (current >= permits) {
            return availablePermits.compareAndSet(current, current - permits);
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        if (elapsed >= refillIntervalMs) {
            lock.lock();
            try {
                // 双重检查，防止多线程重复填充
                elapsed = now - lastRefillTime;
                if (elapsed >= refillIntervalMs) {
                    long tokensToAdd = (elapsed / refillIntervalMs) * permitsPerRefill;
                    if (tokensToAdd > 0) {
                        availablePermits.updateAndGet(current -> Math.min(maxPermits, current + tokensToAdd));
                        lastRefillTime = now - (elapsed % refillIntervalMs);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
