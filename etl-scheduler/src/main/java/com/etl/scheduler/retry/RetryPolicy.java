package com.etl.scheduler.retry;

import lombok.Data;

@Data
public class RetryPolicy {
    private int maxRetries = 3;
    private long intervalMs = 5000;
    private String strategy = "FIXED"; // FIXED or EXPONENTIAL

    public long getDelay(int attempt) {
        if ("EXPONENTIAL".equals(strategy)) {
            long delay = intervalMs * (1L << Math.min(attempt, 10));
            return Math.min(delay, 300_000L); // 最大5分钟
        }
        return intervalMs;
    }
}
