package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<EtlAuditLog> {
}
