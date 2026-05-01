package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTransformPipeline;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转换流水线Mapper
 */
@Mapper
public interface TransformPipelineMapper extends BaseMapper<EtlTransformPipeline> {
}
