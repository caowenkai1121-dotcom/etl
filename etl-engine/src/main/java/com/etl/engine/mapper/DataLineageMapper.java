package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlDataLineage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据血缘关系Mapper
 */
public interface DataLineageMapper extends BaseMapper<EtlDataLineage> {

    @Select("SELECT * FROM etl_data_lineage WHERE task_id = #{taskId}")
    List<EtlDataLineage> selectByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM etl_data_lineage WHERE source_datasource_id = #{datasourceId} OR target_datasource_id = #{datasourceId}")
    List<EtlDataLineage> selectByDatasourceId(@Param("datasourceId") Long datasourceId);

    @Select("SELECT * FROM etl_data_lineage WHERE source_table = #{tableName} OR target_table = #{tableName}")
    List<EtlDataLineage> selectByTableName(@Param("tableName") String tableName);
}
