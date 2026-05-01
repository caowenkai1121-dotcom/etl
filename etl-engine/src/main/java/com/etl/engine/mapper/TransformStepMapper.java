package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTransformStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 转换步骤Mapper
 */
@Mapper
public interface TransformStepMapper extends BaseMapper<EtlTransformStep> {

    /**
     * 根据流水线ID查询步骤列表
     */
    @Select("SELECT * FROM etl_transform_step WHERE pipeline_id = #{pipelineId} ORDER BY step_order ASC")
    List<EtlTransformStep> selectByPipelineId(@Param("pipelineId") Long pipelineId);
}
