package com.etl.engine.full;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.SyncEngine;
import com.etl.engine.concurrent.ProgressTracker;
import com.etl.engine.concurrent.ThreadPoolManager;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 并行同步引擎
 * 用于大表分片并行同步
 */
@Slf4j
public class ParallelSyncEngine implements SyncEngine {

    private final SyncTaskService syncTaskService;
    private final TaskExecutionService taskExecutionService;
    private final DatasourceService datasourceService;
    private final ThreadPoolManager threadPoolManager;

    @Getter
    private volatile boolean running = false;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    // 并行配置
    private final int parallelThreads;
    private final int batchSize;
    private final long chunkSize; // 每个分片的行数

    // 总进度追踪器
    private final ProgressTracker totalProgressTracker = new ProgressTracker();

    public ParallelSyncEngine(SyncTaskService syncTaskService,
                             TaskExecutionService taskExecutionService,
                             DatasourceService datasourceService,
                             ThreadPoolManager threadPoolManager,
                             int parallelThreads,
                             int batchSize,
                             long chunkSize) {
        this.syncTaskService = syncTaskService;
        this.taskExecutionService = taskExecutionService;
        this.datasourceService = datasourceService;
        this.threadPoolManager = threadPoolManager;
        this.parallelThreads = Math.max(1, parallelThreads);
        this.batchSize = Math.max(100, batchSize);
        this.chunkSize = Math.max(10000, chunkSize);
    }

    @Override
    public void sync(SyncPipelineContext context) throws Exception {
        running = true;
        stopped.set(false);
        totalProgressTracker.start();

        EtlSyncTask task = syncTaskService.getDetail(context.getTaskId());

        try {
            // 获取连接器
            DatabaseConnector sourceConnector = datasourceService.getConnector(task.getSourceDsId());
            DatabaseConnector targetConnector = datasourceService.getConnector(task.getTargetDsId());

            // 获取主键范围
            String tableName = context.getSourceTable();
            String primaryKey = getPrimaryKey(sourceConnector, tableName);

            if (primaryKey == null) {
                log.warn("表 {} 没有主键，无法并行分片，回退到普通同步", tableName);
                throw new UnsupportedOperationException("表没有主键，无法分片并行同步");
            }

            // 获取主键范围
            KeyRange keyRange = getKeyRange(sourceConnector, tableName, primaryKey);
            log.info("表 {} 主键范围: {} - {}", tableName, keyRange.getMin(), keyRange.getMax());

            // 计算分片
            List<Chunk> chunks = calculateChunks(sourceConnector, tableName, primaryKey, keyRange);
            log.info("表 {} 分为 {} 个分片", tableName, chunks.size());

            // 初始化总行数估算
            long estimatedTotal = estimateTotalRows(sourceConnector, tableName);
            totalProgressTracker.initTotalRows(estimatedTotal);

            List<Future<ChunkResult>> futures = new ArrayList<>();

            // 提交分片任务
            for (Chunk chunk : chunks) {
                if (stopped.get()) {
                    break;
                }

                // 获取全局并发许可
                if (!threadPoolManager.tryAcquireConcurrency()) {
                    log.info("等待并发许可，当前分片: {}", chunk);
                    threadPoolManager.acquireConcurrency();
                }

                Future<ChunkResult> future = threadPoolManager.getSyncExecutor().submit(() -> {
                    try {
                        return syncChunkWithRetry(sourceConnector, targetConnector, tableName,
                            context.getTargetTable(), chunk, task, 2);
                    } finally {
                        threadPoolManager.releaseConcurrency();
                    }
                });
                futures.add(future);
            }

            // 等待所有分片完成
            for (Future<ChunkResult> future : futures) {
                try {
                    ChunkResult result = future.get();
                    totalProgressTracker.addProcessedRows(result.getTotalRows());
                    totalProgressTracker.addSuccessRows(result.getSuccessRows());
                    totalProgressTracker.addFailedRows(result.getFailedRows());
                    totalProgressTracker.recordRateSample();

                } catch (ExecutionException e) {
                    log.error("分片同步失败", e.getCause());
                    totalProgressTracker.addFailedRows(1);
                }
            }

            log.info("并行同步完成: totalRows={}, successRows={}, failedRows={}, progress={}%",
                totalProgressTracker.getTotalRows(),
                totalProgressTracker.getSuccessRows(),
                totalProgressTracker.getFailedRows(),
                totalProgressTracker.getProgress());

        } catch (Exception e) {
            log.error("并行同步失败: taskId={}", task.getId(), e);
            throw e;
        } finally {
            running = false;
        }
    }

    /**
     * 同步单个分片（带重试）
     */
    private ChunkResult syncChunkWithRetry(DatabaseConnector sourceConnector, DatabaseConnector targetConnector,
                                            String sourceTable, String targetTable, Chunk chunk,
                                            EtlSyncTask task, int maxRetries) {
        int retryCount = 0;
        while (retryCount <= maxRetries) {
            try {
                ProgressTracker chunkTracker = new ProgressTracker();
                chunkTracker.start();
                ChunkResult result = syncChunk(sourceConnector, targetConnector, sourceTable, targetTable,
                    chunk, task, chunkTracker);

                if (retryCount > 0) {
                    log.info("分片同步成功（重试{}次）: {}", retryCount, chunk);
                }
                return result;

            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("分片同步失败，已达最大重试次数: {}", chunk, e);
                    ChunkResult result = new ChunkResult();
                    result.failedRows = 1;
                    return result;
                }
                log.warn("分片同步失败，准备重试（{}/{}）: {}", retryCount, maxRetries, chunk);
                try {
                    Thread.sleep(1000 * retryCount); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    ChunkResult result = new ChunkResult();
                    result.failedRows = 1;
                    return result;
                }
            }
        }
        ChunkResult result = new ChunkResult();
        result.failedRows = 1;
        return result;
    }

    /**
     * 获取主键列名
     */
    private String getPrimaryKey(DatabaseConnector connector, String tableName) throws Exception {
        String quotedTable = quoteIdentifier(connector, tableName);
        String sql = String.format(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
            "WHERE TABLE_NAME = '%s' AND CONSTRAINT_NAME = 'PRIMARY' ORDER BY ORDINAL_POSITION",
            tableName
        );

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("COLUMN_NAME");
            }
        }
        return null;
    }

    /**
     * 获取主键范围
     */
    private KeyRange getKeyRange(DatabaseConnector connector, String tableName, String primaryKey) throws Exception {
        String quotedTable = quoteIdentifier(connector, tableName);
        String quotedPk = quoteColumn(connector, primaryKey);
        String sql = String.format("SELECT MIN(%s) as min_val, MAX(%s) as max_val FROM %s",
            quotedPk, quotedPk, quotedTable);

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return new KeyRange(rs.getObject("min_val"), rs.getObject("max_val"));
            }
        }
        return new KeyRange(0, 0);
    }

    /**
     * 估算总行数
     */
    private long estimateTotalRows(DatabaseConnector connector, String tableName) {
        try {
            String quotedTable = quoteIdentifier(connector, tableName);
            String sql = String.format("SELECT COUNT(*) FROM %s", quotedTable);

            try (Connection conn = connector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            log.warn("估算总行数失败: {}", tableName, e);
        }
        return 0;
    }

    /**
     * 计算分片
     */
    private List<Chunk> calculateChunks(DatabaseConnector connector, String tableName,
                                         String primaryKey, KeyRange keyRange) {
        List<Chunk> chunks = new ArrayList<>();

        Object min = keyRange.getMin();
        Object max = keyRange.getMax();

        if (min instanceof Number && max instanceof Number) {
            // 数值类型主键，使用范围分片
            long minVal = ((Number) min).longValue();
            long maxVal = ((Number) max).longValue();
            long range = maxVal - minVal;

            int chunkCount = (int) Math.ceil((double) range / chunkSize);

            for (int i = 0; i < chunkCount; i++) {
                long chunkMin = minVal + (long) i * chunkSize;
                long chunkMax = Math.min(minVal + (long) (i + 1) * chunkSize - 1, maxVal);
                chunks.add(new Chunk(primaryKey, chunkMin, chunkMax, ChunkType.RANGE));
            }
        } else {
            // 非数值类型主键，使用哈希分片
            int shardCount = Math.max(parallelThreads, 4);
            for (int i = 0; i < shardCount; i++) {
                chunks.add(new Chunk(primaryKey, i, shardCount, ChunkType.HASH));
            }
        }

        return chunks;
    }

    /**
     * 同步单个分片
     */
    private ChunkResult syncChunk(DatabaseConnector sourceConnector, DatabaseConnector targetConnector,
                                  String sourceTable, String targetTable, Chunk chunk, EtlSyncTask task,
                                  ProgressTracker chunkTracker) {
        ChunkResult result = new ChunkResult();

        String quotedSourceTable = quoteIdentifier(sourceConnector, sourceTable);
        String quotedTargetTable = quoteIdentifier(targetConnector, targetTable);
        String quotedPk = quoteColumn(sourceConnector, chunk.getPrimaryKey());

        String selectSql;
        if (chunk.getType() == ChunkType.RANGE) {
            selectSql = String.format(
                "SELECT * FROM %s WHERE %s >= ? AND %s <= ?",
                quotedSourceTable, quotedPk, quotedPk
            );
        } else {
            // HASH 分片: MOD(HASH(主键), 分片数) = 分片索引
            String dbType = sourceConnector.getDatabaseType();
            if ("POSTGRESQL".equals(dbType)) {
                selectSql = String.format(
                    "SELECT * FROM %s WHERE MOD(ABS(HASHTEXT(%s::TEXT)), ?) = ?",
                    quotedSourceTable, quotedPk
                );
            } else {
                // MySQL
                selectSql = String.format(
                    "SELECT * FROM %s WHERE MOD(ABS(CRC32(%s)), ?) = ?",
                    quotedSourceTable, quotedPk
                );
            }
        }

        try (Connection sourceConn = sourceConnector.getConnection();
             Connection targetConn = targetConnector.getConnection();
             PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql)) {

            if (chunk.getType() == ChunkType.RANGE) {
                selectStmt.setObject(1, chunk.getMin());
                selectStmt.setObject(2, chunk.getMax());
            } else {
                selectStmt.setInt(1, (Integer) chunk.getMax()); // 分片总数
                selectStmt.setInt(2, (Integer) chunk.getMin()); // 分片索引
            }

            try (ResultSet rs = selectStmt.executeQuery()) {
                targetConn.setAutoCommit(false);

                PreparedStatement insertStmt = null;
                int batchCount = 0;

                while (rs.next() && !stopped.get()) {
                    if (insertStmt == null) {
                        insertStmt = buildInsertStatement(targetConn, targetTable, rs);
                    }

                    // 复制行数据
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        insertStmt.setObject(i, rs.getObject(i));
                    }

                    insertStmt.addBatch();
                    batchCount++;
                    result.totalRows++;
                    chunkTracker.addProcessedRows(1);

                    if (batchCount >= batchSize) {
                        insertStmt.executeBatch();
                        targetConn.commit();
                        result.successRows += batchCount;
                        chunkTracker.addSuccessRows(batchCount);
                        batchCount = 0;
                    }
                }

                // 提交剩余批次
                if (batchCount > 0 && insertStmt != null) {
                    insertStmt.executeBatch();
                    targetConn.commit();
                    result.successRows += batchCount;
                    chunkTracker.addSuccessRows(batchCount);
                }

                if (insertStmt != null) {
                    insertStmt.close();
                }
            }

        } catch (Exception e) {
            log.error("分片同步失败: chunk={}", chunk, e);
            result.failedRows = result.totalRows - result.successRows;
            chunkTracker.addFailedRows(result.failedRows);
            throw new RuntimeException("分片同步失败", e);
        }

        return result;
    }

    /**
     * 构建 INSERT 语句
     */
    private PreparedStatement buildInsertStatement(Connection conn, String tableName, ResultSet sampleRs) throws SQLException {
        ResultSetMetaData metaData = sampleRs.getMetaData();
        int columnCount = metaData.getColumnCount();

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append("`").append(metaData.getColumnName(i)).append("`");
            placeholders.append("?");
        }

        String sql = String.format("INSERT IGNORE INTO `%s` (%s) VALUES (%s)",
            tableName, columns, placeholders);

        return conn.prepareStatement(sql);
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(DatabaseConnector connector, String identifier) {
        if ("POSTGRESQL".equals(connector.getDatabaseType())) {
            return "\"" + identifier + "\"";
        }
        return "`" + identifier + "`";
    }

    /**
     * 引用列名
     */
    private String quoteColumn(DatabaseConnector connector, String columnName) {
        if ("POSTGRESQL".equals(connector.getDatabaseType())) {
            return "\"" + columnName + "\"";
        }
        return "`" + columnName + "`";
    }

    @Override
    public void stop() {
        stopped.set(true);
        log.info("停止并行同步引擎");
    }

    @Override
    public int getProgress() {
        return totalProgressTracker.getProgress().intValue();
    }

    // ==================== 内部类 ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class KeyRange {
        private Object min;
        private Object max;
    }

    @lombok.Data
    private static class Chunk {
        private String primaryKey;
        private Object min;
        private Object max;
        private ChunkType type;

        public Chunk(String primaryKey, Object min, Object max, ChunkType type) {
            this.primaryKey = primaryKey;
            this.min = min;
            this.max = max;
            this.type = type;
        }
    }

    @lombok.Data
    private static class ChunkResult {
        private long totalRows = 0;
        private long successRows = 0;
        private long failedRows = 0;
    }

    private enum ChunkType {
        RANGE,  // 范围分片（数值主键）
        HASH    // 哈希分片（非数值主键）
    }
}
