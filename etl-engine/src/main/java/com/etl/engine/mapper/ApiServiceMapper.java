package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlApiService;
import org.apache.ibatis.annotations.Mapper;

/**
 * API服务Mapper
 */
@Mapper
public interface ApiServiceMapper extends BaseMapper<EtlApiService> {
}
