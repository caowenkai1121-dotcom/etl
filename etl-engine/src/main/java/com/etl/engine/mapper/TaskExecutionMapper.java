package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务执行记录Mapper
 */
@Mapper
public interface TaskExecutionMapper extends BaseMapper<EtlTaskExecution> {

    /**
     * 查询指定时间范围内的所有任务执行记录
     */
    @Select("SELECT * FROM etl_task_execution WHERE start_time >= #{startTime} AND start_time <= #{endTime}")
    List<EtlTaskExecution> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 批量查询多个任务的最新执行记录（一条SQL，避免N+1）
     */
    @Select("<script>" +
        "SELECT t.* FROM etl_task_execution t " +
        "INNER JOIN (SELECT task_id, MAX(id) AS max_id FROM etl_task_execution " +
        "WHERE task_id IN <foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
        "GROUP BY task_id) latest ON t.id = latest.max_id" +
        "</script>")
    List<EtlTaskExecution> selectLatestByTaskIds(@Param("taskIds") List<Long> taskIds);
}
