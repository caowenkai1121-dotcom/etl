package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 类型推断与转换规则
 */
@Slf4j
@Component
public class TypeInferRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.TYPE_INFER;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String field = (String) config.get("field");
        String targetType = (String) config.getOrDefault("targetType", "STRING");
        String targetField = (String) config.getOrDefault("targetField", field);

        if (field != null && result.containsKey(field)) {
            Object value = result.get(field);
            if (value != null) {
                try {
                    Object converted = convertType(value.toString(), targetType);
                    result.put(targetField, converted);
                } catch (Exception e) {
                    log.warn("类型转换失败: {}", e.getMessage());
                    result.put(targetField, value);
                }
            }
        }
        return result;
    }

    private Object convertType(String input, String type) {
        switch (type.toUpperCase()) {
            case "INTEGER":
                return Integer.parseInt(input);
            case "LONG":
                return Long.parseLong(input);
            case "DOUBLE":
                return Double.parseDouble(input);
            case "BOOLEAN":
                return Boolean.parseBoolean(input);
            case "STRING":
            default:
                return input;
        }
    }
}
