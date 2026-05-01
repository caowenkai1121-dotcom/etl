package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlFolder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件夹Mapper
 */
@Mapper
public interface FolderMapper extends BaseMapper<EtlFolder> {
}
