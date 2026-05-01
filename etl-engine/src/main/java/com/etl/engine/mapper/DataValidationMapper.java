package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlDataValidation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据校验Mapper
 */
@Mapper
public interface DataValidationMapper extends BaseMapper<EtlDataValidation> {
}
