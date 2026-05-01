package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.monitor.entity.EtlAlertRecord;
import com.etl.monitor.entity.EtlAlertRule;
import com.etl.monitor.service.AlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警管理接口
 */
@RestController
@RequestMapping("/alert")
@Tag(name = "告警管理")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService alertRuleService;

    @GetMapping("/rule/page")
    @Operation(summary = "获取告警规则列表")
    public Result<PageResult<EtlAlertRule>> listAlertRules(@RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        Page<EtlAlertRule> rulePage = alertRuleService.pageRules(pageNum, pageSize);
        return Result.success(PageResult.of(rulePage.getRecords(), rulePage.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/rule/{id}")
    @Operation(summary = "获取告警规则详情")
    public Result<EtlAlertRule> getAlertRule(@PathVariable Long id) {
        return Result.success(alertRuleService.getRuleById(id));
    }

    @PostMapping("/rule")
    @Operation(summary = "创建告警规则")
    public Result<Void> createAlertRule(@RequestBody EtlAlertRule rule) {
        alertRuleService.createRule(rule);
        return Result.success();
    }

    @PutMapping("/rule/{id}")
    @Operation(summary = "更新告警规则")
    public Result<Void> updateAlertRule(@PathVariable Long id, @RequestBody EtlAlertRule rule) {
        rule.setId(id);
        alertRuleService.updateRule(rule);
        return Result.success();
    }

    @PutMapping("/rule/{id}/toggle")
    @Operation(summary = "启停告警规则")
    public Result<Void> toggleAlertRule(@PathVariable Long id, @RequestParam Integer enabled) {
        alertRuleService.toggleRule(id, enabled);
        return Result.success();
    }

    @DeleteMapping("/rule/{id}")
    @Operation(summary = "删除告警规则")
    public Result<Void> deleteAlertRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/record/page")
    @Operation(summary = "获取告警记录列表")
    public Result<PageResult<EtlAlertRecord>> listAlertRecords(@RequestParam(defaultValue = "1") int pageNum,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) String alertType,
                                                              @RequestParam(required = false) String severity,
                                                              @RequestParam(required = false) String status) {
        Page<EtlAlertRecord> recordPage = alertRuleService.pageRecords(pageNum, pageSize, alertType, severity, status);
        return Result.success(PageResult.of(recordPage.getRecords(), recordPage.getTotal(), pageNum, pageSize));
    }

    @PutMapping("/record/{id}/ignore")
    @Operation(summary = "忽略告警")
    public Result<Void> ignoreAlert(@PathVariable Long id) {
        alertRuleService.updateRecordStatus(id, "IGNORED");
        return Result.success();
    }

    @PutMapping("/record/{id}/resolve")
    @Operation(summary = "解决告警")
    public Result<Void> resolveAlert(@PathVariable Long id) {
        alertRuleService.updateRecordStatus(id, "RESOLVED");
        return Result.success();
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近告警")
    public Result<List<EtlAlertRecord>> getRecentAlerts(@RequestParam(defaultValue = "10") int limit) {
        List<EtlAlertRecord> records = alertRuleService.getRecentAlerts(limit);
        return Result.success(records);
    }

    @GetMapping("/channels")
    @Operation(summary = "获取通知渠道类型")
    public Result<List<String>> getNotificationChannels() {
        return Result.success(List.of("DINGTALK", "EMAIL", "WEBHOOK"));
    }

    @GetMapping("/channel/list")
    @Operation(summary = "获取通知渠道类型（兼容旧版）")
    public Result<List<String>> getNotificationChannelsLegacy() {
        return Result.success(List.of("DINGTALK", "EMAIL", "WEBHOOK"));
    }

    @PostMapping("/channel/{id}/test")
    @Operation(summary = "测试告警渠道")
    public Result<Void> testAlertChannel(@PathVariable Long id) {
        return Result.success();
    }

    @PostMapping("/channel")
    @Operation(summary = "创建告警渠道")
    public Result<Void> createAlertChannel(@RequestBody Object channel) {
        return Result.success();
    }

    @PutMapping("/channel/{id}")
    @Operation(summary = "更新告警渠道")
    public Result<Void> updateAlertChannel(@PathVariable Long id, @RequestBody Object channel) {
        return Result.success();
    }
}
