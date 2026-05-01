package com.etl.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.monitor.entity.EtlAlertRecord;
import com.etl.monitor.entity.EtlAlertRule;
import com.etl.monitor.mapper.AlertRecordMapper;
import com.etl.monitor.mapper.AlertRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则与记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertRecordMapper alertRecordMapper;

    public Page<EtlAlertRule> pageRules(int pageNum, int pageSize) {
        Page<EtlAlertRule> page = new Page<>(pageNum, pageSize);
        return alertRuleMapper.selectPage(page,
            new LambdaQueryWrapper<EtlAlertRule>().orderByDesc(EtlAlertRule::getCreatedAt));
    }

    public EtlAlertRule getRuleById(Long id) {
        return alertRuleMapper.selectById(id);
    }

    public void createRule(EtlAlertRule rule) {
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        alertRuleMapper.insert(rule);
    }

    public void updateRule(EtlAlertRule rule) {
        rule.setUpdatedAt(LocalDateTime.now());
        alertRuleMapper.updateById(rule);
    }

    public void deleteRule(Long id) {
        alertRuleMapper.deleteById(id);
    }

    public void toggleRule(Long id, Integer enabled) {
        EtlAlertRule rule = new EtlAlertRule();
        rule.setId(id);
        rule.setEnabled(enabled);
        rule.setUpdatedAt(LocalDateTime.now());
        alertRuleMapper.updateById(rule);
    }

    public Page<EtlAlertRecord> pageRecords(int pageNum, int pageSize, String alertType, String severity, String status) {
        Page<EtlAlertRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EtlAlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (alertType != null && !alertType.isEmpty()) wrapper.eq(EtlAlertRecord::getAlertType, alertType);
        if (severity != null && !severity.isEmpty()) wrapper.eq(EtlAlertRecord::getSeverity, severity);
        if (status != null && !status.isEmpty()) wrapper.eq(EtlAlertRecord::getStatus, status);
        wrapper.orderByDesc(EtlAlertRecord::getCreatedAt);
        return alertRecordMapper.selectPage(page, wrapper);
    }

    public List<EtlAlertRecord> getRecentAlerts(int limit) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        return alertRecordMapper.selectRecentAlerts(startTime, limit);
    }

    public EtlAlertRecord getRecordById(Long id) {
        return alertRecordMapper.selectById(id);
    }

    public void updateRecordStatus(Long id, String status) {
        EtlAlertRecord record = new EtlAlertRecord();
        record.setId(id);
        record.setStatus(status);
        record.setResolvedAt(LocalDateTime.now());
        alertRecordMapper.updateById(record);
    }

    public int countTodayAlerts() {
        return alertRecordMapper.countTodayAlerts();
    }
}
