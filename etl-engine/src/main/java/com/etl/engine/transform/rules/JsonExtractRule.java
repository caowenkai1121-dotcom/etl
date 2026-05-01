package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON提取规则
 */
@Slf4j
@Component
public class JsonExtractRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.JSON_EXTRACT;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String field = (String) config.get("field");
        String targetField = (String) config.get("targetField");
        String jsonPath = (String) config.get("jsonPath");

        if (field != null && targetField != null && jsonPath != null && result.containsKey(field)) {
            Object value = result.get(field);
            if (value != null) {
                try {
                    String jsonStr = value.toString();
                    Map<String, Object> parsed = com.alibaba.fastjson2.JSON.parseObject(jsonStr, Map.class);
                    Object extracted = extractValue(parsed, jsonPath);
                    result.put(targetField, extracted);
                } catch (Exception e) {
                    log.warn("JSON提取失败: {}", e.getMessage());
                    result.put(targetField, null);
                }
            }
        }
        return result;
    }

    private Object extractValue(Map<String, Object> json, String path) {
        String[] parts = path.split("\\.");
        Object current = json;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
            if (current == null) {
                break;
            }
        }

        return current;
    }
}
