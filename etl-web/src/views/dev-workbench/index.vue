<template>
  <div class="ide-editor" @keydown="handleKeydown">
    <!-- 顶部工具栏 -->
    <div class="ide-toolbar">
      <div class="toolbar-section left">
        <el-button text size="small" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <el-divider direction="vertical" />
        <div class="task-title-area">
          <el-icon :size="18" class="task-icon"><Calendar /></el-icon>
          <span class="task-title" :title="pageTitle">{{ pageTitle }}</span>
          <el-tag v-if="store.currentTask?.publishStatus" :type="statusTagType" size="small" effect="plain">
            {{ statusText }}
          </el-tag>
        </div>
        <el-divider direction="vertical" />
        <el-radio-group v-model="store.mode" size="small" class="mode-switch">
          <el-radio-button value="dev">开发</el-radio-button>
          <el-radio-button value="prod">生产</el-radio-button>
        </el-radio-group>
        <el-divider direction="vertical" />
        <el-radio-group v-model="taskSubType" size="small" @change="handleTaskTypeChange">
          <el-radio-button value="scheduled">定时任务</el-radio-button>
          <el-radio-button value="realtime">实时任务</el-radio-button>
        </el-radio-group>
        <span class="unsaved-dot" v-if="hasUnsavedChanges" title="有未保存更改"></span>
      </div>

      <div class="toolbar-section center">
        <el-button-group size="small">
          <el-tooltip content="撤销 Ctrl+Z" placement="bottom">
            <el-button :disabled="undoStack.length === 0" @click="undo"><el-icon><RefreshLeft /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="重做 Ctrl+Y" placement="bottom">
            <el-button :disabled="redoStack.length === 0" @click="redo"><el-icon><RefreshRight /></el-icon></el-button>
          </el-tooltip>
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button-group size="small">
          <el-tooltip content="复制 Ctrl+C" placement="bottom">
            <el-button :disabled="!store.selectedNode" @click="copySelected"><el-icon><CopyDocument /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="粘贴 Ctrl+V" placement="bottom">
            <el-button :disabled="!clipboardNode" @click="pasteNode"><el-icon><DocumentCopy /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip content="删除 Delete" placement="bottom">
            <el-button :disabled="!store.selectedNode && !store.selectedEdge" @click="deleteSelected"><el-icon><Delete /></el-icon></el-button>
          </el-tooltip>
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button-group size="small">
          <el-tooltip content="放大" placement="bottom"><el-button @click="zoomIn"><el-icon><Plus /></el-icon></el-button></el-tooltip>
          <el-tooltip content="缩小" placement="bottom"><el-button @click="zoomOut"><el-icon><Minus /></el-icon></el-button></el-tooltip>
          <el-tooltip content="适应画布" placement="bottom"><el-button @click="fitCanvas"><el-icon><FullScreen /></el-icon></el-button></el-tooltip>
          <el-tooltip content="自动布局" placement="bottom"><el-button @click="handleAutoLayout"><el-icon><Grid /></el-icon></el-button></el-tooltip>
        </el-button-group>
      </div>

      <div class="toolbar-section right">
        <el-dropdown split-button type="success" size="small" :loading="store.running" @click="handleRun" @command="handleRunCommand">
          <el-icon><CaretRight /></el-icon> 运行
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="run">立即运行</el-dropdown-item>
              <el-dropdown-item command="params">带参数运行</el-dropdown-item>
              <el-dropdown-item command="debug">调试运行</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button type="primary" size="small" :loading="store.saving" @click="handleSave" :disabled="store.isProdMode || !hasUnsavedChanges">
          <el-icon><Check /></el-icon> 保存
        </el-button>

        <el-button size="small" @click="handleRestore" :disabled="store.isProdMode || !hasUnsavedChanges">
          <el-icon><RefreshLeft /></el-icon> 还原
        </el-button>

        <el-button size="small" @click="handlePublish" :disabled="store.isProdMode || !hasUnsavedChanges">
          <el-icon><Upload /></el-icon> 发布
        </el-button>

        <el-button size="small" @click="showScheduleDialog" :disabled="store.isDevMode">
          <el-icon><Timer /></el-icon> 调度
        </el-button>

        <el-dropdown size="small" @command="handleMoreCommand">
          <el-button size="small">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="copy">复制任务</el-dropdown-item>
              <el-dropdown-item command="version">版本管理</el-dropdown-item>
              <el-dropdown-item command="lineage">数据血缘</el-dropdown-item>
              <el-dropdown-item command="export">导出JSON</el-dropdown-item>
              <el-dropdown-item command="import">导入JSON</el-dropdown-item>
              <el-dropdown-item command="favorite">{{ store.currentTask?.favorite ? '取消收藏' : '收藏任务' }}</el-dropdown-item>
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item divided command="delete" style="color:#ff4d4f">删除任务</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主编辑区：三栏布局 + 底部面板 -->
    <div class="ide-body">
      <!-- 左侧面板：节点面板 + 文件夹树 -->
      <div class="ide-left-panel" :class="{ collapsed: !store.leftPanelExpanded }">
        <div class="panel-tab-bar" v-if="store.leftPanelExpanded">
          <div class="panel-tab" :class="{ active: leftTab === 'node' }" @click="leftTab = 'node'">
            <el-icon><Connection /></el-icon><span>节点</span>
          </div>
          <div class="panel-tab" :class="{ active: leftTab === 'folder' }" @click="leftTab = 'folder'">
            <el-icon><Folder /></el-icon><span>目录</span>
          </div>
        </div>
        <div class="panel-tab-bar mini" v-else>
          <div class="panel-tab" :class="{ active: leftTab === 'node' }" @click="expandLeft('node')">
            <el-icon><Connection /></el-icon>
          </div>
          <div class="panel-tab" :class="{ active: leftTab === 'folder' }" @click="expandLeft('folder')">
            <el-icon><Folder /></el-icon>
          </div>
        </div>

        <div class="panel-content" v-show="store.leftPanelExpanded">
          <div v-if="leftTab === 'node'" class="panel-section">
            <div class="panel-search">
              <el-input v-model="nodeSearch" placeholder="搜索节点..." size="small" clearable>
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
            <NodePanel
              :search-keyword="nodeSearch"
              @drag-end="handleDragEnd"
            />
          </div>
          <div v-if="leftTab === 'folder'" class="panel-section">
            <div class="panel-search">
              <el-button-group size="small" style="width:100%">
                <el-button size="small" @click="createFolder" style="flex:1"><el-icon><FolderAdd /></el-icon> 文件夹</el-button>
                <el-button size="small" type="primary" @click="createTask" style="flex:1"><el-icon><Plus /></el-icon> 任务</el-button>
              </el-button-group>
            </div>
            <TaskFolderTree
              :data="store.folderTree"
              :current-task="store.currentTask"
              @select-task="handleSelectTask"
            />
          </div>
        </div>

        <div class="panel-collapse-btn" @click="toggleLeftPanel">
          <el-icon :size="10"><Fold v-if="store.leftPanelExpanded" /><Expand v-else /></el-icon>
        </div>
      </div>

      <!-- 中间：画布 + 底部面板 -->
      <div class="ide-center">
        <div class="ide-canvas" >
          <DagCanvas
            ref="dagCanvasRef"
            :nodes="store.dagConfig.nodes"
            :edges="store.dagConfig.edges"
            :zoom="store.canvasZoom"
            :editable="store.isDevMode"
            :task-id="route.params.id || route.params.taskId"
            @node-select="handleNodeSelect"
            @edge-select="handleEdgeSelect"
            @node-add="handleNodeAdd"
            @node-remove="handleNodeRemove"
            @edge-add="handleEdgeAdd"
            @edge-remove="handleEdgeRemove"
            @node-move="handleNodeMove"
            @node-dblclick="handleNodeDblClick"
            @canvas-click="handleCanvasClick"
          />
        </div>

        <!-- 底部面板：Log + Statistics -->
        <div
          class="ide-bottom-panel"
          :class="{ collapsed: bottomCollapsed, resizing: isResizingBottom }"
          :style="bottomPanelStyle"
        >
          <div class="bottom-drag-handle" @mousedown="startResizeBottom">
            <span class="drag-indicator"></span>
          </div>
          <div class="bottom-header">
            <div class="bottom-tabs">
              <span
                class="bottom-tab"
                :class="{ active: store.bottomPanelTab === 'log' }"
                @click="store.setBottomPanelTab('log')"
              >Log</span>
              <span
                class="bottom-tab"
                :class="{ active: store.bottomPanelTab === 'stat' }"
                @click="store.setBottomPanelTab('stat')"
              >Statistics</span>
            </div>
            <el-button link size="small" class="bottom-toggle-btn" @click="toggleBottomPanel">
              <el-icon :size="14"><Fold v-if="!bottomCollapsed" /><Expand v-else /></el-icon>
            </el-button>
          </div>
          <div class="bottom-content" v-show="!bottomCollapsed">
            <LogPanel v-if="store.bottomPanelTab === 'log'" :logs="store.runLogs" />
            <StatPanel v-if="store.bottomPanelTab === 'stat'" :stats="store.runStats" />
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="ide-right-panel" :class="{ collapsed: !store.selectedNode || !store.propertyPanelExpanded }">
        <div class="panel-header" v-if="store.selectedNode && store.propertyPanelExpanded">
          <span class="panel-node-name">
            <el-icon :size="16"><Connection /></el-icon>
            {{ store.selectedNode?.name || store.selectedNode?.type || '节点属性' }}
          </span>
          <el-button link size="small" @click="togglePropertyPanel">
            <el-icon><Fold /></el-icon>
          </el-button>
        </div>
        <div class="panel-header mini" v-else-if="store.selectedNode && !store.propertyPanelExpanded">
          <el-button link size="small" class="expand-btn-full" @click="store.propertyPanelExpanded = true">
            <el-icon :size="14"><Expand /></el-icon>
          </el-button>
        </div>
        <div class="panel-body" v-show="store.propertyPanelExpanded && store.selectedNode">
          <NodePropertyPanel
            :node="store.selectedNode"
            :edge="null"
            :editable="store.isDevMode"
            @update="handlePropertyUpdate"
            @delete-node="handleNodeRemove"
            @delete-edge="handleEdgeRemove"
          />
        </div>
      </div>
    </div>

    <!-- 弹窗组件 -->
    <RunParamsDialog v-model:visible="runParamsDialogVisible" :task-id="store.currentTask?.id" @confirm="handleRunWithParams" />
    <ScheduleConfigDialog v-model:visible="scheduleDialogVisible" :task-id="store.currentTask?.id" @confirm="handleScheduleConfirm" />
    <PublishDialog v-model:visible="publishDialogVisible" :task-id="store.currentTask?.id" @confirm="handlePublishConfirm" />
    <VersionDialog v-model:visible="versionDialogVisible" :task-id="store.currentTask?.id" />
    <LineageDialog v-model:visible="lineageDialogVisible" :task-id="store.currentTask?.id" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDevWorkbenchStore } from '@/stores/devWorkbench'
import { devAPI } from '@/api/dev'
import TaskFolderTree from './components/TaskFolderTree.vue'
import NodePanel from './components/NodePanel.vue'
import DagCanvas from './components/DagCanvas.vue'
import NodePropertyPanel from './components/NodePropertyPanel.vue'
import LogPanel from './components/LogPanel.vue'
import StatPanel from './components/StatPanel.vue'
import ScheduleConfigDialog from './components/ScheduleConfigDialog.vue'
import PublishDialog from './components/PublishDialog.vue'
import RunParamsDialog from './components/RunParamsDialog.vue'
import VersionDialog from './components/VersionDialog.vue'
import LineageDialog from './components/LineageDialog.vue'

const router = useRouter()
const route = useRoute()
const store = useDevWorkbenchStore()

const dagCanvasRef = ref(null)
// 面板状态
const leftTab = ref('node')
const nodeSearch = ref('')
const taskSubType = ref('scheduled')

const handleTaskTypeChange = (val) => {
  if (val === 'realtime') {
    router.push('/data-pipeline')
  }
}

// 底部面板状态
const bottomCollapsed = ref(false)
const bottomPanelHeight = ref(220)
const bottomPanelHeightBeforeCollapse = ref(220)
const isResizingBottom = ref(false)

const bottomPanelStyle = computed(() => {
  if (bottomCollapsed.value) return { height: '32px' }
  return { height: bottomPanelHeight.value + 'px' }
})

const toggleBottomPanel = () => {
  if (bottomCollapsed.value) {
    bottomCollapsed.value = false
    bottomPanelHeight.value = bottomPanelHeightBeforeCollapse.value
  } else {
    bottomCollapsed.value = true
    bottomPanelHeightBeforeCollapse.value = bottomPanelHeight.value
  }
}

const startResizeBottom = (e) => {
  if (bottomCollapsed.value) return
  e.preventDefault()
  isResizingBottom.value = true
  const startY = e.clientY
  const startHeight = bottomPanelHeight.value

  const onMove = (ev) => {
    const dy = startY - ev.clientY
    const newHeight = Math.max(100, Math.min(800, startHeight + dy))
    bottomPanelHeight.value = newHeight
  }

  const onUp = () => {
    isResizingBottom.value = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  document.body.style.cursor = 'ns-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// 对话框
const runParamsDialogVisible = ref(false)
const scheduleDialogVisible = ref(false)
const publishDialogVisible = ref(false)
const versionDialogVisible = ref(false)
const lineageDialogVisible = ref(false)

// 撤销/重做栈
const undoStack = ref([])
const redoStack = ref([])
const hasUnsavedChanges = ref(false)
const clipboardNode = ref(null)

// WebSocket
const ws = ref(null)

// 计算属性
const pageTitle = computed(() => store.currentTask?.name || '未命名任务')
const statusTagType = computed(() => {
  const m = { PUBLISHED: 'success', PENDING: 'warning', UPDATED: 'info' }
  return m[store.currentTask?.publishStatus] || ''
})
const statusText = computed(() => {
  const m = { PUBLISHED: '已发布', PENDING: '待发布', UPDATED: '待更新', DRAFT: '草稿' }
  return m[store.currentTask?.publishStatus] || ''
})

// 监听选中节点 - 无节点时自动折叠右侧面板
watch(() => store.selectedNode, (node) => {
  if (!node) {
    store.propertyPanelExpanded = false
  }
})

// 保存当前状态到撤销栈
const pushUndoState = () => {
  undoStack.value.push(JSON.stringify({
    nodes: store.dagConfig.nodes,
    edges: store.dagConfig.edges
  }))
  redoStack.value = []
  hasUnsavedChanges.value = true
}

// 撤销
const undo = () => {
  if (undoStack.value.length === 0) return
  redoStack.value.push(JSON.stringify({ nodes: store.dagConfig.nodes, edges: store.dagConfig.edges }))
  const state = JSON.parse(undoStack.value.pop())
  store.setDagConfig({ ...store.dagConfig, nodes: state.nodes, edges: state.edges })
}

// 重做
const redo = () => {
  if (redoStack.value.length === 0) return
  undoStack.value.push(JSON.stringify({ nodes: store.dagConfig.nodes, edges: store.dagConfig.edges }))
  const state = JSON.parse(redoStack.value.pop())
  store.setDagConfig({ ...store.dagConfig, nodes: state.nodes, edges: state.edges })
}

// 复制/粘贴节点
const copySelected = () => {
  if (store.selectedNode) clipboardNode.value = JSON.parse(JSON.stringify(store.selectedNode))
}

const pasteNode = () => {
  if (!clipboardNode.value) return
  pushUndoState()
  const newNode = JSON.parse(JSON.stringify(clipboardNode.value))
  newNode.id = 'node_' + Date.now()
  newNode.x = (newNode.x || 100) + 40
  newNode.y = (newNode.y || 100) + 40
  store.addNode(newNode)
  dagCanvasRef.value?.addNodeAt(newNode.type, newNode.name, newNode.x, newNode.y)
}

// 键盘快捷键
const handleKeydown = (e) => {
  if (e.ctrlKey || e.metaKey) {
    if (e.key === 'z') { e.preventDefault(); undo(); return }
    if (e.key === 'y') { e.preventDefault(); redo(); return }
    if (e.key === 'c' && store.selectedNode) { e.preventDefault(); copySelected(); return }
    if (e.key === 'v' && clipboardNode.value) { e.preventDefault(); pasteNode(); return }
    if (e.key === 's') { e.preventDefault(); handleSave(); return }
  }
  if (e.key === 'Delete' && (store.selectedNode || store.selectedEdge)) {
    e.preventDefault(); deleteSelected()
  }
}

// 删除选中（canvas的doDeleteSelected会emit事件触发store清理和undo推送）
const deleteSelected = () => {
  dagCanvasRef.value?.deleteSelected()
}

// WebSocket连接
const findNodeIdByName = (name) => {
  const nodeMap = dagCanvasRef.value?.getNodeDataMap() || {}
  for (const [id, data] of Object.entries(nodeMap)) {
    if (data.name === name) return id
  }
  return null
}

const updateNodeStatusByLog = (logEntry) => {
  const canvas = dagCanvasRef.value
  if (!canvas) return
  const nodeId = logEntry.nodeId || findNodeIdByName(logEntry.nodeName)
  const status = logEntry.status || (logEntry.level === 'ERROR' ? 'FAILED' : 'RUNNING')
  if (nodeId) canvas.updateNodeStatus(nodeId, status)
}

const connectWS = (taskId) => {
  if (ws.value) ws.value.close()
  dagCanvasRef.value?.clearNodeStatuses()
  try {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const s = new WebSocket(`${protocol}//${location.host}/api/ws/sync-log`)
    s.onopen = () => s.send(JSON.stringify({ action: 'subscribe', taskId: Number(taskId) }))
    s.onmessage = (e) => {
      try {
        const m = JSON.parse(e.data)
        if (m.action === 'log') {
          store.addRunLog({ level: m.logLevel || 'INFO', nodeName: m.tableName, message: m.message, time: m.timestamp })
          updateNodeStatusByLog({ nodeName: m.tableName, status: m.logLevel === 'ERROR' ? 'FAILED' : 'RUNNING' })
        } else if (m.action === 'complete') {
          store.addRunLog({ level: m.status === 'SUCCESS' ? 'SUCCESS' : 'ERROR', message: `执行${m.status === 'SUCCESS' ? '成功' : '失败'}`, time: m.timestamp })
          // 从logs API同步最终节点状态
          fetchNodeStatuses(taskId)
          setTimeout(() => { if (ws.value) { ws.value.close(); ws.value = null } }, 3000)
        }
      } catch (_) {}
    }
    s.onerror = () => {}
    s.onclose = () => { ws.value = null }
    ws.value = s
  } catch (_) {}
}

const fetchNodeStatuses = async (taskId) => {
  try {
    const logs = await devAPI.getTaskLogs(taskId)
    if (logs?.data) {
      const statusByNode = {}
      // Use latest status per node from logs
      logs.data.forEach(entry => {
        if (entry.nodeId && entry.status) {
          statusByNode[entry.nodeId] = entry.status
        }
      })
      Object.entries(statusByNode).forEach(([id, status]) => {
        dagCanvasRef.value?.updateNodeStatus(id, status)
      })
    }
  } catch (_) {}
}

// 初始化
onMounted(async () => {
  const tid = route.params.id || route.params.taskId
  await loadFolderTree()
  if (tid && tid !== 'new') await loadTask(tid)
  else if (route.path.includes('/task/new')) createTask()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (ws.value) ws.value.close()
})

watch(() => store.currentTask, async (t) => {
  if (t?.id) await loadDagConfig(t.id)
}, { deep: true })

// 数据加载
const loadFolderTree = async () => {
  try { const r = await devAPI.getFolderTree(); store.setFolderTree(r.data || []) } catch (_) {}
}
const loadTask = async (id) => {
  try {
    const r = await devAPI.getTask(id)
    store.setCurrentTask(r.data)
    if (r.data?.dagConfig) store.setDagConfig({ nodes: r.data.dagConfig.nodes || [], edges: r.data.dagConfig.edges || [], viewport: r.data.dagConfig.viewport || { x:0,y:0,zoom:1 } })
  } catch (_) { ElMessage.error('加载任务失败') }
}
const loadDagConfig = async (taskId) => {
  try {
    const r = await devAPI.getDag(taskId)
    if (r?.data) store.setDagConfig({ nodes: r.data.nodes || [], edges: r.data.edges || [], viewport: r.data.viewport || { x:0,y:0,zoom:1 } })
  } catch (_) {}
}

// 操作方法
const goBack = () => router.push('/dev/workbench')
const toggleLeftPanel = () => store.toggleLeftPanel()
const togglePropertyPanel = () => store.togglePropertyPanel()
const expandLeft = (tab) => { leftTab.value = tab; store.setLeftPanelExpanded(true) }
const zoomIn = () => dagCanvasRef.value?.zoomIn()
const zoomOut = () => dagCanvasRef.value?.zoomOut()
const fitCanvas = () => dagCanvasRef.value?.fitContent()
const handleAutoLayout = () => dagCanvasRef.value?.autoLayout()

const handleSave = async () => {
  if (!store.currentTask?.id) { ElMessage.warning('请先选择任务'); return }
  store.setSaving(true)
  try {
    const nodeDataMap = dagCanvasRef.value?.getNodeDataMap() || {}
    const canvasNodes = Object.values(nodeDataMap)
    let canvasEdges = dagCanvasRef.value?.getEdges() || []
    if (canvasEdges.length === 0) {
      try {
        const r = await devAPI.getDag(store.currentTask.id)
        canvasEdges = r.data?.edges || []
      } catch (_) {}
    }
    await devAPI.saveDag(store.currentTask.id, { nodes: canvasNodes.length > 0 ? canvasNodes : store.dagConfig.nodes, edges: canvasEdges, viewport: store.dagConfig.viewport })
    store.setDagConfig({ nodes: canvasNodes, edges: canvasEdges, viewport: store.dagConfig.viewport })
    hasUnsavedChanges.value = false
    ElMessage.success('保存成功')
  } catch (_) { ElMessage.error('保存失败') } finally { store.setSaving(false) }
}

const handleRun = async () => {
  if (!store.currentTask?.id) { ElMessage.warning('请先选择任务'); return }
  store.setRunning(true); store.clearRunLogs(); connectWS(store.currentTask.id)
  try {
    await devAPI.runTask(store.currentTask.id)
    ElMessage.success('任务已开始运行')
    store.addRunLog({ level: 'INFO', message: '任务开始执行', time: new Date() })
  } catch (_) { ElMessage.error('运行失败'); store.setRunning(false); if (ws.value) { ws.value.close(); ws.value = null } }
}
const handleRunCommand = (cmd) => {
  if (cmd === 'params') runParamsDialogVisible.value = true
  else if (cmd === 'debug') handleRunDebug()
  else handleRun()
}
const handleRunDebug = async () => {
  store.setRunning(true); store.clearRunLogs(); connectWS(store.currentTask.id)
  try {
    await devAPI.runTask(store.currentTask.id, { debug: true })
    ElMessage.success('调试运行已启动')
  } catch (_) { ElMessage.error('调试运行失败'); store.setRunning(false) }
}
const handleRunWithParams = async (params) => {
  store.setRunning(true); store.clearRunLogs(); connectWS(store.currentTask.id)
  try {
    await devAPI.runTask(store.currentTask.id, params)
    ElMessage.success('带参数运行已启动')
    store.addRunLog({ level: 'INFO', message: '任务开始执行（带参数）', time: new Date() })
  } catch (_) { ElMessage.error('运行失败'); store.setRunning(false) }
}

const handleRestore = async () => {
  if (!store.currentTask?.id) return
  try {
    await ElMessageBox.confirm('还原到上次保存版本？未保存修改将丢失。', '提示', { type: 'warning' })
    await loadDagConfig(store.currentTask.id)
    hasUnsavedChanges.value = false
    ElMessage.success('已还原')
  } catch (_) {}
}

const handlePublish = () => {
  if (!store.currentTask?.id) { ElMessage.warning('请先选择任务'); return }
  publishDialogVisible.value = true
}
const handlePublishConfirm = async (data) => {
  try {
    await devAPI.publishTask(store.currentTask.id, data)
    ElMessage.success('发布成功')
    await loadTask(store.currentTask.id)
  } catch (_) { ElMessage.error('发布失败') }
}

const showScheduleDialog = () => { scheduleDialogVisible.value = true }
const handleScheduleConfirm = async (data) => {
  try { await devAPI.saveSchedule(store.currentTask.id, data); ElMessage.success('调度配置已保存') } catch (_) { ElMessage.error('保存失败') }
}

// 更多操作
const handleMoreCommand = async (cmd) => {
  if (!store.currentTask?.id) { ElMessage.warning('请先选择任务'); return }
  try {
    switch (cmd) {
      case 'copy': await devAPI.copyTask(store.currentTask.id); ElMessage.success('已复制'); await loadFolderTree(); break
      case 'version': versionDialogVisible.value = true; break
      case 'lineage': lineageDialogVisible.value = true; break
      case 'export': exportTaskJSON(); break
      case 'import': importTaskJSON(); break
      case 'favorite': await devAPI.favoriteTask(store.currentTask.id, !store.currentTask?.favorite); ElMessage.success(store.currentTask?.favorite ? '已取消收藏' : '已收藏'); break
      case 'rename': await handleRename(); break
      case 'delete': await handleDeleteTask(); break
    }
  } catch (_) {}
}

const exportTaskJSON = () => {
  const data = { nodes: store.dagConfig.nodes, edges: store.dagConfig.edges, viewport: store.dagConfig.viewport }
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = `${store.currentTask?.name || 'task'}.json`; a.click()
  URL.revokeObjectURL(url); ElMessage.success('导出成功')
}

const importTaskJSON = () => {
  const input = document.createElement('input'); input.type = 'file'; input.accept = '.json'
  input.onchange = async (e) => {
    try {
      const text = await e.target.files[0].text()
      const data = JSON.parse(text)
      if (data.nodes) {
        pushUndoState()
        store.setDagConfig({ nodes: data.nodes, edges: data.edges || [], viewport: data.viewport || { x:0,y:0,zoom:1 } })
        ElMessage.success('导入成功')
      }
    } catch (_) { ElMessage.error('导入失败，请检查JSON格式') }
  }
  input.click()
}

const handleRename = async () => {
  const { value } = await ElMessageBox.prompt('新名称', '重命名', {
    inputValue: store.currentTask.name, inputPattern: /^.{1,50}$/, inputErrorMessage: '1-50字符'
  })
  if (value) {
    await devAPI.updateTask(store.currentTask.id, { name: value })
    ElMessage.success('已重命名')
    await loadTask(store.currentTask.id)
  }
}

const handleDeleteTask = async () => {
  await ElMessageBox.confirm('确定删除该任务？此操作不可恢复。', '删除任务', { type: 'warning' })
  await devAPI.deleteTask(store.currentTask.id)
  ElMessage.success('已删除')
  store.setCurrentTask(null); router.push('/dev/workbench')
}

// 事件处理
const handleDragEnd = ({ node, x, y }) => {
  pushUndoState()
  if (dagCanvasRef.value) dagCanvasRef.value.addNodeAt(node.type, node.label, x, y)
}
const handleNodeSelect = (node) => store.selectNode(node)
const handleEdgeSelect = (edge) => store.selectEdge(edge)
const handleCanvasClick = () => store.clearSelection()
const handleNodeAdd = (node) => { pushUndoState(); store.addNode(node) }
const handleNodeRemove = (nodeId) => { pushUndoState(); store.removeNode(nodeId) }
const handleEdgeAdd = (edge) => { pushUndoState(); store.addEdge(edge) }
const handleEdgeRemove = (edgeId) => { pushUndoState(); store.removeEdge(edgeId) }
const handleNodeMove = (nodeId, pos) => { pushUndoState(); store.updateNode(nodeId, pos) }
const handleNodeDblClick = (node) => {
  // 双击节点时展开右侧属性面板
  store.propertyPanelExpanded = true
}
const handlePropertyUpdate = (updates) => {
  if (store.selectedNode) store.updateNode(store.selectedNode.id, updates)
  hasUnsavedChanges.value = true
}
const handleSelectTask = async (task) => {
  try {
    const r = await devAPI.getTask(task.id)
    store.setCurrentTask(r.data)
    router.push(`/dev/task/${task.id}`)
  } catch (_) { ElMessage.error('加载失败') }
}

const createFolder = async () => {
  try {
    const { value } = await ElMessageBox.prompt('文件夹名称', '新建文件夹')
    if (value) { await devAPI.createFolder({ name: value }); await loadFolderTree(); ElMessage.success('已创建') }
  } catch (_) {}
}
const createTask = async () => {
  try {
    const { value } = await ElMessageBox.prompt('任务名称', '新建任务', { inputPattern: /^.{1,50}$/, inputErrorMessage: '1-50字符' })
    if (value) {
      const r = await devAPI.createTask({ name: value })
      ElMessage.success('已创建')
      await loadFolderTree()
      store.setCurrentTask(r.data)
      router.push(`/dev/task/${r.data.id}`)
    }
  } catch (_) {}
}
</script>

<style lang="scss" scoped>
.ide-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

// 工具栏
.ide-toolbar {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  z-index: 10;

  .toolbar-section {
    display: flex;
    align-items: center;
    gap: 6px;

    &.left {
      flex: 1;
      min-width: 0;

      .task-title-area {
        display: flex; align-items: center; gap: 6px;
        min-width: 0;
        .task-icon { color: #1890ff; flex-shrink: 0; }
        .task-title {
          font-size: 14px; font-weight: 600; color: #333;
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
        }
      }
      .unsaved-dot {
        width: 8px; height: 8px; border-radius: 50%; background: #faad14;
        flex-shrink: 0;
      }

      .mode-switch {
        :deep(.el-radio-button__inner) {
          padding: 4px 10px;
        }
      }
    }

    &.center {
      :deep(.el-button) { padding: 4px 8px; }
    }

    &.right { flex-shrink: 0; }
  }
}

// 编辑主体
.ide-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// 中间区域（画布 + 底部面板）
.ide-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

// 左侧面板
.ide-left-panel {
  width: 240px;
  display: flex;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  transition: width 0.25s;
  position: relative;
  flex-shrink: 0;

  &.collapsed { width: 44px; }

  .panel-tab-bar {
    width: 44px;
    border-right: 1px solid #f0f0f0;
    display: flex;
    flex-direction: column;
    padding-top: 8px;
    flex-shrink: 0;

    .panel-tab {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;
      padding: 10px 0;
      cursor: pointer;
      color: #999;
      font-size: 11px;
      transition: all 0.2s;
      border-left: 2px solid transparent;

      &:hover { color: #1890ff; background: #f0f7ff; }
      &.active {
        color: #1890ff; background: #e6f4ff; border-left-color: #1890ff;
        font-weight: 500;
      }
    }

    &.mini .panel-tab { padding: 14px 0; }
  }

  .panel-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    min-width: 0;

    .panel-section {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .panel-search {
      padding: 8px 10px;
      border-bottom: 1px solid #f0f0f0;
      flex-shrink: 0;
    }
  }

  .panel-collapse-btn {
    position: absolute;
    right: -12px;
    top: 50%;
    transform: translateY(-50%);
    width: 12px; height: 48px;
    background: #fff;
    border: 1px solid #e8e8e8; border-left: none;
    border-radius: 0 4px 4px 0;
    display: flex; align-items: center; justify-content: center;
    cursor: pointer; color: #999;
    z-index: 10;
    &:hover { color: #1890ff; }
  }
}

// 画布
.ide-canvas {
  flex: 1;
  background: #fafafa;
  overflow: hidden;
  min-height: 200px;
}

// 底部面板
.ide-bottom-panel {
  background: #fff;
  border-top: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  transition: height 0.2s;

  &.resizing {
    transition: none;
  }

  .bottom-drag-handle {
    height: 5px;
    cursor: ns-resize;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    background: #f5f7fa;
    border-bottom: 1px solid #e8e8e8;

    &:hover {
      background: #e6f4ff;
      .drag-indicator { background: #1890ff; }
    }

    .drag-indicator {
      width: 32px;
      height: 3px;
      border-radius: 2px;
      background: #d9d9d9;
      transition: background 0.2s;
    }
  }

  .bottom-header {
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 12px;
    border-bottom: 1px solid #f0f0f0;
    flex-shrink: 0;
    background: #fafafa;

    .bottom-tabs {
      display: flex;
      gap: 16px;

      .bottom-tab {
        font-size: 12px;
        font-weight: 500;
        color: #999;
        cursor: pointer;
        padding: 4px 0;
        border-bottom: 2px solid transparent;
        transition: all 0.2s;

        &:hover { color: #555; }
        &.active {
          color: #1890ff;
          border-bottom-color: #1890ff;
        }
      }
    }

    .bottom-toggle-btn {
      color: #999;
      &:hover { color: #333; }
    }
  }

  .bottom-content {
    flex: 1;
    overflow: auto;
  }

  &.collapsed {
    .bottom-header { border-bottom: none; }
  }
}

// 右侧面板
.ide-right-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: width 0.25s;
  flex-shrink: 0;

  &.collapsed { width: 42px; }

  .panel-header {
    height: 42px;
    padding: 0 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #f0f0f0;
    flex-shrink: 0;

    &.mini {
      justify-content: center;
      padding: 0;
      height: 42px;

      .expand-btn-full {
        width: 100%;
        height: 100%;
        color: #999;
        &:hover { color: #1890ff; }
      }
    }

    .panel-node-name {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;

      .el-icon { color: #1890ff; flex-shrink: 0; }
    }
  }

  .panel-body {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
  }
}
</style>
