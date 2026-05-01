package com.etl.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlScriptTemplate;
import com.etl.engine.mapper.ScriptTemplateMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 脚本模板管理Controller
 */
@Tag(name = "脚本模板管理", description = "ETL转换脚本模板的增删改查")
@RestController
@RequestMapping("/etl/script")
@RequiredArgsConstructor
public class ScriptTemplateController {

    private final ScriptTemplateMapper scriptTemplateMapper;

    @Operation(summary = "分页查询脚本模板")
    @GetMapping("/page")
    public Result<PageResult<EtlScriptTemplate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String language) {
        Page<EtlScriptTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EtlScriptTemplate> qw = new LambdaQueryWrapper<EtlScriptTemplate>()
                .orderByDesc(EtlScriptTemplate::getCreatedAt);
        if (StringUtils.hasText(language)) {
            qw.eq(EtlScriptTemplate::getScriptLanguage, language);
        }
        scriptTemplateMapper.selectPage(page, qw);
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取脚本模板详情")
    @GetMapping("/{id}")
    public Result<EtlScriptTemplate> get(@PathVariable Long id) {
        return Result.success(scriptTemplateMapper.selectById(id));
    }

    @Operation(summary = "创建脚本模板")
    @PostMapping
    public Result<Long> create(@RequestBody EtlScriptTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setEnabled(1);
        scriptTemplateMapper.insert(template);
        return Result.success(template.getId());
    }

    @Operation(summary = "更新脚本模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody EtlScriptTemplate template) {
        template.setId(id);
        template.setUpdatedAt(LocalDateTime.now());
        scriptTemplateMapper.updateById(template);
        return Result.success();
    }

    @Operation(summary = "删除脚本模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scriptTemplateMapper.deleteById(id);
        return Result.success();
    }
}
