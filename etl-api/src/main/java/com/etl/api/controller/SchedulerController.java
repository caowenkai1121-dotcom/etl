package com.etl.api.controller;

import com.alibaba.fastjson2.JSON;
import com.etl.common.result.Result;
import com.etl.scheduler.dag.DagEdge;
import com.etl.scheduler.dag.DagNode;
import com.etl.scheduler.dag.DagValidator;
import com.etl.scheduler.dto.ScheduleInfoResponse;
import com.etl.scheduler.entity.EtlScheduleDag;
import com.etl.scheduler.service.DagScheduleService;
import com.etl.scheduler.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度管理Controller
 */
@Tag(name = "调度管理", description = "任务的定时调度管理")
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerService schedulerService;
    private final DagScheduleService dagScheduleService;

    @Operation(summary = "获取所有已调度任务列表")
    @GetMapping("/tasks")
    public Result<List<Long>> getAllScheduledTasks() throws Exception {
        return Result.success(schedulerService.getAllScheduledTaskIds());
    }

    @Operation(summary = "创建定时任务")
    @PostMapping("/task/{taskId}")
    public Result<Void> scheduleTask(
            @PathVariable Long taskId,
            @Parameter(description = "Cron表达式") @RequestParam String cronExpression) throws Exception {
        schedulerService.scheduleTask(taskId, cronExpression);
        return Result.success();
    }

    @Operation(summary = "删除定时任务")
    @DeleteMapping("/task/{taskId}")
    public Result<Void> unscheduleTask(@PathVariable Long taskId) throws Exception {
        schedulerService.unscheduleTask(taskId);
        return Result.success();
    }

    @Operation(summary = "暂停定时任务")
    @PostMapping("/task/{taskId}/pause")
    public Result<Void> pauseTask(@PathVariable Long taskId) throws Exception {
        schedulerService.pauseTask(taskId);
        return Result.success();
    }

    @Operation(summary = "恢复定时任务")
    @PostMapping("/task/{taskId}/resume")
    public Result<Void> resumeTask(@PathVariable Long taskId) throws Exception {
        schedulerService.resumeTask(taskId);
        return Result.success();
    }

    @Operation(summary = "立即执行一次")
    @PostMapping("/task/{taskId}/trigger")
    public Result<Void> triggerTask(@PathVariable Long taskId) throws Exception {
        schedulerService.triggerTask(taskId);
        return Result.success();
    }

    @Operation(summary = "更新Cron表达式")
    @PutMapping("/task/{taskId}/cron")
    public Result<Void> updateCron(
            @PathVariable Long taskId,
            @Parameter(description = "Cron表达式") @RequestParam String cronExpression) throws Exception {
        schedulerService.updateCronExpression(taskId, cronExpression);
        return Result.success();
    }

    @Operation(summary = "获取任务调度信息")
    @GetMapping("/task/{taskId}/info")
    public Result<ScheduleInfoResponse> getTaskScheduleInfo(@PathVariable Long taskId) throws Exception {
        return Result.success(schedulerService.getTaskScheduleInfo(taskId));
    }

    // DAG调度接口

    @Operation(summary = "获取DAG列表")
    @GetMapping("/dag")
    public Result<List<EtlScheduleDag>> listDags() throws Exception {
        return Result.success(dagScheduleService.listAllDags());
    }

    @Operation(summary = "创建DAG")
    @PostMapping("/dag")
    public Result<Long> createDag(@RequestBody EtlScheduleDag dag) throws Exception {
        return Result.success(dagScheduleService.createDag(dag));
    }

    @Operation(summary = "查询DAG")
    @GetMapping("/dag/{id}")
    public Result<EtlScheduleDag> getDag(@PathVariable Long id) throws Exception {
        return Result.success(dagScheduleService.getDag(id));
    }

    @Operation(summary = "更新DAG")
    @PutMapping("/dag/{id}")
    public Result<Void> updateDag(@PathVariable Long id, @RequestBody EtlScheduleDag dag) throws Exception {
        dag.setId(id);
        dagScheduleService.updateDag(dag);
        return Result.success();
    }

    @Operation(summary = "删除DAG")
    @DeleteMapping("/dag/{id}")
    public Result<Void> deleteDag(@PathVariable Long id) throws Exception {
        dagScheduleService.deleteDag(id);
        return Result.success();
    }

    @Operation(summary = "执行DAG")
    @PostMapping("/dag/{id}/execute")
    public Result<Void> executeDag(@PathVariable Long id) throws Exception {
        dagScheduleService.executeDag(id);
        return Result.success();
    }

    @Operation(summary = "验证DAG配置")
    @PostMapping("/dag/validate")
    public Result<Boolean> validateDag(@RequestBody String dagConfig) throws Exception {
        if (dagConfig == null || dagConfig.isBlank()) {
            return Result.success(false);
        }
        try {
            // dagConfig 应该是包含 nodes 和 edges 的 JSON 对象
            com.alibaba.fastjson2.JSONObject dagJson = JSON.parseObject(dagConfig);
            List<DagNode> nodes = dagJson.getList("nodes", DagNode.class);
            List<DagEdge> edges = dagJson.getList("edges", DagEdge.class);
            if (nodes == null || edges == null) {
                return Result.success(false);
            }
            boolean hasCycle = DagValidator.hasCycle(nodes, edges);
            return Result.success(!hasCycle);
        } catch (Exception e) {
            return Result.success(false);
        }
    }

    @Operation(summary = "获取DAG节点列表")
    @GetMapping("/dag/{id}/nodes")
    public Result<List<DagNode>> getDagNodes(@PathVariable Long id) throws Exception {
        EtlScheduleDag dag = dagScheduleService.getDag(id);
        if (dag == null || dag.getDagConfig() == null) {
            return Result.success(List.of());
        }
        try {
            List<DagNode> nodes = JSON.parseArray(dag.getDagConfig(), DagNode.class);
            return Result.success(nodes != null ? nodes : List.of());
        } catch (Exception e) {
            return Result.success(List.of());
        }
    }

    @Operation(summary = "保存DAG节点")
    @PutMapping("/dag/{id}/nodes")
    public Result<Void> saveDagNodes(@PathVariable Long id, @RequestBody String dagConfig) throws Exception {
        EtlScheduleDag dag = new EtlScheduleDag();
        dag.setId(id);
        dag.setDagConfig(dagConfig);
        dagScheduleService.updateDag(dag);
        return Result.success();
    }
}
