package com.etl.engine.incremental;

import com.etl.common.enums.IncrementalType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IncrementalStrategyFactory {
    private final Map<String, IncrementalStrategy> strategyMap = new ConcurrentHashMap<>();
    private final ObjectProvider<List<IncrementalStrategy>> strategiesProvider;

    public IncrementalStrategyFactory(ObjectProvider<List<IncrementalStrategy>> strategiesProvider) {
        this.strategiesProvider = strategiesProvider;
    }

    @PostConstruct
    public void init() {
        List<IncrementalStrategy> strategyList = strategiesProvider.getIfAvailable();
        if (strategyList != null) {
            for (IncrementalStrategy strategy : strategyList) {
                register(strategy);
            }
        }
    }

    public void register(IncrementalStrategy strategy) {
        strategyMap.put(strategy.getType(), strategy);
    }

    public IncrementalStrategy getStrategy(String type) {
        IncrementalType incrementalType = IncrementalType.fromCode(type);
        IncrementalStrategy strategy = strategyMap.get(incrementalType.getCode());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的增量策略类型: " + type);
        }
        return strategy;
    }

    public IncrementalStrategy getStrategy(IncrementalType type) {
        IncrementalStrategy strategy = strategyMap.get(type.getCode());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的增量策略类型: " + type);
        }
        return strategy;
    }
}
