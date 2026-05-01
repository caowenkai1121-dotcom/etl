package com.etl.scheduler.retry;

import lombok.extern.slf4j.Slf4j;

import java.util.function.*;

@Slf4j
public class RetryExecutor {
    public static <T> T execute(RetryPolicy policy, Supplier<T> action, Predicate<Exception> retryable) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= policy.getMaxRetries(); attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                if (!retryable.test(e) || attempt == policy.getMaxRetries()) {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
                long delay = policy.getDelay(attempt);
                log.warn("[Retry] 第{}次重试, {}ms后执行", attempt + 1, delay, e);
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
            }
        }
        throw new RuntimeException("重试耗尽", lastException);
    }
}
