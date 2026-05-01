<template>
  <div class="node-panel">
    <el-collapse v-model="activeGroups">
      <!-- General 通用 -->
      <el-collapse-item title="通用" name="general">
        <template #title>
          <div class="group-title">
            <el-icon class="group-icon" style="color:#1890ff"><Connection /></el-icon>
            <span>通用</span>
            <span class="group-count">{{ filteredGeneralNodes.length }}</span>
          </div>
        </template>
        <div class="node-list">
          <div v-for="node in filteredGeneralNodes" :key="node.type" class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node)" @dragend="handleDragEnd">
            <div class="node-color-bar" :style="{ background: node.color }"></div>
            <div class="node-icon-wrap" :style="{ background: node.color + '18', color: node.color }">
              <el-icon :size="14"><component :is="node.icon" /></el-icon>
            </div>
            <div class="node-info">
              <span class="node-label">{{ node.label }}</span>
              <span class="node-desc">{{ node.desc }}</span>
            </div>
          </div>
          <div v-if="filteredGeneralNodes.length === 0" class="node-empty">无匹配节点</div>
        </div>
      </el-collapse-item>

      <!-- Script 脚本 -->
      <el-collapse-item title="脚本" name="script">
        <template #title>
          <div class="group-title">
            <el-icon class="group-icon" style="color:#722ed1"><Document /></el-icon>
            <span>脚本</span>
            <span class="group-count">{{ filteredScriptNodes.length }}</span>
          </div>
        </template>
        <div class="node-list">
          <div v-for="node in filteredScriptNodes" :key="node.type" class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node)" @dragend="handleDragEnd">
            <div class="node-color-bar" :style="{ background: node.color }"></div>
            <div class="node-icon-wrap" :style="{ background: node.color + '18', color: node.color }">
              <el-icon :size="14"><component :is="node.icon" /></el-icon>
            </div>
            <div class="node-info">
              <span class="node-label">{{ node.label }}</span>
              <span class="node-desc">{{ node.desc }}</span>
            </div>
          </div>
          <div v-if="filteredScriptNodes.length === 0" class="node-empty">无匹配节点</div>
        </div>
      </el-collapse-item>

      <!-- Process 流程控制 -->
      <el-collapse-item title="流程" name="process">
        <template #title>
          <div class="group-title">
            <el-icon class="group-icon" style="color:#faad14"><Share /></el-icon>
            <span>流程</span>
            <span class="group-count">{{ filteredProcessNodes.length }}</span>
          </div>
        </template>
        <div class="node-list">
          <div v-for="node in filteredProcessNodes" :key="node.type" class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node)" @dragend="handleDragEnd">
            <div class="node-color-bar" :style="{ background: node.color }"></div>
            <div class="node-icon-wrap" :style="{ background: node.color + '18', color: node.color }">
              <el-icon :size="14"><component :is="node.icon" /></el-icon>
            </div>
            <div class="node-info">
              <span class="node-label">{{ node.label }}</span>
              <span class="node-desc">{{ node.desc }}</span>
            </div>
          </div>
          <div v-if="filteredProcessNodes.length === 0" class="node-empty">无匹配节点</div>
        </div>
      </el-collapse-item>

      <!-- Others 其他 -->
      <el-collapse-item title="其他" name="others">
        <template #title>
          <div class="group-title">
            <el-icon class="group-icon" style="color:#8c8c8c"><More /></el-icon>
            <span>其他</span>
            <span class="group-count">{{ filteredOthersNodes.length }}</span>
          </div>
        </template>
        <div class="node-list">
          <div v-for="node in filteredOthersNodes" :key="node.type" class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node)" @dragend="handleDragEnd">
            <div class="node-color-bar" :style="{ background: node.color }"></div>
            <div class="node-icon-wrap" :style="{ background: node.color + '18', color: node.color }">
              <el-icon :size="14"><component :is="node.icon" /></el-icon>
            </div>
            <div class="node-info">
              <span class="node-label">{{ node.label }}</span>
              <span class="node-desc">{{ node.desc }}</span>
            </div>
          </div>
          <div v-if="filteredOthersNodes.length === 0" class="node-empty">无匹配节点</div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue'

const props = defineProps({
  searchKeyword: { type: String, default: '' }
})

const emit = defineEmits(['drag-node', 'drag-end'])
const dagCanvasAddNode = inject('dagCanvasAddNode', null)

const activeGroups = ref(['general', 'script', 'process', 'others'])

const filterNodes = (nodes) => {
  if (!props.searchKeyword) return nodes
  const kw = props.searchKeyword.toLowerCase()
  return nodes.filter(n => n.label.toLowerCase().includes(kw) || n.type.toLowerCase().includes(kw))
}

// General 通用节点 (与FineDataLink 1:1匹配)
const generalNodes = [
  { type: 'DATA_SYNC', label: '数据同步', desc: 'Data Synchronization', icon: 'Connection', color: '#1890ff' },
  { type: 'DATA_TRANSFORM', label: '数据转换', desc: 'Data Transformation', icon: 'Refresh', color: '#13c2c2' },
  { type: 'FILE_TRANSFER', label: '文件传输', desc: 'File Transfer', icon: 'Upload', color: '#52c41a' }
]

// Script 脚本节点 (与FineDataLink 1:1匹配)
const scriptNodes = [
  { type: 'SQL_SCRIPT', label: 'SQL脚本', desc: 'SQL Script', icon: 'Document', color: '#1890ff' },
  { type: 'SHELL_SCRIPT', label: 'Shell脚本', desc: 'Shell Script', icon: 'Monitor', color: '#722ed1' },
  { type: 'BAT_SCRIPT', label: 'Bat脚本', desc: 'Bat Script', icon: 'Monitor', color: '#faad14' },
  { type: 'PYTHON_SCRIPT', label: 'Python脚本', desc: 'Python Script', icon: 'Document', color: '#52c41a' }
]

// Process 流程控制节点 (与FineDataLink 1:1匹配)
const processNodes = [
  { type: 'PARAM_ASSIGN', label: '参数赋值', desc: 'Parameter Assignment', icon: 'Setting', color: '#722ed1' },
  { type: 'CONDITION', label: '条件分支', desc: 'Conditional Branch', icon: 'Share', color: '#faad14' },
  { type: 'CALL_TASK', label: '调用任务', desc: 'Task Invocation', icon: 'Connection', color: '#13c2c2' },
  { type: 'LOOP_CONTAINER', label: '循环容器', desc: 'Loop Container', icon: 'Refresh', color: '#1890ff' },
  { type: 'MESSAGE_NOTIFY', label: '消息通知', desc: 'Notification', icon: 'Bell', color: '#eb2f96' },
  { type: 'VIRTUAL_NODE', label: '虚拟节点', desc: 'Virtual Node', icon: 'Remove', color: '#8c8c8c' }
]

// Others 其他 (与FineDataLink 1:1匹配)
const othersNodes = [
  { type: 'NOTE', label: '备注', desc: 'Remark', icon: 'Edit', color: '#faad14' }
]

const filteredGeneralNodes = computed(() => filterNodes(generalNodes))
const filteredScriptNodes = computed(() => filterNodes(scriptNodes))
const filteredProcessNodes = computed(() => filterNodes(processNodes))
const filteredOthersNodes = computed(() => filterNodes(othersNodes))

const currentDragNode = ref(null)

const handleDragStart = (e, node) => {
  currentDragNode.value = node
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/json', JSON.stringify(node))
  emit('drag-node', node)
}

const handleDragEnd = (e) => {
  if (currentDragNode.value && e.clientX > 0 && e.clientY > 0) {
    if (dagCanvasAddNode) dagCanvasAddNode(currentDragNode.value, e.clientX, e.clientY)
    emit('drag-end', { node: currentDragNode.value, x: e.clientX, y: e.clientY })
  }
  currentDragNode.value = null
}
</script>

<style lang="scss" scoped>
.node-panel {
  height: 100%;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 2px; }

  :deep(.el-collapse) {
    border: none;
    --el-collapse-header-height: 38px;

    .el-collapse-item__header {
      height: 38px;
      background: transparent;
      border-bottom: 1px solid #f5f5f5;
      padding: 0 12px;
      font-size: 13px;
      font-weight: 500;
      color: #333;

      &:hover { background: #fafafa; }
    }

    .el-collapse-item__wrap { border-bottom: none; background: transparent; }
    .el-collapse-item__content { padding: 0; }
  }
}

.group-title {
  display: flex; align-items: center; gap: 8px; width: 100%;
  .group-icon { flex-shrink: 0; }
  span { flex: 1; }
  .group-count {
    font-size: 11px; color: #bbb; background: #f5f5f5;
    padding: 1px 8px; border-radius: 10px; font-weight: 400;
  }
}

.node-list { padding: 4px 0; }

.node-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; cursor: grab;
  transition: all 0.15s; position: relative; overflow: hidden;

  &:hover { background: #e6f4ff; }
  &:active { cursor: grabbing; background: #bae0ff; }

  .node-color-bar {
    position: absolute; left: 0; top: 0; bottom: 0;
    width: 3px; border-radius: 0 2px 2px 0;
  }

  .node-icon-wrap {
    width: 28px; height: 28px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 6px; flex-shrink: 0; margin-left: 4px;
  }

  .node-info {
    flex: 1; min-width: 0;
    display: flex; flex-direction: column; gap: 2px;
    .node-label { font-size: 12px; color: #333; font-weight: 500; }
    .node-desc {
      font-size: 11px; color: #999;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
  }
}

.node-empty {
  padding: 16px; text-align: center; color: #ccc; font-size: 12px;
}
</style>
