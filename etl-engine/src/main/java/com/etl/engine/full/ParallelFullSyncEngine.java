package com.etl.engine.full;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.callback.SyncLogCallback;
import com.etl.engine.pipeline.*;
import com.etl.engine.checkpoint.RetryMechanism;
import com.etl.engine.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 并行全量同步引擎
 * 基于 Pipeline 架构 + 分片并行 + 限流 + 重试
 */
@Slf4j
public class ParallelFullSyncEngine {

    private PipelineEngine pipelineEngine;
    private volatile boolean running = false;

    /**
     * 执行并行全量同步
     */
    public PipelineResult sync(SyncPipelineContext context) throws Exception {
        running = true;
        String traceId = "T" + System.currentTimeMillis() + String.valueOf(context.getTaskId());

        if (context.getLogCallback() != null) {
            context.getLogCallback().info(context.getTaskId(), context.getExecutionId(),
                traceId, context.getSourceTable(), "开始并行全量同步");
        }

        // 创建 Pipeline 上下文
        PipelineContext pipelineContext = new PipelineContext(context);
        pipelineContext.setSourceTable(context.getSourceTable());
        pipelineContext.setTargetTable(context.getTargetTable());
        pipelineContext.setTraceId(traceId);

        // 创建读取器
        StageReader reader = new StageReader() {
            private long readRows;
            private RateLimiter rateLimiter;

            @Override
            public String getName() { return "FullTableReader"; }

            @Override
            public void open(PipelineContext ctx) throws Exception {
                if (context.getMaxReadRowsPerSecond() > 0) {
                    rateLimiter = new RateLimiter(1000, context.getMaxReadRowsPerSecond());
                }
                log.info("打开全量读取器: table={}", ctx.getSourceTable());
            }

            @Override
            public List<Map<String, Object>> read(PipelineContext ctx) throws Exception {
                if (rateLimiter != null) rateLimiter.acquire(context.getBatchSize());
                // 实际实现会从数据源读取数据
                // 此处简化：返回空列表表示没有更多数据
                return new ArrayList<>();
            }

            @Override
            public boolean hasNext(PipelineContext ctx) throws Exception {
                return false; // 简化实现
            }

            @Override
            public void close() throws Exception {
                log.info("关闭全量读取器: totalRows={}", readRows);
            }

            @Override
            public long getReadRows() { return readRows; }
        };

        // 创建写入器
        StageWriter writer = new StageWriter() {
            private long writtenRows;
            private RateLimiter rateLimiter;

            @Override
            public String getName() { return "FullTableWriter"; }

            @Override
            public void open(PipelineContext ctx) throws Exception {
                if (context.getMaxWriteRowsPerSecond() > 0) {
                    rateLimiter = new RateLimiter(1000, context.getMaxWriteRowsPerSecond());
                }
                log.info("打开全量写入器: table={}", ctx.getTargetTable());
            }

            @Override
            public void write(PipelineContext ctx, List<Map<String, Object>> data) throws Exception {
                if (rateLimiter != null) rateLimiter.acquire(data.size());
                writtenRows += data.size();
            }

            @Override
            public void flush() throws Exception {}

            @Override
            public void close() throws Exception {
                log.info("关闭全量写入器: totalRows={}", writtenRows);
            }

            @Override
            public long getWrittenRows() { return writtenRows; }
        };

        // 创建 Pipeline 引擎
        pipelineEngine = new PipelineEngine();
        List<StageProcessor> processors = new ArrayList<>();

        int shardCount = context.getShardTotal();
        PipelineResult result;

        if (shardCount > 1) {
            // 并行分片模式
            ShardingStrategy shardingStrategy = new RangeShardingStrategy();
            int maxConcurrency = Math.min(shardCount, Runtime.getRuntime().availableProcessors() * 2);
            result = pipelineEngine.executeParallel(pipelineContext, reader, processors, writer,
                shardingStrategy, shardCount, maxConcurrency);
        } else {
            // 单线程模式（默认）
            result = pipelineEngine.execute(pipelineContext, reader, processors, writer);
        }

        if (context.getLogCallback() != null) {
            context.getLogCallback().info(context.getTaskId(), context.getExecutionId(),
                traceId, context.getSourceTable(),
                "并行全量同步完成: total=" + result.getTotalRows() +
                ", success=" + result.getSuccessRows() +
                ", failed=" + result.getFailedRows() +
                ", elapsed=" + result.getTotalElapsedMs() + "ms");
        }

        running = false;
        return result;
    }

    public void stop() {
        running = false;
        if (pipelineEngine != null) {
            pipelineEngine.stop();
        }
    }

    public boolean isRunning() { return running; }
}
