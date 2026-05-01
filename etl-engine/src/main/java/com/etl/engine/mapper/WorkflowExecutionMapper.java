package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlWorkflowExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流执行记录Mapper
 */
@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<EtlWorkflowExecution> {
}
