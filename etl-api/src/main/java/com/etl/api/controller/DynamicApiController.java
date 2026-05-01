package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.entity.EtlApiService;
import com.etl.engine.service.ApiManageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态API执行Controller
 * 拦截 /data-api/** 路径，动态执行SQL查询
 */
@Slf4j
@RestController
@RequestMapping("/data-api")
@RequiredArgsConstructor
public class DynamicApiController {

    private final ApiManageService apiManageService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 动态执行GET请求的API
     */
    @GetMapping("/**")
    public Result<Object> executeGet(HttpServletRequest request) {
        String path = request.getRequestURI().replace("/api/data-api", "");
        return executeApi(path, "GET", request.getParameterMap());
    }

    /**
     * 动态执行POST请求的API
     */
    @PostMapping("/**")
    public Result<Object> executePost(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String path = request.getRequestURI().replace("/api/data-api", "");
        return executeApi(path, "POST", body != null ? body : new HashMap<>());
    }

    /**
     * 执行API
     */
    private Result<Object> executeApi(String path, String method, Map<String, ?> params) {
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIp();

        try {
            // 获取API配置
            EtlApiService apiService = apiManageService.getByPath(path, method);
            if (apiService == null) {
                log.warn("API不存在或已下线: path={}, method={}", path, method);
                return Result.error(404, "API不存在或已下线");
            }

            // TODO: 认证校验
            // validateAuth(apiService, request);

            // 替换SQL参数
            String sql = replaceSqlParams(apiService.getSqlTemplate(), params);

            log.info("执行API: name={}, path={}, sql={}", apiService.getName(), path, sql);

            // 执行查询
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);

            // 记录调用日志
            long responseTime = System.currentTimeMillis() - startTime;
            logApiCall(apiService, clientIp, params, responseTime, 200, data.size(), "SUCCESS", null);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("total", data.size());
            return Result.success(result);

        } catch (Exception e) {
            log.error("API执行失败: path={}, method={}", path, method, e);
            long responseTime = System.currentTimeMillis() - startTime;
            // TODO: 记录失败日志
            return Result.error(500, "API执行失败: " + e.getMessage());
        }
    }

    /**
     * 替换SQL参数
     */
    private String replaceSqlParams(String sqlTemplate, Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return sqlTemplate;
        }

        String sql = sqlTemplate;
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            sql = sql.replace(placeholder, value);
        }
        return sql;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        return "unknown";
    }

    /**
     * 记录API调用日志
     */
    private void logApiCall(EtlApiService apiService, String clientIp, Map<String, ?> params,
                           long responseTime, int responseCode, int responseRows, String status, String errorMessage) {
        log.info("API调用: apiId={}, name={}, ip={}, time={}ms, rows={}, status={}",
            apiService.getId(), apiService.getName(), clientIp, responseTime, responseRows, status);
        // TODO: 写入数据库日志表
    }
}
