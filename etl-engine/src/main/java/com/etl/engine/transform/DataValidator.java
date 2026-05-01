package com.etl.engine.transform;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据校验器（transform包）
 * 用于数据转换过程中的校验
 */
@Component
public class DataValidator {

    /**
     * 校验数据
     *
     * @param data 待校验数据
     * @param rules 校验规则
     * @return 校验结果
     */
    public ValidationResult validate(List<Map<String, Object>> data, List<ValidateRule> rules) {
        ValidationResult result = new ValidationResult();
        result.setValidData(new ArrayList<>());
        result.setSkippedCount(0);
        result.setDefaultAppliedCount(0);

        if (data == null || data.isEmpty()) {
            return result;
        }

        // 如果没有规则，所有数据都通过
        if (rules == null || rules.isEmpty()) {
            result.getValidData().addAll(data);
            return result;
        }

        for (Map<String, Object> row : data) {
            Map<String, Object> validatedRow = new HashMap<>(row);
            boolean valid = true;
            boolean useDefault = false;

            for (ValidateRule rule : rules) {
                Object value = validatedRow.get(rule.getField());

                if (!validateValue(value, rule)) {
                    // 校验失败，根据策略处理
                    switch (rule.getOnFailStrategy()) {
                        case SKIP_ROW:
                            valid = false;
                            break;
                        case DEFAULT_VALUE:
                            validatedRow.put(rule.getField(), rule.getDefaultValue());
                            useDefault = true;
                            break;
                        case ABORT:
                            throw new RuntimeException("数据校验失败: 字段=" + rule.getField()
                                + ", 值=" + value + ", 规则=" + rule.getRule());
                    }
                }
            }

            if (valid) {
                result.getValidData().add(validatedRow);
                if (useDefault) {
                    result.setDefaultAppliedCount(result.getDefaultAppliedCount() + 1);
                }
            } else {
                result.setSkippedCount(result.getSkippedCount() + 1);
            }
        }

        return result;
    }

    /**
     * 校验单个值
     */
    private boolean validateValue(Object value, ValidateRule rule) {
        switch (rule.getRule()) {
            case NOT_NULL:
                return value != null;
            case LENGTH:
                if (value == null) return false;
                if (value instanceof String) {
                    int len = ((String) value).length();
                    int min = rule.getParamAsInt("min", 0);
                    int max = rule.getParamAsInt("max", Integer.MAX_VALUE);
                    return len >= min && len <= max;
                }
                return false;
            case RANGE:
                if (value == null) return false;
                if (value instanceof Number) {
                    double num = ((Number) value).doubleValue();
                    double min = rule.getParamAsDouble("min", Double.MIN_VALUE);
                    double max = rule.getParamAsDouble("max", Double.MAX_VALUE);
                    return num >= min && num <= max;
                }
                return false;
            case REGEX:
                if (value == null) return false;
                String pattern = rule.getParamAsString("pattern");
                return pattern != null && value.toString().matches(pattern);
            case ENUM:
                if (value == null) return false;
                List<String> allowedValues = rule.getParamAsStringList("allowed");
                return allowedValues != null && allowedValues.contains(value.toString());
            default:
                return true;
        }
    }

    /**
     * 校验规则
     */
    @Data
    public static class ValidateRule {
        /**
         * 字段名
         */
        private String field;

        /**
         * 校验规则
         */
        private Rule rule;

        /**
         * 校验参数
         */
        private Map<String, Object> params = new HashMap<>();

        /**
         * 失败策略
         */
        private OnFailStrategy onFailStrategy = OnFailStrategy.SKIP_ROW;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 获取参数作为String
         */
        public String getParamAsString(String key) {
            Object value = params.get(key);
            return value != null ? value.toString() : null;
        }

        /**
         * 获取参数作为int
         */
        public int getParamAsInt(String key, int defaultValue) {
            Object value = params.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        /**
         * 获取参数作为double
         */
        public double getParamAsDouble(String key, double defaultValue) {
            Object value = params.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        /**
         * 获取参数作为String列表
         */
        @SuppressWarnings("unchecked")
        public List<String> getParamAsStringList(String key) {
            Object value = params.get(key);
            if (value instanceof List) {
                return (List<String>) value;
            }
            return null;
        }

        public static ValidateRule notNull(String field) {
            ValidateRule rule = new ValidateRule();
            rule.setField(field);
            rule.setRule(Rule.NOT_NULL);
            return rule;
        }

        public static ValidateRule notNull(String field, OnFailStrategy strategy, Object defaultValue) {
            ValidateRule rule = notNull(field);
            rule.setOnFailStrategy(strategy);
            rule.setDefaultValue(defaultValue);
            return rule;
        }
    }

    /**
     * 校验规则枚举
     */
    public enum Rule {
        NOT_NULL,
        LENGTH,
        RANGE,
        REGEX,
        ENUM
    }

    /**
     * 失败策略枚举
     */
    public enum OnFailStrategy {
        SKIP_ROW,
        DEFAULT_VALUE,
        ABORT
    }

    /**
     * 校验结果
     */
    @Data
    public static class ValidationResult {
        /**
         * 有效数据
         */
        private List<Map<String, Object>> validData;

        /**
         * 跳过行数
         */
        private int skippedCount;

        /**
         * 使用默认值的行数
         */
        private int defaultAppliedCount;

        /**
         * 获取有效数据总数
         */
        public int getValidCount() {
            return validData != null ? validData.size() : 0;
        }
    }
}
