package com.etl.engine.schema;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 表结构同步服务
 */
@Slf4j
@RequiredArgsConstructor
public class TableSchemaService {

    private final DatasourceService datasourceService;
    private final TypeMappingService typeMappingService;

    /**
     * 在目标库创建表
     */
    public void createTargetTable(Long sourceDsId, Long targetDsId, String sourceTable,
                                   String targetTable, String fieldMappingJson) throws Exception {
        DatabaseConnector sourceConnector = datasourceService.getConnector(sourceDsId);
        DatabaseConnector targetConnector = datasourceService.getConnector(targetDsId);

        // 检查目标表是否存在
        TableInfo existingTargetTable = targetConnector.getTableInfo(targetTable);
        if (existingTargetTable != null) {
            log.info("目标表已存在: {}", targetTable);
            return;
        }

        // 获取源表信息
        TableInfo sourceTableInfo = sourceConnector.getTableInfo(sourceTable);
        if (sourceTableInfo == null) {
            throw new RuntimeException("源表不存在: " + sourceTable);
        }

        // 生成建表SQL
        TableInfo targetTableInfo = new TableInfo();
        targetTableInfo.setTableName(targetTable);
        targetTableInfo.setTableComment(sourceTableInfo.getTableComment());
        targetTableInfo.setColumns(sourceTableInfo.getColumns().stream().map(column -> {
            ColumnInfo newColumn = new ColumnInfo();
            newColumn.setColumnName(column.getColumnName());
            newColumn.setColumnType(typeMappingService.mapType(
                sourceConnector.getDatabaseType(), column.getColumnType(), targetConnector.getDatabaseType()));
            newColumn.setColumnLength(column.getColumnLength());
            newColumn.setDecimalDigits(column.getDecimalDigits());
            newColumn.setNullable(column.getNullable());
            newColumn.setAutoIncrement(column.getAutoIncrement());
            newColumn.setColumnComment(column.getColumnComment());
            return newColumn;
        }).toList());
        targetTableInfo.setPrimaryKeys(sourceTableInfo.getPrimaryKeys());

        TypeMappingService.CreateConfig config = new TypeMappingService.CreateConfig();
        config.setPrimaryKeys(sourceTableInfo.getPrimaryKeys().toArray(new String[0]));
        if (!sourceTableInfo.getPrimaryKeys().isEmpty()) {
            config.setDistributionKey(sourceTableInfo.getPrimaryKeys().get(0));
        }

        String createSql = typeMappingService.getCreateTableDialect(
            targetConnector.getDatabaseType(), targetTableInfo, config);

        log.info("创建目标表: {}", targetTable);
        log.debug("建表SQL: {}", createSql);

        // 执行建表
        try (var conn = targetConnector.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(createSql);
        }
    }

}
