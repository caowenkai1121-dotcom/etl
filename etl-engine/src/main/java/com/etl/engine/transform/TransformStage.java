package com.etl.engine.transform;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.etl.engine.pipeline.PipelineContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 转换阶段
 * 包含一组转换规则，按顺序执行
 */
@Slf4j
@Getter
public class TransformStage {

    private final String name;
    private final String stageType; // CLEAN / TRANSFORM / ENRICH / VALIDATE / ROUTE
    private final List<TransformRule> rules = new ArrayList<>();
    private final DataTransformer transformer = new DataTransformer();
    private boolean stopOnError = false;

    public TransformStage(String name, String stageType) {
        this.name = name;
        this.stageType = stageType;
    }

    public TransformStage stopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
        return this;
    }

    public TransformStage addRule(TransformRule rule) {
        this.rules.add(rule);
        return this;
    }

    /** 执行该阶段的所有转换规则 */
    public List<Map<String, Object>> process(PipelineContext context, List<Map<String, Object>> data) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : data) {
            try {
                Map<String, Object> transformed = transformer.transform(row, rules);
                if (!isFiltered(transformed)) {
                    result.add(transformed);
                }
            } catch (Exception e) {
                log.error("转换阶段[{}]行处理失败: {}", name, e.getMessage());
                if (stopOnError) throw e;
            }
        }

        return result;
    }

    private boolean isFiltered(Map<String, Object> data) {
        for (TransformRule rule : rules) {
            if (!rule.isEnabled() || rule.getType() != TransformRule.TransformType.FILTER) continue;
            Object value = data.get(rule.getFieldName());
            Object filterValue = rule.getFilterValue();
            if (rule.getFilterOperator() == TransformRule.FilterOperator.EQUALS && java.util.Objects.equals(value, filterValue)) return true;
            if (rule.getFilterOperator() == TransformRule.FilterOperator.NOT_EQUALS && !java.util.Objects.equals(value, filterValue)) return true;
            if (rule.getFilterOperator() == TransformRule.FilterOperator.IS_NULL && value == null) return true;
            if (rule.getFilterOperator() == TransformRule.FilterOperator.IS_NOT_NULL && value != null) return true;
            if (rule.getFilterOperator() == TransformRule.FilterOperator.REGEX_MATCH && value != null && String.valueOf(value).matches(String.valueOf(filterValue))) return true;
        }
        return false;
    }
}
