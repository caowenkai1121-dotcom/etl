package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 日期格式转换规则
 */
@Slf4j
@Component
public class DateFormatRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.DATE_FORMAT;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String field = (String) config.get("field");
        String sourceFormat = (String) config.getOrDefault("sourceFormat", "yyyy-MM-dd HH:mm:ss");
        String targetFormat = (String) config.getOrDefault("targetFormat", "yyyy-MM-dd");
        String targetField = (String) config.getOrDefault("targetField", field);

        if (field != null && result.containsKey(field)) {
            Object value = result.get(field);
            if (value != null) {
                try {
                    String dateStr = value.toString();
                    DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
                    DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, sourceFormatter);
                    result.put(targetField, dateTime.format(targetFormatter));
                } catch (Exception e) {
                    log.warn("日期格式转换失败: {}", e.getMessage());
                    result.put(targetField, value);
                }
            }
        }
        return result;
    }
}
