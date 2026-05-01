package com.etl.engine.pipeline;

import com.etl.common.context.SyncContext;
import com.etl.common.pipeline.SyncPipeline;
import com.etl.engine.extract.ParallelTableExtractor;
import com.etl.engine.load.SmartBatchLoader;

public class FullSyncPipelineBuilder {
    public static SyncPipeline build(SyncContext context, SmartBatchLoader.LoaderDelegate loaderDelegate,
                                      ParallelTableExtractor.ExtractorDelegate extractorDelegate) {
        SyncPipeline pipeline = new SyncPipeline();
        int parallelism = (int) context.getTaskConfig().getOrDefault("parallelThreads", 1);
        int batchSize = (int) context.getTaskConfig().getOrDefault("batchSize", 1000);
        String targetType = (String) context.getTaskConfig().getOrDefault("targetDsType", "MYSQL");
        pipeline.addExtractor(new ParallelTableExtractor(parallelism, extractorDelegate));
        pipeline.addLoader(new SmartBatchLoader(targetType, batchSize, loaderDelegate));
        return pipeline;
    }
}