package com.etl.monitor.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.enums.LogLevel;
import com.etl.engine.entity.EtlSyncLog;
import com.etl.engine.mapper.SyncLogMapper;
import com.etl.monitor.entity.EtlSyncLogDetail;
import com.etl.monitor.log.AsyncLogWriter;
import com.etl.monitor.mapper.SyncLogDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final SyncLogMapper syncLogMapper;
    private final AsyncLogWriter asyncLogWriter;
    private final SyncLogDetailMapper syncLogDetailMapper;

    /**
     * 记录日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void log(Long taskId, Long executionId, String traceId, String level, String logType,
                    String tableName, String stageName, String message, Integer recordCount,
                    Long elapsedMs, String stackTrace) {
        EtlSyncLog syncLog = new EtlSyncLog();
        syncLog.setTaskId(taskId);
        syncLog.setExecutionId(executionId);
        syncLog.setTraceId(traceId);
        syncLog.setLogLevel(level);
        syncLog.setLogType(logType);
        syncLog.setTableName(tableName);
        syncLog.setStageName(stageName);
        syncLog.setMessage(message);
        syncLog.setRecordCount(recordCount);
        syncLog.setElapsedMs(elapsedMs);
        syncLog.setStackTrace(stackTrace);
        syncLog.setCreatedAt(LocalDateTime.now());

        // 优先使用异步写入
        try {
            asyncLogWriter.asyncWrite(syncLog);
        } catch (Exception e) {
            log.error("异步写入日志失败，降级为同步写入", e);
            // 异步写入失败时，降级为同步写入
            syncLogMapper.insert(syncLog);
        }
    }

    /**
     * 记录日志（向后兼容，无traceId等扩展字段）
     */
    @Transactional(rollbackFor = Exception.class)
    public void log(Long taskId, Long executionId, String level, String logType,
                    String tableName, String message, String stackTrace) {
        log(taskId, executionId, null, level, logType, tableName, null, message, null, null, stackTrace);
    }

    /**
     * 记录包含跟踪ID的日志
     */
    public void logTrace(Long taskId, Long executionId, String traceId, String logType,
                         String tableName, String message, String stackTrace) {
        log(taskId, executionId, traceId, LogLevel.INFO.getCode(), logType, tableName,
            null, message, null, null, stackTrace);
    }

    /**
     * 记录INFO日志
     */
    public void info(Long taskId, Long executionId, String traceId, String logType, String tableName, String message) {
        log(taskId, executionId, traceId, LogLevel.INFO.getCode(), logType, tableName, null, message, null, null, null);
    }

    /**
     * 记录WARN日志
     */
    public void warn(Long taskId, Long executionId, String traceId, String logType, String tableName, String message) {
        log(taskId, executionId, traceId, LogLevel.WARN.getCode(), logType, tableName, null, message, null, null, null);
    }

    /**
     * 记录ERROR日志
     */
    public void error(Long taskId, Long executionId, String traceId, String logType, String tableName,
                      String message, Throwable throwable) {
        String stackTrace = throwable != null ? getStackTrace(throwable) : null;
        log(taskId, executionId, traceId, LogLevel.ERROR.getCode(), logType, tableName, null, message, null, null, stackTrace);
    }

    /**
     * 分页查询日志
     */
    public Page<EtlSyncLog> pageList(Integer pageNum, Integer pageSize, Long taskId,
                                      Long executionId, String level, String logType,
                                      String tableName, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<EtlSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, EtlSyncLog::getTaskId, taskId)
               .eq(executionId != null, EtlSyncLog::getExecutionId, executionId)
               .eq(StrUtil.isNotBlank(level), EtlSyncLog::getLogLevel, level)
               .eq(StrUtil.isNotBlank(logType), EtlSyncLog::getLogType, logType)
               .like(StrUtil.isNotBlank(tableName), EtlSyncLog::getTableName, tableName)
               .ge(startTime != null, EtlSyncLog::getCreatedAt, startTime)
               .le(endTime != null, EtlSyncLog::getCreatedAt, endTime)
               .orderByDesc(EtlSyncLog::getCreatedAt);

        return syncLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 分页查询日志（v3.0 增强版，支持 traceId 和 stageName）
     */
    public Page<EtlSyncLog> pageList(Integer pageNum, Integer pageSize, Long taskId,
                                      Long executionId, String level, String logType,
                                      String tableName, String traceId, String stageName,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<EtlSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, EtlSyncLog::getTaskId, taskId)
               .eq(executionId != null, EtlSyncLog::getExecutionId, executionId)
               .eq(StrUtil.isNotBlank(level), EtlSyncLog::getLogLevel, level)
               .eq(StrUtil.isNotBlank(logType), EtlSyncLog::getLogType, logType)
               .eq(StrUtil.isNotBlank(traceId), EtlSyncLog::getTraceId, traceId)
               .eq(StrUtil.isNotBlank(stageName), EtlSyncLog::getStageName, stageName)
               .like(StrUtil.isNotBlank(tableName), EtlSyncLog::getTableName, tableName)
               .ge(startTime != null, EtlSyncLog::getCreatedAt, startTime)
               .le(endTime != null, EtlSyncLog::getCreatedAt, endTime)
               .orderByDesc(EtlSyncLog::getCreatedAt);
        return syncLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 按 TraceID 查询日志
     */
    public List<EtlSyncLog> getByTraceId(String traceId) {
        LambdaQueryWrapper<EtlSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlSyncLog::getTraceId, traceId)
               .orderByAsc(EtlSyncLog::getCreatedAt);
        return syncLogMapper.selectList(wrapper);
    }

    /**
     * 日志概览统计
     */
    public Map<String, Object> getStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (startTime == null) startTime = LocalDateTime.now().minusDays(7);
        if (endTime == null) endTime = LocalDateTime.now();

        stats.put("startTime", startTime);
        stats.put("endTime", endTime);
        stats.put("totalLogs", syncLogMapper.selectCount(null));
        stats.put("errorLogs", syncLogMapper.selectCount(
            new LambdaQueryWrapper<EtlSyncLog>().eq(EtlSyncLog::getLogLevel, "ERROR")));
        stats.put("warnLogs", syncLogMapper.selectCount(
            new LambdaQueryWrapper<EtlSyncLog>().eq(EtlSyncLog::getLogLevel, "WARN")));
        stats.put("transformLogs", syncLogMapper.selectCount(
            new LambdaQueryWrapper<EtlSyncLog>().isNotNull(EtlSyncLog::getStageName)));
        return stats;
    }

    /**
     * 按阶段统计日志
     */
    public List<Map<String, Object>> getStatsByStage(Long taskId) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 简化实现：返回空列表，实际可用 SQL GROUP BY
        return result;
    }

    /**
     * 按规则统计日志
     */
    public List<Map<String, Object>> getStatsByRule(Long taskId) {
        return new ArrayList<>();
    }

    /**
     * 错误趋势
     */
    public List<Map<String, Object>> getErrorTrend(Integer days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        // 简化实现：返回空列表，实际应按日期分组统计
        return trend;
    }

    /**
     * 导出日志
     */
    public void exportLog(OutputStream outputStream, Long taskId, String level,
                          LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        // 简化实现：写入 CSV 格式
        List<EtlSyncLog> logs = syncLogMapper.selectList(null);
        StringBuilder csv = new StringBuilder("ID,TaskID,Level,Type,Table,Stage,Message,TraceID,Time\n");
        for (EtlSyncLog log : logs) {
            csv.append(log.getId()).append(",")
               .append(log.getTaskId()).append(",")
               .append(log.getLogLevel()).append(",")
               .append(log.getLogType()).append(",")
               .append(log.getTableName()).append(",")
               .append(log.getStageName()).append(",")
               .append(log.getMessage()).append(",")
               .append(log.getTraceId()).append(",")
               .append(log.getCreatedAt()).append("\n");
        }
        outputStream.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\t at ").append(element.toString()).append("\n");
        }
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(throwable.getCause()));
        }
        return sb.toString();
    }

    /**
     * 按 TraceID 查询链路日志
     */
    public List<EtlSyncLog> queryByTraceId(String traceId) {
        LambdaQueryWrapper<EtlSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlSyncLog::getTraceId, traceId)
               .orderByAsc(EtlSyncLog::getCreatedAt);
        return syncLogMapper.selectList(wrapper);
    }

    /**
     * 查询转换详情
     */
    public List<EtlSyncLogDetail> queryDetail(Long logId, String stepCode) {
        LambdaQueryWrapper<EtlSyncLogDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(logId != null, EtlSyncLogDetail::getLogId, logId)
               .eq(StrUtil.isNotBlank(stepCode), EtlSyncLogDetail::getStepCode, stepCode)
               .orderByAsc(EtlSyncLogDetail::getRowIndex);
        return syncLogDetailMapper.selectList(wrapper);
    }

    /**
     * 归档指定天数前的日志
     */
    @Transactional(rollbackFor = Exception.class)
    public int archive(int beforeDays) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(beforeDays);
        LambdaQueryWrapper<EtlSyncLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.lt(EtlSyncLog::getCreatedAt, beforeDate);
        int count = syncLogMapper.delete(logWrapper);

        LambdaQueryWrapper<EtlSyncLogDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.lt(EtlSyncLogDetail::getCreateTime, beforeDate);
        syncLogDetailMapper.delete(detailWrapper);

        return count;
    }

    /**
     * 多维组合查询日志
     */
    public Page<EtlSyncLog> multiConditionQuery(Long taskId, LocalDateTime startTime, LocalDateTime endTime,
                                                 String level, String module, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<EtlSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, EtlSyncLog::getTaskId, taskId)
               .ge(startTime != null, EtlSyncLog::getCreatedAt, startTime)
               .le(endTime != null, EtlSyncLog::getCreatedAt, endTime)
               .eq(StrUtil.isNotBlank(level), EtlSyncLog::getLogLevel, level)
               .like(StrUtil.isNotBlank(module), EtlSyncLog::getStageName, module)
               .and(StrUtil.isNotBlank(keyword), w -> w.like(EtlSyncLog::getMessage, keyword)
                       .or().like(EtlSyncLog::getTableName, keyword))
               .orderByDesc(EtlSyncLog::getCreatedAt);
        return syncLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
