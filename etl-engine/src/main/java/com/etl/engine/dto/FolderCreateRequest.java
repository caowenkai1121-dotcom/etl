package com.etl.engine.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建文件夹请求
 */
@Data
public class FolderCreateRequest {

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 200, message = "文件夹名称长度不能超过200")
    private String name;

    private Long parentId = 0L;

    private Integer sortOrder;
}
