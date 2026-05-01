package com.etl.engine.transform;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据脱敏器
 */
@Component
public class DataDesensitizer {

    /**
     * 脱敏数据
     *
     * @param data 原始数据
     * @param rules 脱敏规则
     * @return 脱敏后的数据
     */
    public List<Map<String, Object>> desensitize(List<Map<String, Object>> data, List<DesensitizeRule> rules) {
        if (data == null || data.isEmpty() || rules == null || rules.isEmpty()) {
            return data;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> desensitizedRow = new HashMap<>(row);

            for (DesensitizeRule rule : rules) {
                Object value = desensitizedRow.get(rule.getField());
                if (value != null && value instanceof String) {
                    String desensitizedValue = applyDesensitization((String) value, rule);
                    desensitizedRow.put(rule.getField(), desensitizedValue);
                }
            }

            result.add(desensitizedRow);
        }

        return result;
    }

    /**
     * 应用脱敏规则
     */
    private String applyDesensitization(String value, DesensitizeRule rule) {
        switch (rule.getStrategy()) {
            case PHONE:
                return desensitizePhone(value);
            case ID_CARD:
                return desensitizeIdCard(value);
            case EMAIL:
                return desensitizeEmail(value);
            case NAME:
                return desensitizeName(value);
            case CUSTOM:
                return applyCustomDesensitization(value, rule);
            default:
                return value;
        }
    }

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard == null) {
            return idCard;
        }
        int length = idCard.length();
        if (length <= 6) {
            return idCard;
        }
        if (length <= 10) {
            return idCard.substring(0, 4) + "****" + idCard.substring(length - 4);
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(length - 4);
    }

    private String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 1) {
            return username + "***@" + domain;
        }
        if (username.length() == 2) {
            return username.charAt(0) + "***@" + domain;
        }
        return username.substring(0, 1) + "***" + username.substring(username.length() - 1) + "@" + domain;
    }

    private String desensitizeName(String name) {
        if (name == null) {
            return name;
        }
        int length = name.length();
        if (length == 1) {
            return name;
        }
        if (length == 2) {
            return name.charAt(0) + "*";
        }
        if (length == 3) {
            return name.charAt(0) + "**" + name.charAt(2);
        }
        return name.charAt(0) + "**" + name.substring(length - 1);
    }

    private String applyCustomDesensitization(String value, DesensitizeRule rule) {
        if (rule.getCustomPattern() != null) {
            return value.replaceAll(rule.getCustomPattern(), "****");
        }
        if (rule.getRetainLength() > 0) {
            int retainLength = Math.min(rule.getRetainLength(), value.length());
            int maskLength = value.length() - retainLength;
            if (maskLength > 0) {
                return value.substring(0, retainLength) + "*".repeat(maskLength);
            }
        }
        return value;
    }

    /**
     * 脱敏规则
     */
    @Data
    public static class DesensitizeRule {
        /**
         * 字段名
         */
        private String field;

        /**
         * 脱敏策略
         */
        private Strategy strategy;

        /**
         * 自定义正则表达式（用于CUSTOM策略）
         */
        private String customPattern;

        /**
         * 保留长度（用于CUSTOM策略）
         */
        private int retainLength;

        /**
         * 脱敏策略枚举
         */
        public enum Strategy {
            PHONE,
            ID_CARD,
            EMAIL,
            NAME,
            CUSTOM
        }

        public static DesensitizeRule of(String field, Strategy strategy) {
            DesensitizeRule rule = new DesensitizeRule();
            rule.setField(field);
            rule.setStrategy(strategy);
            return rule;
        }
    }
}
