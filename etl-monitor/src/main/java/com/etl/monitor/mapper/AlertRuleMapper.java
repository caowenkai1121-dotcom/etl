package com.etl.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.monitor.entity.EtlAlertRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则Mapper
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<EtlAlertRule> {
}
