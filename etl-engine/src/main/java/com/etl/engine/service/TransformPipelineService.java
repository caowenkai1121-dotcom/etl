package com.etl.engine.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.context.SyncContext;
import com.etl.common.enums.TransformRuleType;
import com.etl.engine.entity.EtlTransformPipeline;
import com.etl.engine.entity.EtlTransformStep;
import com.etl.engine.mapper.TransformPipelineMapper;
import com.etl.engine.mapper.TransformStepMapper;
import com.etl.engine.transform.PipelineExecutor;
import com.etl.engine.transform.TransformRuleFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 转换流水线服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformPipelineService {

    private final TransformPipelineMapper pipelineMapper;
    private final TransformStepMapper stepMapper;

    // ============ Pipeline CRUD ============

    public Page<EtlTransformPipeline> getPipelinePage(int pageNum, int pageSize, String name) {
        Page<EtlTransformPipeline> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EtlTransformPipeline> qw = new LambdaQueryWrapper<EtlTransformPipeline>()
                .orderByDesc(EtlTransformPipeline::getCreateTime);
        if (name != null && !name.isEmpty()) {
            qw.like(EtlTransformPipeline::getName, name);
        }
        return pipelineMapper.selectPage(page, qw);
    }

    public EtlTransformPipeline getPipelineById(Long id) {
        return pipelineMapper.selectById(id);
    }

    @Transactional
    public Long createPipeline(EtlTransformPipeline pipeline) {
        pipeline.setCreateTime(LocalDateTime.now());
        pipeline.setUpdateTime(LocalDateTime.now());
        pipeline.setStatus(1);
        pipelineMapper.insert(pipeline);
        return pipeline.getId();
    }

    @Transactional
    public void updatePipeline(Long id, EtlTransformPipeline pipeline) {
        pipeline.setId(id);
        pipeline.setUpdateTime(LocalDateTime.now());
        pipelineMapper.updateById(pipeline);
    }

    @Transactional
    public void deletePipeline(Long id) {
        pipelineMapper.deleteById(id);
        stepMapper.delete(new LambdaQueryWrapper<EtlTransformStep>()
                .eq(EtlTransformStep::getPipelineId, id));
    }

    // ============ Step CRUD ============

    public List<EtlTransformStep> getStepsByPipelineId(Long pipelineId) {
        return stepMapper.selectByPipelineId(pipelineId);
    }

    @Transactional
    public Long addStep(EtlTransformStep step) {
        step.setCreateTime(LocalDateTime.now());
        step.setUpdateTime(LocalDateTime.now());
        if (step.getErrorStrategy() == null) {
            step.setErrorStrategy("IGNORE");
        }
        stepMapper.insert(step);
        return step.getId();
    }

    @Transactional
    public List<Long> addSteps(List<EtlTransformStep> steps) {
        List<Long> stepIds = new ArrayList<>();
        for (EtlTransformStep step : steps) {
            step.setCreateTime(LocalDateTime.now());
            step.setUpdateTime(LocalDateTime.now());
            if (step.getErrorStrategy() == null) {
                step.setErrorStrategy("IGNORE");
            }
            stepMapper.insert(step);
            stepIds.add(step.getId());
        }
        return stepIds;
    }

    @Transactional
    public void updateStep(Long id, EtlTransformStep step) {
        step.setId(id);
        step.setUpdateTime(LocalDateTime.now());
        stepMapper.updateById(step);
    }

    @Transactional
    public void deleteStep(Long id) {
        stepMapper.deleteById(id);
    }

    @Transactional
    public void reorderSteps(Long pipelineId, List<Long> stepIds) {
        List<EtlTransformStep> steps = stepMapper.selectByPipelineId(pipelineId);
        Map<Long, EtlTransformStep> stepMap = steps.stream()
                .collect(Collectors.toMap(EtlTransformStep::getId, s -> s));

        for (int i = 0; i < stepIds.size(); i++) {
            Long stepId = stepIds.get(i);
            EtlTransformStep step = stepMap.get(stepId);
            if (step != null) {
                step.setStepOrder(i);
                step.setUpdateTime(LocalDateTime.now());
                stepMapper.updateById(step);
            }
        }
    }

    // ============ Preview ============

    public Map<String, Object> previewExecution(Long pipelineId, List<Map<String, Object>> sampleData) {
        List<EtlTransformStep> steps = stepMapper.selectByPipelineId(pipelineId);
        Map<String, Object> result = new HashMap<>();
        result.put("steps", steps.stream().map(s -> {
            Map<String, Object> stepInfo = new HashMap<>();
            stepInfo.put("stepCode", s.getStepCode());
            stepInfo.put("stepName", s.getStepName());
            stepInfo.put("ruleType", s.getRuleType());
            return stepInfo;
        }).collect(Collectors.toList()));

        List<Map<String, Object>> stepResults = new ArrayList<>();
        List<Map<String, Object>> currentData = new ArrayList<>(sampleData);
        stepResults.add(Map.of("step", "原始数据", "data", new ArrayList<>(currentData)));

        for (EtlTransformStep step : steps) {
            try {
                TransformRuleType ruleType = TransformRuleType.valueOf(step.getRuleType());
                TransformRuleFactory.TransformRule rule = TransformRuleFactory.getRule(ruleType);
                Map<String, Object> config = JSON.parseObject(step.getRuleConfig(), Map.class);

                List<Map<String, Object>> newData = new ArrayList<>();
                for (Map<String, Object> record : currentData) {
                    try {
                        Map<String, Object> transformed = rule.apply(new HashMap<>(record), config);
                        newData.add(transformed);
                    } catch (Exception e) {
                        newData.add(record);
                    }
                }

                stepResults.add(Map.of("step", step.getStepName(), "data", new ArrayList<>(newData)));
                currentData = newData;
            } catch (Exception e) {
                stepResults.add(Map.of("step", step.getStepName(), "data", currentData, "error", e.getMessage()));
            }
        }

        result.put("stepResults", stepResults);
        return result;
    }

    // ============ Supported Rules ============

    public List<Map<String, Object>> getSupportedRules() {
        return TransformRuleFactory.supportedTypes().stream()
                .map(type -> {
                    Map<String, Object> ruleInfo = new HashMap<>();
                    ruleInfo.put("type", type.name());
                    ruleInfo.put("description", type.getDescription());
                    return ruleInfo;
                })
                .collect(Collectors.toList());
    }
}
