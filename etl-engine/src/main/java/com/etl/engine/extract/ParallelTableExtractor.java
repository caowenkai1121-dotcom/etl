package com.etl.engine.extract;

import com.etl.common.context.SyncContext;
import com.etl.common.pipeline.Extractor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class ParallelTableExtractor implements Extractor {
    private final int parallelism;
    private final ExtractorDelegate delegate;

    public ParallelTableExtractor(int parallelism, ExtractorDelegate delegate) {
        this.parallelism = parallelism;
        this.delegate = delegate;
    }

    @Override
    public String getName() { return "ParallelTableExtractor"; }

    @Override
    public void extract(SyncContext context) {
        long totalCount = delegate.estimateTotalCount(context);
        if (totalCount <= 0 || parallelism <= 1) {
            delegate.extractRange(context, null, null);
            return;
        }
        long shardSize = (totalCount + parallelism - 1) / parallelism;
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < parallelism; i++) {
            long start = i * shardSize;
            long end = Math.min(start + shardSize, totalCount);
            futures.add(executor.submit(() -> {
                try {
                    delegate.extractRange(context, start, end);
                } catch (Exception e) {
                    log.error("[ParallelExtract] 分片 {}-{} 抽取失败", start, end, e);
                    context.incrementError();
                }
            }));
        }
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { log.error("[ParallelExtract] 等待分片完成异常", e); }
        }
        executor.shutdown();
    }

    public interface ExtractorDelegate {
        long estimateTotalCount(SyncContext context);
        void extractRange(SyncContext context, Long startId, Long endId);
    }
}