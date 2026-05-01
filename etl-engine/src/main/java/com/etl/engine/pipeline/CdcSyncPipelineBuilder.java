package com.etl.engine.pipeline;

import com.etl.common.context.SyncContext;
import com.etl.common.pipeline.Extractor;
import com.etl.common.pipeline.SyncPipeline;
import com.etl.engine.load.SmartBatchLoader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CdcSyncPipelineBuilder {

    public interface CdcExtractorDelegate {
        void startConsuming(SyncContext context);
        void stopConsuming();
    }

    public static SyncPipeline build(SyncContext context, SmartBatchLoader.LoaderDelegate loaderDelegate,
                                      CdcExtractorDelegate extractorDelegate) {
        SyncPipeline pipeline = new SyncPipeline();
        int batchSize = (int) context.getTaskConfig().getOrDefault("batchSize", 1000);
        String targetType = (String) context.getTaskConfig().getOrDefault("targetDsType", "MYSQL");

        // CDC同步使用Kafka消费者作为抽取器
        pipeline.addExtractor(new Extractor() {
            @Override
            public String getName() { return "CdcKafkaExtractor"; }

            @Override
            public void extract(SyncContext ctx) {
                extractorDelegate.startConsuming(ctx);
            }
        });

        pipeline.addLoader(new SmartBatchLoader(targetType, batchSize, loaderDelegate));
        return pipeline;
    }
}