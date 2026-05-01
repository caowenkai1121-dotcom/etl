<template>
  <div class="dag-canvas" ref="containerRef"
    @drop.prevent="handleDrop"
    @dragover.prevent="onDragOver"
    @dragenter="onDragEnter"
    @dragleave="onDragLeave"
    @mousedown="onCanvasMouseDown"
    @mousemove="onCanvasMouseMove"
    @mouseup="onCanvasMouseUp"
    @dblclick="onCanvasDblClick"
    @keydown="onKeyDown"
    @contextmenu.prevent="onContextMenu"
    tabindex="0"
  >
    <!-- 拖入提示 -->
    <div class="drop-zone-hint" v-if="isDragOver">
      <span>释放以添加节点</span>
    </div>

    <!-- 遮罩层用于连线拖拽 -->
    <svg class="drag-line-overlay" v-if="connecting" ref="overlaySvgRef">
      <path :d="connectingPath" fill="none" stroke="#1890ff" stroke-width="2" stroke-dasharray="6,3" />
    </svg>

    <!-- 缩放控件 -->
    <div class="zoom-controls">
      <button class="zoom-btn" title="放大" @click="zoomIn">+</button>
      <span class="zoom-value">{{ Math.round(currentZoom * 100) }}%</span>
      <button class="zoom-btn" title="缩小" @click="zoomOut">−</button>
      <button class="zoom-btn zoom-fit" title="适应画布" @click="fitContent">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/>
        </svg>
      </button>
    </div>

    <!-- 缩略图导航器 -->
    <div class="minimap-container" v-if="showMinimap" @mousedown.prevent="onMinimapMouseDown">
      <svg class="minimap-svg" ref="minimapSvgRef" :width="minimapWidth" :height="minimapHeight">
        <rect width="100%" height="100%" fill="#fafafa" stroke="#e0e0e0" rx="2" />
        <!-- 节点缩略 -->
        <rect v-for="n in minimapNodes" :key="n.id"
          :x="n.mx" :y="n.my" :width="n.mw" :height="n.mh"
          :fill="n.color" opacity="0.7" rx="1" />
        <!-- 视口矩形 -->
        <rect :x="viewportRect.x" :y="viewportRect.y"
          :width="viewportRect.w" :height="viewportRect.h"
          fill="none" stroke="#1890ff" stroke-width="1.5" rx="1" />
      </svg>
    </div>
    <button class="minimap-toggle" :class="{ active: showMinimap }" title="缩略图" @click="showMinimap = !showMinimap">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="18" height="18" rx="2"/>
        <path d="M3 9h18M9 3v18"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  zoom: { type: Number, default: 1 },
  editable: { type: Boolean, default: true },
  taskId: { type: [String, Number], default: null }
})

const emit = defineEmits([
  'node-select','edge-select','node-add','node-remove',
  'edge-add','edge-remove','node-move','canvas-click',
  'node-dblclick','canvas-contextmenu','update:nodes','update:edges'
])

const containerRef = ref(null)
const overlaySvgRef = ref(null)
const isDragOver = ref(false)
const currentZoom = ref(1)

// 节点数据存储
const nodeDataMap = new Map()
const edgeDataList = []
let nodeIdCounter = 0
const SVG_NS = 'http://www.w3.org/2000/svg'

// 连线拖拽状态
const connecting = ref(false)
const connectingPath = ref('')
let connectSource = null  // { nodeId, portType, nodeEl, x, y }
let connectSnapTarget = null  // 当前吸附高亮的端口

const SNAP_DISTANCE = 30  // 吸附距离(px)
const GRID_SIZE = 20      // 网格大小(px)，用于节点吸附

// 节点拖拽状态
let dragState = null
let globalMouseUpHandler = null
const DTH = 4

// 选中状态
let selectedNodeId = null
let selectedEdgeId = null

// 节点运行状态可视化
const nodeStatusMap = new Map()
const STATUS_COLORS = {
  RUNNING: '#1890ff', SUCCESS: '#52c41a', FAILED: '#ff4d4f',
  SKIPPED: '#faad14', PENDING: '#d9d9d9'
}

const nextNodeId = () => `node-${Date.now()}-${++nodeIdCounter}`

// 连线类型: bezier | step
const connectorType = ref('bezier')

// 缩略图导航器
const showMinimap = ref(false)
const minimapSvgRef = ref(null)
const minimapWidth = 160
const minimapHeight = 110
const MAP_MARGIN = 40

const minimapNodes = computed(() => {
  const nodes = []
  nodeDataMap.forEach((nd, id) => {
    nodes.push({ id, x: nd.x, y: nd.y, color: nodeTypeColors[nd.type] || '#1890ff' })
  })
  if (nodes.length === 0) return []
  let mnx = Infinity, mny = Infinity, mxx = -Infinity, mxy = -Infinity
  nodes.forEach(n => { if (n.x < mnx) mnx = n.x; if (n.y < mny) mny = n.y; if (n.x + 200 > mxx) mxx = n.x + 200; if (n.y + 56 > mxy) mxy = n.y + 56 })
  const tw = mxx - mnx + MAP_MARGIN * 2, th = mxy - mny + MAP_MARGIN * 2
  const sx = (minimapWidth - 8) / Math.max(tw, 1), sy = (minimapHeight - 8) / Math.max(th, 1)
  const scale = Math.min(sx, sy)
  const ox = 4, oy = 4
  return nodes.map(n => ({
    id: n.id,
    mx: ox + (n.x - mnx + MAP_MARGIN) * scale,
    my: oy + (n.y - mny + MAP_MARGIN) * scale,
    mw: Math.max(200 * scale, 2),
    mh: Math.max(56 * scale, 1),
    color: n.color
  }))
})

const viewportRect = computed(() => {
  const rect = containerRef.value?.getBoundingClientRect()
  if (!rect || minimapNodes.value.length === 0) return { x: 0, y: 0, w: 0, h: 0 }
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  const z = currentZoom.value
  // viewport in canvas coordinates
  const vx = -parseFloat(stage?.getAttribute('data-pan-x') || '0') / z || 0
  const vy = -parseFloat(stage?.getAttribute('data-pan-y') || '0') / z || 0
  const vw = rect.width / z
  const vh = rect.height / z
  // same scale calculation as minimapNodes
  let mnx = Infinity, mny = Infinity, mxx = -Infinity, mxy = -Infinity
  nodeDataMap.forEach(nd => { if (nd.x < mnx) mnx = nd.x; if (nd.y < mny) mny = nd.y; if (nd.x + 200 > mxx) mxx = nd.x + 200; if (nd.y + 56 > mxy) mxy = nd.y + 56 })
  if (!isFinite(mnx)) return { x: 0, y: 0, w: 0, h: 0 }
  const tw = mxx - mnx + MAP_MARGIN * 2, th = mxy - mny + MAP_MARGIN * 2
  const sx = (minimapWidth - 8) / Math.max(tw, 1), sy = (minimapHeight - 8) / Math.max(th, 1)
  const scale = Math.min(sx, sy)
  return {
    x: 4 + (vx - mnx + MAP_MARGIN) * scale,
    y: 4 + (vy - mny + MAP_MARGIN) * scale,
    w: Math.max(vw * scale, 8),
    h: Math.max(vh * scale, 6)
  }
})

const onMinimapMouseDown = (e) => {
  if (!containerRef.value || minimapNodes.value.length === 0) return
  const svgRect = minimapSvgRef.value?.getBoundingClientRect()
  if (!svgRect) return
  const mx = e.clientX - svgRect.left
  const my = e.clientY - svgRect.top
  // reverse calculate target canvas position
  let mnx = Infinity, mny = Infinity, mxx = -Infinity, mxy = -Infinity
  nodeDataMap.forEach(nd => { if (nd.x < mnx) mnx = nd.x; if (nd.y < mny) mny = nd.y; if (nd.x + 200 > mxx) mxx = nd.x + 200; if (nd.y + 56 > mxy) mxy = nd.y + 56 })
  const tw = mxx - mnx + MAP_MARGIN * 2, th = mxy - mny + MAP_MARGIN * 2
  const sx = (minimapWidth - 8) / Math.max(tw, 1), sy = (minimapHeight - 8) / Math.max(th, 1)
  const scale = Math.min(sx, sy)
  const cx = (mx - 4) / scale + mnx - MAP_MARGIN
  const cy = (my - 4) / scale + mny - MAP_MARGIN
  // scroll canvas to center on this point
  const rect = containerRef.value.getBoundingClientRect()
  const targetX = cx - rect.width / (2 * currentZoom.value)
  const targetY = cy - rect.height / (2 * currentZoom.value)
  const stage = containerRef.value.querySelector('.x6-graph-svg-stage')
  if (stage) {
    stage.setAttribute('data-pan-x', String(-targetX * currentZoom.value))
    stage.setAttribute('data-pan-y', String(-targetY * currentZoom.value))
    stage.style.transform = `translate(${-targetX * currentZoom.value}px,${-targetY * currentZoom.value}px) scale(${currentZoom.value})`
  }
}

const computeEdgePath = (sx, sy, tx, ty, type = 'bezier') => {
  if (type === 'step') {
    const mx = (sx + tx) / 2
    return `M ${sx} ${sy} L ${mx} ${sy} L ${mx} ${ty} L ${tx} ${ty}`
  }
  const dx = Math.abs(tx - sx) * 0.5
  return `M ${sx} ${sy} C ${sx + dx} ${sy}, ${tx - dx} ${ty}, ${tx} ${ty}`
}

// FineDataLink 节点颜色和图标
const nodeTypeColors = {
  DATA_SYNC:'#1890ff', DATA_TRANSFORM:'#13c2c2', FILE_TRANSFER:'#52c41a',
  SQL_SCRIPT:'#1890ff', SHELL_SCRIPT:'#722ed1', BAT_SCRIPT:'#faad14', PYTHON_SCRIPT:'#52c41a',
  PARAM_ASSIGN:'#722ed1', CONDITION:'#faad14', CALL_TASK:'#13c2c2',
  LOOP_CONTAINER:'#1890ff', MESSAGE_NOTIFY:'#eb2f96', VIRTUAL_NODE:'#8c8c8c',
  NOTE:'#faad14'
}

const nodeTypeIcons = {
  DATA_SYNC: 'M4 4h16v4H4zm0 6h16v10H4z',
  DATA_TRANSFORM: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
  FILE_TRANSFER: 'M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z',
  SQL_SCRIPT: 'M4 4h16v4H4zm0 6h16v10H4z',
  SHELL_SCRIPT: 'M4 6h16M4 10h16M4 14h16',
  BAT_SCRIPT: 'M4 6h16M4 10h16M4 14h16',
  PYTHON_SCRIPT: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z',
  PARAM_ASSIGN: 'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z',
  CONDITION: 'M12 2L2 22h20L12 2zm0 4l7 14H5l7-14z',
  CALL_TASK: 'M10 9v6l5-3-5-3zM4 4h16v12H4z',
  LOOP_CONTAINER: 'M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z',
  MESSAGE_NOTIFY: 'M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z',
  VIRTUAL_NODE: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z',
  NOTE: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 3v5h5M16 13H8M16 17H8M10 9H8'
}

// 拖放辅助
const onDragOver = (e) => { isDragOver.value = true }
const onDragEnter = (e) => { isDragOver.value = true }
const onDragLeave = (e) => {
  if (!e.currentTarget.contains(e.relatedTarget)) isDragOver.value = false
}

// 画布鼠标事件
const onCanvasMouseDown = (e) => {
  if (e.button !== 0) return
  const target = e.target

  // 检查是否点击了端口
  const portEl = target.closest('.dag-port')
  if (portEl && props.editable) {
    startConnecting(e, portEl)
    return
  }

  // 检查是否点击了连线
  const edgeEl = target.closest('.dag-edge-group') || target.closest('.dag-edge')
  if (edgeEl) {
    selectEdge(edgeEl)
    return
  }

  // 检查是否点击了节点
  const nodeEl = target.closest('.dag-node')
  if (nodeEl) {
    startNodeDrag(e, nodeEl)
    return
  }

  // 点击空白区域
  clearSelection()
  emit('canvas-click')
}

const onCanvasMouseMove = (e) => {
  if (connecting.value) {
    updateConnectingLine(e)
    return
  }
  if (dragState) {
    updateNodeDrag(e)
  }
}

const onCanvasMouseUp = (e) => {
  if (connecting.value) {
    finishConnecting(e)
    return
  }
  if (dragState) {
    finishNodeDrag(e)
  }
}

// 双击处理
const onCanvasDblClick = (e) => {
  const nodeEl = e.target.closest('.dag-node')
  if (nodeEl) {
    const nodeId = nodeEl.getAttribute('data-cell-id')
    if (nodeId) {
      selectNode(nodeId, nodeEl)
      const nd = nodeDataMap.get(nodeId)
      if (nd) emit('node-dblclick', nd)
    }
  }
}

// 键盘删除
const onKeyDown = (e) => {
  if (e.key === 'Delete' || e.key === 'Backspace') {
    if (props.editable && (selectedNodeId || selectedEdgeId)) {
      doDeleteSelected()
    }
  }
}

// 右键菜单
const onContextMenu = (e) => {
  const rect = containerRef.value.getBoundingClientRect()
  emit('canvas-contextmenu', {
    x: (e.clientX - rect.left) / currentZoom.value,
    y: (e.clientY - rect.top) / currentZoom.value
  })
}

// ========== 连线系统 (端口拖拽) ==========

const startConnecting = (e, portEl) => {
  const nodeEl = portEl.closest('.dag-node')
  const nodeId = nodeEl.getAttribute('data-cell-id')
  const portType = portEl.getAttribute('data-port')

  // 只能从output端口连线到input端口
  if (!portType || !portType.startsWith('output-')) return

  // 清理上次残留的吸附高亮
  if (connectSnapTarget) {
    connectSnapTarget.setAttribute('r', String(PORT_R))
    connectSnapTarget.style.fill = '#fff'
    connectSnapTarget = null
  }

  connecting.value = true
  connectSource = {
    nodeId,
    portType,
    nodeEl,
    x: e.clientX,
    y: e.clientY
  }
  updateConnectingLine(e)
}

const findNearestInputPort = (cx, cy) => {
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  if (!stage) return null
  let best = null, bestDist = SNAP_DISTANCE
  stage.querySelectorAll('.dag-port[data-port^="input-"]').forEach(p => {
    const n = p.closest('.dag-node')
    if (!n || n === connectSource?.nodeEl) return
    const pr = p.getBoundingClientRect()
    const px = pr.left + pr.width / 2
    const py = pr.top + pr.height / 2
    const d = Math.hypot(cx - px, cy - py)
    if (d < bestDist) { bestDist = d; best = p }
  })
  return best
}

const updateConnectingLine = (e) => {
  if (!connectSource) return
  const rect = containerRef.value.getBoundingClientRect()
  const srcCenter = getPortCenter(connectSource.nodeEl, connectSource.portType)
  let tx = (e.clientX - rect.left) / currentZoom.value
  let ty = (e.clientY - rect.top) / currentZoom.value

  // 吸附到最近端口
  const snapPort = findNearestInputPort(e.clientX, e.clientY)
  if (snapPort !== connectSnapTarget) {
    // 移除旧高亮
    if (connectSnapTarget) {
      connectSnapTarget.setAttribute('r', String(PORT_R))
      connectSnapTarget.style.fill = '#fff'
    }
    connectSnapTarget = snapPort
    // 添加新高亮
    if (connectSnapTarget) {
      connectSnapTarget.setAttribute('r', String(PORT_R + 3))
      connectSnapTarget.style.fill = '#1890ff'
      const pr = snapPort.getBoundingClientRect()
      tx = (pr.left + pr.width / 2 - rect.left) / currentZoom.value
      ty = (pr.top + pr.height / 2 - rect.top) / currentZoom.value
    }
  }

  connectingPath.value = `M ${srcCenter.x} ${srcCenter.y} C ${srcCenter.x + 50} ${srcCenter.y}, ${tx - 50} ${ty}, ${tx} ${ty}`
}

const finishConnecting = (e) => {
  if (!connectSource) { connecting.value = false; return }

  // 清理吸附高亮
  if (connectSnapTarget) {
    connectSnapTarget.setAttribute('r', String(PORT_R))
    connectSnapTarget.style.fill = '#fff'
    connectSnapTarget = null
  }

  // 查找鼠标下的端口
  const target = document.elementFromPoint(e.clientX, e.clientY)
  let portEl = target?.closest('.dag-port')
  let nodeEl = portEl?.closest('.dag-node')

  // 容差回退：SNAP_DISTANCE内搜索最近的输入端口
  if (!portEl || !nodeEl) {
    const nearest = findNearestInputPort(e.clientX, e.clientY)
    if (nearest) {
      portEl = nearest
      nodeEl = nearest.closest('.dag-node')
    }
  }

  if (portEl && nodeEl) {
    const targetPortType = portEl.getAttribute('data-port')
    if (targetPortType && targetPortType.startsWith('input-')) {
      const targetId = nodeEl.getAttribute('data-cell-id')
      if (targetId !== connectSource.nodeId) {
        // 检查是否已存在相同连线
        const exists = edgeDataList.some(
          ed => ed.source === connectSource.nodeId && ed.target === targetId
        )
        if (!exists) {
          const edgeId = `edge-${Date.now()}`
          const edgeData = {
            id: edgeId,
            source: connectSource.nodeId,
            target: targetId,
            sourcePort: connectSource.portType,
            targetPort: targetPortType,
            condition: 'SUCCESS'
          }
          edgeDataList.push(edgeData)
          drawEdge(connectSource.nodeEl, nodeEl, edgeId, connectSource.portType, targetPortType)
          emit('edge-add', edgeData)
        }
      }
    }
  }

  connecting.value = false
  connectSource = null
  connectingPath.value = ''
}

// ========== 节点拖拽 ==========

const startNodeDrag = (e, nodeEl) => {
  const id = nodeEl.getAttribute('data-cell-id')
  if (!id) return

  const tr = nodeEl.getAttribute('transform') || 'translate(0,0)'
  const m = tr.match(/translate\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)/)
  nodeEl.classList.add('dragging')
  dragState = {
    node: nodeEl,
    id,
    sx: e.clientX,
    sy: e.clientY,
    ox: m ? +m[1] : 0,
    oy: m ? +m[2] : 0,
    moved: false
  }
}

const snapToGrid = (val) => Math.round(val / GRID_SIZE) * GRID_SIZE

const updateNodeDrag = (e) => {
  if (!dragState) return
  const dx = e.clientX - dragState.sx
  const dy = e.clientY - dragState.sy
  if (Math.abs(dx) < DTH && Math.abs(dy) < DTH) return

  dragState.moved = true
  const nx = snapToGrid(dragState.ox + dx)
  const ny = snapToGrid(dragState.oy + dy)
  dragState.node.setAttribute('transform', `translate(${nx},${ny})`)
  updateEdgesForNode(dragState.id)

  // 更新连接线
  if (connecting.value && connectSource) {
    updateConnectingLine(e)
  }
}

const finishNodeDrag = (e) => {
  if (!dragState) return
  const { node, id, moved } = dragState

  node.classList.remove('dragging')

  if (moved) {
    const tr = node.getAttribute('transform') || ''
    const m = tr.match(/translate\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)/)
    if (m) {
      const sx = snapToGrid(+m[1]), sy = snapToGrid(+m[2])
      node.setAttribute('transform', `translate(${sx},${sy})`)
      const nd = nodeDataMap.get(id)
      if (nd) { nd.x = sx; nd.y = sy }
      emit('node-move', id, { x: sx, y: sy })
      nextTick(() => updateEdgesForNode(id))
    }
  } else {
    // 单击选中节点
    selectNode(id, node)
  }

  dragState = null
}

// ========== 节点渲染 ==========

const W = 200, H = 56
const PORT_R = 6

const renderNodeToStage = (id, type, label, x, y, color, config = {}) => {
  const nodeData = { id, type, name: label, x, y, config: config || {} }
  nodeDataMap.set(id, nodeData)

  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  if (!stage) return

  const g = document.createElementNS(SVG_NS, 'g')
  g.setAttribute('data-cell-id', id)
  g.setAttribute('class', 'dag-node')
  g.setAttribute('transform', `translate(${x},${y})`)
  g.style.cursor = 'move'

  // 阴影
  const sh = crEl('rect', {
    class: 'node-shadow', x: -2, y: -2, width: W + 4, height: H + 4, rx: 6,
    fill: 'transparent', stroke: 'transparent', 'stroke-width': '0', 'pointer-events': 'none'
  })
  g.appendChild(sh)

  // 主体 - FineDataLink风格：圆角矩形白色背景
  g.appendChild(crEl('rect', {
    class: 'node-body', width: W, height: H,
    fill: '#fff', stroke: '#d9d9d9', 'stroke-width': '1.5', rx: 6,
    'pointer-events': 'none'
  }))

  // 左侧颜色条 - FineDataLink风格：左侧4px宽色条
  g.appendChild(crEl('rect', {
    class: 'node-color-bar', x: 0, y: 0, width: 4, height: H,
    fill: color || '#1890ff', rx: 2, 'pointer-events': 'none'
  }))

  // 图标
  const IS = 18, IX = 14, IY = 16
  g.appendChild(crEl('circle', {
    cx: IX + IS / 2, cy: IY + IS / 2, r: IS / 2 + 2,
    fill: (color || '#1890ff') + '15', 'pointer-events': 'none'
  }))
  const ip = crEl('path', {
    d: nodeTypeIcons[type] || nodeTypeIcons.DATA_SYNC,
    transform: `translate(${IX + 2},${IY + 2}) scale(0.75)`,
    fill: color || '#1890ff', 'pointer-events': 'none'
  })
  g.appendChild(ip)

  // 文字 - 允许更长节点名称
  const displayLabel = (label || type || 'Node')
  const txt = crEl('text', {
    x: IX + IS + 14, y: H / 2 + 1, dy: '0.35em',
    'font-size': '12', 'font-weight': '500', fill: '#333', 'pointer-events': 'none'
  })
  // 截断过长文本适配200px宽度
  txt.textContent = displayLabel.length > 22 ? displayLabel.substring(0, 22) + '..' : displayLabel
  // 完整名称作为tooltip
  const title = crEl('title', {})
  title.textContent = displayLabel
  txt.appendChild(title)
  g.appendChild(txt)

  // 透明点击区域
  g.appendChild(crEl('rect', {
    width: W, height: H, fill: 'transparent', rx: 6, style: 'cursor:move'
  }))

  // 输入端口-左
  const inLeft = crEl('circle', {
    class: 'dag-port', 'data-port': 'input-left', 'data-node-id': id,
    cx: 0, cy: H / 2, r: PORT_R,
    fill: '#fff', stroke: color || '#1890ff', 'stroke-width': '2',
    style: 'cursor:crosshair'
  })
  g.appendChild(inLeft)

  // 输入端口-上
  const inTop = crEl('circle', {
    class: 'dag-port', 'data-port': 'input-top', 'data-node-id': id,
    cx: W / 2, cy: 0, r: PORT_R,
    fill: '#fff', stroke: color || '#1890ff', 'stroke-width': '2',
    style: 'cursor:crosshair'
  })
  g.appendChild(inTop)

  // 输出端口-右
  const outRight = crEl('circle', {
    class: 'dag-port', 'data-port': 'output-right', 'data-node-id': id,
    cx: W, cy: H / 2, r: PORT_R,
    fill: color || '#1890ff', stroke: color || '#1890ff', 'stroke-width': '2',
    style: 'cursor:grab'
  })
  g.appendChild(outRight)

  // 输出端口-下
  const outBottom = crEl('circle', {
    class: 'dag-port', 'data-port': 'output-bottom', 'data-node-id': id,
    cx: W / 2, cy: H, r: PORT_R,
    fill: color || '#1890ff', stroke: color || '#1890ff', 'stroke-width': '2',
    style: 'cursor:grab'
  })
  g.appendChild(outBottom)

  stage.appendChild(g)
}

const crEl = (tag, attrs) => {
  const e = document.createElementNS(SVG_NS, tag)
  Object.entries(attrs).forEach(([k, v]) => e.setAttribute(k, String(v)))
  return e
}

// ========== 端口位置计算 ==========

const getPortCenter = (nodeEl, portType) => {
  const tr = nodeEl.getAttribute('transform') || 'translate(0,0)'
  const m = tr.match(/translate\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)/)
  const nx = m ? +m[1] : 0
  const ny = m ? +m[2] : 0

  switch (portType) {
    case 'output-right': return { x: nx + W, y: ny + H / 2 }
    case 'output-bottom': return { x: nx + W / 2, y: ny + H }
    case 'input-left': return { x: nx, y: ny + H / 2 }
    case 'input-top': return { x: nx + W / 2, y: ny }
    // fallback for legacy 'output'/'input'
    case 'output': return { x: nx + W, y: ny + H / 2 }
    case 'input': return { x: nx, y: ny + H / 2 }
    default: return { x: nx + W / 2, y: ny + H / 2 }
  }
}

// ========== 连线绘制 ==========

const drawEdge = (sn, tn, edgeId, sourcePort, targetPort) => {
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  if (!stage) return

  const srcPort = sourcePort || 'output-right'
  const tgtPort = targetPort || 'input-left'
  const sid = sn.getAttribute('data-cell-id')
  const tid = tn.getAttribute('data-cell-id')
  const s = getPortCenter(sn, srcPort)
  const t = getPortCenter(tn, tgtPort)

  const g = crEl('g', {
    class: 'dag-edge-group',
    'data-edge-id': edgeId,
    'data-source': sid,
    'data-target': tid,
    style: 'cursor:pointer'
  })

  const path = crEl('path', {
    d: computeEdgePath(s.x, s.y, t.x, t.y, connectorType.value),
    fill: 'none',
    stroke: '#b0b0b0',
    'stroke-width': '2',
    'marker-end': 'url(#dagArrow)',
    class: 'dag-edge',
    'data-edge-id': edgeId,
    'data-source': sid,
    'data-target': tid,
    'pointer-events': 'stroke'
  })
  g.appendChild(path)

  // 条件标签
  const edgeData = edgeDataList.find(e => e.id === edgeId)
  const condition = edgeData?.condition || 'SUCCESS'
  const mx = (s.x + t.x) / 2
  const my = (s.y + t.y) / 2

  const labelBg = crEl('rect', {
    x: mx - 22, y: my - 10, width: 44, height: 18,
    fill: '#fff', rx: 3, 'pointer-events': 'none',
    class: 'dag-edge-label-bg'
  })
  g.appendChild(labelBg)

  const label = crEl('text', {
    x: mx, y: my + 3,
    'font-size': '11', fill: '#8c8c8c', 'text-anchor': 'middle',
    'pointer-events': 'none', class: 'dag-edge-label'
  })
  label.textContent = condition
  g.appendChild(label)

  ensureDefs()
  stage.appendChild(g)
}

const updateEdgesForNode = (nodeId) => {
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  if (!stage) return

  const groups = stage.querySelectorAll(`.dag-edge-group[data-source="${nodeId}"], .dag-edge-group[data-target="${nodeId}"]`)
  groups.forEach(group => {
    const sid = group.getAttribute('data-source')
    const tid = group.getAttribute('data-target')
    const eid = group.getAttribute('data-edge-id')
    const sn = stage.querySelector(`.dag-node[data-cell-id="${sid}"]`)
    const tn = stage.querySelector(`.dag-node[data-cell-id="${tid}"]`)
    if (sn && tn) {
      const edgeData = edgeDataList.find(e => e.id === eid)
      const s = getPortCenter(sn, edgeData?.sourcePort || 'output-right')
      const t = getPortCenter(tn, edgeData?.targetPort || 'input-left')

      const path = group.querySelector('.dag-edge')
      if (path) {
        path.setAttribute('d', computeEdgePath(s.x, s.y, t.x, t.y, connectorType.value))
      }

      const mx = (s.x + t.x) / 2
      const my = (s.y + t.y) / 2
      const label = group.querySelector('.dag-edge-label')
      if (label) { label.setAttribute('x', mx); label.setAttribute('y', my + 3) }
      const labelBg = group.querySelector('.dag-edge-label-bg')
      if (labelBg) { labelBg.setAttribute('x', mx - 22); labelBg.setAttribute('y', my - 10) }
    }
  })
}

const ensureDefs = () => {
  const svg = containerRef.value?.querySelector('svg.x6-graph-svg')
  if (!svg) return
  let defs = svg.querySelector('defs')
  if (!defs) {
    defs = document.createElementNS(SVG_NS, 'defs')
    svg.insertBefore(defs, svg.firstChild)
  }
  if (!defs.querySelector('#dagArrow')) {
    const m = crEl('marker', {
      id: 'dagArrow', markerWidth: '10', markerHeight: '8',
      refX: '8', refY: '4', orient: 'auto', markerUnits: 'userSpaceOnUse'
    })
    m.appendChild(crEl('polygon', { points: '0 0, 10 4, 0 8', fill: '#b0b0b0' }))
    defs.appendChild(m)
  }
}

// ========== 选中管理 ==========

const selectNode = (id, nodeEl) => {
  clearSelection()
  selectedNodeId = id
  if (nodeEl) {
    nodeEl.classList.add('selected')
    const b = nodeEl.querySelector('.node-body')
    if (b) { b.setAttribute('stroke', '#1890ff'); b.setAttribute('stroke-width', '2') }
    const s = nodeEl.querySelector('.node-shadow')
    if (s) { s.setAttribute('fill', '#e6f7ff'); s.setAttribute('stroke', '#1890ff') }
  }
  const nd = nodeDataMap.get(id)
  if (nd) emit('node-select', nd)
}

const selectEdge = (edgeEl) => {
  clearSelection()
  const group = edgeEl.classList.contains('dag-edge-group') ? edgeEl : edgeEl.closest('.dag-edge-group')
  const eid = group ? group.getAttribute('data-edge-id') : edgeEl.getAttribute('data-edge-id')
  selectedEdgeId = eid
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  // 取消所有连线选中
  stage?.querySelectorAll('.dag-edge-group.selected, .dag-edge.selected').forEach(e => {
    e.classList.remove('selected')
    const p = e.querySelector('.dag-edge') || e
    p.setAttribute('stroke', '#b0b0b0')
    p.setAttribute('stroke-width', '2')
  })
  if (group) {
    group.classList.add('selected')
    const p = group.querySelector('.dag-edge')
    if (p) { p.setAttribute('stroke', '#1890ff'); p.setAttribute('stroke-width', '2.5') }
  }
  const ed = edgeDataList.find(e => e.id === eid)
  if (ed) emit('edge-select', ed)
}

const clearSelection = () => {
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  stage?.querySelectorAll('.dag-node.selected').forEach(el => {
    el.classList.remove('selected')
    const b = el.querySelector('.node-body')
    if (b) { b.setAttribute('stroke', '#d9d9d9'); b.setAttribute('stroke-width', '1.5') }
    const s = el.querySelector('.node-shadow')
    if (s) { s.setAttribute('fill', 'transparent'); s.setAttribute('stroke', 'transparent') }
  })
  stage?.querySelectorAll('.dag-edge-group.selected, .dag-edge.selected').forEach(el => {
    el.classList.remove('selected')
    const p = el.querySelector('.dag-edge') || el
    p.setAttribute('stroke', '#b0b0b0')
    p.setAttribute('stroke-width', '2')
  })
  selectedNodeId = null
  selectedEdgeId = null
}

// ========== 拖放添加节点 ==========

const handleDrop = (e) => {
  isDragOver.value = false
  try {
    const data = JSON.parse(e.dataTransfer.getData('application/json'))
    if (data?.type) {
      const rect = containerRef.value.getBoundingClientRect()
      const x = (e.clientX - rect.left) / currentZoom.value
      const y = (e.clientY - rect.top) / currentZoom.value
      const id = nextNodeId()
      const color = nodeTypeColors[data.type] || '#1890ff'
      renderNodeToStage(id, data.type, data.label, x, y, color, {})
      emit('node-add', { id, type: data.type, name: data.label, x, y, config: {} })
    }
  } catch (_) {}
}

// ========== API加载 ==========

const loadDagFromApi = async (taskId) => {
  if (!taskId) return
  try {
    const resp = await fetch(`/api/dag/${taskId}`)
    const json = await resp.json()
    const nodes = json.data?.nodes || []
    const edges = json.data?.edges || []

    const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
    if (!stage) return

    // 清空现有
    while (stage.firstChild) stage.removeChild(stage.firstChild)
    nodeDataMap.clear()
    edgeDataList.length = 0

    // 渲染节点
    nodes.forEach(n => {
      renderNodeToStage(n.id, n.type, n.name, n.x || 100, n.y || 100,
        nodeTypeColors[n.type] || '#1890ff', n.config || {})
    })

    // 渲染连线
    await nextTick()
    edges.forEach(e => {
      edgeDataList.push(e)
      const sn = stage.querySelector(`.dag-node[data-cell-id="${e.source}"]`)
      const tn = stage.querySelector(`.dag-node[data-cell-id="${e.target}"]`)
      if (sn && tn) drawEdge(sn, tn, e.id)
    })
  } catch (_) {}
}

// ========== 初始化 ==========

const init = () => {
  if (!containerRef.value) return

  // 确保有stage容器
  let stage = containerRef.value.querySelector('.x6-graph-svg-stage')
  let svg = containerRef.value.querySelector('svg.x6-graph-svg')

  if (!svg) {
    svg = document.createElementNS(SVG_NS, 'svg')
    svg.setAttribute('class', 'x6-graph-svg')
    svg.style.cssText = 'position:absolute;inset:0;width:100%;height:100%'
    containerRef.value.appendChild(svg)
  }

  if (!stage) {
    stage = document.createElementNS(SVG_NS, 'g')
    stage.setAttribute('class', 'x6-graph-svg-stage')
    svg.appendChild(stage)
  }

  ensureDefs()

  // 加载DAG数据
  if (props.taskId) {
    loadDagFromApi(props.taskId)
  }

  // 全局mouseup处理
  globalMouseUpHandler = () => {
    if (connecting.value) {
      connecting.value = false
      connectSource = null
      connectingPath.value = ''
    }
    if (dragState) {
      dragState.node.classList.remove('dragging')
      finishNodeDrag({ clientX: dragState.sx, clientY: dragState.sy })
    }
  }
  document.addEventListener('mouseup', globalMouseUpHandler)
}

// 删除选中元素（内部函数，供键盘和 defineExpose 共用）
const doDeleteSelected = () => {
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  if (!stage) return
  if (selectedNodeId) {
    stage.querySelectorAll(`.dag-edge-group[data-source="${selectedNodeId}"], .dag-edge-group[data-target="${selectedNodeId}"]`).forEach(e => e.remove())
    const el = stage.querySelector(`.dag-node[data-cell-id="${selectedNodeId}"]`)
    if (el) el.remove()
    nodeDataMap.delete(selectedNodeId)
    for (let i = edgeDataList.length - 1; i >= 0; i--) {
      if (edgeDataList[i].source === selectedNodeId || edgeDataList[i].target === selectedNodeId) {
        edgeDataList.splice(i, 1)
      }
    }
    emit('node-remove', selectedNodeId)
  } else if (selectedEdgeId) {
    const el = stage.querySelector(`.dag-edge-group[data-edge-id="${selectedEdgeId}"]`)
    if (el) el.remove()
    const idx = edgeDataList.findIndex(e => e.id === selectedEdgeId)
    if (idx >= 0) edgeDataList.splice(idx, 1)
    emit('edge-remove', selectedEdgeId)
  }
  clearSelection()
}

// 缩放操作
const zoomIn = () => { currentZoom.value = Math.min(2, +(currentZoom.value + 0.1).toFixed(1)); applyZoom() }
const zoomOut = () => { currentZoom.value = Math.max(0.25, +(currentZoom.value - 0.1).toFixed(1)); applyZoom() }
const fitContent = () => {
  if (nodeDataMap.size === 0) return
  const rect = containerRef.value?.getBoundingClientRect()
  if (!rect) return
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  nodeDataMap.forEach(v => {
    if (v.x < minX) minX = v.x
    if (v.y < minY) minY = v.y
    if (v.x + 220 > maxX) maxX = v.x + 220
    if (v.y + 80 > maxY) maxY = v.y + 80
  })
  if (!isFinite(minX)) return
  const cw = maxX - minX + 80, ch = maxY - minY + 80
  const zx = (rect.width - 80) / cw, zy = (rect.height - 80) / ch
  currentZoom.value = Math.min(1.5, Math.max(0.25, Math.min(zx, zy)))
  applyZoom()
}

// 暴露给父组件
defineExpose({
  zoomIn,
  zoomOut,
  fitContent,
  autoLayout: () => {
    if (nodeDataMap.size === 0) return
    const nodeIds = new Set()
    nodeDataMap.forEach((_, id) => nodeIds.add(id))
    const adj = new Map()
    const inDeg = new Map()
    nodeIds.forEach(id => { adj.set(id, []); inDeg.set(id, 0) })
    edgeDataList.forEach(e => {
      if (nodeIds.has(e.source) && nodeIds.has(e.target)) {
        adj.get(e.source).push(e.target)
        inDeg.set(e.target, (inDeg.get(e.target) || 0) + 1)
      }
    })

    const layers = []
    const queue = []
    inDeg.forEach((d, id) => { if (d === 0) queue.push(id) })
    while (queue.length) {
      const layer = [...queue]
      layers.push(layer)
      queue.length = 0
      layer.forEach(id => {
        (adj.get(id) || []).forEach(t => {
          inDeg.set(t, inDeg.get(t) - 1)
          if (inDeg.get(t) === 0) queue.push(t)
        })
      })
    }
    const remaining = []
    nodeDataMap.forEach((_, id) => {
      if (!layers.some(l => l.includes(id))) remaining.push(id)
    })
    if (remaining.length) layers.push(remaining)

    const sx = 60, sy = 60, cg = 280, rg = 80
    const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
    layers.forEach((layer, li) => {
      layer.forEach((id, ni) => {
        const nx = sx + li * cg, ny = sy + ni * rg
        const nd = nodeDataMap.get(id)
        if (nd) { nd.x = nx; nd.y = ny }
        const el = stage?.querySelector(`.dag-node[data-cell-id="${id}"]`)
        if (el) el.setAttribute('transform', `translate(${nx},${ny})`)
      })
    })
    nextTick(() => {
      edgeDataList.forEach(e => {
        const sn = stage?.querySelector(`.dag-node[data-cell-id="${e.source}"]`)
        const tn = stage?.querySelector(`.dag-node[data-cell-id="${e.target}"]`)
        if (sn && tn) updateEdgesForNode(e.source)
      })
    })
  },
  getNodeDataMap: () => {
    const r = {}
    nodeDataMap.forEach((v, k) => { r[k] = { ...v } })
    return r
  },
  getEdges: () => [...edgeDataList],
  addNodeAt: (type, label, cx, cy) => {
    if (!containerRef.value) return
    const rect = containerRef.value.getBoundingClientRect()
    const color = nodeTypeColors[type] || '#1890ff'
    const id = nextNodeId()
    const x = cx - rect.left, y = cy - rect.top
    renderNodeToStage(id, type, label, x, y, color, {})
    emit('node-add', { id, type, name: label, x, y, config: {} })
  },
  deleteSelected: doDeleteSelected,
  updateNodeStatus: (nodeId, status) => {
    const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
    if (!stage) return
    const nodeEl = stage.querySelector(`.dag-node[data-cell-id="${nodeId}"]`)
    if (!nodeEl) return
    const bodyEl = nodeEl.querySelector('.node-body')
    const color = STATUS_COLORS[status] || STATUS_COLORS.PENDING

    // 更新边框颜色
    if (bodyEl) {
      bodyEl.setAttribute('stroke', color)
      bodyEl.setAttribute('stroke-width', status === 'RUNNING' ? '2.5' : '2')
    }

    // 移除旧状态徽章
    const oldBadge = nodeEl.querySelector('.node-status-badge')
    if (oldBadge) oldBadge.remove()

    // 移除运行动画类
    nodeEl.classList.remove('node-running', 'node-success', 'node-failed', 'node-skipped')

    // 添加新状态类
    const statusClass = `node-${status.toLowerCase()}`
    nodeEl.classList.add(statusClass)

    // 添加状态徽章 (右上角)
    if (status && status !== 'PENDING') {
      const badge = document.createElementNS(SVG_NS, 'g')
      badge.setAttribute('class', 'node-status-badge')
      badge.setAttribute('transform', `translate(${W - 16}, 4)`)
      const dot = document.createElementNS(SVG_NS, 'circle')
      dot.setAttribute('cx', '8'); dot.setAttribute('cy', '8'); dot.setAttribute('r', '6')
      dot.setAttribute('fill', color)
      dot.setAttribute('stroke', '#fff'); dot.setAttribute('stroke-width', '1.5')
      badge.appendChild(dot)
      if (status === 'RUNNING') {
        const pulse = document.createElementNS(SVG_NS, 'circle')
        pulse.setAttribute('cx', '8'); pulse.setAttribute('cy', '8'); pulse.setAttribute('r', '6')
        pulse.setAttribute('fill', 'none'); pulse.setAttribute('stroke', color)
        pulse.setAttribute('stroke-width', '2'); pulse.setAttribute('class', 'status-pulse')
        badge.appendChild(pulse)
      }
      nodeEl.appendChild(badge)
    }

    nodeStatusMap.set(nodeId, status)
  },
  clearNodeStatuses: () => {
    const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
    if (!stage) return
    nodeStatusMap.forEach((_, nodeId) => {
      const nodeEl = stage.querySelector(`.dag-node[data-cell-id="${nodeId}"]`)
      if (nodeEl) {
        const bodyEl = nodeEl.querySelector('.node-body')
        if (bodyEl) { bodyEl.setAttribute('stroke', '#d9d9d9'); bodyEl.setAttribute('stroke-width', '1.5') }
        const badge = nodeEl.querySelector('.node-status-badge')
        if (badge) badge.remove()
        nodeEl.classList.remove('node-running', 'node-success', 'node-failed', 'node-skipped')
      }
    })
    nodeStatusMap.clear()
  },
  getNodeStatuses: () => {
    const r = {}
    nodeStatusMap.forEach((v, k) => { r[k] = v })
    return r
  },
  connectorType,
  toggleConnectorType: () => {
    connectorType.value = connectorType.value === 'bezier' ? 'step' : 'bezier'
  }
})

const applyZoom = () => {
  const stage = containerRef.value?.querySelector('.x6-graph-svg-stage')
  if (stage) stage.setAttribute('transform', `scale(${currentZoom.value})`)
}

onMounted(() => {
  init()
})

onUnmounted(() => {
  if (globalMouseUpHandler) {
    document.removeEventListener('mouseup', globalMouseUpHandler)
    globalMouseUpHandler = null
  }
})
</script>

<style lang="scss" scoped>
.dag-canvas {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: radial-gradient(circle, #e0e0e0 1px, transparent 1px);
  background-size: 20px 20px;
}

.drop-zone-hint {
  position: absolute;
  inset: 0;
  z-index: 20;
  background: rgba(24, 144, 255, 0.06);
  border: 2px dashed #1890ff;
  border-radius: 12px;
  margin: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1890ff;
  font-size: 15px;
  font-weight: 500;
  pointer-events: none;
}

.drag-line-overlay {
  position: absolute;
  inset: 0;
  z-index: 30;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.zoom-controls {
  position: absolute;
  bottom: 12px;
  right: 12px;
  z-index: 25;
  display: flex;
  align-items: center;
  gap: 2px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 2px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  user-select: none;
}

.zoom-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #666;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 0;
  transition: all 0.15s;

  &:hover {
    background: #f0f0f0;
    color: #1890ff;
  }
}

.zoom-value {
  font-size: 11px;
  color: #999;
  min-width: 36px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.zoom-fit {
  border-left: 1px solid #eee;
  border-radius: 0 4px 4px 0;
  padding-left: 1px;
}

.minimap-container {
  position: absolute;
  bottom: 48px;
  right: 12px;
  z-index: 25;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  overflow: hidden;
}

.minimap-svg {
  display: block;
}

.minimap-toggle {
  position: absolute;
  bottom: 12px;
  right: 88px;
  z-index: 25;
  width: 28px;
  height: 28px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #999;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: all 0.15s;

  &:hover, &.active {
    color: #1890ff;
    border-color: #1890ff;
  }
}

:deep(.dag-node) {
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.08));
  transition: filter 0.2s;

  &.dragging {
    filter: none !important;
    transition: none !important;
  }

  &:hover {
    filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.15));

    .dag-port {
      opacity: 1;
      r: 6;
    }
  }

  &.selected {
    filter: drop-shadow(0 0 8px rgba(24, 144, 255, 0.4));
  }

  .dag-port {
    opacity: 0.4;
    transition: all 0.2s;

    &:hover {
      opacity: 1;
      r: 7;
      stroke: #1890ff;
      stroke-width: 3;
    }
  }

  .dag-port[data-port^="output-"]:hover {
    fill: #1890ff;
    cursor: grabbing !important;
  }

  .dag-port[data-port^="input-"]:hover {
    cursor: crosshair !important;
  }
}

:deep(.dag-edge) {
  transition: stroke 0.2s, stroke-width 0.2s;

  &.selected {
    stroke: #1890ff !important;
    stroke-width: 2.5 !important;
    filter: drop-shadow(0 1px 2px rgba(24, 144, 255, 0.3));
  }

  &:hover {
    stroke: #ff4d4f !important;
    stroke-width: 2.5 !important;
  }
}

// 节点运行状态动画
@keyframes status-pulse {
  0% { r: 6; opacity: 1; }
  100% { r: 12; opacity: 0; }
}

@keyframes node-running-glow {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

:deep(.node-running) {
  .node-body {
    animation: node-running-glow 1.5s ease-in-out infinite;
  }
}

:deep(.node-success) {
  filter: drop-shadow(0 2px 4px rgba(82, 196, 26, 0.15));
}

:deep(.node-failed) {
  filter: drop-shadow(0 2px 4px rgba(255, 77, 79, 0.15));
}

:deep(.node-skipped) {
  filter: drop-shadow(0 2px 4px rgba(250, 173, 20, 0.12));
}

.status-pulse {
  animation: status-pulse 1.2s ease-out infinite;
}
</style>
