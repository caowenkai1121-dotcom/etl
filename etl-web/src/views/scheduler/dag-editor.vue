<template>
  <div class="dag-editor-page page-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <span class="page-title">{{ currentDagName }}</span>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="handleSave">
          <el-icon><Check /></el-icon>
          保存DAG
        </el-button>
        <el-button type="success" @click="handleExecute">
          <el-icon><CaretRight /></el-icon>
          执行DAG
        </el-button>
        <el-button @click="handleAutoLayout">
          <el-icon><Refresh /></el-icon>
          自动布局
        </el-button>
        <el-button-group>
          <el-button @click="zoomIn">+</el-button>
          <el-button @click="zoomOut">-</el-button>
        </el-button-group>
      </div>
    </div>

    <div class="dag-container">
      <!-- 左侧节点列表 -->
      <div class="node-panel">
        <div class="panel-header">
          <span class="panel-title">任务节点</span>
          <el-button size="small" @click="refreshNodeList">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
        <div class="node-list" v-loading="loadingNodes">
          <div
            v-for="node in availableNodes"
            :key="node.id"
            class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node)"
            @dragend="handleDragEnd"
          >
            <el-icon :class="getNodeIcon(node.type)">{{ getNodeIcon(node.type) }}</el-icon>
            <span class="node-name">{{ node.name }}</span>
          </div>
        </div>
      </div>

      <!-- 中间画布 -->
      <div class="canvas-container" ref="canvasContainer">
        <svg
          ref="svgCanvas"
          class="canvas"
          @mousedown="handleCanvasMouseDown"
          @mousemove="handleCanvasMouseMove"
          @mouseup="handleCanvasMouseUp"
          @mouseleave="handleCanvasMouseUp"
        >
          <!-- 连线 -->
          <g class="edges">
            <line
              v-for="edge in edges"
              :key="edge.id"
              :x1="edge.x1"
              :y1="edge.y1"
              :x2="edge.x2"
              :y2="edge.y2"
              :class="'edge ' + (selectedElement === edge ? 'selected' : '')"
              @mousedown="handleEdgeMouseDown($event, edge)"
            />
          </g>
          <!-- 节点 -->
          <g class="nodes">
            <g
              v-for="node in nodes"
              :key="node.id"
              :transform="`translate(${node.x}, ${node.y})`"
              :class="'node ' + (selectedElement === node ? 'selected' : '')"
              @mousedown="handleNodeMouseDown($event, node)"
            >
              <!-- 连接点 -->
              <circle class="connection-point top" cy="-30" r="4" @mousedown="handleConnectionMouseDown($event, node, 'top')" />
              <circle class="connection-point bottom" cy="30" r="4" @mousedown="handleConnectionMouseDown($event, node, 'bottom')" />
              <!-- 节点卡片 -->
              <rect width="180" height="60" rx="10" class="node-rect" />
              <text x="90" y="25" text-anchor="middle" class="node-text">{{ node.name }}</text>
              <text x="90" y="45" text-anchor="middle" class="node-type">{{ getNodeTypeLabel(node.type) }}</text>
            </g>
          </g>
        </svg>
      </div>

      <!-- 右侧属性面板 -->
      <div class="property-panel">
        <div class="panel-header">
          <span class="panel-title">属性编辑</span>
        </div>
        <div class="property-content">
          <div v-if="selectedElement && selectedElement.type === 'node'" class="node-properties">
            <el-form label-width="80px" size="small">
              <el-form-item label="任务名称">
                <el-input v-model="selectedNode.name" placeholder="请输入任务名称" />
              </el-form-item>
              <el-form-item label="任务类型">
                <el-select v-model="selectedNode.type" placeholder="请选择类型" style="width: 100%">
                  <el-option label="数据抽取" value="EXTRACT" />
                  <el-option label="数据转换" value="TRANSFORM" />
                  <el-option label="数据加载" value="LOAD" />
                  <el-option label="发送邮件" value="EMAIL" />
                  <el-option label="HTTP请求" value="HTTP" />
                </el-select>
              </el-form-item>
              <el-form-item label="触发条件">
                <el-checkbox-group v-model="selectedNode.conditions">
                  <el-checkbox label="成功时触发">SUCCESS</el-checkbox>
                  <el-checkbox label="失败时触发">FAILED</el-checkbox>
                  <el-checkbox label="超时时触发">TIMEOUT</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="超时时间">
                <el-input-number v-model="selectedNode.timeout" :min="60" :max="3600" style="width: 100%" />
                <span style="margin-left: 8px; color: var(--text-muted)">秒</span>
              </el-form-item>
              <el-form-item label="操作">
                <el-button type="danger" size="small" @click="handleDeleteNode">删除节点</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="selectedElement && selectedElement.type === 'edge'" class="edge-properties">
            <el-form label-width="80px" size="small">
              <el-form-item label="触发条件">
                <el-radio-group v-model="selectedEdge.condition">
                  <el-radio label="SUCCESS">成功时</el-radio>
                  <el-radio label="FAILED">失败时</el-radio>
                  <el-radio label="ANY">任何情况</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="操作">
                <el-button type="danger" size="small" @click="handleDeleteEdge">删除连线</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="!selectedElement" class="no-selection">
            <el-empty description="请选择一个节点或连线" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, CaretRight, Refresh } from '@element-plus/icons-vue'
import { getTaskPage } from '@/api'

const router = useRouter()
const route = useRoute()

const canvasContainer = ref(null)
const svgCanvas = ref(null)
const loadingNodes = ref(false)
const availableNodes = ref([])
const currentDagName = ref('DAG编排')
const selectedElement = ref(null)

// DAG数据
const nodes = ref([])
const edges = ref([])

// 拖拽状态
const isDraggingNode = ref(false)
const isDraggingEdge = ref(false)
const currentNode = ref(null)
const startPos = ref({ x: 0, y: 0 })
const tempEdge = ref(null)

// 选中的节点/边属性
const selectedNode = ref({
  id: null,
  name: '',
  type: 'EXTRACT',
  conditions: ['SUCCESS'],
  timeout: 300
})
const selectedEdge = ref({
  id: null,
  condition: 'SUCCESS'
})

// 缩放
const scale = ref(1)

// 初始化
onMounted(() => {
  fetchAvailableNodes()
  // 初始化一些示例节点
  initSampleNodes()
})

// 获取可用任务节点
const fetchAvailableNodes = async () => {
  loadingNodes.value = true
  try {
    const res = await getTaskPage({ pageNum: 1, pageSize: 100 })
    availableNodes.value = (res.data?.list || []).map(task => ({
      id: `task_${task.id}`,
      name: task.name,
      type: task.syncMode === 'CDC' ? 'STREAM' : 'BATCH'
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loadingNodes.value = false
  }
}

const refreshNodeList = () => {
  fetchAvailableNodes()
}

// 初始化示例节点
const initSampleNodes = () => {
  const sampleNodes = [
    { id: '1', name: '数据抽取', type: 'EXTRACT', x: 200, y: 100, conditions: ['SUCCESS'], timeout: 300 },
    { id: '2', name: '数据转换', type: 'TRANSFORM', x: 400, y: 100, conditions: ['SUCCESS'], timeout: 600 },
    { id: '3', name: '数据加载', type: 'LOAD', x: 600, y: 100, conditions: ['SUCCESS'], timeout: 300 }
  ]
  const sampleEdges = [
    { id: 'e1', x1: 290, y1: 130, x2: 310, y2: 70, condition: 'SUCCESS' }
  ]
  nodes.value = sampleNodes
  edges.value = sampleEdges
}

// 返回列表
const goBack = () => {
  router.push('/scheduler')
}

// 保存DAG
const handleSave = async () => {
  try {
    await ElMessageBox.confirm('确定保存当前DAG？', '提示', { type: 'info' })
    // 这里应该调用实际API
    ElMessage.success('保存成功')
  } catch (e) {}
}

// 执行DAG
const handleExecute = async () => {
  try {
    await ElMessageBox.confirm('确定执行当前DAG？', '提示', { type: 'info' })
    // 这里应该调用实际API
    ElMessage.success('DAG执行已开始')
  } catch (e) {}
}

// 自动布局
const handleAutoLayout = () => {
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  const nodeWidth = 200
  const nodeHeight = 100
  const colCount = 3
  const colWidth = width / colCount
  const rowHeight = 150

  nodes.value.forEach((node, index) => {
    const col = index % colCount
    const row = Math.floor(index / colCount)
    node.x = col * colWidth + colWidth / 2 - nodeWidth / 2
    node.y = row * rowHeight + 50
  })
  ElMessage.success('布局完成')
}

// 缩放
const zoomIn = () => {
  scale.value = Math.min(2, scale.value + 0.1)
}

const zoomOut = () => {
  scale.value = Math.max(0.5, scale.value - 0.1)
}

// 拖拽节点开始
const handleDragStart = (e, node) => {
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/vueflow', JSON.stringify({ type: 'node', data: node }))
}

const handleDragEnd = () => {
  isDraggingNode.value = false
}

// 画布事件
const handleCanvasMouseDown = (e) => {
  if (e.target === svgCanvas.value) {
    selectedElement.value = null
  }
}

const handleCanvasMouseMove = (e) => {
  if (isDraggingNode && currentNode.value) {
    const rect = canvasContainer.value.getBoundingClientRect()
    currentNode.value.x = e.clientX - rect.left
    currentNode.value.y = e.clientY - rect.top
  } else if (isDraggingEdge && tempEdge.value) {
    const rect = canvasContainer.value.getBoundingClientRect()
    tempEdge.value.x2 = e.clientX - rect.left
    tempEdge.value.y2 = e.clientY - rect.top
  }
}

const handleCanvasMouseUp = (e) => {
  if (isDraggingEdge && tempEdge.value) {
    const targetNode = findNodeAtPosition(e.clientX, e.clientY)
    if (targetNode && targetNode !== currentNode.value) {
      edges.value.push({
        id: `e${Date.now()}`,
        x1: currentNode.value.x + 90,
        y1: currentNode.value.y + 60,
        x2: targetNode.x + 90,
        y2: targetNode.y,
        condition: 'SUCCESS'
      })
    }
    tempEdge.value = null
    isDraggingEdge.value = false
  }

  isDraggingNode.value = false
}

// 节点事件
const handleNodeMouseDown = (e, node) => {
  e.stopPropagation()
  selectedElement.value = node
  selectedNode.value = { ...node }
  isDraggingNode.value = true
  currentNode.value = node
  startPos.value = { x: e.clientX - node.x, y: e.clientY - node.y }
}

// 连接点事件
const handleConnectionMouseDown = (e, node, point) => {
  e.stopPropagation()
  selectedElement.value = null
  isDraggingEdge.value = true
  currentNode.value = node

  const startX = node.x + 90
  const startY = point === 'top' ? node.y : node.y + 60

  tempEdge.value = {
    id: 'temp',
    x1: startX,
    y1: startY,
    x2: startX,
    y2: startY
  }
  edges.value.push(tempEdge.value)
}

// 连线事件
const handleEdgeMouseDown = (e, edge) => {
  e.stopPropagation()
  selectedElement.value = edge
  selectedEdge.value = { ...edge }
}

// 查找节点
const findNodeAtPosition = (x, y) => {
  const rect = canvasContainer.value.getBoundingClientRect()
  return nodes.value.find(node => {
    const nx = node.x
    const ny = node.y
    return x >= rect.left + nx &&
           x <= rect.left + nx + 180 &&
           y >= rect.top + ny &&
           y <= rect.top + ny + 60
  })
}

// 删除节点
const handleDeleteNode = async () => {
  if (!selectedNode.value.id) return
  try {
    await ElMessageBox.confirm('确定删除该节点？', '提示', { type: 'warning' })
    const index = nodes.value.findIndex(n => n.id === selectedNode.value.id)
    if (index !== -1) {
      nodes.value.splice(index, 1)
    }
    selectedElement.value = null
    ElMessage.success('删除成功')
  } catch (e) {}
}

// 删除连线
const handleDeleteEdge = async () => {
  if (!selectedEdge.value.id) return
  try {
    await ElMessageBox.confirm('确定删除该连线？', '提示', { type: 'warning' })
    const index = edges.value.findIndex(e => e.id === selectedEdge.value.id)
    if (index !== -1) {
      edges.value.splice(index, 1)
    }
    selectedElement.value = null
    ElMessage.success('删除成功')
  } catch (e) {}
}

// 节点类型标签
const getNodeTypeLabel = (type) => {
  const labels = { EXTRACT: '数据抽取', TRANSFORM: '数据转换', LOAD: '数据加载', EMAIL: '发送邮件', HTTP: 'HTTP请求' }
  return labels[type] || '未知'
}

// 节点图标
const getNodeIcon = (type) => {
  // 这里简化处理，实际项目中可以使用Element Plus的图标
  return type === 'EXTRACT' ? 'Document' : type === 'TRANSFORM' ? 'Refresh' : 'Upload'
}
</script>

<style lang="scss" scoped>
.dag-editor-page {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #ffffff;
    border: 1px solid var(--border-color);
    border-radius: 12px;
    margin-bottom: 16px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .page-title {
        font-size: 18px;
        font-weight: 600;
        color: var(--text-primary);
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .dag-container {
    display: flex;
    gap: 8px;
    height: calc(100vh - 180px);

    .node-panel {
      width: 200px;
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      display: flex;
      flex-direction: column;

      .panel-header {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border-color);
        display: flex;
        justify-content: space-between;
        align-items: center;

        .panel-title {
          font-weight: 600;
          color: var(--text-primary);
        }
      }

      .node-list {
        flex: 1;
        padding: 12px;
        overflow-y: auto;
        gap: 8px;

        .node-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px;
          background: rgba(79, 110, 247, 0.05);
          border: 1px solid var(--border-color);
          border-radius: 8px;
          cursor: grab;
          transition: all 0.2s;
          margin-bottom: 8px;

          &:hover {
            border-color: var(--primary-color);
            background: rgba(79, 110, 247, 0.1);
          }

          &:active {
            cursor: grabbing;
          }

          .node-name {
            font-size: 14px;
            color: var(--text-primary);
          }
        }
      }
    }

    .canvas-container {
      flex: 1;
      background: #f8f9fa;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      overflow: hidden;
      position: relative;

      .canvas {
        width: 100%;
        height: 100%;
        background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
        background-size: 20px 20px;

        .edges {
          .edge {
            fill: none;
            stroke: #94a3b8;
            stroke-width: 2;
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
              stroke: var(--primary-color);
              stroke-width: 3;
            }

            &.selected {
              stroke: var(--primary-color);
              stroke-width: 3;
            }
          }
        }

        .nodes {
          .node {
            cursor: grab;

            &.selected {
              .node-rect {
                fill: rgba(79, 110, 247, 0.2);
                stroke: var(--primary-color);
              }
            }

            .connection-point {
              fill: #64748b;
              cursor: crosshair;
              transition: all 0.2s;

              &:hover {
                fill: var(--primary-color);
                r: 6;
              }
            }

            .node-rect {
              fill: #ffffff;
              stroke: #e2e8f0;
              stroke-width: 1;
              transition: all 0.2s;
            }

            .node-text {
              fill: var(--text-primary);
              font-size: 14px;
              font-weight: 600;
            }

            .node-type {
              fill: var(--text-muted);
              font-size: 12px;
            }
          }
        }
      }
    }

    .property-panel {
      width: 280px;
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      display: flex;
      flex-direction: column;

      .panel-header {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border-color);
        display: flex;
        justify-content: space-between;
        align-items: center;

        .panel-title {
          font-weight: 600;
          color: var(--text-primary);
        }
      }

      .property-content {
        flex: 1;
        padding: 16px;
        overflow-y: auto;

        .node-properties, .edge-properties {
          .no-selection {
            padding: 40px 0;
          }
        }
      }
    }
  }
}
</style>
