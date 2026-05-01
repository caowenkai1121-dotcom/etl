package com.etl.monitor.quality;

import com.etl.monitor.entity.EtlQualityRule;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class QualityChecker {
    public QualityResult check(EtlQualityRule rule, List<Map<String, Object>> data) {
        int total = data.size();
        int pass = 0;
        List<Map<String, Object>> failSamples = new ArrayList<>();

        if (rule.getRuleConfig() == null || rule.getRuleConfig().isEmpty()) {
            log.warn("规则配置为空: ruleId={}", rule.getId());
            return new QualityResult(100, total, total, 0, failSamples);
        }

        Map<String, Object> config;
        try {
            config = JSON.parseObject(rule.getRuleConfig(), Map.class);
        } catch (Exception e) {
            log.error("解析规则配置失败: ruleId={}, config={}", rule.getId(), rule.getRuleConfig(), e);
            return new QualityResult(0, total, 0, total, failSamples);
        }

        for (Map<String, Object> record : data) {
            if (evaluate(rule.getRuleType(), config, record)) {
                pass++;
            } else if (failSamples.size() < 100) {
                failSamples.add(record);
            }
        }
        int fail = total - pass;
        double score = total > 0 ? (pass * 100.0 / total) : 100;
        return new QualityResult(score, total, pass, fail, failSamples);
    }

    public boolean evaluate(String ruleType, Map<String, Object> config, Map<String, Object> record) {
        String fieldName = (String) config.get("field");
        if (fieldName == null) {
            return true;
        }
        Object value = record.get(fieldName);
        return switch (ruleType) {
            case "NOT_NULL" -> value != null && !value.toString().isEmpty();
            case "UNIQUE" -> true;
            case "RANGE" -> checkRange(value, config);
            case "REGEX" -> {
                String pattern = (String) config.get("pattern");
                if (pattern == null || value == null) yield false;
                yield value.toString().matches(pattern);
            }
            case "ENUM" -> {
                List<?> values = (List<?>) config.get("values");
                yield values != null && values.contains(value);
            }
            case "FK_EXISTS" -> true;
            case "UPDATE_FREQ" -> true;
            default -> true;
        };
    }

    public boolean checkRange(Object value, Map<String, Object> config) {
        if (value == null) return false;
        try {
            double num = Double.parseDouble(value.toString());
            double min = config.containsKey("min") ? Double.parseDouble(config.get("min").toString()) : Double.MIN_VALUE;
            double max = config.containsKey("max") ? Double.parseDouble(config.get("max").toString()) : Double.MAX_VALUE;
            return num >= min && num <= max;
        } catch (NumberFormatException e) { return false; }
    }

    public record QualityResult(double score, int total, int pass, int fail, List<Map<String, Object>> failSamples) {}
}
