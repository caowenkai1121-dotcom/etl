package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTransformRuleDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转换规则详情Mapper
 */
@Mapper
public interface TransformRuleDetailMapper extends BaseMapper<EtlTransformRuleDetail> {
}
