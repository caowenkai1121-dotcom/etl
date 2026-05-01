package com.etl.engine.cdc;

import com.etl.common.domain.CdcEvent;
import com.etl.common.enums.CdcEventType;
import com.etl.datasource.connector.CdcReader;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Oracle LogMiner CDC 客户端
 * 使用 Oracle LogMiner 读取 Redo 日志实现 CDC
 */
@Slf4j
public class OracleLogMinerClient implements CdcReader {

    private final Connection connection;
    private final String schemaName;
    private final String tableName;
    private Long currentScn;
    private volatile boolean running = false;

    public OracleLogMinerClient(Connection connection, String schemaName, String tableName, Long startScn) {
        this.connection = connection;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.currentScn = startScn;
    }

    @Override
    public void start(Consumer<CdcEvent> eventConsumer) {
        running = true;
        log.info("启动 Oracle LogMiner CDC: schema={}, table={}, startScn={}", schemaName, tableName, currentScn);

        while (running) {
            try {
                // 1. 构建 LogMiner 挖掘范围
                buildLogMinerSession();

                // 2. 查询变更数据
                List<CdcEvent> events = queryChanges();

                // 3. 处理事件
                for (CdcEvent event : events) {
                    eventConsumer.accept(event);
                    String pos = event.getPosition();
                    if (pos != null) {
                        try {
                            currentScn = Long.parseLong(pos);
                        } catch (NumberFormatException ignored) {
                            // keep current SCN
                        }
                    }
                }

                // 4. 关闭当前挖掘会话
                closeLogMinerSession();

                // 5. 等待下一次轮询
                Thread.sleep(1000);

            } catch (SQLException e) {
                log.error("Oracle LogMiner 查询失败", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void buildLogMinerSession() throws SQLException {
        long currentDbScn = getCurrentScn();

        String sql = String.format(
            "BEGIN DBMS_LOGMNR.START_LOGMNR(" +
            "STARTSCN => %d, " +
            "ENDSCN => %d, " +
            "OPTIONS => DBMS_LOGMNR.DICT_FROM_ONLINE_CATALOG + DBMS_LOGMNR.CONTINUOUS_MINE); END;",
            currentScn != null ? currentScn : 0,
            currentDbScn
        );

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private List<CdcEvent> queryChanges() throws SQLException {
        List<CdcEvent> events = new ArrayList<>();

        String sql = String.format(
            "SELECT SCN, SQL_REDO, OPERATION, TABLE_NAME " +
            "FROM V$LOGMNR_CONTENTS " +
            "WHERE SEG_OWNER = '%s' AND TABLE_NAME = '%s' " +
            "AND OPERATION IN ('INSERT', 'UPDATE', 'DELETE')",
            schemaName.toUpperCase(), tableName.toUpperCase()
        );

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                CdcEvent event = new CdcEvent();
                long scn = rs.getLong("SCN");
                String operation = rs.getString("OPERATION");

                event.setPosition(String.valueOf(scn));
                event.setTable(rs.getString("TABLE_NAME"));
                event.setEventType(mapOperation(operation));
                event.setTimestamp(System.currentTimeMillis());

                // 将 SQL_REDO 存入 afterData 供下游解析
                Map<String, Object> afterData = new HashMap<>();
                afterData.put("sql_redo", rs.getString("SQL_REDO"));
                afterData.put("scn", scn);
                event.setAfterData(afterData);

                events.add(event);
            }
        }

        return events;
    }

    private CdcEventType mapOperation(String operation) {
        if (operation == null) {
            return null;
        }
        return switch (operation.toUpperCase()) {
            case "INSERT" -> CdcEventType.INSERT;
            case "UPDATE" -> CdcEventType.UPDATE;
            case "DELETE" -> CdcEventType.DELETE;
            default -> null;
        };
    }

    private void closeLogMinerSession() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("BEGIN DBMS_LOGMNR.END_LOGMNR; END;");
        }
    }

    private long getCurrentScn() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CURRENT_SCN FROM V$DATABASE")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    @Override
    public void stop() {
        running = false;
        log.info("停止 Oracle LogMiner CDC");
    }

    @Override
    public String getPosition() {
        return currentScn != null ? String.valueOf(currentScn) : null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
