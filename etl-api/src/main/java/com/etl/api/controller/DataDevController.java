package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.dto.DagSaveRequest;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.service.DagConfigService;
import com.etl.engine.service.SyncTaskService;
import com.etl.scheduler.dto.ScheduleInfoResponse;
import com.etl.scheduler.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据开发")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DataDevController {

    private final DagConfigService dagConfigService;
    private final SyncTaskService syncTaskService;
    private final SchedulerService schedulerService;

    // ========== 任务CRUD ==========

    @Operation(summary = "获取任务列表（增强版：多条件过滤和多字段排序）")
    @GetMapping("/tasks")
    public Result<Map<String, Object>> getTaskList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "文件夹ID") @RequestParam(required = false) Long folderId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "发布状态") @RequestParam(required = false) String publishStatus,
            @Parameter(description = "任务状态") @RequestParam(required = false) String status,
            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
            @Parameter(description = "同步模式") @RequestParam(required = false) String syncMode,
            @Parameter(description = "是否仅看我的") @RequestParam(defaultValue = "false") Boolean relatedToMe,
            @Parameter(description = "是否收藏") @RequestParam(required = false) Boolean favorite,
            @Parameter(description = "标签页") @RequestParam(defaultValue = "recent") String tab,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "updatedAt") String sortField,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "创建时间-开始") @RequestParam(required = false) String createdStart,
            @Parameter(description = "创建时间-结束") @RequestParam(required = false) String createdEnd,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.getDevTaskList(
            pageNum, pageSize, folderId, keyword, publishStatus, status, taskType,
            syncMode, relatedToMe, favorite, tab, sortField, sortOrder,
            createdStart, createdEnd, currentUser));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/tasks/{id}")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable Long id) {
        return Result.success(dagConfigService.getTaskDetail(id));
    }

    @Operation(summary = "创建开发任务")
    @PostMapping("/tasks")
    public Result<EtlSyncTask> createTask(@RequestBody Map<String, Object> request,
                                          @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.createDevTask(request, currentUser));
    }

    @Operation(summary = "保存任务设计")
    @PutMapping("/tasks/{id}")
    public Result<Void> saveTask(@PathVariable Long id,
                                 @RequestBody DagSaveRequest request,
                                 @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        dagConfigService.saveTask(id, request, currentUser);
        return Result.success();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        dagConfigService.deleteDevTask(id);
        return Result.success();
    }

    @Operation(summary = "复制任务")
    @PostMapping("/tasks/{id}/copy")
    public Result<EtlSyncTask> copyTask(@PathVariable Long id,
                                        @RequestBody Map<String, String> request,
                                        @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        String newName = request.get("name");
        return Result.success(dagConfigService.copyTask(id, newName, currentUser));
    }

    // ========== 批量操作 ==========

    @Operation(summary = "批量删除任务")
    @PostMapping("/tasks/batch-delete")
    public Result<Map<String, Object>> batchDeleteTasks(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        @SuppressWarnings("unchecked")
        List<Long> taskIds = (List<Long>) request.get("taskIds");
        return Result.success(dagConfigService.batchDeleteTasks(taskIds, currentUser));
    }

    @Operation(summary = "批量发布任务")
    @PostMapping("/tasks/batch-publish")
    public Result<Map<String, Object>> batchPublishTasks(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        @SuppressWarnings("unchecked")
        List<Long> taskIds = (List<Long>) request.get("taskIds");
        String changeLog = (String) request.get("changeLog");
        return Result.success(dagConfigService.batchPublishTasks(taskIds, changeLog, currentUser));
    }

    @Operation(summary = "批量移动任务")
    @PostMapping("/tasks/batch-move")
    public Result<Map<String, Object>> batchMoveTasks(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        @SuppressWarnings("unchecked")
        List<Long> taskIds = (List<Long>) request.get("taskIds");
        Long targetFolderId = Long.valueOf(request.get("targetFolderId").toString());
        return Result.success(dagConfigService.batchMoveTasks(taskIds, targetFolderId, currentUser));
    }

    // ========== 任务导入/导出 ==========

    @Operation(summary = "导出任务（返回完整配置JSON）")
    @GetMapping("/tasks/{id}/export")
    public Result<Map<String, Object>> exportTask(@PathVariable Long id) {
        return Result.success(dagConfigService.exportTask(id));
    }

    @Operation(summary = "批量导出任务")
    @PostMapping("/tasks/export-batch")
    public Result<Map<String, Object>> exportTasksBatch(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> taskIds = (List<Long>) request.get("taskIds");
        return Result.success(dagConfigService.exportTasksBatch(taskIds));
    }

    @Operation(summary = "导入任务（从配置JSON创建）")
    @PostMapping("/tasks/import")
    public Result<Map<String, Object>> importTask(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.importTask(request, currentUser));
    }

    // ========== 任务模板 ==========

    @Operation(summary = "获取任务模板列表")
    @GetMapping("/templates")
    public Result<Map<String, Object>> getTemplates(
            @Parameter(description = "模板分类") @RequestParam(required = false) String category,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(dagConfigService.getTemplates(category, keyword, pageNum, pageSize));
    }

    @Operation(summary = "从模板创建任务")
    @PostMapping("/templates/{templateId}/create")
    public Result<EtlSyncTask> createFromTemplate(
            @PathVariable Long templateId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        String taskName = (String) request.get("name");
        Long folderId = request.get("folderId") != null ? Long.valueOf(request.get("folderId").toString()) : 0L;
        return Result.success(dagConfigService.createFromTemplate(templateId, taskName, folderId, currentUser));
    }

    @Operation(summary = "保存为模板")
    @PostMapping("/tasks/{id}/save-as-template")
    public Result<Map<String, Object>> saveAsTemplate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.saveAsTemplate(id, request, currentUser));
    }

    // ========== 任务运行 ==========

    @Operation(summary = "运行任务")
    @PostMapping("/tasks/{id}/run")
    public Result<Map<String, Object>> runTask(@PathVariable Long id,
                                               @RequestBody(required = false) Map<String, Object> options) {
        return Result.success(dagConfigService.runTask(id, options));
    }

    @Operation(summary = "停止任务")
    @PostMapping("/tasks/{id}/stop")
    public Result<Void> stopTask(@PathVariable Long id) {
        dagConfigService.stopTask(id);
        return Result.success();
    }

    @Operation(summary = "获取运行日志")
    @GetMapping("/tasks/{id}/logs")
    public Result<List<Map<String, Object>>> getRunLogs(@PathVariable Long id,
                                                        @RequestParam(required = false) Long executionId) {
        return Result.success(dagConfigService.getRunLogs(id, executionId));
    }

    @Operation(summary = "获取运行统计")
    @GetMapping("/tasks/{id}/stats")
    public Result<Map<String, Object>> getRunStats(@PathVariable Long id,
                                                   @RequestParam(required = false) Long executionId) {
        return Result.success(dagConfigService.getRunStats(id, executionId));
    }

    // ========== 发布管理 ==========

    @Operation(summary = "发布任务")
    @PostMapping("/tasks/{id}/publish")
    public Result<Map<String, Object>> publishTask(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> request,
                                                   @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.publishTask(id, request, currentUser));
    }

    @Operation(summary = "获取发布历史")
    @GetMapping("/tasks/{id}/versions")
    public Result<List<Map<String, Object>>> getPublishHistory(@PathVariable Long id) {
        return Result.success(dagConfigService.getPublishHistory(id));
    }

    // ========== 统计与收藏 ==========

    @Operation(summary = "获取统计概览")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.getDevStats(currentUser));
    }

    @Operation(summary = "收藏任务")
    @PostMapping("/tasks/{id}/favorite")
    public Result<Void> addFavorite(@PathVariable Long id,
                                    @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        dagConfigService.addFavorite(id, currentUser);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/tasks/{id}/favorite")
    public Result<Void> removeFavorite(@PathVariable Long id,
                                       @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        dagConfigService.removeFavorite(id, currentUser);
        return Result.success();
    }

    @Operation(summary = "移动任务")
    @PutMapping("/tasks/{id}/move")
    public Result<Void> moveTask(@PathVariable Long id,
                                 @RequestBody Map<String, Long> request) {
        Long targetFolderId = request.getOrDefault("targetFolderId", request.get("folderId"));
        dagConfigService.moveTask(id, targetFolderId);
        return Result.success();
    }

    // ========== 调度配置 ==========

    @Operation(summary = "保存调度配置（支持多种调度类型）")
    @PutMapping("/schedule/{taskId}")
    public Result<Void> saveSchedule(@PathVariable Long taskId,
                                     @RequestBody Map<String, Object> request) throws Exception {
        String scheduleType = (String) request.getOrDefault("scheduleType", "CRON");
        if ("CRON".equals(scheduleType)) {
            String cronExpression = (String) request.get("cronExpression");
            if (cronExpression != null && !cronExpression.isEmpty()) {
                schedulerService.scheduleTask(taskId, cronExpression);
            }
        } else if ("API".equals(scheduleType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> apiConfig = (Map<String, Object>) request.get("apiConfig");
            schedulerService.scheduleByApiTrigger(taskId, apiConfig);
        } else if ("EVENT".equals(scheduleType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventConfig = (Map<String, Object>) request.get("eventConfig");
            schedulerService.scheduleByEventTrigger(taskId, eventConfig);
        } else if ("DEPENDENCY".equals(scheduleType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dependencyConfig = (Map<String, Object>) request.get("dependencyConfig");
            schedulerService.scheduleByDependencyTrigger(taskId, dependencyConfig);
        } else {
            schedulerService.unscheduleTask(taskId);
        }
        return Result.success();
    }

    @Operation(summary = "获取调度信息")
    @GetMapping("/schedule/{taskId}")
    public Result<ScheduleInfoResponse> getScheduleInfo(@PathVariable Long taskId) throws Exception {
        return Result.success(schedulerService.getTaskScheduleInfo(taskId));
    }

    @Operation(summary = "获取调度预览（未来N次执行时间）")
    @GetMapping("/schedule/{taskId}/preview")
    public Result<Map<String, Object>> getSchedulePreview(
            @PathVariable Long taskId,
            @Parameter(description = "预览次数") @RequestParam(defaultValue = "5") Integer count) throws Exception {
        return Result.success(schedulerService.getSchedulePreview(taskId, count));
    }
}
