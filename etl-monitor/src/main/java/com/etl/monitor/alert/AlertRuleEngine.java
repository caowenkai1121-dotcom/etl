package com.etl.monitor.alert;

import com.etl.common.enums.AlertType;
import com.etl.monitor.entity.EtlAlertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlertRuleEngine {
    public boolean evaluate(EtlAlertRule rule, double currentValue) {
        double threshold = rule.getThreshold() != null ? rule.getThreshold() : 0;
        return switch (AlertType.valueOf(rule.getAlertType())) {
            case THRESHOLD_EXCEEDED -> currentValue > threshold;
            case TASK_FAILED -> currentValue > threshold;
            case SYNC_DELAY -> currentValue > threshold;
            case CONNECTION_ERROR -> currentValue > threshold;
            case SYSTEM_ERROR -> currentValue > threshold;
        };
    }

    private boolean evaluateSmart(EtlAlertRule rule, double currentValue) {
        double mean = rule.getThreshold() != null ? rule.getThreshold() : currentValue;
        double stdDev = mean * 0.2;
        return Math.abs(currentValue - mean) > 2 * stdDev;
    }
}
