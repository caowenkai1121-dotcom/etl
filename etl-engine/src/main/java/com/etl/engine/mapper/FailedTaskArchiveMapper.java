package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlFailedTaskArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 失败任务归档Mapper
 */
@Mapper
public interface FailedTaskArchiveMapper extends BaseMapper<EtlFailedTaskArchive> {
}
