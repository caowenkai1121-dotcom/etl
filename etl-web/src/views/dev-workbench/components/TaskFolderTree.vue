<template>
  <div class="task-folder-tree">
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="treeProps"
      node-key="id"
      :default-expanded-keys="expandedKeys"
      :highlight-current="true"
      :expand-on-click-node="false"
      @node-click="handleNodeClick"
      @node-contextmenu="handleContextMenu"
    >
      <template #default="{ node, data }">
        <div class="tree-node" :class="{ 'is-task': !data.isFolder }">
          <!-- 文件夹图标 -->
          <el-icon v-if="data.isFolder" class="folder-icon">
            <FolderOpened v-if="node.expanded" />
            <Folder v-else />
          </el-icon>

          <!-- 任务图标 -->
          <el-icon v-else class="task-icon" :class="getTaskStatusClass(data)">
            <Document />
          </el-icon>

          <!-- 名称 -->
          <span class="node-name">{{ data.name }}</span>

          <!-- 发布状态标签 -->
          <el-tag
            v-if="!data.isFolder && data.publishStatus"
            :type="getPublishStatusType(data.publishStatus)"
            size="small"
            class="status-tag"
          >
            {{ getPublishStatusText(data.publishStatus) }}
          </el-tag>

          <!-- 任务数量 -->
          <span v-if="data.isFolder && data.taskCount" class="task-count">
            ({{ data.taskCount }})
          </span>

          <!-- 操作按钮 -->
          <div class="node-actions" @click.stop>
            <el-dropdown
              v-if="data.isFolder"
              trigger="click"
              @command="(cmd) => handleCommand(cmd, data)"
            >
              <el-icon class="action-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="createFolder">
                    <el-icon><FolderAdd /></el-icon>新建文件夹
                  </el-dropdown-item>
                  <el-dropdown-item command="createTask">
                    <el-icon><Plus /></el-icon>新建任务
                  </el-dropdown-item>
                  <el-dropdown-item command="rename">
                    <el-icon><Edit /></el-icon>重命名
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon>删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createFolder, updateFolder, deleteFolder as deleteFolderApi } from '@/api'

const router = useRouter()

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  currentTask: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['select-folder', 'select-task'])

const treeRef = ref(null)
const expandedKeys = ref([])

// 树属性配置
const treeProps = {
  children: 'children',
  label: 'name'
}

// 计算树数据，添加任务列表到文件夹
const treeData = computed(() => {
  return buildTreeData(props.data)
})

// 构建树数据
const buildTreeData = (nodes) => {
  if (!nodes) return []
  return nodes.map(node => {
    // 任务节点（isFolder=false）没有children
    if (!node.isFolder) {
      return node
    }
    // 文件夹节点递归处理children
    return {
      ...node,
      children: node.children ? buildTreeData(node.children) : []
    }
  })
}

// 处理节点点击
const handleNodeClick = (data, node) => {
  if (data.isFolder) {
    emit('select-folder', data)
  } else {
    emit('select-task', data)
  }
}

// 处理右键菜单
const handleContextMenu = (event, data, node) => {
  event.preventDefault()
}

// 处理下拉命令
const handleCommand = (command, data) => {
  switch (command) {
    case 'createFolder':
      createSubFolder(data)
      break
    case 'createTask':
      createTask(data)
      break
    case 'rename':
      renameFolder(data)
      break
    case 'delete':
      deleteFolder(data)
      break
  }
}

// 创建子文件夹
const createSubFolder = async (parentFolder) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (value) {
      await createFolder({ name: value, parentId: parentFolder.id || parentFolder.value || 0 })
      ElMessage.success('创建成功')
      emit('select-folder', { action: 'refresh' })
    }
  } catch (e) {
    // 用户取消
  }
}

// 创建任务
const createTask = (folder) => {
  router.push({ path: '/dev-workbench', query: { folderId: folder.id, action: 'createTask' } })
  emit('select-folder', { ...folder, action: 'createTask' })
}

// 重命名文件夹
const renameFolder = async (folder) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: folder.name
    })
    if (value && value !== folder.name) {
      await updateFolder(folder.id, { name: value })
      ElMessage.success('重命名成功')
      emit('select-folder', { action: 'refresh' })
    }
  } catch (e) {
    // 用户取消
  }
}

// 删除文件夹
const deleteFolder = async (folder) => {
  try {
    await ElMessageBox.confirm('确定删除该文件夹？文件夹内的任务将移至根目录。', '提示', {
      type: 'warning'
    })
    await deleteFolderApi(folder.id)
    ElMessage.success('删除成功')
    emit('select-folder', { action: 'refresh' })
  } catch (e) {
    // 用户取消
  }
}

// 获取发布状态类型
const getPublishStatusType = (status) => {
  const types = {
    'PUBLISHED': 'success',
    'PENDING': 'warning',
    'UPDATED': 'info',
    'DRAFT': ''
  }
  return types[status] || ''
}

// 获取发布状态文本
const getPublishStatusText = (status) => {
  const texts = {
    'PUBLISHED': '已发布',
    'PENDING': '待发布',
    'UPDATED': '待更新',
    'DRAFT': '草稿'
  }
  return texts[status] || status
}

// 获取任务状态样式类
const getTaskStatusClass = (task) => {
  return `status-${task.publishStatus?.toLowerCase() || 'draft'}`
}

// 监听当前任务变化，自动展开父节点
watch(() => props.currentTask, (task) => {
  if (task?.id) {
    // 查找任务所在文件夹路径并展开
    const path = findNodePath(props.data, task.id)
    if (path.length > 0) {
      expandedKeys.value = path.slice(0, -1)
    }
  }
}, { immediate: true })

// 查找节点路径（用function声明，支持变量提升，避免TDZ错误）
function findNodePath(nodes, targetId, path = []) {
  for (const node of nodes) {
    const currentPath = [...path, node.id]
    if (node.id === targetId) {
      return currentPath
    }
    if (node.children?.length) {
      const found = findNodePath(node.children, targetId, currentPath)
      if (found.length) return found
    }
  }
  return []
}
</script>

<style lang="scss" scoped>
.task-folder-tree {
  height: 100%;

  :deep(.el-tree) {
    background: transparent;

    .el-tree-node__content {
      height: 36px;
      border-radius: 6px;
      margin: 2px 0;

      &:hover {
        background: #f0f5ff;
      }

      &.is-current {
        background: #e6f4ff;
      }
    }
  }
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding-right: 8px;

  .folder-icon {
    color: #faad14;
    font-size: 16px;
  }

  .task-icon {
    font-size: 14px;

    &.status-published {
      color: #52c41a;
    }

    &.status-pending {
      color: #faad14;
    }

    &.status-updated {
      color: #1890ff;
    }

    &.status-draft {
      color: #999;
    }
  }

  .node-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
  }

  .status-tag {
    font-size: 10px;
    padding: 0 4px;
    height: 18px;
    line-height: 16px;
  }

  .task-count {
    color: #999;
    font-size: 12px;
  }

  .node-actions {
    opacity: 0;
    transition: opacity 0.2s;

    .action-icon {
      cursor: pointer;
      color: #666;

      &:hover {
        color: #1890ff;
      }
    }
  }

  &:hover .node-actions {
    opacity: 1;
  }
}
</style>