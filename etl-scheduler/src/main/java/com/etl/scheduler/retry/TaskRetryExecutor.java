package com.etl.scheduler.retry;

import com.etl.common.exception.EtlException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 任务重试执行器，统一委托 RetryPolicy + RetryExecutor
 */
@Slf4j
@Component
public class TaskRetryExecutor {

    public boolean shouldRetry(Exception e, int currentAttempt, int maxRetries) {
        if (currentAttempt >= maxRetries) return false;
        if (e instanceof EtlException etl) return etl.isRetryable();
        return true;
    }

    public long calculateBackoff(int attempt, int baseIntervalSeconds) {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(3);
        policy.setIntervalMs(baseIntervalSeconds * 1000L);
        policy.setStrategy("EXPONENTIAL");
        return policy.getDelay(attempt);
    }

    public void executeWithRetry(Runnable task, int maxRetries, int baseIntervalSeconds) {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(maxRetries);
        policy.setIntervalMs(baseIntervalSeconds * 1000L);
        policy.setStrategy("EXPONENTIAL");

        RetryExecutor.execute(policy, () -> {
            task.run();
            return null;
        }, e -> {
            if (e instanceof EtlException etl) return etl.isRetryable();
            return true; // 非EtlException默认可重试
        });
    }
}
