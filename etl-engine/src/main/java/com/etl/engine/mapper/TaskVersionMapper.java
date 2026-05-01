package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 任务版本Mapper
 */
@Mapper
public interface TaskVersionMapper extends BaseMapper<EtlTaskVersion> {

    /**
     * 获取任务最新版本号
     */
    @Select("SELECT COALESCE(MAX(version), 0) FROM etl_task_version WHERE task_id = #{taskId}")
    int selectMaxVersionByTaskId(@Param("taskId") Long taskId);

    /**
     * 获取任务最新版本
     */
    @Select("SELECT * FROM etl_task_version WHERE task_id = #{taskId} ORDER BY version DESC LIMIT 1")
    EtlTaskVersion selectLatestByTaskId(@Param("taskId") Long taskId);

    /**
     * 获取任务指定版本
     */
    @Select("SELECT * FROM etl_task_version WHERE task_id = #{taskId} AND version = #{version}")
    EtlTaskVersion selectByTaskIdAndVersion(@Param("taskId") Long taskId, @Param("version") Integer version);
}