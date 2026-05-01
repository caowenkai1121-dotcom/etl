package com.etl.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.datasource.entity.EtlDatasource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源Mapper
 */
@Mapper
public interface DatasourceMapper extends BaseMapper<EtlDatasource> {
}
