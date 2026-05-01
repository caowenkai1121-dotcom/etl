package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlDictMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典映射Mapper
 */
@Mapper
public interface DictMappingMapper extends BaseMapper<EtlDictMapping> {
}
