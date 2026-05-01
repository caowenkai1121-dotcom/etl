package com.etl.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.monitor.entity.EtlOperationLog;
import com.etl.monitor.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void log(String userName, String operation, String module, Long targetId, String targetName, String detail, String ip) {
        EtlOperationLog log = new EtlOperationLog();
        log.setUserName(userName);
        log.setOperation(operation);
        log.setModule(module);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        log.setIp(ip);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    /**
     * 分页查询操作日志
     */
    public Page<EtlOperationLog> page(String module, String operation, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer size) {
        LambdaQueryWrapper<EtlOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(module != null, EtlOperationLog::getModule, module)
               .eq(operation != null, EtlOperationLog::getOperation, operation)
               .ge(startTime != null, EtlOperationLog::getCreateTime, startTime)
               .le(endTime != null, EtlOperationLog::getCreateTime, endTime)
               .orderByDesc(EtlOperationLog::getCreateTime);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 按模块查询操作日志
     */
    public List<EtlOperationLog> getByModule(String module) {
        LambdaQueryWrapper<EtlOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlOperationLog::getModule, module)
               .orderByDesc(EtlOperationLog::getCreateTime);
        return operationLogMapper.selectList(wrapper);
    }
}
