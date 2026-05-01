package com.etl.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.monitor.entity.EtlQualityRule;
import com.etl.monitor.entity.EtlQualityReport;
import com.etl.monitor.mapper.QualityRuleMapper;
import com.etl.monitor.mapper.QualityReportMapper;
import com.etl.monitor.quality.QualityChecker;
import com.etl.monitor.quality.QualityChecker.QualityResult;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据质量服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityService {

    private final QualityRuleMapper qualityRuleMapper;
    private final QualityReportMapper qualityReportMapper;
    private final QualityChecker qualityChecker = new QualityChecker();

    // 规则管理

    public Page<EtlQualityRule> getRules(int pageNum, int pageSize, Long taskId) {
        Page<EtlQualityRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EtlQualityRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EtlQualityRule::getCreatedAt);
        return qualityRuleMapper.selectPage(page, wrapper);
    }

    public EtlQualityRule getRule(Long id) {
        return qualityRuleMapper.selectById(id);
    }

    public void createRule(EtlQualityRule rule) {
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        qualityRuleMapper.insert(rule);
    }

    public void updateRule(EtlQualityRule rule) {
        rule.setUpdatedAt(LocalDateTime.now());
        qualityRuleMapper.updateById(rule);
    }

    public void deleteRule(Long id) {
        qualityRuleMapper.deleteById(id);
    }

    public void toggleRule(Long id, Integer enabled) {
        EtlQualityRule rule = new EtlQualityRule();
        rule.setId(id);
        rule.setEnabled(enabled);
        rule.setUpdatedAt(LocalDateTime.now());
        qualityRuleMapper.updateById(rule);
    }

    // 质量检查

    public QualityResult checkQuality(Long taskId, Long executionId, List<Map<String, Object>> data) {
        List<EtlQualityRule> rules = qualityRuleMapper.selectList(
            new LambdaQueryWrapper<EtlQualityRule>()
                .eq(taskId != null, EtlQualityRule::getTaskId, taskId)
                .eq(EtlQualityRule::getEnabled, 1)
        );

        int total = data.size();
        int pass = 0;
        List<Map<String, Object>> failSamples = new java.util.ArrayList<>();

        for (Map<String, Object> record : data) {
            boolean allPass = true;
            for (EtlQualityRule rule : rules) {
                Map<String, Object> config;
                try {
                    config = rule.getRuleConfig() != null && !rule.getRuleConfig().isEmpty()
                        ? JSON.parseObject(rule.getRuleConfig(), Map.class)
                        : new java.util.HashMap<>();
                } catch (Exception e) {
                    log.warn("解析规则配置失败: ruleId={}", rule.getId(), e);
                    config = new java.util.HashMap<>();
                }
                if (!qualityChecker.evaluate(rule.getRuleType(), config, record)) {
                    allPass = false;
                    break;
                }
            }
            if (allPass) {
                pass++;
            } else if (failSamples.size() < 100) {
                failSamples.add(record);
            }
        }

        int fail = total - pass;
        double score = total > 0 ? (pass * 100.0 / total) : 100;
        return new QualityResult(score, total, pass, fail, failSamples);
    }

    // 报告管理

    public void generateReport(Long taskId, Long executionId, QualityResult result) {
        EtlQualityReport report = new EtlQualityReport();
        report.setTaskId(taskId);
        report.setExecutionId(executionId);
        report.setQualityScore(BigDecimal.valueOf(result.score()));
        report.setTotalCount((long) result.total());
        report.setPassCount((long) result.pass());
        report.setFailCount((long) result.fail());
        report.setFailSamples(JSON.toJSONString(result.failSamples()));
        report.setReportTime(LocalDateTime.now());
        qualityReportMapper.insert(report);
    }

    public Page<EtlQualityReport> getReports(Long taskId, int pageNum, int pageSize) {
        Page<EtlQualityReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EtlQualityReport> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(EtlQualityReport::getTaskId, taskId);
        }
        wrapper.orderByDesc(EtlQualityReport::getReportTime);
        return qualityReportMapper.selectPage(page, wrapper);
    }

    // 质量统计

    public BigDecimal getQualityScore(Long taskId) {
        EtlQualityReport latestReport = qualityReportMapper.selectOne(
            new LambdaQueryWrapper<EtlQualityReport>()
                .eq(EtlQualityReport::getTaskId, taskId)
                .orderByDesc(EtlQualityReport::getReportTime)
                .last("LIMIT 1")
        );
        return latestReport != null ? latestReport.getQualityScore() : BigDecimal.valueOf(100);
    }

    public List<EtlQualityReport> getQualityTrend(Long taskId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        return qualityReportMapper.selectList(
            new LambdaQueryWrapper<EtlQualityReport>()
                .eq(EtlQualityReport::getTaskId, taskId)
                .ge(EtlQualityReport::getReportTime, startTime)
                .orderByAsc(EtlQualityReport::getReportTime)
        );
    }
}
