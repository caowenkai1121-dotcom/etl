package com.etl.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.monitor.entity.EtlOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper 接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<EtlOperationLog> {
}
