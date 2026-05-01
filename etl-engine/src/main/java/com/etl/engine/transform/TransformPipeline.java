package com.etl.engine.transform;

import com.etl.engine.pipeline.PipelineContext;
import com.etl.engine.pipeline.StageProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 转换流水线编排器
 * 管理多阶段转换的执行顺序
 */
@Slf4j
public class TransformPipeline {

    private final List<TransformStage> stages = new ArrayList<>();
    private final String name;
    private volatile boolean stopped = false;

    public TransformPipeline(String name) {
        this.name = name;
    }

    /** 添加转换阶段 */
    public TransformPipeline addStage(TransformStage stage) {
        stages.add(stage);
        return this;
    }

    /** 执行所有转换阶段 */
    public List<Map<String, Object>> execute(PipelineContext context, List<Map<String, Object>> data) {
        List<Map<String, Object>> current = data;
        for (TransformStage stage : stages) {
            if (stopped || current == null || current.isEmpty()) break;
            try {
                long start = System.currentTimeMillis();
                current = stage.process(context, current);
                long elapsed = System.currentTimeMillis() - start;
                log.debug("转换阶段[{}]完成, 耗时={}ms, 记录数={}", stage.getName(), elapsed, current.size());
                context.recordStageStats("TRANSFORM:" + stage.getName(), elapsed, current.size(), true);
            } catch (Exception e) {
                log.error("转换阶段[{}]执行失败", stage.getName(), e);
                context.recordStageStats("TRANSFORM:" + stage.getName(), 0, 0, false);
                if (stage.isStopOnError()) {
                    throw new RuntimeException("转换阶段执行失败: " + stage.getName(), e);
                }
            }
        }
        return current;
    }

    /** 转换为 StageProcessor 列表（适配 Pipeline） */
    public List<StageProcessor> toProcessors() {
        List<StageProcessor> processors = new ArrayList<>();
        for (TransformStage stage : stages) {
            processors.add(new StageProcessor() {
                @Override
                public String getName() { return stage.getName(); }

                @Override
                public List<Map<String, Object>> process(PipelineContext context, List<Map<String, Object>> data) {
                    return stage.process(context, data);
                }
            });
        }
        return processors;
    }

    public void stop() { this.stopped = true; }
    public String getName() { return name; }
    public List<TransformStage> getStages() { return stages; }
}
