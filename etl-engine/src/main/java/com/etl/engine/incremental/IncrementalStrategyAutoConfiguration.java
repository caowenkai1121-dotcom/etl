package com.etl.engine.incremental;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IncrementalStrategyAutoConfiguration {

    @Bean
    public TimestampIncrementalStrategy timestampIncrementalStrategy() {
        return new TimestampIncrementalStrategy();
    }

    @Bean
    public AutoIncrementStrategy autoIncrementStrategy() {
        return new AutoIncrementStrategy();
    }

    // BinlogIncrementalStrategy 通过 @ConditionalOnProperty 条件注册
}
