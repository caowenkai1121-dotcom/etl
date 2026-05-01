package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskWorkflow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流Mapper
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<EtlTaskWorkflow> {
}
