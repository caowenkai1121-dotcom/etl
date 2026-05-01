package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTransformLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转换日志Mapper
 */
@Mapper
public interface TransformLogMapper extends BaseMapper<EtlTransformLog> {
}
