package com.etl.api.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.datasource.entity.EtlDatasource;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.cdc.DebeziumConnectorManager;
import com.etl.engine.entity.EtlCdcConfig;
import com.etl.engine.service.CdcConfigService;
import com.etl.api.dto.CdcConfigRequest;
import com.etl.api.dto.CdcConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CDC配置管理Controller
 */
@Tag(name = "CDC配置管理", description = "Debezium连接器配置的增删改查和部署")
@RestController
@RequestMapping("/cdc-config")
@RequiredArgsConstructor
public class CdcConfigController {

    private final CdcConfigService cdcConfigService;
    private final DatasourceService datasourceService;
    private final DebeziumConnectorManager debeziumConnectorManager;

    @Operation(summary = "获取所有CDC配置列表")
    @GetMapping("/list")
    public Result<List<CdcConfigResponse>> list() {
        List<EtlCdcConfig> configs = cdcConfigService.listEnabled();
        List<CdcConfigResponse> responses = configs.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "分页查询CDC配置")
    @GetMapping("/page")
    public Result<PageResult<CdcConfigResponse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "配置名称") @RequestParam(required = false) String name,
            @Parameter(description = "同步状态") @RequestParam(required = false) String syncStatus) {
        Page<EtlCdcConfig> page = cdcConfigService.pageList(pageNum, pageSize, name, syncStatus);
        List<CdcConfigResponse> records = page.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(PageResult.of(records, page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取CDC配置详情")
    @GetMapping("/{id}")
    public Result<CdcConfigResponse> get(@PathVariable Long id) {
        EtlCdcConfig config = cdcConfigService.getById(id);
        if (config == null) {
            return Result.error("CDC配置不存在");
        }
        return Result.success(toResponse(config));
    }

    @Operation(summary = "创建CDC配置")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CdcConfigRequest request) {
        EtlCdcConfig config = new EtlCdcConfig();
        config.setName(request.getName());
        config.setDatasourceId(request.getDatasourceId());
        config.setConnectorName(request.getConnectorName());
        config.setConnectorType(request.getConnectorType());
        config.setServerName(request.getServerName());
        config.setDatabaseHost(request.getDatabaseHost());
        config.setDatabasePort(request.getDatabasePort());
        config.setDbUsername(request.getDbUsername());
        config.setDbPassword(request.getDbPassword());
        config.setFilterRegex(request.getFilterRegex());
        config.setFilterBlackRegex(request.getFilterBlackRegex());
        config.setKafkaTopicPrefix(request.getKafkaTopicPrefix());
        config.setExtraConfig(request.getExtraConfig());
        config.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        Long id = cdcConfigService.createConfig(config);
        return Result.success(id);
    }

    @Operation(summary = "更新CDC配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CdcConfigRequest request) {
        EtlCdcConfig config = new EtlCdcConfig();
        config.setId(id);
        config.setName(request.getName());
        config.setDatasourceId(request.getDatasourceId());
        config.setConnectorName(request.getConnectorName());
        config.setConnectorType(request.getConnectorType());
        config.setServerName(request.getServerName());
        config.setDatabaseHost(request.getDatabaseHost());
        config.setDatabasePort(request.getDatabasePort());
        config.setDbUsername(request.getDbUsername());
        config.setDbPassword(request.getDbPassword());
        config.setFilterRegex(request.getFilterRegex());
        config.setFilterBlackRegex(request.getFilterBlackRegex());
        config.setKafkaTopicPrefix(request.getKafkaTopicPrefix());
        config.setExtraConfig(request.getExtraConfig());
        config.setStatus(request.getStatus());

        cdcConfigService.updateConfig(config);
        return Result.success();
    }

    @Operation(summary = "删除CDC配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cdcConfigService.deleteConfig(id);
        return Result.success();
    }

    @Operation(summary = "部署CDC配置到Debezium Connect")
    @PostMapping("/{id}/deploy")
    public Result<Boolean> deploy(@PathVariable Long id) {
        boolean success = debeziumConnectorManager.deployConnector(id);
        return Result.success(success);
    }

    @Operation(summary = "启动CDC连接器")
    @PostMapping("/{id}/start")
    public Result<Boolean> start(@PathVariable Long id) {
        EtlCdcConfig config = cdcConfigService.getById(id);
        if (config == null) {
            return Result.error("CDC配置不存在");
        }

        String connectorName = config.getConnectorName();

        // 检查连接器是否存在，不存在则先部署
        boolean connectorExists = debeziumConnectorManager.checkConnectorExists(connectorName);

        boolean success;
        if (!connectorExists) {
            // 连接器不存在，先部署
            success = debeziumConnectorManager.deployConnector(id);
        } else {
            // 连接器已存在，尝试恢复
            success = debeziumConnectorManager.startConnector(connectorName);
        }

        if (success) {
            cdcConfigService.updateSyncStatus(id, "RUNNING", null);
        }
        return Result.success(success);
    }

    @Operation(summary = "停止CDC连接器")
    @PostMapping("/{id}/stop")
    public Result<Boolean> stop(@PathVariable Long id) {
        EtlCdcConfig config = cdcConfigService.getById(id);
        if (config == null) {
            return Result.error("CDC配置不存在");
        }

        boolean success = debeziumConnectorManager.pauseConnector(config.getConnectorName());
        if (success) {
            cdcConfigService.updateSyncStatus(id, "STOPPED", null);
        }
        return Result.success(success);
    }

    @Operation(summary = "获取Debezium连接器状态")
    @GetMapping("/{id}/status")
    public Result<JSONObject> getStatus(@PathVariable Long id) {
        EtlCdcConfig config = cdcConfigService.getById(id);
        if (config == null) {
            return Result.error("CDC配置不存在");
        }

        JSONObject status = debeziumConnectorManager.getConnectorStatus(config.getConnectorName());
        return Result.success(status);
    }

    @Operation(summary = "启用/禁用CDC配置")
    @PutMapping("/{id}/enable")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        cdcConfigService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 转换为响应对象
     */
    private CdcConfigResponse toResponse(EtlCdcConfig config) {
        if (config == null) {
            return null;
        }
        // 获取数据源名称
        String datasourceName = null;
        EtlDatasource datasource = datasourceService.getById(config.getDatasourceId());
        if (datasource != null) {
            datasourceName = datasource.getName();
        }
        return CdcConfigResponse.from(config, datasourceName);
    }
}
