package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.dto.DagConfigResponse;
import com.etl.engine.dto.DagSaveRequest;
import com.etl.engine.service.DagConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "DAG配置管理")
@RestController
@RequestMapping("/dag")
@RequiredArgsConstructor
public class DagConfigController {

    private final DagConfigService dagConfigService;

    @Operation(summary = "获取DAG配置")
    @GetMapping("/{taskId}")
    public Result<DagConfigResponse> getDagConfig(@PathVariable Long taskId,
                                                   @RequestParam(required = false) Integer version) {
        return Result.success(dagConfigService.getDagConfig(taskId, version));
    }

    @Operation(summary = "保存DAG配置（含节点和连线完整性校验）")
    @PutMapping("/{taskId}")
    public Result<DagConfigResponse> saveDagConfig(@PathVariable Long taskId,
                                                    @RequestBody DagSaveRequest request,
                                                    @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        dagConfigService.validateDagIntegrity(request);
        return Result.success(dagConfigService.saveDagConfig(taskId, request, currentUser));
    }

    @Operation(summary = "复制DAG配置")
    @PostMapping("/copy")
    public Result<DagConfigResponse> copyDagConfig(@RequestParam Long sourceTaskId,
                                                    @RequestParam Long targetTaskId,
                                                    @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(dagConfigService.copyDagConfig(sourceTaskId, targetTaskId, currentUser));
    }

    @Operation(summary = "校验DAG配置完整性")
    @PostMapping("/{taskId}/validate")
    public Result<Map<String, Object>> validateDag(@PathVariable Long taskId,
                                                    @RequestBody DagSaveRequest request) {
        dagConfigService.validateDagIntegrity(request);
        return Result.success(Map.of("valid", true, "message", "DAG配置校验通过"));
    }

    @Operation(summary = "获取DAG版本快照列表")
    @GetMapping("/{taskId}/versions")
    public Result<List<Map<String, Object>>> getDagSnapshots(@PathVariable Long taskId) {
        return Result.success(dagConfigService.getDagSnapshots(taskId));
    }

    @Operation(summary = "对比两个DAG版本的差异")
    @GetMapping("/{taskId}/diff")
    public Result<Map<String, Object>> getDagDiff(
            @PathVariable Long taskId,
            @Parameter(description = "版本1") @RequestParam Integer v1,
            @Parameter(description = "版本2") @RequestParam Integer v2) {
        return Result.success(dagConfigService.getDagDiff(taskId, v1, v2));
    }
}