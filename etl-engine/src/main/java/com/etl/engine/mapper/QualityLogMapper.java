package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlQualityLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质量日志Mapper
 */
@Mapper
public interface QualityLogMapper extends BaseMapper<EtlQualityLog> {
}
