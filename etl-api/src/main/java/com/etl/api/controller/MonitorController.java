package com.etl.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.datasource.connector.ConnectionPoolManager;
import com.etl.datasource.metadata.MetadataCacheManager;
import com.etl.engine.concurrent.ThreadPoolManager;
import com.etl.engine.dto.TaskExecutionResponse;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.service.TaskExecutionManager;
import com.etl.engine.service.TaskExecutionService;
import com.etl.monitor.dto.AlertRuleRequest;
import com.etl.monitor.entity.EtlAlertRecord;
import com.etl.monitor.entity.EtlAlertRule;
import com.etl.monitor.service.AlertRuleService;
import com.etl.monitor.service.MonitorService;
import com.etl.monitor.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 监控Controller
 */
@Tag(name = "监控管理", description = "任务执行监控和统计")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final TaskExecutionService taskExecutionService;
    private final TaskExecutionManager taskExecutionManager;
    private final StatisticsService statisticsService;
    private final AlertRuleService alertRuleService;
    private final ThreadPoolManager threadPoolManager;
    private final MetadataCacheManager metadataCacheManager;

    @Operation(summary = "获取系统概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 今日统计
        StatisticsService.DailyStatistics today = statisticsService.getDailyStatistics(LocalDate.now());
        overview.put("todayExecutions", today.getTotalExecutions());
        overview.put("todaySuccess", today.getSuccessCount());
        overview.put("todayFailed", today.getFailedCount());
        overview.put("todaySuccessRows", today.getSuccessRows());
        overview.put("todaySuccessRate", today.getSuccessRate());
        overview.put("runningTasks", (int) taskExecutionManager.getRunningExecutions().values().stream()
                .filter(TaskExecutionManager.ExecutionInfo::isRunning)
                .count());

        // 今日告警数
        overview.put("alertCount", alertRuleService.countTodayAlerts());

        // 系统性能概览
        StatisticsService.SystemPerformanceOverview systemOverview = statisticsService.getSystemPerformanceOverview();
        overview.put("weekTotalRows", systemOverview.getWeekTotalRows());
        overview.put("weekSuccessRows", systemOverview.getWeekSuccessRows());
        overview.put("avgDailyRows", systemOverview.getAvgDailyRows());

        return Result.success(overview);
    }

    @Operation(summary = "获取执行趋势")
    @GetMapping("/trend")
    public Result<?> getTrend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(statisticsService.getSuccessRateTrend(days));
    }

    @Operation(summary = "获取任务性能排行")
    @GetMapping("/performance")
    public Result<List<StatisticsService.TaskPerformance>> getPerformanceRank(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        return Result.success(statisticsService.getTaskPerformanceRank(limit, startDate, endDate));
    }

    @Operation(summary = "分页查询执行记录")
    @GetMapping("/execution/page")
    public Result<PageResult<EtlTaskExecution>> executionPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        Page<EtlTaskExecution> page = taskExecutionService.pageList(pageNum, pageSize, taskId, status);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取执行详情")
    @GetMapping("/execution/{id}")
    public Result<TaskExecutionResponse> getExecutionDetail(@PathVariable Long id) {
        return Result.success(monitorService.getExecutionDetail(id));
    }

    @Operation(summary = "清理历史数据")
    @PostMapping("/clean")
    public Result<Integer> cleanHistory(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(monitorService.cleanHistoryLogs(days));
    }

    // ==================== 告警规则管理 ====================

    @Operation(summary = "分页查询告警规则")
    @GetMapping("/alert/rule/page")
    public Result<PageResult<EtlAlertRule>> getAlertRulePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<EtlAlertRule> page = alertRuleService.pageRules(pageNum, pageSize);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取告警规则详情")
    @GetMapping("/alert/rule/{id}")
    public Result<EtlAlertRule> getAlertRule(@PathVariable Long id) {
        return Result.success(alertRuleService.getRuleById(id));
    }

    @Operation(summary = "创建告警规则")
    @PostMapping("/alert/rule")
    public Result<Void> createAlertRule(@Valid @RequestBody AlertRuleRequest request) {
        EtlAlertRule rule = new EtlAlertRule();
        rule.setName(request.getName());
        rule.setAlertType(request.getAlertType());
        rule.setDescription(request.getDescription());
        rule.setConditionExpr(request.getConditionExpr());
        rule.setSeverity(request.getSeverity());
        rule.setChannels(request.getChannels());
        rule.setRecipients(request.getRecipients());
        rule.setEnabled(request.getEnabled());
        rule.setSilenceMinutes(request.getSilenceMinutes());
        alertRuleService.createRule(rule);
        return Result.success();
    }

    @Operation(summary = "更新告警规则")
    @PutMapping("/alert/rule/{id}")
    public Result<Void> updateAlertRule(@PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        EtlAlertRule rule = new EtlAlertRule();
        rule.setId(id);
        rule.setName(request.getName());
        rule.setAlertType(request.getAlertType());
        rule.setDescription(request.getDescription());
        rule.setConditionExpr(request.getConditionExpr());
        rule.setSeverity(request.getSeverity());
        rule.setChannels(request.getChannels());
        rule.setRecipients(request.getRecipients());
        rule.setEnabled(request.getEnabled());
        rule.setSilenceMinutes(request.getSilenceMinutes());
        alertRuleService.updateRule(rule);
        return Result.success();
    }

    @Operation(summary = "删除告警规则")
    @DeleteMapping("/alert/rule/{id}")
    public Result<Void> deleteAlertRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用告警规则")
    @PutMapping("/alert/rule/{id}/toggle")
    public Result<Void> toggleAlertRule(@PathVariable Long id, @RequestParam Integer enabled) {
        alertRuleService.toggleRule(id, enabled);
        return Result.success();
    }

    // ==================== 告警记录管理 ====================

    @Operation(summary = "分页查询告警记录")
    @GetMapping("/alert/record/page")
    public Result<PageResult<EtlAlertRecord>> getAlertRecordPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {
        Page<EtlAlertRecord> page = alertRuleService.pageRecords(pageNum, pageSize, alertType, severity, status);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取最近告警")
    @GetMapping("/alerts/recent")
    public Result<List<EtlAlertRecord>> getRecentAlerts(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(alertRuleService.getRecentAlerts(limit));
    }

    @Operation(summary = "获取告警记录详情")
    @GetMapping("/alert/record/{id}")
    public Result<EtlAlertRecord> getAlertRecord(@PathVariable Long id) {
        return Result.success(alertRuleService.getRecordById(id));
    }

    @Operation(summary = "忽略告警")
    @PutMapping("/alert/record/{id}/ignore")
    public Result<Void> ignoreAlert(@PathVariable Long id) {
        alertRuleService.updateRecordStatus(id, "IGNORED");
        return Result.success();
    }

    @Operation(summary = "解决告警")
    @PutMapping("/alert/record/{id}/resolve")
    public Result<Void> resolveAlert(@PathVariable Long id) {
        alertRuleService.updateRecordStatus(id, "RESOLVED");
        return Result.success();
    }

    // ==================== 运维监控增强接口 ====================

    @Operation(summary = "获取所有连接池状态")
    @GetMapping("/pool-status")
    public Result<Map<String, Map<String, Object>>> getPoolStatus() {
        return Result.success(ConnectionPoolManager.getAllPoolStatus());
    }

    @Operation(summary = "获取线程池状态")
    @GetMapping("/thread-pool-status")
    public Result<Map<String, Object>> getThreadPoolStatus() {
        return Result.success(threadPoolManager.getSyncPoolStatus());
    }

    @Operation(summary = "获取缓存命中率和大小")
    @GetMapping("/cache-status")
    public Result<Map<String, Object>> getCacheStatus() {
        return Result.success(metadataCacheManager.getCacheStats());
    }

    @Operation(summary = "获取系统信息")
    @GetMapping("/system-info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        File file = new File("/");

        systemInfo.put("jvmMaxMemory", runtime.maxMemory());
        systemInfo.put("jvmUsedMemory", runtime.totalMemory() - runtime.freeMemory());
        systemInfo.put("jvmFreeMemory", runtime.freeMemory());
        systemInfo.put("availableProcessors", runtime.availableProcessors());
        systemInfo.put("diskTotal", file.getTotalSpace());
        systemInfo.put("diskFree", file.getFreeSpace());
        systemInfo.put("uptime", runtimeMXBean.getUptime());

        return Result.success(systemInfo);
    }

    @Operation(summary = "获取实时任务状态")
    @GetMapping("/realtime-tasks")
    public Result<List<Map<String, Object>>> getRealtimeTasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();

        // 获取运行中的任务
        taskExecutionManager.getRunningExecutions().forEach((id, info) -> {
            Map<String, Object> task = new HashMap<>();
            task.put("id", id);
            task.put("taskName", "任务-" + id);
            task.put("status", info.isRunning() ? "RUNNING" : "PENDING");
            task.put("progress", (int)(info.getEngine().getProgress() * 100));
            task.put("syncRows", info.getContext().getSuccessRows());
            task.put("startTime", info.getExecution().getStartTime());
            task.put("duration", info.getDuration());
            tasks.add(task);
        });

        // 获取最近完成的任务（最多5个）
        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EtlTaskExecution::getStatus, "SUCCESS", "FAILED")
               .orderByDesc(EtlTaskExecution::getEndTime)
               .last("LIMIT 5");

        List<EtlTaskExecution> recentCompleted = taskExecutionService.list(wrapper);
        for (EtlTaskExecution exec : recentCompleted) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", exec.getId());
            task.put("taskName", "任务-" + exec.getTaskId());
            task.put("status", exec.getStatus());
            task.put("syncRows", exec.getSuccessRows());
            task.put("startTime", exec.getStartTime());
            task.put("endTime", exec.getEndTime());
            if (exec.getDuration() != null) {
                task.put("duration", exec.getDuration());
            }
            tasks.add(task);
        }

        return Result.success(tasks);
    }

    @Operation(summary = "获取资源使用率")
    @GetMapping("/resource")
    public Result<Map<String, Object>> getResource() {
        Map<String, Object> resource = new HashMap<>();

        // 内存使用率
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        int memoryUsage = (int) ((usedMemory * 100) / maxMemory);
        resource.put("memory", memoryUsage);

        // 连接池使用率
        Map<String, Map<String, Object>> poolStatus = ConnectionPoolManager.getAllPoolStatus();
        int totalActive = 0;
        int totalMax = 0;
        for (Map<String, Object> pool : poolStatus.values()) {
            totalActive += (Integer) pool.getOrDefault("activeConnections", 0);
            totalMax += (Integer) pool.getOrDefault("maxConnections", 10);
        }
        int connectionUsage = totalMax > 0 ? (totalActive * 100) / totalMax : 0;
        resource.put("connection", connectionUsage);

        // CPU使用率（通过线程池活跃线程数模拟）
        Map<String, Object> threadPoolStatus = threadPoolManager.getSyncPoolStatus();
        int activeThreads = (Integer) threadPoolStatus.getOrDefault("activeThreads", 0);
        int maxThreads = (Integer) threadPoolStatus.getOrDefault("maximumPoolSize", 10);
        // 模拟CPU负载，结合线程使用率
        int cpuUsage = (activeThreads * 100) / Math.max(maxThreads, 1);
        resource.put("cpu", Math.min(cpuUsage, 100));

        return Result.success(resource);
    }
}
