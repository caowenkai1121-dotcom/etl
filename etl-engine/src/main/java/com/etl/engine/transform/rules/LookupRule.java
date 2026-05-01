package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 查找表规则
 */
@Slf4j
@Component
public class LookupRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.LOOKUP;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String sourceField = (String) config.get("sourceField");
        String targetField = (String) config.get("targetField");
        Object defaultValue = config.get("defaultValue");
        Map<String, Object> mapping = (Map<String, Object>) config.get("mapping");

        if (sourceField != null && targetField != null) {
            Object key = result.get(sourceField);
            if (key != null && mapping != null && mapping.containsKey(key.toString())) {
                result.put(targetField, mapping.get(key.toString()));
            } else if (defaultValue != null) {
                result.put(targetField, defaultValue);
            }
        }
        return result;
    }
}
