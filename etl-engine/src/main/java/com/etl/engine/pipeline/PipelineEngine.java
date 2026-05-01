package com.etl.engine.pipeline;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline 引擎
 * 编排 Reader → Processor[] → Writer 的流水线执行
 */
@Slf4j
public class PipelineEngine {

    private volatile boolean running = false;
    private volatile boolean stopped = false;
    private ShardCoordinator shardCoordinator;
    private PipelineResult finalResult;

    /**
     * 执行流水线（单线程模式）
     */
    public PipelineResult execute(PipelineContext context, StageReader reader,
                                   List<StageProcessor> processors, StageWriter writer) throws Exception {
        running = true;
        stopped = false;
        long startTime = System.currentTimeMillis();

        if (context.getTraceId() == null) {
            context.setTraceId(generateTraceId());
        }

        PipelineResult result = new PipelineResult();
        result.setTraceId(context.getTraceId());
        log.info("Pipeline开始执行: traceId={}", context.getTraceId());

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
                context.recordStageStats("READ", readEnd - batchStart, batch.size(), true);
                result.setTotalRows(result.getTotalRows() + batch.size());

                // PROCESS
                for (StageProcessor processor : processors) {
                    long procStart = System.currentTimeMillis();
                    batch = processor.process(context, batch);
                    context.recordStageStats(processor.getName(), System.currentTimeMillis() - procStart, batch.size(), true);
                }

                // WRITE
                long writeStart = System.currentTimeMillis();
                writer.write(context, batch);
                context.recordStageStats("WRITE", System.currentTimeMillis() - writeStart, batch.size(), true);
                result.setSuccessRows(result.getSuccessRows() + batch.size());
            }

            writer.flush();
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("Pipeline执行异常: traceId={}", context.getTraceId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setFailedRows(result.getTotalRows() - result.getSuccessRows());
        } finally {
            try { reader.close(); } catch (Exception e) { log.warn("关闭Reader异常", e); }
            try { writer.close(); } catch (Exception e) { log.warn("关闭Writer异常", e); }
            for (StageProcessor processor : processors) {
                try { processor.destroy(); } catch (Exception e) { log.warn("销毁Processor异常: {}", processor.getName(), e); }
            }
            running = false;
        }

        result.setTotalElapsedMs(System.currentTimeMillis() - startTime);
        result.getStageStats().putAll(context.getStageStats());
        this.finalResult = result;
        return result;
    }

    /**
     * 执行流水线（并行分片模式）
     */
    public PipelineResult executeParallel(PipelineContext context, StageReader reader,
                                           List<StageProcessor> processors, StageWriter writer,
                                           ShardingStrategy shardingStrategy, int shardCount, int maxConcurrency) throws Exception {
        running = true;
        stopped = false;

        if (context.getTraceId() == null) {
            context.setTraceId(generateTraceId());
        }

        log.info("Pipeline并行执行开始: traceId={}, 分片数={}", context.getTraceId(), shardCount);

        this.shardCoordinator = new ShardCoordinator(shardingStrategy, shardCount, maxConcurrency);
        PipelineResult result = shardCoordinator.executeParallel(context, reader, processors, writer);
        this.finalResult = result;
        running = false;
        return result;
    }

    /**
     * 停止流水线
     */
    public void stop() {
        this.stopped = true;
        if (shardCoordinator != null) {
            shardCoordinator.stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getProgress() {
        if (finalResult == null) return 0;
        return finalResult.getProgress();
    }

    public PipelineResult getResult() {
        return finalResult;
    }

    private String generateTraceId() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
