package com.etl.engine.transform;

import com.etl.engine.pipeline.PipelineContext;
import com.etl.engine.pipeline.StageReader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Pipeline 适配器：将 TransformPipeline 包装为 StageReader 的后置处理
 * 用于在 Pipeline 中嵌入转换阶段
 */
@Slf4j
public class TransformStageProcessor {

    private final TransformPipeline pipeline;

    public TransformStageProcessor(TransformPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 创建包裹了转换逻辑的 Reader
     */
    public StageReader wrapReader(StageReader originalReader) {
        return new StageReader() {
            @Override
            public String getName() { return originalReader.getName() + "+Transform"; }

            @Override
            public void open(PipelineContext context) throws Exception { originalReader.open(context); }

            @Override
            public List<Map<String, Object>> read(PipelineContext context) throws Exception {
                List<Map<String, Object>> data = originalReader.read(context);
                return pipeline.execute(context, data);
            }

            @Override
            public boolean hasNext(PipelineContext context) throws Exception { return originalReader.hasNext(context); }

            @Override
            public void close() throws Exception { originalReader.close(); }

            @Override
            public long getReadRows() { return originalReader.getReadRows(); }
        };
    }

    public TransformPipeline getPipeline() { return pipeline; }
}
