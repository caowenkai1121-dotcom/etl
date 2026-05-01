package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlDagNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * DAG节点Mapper
 */
@Mapper
public interface DagNodeMapper extends BaseMapper<EtlDagNode> {

    /**
     * 获取DAG的所有节点
     */
    @Select("SELECT * FROM etl_dag_node WHERE dag_id = #{dagId} ORDER BY position_x, position_y")
    List<EtlDagNode> selectByDagId(@Param("dagId") Long dagId);

    /**
     * 根据节点ID获取节点
     */
    @Select("SELECT * FROM etl_dag_node WHERE dag_id = #{dagId} AND node_id = #{nodeId}")
    EtlDagNode selectByDagIdAndNodeId(@Param("dagId") Long dagId, @Param("nodeId") String nodeId);
}