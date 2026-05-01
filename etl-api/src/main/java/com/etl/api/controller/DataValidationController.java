package com.etl.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlDataValidation;
import com.etl.engine.mapper.DataValidationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 数据校验管理Controller
 */
@Tag(name = "数据校验管理", description = "数据一致性校验")
@RestController
@RequestMapping("/etl/validation")
@RequiredArgsConstructor
public class DataValidationController {

    private final DataValidationMapper dataValidationMapper;

    @Operation(summary = "分页查询校验记录")
    @GetMapping("/page")
    public Result<PageResult<EtlDataValidation>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status) {
        Page<EtlDataValidation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EtlDataValidation> qw = new LambdaQueryWrapper<EtlDataValidation>()
                .orderByDesc(EtlDataValidation::getCreatedAt);
        if (taskId != null) {
            qw.eq(EtlDataValidation::getTaskId, taskId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(EtlDataValidation::getStatus, status);
        }
        dataValidationMapper.selectPage(page, qw);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取校验详情")
    @GetMapping("/{id}")
    public Result<EtlDataValidation> get(@PathVariable Long id) {
        return Result.success(dataValidationMapper.selectById(id));
    }
}
