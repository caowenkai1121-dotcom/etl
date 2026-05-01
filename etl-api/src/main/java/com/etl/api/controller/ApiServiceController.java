package com.etl.api.controller;

import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.dto.ApiServiceCreateRequest;
import com.etl.engine.dto.ApiServiceResponse;
import com.etl.engine.entity.EtlApiService;
import com.etl.engine.service.ApiManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "API服务管理", description = "数据服务API的创建、发布和调用")
@Slf4j
@RestController
@RequestMapping("/api-service")
@RequiredArgsConstructor
public class ApiServiceController {

    private final ApiManageService apiManageService;

    @Operation(summary = "分页查询API服务")
    @GetMapping("/page")
    public Result<PageResult<ApiServiceResponse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "名称") @RequestParam(required = false) String name,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "文件夹ID") @RequestParam(required = false) Long folderId) {
        return Result.success(apiManageService.pageList(pageNum, pageSize, name, status, folderId));
    }

    @Operation(summary = "获取API服务详情")
    @GetMapping("/{id}")
    public Result<ApiServiceResponse> get(@PathVariable Long id) {
        ApiServiceResponse response = apiManageService.getDetail(id);
        return response != null ? Result.success(response) : Result.error("API服务不存在");
    }

    @Operation(summary = "创建API服务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ApiServiceCreateRequest request) {
        Long id = apiManageService.createApiService(request);
        return Result.success(id);
    }

    @Operation(summary = "更新API服务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ApiServiceCreateRequest request) {
        apiManageService.updateApiService(id, request);
        return Result.success();
    }

    @Operation(summary = "删除API服务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apiManageService.deleteApiService(id);
        return Result.success();
    }

    @Operation(summary = "上线API服务")
    @PostMapping("/{id}/online")
    public Result<Void> online(@PathVariable Long id) {
        apiManageService.onlineApi(id);
        return Result.success();
    }

    @Operation(summary = "下线API服务")
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        apiManageService.offlineApi(id);
        return Result.success();
    }

    @Operation(summary = "测试API服务（执行SQL并返回结果）")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, Object> params,
                                            HttpServletRequest request) {
        Map<String, Object> result = apiManageService.testApi(id, params != null ? params : Map.of());
        return Result.success(result);
    }

    @Operation(summary = "获取API调用统计（含趋势和分布）")
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getStats(@PathVariable Long id,
                                                 @Parameter(description = "统计粒度: today/week/month") @RequestParam(defaultValue = "today") String period) {
        return Result.success(apiManageService.getApiStats(id, period));
    }

    @Operation(summary = "获取API调用日志")
    @GetMapping("/{id}/logs")
    public Result<Map<String, Object>> getLogs(
            @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "状态: SUCCESS/FAILED") @RequestParam(required = false) String status) {
        return Result.success(apiManageService.getApiCallLogs(id, pageNum, pageSize, status));
    }

    @Operation(summary = "生成API文档")
    @GetMapping("/{id}/doc")
    public Result<Map<String, Object>> getDoc(@PathVariable Long id) {
        return Result.success(apiManageService.generateApiDoc(id));
    }

    @Operation(summary = "发布API服务（生成公开访问地址）")
    @PostMapping("/{id}/publish")
    public Result<Map<String, Object>> publishApi(@PathVariable Long id,
                                                   @RequestHeader(value = "X-User", defaultValue = "admin") String currentUser) {
        return Result.success(apiManageService.publishApi(id, currentUser));
    }

    @Operation(summary = "获取所有在线API列表（用于外部调用）")
    @GetMapping("/public/list")
    public Result<List<Map<String, Object>>> getPublicApiList() {
        return Result.success(apiManageService.getPublicApiList());
    }
}
