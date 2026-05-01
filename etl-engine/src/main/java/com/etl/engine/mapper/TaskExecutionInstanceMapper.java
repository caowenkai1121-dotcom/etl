package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskExecutionInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskExecutionInstanceMapper extends BaseMapper<EtlTaskExecutionInstance> {

    @Select("SELECT * FROM etl_task_execution_instance WHERE task_id = #{taskId} ORDER BY created_at DESC LIMIT 50")
    List<EtlTaskExecutionInstance> selectByTaskId(@Param("taskId") Long taskId);
}
