package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.dto.FolderCreateRequest;
import com.etl.engine.dto.FolderTreeNode;
import com.etl.engine.dto.FolderUpdateRequest;
import com.etl.engine.entity.EtlTaskFolder;
import com.etl.engine.service.TaskFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "任务文件夹管理")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class TaskFolderController {

    private final TaskFolderService taskFolderService;

    @Operation(summary = "获取文件夹树")
    @GetMapping("/tree")
    public Result<List<FolderTreeNode>> getFolderTree() {
        return Result.success(taskFolderService.getFolderTree());
    }

    @Operation(summary = "创建文件夹")
    @PostMapping
    public Result<EtlTaskFolder> createFolder(@Valid @RequestBody FolderCreateRequest request,
                                              @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(taskFolderService.createFolder(request, currentUser));
    }

    @Operation(summary = "更新文件夹")
    @PutMapping("/{id}")
    public Result<EtlTaskFolder> updateFolder(@PathVariable Long id,
                                               @Valid @RequestBody FolderUpdateRequest request,
                                               @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(taskFolderService.updateFolder(id, request, currentUser));
    }

    @Operation(summary = "删除文件夹")
    @DeleteMapping("/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        taskFolderService.deleteFolder(id);
        return Result.success();
    }

    @Operation(summary = "移动文件夹")
    @PutMapping("/{id}/move")
    public Result<Void> moveFolder(@PathVariable Long id,
                                    @RequestBody Map<String, Object> request) {
        Long targetParentId = request.get("targetParentId") != null ? Long.valueOf(request.get("targetParentId").toString()) : null;
        Integer sortOrder = request.get("sortOrder") != null ? Integer.valueOf(request.get("sortOrder").toString()) : null;
        taskFolderService.moveFolder(id, targetParentId, sortOrder);
        return Result.success();
    }

    @Operation(summary = "批量更新排序（拖拽排序）")
    @PutMapping("/reorder")
    public Result<Void> reorderFolders(
            @Parameter(description = "排序列表 [{id, sortOrder, parentId}]")
            @RequestBody List<Map<String, Object>> orders,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        taskFolderService.reorderFolders(orders, currentUser);
        return Result.success();
    }

    @Operation(summary = "复制文件夹（含子文件夹和任务）")
    @PostMapping("/{id}/copy")
    public Result<EtlTaskFolder> copyFolder(
            @PathVariable Long id,
            @Parameter(description = "新文件夹名称") @RequestParam(required = false) String newName,
            @Parameter(description = "目标父文件夹ID") @RequestParam(required = false) Long targetParentId,
            @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(taskFolderService.copyFolder(id, newName, targetParentId, currentUser));
    }
}
