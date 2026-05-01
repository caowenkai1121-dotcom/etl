import { defineStore } from 'pinia'

export const useDevWorkbenchStore = defineStore('devWorkbench', {
  state: () => ({
    // 当前模式：dev-开发模式, prod-生产模式
    mode: 'dev',

    // 当前选中的任务
    currentTask: null,

    // DAG配置
    dagConfig: {
      nodes: [],
      edges: [],
      viewport: { x: 0, y: 0, zoom: 1 }
    },

    // 当前选中的节点
    selectedNode: null,

    // 当前选中的连线
    selectedEdge: null,

    // 文件夹树
    folderTree: [],

    // 任务列表
    taskList: [],

    // 左侧边栏展开状态
    leftPanelExpanded: true,

    // 属性面板展开状态
    propertyPanelExpanded: true,

    // 底部面板展开状态
    bottomPanelExpanded: true,

    // 底部面板当前tab
    bottomPanelTab: 'log',

    // 运行日志
    runLogs: [],

    // 运行统计
    runStats: {
      success: 0,
      failed: 0,
      interrupted: 0,
      skipped: 0,
      duration: 0
    },

    // 画布缩放
    canvasZoom: 1,

    // 是否正在保存
    saving: false,

    // 是否正在运行
    running: false
  }),

  getters: {
    isDevMode: (state) => state.mode === 'dev',
    isProdMode: (state) => state.mode === 'prod',
    hasSelectedNode: (state) => state.selectedNode !== null,
    hasSelectedEdge: (state) => state.selectedEdge !== null,

    // 获取节点数量
    nodeCount: (state) => state.dagConfig.nodes.length,

    // 获取连线数量
    edgeCount: (state) => state.dagConfig.edges.length
  },

  actions: {
    // 切换模式
    toggleMode() {
      this.mode = this.mode === 'dev' ? 'prod' : 'dev'
    },

    // 设置当前任务
    setCurrentTask(task) {
      this.currentTask = task
    },

    // 设置DAG配置
    setDagConfig(config) {
      this.dagConfig = config
    },

    // 更新节点
    updateNode(nodeId, updates) {
      const node = this.dagConfig.nodes.find(n => n.id === nodeId)
      if (node) {
        Object.assign(node, updates)
      }
    },

    // 添加节点
    addNode(node) {
      this.dagConfig.nodes.push(node)
    },

    // 删除节点
    removeNode(nodeId) {
      this.dagConfig.nodes = this.dagConfig.nodes.filter(n => n.id !== nodeId)
      // 同时删除相关连线
      this.dagConfig.edges = this.dagConfig.edges.filter(
        e => e.source !== nodeId && e.target !== nodeId
      )
    },

    // 添加连线
    addEdge(edge) {
      this.dagConfig.edges.push(edge)
    },

    // 删除连线
    removeEdge(edgeId) {
      this.dagConfig.edges = this.dagConfig.edges.filter(e => e.id !== edgeId)
    },

    // 选中节点
    selectNode(node) {
      this.selectedNode = node
      this.selectedEdge = null
    },

    // 选中连线
    selectEdge(edge) {
      this.selectedEdge = edge
      this.selectedNode = null
    },

    // 清除选中
    clearSelection() {
      this.selectedNode = null
      this.selectedEdge = null
    },

    // 设置文件夹树
    setFolderTree(tree) {
      this.folderTree = tree
    },

    // 设置任务列表
    setTaskList(list) {
      this.taskList = list
    },

    // 切换左侧边栏展开
    toggleLeftPanel() {
      this.leftPanelExpanded = !this.leftPanelExpanded
    },

    // 设置左侧边栏展开
    setLeftPanelExpanded(expanded) {
      this.leftPanelExpanded = expanded
    },

    // 切换属性面板展开
    togglePropertyPanel() {
      this.propertyPanelExpanded = !this.propertyPanelExpanded
    },

    // 切换底部面板展开
    toggleBottomPanel() {
      this.bottomPanelExpanded = !this.bottomPanelExpanded
    },

    // 设置底部面板tab
    setBottomPanelTab(tab) {
      this.bottomPanelTab = tab
    },

    // 添加运行日志
    addRunLog(log) {
      this.runLogs.push({
        ...log,
        timestamp: new Date().toISOString()
      })
    },

    // 清空运行日志
    clearRunLogs() {
      this.runLogs = []
    },

    // 设置运行统计
    setRunStats(stats) {
      this.runStats = stats
    },

    // 设置画布缩放
    setCanvasZoom(zoom) {
      this.canvasZoom = zoom
    },

    // 设置保存状态
    setSaving(saving) {
      this.saving = saving
    },

    // 设置运行状态
    setRunning(running) {
      this.running = running
    },

    // 重置状态
    reset() {
      this.currentTask = null
      this.dagConfig = { nodes: [], edges: [], viewport: { x: 0, y: 0, zoom: 1 } }
      this.selectedNode = null
      this.selectedEdge = null
      this.runLogs = []
      this.runStats = { success: 0, failed: 0, interrupted: 0, skipped: 0, duration: 0 }
      this.saving = false
      this.running = false
    }
  }
})