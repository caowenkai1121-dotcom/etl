package com.etl.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etl.common.exception.EtlException;
import com.etl.engine.dto.FolderCreateRequest;
import com.etl.engine.dto.FolderTreeNode;
import com.etl.engine.dto.FolderUpdateRequest;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.entity.EtlTaskFolder;
import com.etl.engine.mapper.SyncTaskMapper;
import com.etl.engine.mapper.TaskFolderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务文件夹服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskFolderService {

    private final TaskFolderMapper taskFolderMapper;
    private final SyncTaskMapper syncTaskMapper;

    /**
     * 获取文件夹树（包含任务节点）
     */
    public List<FolderTreeNode> getFolderTree() {
        List<EtlTaskFolder> allFolders = taskFolderMapper.selectAllFolders();

        LambdaQueryWrapper<EtlSyncTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(EtlSyncTask::getDeleted, 0);
        taskWrapper.select(EtlSyncTask::getId, EtlSyncTask::getTaskName, EtlSyncTask::getFolderId, EtlSyncTask::getPublishStatus);
        List<EtlSyncTask> allTasks = syncTaskMapper.selectList(taskWrapper);

        // 使用 LinkedHashMap 避免 HashMap 内部冲突导致 ConcurrentModificationException，
        // 同时预分配容量防止 rehash
        int capacity = allFolders.size() + allTasks.size() + 5;
        Map<Long, FolderTreeNode> nodeMap = new LinkedHashMap<>(capacity < 16 ? 16 : capacity);
        List<FolderTreeNode> rootNodes = new ArrayList<>();

        for (EtlTaskFolder folder : allFolders) {
            FolderTreeNode node = new FolderTreeNode();
            node.setId(folder.getId());
            node.setName(folder.getName());
            node.setParentId(folder.getParentId());
            node.setPath(folder.getPath());
            node.setSortOrder(folder.getSortOrder());
            node.setIsFolder(true);
            node.setChildren(new ArrayList<>());
            node.setPublishCount(new FolderTreeNode.PublishStatusCount());
            nodeMap.put(folder.getId(), node);
        }

        // 添加任务节点（使用负数ID避免与文件夹ID冲突）
        for (EtlSyncTask task : allTasks) {
            FolderTreeNode taskNode = new FolderTreeNode();
            taskNode.setId(-task.getId()); // 使用负数ID区分任务
            taskNode.setName(task.getTaskName());
            taskNode.setParentId(task.getFolderId() != null ? task.getFolderId() : 0L);
            taskNode.setIsFolder(false);
            taskNode.setPublishStatus(task.getPublishStatus());
            nodeMap.put(taskNode.getId(), taskNode);
        }

        // 先创建虚拟根节点（用于存放根目录下的任务）
        FolderTreeNode virtualRoot = new FolderTreeNode();
        virtualRoot.setId(0L);
        virtualRoot.setName("根目录");
        virtualRoot.setIsFolder(true);
        virtualRoot.setChildren(new ArrayList<>());
        nodeMap.put(0L, virtualRoot);

        // 将 nodeMap.values() 转换为列表，避免 ConcurrentModificationException
        List<FolderTreeNode> allNodes = new ArrayList<>(nodeMap.values());

        // 构建树结构
        for (FolderTreeNode node : allNodes) {
            if (node.getId().equals(0L)) {
                // 跳过虚拟根节点本身
                continue;
            }
            if (node.getIsFolder()) {
                // 文件夹节点
                if (node.getParentId() == null || node.getParentId() == 0) {
                    virtualRoot.getChildren().add(node);
                } else {
                    FolderTreeNode parentNode = nodeMap.get(node.getParentId());
                    if (parentNode != null && parentNode.getChildren() != null) {
                        parentNode.getChildren().add(node);
                    }
                }
            } else {
                // 任务节点，添加到父文件夹
                Long parentId = node.getParentId() != null ? node.getParentId() : 0L;
                if (parentId == 0) {
                    // 根目录下的任务，添加到虚拟根节点
                    if (virtualRoot.getChildren() != null) {
                        virtualRoot.getChildren().add(node);
                    }
                } else {
                    FolderTreeNode parentNode = nodeMap.get(parentId);
                    if (parentNode != null && parentNode.getChildren() != null) {
                        parentNode.getChildren().add(node);
                    }
                }
            }
        }

        // 如果虚拟根节点有子节点，添加到结果中
        if (!virtualRoot.getChildren().isEmpty()) {
            rootNodes.add(0, virtualRoot);
        }

        // 排序
        sortTreeNodes(rootNodes);

        return rootNodes;
    }

    /**
     * 创建文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlTaskFolder createFolder(FolderCreateRequest request, String currentUser) {
        // 检查名称是否重复
        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;
        int count = taskFolderMapper.countByNameAndParentId(request.getName(), parentId);
        if (count > 0) {
            throw new EtlException("同一目录下已存在同名文件夹");
        }

        // 获取父文件夹路径
        String parentPath = "";
        if (parentId != null && parentId > 0) {
            EtlTaskFolder parent = taskFolderMapper.selectById(parentId);
            if (parent == null) {
                throw new EtlException("父文件夹不存在");
            }
            parentPath = parent.getPath();
        }

        // 获取排序号
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = taskFolderMapper.selectMaxSortOrder(parentId) + 1;
        }

        // 创建文件夹
        EtlTaskFolder folder = new EtlTaskFolder();
        folder.setName(request.getName());
        folder.setParentId(parentId);
        folder.setPath(parentPath + "/" + request.getName());
        folder.setSortOrder(sortOrder);
        folder.setCreatedBy(currentUser);

        taskFolderMapper.insert(folder);

        log.info("创建文件夹成功: id={}, name={}, path={}", folder.getId(), folder.getName(), folder.getPath());
        return folder;
    }

    /**
     * 更新文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlTaskFolder updateFolder(Long id, FolderUpdateRequest request, String currentUser) {
        EtlTaskFolder folder = taskFolderMapper.selectById(id);
        if (folder == null) {
            throw new EtlException("文件夹不存在");
        }

        // 检查名称是否重复（排除自身）
        Long parentId = request.getParentId() != null ? request.getParentId() : folder.getParentId();
        int count = taskFolderMapper.countByNameAndParentId(request.getName(), parentId);
        if (count > 0 && !folder.getName().equals(request.getName())) {
            throw new EtlException("同一目录下已存在同名文件夹");
        }

        // 更新文件夹
        folder.setName(request.getName());
        if (request.getParentId() != null && !request.getParentId().equals(folder.getParentId())) {
            // 移动到新目录
            folder.setParentId(request.getParentId());
            // 更新路径
            EtlTaskFolder parent = taskFolderMapper.selectById(request.getParentId());
            folder.setParentId(request.getParentId());
            folder.setPath(parent != null ? parent.getPath() + "/" + request.getName() : "/" + request.getName());
            // TODO: 更新所有子文件夹路径
        }
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }
        folder.setUpdatedBy(currentUser);

        taskFolderMapper.updateById(folder);

        log.info("更新文件夹成功: id={}, name={}", folder.getId(), folder.getName());
        return folder;
    }

    /**
     * 删除文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long id) {
        EtlTaskFolder folder = taskFolderMapper.selectById(id);
        if (folder == null) {
            throw new EtlException("文件夹不存在");
        }

        // 检查是否有子文件夹
        List<EtlTaskFolder> children = taskFolderMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new EtlException("文件夹下存在子文件夹，无法删除");
        }

        // TODO: 检查文件夹下是否有任务

        // 逻辑删除
        taskFolderMapper.deleteById(id);

        log.info("删除文件夹成功: id={}, name={}", id, folder.getName());
    }

    /**
     * 移动文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveFolder(Long id, Long targetParentId, Integer sortOrder) {
        EtlTaskFolder folder = taskFolderMapper.selectById(id);
        if (folder == null) {
            throw new EtlException("文件夹不存在");
        }

        // 检查目标文件夹是否是自身的子文件夹
        if (isChildFolder(id, targetParentId)) {
            throw new EtlException("不能移动到自身或子文件夹下");
        }

        // 更新父文件夹和排序
        folder.setParentId(targetParentId != null ? targetParentId : 0L);
        if (sortOrder != null) {
            folder.setSortOrder(sortOrder);
        }

        // 更新路径
        if (targetParentId != null && targetParentId > 0) {
            EtlTaskFolder parent = taskFolderMapper.selectById(targetParentId);
            if (parent != null) {
                folder.setPath(parent.getPath() + "/" + folder.getName());
            }
        } else {
            folder.setPath("/" + folder.getName());
        }

        taskFolderMapper.updateById(folder);

        log.info("移动文件夹成功: id={}, targetParentId={}", id, targetParentId);
    }

    /**
     * 批量更新排序（拖拽排序）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderFolders(List<Map<String, Object>> orders, String currentUser) {
        for (Map<String, Object> order : orders) {
            Long id = Long.valueOf(order.get("id").toString());
            Integer sortOrder = order.get("sortOrder") != null ? Integer.valueOf(order.get("sortOrder").toString()) : null;
            Long parentId = order.get("parentId") != null ? Long.valueOf(order.get("parentId").toString()) : null;

            EtlTaskFolder folder = taskFolderMapper.selectById(id);
            if (folder != null) {
                if (sortOrder != null) {
                    folder.setSortOrder(sortOrder);
                }
                if (parentId != null && !parentId.equals(folder.getParentId())) {
                    folder.setParentId(parentId);
                    if (parentId > 0) {
                        EtlTaskFolder parent = taskFolderMapper.selectById(parentId);
                        if (parent != null) {
                            folder.setPath(parent.getPath() + "/" + folder.getName());
                        }
                    } else {
                        folder.setPath("/" + folder.getName());
                    }
                }
                folder.setUpdatedBy(currentUser);
                taskFolderMapper.updateById(folder);
            }
        }
        log.info("批量更新排序完成: count={}", orders.size());
    }

    /**
     * 复制文件夹（含子文件夹）
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlTaskFolder copyFolder(Long sourceId, String newName, Long targetParentId, String currentUser) {
        EtlTaskFolder source = taskFolderMapper.selectById(sourceId);
        if (source == null) {
            throw new EtlException("源文件夹不存在");
        }

        // 确定目标父文件夹
        Long destParentId = targetParentId != null ? targetParentId : source.getParentId();
        String folderName = newName != null ? newName : source.getName() + "_副本";
        String parentPath = "";
        if (destParentId != null && destParentId > 0) {
            EtlTaskFolder parent = taskFolderMapper.selectById(destParentId);
            if (parent != null) {
                parentPath = parent.getPath();
            }
        }

        // 创建新文件夹
        EtlTaskFolder newFolder = new EtlTaskFolder();
        newFolder.setName(folderName);
        newFolder.setParentId(destParentId);
        newFolder.setPath(parentPath + "/" + folderName);
        newFolder.setSortOrder(taskFolderMapper.selectMaxSortOrder(destParentId) + 1);
        newFolder.setCreatedBy(currentUser);
        taskFolderMapper.insert(newFolder);

        // 复制子文件夹
        List<EtlTaskFolder> children = taskFolderMapper.selectByParentId(sourceId);
        for (EtlTaskFolder child : children) {
            copyFolder(child.getId(), child.getName(), newFolder.getId(), currentUser);
        }

        log.info("复制文件夹成功: sourceId={}, newId={}, name={}", sourceId, newFolder.getId(), folderName);
        return newFolder;
    }

    /**
     * 检查是否是子文件夹
     */
    private boolean isChildFolder(Long parentId, Long childId) {
        if (childId == null || childId <= 0) {
            return false;
        }
        EtlTaskFolder child = taskFolderMapper.selectById(childId);
        if (child == null) {
            return false;
        }
        if (child.getParentId().equals(parentId)) {
            return true;
        }
        return isChildFolder(parentId, child.getParentId());
    }

    /**
     * 对树节点排序
     */
    private void sortTreeNodes(List<FolderTreeNode> nodes) {
        nodes.sort(Comparator.comparing(FolderTreeNode::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FolderTreeNode::getId));

        for (FolderTreeNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTreeNodes(node.getChildren());
            }
        }
    }
}
