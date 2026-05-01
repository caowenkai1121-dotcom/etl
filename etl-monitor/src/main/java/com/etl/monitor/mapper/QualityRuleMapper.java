package com.etl.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.monitor.entity.EtlQualityRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据质量规则Mapper
 */
@Mapper
public interface QualityRuleMapper extends BaseMapper<EtlQualityRule> {
}
