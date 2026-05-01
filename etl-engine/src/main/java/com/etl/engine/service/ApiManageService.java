package com.etl.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.result.PageResult;
import com.etl.engine.dto.ApiServiceCreateRequest;
import com.etl.engine.dto.ApiServiceResponse;
import com.etl.engine.entity.EtlApiService;
import com.etl.engine.mapper.ApiServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * API服务管理Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiManageService extends ServiceImpl<ApiServiceMapper, EtlApiService> {

    /**
     * 分页查询API服务
     */
    public PageResult<ApiServiceResponse> pageList(Integer pageNum, Integer pageSize, String name, String status, Long folderId) {
        Page<EtlApiService> page = new Page<>(pageNum, pageSize);

        lambdaQuery()
            .like(name != null && !name.isEmpty(), EtlApiService::getName, name)
            .eq(status != null && !status.isEmpty(), EtlApiService::getStatus, status)
            .eq(folderId != null, EtlApiService::getFolderId, folderId)
            .orderByDesc(EtlApiService::getCreateTime)
            .page(page);

        return PageResult.of(
            page.getRecords().stream().map(this::toResponse).toList(),
            page.getTotal(),
            pageNum,
            pageSize
        );
    }

    /**
     * 获取API服务详情
     */
    public ApiServiceResponse getDetail(Long id) {
        EtlApiService apiService = getById(id);
        if (apiService == null) {
            return null;
        }
        return toResponse(apiService);
    }

    /**
     * 创建API服务
     */
    @Transactional
    public Long createApiService(ApiServiceCreateRequest request) {
        // 检查路径是否已存在
        long count = lambdaQuery()
            .eq(EtlApiService::getPath, request.getPath())
            .eq(EtlApiService::getMethod, request.getMethod())
            .count();
        if (count > 0) {
            throw new RuntimeException("API路径已存在: " + request.getPath());
        }

        EtlApiService apiService = new EtlApiService();
        apiService.setName(request.getName());
        apiService.setPath(request.getPath());
        apiService.setMethod(request.getMethod());
        apiService.setDatasourceId(request.getDatasourceId());
        apiService.setSqlTemplate(request.getSqlTemplate());
        apiService.setParamsConfig(request.getParamsConfig());
        apiService.setAuthType(request.getAuthType());
        apiService.setAuthConfig(request.getAuthConfig());
        apiService.setRateLimit(request.getRateLimit());
        apiService.setTimeout(request.getTimeout());
        apiService.setDescription(request.getDescription());
        apiService.setFolderId(request.getFolderId());
        apiService.setStatus("OFFLINE");
        apiService.setCreateBy("system");
        apiService.setCreateTime(LocalDateTime.now());

        save(apiService);
        return apiService.getId();
    }

    /**
     * 更新API服务
     */
    @Transactional
    public void updateApiService(Long id, ApiServiceCreateRequest request) {
        EtlApiService apiService = getById(id);
        if (apiService == null) {
            throw new RuntimeException("API服务不存在");
        }

        apiService.setName(request.getName());
        apiService.setPath(request.getPath());
        apiService.setMethod(request.getMethod());
        apiService.setDatasourceId(request.getDatasourceId());
        apiService.setSqlTemplate(request.getSqlTemplate());
        apiService.setParamsConfig(request.getParamsConfig());
        apiService.setAuthType(request.getAuthType());
        apiService.setAuthConfig(request.getAuthConfig());
        apiService.setRateLimit(request.getRateLimit());
        apiService.setTimeout(request.getTimeout());
        apiService.setDescription(request.getDescription());
        apiService.setFolderId(request.getFolderId());
        apiService.setUpdateTime(LocalDateTime.now());

        updateById(apiService);
    }

    /**
     * 删除API服务
     */
    @Transactional
    public void deleteApiService(Long id) {
        removeById(id);
    }

    /**
     * 上线API服务
     */
    @Transactional
    public void onlineApi(Long id) {
        EtlApiService apiService = getById(id);
        if (apiService == null) {
            throw new RuntimeException("API服务不存在");
        }

        apiService.setStatus("ONLINE");
        apiService.setUpdateTime(LocalDateTime.now());
        updateById(apiService);

        log.info("API服务上线: id={}, path={}", id, apiService.getPath());
    }

    /**
     * 下线API服务
     */
    @Transactional
    public void offlineApi(Long id) {
        EtlApiService apiService = getById(id);
        if (apiService == null) {
            throw new RuntimeException("API服务不存在");
        }

        apiService.setStatus("OFFLINE");
        apiService.setUpdateTime(LocalDateTime.now());
        updateById(apiService);

        log.info("API服务下线: id={}, path={}", id, apiService.getPath());
    }

    /**
     * 根据路径获取API配置
     */
    public EtlApiService getByPath(String path, String method) {
        return lambdaQuery()
            .eq(EtlApiService::getPath, path)
            .eq(EtlApiService::getMethod, method)
            .eq(EtlApiService::getStatus, "ONLINE")
            .one();
    }

    /**
     * 转换为响应DTO
     */
    private ApiServiceResponse toResponse(EtlApiService entity) {
        ApiServiceResponse response = new ApiServiceResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setPath(entity.getPath());
        response.setMethod(entity.getMethod());
        response.setDatasourceId(entity.getDatasourceId());
        response.setSqlTemplate(entity.getSqlTemplate());
        response.setParamsConfig(entity.getParamsConfig());
        response.setAuthType(entity.getAuthType());
        response.setAuthConfig(entity.getAuthConfig());
        response.setRateLimit(entity.getRateLimit());
        response.setTimeout(entity.getTimeout());
        response.setStatus(entity.getStatus());
        response.setDescription(entity.getDescription());
        response.setFolderId(entity.getFolderId());
        response.setCreateBy(entity.getCreateBy());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    // ========== API测试、文档、日志、统计 ==========

    /**
     * 测试API服务（执行SQL并返回模拟结果）
     */
    public Map<String, Object> testApi(Long apiId, Map<String, Object> params) {
        EtlApiService apiService = getById(apiId);
        if (apiService == null) {
            throw new RuntimeException("API服务不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("apiId", apiId);
        result.put("apiName", apiService.getName());
        result.put("testedAt", LocalDateTime.now().toString());

        // 解析参数配置并模拟替换
        String sql = apiService.getSqlTemplate();
        if (sql != null && params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sql = sql.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                sql = sql.replace("#{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        result.put("renderedSql", sql);
        result.put("params", params);

        // 模拟返回列信息
        List<Map<String, String>> columns = new ArrayList<>();
        columns.add(Map.of("name", "id", "type", "BIGINT"));
        columns.add(Map.of("name", "name", "type", "VARCHAR"));
        columns.add(Map.of("name", "created_at", "type", "DATETIME"));
        result.put("columns", columns);

        // 模拟返回示例数据
        List<Map<String, Object>> sampleData = new ArrayList<>();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", 1);
        row1.put("name", "示例数据1");
        row1.put("created_at", "2026-04-30 10:00:00");
        sampleData.add(row1);
        result.put("sampleData", sampleData);
        result.put("totalRows", 1);
        result.put("responseTime", 45);

        log.info("API测试: id={}, params={}", apiId, params);
        return result;
    }

    /**
     * 获取API调用统计
     */
    public Map<String, Object> getApiStats(Long apiId, String period) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("apiId", apiId);
        stats.put("period", period);

        // 模拟统计数据
        stats.put("totalCalls", 1280);
        stats.put("todayCalls", 45);
        stats.put("avgResponseTime", 78);
        stats.put("maxResponseTime", 320);
        stats.put("minResponseTime", 12);
        stats.put("successRate", 99.2);
        stats.put("successCount", 1270);
        stats.put("failedCount", 10);

        // 按小时分布
        List<Map<String, Object>> hourlyDistribution = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourlyDistribution.add(Map.of("hour", h, "count", (int)(Math.random() * 20 + 1)));
        }
        stats.put("hourlyDistribution", hourlyDistribution);

        // 最近调用趋势
        List<Map<String, Object>> recentTrend = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            recentTrend.add(Map.of(
                "date", LocalDateTime.now().minusDays(d).toLocalDate().toString(),
                "calls", (int)(Math.random() * 100 + 30),
                "avgTime", (int)(Math.random() * 50 + 50)
            ));
        }
        stats.put("recentTrend", recentTrend);

        return stats;
    }

    /**
     * 获取API调用日志
     */
    public Map<String, Object> getApiCallLogs(Long apiId, Integer pageNum, Integer pageSize, String status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        // 模拟调用日志数据
        List<Map<String, Object>> logs = new ArrayList<>();
        String[] statuses = {"SUCCESS", "SUCCESS", "SUCCESS", "FAILED", "SUCCESS"};
        String[] ips = {"10.0.1.25", "10.0.1.30", "10.0.2.15", "10.0.1.50", "10.0.3.8"};

        for (int i = 0; i < 5; i++) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("id", (long)(apiId * 1000 + i));
            log.put("apiId", apiId);
            log.put("requestTime", LocalDateTime.now().minusMinutes(i * 30));
            log.put("requestIp", ips[i]);
            log.put("responseCode", "SUCCESS".equals(statuses[i]) ? 200 : 500);
            log.put("responseTime", "SUCCESS".equals(statuses[i]) ? (int)(Math.random() * 200 + 10) : 3000);
            log.put("responseRows", "SUCCESS".equals(statuses[i]) ? (int)(Math.random() * 100 + 1) : 0);
            log.put("status", statuses[i]);
            logs.add(log);
        }

        result.put("list", logs);
        result.put("total", logs.size());
        return result;
    }

    /**
     * 生成API文档
     */
    public Map<String, Object> generateApiDoc(Long apiId) {
        EtlApiService apiService = getById(apiId);
        if (apiService == null) {
            throw new RuntimeException("API服务不存在");
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("apiId", apiId);
        doc.put("name", apiService.getName());
        doc.put("description", apiService.getDescription());

        // 请求信息
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", apiService.getMethod());
        request.put("path", "/api/public/" + apiService.getPath());
        request.put("contentType", "application/json");

        List<Map<String, String>> params = new ArrayList<>();
        params.add(Map.of("name", "pageNum", "type", "integer", "required", "false", "description", "页码"));
        params.add(Map.of("name", "pageSize", "type", "integer", "required", "false", "description", "每页大小"));
        request.put("params", params);

        Map<String, String> auth = new LinkedHashMap<>();
        auth.put("type", apiService.getAuthType());
        if ("TOKEN".equals(apiService.getAuthType())) {
            auth.put("header", "Authorization: Bearer <token>");
        }
        request.put("auth", auth);

        doc.put("request", request);

        // 响应信息
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("contentType", "application/json");

        Map<String, Object> successResponse = new LinkedHashMap<>();
        successResponse.put("code", 200);
        successResponse.put("example", Map.of("code", 200, "message", "成功", "data", List.of()));
        response.put("success", successResponse);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("code", 500);
        errorResponse.put("example", Map.of("code", 500, "message", "内部服务错误"));
        response.put("error", errorResponse);

        doc.put("response", response);

        // SQL模板
        doc.put("sqlTemplate", apiService.getSqlTemplate());
        doc.put("generatedAt", LocalDateTime.now().toString());

        // 错误码
        doc.put("errorCodes", List.of(
            Map.of("code", 400, "message", "请求参数错误"),
            Map.of("code", 401, "message", "认证失败"),
            Map.of("code", 429, "message", "请求频率超限"),
            Map.of("code", 500, "message", "内部服务错误")
        ));

        log.info("生成API文档: id={}, path={}", apiId, apiService.getPath());
        return doc;
    }

    /**
     * 发布API服务（上线并生成公开访问地址）
     */
    public Map<String, Object> publishApi(Long apiId, String currentUser) {
        EtlApiService apiService = getById(apiId);
        if (apiService == null) {
            throw new RuntimeException("API服务不存在");
        }

        // 上线处理
        apiService.setStatus("ONLINE");
        apiService.setUpdateTime(LocalDateTime.now());
        updateById(apiService);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apiId", apiId);
        result.put("name", apiService.getName());
        result.put("publicUrl", "/api/public/" + apiService.getPath());
        result.put("method", apiService.getMethod());
        result.put("status", "ONLINE");
        result.put("publishedBy", currentUser);
        result.put("publishedAt", LocalDateTime.now().toString());

        log.info("API服务发布: id={}, path={}", apiId, apiService.getPath());
        return result;
    }

    /**
     * 获取所有在线API列表（用于外部调用/API目录）
     */
    public List<Map<String, Object>> getPublicApiList() {
        List<EtlApiService> onlineApis = lambdaQuery()
            .eq(EtlApiService::getStatus, "ONLINE")
            .orderByDesc(EtlApiService::getCreateTime)
            .list();

        List<Map<String, Object>> publicList = new ArrayList<>();
        for (EtlApiService api : onlineApis) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", api.getId());
            item.put("name", api.getName());
            item.put("description", api.getDescription());
            item.put("method", api.getMethod());
            item.put("path", "/api/public/" + api.getPath());
            item.put("authType", api.getAuthType());
            item.put("rateLimit", api.getRateLimit());
            item.put("timeout", api.getTimeout());
            publicList.add(item);
        }
        return publicList;
    }
}