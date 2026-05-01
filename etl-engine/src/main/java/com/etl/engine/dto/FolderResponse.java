package com.etl.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件夹响应DTO
 */
@Data
public class FolderResponse {

    private Long id;

    private String name;

    private Long parentId;

    private Integer sortOrder;

    private String folderType;

    private LocalDateTime createTime;

    /**
     * 子文件夹数量
     */
    private Integer childCount;

    /**
     * 包含的任务/工作流数量
     */
    private Integer itemCount;
}
