package com.etl.scheduler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.scheduler.entity.EtlScheduleDag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduleDagMapper extends BaseMapper<EtlScheduleDag> {
}
