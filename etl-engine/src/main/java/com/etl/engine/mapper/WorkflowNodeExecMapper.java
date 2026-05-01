package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlWorkflowNodeExec;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流节点执行记录Mapper
 */
@Mapper
public interface WorkflowNodeExecMapper extends BaseMapper<EtlWorkflowNodeExec> {
}
