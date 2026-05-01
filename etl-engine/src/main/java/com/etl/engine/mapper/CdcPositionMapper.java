package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlCdcPosition;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDC同步位点Mapper
 */
@Mapper
public interface CdcPositionMapper extends BaseMapper<EtlCdcPosition> {

    /**
     * 插入或更新CDC位点
     */
    @Insert("INSERT INTO etl_cdc_position (task_id, source_ds_id, table_name, position_type, position_value, binlog_file, binlog_position, gtid) " +
            "VALUES (#{taskId}, #{sourceDsId}, #{tableName}, #{positionType}, #{positionValue}, #{binlogFile}, #{binlogPosition}, #{gtid}) " +
            "ON DUPLICATE KEY UPDATE " +
            "position_value = #{positionValue}, " +
            "binlog_file = #{binlogFile}, " +
            "binlog_position = #{binlogPosition}, " +
            "gtid = #{gtid}, " +
            "updated_at = NOW()")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrUpdate(EtlCdcPosition position);

    /**
     * 根据任务ID查询位点
     */
    @Select("SELECT * FROM etl_cdc_position WHERE task_id = #{taskId}")
    EtlCdcPosition selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据任务ID删除位点
     */
    @Delete("DELETE FROM etl_cdc_position WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") Long taskId);

    /**
     * 更新任务位置
     */
    @Update("UPDATE etl_cdc_position SET position_value = #{position}, updated_at = NOW() WHERE task_id = #{taskId}")
    int updatePositionByTaskId(@Param("taskId") Long taskId, @Param("position") String position);

    /**
     * 查询过期任务的ID列表
     */
    @Select("SELECT task_id FROM etl_cdc_position WHERE updated_at < #{threshold}")
    List<Long> selectStaleTaskIds(@Param("threshold") LocalDateTime threshold);

    /**
     * 删除过期的位点记录
     */
    @Delete("DELETE FROM etl_cdc_position WHERE updated_at < #{threshold}")
    int deleteByUpdatedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * 根据任务ID和连接器名称查询位点
     */
    @Select("SELECT * FROM etl_cdc_position WHERE task_id = #{taskId} AND position_type = #{connectorName}")
    EtlCdcPosition findByTaskAndConnector(@Param("taskId") Long taskId, @Param("connectorName") String connectorName);
}
