package com.etl.engine.pipeline;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分片协调器
 * 管理分片的创建、调度和执行
 */
@Slf4j
public class ShardCoordinator {

    private final ShardingStrategy shardingStrategy;
    private final int shardCount;
    private final ExecutorService executorService;
    private final List<Future<PipelineResult>> futures = new CopyOnWriteArrayList<>();
    private final AtomicInteger completedShards = new AtomicInteger(0);
    private volatile boolean stopped = false;

    public ShardCoordinator(ShardingStrategy shardingStrategy, int shardCount, int maxConcurrency) {
        this.shardingStrategy = shardingStrategy;
        this.shardCount = shardCount;
        this.executorService = Executors.newFixedThreadPool(
            Math.min(maxConcurrency, shardCount),
            r -> {
                Thread t = new Thread(r, "shard-worker-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
    }

    /**
     * 执行分片并行同步
     */
    public PipelineResult executeParallel(PipelineContext context, StageReader reader,
                                           List<StageProcessor> processors, StageWriter writer) throws Exception {
        List<Shard> shards = shardingStrategy.computeShards(context, context.getSourceTable(), shardCount);
        log.info("分片执行: 共{}个分片, 最大并发{}", shards.size(), shardCount);

        PipelineResult finalResult = new PipelineResult();
        finalResult.setSuccess(true);
        finalResult.setTraceId(context.getTraceId());

        for (Shard shard : shards) {
            if (stopped) break;
            context.setShard(shard);

            PipelineContext shardContext = new PipelineContext(context.getSyncContext());
            shardContext.setSourceTable(context.getSourceTable());
            shardContext.setTargetTable(context.getTargetTable());
            shardContext.setTraceId(context.getTraceId());
            shardContext.setShard(shard);

            Future<PipelineResult> future = executorService.submit(() -> executeSingleShard(shardContext, reader, processors, writer));
            futures.add(future);
        }

        // 收集结果
        for (Future<PipelineResult> future : futures) {
            try {
                PipelineResult shardResult = future.get();
                finalResult.setTotalRows(finalResult.getTotalRows() + shardResult.getTotalRows());
                finalResult.setSuccessRows(finalResult.getSuccessRows() + shardResult.getSuccessRows());
                finalResult.setFailedRows(finalResult.getFailedRows() + shardResult.getFailedRows());
                if (!shardResult.isSuccess()) {
                    finalResult.setSuccess(false);
                    finalResult.setErrorMessage(shardResult.getErrorMessage());
                }
                completedShards.incrementAndGet();
            } catch (Exception e) {
                log.error("分片执行失败", e);
                finalResult.setSuccess(false);
                finalResult.setErrorMessage(e.getMessage());
            }
        }

        finalResult.setTotalElapsedMs(0); // 实际应从各分片汇总
        return finalResult;
    }

    private PipelineResult executeSingleShard(PipelineContext context, StageReader reader,
                                               List<StageProcessor> processors, StageWriter writer) {
        PipelineResult result = new PipelineResult();
        result.setTraceId(context.getTraceId());

        long startTime = System.currentTimeMillis();
        try {
            reader.open(context);
            writer.open(context);

            for (StageProcessor processor : processors) {
                processor.init(context);
            }

            while (reader.hasNext(context) && !stopped) {
                long batchStart = System.currentTimeMillis();

                // READ
                List<Map<String, Object>> batch = reader.read(context);
                long readEnd = System.currentTimeMillis();
                context.recordStageStats("READ[" + context.getShard().getShardId() + "]", readEnd - batchStart, batch.size(), true);

                // PROCESS (链式处理)
                for (StageProcessor processor : processors) {
                    long procStart = System.currentTimeMillis();
                    batch = processor.process(context, batch);
                    context.recordStageStats(processor.getName(), System.currentTimeMillis() - procStart, batch.size(), true);
                }

                // WRITE
                long writeStart = System.currentTimeMillis();
                writer.write(context, batch);
                context.recordStageStats("WRITE[" + context.getShard().getShardId() + "]", System.currentTimeMillis() - writeStart, batch.size(), true);

                result.setTotalRows(result.getTotalRows() + batch.size());
                result.setSuccessRows(result.getSuccessRows() + batch.size());
            }

            writer.flush();
            reader.close();
            writer.close();
            for (StageProcessor processor : processors) {
                processor.destroy();
            }

            result.setSuccess(true);
        } catch (Exception e) {
            log.error("分片{}执行异常", context.getShard().getShardId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setFailedRows(result.getTotalRows() - result.getSuccessRows());
        }

        result.setTotalElapsedMs(System.currentTimeMillis() - startTime);
        return result;
    }

    public void stop() {
        this.stopped = true;
        for (Future<PipelineResult> future : futures) {
            future.cancel(true);
        }
        executorService.shutdownNow();
    }

    public int getCompletedShards() {
        return completedShards.get();
    }

    public int getProgress() {
        return futures.isEmpty() ? 0 : (completedShards.get() * 100 / futures.size());
    }
}
