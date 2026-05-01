package com.etl.engine.transform.rules;

import com.etl.common.enums.TransformRuleType;
import com.etl.engine.transform.TransformRuleFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件分支规则
 */
@Slf4j
@Component
public class ConditionBranchRule implements TransformRuleFactory.TransformRule {

    @PostConstruct
    public void init() {
        TransformRuleFactory.register(getType(), this);
    }

    @Override
    public TransformRuleType getType() {
        return TransformRuleType.CONDITION_BRANCH;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> record, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>(record);
        String conditionField = (String) config.get("conditionField");
        String operator = (String) config.getOrDefault("operator", "EQUALS");
        Object conditionValue = config.get("conditionValue");
        String targetField = (String) config.get("targetField");
        Object trueValue = config.get("trueValue");
        Object falseValue = config.get("falseValue");

        if (conditionField != null && targetField != null) {
            boolean matches = evaluateCondition(result.get(conditionField), operator, conditionValue);
            result.put(targetField, matches ? trueValue : falseValue);
        }
        return result;
    }

    private boolean evaluateCondition(Object fieldValue, String operator, Object conditionValue) {
        if (fieldValue == null) {
            return conditionValue == null;
        }

        String fieldStr = fieldValue.toString();
        String conditionStr = conditionValue != null ? conditionValue.toString() : "";

        switch (operator.toUpperCase()) {
            case "EQUALS":
                return fieldStr.equals(conditionStr);
            case "NOT_EQUALS":
                return !fieldStr.equals(conditionStr);
            case "CONTAINS":
                return fieldStr.contains(conditionStr);
            case "STARTS_WITH":
                return fieldStr.startsWith(conditionStr);
            case "ENDS_WITH":
                return fieldStr.endsWith(conditionStr);
            case "GREATER_THAN":
                try {
                    return Double.parseDouble(fieldStr) > Double.parseDouble(conditionStr);
                } catch (NumberFormatException e) {
                    return false;
                }
            case "LESS_THAN":
                try {
                    return Double.parseDouble(fieldStr) < Double.parseDouble(conditionStr);
                } catch (NumberFormatException e) {
                    return false;
                }
            case "NOT_NULL":
                return fieldValue != null && !fieldStr.isEmpty();
            default:
                return fieldStr.equals(conditionStr);
        }
    }
}
