package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.cdc.CdcManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CDC同步任务Controller
 */
@Tag(name = "CDC同步管理", description = "CDC实时同步任务的启动、停止和监控")
@RestController
@RequestMapping("/cdc")
@RequiredArgsConstructor
public class CdcController {

    private final CdcManagerService cdcManagerService;

    @Operation(summary = "启动CDC任务")
    @PostMapping("/task/{taskId}/start")
    public Result<Map<String, Object>> startCdcTask(@PathVariable Long taskId) {
        // 检查是否已在运行
        if (cdcManagerService.isRunning(taskId)) {
            return Result.error("CDC任务已在运行中");
        }

        cdcManagerService.startCdcTask(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("message", "CDC任务启动成功");
        return Result.success(result);
    }

    @Operation(summary = "停止CDC任务")
    @PostMapping("/task/{taskId}/stop")
    public Result<Map<String, Object>> stopCdcTask(@PathVariable Long taskId) {
        if (!cdcManagerService.isRunning(taskId)) {
            return Result.error("CDC任务未在运行");
        }

        cdcManagerService.stopCdcTask(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("message", "CDC任务已停止");
        return Result.success(result);
    }

    @Operation(summary = "获取CDC任务状态")
    @GetMapping("/task/{taskId}/status")
    public Result<Map<String, Object>> getCdcTaskStatus(@PathVariable Long taskId) {
        CdcManagerService.CdcRunningTask runningTask = cdcManagerService.getCdcTaskStatus(taskId);

        Map<String, Object> result = new HashMap<>();
        if (runningTask != null) {
            result.put("taskId", taskId);
            result.put("running", true);
            result.put("executionId", runningTask.getExecutionId());
            result.put("processedCount", runningTask.getProcessedCount());
            result.put("duration", runningTask.getDuration());
            result.put("startTime", runningTask.getStartTime());
        } else {
            result.put("taskId", taskId);
            result.put("running", false);
        }

        return Result.success(result);
    }

    @Operation(summary = "获取所有运行的CDC任务")
    @GetMapping("/running")
    public Result<Map<String, Object>> getAllRunningCdcTasks() {
        Map<Long, CdcManagerService.CdcRunningTask> runningTasks = cdcManagerService.getAllRunningTasks();

        Map<String, Object> result = new HashMap<>();
        result.put("total", runningTasks.size());
        result.put("tasks", runningTasks.entrySet().stream()
            .map(entry -> {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("taskId", entry.getKey());
                taskInfo.put("processedCount", entry.getValue().getProcessedCount());
                taskInfo.put("duration", entry.getValue().getDuration());
                return taskInfo;
            })
            .toList());

        return Result.success(result);
    }
}
