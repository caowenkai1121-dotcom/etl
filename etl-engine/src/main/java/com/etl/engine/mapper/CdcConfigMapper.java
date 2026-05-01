package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlCdcConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * CDC配置Mapper
 */
@Mapper
public interface CdcConfigMapper extends BaseMapper<EtlCdcConfig> {
}
