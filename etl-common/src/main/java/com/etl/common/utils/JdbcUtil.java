package com.etl.common.utils;

import cn.hutool.core.util.StrUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC工具类
 */
public class JdbcUtil {

    private JdbcUtil() {}

    /**
     * 关闭连接
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    /**
     * 关闭资源
     */
    public static void close(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
        close(conn);
    }

    /**
     * 执行查询，返回List<Map>
     */
    public static List<Map<String, Object>> query(Connection conn, String sql) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                result.add(row);
            }
        }
        return result;
    }

    /**
     * 执行查询，返回单条记录
     */
    public static Map<String, Object> queryOne(Connection conn, String sql) throws SQLException {
        List<Map<String, Object>> list = query(conn, sql);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 执行更新
     */
    public static int update(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * 执行批量更新
     */
    public static int[] batchUpdate(Connection conn, List<String> sqlList) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (String sql : sqlList) {
                if (StrUtil.isNotBlank(sql)) {
                    stmt.addBatch(sql);
                }
            }
            return stmt.executeBatch();
        }
    }

    /**
     * 获取表的行数
     * 注意：tableName需要进行SQL标识符引用以防止注入，建议使用SqlBuilder.quoteIdentifier处理
     */
    public static long count(Connection conn, String tableName) throws SQLException {
        // 对表名进行基本验证，防止SQL注入
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        // 移除可能的危险字符
        String safeTableName = tableName.replaceAll("[;'\"\\-\\-]", "");
        String sql = "SELECT COUNT(*) AS cnt FROM " + safeTableName;
        Map<String, Object> result = queryOne(conn, sql);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).longValue();
        }
        return 0;
    }

    /**
     * 测试连接
     */
    public static boolean testConnection(String url, String username, String password, String driverClass) {
        Connection conn = null;
        try {
            Class.forName(driverClass);
            conn = DriverManager.getConnection(url, username, password);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            close(conn);
        }
    }
}
