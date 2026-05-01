package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlApiToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * API Token Mapper
 */
@Mapper
public interface ApiTokenMapper extends BaseMapper<EtlApiToken> {
}
