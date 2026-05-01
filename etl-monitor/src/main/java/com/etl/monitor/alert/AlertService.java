package com.etl.monitor.alert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.event.AlertEvent;
import com.etl.monitor.entity.EtlAlertRecord;
import com.etl.monitor.entity.EtlAlertRule;
import com.etl.monitor.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 增强告警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleEngine alertRuleEngine;
    private final AlertCoordinator alertCoordinator;
    private final NotificationDispatcher notificationDispatcher;
    private final AlertRuleService alertRuleService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 评估规则并触发告警
     */
    public void evaluateAndAlert(Long ruleId, double currentValue) {
        EtlAlertRule rule = alertRuleService.getRuleById(ruleId);
        if (rule == null || rule.getEnabled() != 1) {
            return;
        }

        boolean shouldAlert = alertRuleEngine.evaluate(rule, currentValue);
        if (shouldAlert) {
            int cooldownMinutes = rule.getCooldownMinutes() != null ? rule.getCooldownMinutes() : 5;
            if (alertCoordinator.shouldAlert(ruleId, cooldownMinutes)) {
                String title = "告警: " + rule.getName();
                String message = String.format("规则【%s】触发告警，当前值: %.2f", rule.getName(), currentValue);
                notificationDispatcher.dispatch(rule.getNotificationChannels(), title, message);
                eventPublisher.publishEvent(new AlertEvent(this, ruleId, rule.getAlertType(), message, rule.getSeverity()));
                log.info("告警触发: ruleId={}, name={}, currentValue={}", ruleId, rule.getName(), currentValue);
            }
        }
    }

    /**
     * 获取告警规则列表
     */
    public Page<EtlAlertRule> getAlertRules(int pageNum, int pageSize) {
        return alertRuleService.pageRules(pageNum, pageSize);
    }

    /**
     * 创建告警规则
     */
    public void createRule(EtlAlertRule rule) {
        alertRuleService.createRule(rule);
    }

    /**
     * 更新告警规则
     */
    public void updateRule(EtlAlertRule rule) {
        alertRuleService.updateRule(rule);
    }

    /**
     * 删除告警规则
     */
    public void deleteRule(Long id) {
        alertRuleService.deleteRule(id);
    }

    /**
     * 启停告警规则
     */
    public void toggleRule(Long id, Integer enabled) {
        alertRuleService.toggleRule(id, enabled);
    }

    /**
     * 获取告警记录列表
     */
    public Page<EtlAlertRecord> getAlertRecords(int pageNum, int pageSize, String alertType, String severity, String status) {
        return alertRuleService.pageRecords(pageNum, pageSize, alertType, severity, status);
    }

    /**
     * 确认告警
     */
    public void ackRecord(Long id) {
        alertRuleService.updateRecordStatus(id, "ACKNOWLEDGED");
    }
}
