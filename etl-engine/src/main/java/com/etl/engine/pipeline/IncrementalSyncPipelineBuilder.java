package com.etl.engine.pipeline;

import com.etl.common.context.SyncContext;
import com.etl.common.pipeline.Extractor;
import com.etl.common.pipeline.SyncPipeline;
import com.etl.engine.load.SmartBatchLoader;

import java.util.Map;

public class IncrementalSyncPipelineBuilder {

    public interface IncrementalExtractorDelegate {
        void extractIncrementalData(SyncContext context);
    }

    public static SyncPipeline build(SyncContext context, SmartBatchLoader.LoaderDelegate loaderDelegate,
                                      IncrementalExtractorDelegate extractorDelegate) {
        SyncPipeline pipeline = new SyncPipeline();
        int batchSize = (int) context.getTaskConfig().getOrDefault("batchSize", 1000);
        String targetType = (String) context.getTaskConfig().getOrDefault("targetDsType", "MYSQL");

        // 增量同步使用单线程抽取器（不需要并行分片）
        pipeline.addExtractor(new Extractor() {
            @Override
            public String getName() { return "IncrementalExtractor"; }

            @Override
            public void extract(SyncContext ctx) {
                extractorDelegate.extractIncrementalData(ctx);
            }
        });

        pipeline.addLoader(new SmartBatchLoader(targetType, batchSize, loaderDelegate));
        return pipeline;
    }
}