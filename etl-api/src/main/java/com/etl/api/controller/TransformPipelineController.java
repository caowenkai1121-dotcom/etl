package com.etl.api.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlTransformPipeline;
import com.etl.engine.entity.EtlTransformStep;
import com.etl.engine.service.TransformPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ETL转换流水线Controller
 */
@Tag(name = "ETL转换流水线", description = "转换流水线的增删改查和编排")
@RestController
@RequestMapping("/transform/pipeline")
@RequiredArgsConstructor
public class TransformPipelineController {

    private final TransformPipelineService pipelineService;

    // ---- Pipeline CRUD ----

    @Operation(summary = "分页查询转换流水线")
    @GetMapping
    public Result<PageResult<EtlTransformPipeline>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        Page<EtlTransformPipeline> page = pipelineService.getPipelinePage(pageNum, pageSize, name);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取转换流水线详情")
    @GetMapping("/{id}")
    public Result<EtlTransformPipeline> get(@PathVariable Long id) {
        return Result.success(pipelineService.getPipelineById(id));
    }

    @Operation(summary = "创建转换流水线")
    @PostMapping
    public Result<Long> create(@RequestBody EtlTransformPipeline pipeline) {
        return Result.success(pipelineService.createPipeline(pipeline));
    }

    @Operation(summary = "更新转换流水线")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody EtlTransformPipeline pipeline) {
        pipelineService.updatePipeline(id, pipeline);
        return Result.success();
    }

    @Operation(summary = "删除转换流水线")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
        return Result.success();
    }

    // ---- Step CRUD ----

    @Operation(summary = "获取流水线的步骤列表")
    @GetMapping("/{id}/steps")
    public Result<List<EtlTransformStep>> getSteps(@PathVariable Long id) {
        return Result.success(pipelineService.getStepsByPipelineId(id));
    }

    @Operation(summary = "添加转换步骤")
    @PostMapping("/{id}/steps")
    public Result<List<Long>> addStep(@PathVariable Long id, @RequestBody List<EtlTransformStep> steps) {
        steps.forEach(step -> step.setPipelineId(id));
        List<Long> stepIds = pipelineService.addSteps(steps);
        return Result.success(stepIds);
    }

    @Operation(summary = "更新转换步骤")
    @PutMapping("/steps/{stepId}")
    public Result<Void> updateStep(@PathVariable Long stepId, @RequestBody EtlTransformStep step) {
        pipelineService.updateStep(stepId, step);
        return Result.success();
    }

    @Operation(summary = "删除转换步骤")
    @DeleteMapping("/steps/{stepId}")
    public Result<Void> deleteStep(@PathVariable Long stepId) {
        pipelineService.deleteStep(stepId);
        return Result.success();
    }

    @Operation(summary = "重新排序转换步骤")
    @PutMapping("/{id}/steps/reorder")
    public Result<Void> reorderSteps(@PathVariable Long id, @RequestBody List<Long> stepIds) {
        pipelineService.reorderSteps(id, stepIds);
        return Result.success();
    }

    // ---- Preview & Rules ----

    @Operation(summary = "预览转换结果")
    @PostMapping("/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id, @RequestBody List<Map<String, Object>> sampleData) {
        return Result.success(pipelineService.previewExecution(id, sampleData));
    }

    @Operation(summary = "获取支持的转换规则")
    @GetMapping("/rules")
    public Result<List<Map<String, Object>>> getSupportedRules() {
        return Result.success(pipelineService.getSupportedRules());
    }
}

/**
 * ETL转换规则Controller
 */
@Tag(name = "ETL转换规则", description = "获取支持的转换规则类型")
@RestController
@RequestMapping("/transform/rules")
@RequiredArgsConstructor
class TransformRuleController {

    private final TransformPipelineService pipelineService;

    @Operation(summary = "获取支持的转换规则列表")
    @GetMapping
    public Result<List<Map<String, Object>>> getRules() {
        return Result.success(pipelineService.getSupportedRules());
    }
}
