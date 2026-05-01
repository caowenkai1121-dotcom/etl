package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聚合计算规则
 */
@Slf4j
@Component
public class AggregateRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.AGGREGATE;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String groupByField = (String) config.get("groupByField");
        String aggregateField = (String) config.get("aggregateField");
        String aggregateType = (String) config.getOrDefault("aggregateType", "COUNT");
        String targetField = (String) config.getOrDefault("targetField", aggregateType + "_" + aggregateField);

        if (groupByField != null && aggregateField != null) {
            // 此处需要全量数据进行聚合，实际使用时需要上下文支持
            log.warn("聚合规则需要在流水线上下文中处理，当前仅返回原始值");
            result.put(targetField, result.get(aggregateField));
        }
        return result;
    }
}
