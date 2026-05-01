package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlNodeExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NodeExecutionLogMapper extends BaseMapper<EtlNodeExecutionLog> {

    @Select("SELECT * FROM etl_node_execution_log WHERE execution_id = #{executionId} ORDER BY created_at ASC")
    List<EtlNodeExecutionLog> selectByExecutionId(@Param("executionId") Long executionId);
}
