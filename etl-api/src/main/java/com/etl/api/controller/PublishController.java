package com.etl.api.controller;

import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlTaskPublish;
import com.etl.engine.service.TaskPublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务发布管理Controller
 */
@Tag(name = "发布管理", description = "任务发布审批流程")
@Slf4j
@RestController
@RequestMapping("/publish")
@RequiredArgsConstructor
public class PublishController {

    private final TaskPublishService taskPublishService;

    @Operation(summary = "分页查询发布记录")
    @GetMapping("/page")
    public Result<PageResult<EtlTaskPublish>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "发布状态") @RequestParam(required = false) String publishStatus,
            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType) {
        return Result.success(taskPublishService.pageList(pageNum, pageSize, publishStatus, taskType));
    }

    @Operation(summary = "发布任务")
    @PostMapping("/task/{taskId}")
    public Result<Long> publishTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> body) {
        String taskType = body != null ? (String) body.getOrDefault("taskType", "WORKFLOW") : "WORKFLOW";
        String changeLog = body != null ? (String) body.get("changeLog") : null;
        String snapshotConfig = body != null ? (String) body.get("snapshotConfig") : null;

        Long id = taskPublishService.publishTask(taskId, taskType, changeLog, snapshotConfig);
        return Result.success(id);
    }

    @Operation(summary = "获取待发布任务列表")
    @GetMapping("/pending")
    public Result<List<EtlTaskPublish>> getPendingList() {
        return Result.success(taskPublishService.getPendingList());
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        String approvedBy = body != null ? (String) body.getOrDefault("approvedBy", "system") : "system";
        taskPublishService.approve(id, approvedBy);
        return Result.success();
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String rejectedBy = (String) body.getOrDefault("rejectedBy", "system");
        String reason = (String) body.get("reason");
        taskPublishService.reject(id, rejectedBy, reason);
        return Result.success();
    }

    @Operation(summary = "获取任务的发布历史")
    @GetMapping("/history/{taskId}")
    public Result<List<EtlTaskPublish>> getHistory(@PathVariable Long taskId) {
        return Result.success(taskPublishService.getHistory(taskId));
    }

    @Operation(summary = "获取发布统计概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        List<EtlTaskPublish> pending = taskPublishService.getPendingList();

        Map<String, Object> overview = new HashMap<>();
        overview.put("pendingCount", pending.size());
        overview.put("pendingTasks", pending);
        return Result.success(overview);
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/{id}/rollback")
    public Result<Map<String, Object>> rollback(
            @PathVariable Long id,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        Map<String, Object> result = taskPublishService.rollback(id, currentUser);
        return Result.success(result);
    }

    @Operation(summary = "对比两个发布版本的差异")
    @GetMapping("/diff/{taskId}")
    public Result<Map<String, Object>> getDiff(
            @PathVariable Long taskId,
            @Parameter(description = "版本号1") @RequestParam Integer v1,
            @Parameter(description = "版本号2") @RequestParam Integer v2) {
        return Result.success(taskPublishService.getDiff(taskId, v1, v2));
    }
}
