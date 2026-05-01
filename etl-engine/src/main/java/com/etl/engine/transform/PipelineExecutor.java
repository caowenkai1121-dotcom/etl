package com.etl.engine.transform;

import com.etl.common.context.SyncContext;
import com.etl.common.enums.TransformRuleType;
import com.etl.engine.entity.EtlTransformStep;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class PipelineExecutor {
    public void execute(List<EtlTransformStep> steps, SyncContext context) {
        List<Map<String, Object>> data = new ArrayList<>(context.getExtractedData());
        context.setTransformedData(data);
        for (EtlTransformStep step : steps) {
            if (context.isInterrupted()) break;
            TransformRuleType ruleType = TransformRuleType.valueOf(step.getRuleType());
            TransformRuleFactory.TransformRule rule = TransformRuleFactory.getRule(ruleType);
            Map<String, Object> config = parseConfig(step.getRuleConfig());
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> record : data) {
                try {
                    Map<String, Object> transformed = rule.apply(record, config);
                    if (transformed != null) result.add(transformed);
                } catch (Exception e) {
                    log.error("[Transform] 步骤{}转换失败", step.getStepName(), e);
                    context.incrementError();
                    if ("ABORT".equals(step.getErrorStrategy())) {
                        context.setInterrupted(true);
                        break;
                    }
                }
            }
            data = result;
            context.setTransformedData(data);
            context.incrementTransformed(data.size());
            log.info("[Pipeline] 步骤{}完成, 剩余{}条", step.getStepName(), data.size());
        }
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        return com.alibaba.fastjson2.JSON.parseObject(json, Map.class);
    }
}
