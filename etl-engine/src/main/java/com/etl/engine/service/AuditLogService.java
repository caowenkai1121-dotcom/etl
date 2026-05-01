package com.etl.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.engine.entity.EtlAuditLog;
import com.etl.engine.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService extends ServiceImpl<AuditLogMapper, EtlAuditLog> {

    /**
     * 记录操作审计日志
     */
    public void record(String operator, String operationType, String targetType,
                       Long targetId, String targetName, String detail,
                       String status, String errorMessage) {
        try {
            EtlAuditLog auditLog = new EtlAuditLog();
            auditLog.setOperator(operator);
            auditLog.setOperationType(operationType);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setTargetName(targetName);
            auditLog.setDetail(detail);
            auditLog.setStatus(status);
            auditLog.setErrorMessage(errorMessage);
            save(auditLog);
            log.debug("审计日志已记录: operator={}, type={}, target={}", operator, operationType, targetName);
        } catch (Exception e) {
            log.warn("记录审计日志失败", e);
        }
    }

    /**
     * 记录成功操作
     */
    public void recordSuccess(String operator, String operationType,
                               String targetType, Long targetId, String targetName, String detail) {
        record(operator, operationType, targetType, targetId, targetName, detail, "SUCCESS", null);
    }

    /**
     * 记录失败操作
     */
    public void recordFailure(String operator, String operationType,
                               String targetType, Long targetId, String targetName, String errorMessage) {
        record(operator, operationType, targetType, targetId, targetName, null, "FAILED", errorMessage);
    }
}
