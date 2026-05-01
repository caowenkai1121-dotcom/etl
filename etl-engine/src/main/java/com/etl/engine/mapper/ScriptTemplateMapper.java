package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlScriptTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脚本模板Mapper
 */
@Mapper
public interface ScriptTemplateMapper extends BaseMapper<EtlScriptTemplate> {
}
