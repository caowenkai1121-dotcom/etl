package com.etl.engine.transform;

import com.etl.common.enums.TransformRuleType;
import java.util.*;

public class TransformRuleFactory {
    private static final Map<TransformRuleType, TransformRule> rules = new EnumMap<>(TransformRuleType.class);

    public static void register(TransformRuleType type, TransformRule rule) {
        rules.put(type, rule);
    }

    public static TransformRule getRule(TransformRuleType type) {
        TransformRule rule = rules.get(type);
        if (rule == null) throw new IllegalArgumentException("未注册的转换规则: " + type);
        return rule;
    }

    public static Collection<TransformRuleType> supportedTypes() { return rules.keySet(); }

    public interface TransformRule {
        TransformRuleType getType();
        Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config);
    }
}
