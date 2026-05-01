package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlDagConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * DAG配置Mapper
 */
@Mapper
public interface DagConfigMapper extends BaseMapper<EtlDagConfig> {

    /**
     * 获取任务最新版本的DAG配置
     */
    @Select("SELECT * FROM etl_dag_config WHERE task_id = #{taskId} ORDER BY version DESC LIMIT 1")
    EtlDagConfig selectLatestByTaskId(@Param("taskId") Long taskId);

    /**
     * 获取任务指定版本的DAG配置
     */
    @Select("SELECT * FROM etl_dag_config WHERE task_id = #{taskId} AND version = #{version}")
    EtlDagConfig selectByTaskIdAndVersion(@Param("taskId") Long taskId, @Param("version") Integer version);

    @Select("SELECT * FROM etl_dag_config WHERE task_id = #{taskId} ORDER BY version DESC")
    List<EtlDagConfig> selectByTaskId(@Param("taskId") Long taskId);
}