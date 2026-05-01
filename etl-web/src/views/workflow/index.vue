<template>
  <div class="workflow-page">
    <!-- 左侧文件夹树 -->
    <div class="folder-panel">
      <div class="panel-header">
        <span class="title">工作流目录</span>
        <el-button type="primary" size="small" @click="handleCreateFolder">
          <el-icon><FolderAdd /></el-icon>
        </el-button>
      </div>
      <el-tree
        ref="folderTreeRef"
        :data="folderTree"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        highlight-current
        node-key="id"
        @node-click="handleFolderClick"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <el-icon><Folder /></el-icon>
            <span>{{ data.name }}</span>
            <span class="node-count">{{ data.itemCount || 0 }}</span>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 右侧内容区 -->
    <div class="content-panel">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="left">
          <el-input v-model="searchName" placeholder="搜索工作流名称" clearable style="width: 200px">
            <template #append>
              <el-button @click="fetchData"><el-icon><Search /></el-icon></el-button>
            </template>
          </el-input>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 120px; margin-left: 16px" @change="fetchData">
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
          <el-select v-model="filterPublish" placeholder="发布状态" clearable style="width: 120px; margin-left: 12px" @change="fetchData">
            <el-option label="全部" value="" />
            <el-option label="待发布" value="PENDING" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="待更新" value="UPDATED" />
          </el-select>
        </div>
        <div class="right">
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建工作流
          </el-button>
        </div>
      </div>

      <!-- 工作流列表 -->
      <el-card class="data-card">
        <el-table :data="tableData" v-loading="loading">
          <el-table-column prop="name" label="工作流名称" min-width="180">
            <template #default="{ row }">
              <div class="workflow-name">
                <el-icon><Share /></el-icon>
                <span class="name-text">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="节点数" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.nodeCount || 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
                {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publishStatus" label="发布状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.publishStatus === 'PUBLISHED' ? 'success' : row.publishStatus === 'UPDATED' ? 'warning' : 'info'"
                size="small"
              >
                {{ getPublishStatusText(row.publishStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="70" align="center">
            <template #default="{ row }">
              <span>v{{ row.version }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="cronExpression" label="调度" width="140" show-overflow-tooltip />
          <el-table-column prop="updateTime" label="更新时间" width="180" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button type="success" size="small" @click="handleExecute(row)" :disabled="row.status !== 'PUBLISHED'">
                <el-icon><VideoPlay /></el-icon>
                执行
              </el-button>
              <el-button type="warning" size="small" @click="handlePublish(row)" :disabled="row.publishStatus === 'PUBLISHED'">
                <el-icon><Upload /></el-icon>
                发布
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
          class="pagination"
        />
      </el-card>
    </div>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="folderDialogVisible" title="新建文件夹" width="400px">
      <el-form :model="folderForm" label-width="80px">
        <el-form-item label="文件夹名">
          <el-input v-model="folderForm.name" placeholder="请输入文件夹名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFolder">确定</el-button>
      </template>
    </el-dialog>

    <!-- 工作流编辑器对话框 -->
    <el-dialog v-model="editorVisible" :title="editorTitle" width="95%" top="2vh" :close-on-click-modal="false">
      <!-- 工作流基础信息 -->
      <div class="workflow-info-bar">
        <el-form :inline="true" size="small">
          <el-form-item label="工作流名称">
            <el-input v-model="workflowForm.name" placeholder="请输入工作流名称" style="width: 200px" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="workflowForm.description" placeholder="工作流描述" style="width: 300px" />
          </el-form-item>
          <el-form-item label="调度配置">
            <el-input v-model="workflowForm.cronExpression" placeholder="Cron表达式，如: 0 0 2 * * ?" style="width: 180px" />
          </el-form-item>
        </el-form>
      </div>
      <div class="workflow-editor">
        <!-- 节点面板 -->
        <div class="node-panel">
          <div class="panel-title">节点组件</div>
          <div v-for="node in nodeTypes" :key="node.type" class="node-item" draggable="true" @dragstart="handleDragStart($event, node)">
            <el-icon :size="24"><component :is="node.icon" /></el-icon>
            <span>{{ node.name }}</span>
          </div>
        </div>

        <!-- 画布区 -->
        <div ref="canvasContainer" class="canvas-container"></div>

        <!-- 配置面板 -->
        <div class="config-panel" v-if="selectedNode">
          <div class="panel-title">节点配置</div>
          <el-form :model="nodeConfig" label-width="80px" size="small">
            <el-form-item label="节点名称">
              <el-input v-model="selectedNode.name" />
            </el-form-item>

            <!-- 根据节点类型显示不同配置 -->
            <template v-if="selectedNode.type === 'SYNC'">
              <el-divider>数据同步配置</el-divider>
              <el-form-item label="源数据源">
                <el-select v-model="nodeConfig.sourceDsId" placeholder="选择源数据源" style="width: 100%">
                  <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="目标数据源">
                <el-select v-model="nodeConfig.targetDsId" placeholder="选择目标数据源" style="width: 100%">
                  <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="同步模式">
                <el-select v-model="nodeConfig.syncMode" style="width: 100%">
                  <el-option label="全量" value="FULL" />
                  <el-option label="增量" value="INCREMENTAL" />
                  <el-option label="CDC" value="CDC" />
                </el-select>
              </el-form-item>
              <el-form-item label="表配置">
                <el-input v-model="nodeConfig.tableConfig" type="textarea" :rows="4" placeholder="JSON格式表配置" />
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'TRANSFORM'">
              <el-divider>数据转换配置</el-divider>
              <el-form-item label="转换规则">
                <el-input v-model="nodeConfig.transformRules" type="textarea" :rows="6" placeholder="转换规则JSON" />
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'SCRIPT'">
              <el-divider>脚本配置</el-divider>
              <el-form-item label="脚本类型">
                <el-select v-model="nodeConfig.scriptType" style="width: 100%">
                  <el-option label="SQL" value="SQL" />
                  <el-option label="Shell" value="SHELL" />
                </el-select>
              </el-form-item>
              <el-form-item label="脚本内容">
                <el-input v-model="nodeConfig.scriptContent" type="textarea" :rows="8" placeholder="脚本内容" />
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'CONDITION'">
              <el-divider>条件配置</el-divider>
              <el-form-item label="条件表达式">
                <el-input v-model="nodeConfig.conditionExpr" placeholder="如: ${row.status} == 1" />
              </el-form-item>
            </template>

            <template v-if="selectedNode.type === 'LOOP'">
              <el-divider>循环配置</el-divider>
              <el-form-item label="循环变量">
                <el-input v-model="nodeConfig.loopVar" placeholder="循环变量名" />
              </el-form-item>
              <el-form-item label="循环次数">
                <el-input-number v-model="nodeConfig.loopCount" :min="1" style="width: 100%" />
              </el-form-item>
            </template>
          </el-form>
          <el-button type="danger" size="small" @click="deleteSelectedNode" style="margin-top: 16px; width: 100%">
            删除节点
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveWorkflow" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workflowAPI, getFolderTree, createFolder, getDatasourceList } from '@/api'
import { Graph, Shape } from '@antv/x6'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const searchName = ref('')
const filterStatus = ref('')
const filterPublish = ref('')
const folderTree = ref([])
const datasourceList = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: '',
  publishStatus: '',
  folderId: null
})

// 节点类型
const nodeTypes = ref([
  { type: 'SYNC', name: '数据同步', icon: 'Refresh' },
  { type: 'TRANSFORM', name: '数据转换', icon: 'Edit' },
  { type: 'SCRIPT', name: '脚本执行', icon: 'Document' },
  { type: 'CONDITION', name: '条件分支', icon: 'Share' },
  { type: 'LOOP', name: '循环控制', icon: 'RefreshRight' }
])

// 文件夹相关
const folderDialogVisible = ref(false)
const folderForm = reactive({ name: '', parentId: 0 })

// 编辑器相关
const editorVisible = ref(false)
const editorTitle = ref('新建工作流')
const saving = ref(false)
const currentWorkflow = ref(null)
const canvasContainer = ref(null)
const selectedNode = ref(null)
const nodeConfig = reactive({})
const workflowForm = reactive({
  id: null,
  name: '',
  description: '',
  folderId: null,
  workflowJson: '',
  cronExpression: ''
})

let graph = null

onMounted(() => {
  fetchData()
  fetchFolderTree()
  fetchDatasources()
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      name: searchName.value,
      status: filterStatus.value,
      publishStatus: filterPublish.value
    }
    const res = await workflowAPI.getPage(params)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取工作流列表失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchFolderTree = async () => {
  try {
    const res = await getFolderTree()
    folderTree.value = [{ id: 0, name: '全部工作流', children: res.data || [] }]
  } catch (e) {
    console.error('获取文件夹树失败:', e)
  }
}

const fetchDatasources = async () => {
  try {
    const res = await getDatasourceList()
    datasourceList.value = res.data || []
  } catch (e) {
    console.error('获取数据源列表失败:', e)
  }
}

const handleFolderClick = (data) => {
  queryParams.folderId = data.id === 0 ? null : data.id
  fetchData()
}

const handleCreateFolder = () => {
  folderForm.name = ''
  folderForm.parentId = queryParams.folderId || 0
  folderDialogVisible.value = true
}

const submitFolder = async () => {
  if (!folderForm.name) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  try {
    await createFolder(folderForm)
    ElMessage.success('创建成功')
    folderDialogVisible.value = false
    fetchFolderTree()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const handleCreate = () => {
  editorTitle.value = '新建工作流'
  currentWorkflow.value = null
  Object.assign(workflowForm, {
    id: null,
    name: '',
    description: '',
    folderId: queryParams.folderId,
    workflowJson: '{"nodes":[],"edges":[]}',
    cronExpression: ''
  })
  editorVisible.value = true
  nextTick(() => {
    setTimeout(() => {
      initGraph()
    }, 100)
  })
}

const handleEdit = (row) => {
  editorTitle.value = '编辑工作流'
  currentWorkflow.value = row
  Object.assign(workflowForm, row)
  editorVisible.value = true
  nextTick(() => {
    initGraph()
    if (row.workflowJson) {
      loadGraphData(row.workflowJson)
    }
  })
}

const handleExecute = async (row) => {
  try {
    await workflowAPI.execute(row.id)
    ElMessage.success('工作流已开始执行')
  } catch (e) {
    ElMessage.error('执行失败')
  }
}

const handlePublish = async (row) => {
  try {
    await workflowAPI.publish(row.id)
    ElMessage.success('发布成功')
    fetchData()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该工作流？', '提示', { type: 'warning' })
    await workflowAPI.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getPublishStatusText = (status) => {
  const map = { 'PENDING': '待发布', 'PUBLISHED': '已发布', 'UPDATED': '待更新' }
  return map[status] || status
}

// 初始化画布
const initGraph = () => {
  if (!canvasContainer.value) {
    console.warn('画布容器不存在')
    return
  }

  // 销毁旧图
  if (graph) {
    graph.dispose()
    graph = null
  }

  // 确保容器有尺寸
  const container = canvasContainer.value
  const width = container.offsetWidth || container.clientWidth || 800
  const height = container.offsetHeight || container.clientHeight || 500

  try {
    graph = new Graph({
      container: container,
      width: width,
      height: height,
      grid: true,
      panning: true,
      mousewheel: true,
      connecting: {
        anchor: 'center',
        connectionPoint: 'anchor',
        allowBlank: false,
        allowLoop: false,
        allowNode: true,
        allowEdge: false,
        highlight: true,
        createEdge() {
          return new Shape.Edge({
            attrs: {
              line: {
                stroke: '#409eff',
                strokeWidth: 2,
                targetMarker: {
                  name: 'block',
                  width: 12,
                  height: 8
                }
              }
            },
            zIndex: 0
          })
        }
      },
      highlighting: {
        magnetAvailable: {
          name: 'stroke',
          args: {
            attrs: {
              fill: '#fff',
              stroke: '#409eff'
            }
          }
        }
      }
    })
    console.log('图表初始化成功')
  } catch (e) {
    console.error('图表初始化失败:', e)
  }

  // 监听节点点击
  graph.on('node:click', ({ node }) => {
    selectNode(node)
  })

  // 监听空白区域点击
  graph.on('blank:click', () => {
    selectedNode.value = null
  })

  // 监听画布拖放
  container.addEventListener('dragover', (e) => {
    e.preventDefault()
  })
  container.addEventListener('drop', handleDrop)
}

const handleDrop = (e) => {
  e.preventDefault()
  const nodeType = e.dataTransfer.getData('nodeType')
  if (!nodeType || !graph) return

  const rect = canvasContainer.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  addNode(nodeType, x, y)
}

const addNode = (type, x, y) => {
  const nodeType = nodeTypes.value.find(n => n.type === type)
  const nodeName = nodeType ? nodeType.name : type

  const colorMap = {
    'SYNC': '#67C23A',
    'TRANSFORM': '#409EFF',
    'SCRIPT': '#E6A23C',
    'CONDITION': '#F56C6C',
    'LOOP': '#909399'
  }

  const node = graph.addNode({
    id: `node-${Date.now()}`,
    shape: 'rect',
    x: x - 60,
    y: y - 25,
    width: 120,
    height: 50,
    attrs: {
      body: {
        fill: colorMap[type] || '#409EFF',
        stroke: '#fff',
        strokeWidth: 2,
        rx: 8,
        ry: 8
      },
      label: {
        text: nodeName,
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    },
    data: {
      type: type,
      name: nodeName,
      config: {}
    }
  })

  selectNode(node)
}

const selectNode = (node) => {
  // 清除之前的选中状态
  graph.getNodes().forEach(n => {
    n.setAttrs({
      body: { strokeWidth: 2 }
    })
  })

  // 设置选中状态
  node.setAttrs({
    body: { strokeWidth: 3 }
  })

  const nodeData = node.getData()
  selectedNode.value = {
    id: node.id,
    type: nodeData.type,
    name: nodeData.name
  }

  // 加载节点配置
  Object.keys(nodeConfig).forEach(key => delete nodeConfig[key])
  Object.assign(nodeConfig, nodeData.config || {})
}

const loadGraphData = (json) => {
  try {
    const data = typeof json === 'string' ? JSON.parse(json) : json
    if (!graph || !data.nodes) return

    const colorMap = {
      'SYNC': '#67C23A',
      'TRANSFORM': '#409EFF',
      'SCRIPT': '#E6A23C',
      'CONDITION': '#F56C6C',
      'LOOP': '#909399'
    }

    // 加载节点
    data.nodes.forEach(nodeData => {
      graph.addNode({
        id: nodeData.id,
        shape: 'rect',
        x: nodeData.x || 100,
        y: nodeData.y || 100,
        width: 120,
        height: 50,
        attrs: {
          body: {
            fill: colorMap[nodeData.type] || '#409EFF',
            stroke: '#fff',
            strokeWidth: 2,
            rx: 8,
            ry: 8
          },
          label: {
            text: nodeData.name,
            fill: '#fff',
            fontSize: 12,
            fontWeight: 'bold'
          }
        },
        data: {
          type: nodeData.type,
          name: nodeData.name,
          config: nodeData.config || {}
        }
      })
    })

    // 加载边
    if (data.edges) {
      data.edges.forEach(edge => {
        graph.addEdge({
          id: edge.id,
          source: edge.source,
          target: edge.target,
          attrs: {
            line: {
              stroke: '#409eff',
              strokeWidth: 2,
              targetMarker: {
                name: 'block',
                width: 12,
                height: 8
              }
            }
          }
        })
      })
    }
  } catch (e) {
    console.error('加载工作流数据失败:', e)
  }
}

const handleDragStart = (e, node) => {
  e.dataTransfer.setData('nodeType', node.type)
}

const saveWorkflow = async () => {
  saving.value = true
  try {
    // 获取画布数据
    const nodes = graph.getNodes().map(node => ({
      id: node.id,
      type: node.getData().type,
      name: node.getData().name,
      x: node.getPosition().x,
      y: node.getPosition().y,
      config: node.getData().config || {}
    }))

    const edges = graph.getEdges().map(edge => ({
      id: edge.id,
      source: edge.getSourceCellId(),
      target: edge.getTargetCellId()
    }))

    const data = { nodes, edges }
    workflowForm.workflowJson = JSON.stringify(data)

    if (workflowForm.id) {
      await workflowAPI.update(workflowForm.id, workflowForm)
    } else {
      await workflowAPI.create(workflowForm)
    }
    ElMessage.success('保存成功')
    editorVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteSelectedNode = () => {
  if (selectedNode.value && graph) {
    const node = graph.getCellById(selectedNode.value.id)
    if (node) {
      graph.removeCell(node)
    }
    selectedNode.value = null
  }
}
</script>

<style lang="scss" scoped>
.workflow-page {
  display: flex;
  height: calc(100vh - 100px);
  gap: 16px;
  padding: 20px;
  background: #f5f7fa;
}

.folder-panel {
  width: 260px;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;

    .title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: 8px;

    .node-count {
      margin-left: auto;
      font-size: 12px;
      color: #909399;
    }
  }
}

.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .data-card {
    flex: 1;
    overflow: hidden;

    :deep(.el-card__body) {
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .el-table {
      flex: 1;
    }

    .pagination {
      margin-top: 16px;
      justify-content: flex-end;
    }
  }
}

.workflow-name {
  display: flex;
  align-items: center;
  gap: 8px;

  .name-text {
    font-weight: 500;
  }
}

.workflow-info-bar {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.workflow-editor {
  display: flex;
  height: 70vh;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;

  .node-panel {
    width: 200px;
    background: #fafafa;
    padding: 16px;
    border-right: 1px solid #ebeef5;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 16px;
      color: #303133;
    }

    .node-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px 8px;
      margin-bottom: 12px;
      background: #fff;
      border-radius: 8px;
      cursor: grab;
      border: 1px solid #ebeef5;
      transition: all 0.2s;

      &:hover {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
      }

      span {
        margin-top: 8px;
        font-size: 12px;
        color: #606266;
      }
    }
  }

  .canvas-container {
    flex: 1;
    background: #fff;
    position: relative;
  }

  .config-panel {
    width: 300px;
    background: #fafafa;
    padding: 16px;
    border-left: 1px solid #ebeef5;
    overflow-y: auto;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 16px;
      color: #303133;
    }
  }
}
</style>
