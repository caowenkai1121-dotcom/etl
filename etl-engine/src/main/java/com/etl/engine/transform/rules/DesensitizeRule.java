package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据脱敏规则
 */
@Slf4j
@Component
public class DesensitizeRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.DESENSITIZE;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String field = (String) config.get("field");
        String type = (String) config.getOrDefault("type", "PHONE");
        String targetField = (String) config.getOrDefault("targetField", field);

        if (field != null && result.containsKey(field)) {
            Object value = result.get(field);
            if (value != null) {
                String desensitized = desensitize(value.toString(), type);
                result.put(targetField, desensitized);
            }
        }
        return result;
    }

    private String desensitize(String input, String type) {
        if (input == null || input.isEmpty()) return input;

        switch (type.toUpperCase()) {
            case "PHONE":
                return desensitizePhone(input);
            case "ID_CARD":
                return desensitizeIdCard(input);
            case "NAME":
                return desensitizeName(input);
            case "EMAIL":
                return desensitizeEmail(input);
            default:
                return desensitizePhone(input);
        }
    }

    private String desensitizePhone(String phone) {
        if (phone.length() >= 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard.length() >= 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        }
        return idCard;
    }

    private String desensitizeName(String name) {
        if (name.length() <= 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    private String desensitizeEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
