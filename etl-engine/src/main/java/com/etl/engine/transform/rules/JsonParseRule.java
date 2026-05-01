package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON解析规则
 */
@Slf4j
@Component
public class JsonParseRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.JSON_PARSE;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String field = (String) config.get("field");
        String targetField = (String) config.getOrDefault("targetField", field);

        if (field != null && result.containsKey(field)) {
            Object value = result.get(field);
            if (value != null) {
                try {
                    String jsonStr = value.toString();
                    Map<String, Object> parsed = com.alibaba.fastjson2.JSON.parseObject(jsonStr, Map.class);
                    result.put(targetField, parsed);
                } catch (Exception e) {
                    log.warn("JSON解析失败: {}", e.getMessage());
                    result.put(targetField, null);
                }
            }
        }
        return result;
    }
}
