package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTransformStage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转换阶段Mapper
 */
@Mapper
public interface TransformStageMapper extends BaseMapper<EtlTransformStage> {
}
