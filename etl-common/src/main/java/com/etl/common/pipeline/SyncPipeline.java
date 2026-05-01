package com.etl.common.pipeline;

import com.etl.common.context.SyncContext;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class SyncPipeline {
    private final List<Extractor> extractors = new ArrayList<>();
    private final List<Transformer> transformers = new ArrayList<>();
    private final List<Loader> loaders = new ArrayList<>();

    public SyncPipeline addExtractor(Extractor extractor) {
        this.extractors.add(extractor);
        return this;
    }

    public SyncPipeline addTransformer(Transformer transformer) {
        this.transformers.add(transformer);
        return this;
    }

    public SyncPipeline addLoader(Loader loader) {
        this.loaders.add(loader);
        return this;
    }

    public void execute(SyncContext context) {
        context.setStartTime(System.currentTimeMillis());
        for (Extractor extractor : extractors) {
            if (context.isInterrupted()) break;
            log.info("[Pipeline] 抽取阶段: {}", extractor.getName());
            extractor.extract(context);
        }
        for (Transformer transformer : transformers) {
            if (context.isInterrupted()) break;
            log.info("[Pipeline] 转换阶段: {}", transformer.getName());
            transformer.transform(context);
        }
        for (Loader loader : loaders) {
            if (context.isInterrupted()) break;
            log.info("[Pipeline] 加载阶段: {}", loader.getName());
            loader.load(context);
        }
        log.info("[Pipeline] 完成 - 抽取:{} 转换:{} 加载:{} 错误:{}",
            context.getExtractedCount(), context.getTransformedCount(),
            context.getLoadedCount(), context.getErrorCount());
    }
}
