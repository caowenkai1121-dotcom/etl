package com.etl.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.datasource.dto.DatasourceCreateRequest;
import com.etl.datasource.dto.DatasourceResponse;
import com.etl.datasource.dto.DatasourceUpdateRequest;
import com.etl.datasource.entity.EtlDatasource;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.cdc.DebeziumConnectorManager;
import com.etl.engine.entity.EtlCdcConfig;
import com.etl.engine.service.CdcConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源管理Controller
 */
@Slf4j
@Tag(name = "数据源管理", description = "数据源的增删改查和连接测试")
@RestController
@RequestMapping("/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;
    private final CdcConfigService cdcConfigService;
    private final DebeziumConnectorManager debeziumConnectorManager;

    @Operation(summary = "获取数据源类型列表")
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        return Result.success(Arrays.stream(DataSourceType.values())
            .map(DataSourceType::getCode)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "获取所有数据源列表")
    @GetMapping("/list")
    public Result<List<DatasourceResponse>> list() {
        List<EtlDatasource> list = datasourceService.listAll();
        List<DatasourceResponse> records = list.stream()
            .map(DatasourceResponse::from)
            .collect(Collectors.toList());
        return Result.success(records);
    }

    @Operation(summary = "分页查询数据源")
    @GetMapping("/page")
    public Result<PageResult<DatasourceResponse>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "名称") @RequestParam(required = false) String name,
            @Parameter(description = "类型") @RequestParam(required = false) String type) {
        Page<EtlDatasource> page = datasourceService.pageList(pageNum, pageSize, name, type);
        List<DatasourceResponse> records = page.getRecords().stream()
            .map(entity -> enrichWithCdcStatus(entity))
            .collect(Collectors.toList());
        return Result.success(PageResult.of(records, page.getTotal(), pageNum, pageSize));
    }

    @Operation(summary = "获取数据源详情")
    @GetMapping("/{id}")
    public Result<DatasourceResponse> get(@PathVariable Long id) {
        EtlDatasource entity = datasourceService.getById(id);
        if (entity == null) {
            return Result.error("数据源不存在");
        }
        return Result.success(enrichWithCdcStatus(entity));
    }

    @Operation(summary = "创建数据源")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DatasourceCreateRequest request) {
        EtlDatasource datasource = new EtlDatasource();
        datasource.setName(request.getName());
        datasource.setType(request.getType());
        datasource.setHost(request.getHost());
        datasource.setPort(request.getPort());
        datasource.setDatabaseName(request.getDatabaseName());
        datasource.setUsername(request.getUsername());
        datasource.setPassword(request.getPassword());
        datasource.setCharset(request.getCharset());
        datasource.setExtraConfig(request.getExtraConfig());
        datasource.setRemark(request.getRemark());

        Long datasourceId = datasourceService.createDatasource(datasource);

        // MySQL/PostgreSQL类型且启用CDC时，自动创建CDC配置并注册Debezium连接器
        String dbType = request.getType();
        boolean supportsCdc = "MYSQL".equalsIgnoreCase(dbType) || "POSTGRESQL".equalsIgnoreCase(dbType);

        if (supportsCdc && Boolean.TRUE.equals(request.getEnableCdc())) {
            try {
                Long cdcConfigId = createCdcConfigForDatasource(datasourceId, request);
                log.info("自动创建CDC配置成功: datasourceId={}, cdcConfigId={}", datasourceId, cdcConfigId);

                // 默认自动部署连接器（如果未明确指定，则默认部署）
                boolean shouldDeploy = request.getDeployConnector() == null || request.getDeployConnector();
                if (shouldDeploy) {
                    boolean deployed = debeziumConnectorManager.deployConnector(cdcConfigId);
                    log.info("自动部署Debezium连接器: datasourceId={}, result={}", datasourceId, deployed ? "成功" : "失败");
                }
            } catch (Exception e) {
                log.warn("自动创建CDC配置失败，数据源已创建: datasourceId={}, error={}", datasourceId, e.getMessage());
            }
        }

        return Result.success(datasourceId);
    }

    @Operation(summary = "更新数据源")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DatasourceUpdateRequest request) {
        EtlDatasource datasource = new EtlDatasource();
        datasource.setId(id);
        datasource.setName(request.getName());
        datasource.setType(request.getType());
        datasource.setHost(request.getHost());
        datasource.setPort(request.getPort());
        datasource.setDatabaseName(request.getDatabaseName());
        datasource.setUsername(request.getUsername());
        datasource.setPassword(request.getPassword());
        datasource.setCharset(request.getCharset());
        datasource.setExtraConfig(request.getExtraConfig());
        datasource.setRemark(request.getRemark());
        datasourceService.updateDatasource(datasource);
        return Result.success();
    }

    @Operation(summary = "删除数据源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 先删除关联的CDC配置和Debezium连接器
        EtlCdcConfig cdcConfig = cdcConfigService.getByDatasourceId(id);
        if (cdcConfig != null) {
            try {
                // 先删除Debezium连接器
                if (cdcConfig.getConnectorName() != null) {
                    debeziumConnectorManager.deleteConnector(cdcConfig.getConnectorName());
                    log.info("删除Debezium连接器: connectorName={}", cdcConfig.getConnectorName());
                }
                // 再删除CDC配置
                cdcConfigService.deleteConfig(cdcConfig.getId());
                log.info("级联删除CDC配置: datasourceId={}, cdcConfigId={}", id, cdcConfig.getId());
            } catch (Exception e) {
                log.warn("删除CDC配置失败: datasourceId={}, error={}", id, e.getMessage());
            }
        }

        datasourceService.deleteDatasource(id);
        return Result.success();
    }

    @Operation(summary = "测试连接")
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return Result.success(datasourceService.testConnection(id));
    }

    @Operation(summary = "获取数据源所有表")
    @GetMapping("/{id}/tables")
    public Result<List<TableInfo>> getTables(@PathVariable Long id) {
        return Result.success(datasourceService.getTables(id));
    }

    @Operation(summary = "获取表信息")
    @GetMapping("/{id}/tables/{tableName}")
    public Result<TableInfo> getTableInfo(@PathVariable Long id, @PathVariable String tableName) {
        return Result.success(datasourceService.getTableInfo(id, tableName));
    }

    @Operation(summary = "获取表字段")
    @GetMapping("/{id}/tables/{tableName}/columns")
    public Result<List<ColumnInfo>> getColumns(@PathVariable Long id, @PathVariable String tableName) {
        return Result.success(datasourceService.getColumns(id, tableName));
    }

    @Operation(summary = "启用/禁用数据源")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        datasourceService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 为数据源创建CDC配置（使用Debezium）
     * @return 返回创建的CDC配置ID
     */
    private Long createCdcConfigForDatasource(Long datasourceId, DatasourceCreateRequest request) {
        EtlCdcConfig cdcConfig = new EtlCdcConfig();
        cdcConfig.setDatasourceId(datasourceId);
        cdcConfig.setName(request.getName() + "-CDC");

        // 确定连接器类型
        String connectorType = determineConnectorType(request.getType());
        cdcConfig.setConnectorType(connectorType);

        // 生成唯一的连接器名称
        String connectorName = "etl-" + connectorType + "-" + datasourceId;
        cdcConfig.setConnectorName(connectorName);

        // 生成服务器名称（Kafka Topic前缀）
        String serverName = "etl-" + connectorType + "-" + datasourceId;
        cdcConfig.setServerName(serverName);
        cdcConfig.setKafkaTopicPrefix(serverName);

        // 设置表过滤规则
        String filterRegex = request.getCdcFilterRegex();
        if (filterRegex == null || filterRegex.isBlank()) {
            // 默认同步该数据源对应数据库的所有表
            filterRegex = request.getDatabaseName() + "\\..*";
        }
        cdcConfig.setFilterRegex(filterRegex);
        cdcConfig.setFilterBlackRegex(request.getCdcFilterBlackRegex());

        cdcConfig.setStatus(1);
        cdcConfig.setSyncStatus("STOPPED");

        // 创建CDC配置并返回ID
        return cdcConfigService.createConfig(cdcConfig);
    }

    /**
     * 确定Debezium连接器类型
     */
    private String determineConnectorType(String datasourceType) {
        if ("MYSQL".equalsIgnoreCase(datasourceType)) {
            return "mysql";
        } else if ("POSTGRESQL".equalsIgnoreCase(datasourceType)) {
            return "postgresql";
        }
        throw new IllegalArgumentException("不支持的CDC数据源类型: " + datasourceType);
    }

    /**
     * 为响应添加CDC状态信息
     */
    private DatasourceResponse enrichWithCdcStatus(EtlDatasource entity) {
        DatasourceResponse response = DatasourceResponse.from(entity);
        if (entity == null) {
            return response;
        }

        // 查询关联的CDC配置
        EtlCdcConfig cdcConfig = cdcConfigService.getByDatasourceId(entity.getId());
        if (cdcConfig != null) {
            response.setCdcConfigId(cdcConfig.getId());
            response.setCdcSyncStatus(cdcConfig.getSyncStatus());
            response.setCdcEnabled(cdcConfig.getStatus() == 1);
        } else {
            response.setCdcEnabled(false);
        }

        return response;
    }
}
