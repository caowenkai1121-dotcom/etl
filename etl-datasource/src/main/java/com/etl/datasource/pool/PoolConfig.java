package com.etl.datasource.pool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PoolConfig {
    @Builder.Default private int maximumPoolSize = 10;
    @Builder.Default private int minimumIdle = 2;
    @Builder.Default private long connectionTimeoutMs = 30000;
    @Builder.Default private long idleTimeoutMs = 600000;
    @Builder.Default private long maxLifetimeMs = 1800000;
    @Builder.Default private long leakDetectionThresholdMs = 60000;
    @Builder.Default private long validationTimeoutMs = 5000;
}
