package com.etl.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.engine.entity.EtlTransformLog;
import com.etl.engine.mapper.TransformLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 转换日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformLogService extends ServiceImpl<TransformLogMapper, EtlTransformLog> {

    /**
     * 记录转换日志
     */
    public void record(EtlTransformLog transformLog) {
        try {
            save(transformLog);
        } catch (Exception e) {
            log.warn("保存转换日志失败", e);
        }
    }

    /**
     * 记录转换成功日志
     */
    public void recordSuccess(Long taskId, Long executionId, String traceId,
                               Long stageId, Long ruleId, String ruleName, String ruleType,
                               String tableName, String sourceValue, String targetValue,
                               long elapsedMs, int recordCount) {
        EtlTransformLog logEntry = new EtlTransformLog();
        logEntry.setTaskId(taskId);
        logEntry.setExecutionId(executionId);
        logEntry.setTraceId(traceId);
        logEntry.setStageId(stageId);
        logEntry.setRuleId(ruleId);
        logEntry.setRuleName(ruleName);
        logEntry.setRuleType(ruleType);
        logEntry.setTableName(tableName);
        logEntry.setSourceValue(sourceValue);
        logEntry.setTargetValue(targetValue);
        logEntry.setStatus("SUCCESS");
        logEntry.setElapsedMs(elapsedMs);
        logEntry.setRecordCount(recordCount);
        record(logEntry);
    }

    /**
     * 记录转换失败日志
     */
    public void recordFailure(Long taskId, Long executionId, String traceId,
                               Long stageId, Long ruleId, String ruleName, String ruleType,
                               String tableName, String sourceValue, String errorMessage) {
        EtlTransformLog logEntry = new EtlTransformLog();
        logEntry.setTaskId(taskId);
        logEntry.setExecutionId(executionId);
        logEntry.setTraceId(traceId);
        logEntry.setStageId(stageId);
        logEntry.setRuleId(ruleId);
        logEntry.setRuleName(ruleName);
        logEntry.setRuleType(ruleType);
        logEntry.setTableName(tableName);
        logEntry.setSourceValue(sourceValue);
        logEntry.setStatus("FAILED");
        logEntry.setErrorMessage(errorMessage);
        record(logEntry);
    }

    /**
     * 分页查询转换日志
     */
    public Page<EtlTransformLog> pageList(Integer pageNum, Integer pageSize, Long taskId, String ruleType,
                                           String status, String traceId) {
        Page<EtlTransformLog> page = new Page<>(pageNum, pageSize);
        return lambdaQuery()
            .eq(taskId != null, EtlTransformLog::getTaskId, taskId)
            .eq(ruleType != null && !ruleType.isEmpty(), EtlTransformLog::getRuleType, ruleType)
            .eq(status != null && !status.isEmpty(), EtlTransformLog::getStatus, status)
            .eq(traceId != null && !traceId.isEmpty(), EtlTransformLog::getTraceId, traceId)
            .orderByDesc(EtlTransformLog::getCreatedAt)
            .page(page);
    }
}
