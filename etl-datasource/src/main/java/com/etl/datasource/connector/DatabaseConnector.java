package com.etl.datasource.connector;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接器接口
 */
public interface DatabaseConnector {

    // ==================== 连接管理 ====================

    /**
     * 获取连接
     */
    Connection getConnection() throws Exception;

    /**
     * 测试连接
     */
    boolean testConnection();

    /**
     * 关闭连接
     */
    void close();

    /**
     * 获取数据库类型
     */
    String getDatabaseType();

    /**
     * 支持的数据源类型
     */
    DataSourceType supportsType();

    // ==================== Schema 元数据 ====================

    /**
     * 获取所有数据库/Schema列表
     */
    default List<String> listDatabases() throws Exception {
        return List.of(getDatabaseName());
    }

    /**
     * 获取所有表
     */
    List<TableInfo> getTables() throws Exception;

    /**
     * 获取指定数据库的所有表
     */
    default List<TableInfo> getTables(String database) throws Exception {
        return getTables();
    }

    /**
     * 获取指定表信息
     */
    TableInfo getTableInfo(String tableName) throws Exception;

    /**
     * 获取表字段信息
     */
    List<ColumnInfo> getColumns(String tableName) throws Exception;

    /**
     * 获取主键
     */
    List<String> getPrimaryKeys(String tableName) throws Exception;

    // ==================== 数据操作 ====================

    /**
     * 获取表行数
     */
    long getRowCount(String tableName) throws Exception;

    /**
     * 执行查询
     */
    default java.sql.ResultSet query(String sql) throws Exception {
        throw new UnsupportedOperationException("query not implemented");
    }

    /**
     * 执行更新/删除
     */
    default int execute(String sql) throws Exception {
        throw new UnsupportedOperationException("execute not implemented");
    }

    /**
     * 批量插入
     */
    default int[] batchInsert(String tableName, List<Map<String, Object>> data) throws Exception {
        throw new UnsupportedOperationException("batchInsert not implemented");
    }

    // ==================== 连接信息 ====================

    /**
     * 获取JDBC URL
     */
    String getJdbcUrl();

    /**
     * 获取主机地址
     */
    String getHost();

    /**
     * 获取端口
     */
    Integer getPort();

    /**
     * 获取数据库名
     */
    String getDatabaseName();

    /**
     * 获取用户名
     */
    String getUsername();

    /**
     * 获取密码
     */
    String getPassword();

    // ==================== CDC 支持（可选） ====================

    /**
     * 是否支持 CDC
     */
    default boolean supportsCdc() {
        return false;
    }

    /**
     * 创建 CDC 读取器
     */
    default CdcReader createCdcReader(CdcConfig config) {
        throw new UnsupportedOperationException("CDC not supported for this data source");
    }
}
