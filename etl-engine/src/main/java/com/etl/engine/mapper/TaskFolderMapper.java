package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务文件夹Mapper
 */
@Mapper
public interface TaskFolderMapper extends BaseMapper<EtlTaskFolder> {

    /**
     * 查询子文件夹
     */
    @Select("SELECT * FROM etl_task_folder WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order, id")
    List<EtlTaskFolder> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有文件夹（树形结构用）
     */
    @Select("SELECT * FROM etl_task_folder WHERE deleted = 0 ORDER BY sort_order, id")
    List<EtlTaskFolder> selectAllFolders();

    /**
     * 检查文件夹名称是否存在
     */
    @Select("SELECT COUNT(*) FROM etl_task_folder WHERE name = #{name} AND parent_id = #{parentId} AND deleted = 0")
    int countByNameAndParentId(@Param("name") String name, @Param("parentId") Long parentId);

    /**
     * 查询最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM etl_task_folder WHERE parent_id = #{parentId} AND deleted = 0")
    int selectMaxSortOrder(@Param("parentId") Long parentId);
}
