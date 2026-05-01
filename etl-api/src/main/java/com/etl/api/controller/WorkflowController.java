package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.dto.FolderResponse;
import com.etl.engine.dto.WorkflowCreateRequest;
import com.etl.engine.dto.WorkflowResponse;
import com.etl.engine.entity.EtlTaskWorkflow;
import com.etl.engine.service.FolderService;
import com.etl.engine.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流管理Controller
 */
@Tag(name = "工作流管理", description = "工作流的增删改查和执行控制")
@Slf4j
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final FolderService folderService;

    @Operation(summary = "分页查询工作流")
    @GetMapping("/page")
    public Result<PageResult<WorkflowResponse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "名称") @RequestParam(required = false) String name,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "文件夹ID") @RequestParam(required = false) Long folderId) {
        return Result.success(workflowService.pageList(pageNum, pageSize, name, status, folderId));
    }

    @Operation(summary = "获取工作流详情")
    @GetMapping("/{id}")
    public Result<WorkflowResponse> get(@PathVariable Long id) {
        WorkflowResponse response = workflowService.getDetail(id);
        return response != null ? Result.success(response) : Result.error("工作流不存在");
    }

    @Operation(summary = "创建工作流")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WorkflowCreateRequest request) {
        Long id = workflowService.createWorkflow(request);
        return Result.success(id);
    }

    @Operation(summary = "更新工作流")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WorkflowCreateRequest request) {
        workflowService.updateWorkflow(id, request);
        return Result.success();
    }

    @Operation(summary = "删除工作流")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return Result.success();
    }

    @Operation(summary = "发布工作流")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        workflowService.publishWorkflow(id);
        return Result.success();
    }

    @Operation(summary = "执行工作流")
    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> execute(@PathVariable Long id) {
        String executionNo = workflowService.executeWorkflow(id);
        Map<String, Object> result = new HashMap<>();
        result.put("executionNo", executionNo);
        result.put("message", "工作流已开始执行");
        return Result.success(result);
    }

    @Operation(summary = "获取文件夹树")
    @GetMapping("/folder/tree")
    public Result<List<FolderResponse>> getFolderTree() {
        return Result.success(folderService.getFolderTree("WORKFLOW"));
    }

    @Operation(summary = "创建文件夹")
    @PostMapping("/folder")
    public Result<Long> createFolder(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Long id = folderService.createFolder(name, parentId, "WORKFLOW");
        return Result.success(id);
    }

    @Operation(summary = "更新文件夹")
    @PutMapping("/folder/{id}")
    public Result<Void> updateFolder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sortOrder = body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder").toString()) : null;
        folderService.updateFolder(id, name, sortOrder);
        return Result.success();
    }

    @Operation(summary = "删除文件夹")
    @DeleteMapping("/folder/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        folderService.deleteFolder(id);
        return Result.success();
    }

    @Operation(summary = "获取文件夹下的工作流列表")
    @GetMapping("/folder/{folderId}/workflows")
    public Result<List<WorkflowResponse>> listByFolder(@PathVariable Long folderId) {
        return Result.success(workflowService.listByFolder(folderId));
    }

    @Operation(summary = "获取工作流节点类型列表")
    @GetMapping("/node-types")
    public Result<List<Map<String, Object>>> getNodeTypes() {
        List<Map<String, Object>> types = List.of(
            Map.of("type", "SYNC", "name", "数据同步", "icon", "Refresh"),
            Map.of("type", "TRANSFORM", "name", "数据转换", "icon", "Edit"),
            Map.of("type", "SCRIPT", "name", "脚本执行", "icon", "Document"),
            Map.of("type", "CONDITION", "name", "条件分支", "icon", "Share"),
            Map.of("type", "LOOP", "name", "循环控制", "icon", "RefreshRight")
        );
        return Result.success(types);
    }
}
