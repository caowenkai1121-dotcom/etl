package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlTransformLog;
import com.etl.engine.service.TransformLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 转换日志管理Controller
 */
@Tag(name = "转换日志管理", description = "转换日志查询和管理")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class TransformLogController {

    private final TransformLogService transformLogService;

    @Operation(summary = "分页查询转换日志")
    @GetMapping("/transform/page")
    public Result<PageResult<EtlTransformLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String traceId) {
        Page<EtlTransformLog> page = transformLogService.pageList(pageNum, pageSize, taskId, ruleType, status, traceId);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取转换日志详情")
    @GetMapping("/transform/{id}")
    public Result<EtlTransformLog> getDetail(@PathVariable Long id) {
        return Result.success(transformLogService.getById(id));
    }
}
