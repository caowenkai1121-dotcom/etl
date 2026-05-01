package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlApiCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * API调用日志Mapper
 */
@Mapper
public interface ApiCallLogMapper extends BaseMapper<EtlApiCallLog> {
}
