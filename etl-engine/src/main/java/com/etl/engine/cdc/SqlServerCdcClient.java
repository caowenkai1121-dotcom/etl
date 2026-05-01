package com.etl.engine.cdc;

import com.etl.common.domain.CdcEvent;
import com.etl.common.enums.CdcEventType;
import com.etl.datasource.connector.CdcReader;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * SQL Server CDC 客户端
 * 使用 SQL Server 原生 CDC 功能实现变更捕获
 */
@Slf4j
public class SqlServerCdcClient implements CdcReader {

    private final Connection connection;
    private final String schemaName;
    private final String tableName;
    private byte[] currentLsn;
    private volatile boolean running = false;

    public SqlServerCdcClient(Connection connection, String schemaName, String tableName, byte[] startLsn) {
        this.connection = connection;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.currentLsn = startLsn;
    }

    @Override
    public void start(Consumer<CdcEvent> eventConsumer) {
        running = true;
        log.info("启动 SQL Server CDC: schema={}, table={}", schemaName, tableName);

        try {
            // 获取捕获实例名称
            String captureInstance = getCaptureInstance();

            while (running) {
                try {
                    // 查询变更数据
                    List<CdcEvent> events = queryChanges(captureInstance);

                    // 处理事件
                    for (CdcEvent event : events) {
                        eventConsumer.accept(event);
                        String pos = event.getPosition();
                        if (pos != null) {
                            currentLsn = Base64.getDecoder().decode(pos);
                        }
                    }

                    // 等待下一次轮询
                    Thread.sleep(1000);

                } catch (SQLException e) {
                    log.error("SQL Server CDC 查询失败", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取 SQL Server CDC 捕获实例失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getCaptureInstance() throws SQLException {
        String sql = "SELECT capture_instance FROM cdc.change_tables WHERE source_schema = ? AND source_table = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("capture_instance");
                }
            }
        }
        throw new SQLException("CDC 未启用: " + schemaName + "." + tableName);
    }

    private List<CdcEvent> queryChanges(String captureInstance) throws SQLException {
        List<CdcEvent> events = new ArrayList<>();

        String lsnParam = currentLsn != null ?
            "sys.fn_varbintohexstr(?)" : "sys.fn_cdc_get_min_lsn(?)";

        String sql = String.format(
            "SELECT __$operation, __$start_lsn, * " +
            "FROM cdc.fn_cdc_get_all_changes_%s(%s, sys.fn_cdc_get_max_lsn(), 'all')",
            captureInstance, lsnParam
        );

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (currentLsn != null) {
                stmt.setBytes(1, currentLsn);
            } else {
                stmt.setString(1, captureInstance);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    CdcEvent event = new CdcEvent();
                    int operation = rs.getInt("__$operation");
                    byte[] lsn = rs.getBytes("__$start_lsn");

                    event.setEventType(mapOperation(operation));
                    event.setPosition(lsn != null ? Base64.getEncoder().encodeToString(lsn) : null);
                    event.setTable(tableName);
                    event.setTimestamp(System.currentTimeMillis());

                    // 提取数据列（排除 CDC 系统列）
                    Map<String, Object> afterData = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        if (!columnName.startsWith("__$")) {
                            afterData.put(columnName, rs.getObject(i));
                        }
                    }
                    event.setAfterData(afterData);

                    events.add(event);
                }
            }
        }

        return events;
    }

    private CdcEventType mapOperation(int operation) {
        return switch (operation) {
            case 1 -> CdcEventType.DELETE;
            case 2 -> CdcEventType.INSERT;
            case 3, 4 -> CdcEventType.UPDATE;
            default -> null;
        };
    }

    @Override
    public void stop() {
        running = false;
        log.info("停止 SQL Server CDC");
    }

    @Override
    public String getPosition() {
        return currentLsn != null ? Base64.getEncoder().encodeToString(currentLsn) : null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
