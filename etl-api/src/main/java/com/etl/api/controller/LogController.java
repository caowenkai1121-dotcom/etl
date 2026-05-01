package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlSyncLog;
import com.etl.monitor.entity.EtlSyncLogDetail;
import com.etl.monitor.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志Controller
 */
@Tag(name = "日志管理", description = "同步日志查询")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(summary = "分页查询日志")
    @GetMapping("/page")
    public Result<PageResult<EtlSyncLog>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "执行ID") @RequestParam(required = false) Long executionId,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "日志类型") @RequestParam(required = false) String logType,
            @Parameter(description = "表名") @RequestParam(required = false) String tableName,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<EtlSyncLog> page = logService.pageList(pageNum, pageSize, taskId, executionId, level, logType, tableName, startTime, endTime);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "日志概览统计")
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> getStats(
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Map<String, Object> stats = logService.getStats(startTime, endTime);
        return Result.success(stats);
    }

    @Operation(summary = "按阶段统计日志")
    @GetMapping("/stats/by-stage")
    public Result<List<Map<String, Object>>> getStatsByStage(
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId) {
        return Result.success(logService.getStatsByStage(taskId));
    }

    @Operation(summary = "按规则统计日志")
    @GetMapping("/stats/by-rule")
    public Result<List<Map<String, Object>>> getStatsByRule(
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId) {
        return Result.success(logService.getStatsByRule(taskId));
    }

    @Operation(summary = "错误趋势")
    @GetMapping("/stats/error-trend")
    public Result<List<Map<String, Object>>> getErrorTrend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(logService.getErrorTrend(days));
    }

    @Operation(summary = "按TraceID查询链路日志")
    @GetMapping("/trace/{traceId}")
    public Result<List<EtlSyncLog>> queryByTraceId(@Parameter(description = "跟踪ID") @PathVariable String traceId) {
        return Result.success(logService.queryByTraceId(traceId));
    }

    @Operation(summary = "导出日志")
    @GetMapping("/export")
    public void exportLog(HttpServletResponse response,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) throws Exception {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=logs.xlsx");
        // 实际导出逻辑
        logService.exportLog(response.getOutputStream(), taskId, level, startTime, endTime);
    }

    @Operation(summary = "查询转换详情")
    @GetMapping("/detail")
    public Result<List<EtlSyncLogDetail>> queryDetail(
            @Parameter(description = "日志ID") @RequestParam(required = false) Long logId,
            @Parameter(description = "步骤代码") @RequestParam(required = false) String stepCode) {
        return Result.success(logService.queryDetail(logId, stepCode));
    }

    @Operation(summary = "手动归档日志")
    @PostMapping("/archive")
    public Result<Integer> archive(
            @Parameter(description = "归档多少天前的日志") @RequestParam(defaultValue = "30") Integer beforeDays) {
        return Result.success(logService.archive(beforeDays));
    }

    @Operation(summary = "多维组合查询日志")
    @GetMapping("/multi-condition")
    public Result<PageResult<EtlSyncLog>> multiConditionQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "模块/阶段名称") @RequestParam(required = false) String module,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        Page<EtlSyncLog> page = logService.multiConditionQuery(taskId, startTime, endTime, level, module, keyword, pageNum, pageSize);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }
}
