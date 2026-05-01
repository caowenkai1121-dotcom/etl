package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.api.event.SyncLogCallbackImpl;
import com.etl.common.callback.SyncLogCallback;
import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.enums.SyncMode;
import com.etl.common.enums.SyncScope;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.SyncEngine;
import com.etl.engine.SyncEngineFactory;
import com.etl.engine.dto.TaskCreateRequest;
import com.etl.engine.dto.TaskProgressResponse;
import com.etl.engine.dto.TaskResponse;
import com.etl.engine.dto.TaskUpdateRequest;
import com.etl.engine.dto.TaskExecutionResponse;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.entity.EtlTaskDependency;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionManager;
import com.etl.engine.service.TaskExecutionService;
import com.etl.scheduler.dependency.TaskDependencyManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 同步任务Controller
 */
@Tag(name = "同步任务管理", description = "同步任务的增删改查和执行控制")
@Slf4j
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final SyncTaskService syncTaskService;
    private final SyncEngineFactory syncEngineFactory;
    private final TaskExecutionManager executionManager;
    private final TaskExecutionService taskExecutionService;
    private final SyncLogCallbackImpl syncLogCallback;

    @Operation(summary = "获取同步模式列表")
    @GetMapping("/sync-modes")
    public Result<List<String>> getSyncModes() {
        return Result.success(Arrays.stream(SyncMode.values())
            .map(SyncMode::getCode)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "获取同步范围列表")
    @GetMapping("/sync-scopes")
    public Result<List<String>> getSyncScopes() {
        return Result.success(Arrays.stream(SyncScope.values())
            .map(SyncScope::getCode)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "分页查询任务")
    @GetMapping("/page")
    public Result<PageResult<TaskResponse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "名称") @RequestParam(required = false) String name,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        Page<EtlSyncTask> page = syncTaskService.pageList(pageNum, pageSize, name, status);
        List<TaskResponse> records = page.getRecords().stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
        return Result.success(PageResult.of(records, page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public Result<TaskResponse> get(@PathVariable Long id) {
        return Result.success(TaskResponse.from(syncTaskService.getDetail(id)));
    }

    @Operation(summary = "创建任务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TaskCreateRequest request) {
        EtlSyncTask task = new EtlSyncTask();
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setSourceDsId(request.getSourceDsId());
        task.setTargetDsId(request.getTargetDsId());
        task.setSyncMode(request.getSyncMode());
        task.setSyncScope(request.getSyncScope());
        task.setTableConfig(request.getTableConfig());
        task.setFieldMapping(request.getFieldMapping());
        task.setIncrementalField(request.getIncrementalField());
        task.setIncrementalValue(request.getIncrementalValue());
        task.setCronExpression(request.getCronExpression());
        task.setSyncStrategy(request.getSyncStrategy());
        task.setBatchSize(request.getBatchSize());
        task.setParallelThreads(request.getParallelThreads());
        task.setRetryTimes(request.getRetryTimes());
        task.setRetryInterval(request.getRetryInterval());
        return Result.success(syncTaskService.createTask(task));
    }

    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        EtlSyncTask task = new EtlSyncTask();
        task.setId(id);
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setSourceDsId(request.getSourceDsId());
        task.setTargetDsId(request.getTargetDsId());
        task.setSyncMode(request.getSyncMode());
        task.setSyncScope(request.getSyncScope());
        task.setTableConfig(request.getTableConfig());
        task.setFieldMapping(request.getFieldMapping());
        task.setIncrementalField(request.getIncrementalField());
        task.setIncrementalValue(request.getIncrementalValue());
        task.setCronExpression(request.getCronExpression());
        task.setSyncStrategy(request.getSyncStrategy());
        task.setBatchSize(request.getBatchSize());
        task.setParallelThreads(request.getParallelThreads());
        task.setRetryTimes(request.getRetryTimes());
        task.setRetryInterval(request.getRetryInterval());
        syncTaskService.updateTask(task);
        return Result.success();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        syncTaskService.deleteTask(id);
        return Result.success();
    }

    @Operation(summary = "手动执行任务")
    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> execute(@PathVariable Long id) {
        EtlSyncTask task = syncTaskService.getDetail(id);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (executionManager.isRunning(id)) {
            return Result.error("任务正在执行中");
        }

        CompletableFuture.runAsync(() -> {
            SyncPipelineContext context = new SyncPipelineContext();
            context.setTaskId(id);
            context.setSourceDsId(task.getSourceDsId());
            context.setTargetDsId(task.getTargetDsId());
            context.setBatchSize(task.getBatchSize() != null ? task.getBatchSize() : 1000);
            context.setLogCallback(syncLogCallback);

            SyncMode syncMode = SyncMode.fromCode(task.getSyncMode());
            SyncEngine engine = syncEngineFactory.getEngine(syncMode);

            try {
                EtlTaskExecution execution = taskExecutionService.createExecution(id, "MANUAL");
                context.setExecutionId(execution.getId());
                context.setExecutionNo(execution.getExecutionNo());

                // 记录开始日志
                syncLogCallback.info(id, execution.getId(), context.getTraceId(), null, "任务开始执行: " + task.getName());

                executionManager.startExecution(id, engine, context, execution);
                engine.sync(context);

                taskExecutionService.completeExecution(execution.getId(), "SUCCESS", null, null);

                // 记录完成日志
                syncLogCallback.info(id, execution.getId(), context.getTraceId(), null, "任务执行完成");
            } catch (Exception e) {
                log.error("任务执行失败: taskId={}", id, e);
                syncLogCallback.error(id, context.getExecutionId(), context.getTraceId(), null, "任务执行失败: " + e.getMessage());
                try {
                    EtlTaskExecution latest = taskExecutionService.getLatestExecution(id);
                    if (latest != null) {
                        taskExecutionService.completeExecution(latest.getId(), "FAILED", e.getMessage(), null);
                    }
                } catch (Exception ex) {
                    log.error("更新执行状态失败: taskId={}", id, ex);
                }
            } finally {
                executionManager.endExecution(id);
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", id);
        result.put("message", "任务已开始执行");
        return Result.success(result);
    }

    @Operation(summary = "停止任务")
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        boolean stopped = executionManager.stopTask(id);
        if (stopped) {
            syncTaskService.updateStatus(id, "STOPPED");
            return Result.success();
        } else {
            return Result.error("任务未在执行中");
        }
    }

    @Operation(summary = "获取任务进度")
    @GetMapping("/{id}/progress")
    public Result<TaskProgressResponse> getProgress(@PathVariable Long id) {
        TaskProgressResponse progress = executionManager.getProgress(id);
        if (progress == null) {
            EtlTaskExecution latest = taskExecutionService.getLatestExecution(id);
            if (latest != null) {
                TaskProgressResponse info = new TaskProgressResponse();
                info.setTaskId(id);
                info.setExecutionId(latest.getId());
                info.setExecutionNo(latest.getExecutionNo());
                info.setProgress(latest.getProgress() != null ? latest.getProgress().intValue() : 0);
                info.setTotalRows(latest.getTotalRows());
                info.setSuccessRows(latest.getSuccessRows());
                info.setFailedRows(latest.getFailedRows());
                info.setRunning(false);
                info.setStatus(latest.getStatus());
                return Result.success(info);
            }
            return Result.error("未找到执行记录");
        }
        return Result.success(progress);
    }

    @Operation(summary = "获取任务执行历史")
    @GetMapping("/{id}/executions")
    public Result<PageResult<TaskExecutionResponse>> getExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<EtlTaskExecution> page = taskExecutionService.pageList(pageNum, pageSize, id, null);
        List<TaskExecutionResponse> records = page.getRecords().stream()
            .map(TaskExecutionResponse::from)
            .collect(Collectors.toList());
        return Result.success(PageResult.of(records, page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取执行详情")
    @GetMapping("/execution/{executionId}")
    public Result<TaskExecutionResponse> getExecutionDetail(@PathVariable Long executionId) {
        return Result.success(TaskExecutionResponse.from(taskExecutionService.getById(executionId)));
    }

    // ==================== 任务依赖管理 ====================

    private final TaskDependencyManager taskDependencyManager;

    @Operation(summary = "获取任务依赖")
    @GetMapping("/{taskId}/dependencies")
    public Result<List<EtlTaskDependency>> getDependencies(@PathVariable Long taskId) {
        return Result.success(taskDependencyManager.getDependencies(taskId));
    }

    @Operation(summary = "添加任务依赖")
    @PostMapping("/{taskId}/dependencies")
    public Result<Void> addDependency(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        Long dependsOnTaskId = Long.valueOf(body.get("dependsOnTaskId").toString());
        String dependencyType = (String) body.getOrDefault("dependencyType", "FINISH");
        taskDependencyManager.addDependency(taskId, dependsOnTaskId, dependencyType);
        return Result.success();
    }

    @Operation(summary = "移除任务依赖")
    @DeleteMapping("/{taskId}/dependencies/{dependsOnTaskId}")
    public Result<Void> removeDependency(@PathVariable Long taskId, @PathVariable Long dependsOnTaskId) {
        taskDependencyManager.removeDependency(taskId, dependsOnTaskId);
        return Result.success();
    }
}
