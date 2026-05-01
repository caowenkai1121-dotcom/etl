package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.monitor.entity.EtlQualityReport;
import com.etl.monitor.entity.EtlQualityRule;
import com.etl.monitor.quality.QualityChecker;
import com.etl.monitor.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据质量监控Controller
 */
@Tag(name = "数据质量监控", description = "数据质量规则、检查和报告")
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
public class DataQualityController {

    private final QualityService qualityService;

    @Operation(summary = "规则列表")
    @GetMapping("/rule/page")
    public Result<PageResult<EtlQualityRule>> getRules(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long taskId) {
        Page<EtlQualityRule> pageData = qualityService.getRules(pageNum, pageSize, taskId);
        return Result.success(PageResult.of(pageData.getRecords(), pageData.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取规则详情")
    @GetMapping("/rule/{id}")
    public Result<EtlQualityRule> getRule(@PathVariable Long id) {
        return Result.success(qualityService.getRule(id));
    }

    @Operation(summary = "创建规则")
    @PostMapping("/rule")
    public Result<Void> createRule(@RequestBody EtlQualityRule rule) {
        qualityService.createRule(rule);
        return Result.success();
    }

    @Operation(summary = "更新规则")
    @PutMapping("/rule/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody EtlQualityRule rule) {
        rule.setId(id);
        qualityService.updateRule(rule);
        return Result.success();
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        qualityService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "启停规则")
    @PostMapping("/rule/{id}/toggle")
    public Result<Void> toggleRule(@PathVariable Long id, @RequestParam Integer enabled) {
        qualityService.toggleRule(id, enabled);
        return Result.success();
    }

    @Operation(summary = "执行质量校验")
    @PostMapping("/check/{taskId}")
    public Result<QualityChecker.QualityResult> checkQuality(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long executionId,
            @RequestBody List<Map<String, Object>> data) {
        QualityChecker.QualityResult result = qualityService.checkQuality(taskId, executionId, data);
        qualityService.generateReport(taskId, executionId, result);
        return Result.success(result);
    }

    @Operation(summary = "质量报告列表")
    @GetMapping("/report")
    public Result<PageResult<EtlQualityReport>> getReports(
            @RequestParam(required = false) Long taskId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<EtlQualityReport> pageData = qualityService.getReports(taskId, pageNum, pageSize);
        return Result.success(PageResult.of(pageData.getRecords(), pageData.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取质量评分")
    @GetMapping("/score/{taskId}")
    public Result<BigDecimal> getQualityScore(@PathVariable Long taskId) {
        return Result.success(qualityService.getQualityScore(taskId));
    }

    @Operation(summary = "质量趋势")
    @GetMapping("/trend/{taskId}")
    public Result<List<EtlQualityReport>> getQualityTrend(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(qualityService.getQualityTrend(taskId, days));
    }
}
