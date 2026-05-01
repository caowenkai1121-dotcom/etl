package com.etl.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.engine.dto.FolderResponse;
import com.etl.engine.entity.EtlFolder;
import com.etl.engine.mapper.FolderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件夹服务
 */
@Service
@RequiredArgsConstructor
public class FolderService extends ServiceImpl<FolderMapper, EtlFolder> {

    /**
     * 获取文件夹树
     */
    public List<FolderResponse> getFolderTree(String folderType) {
        List<EtlFolder> folders = lambdaQuery()
            .eq(folderType != null, EtlFolder::getFolderType, folderType)
            .orderByAsc(EtlFolder::getSortOrder)
            .orderByAsc(EtlFolder::getName)
            .list();

        return folders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * 创建文件夹
     */
    @Transactional
    public Long createFolder(String name, Long parentId, String folderType) {
        EtlFolder folder = new EtlFolder();
        folder.setName(name);
        folder.setParentId(parentId != null ? parentId : 0L);
        folder.setFolderType(folderType != null ? folderType : "WORKFLOW");
        folder.setSortOrder(0);
        folder.setCreateTime(LocalDateTime.now());

        save(folder);
        return folder.getId();
    }

    /**
     * 更新文件夹
     */
    @Transactional
    public void updateFolder(Long id, String name, Integer sortOrder) {
        EtlFolder folder = getById(id);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        folder.setName(name);
        if (sortOrder != null) {
            folder.setSortOrder(sortOrder);
        }
        folder.setUpdateTime(LocalDateTime.now());
        updateById(folder);
    }

    /**
     * 删除文件夹
     */
    @Transactional
    public void deleteFolder(Long id) {
        removeById(id);
    }

    /**
     * 获取子文件夹列表
     */
    public List<FolderResponse> getChildren(Long parentId) {
        return lambdaQuery()
            .eq(EtlFolder::getParentId, parentId != null ? parentId : 0L)
            .orderByAsc(EtlFolder::getSortOrder)
            .list()
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * 转换为响应DTO
     */
    private FolderResponse toResponse(EtlFolder entity) {
        FolderResponse response = new FolderResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setParentId(entity.getParentId());
        response.setSortOrder(entity.getSortOrder());
        response.setFolderType(entity.getFolderType());
        response.setCreateTime(entity.getCreateTime());

        // 计算子文件夹数量
        response.setChildCount(lambdaQuery()
            .eq(EtlFolder::getParentId, entity.getId())
            .count().intValue());

        return response;
    }
}