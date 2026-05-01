package com.etl.monitor.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AlertCoordinator {
    private final Map<Long, Long> lastAlertTime = new ConcurrentHashMap<>();

    public boolean shouldAlert(Long ruleId, int configuredCooldown) {
        long now = System.currentTimeMillis();
        Long lastTime = lastAlertTime.get(ruleId);
        if (lastTime != null && (now - lastTime) < configuredCooldown * 60 * 1000L) {
            log.debug("[Alert] 规则{}在冷却期内, 跳过", ruleId);
            return false;
        }
        lastAlertTime.put(ruleId, now);
        return true;
    }
}
