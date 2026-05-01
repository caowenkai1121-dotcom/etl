package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskPublish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务发布Mapper
 */
@Mapper
public interface TaskPublishMapper extends BaseMapper<EtlTaskPublish> {
}
