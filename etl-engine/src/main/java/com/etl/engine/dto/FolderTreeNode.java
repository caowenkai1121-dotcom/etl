package com.etl.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 文件夹树节点响应
 */
@Data
public class FolderTreeNode {

    private Long id;

    private String name;

    private Long parentId;

    private String path;

    private Integer sortOrder;

    private Boolean isFolder = true;

    private List<FolderTreeNode> children;

    /**
     * 任务数量（仅文件夹有效）
     */
    private Integer taskCount;

    /**
     * 发布状态（仅任务有效）
     */
    private String publishStatus;

    /**
     * 发布状态统计（仅文件夹有效）
     */
    private PublishStatusCount publishCount;

    @Data
    public static class PublishStatusCount {
        private Integer published = 0;
        private Integer pending = 0;
        private Integer updated = 0;
        private Integer draft = 0;
    }
}
