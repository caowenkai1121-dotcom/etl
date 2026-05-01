<template>
  <div class="pipeline-home">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1 class="welcome-title">Real-Time Pipeline</h1>
        <p class="welcome-desc">实时CDC数据管道，支持MySQL/Oracle/PG等数据库增量同步到Doris/ES/Kafka等目标端</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" @click="handleCreatePipeline">
            <el-icon><Plus /></el-icon> 创建管道任务
          </el-button>
          <el-button size="large" @click="$router.push('/datasource')">
            <el-icon><Coin /></el-icon> 管理数据源
          </el-button>
          <el-button size="large" @click="$router.push('/execution')">
            <el-icon><Timer /></el-icon> 查看执行记录
          </el-button>
        </div>
      </div>
      <div class="welcome-illustration">
        <div class="illustration-circle c1"></div>
        <div class="illustration-circle c2"></div>
        <div class="illustration-circle c3"></div>
        <div class="illustration-line l1"></div>
        <div class="illustration-line l2"></div>
      </div>
    </div>

    <!-- 概览统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap blue">
              <el-icon :size="20"><Connection /></el-icon>
            </div>
            <span class="stat-trend up">+3 <el-icon><Top /></el-icon></span>
          </div>
          <div class="stat-value">{{ overview.runningCount }}</div>
          <div class="stat-label">运行中管道</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap cyan">
              <el-icon :size="20"><Clock /></el-icon>
            </div>
            <span class="stat-info">总容量 {{ overview.capacity }}</span>
          </div>
          <div class="stat-value">{{ overview.remainingCapacity }}</div>
          <div class="stat-label">剩余任务容量</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap purple">
              <el-icon :size="20"><User /></el-icon>
            </div>
            <span class="stat-info">本周</span>
          </div>
          <div class="stat-value">{{ overview.myCreated }}</div>
          <div class="stat-label">我创建的管道</div>
        </div>
      </el-col>
    </el-row>

    <!-- 操作流程指引 -->
    <div class="guide-section">
      <div class="section-title-bar">
        <h3>管道操作指引</h3>
        <el-button text size="small" @click="guideCollapsed = !guideCollapsed">
          {{ guideCollapsed ? '展开' : '收起' }}
          <el-icon><ArrowUp v-if="!guideCollapsed" /><ArrowDown v-else /></el-icon>
        </el-button>
      </div>
      <el-collapse-transition>
        <div v-show="!guideCollapsed" class="guide-steps">
          <div class="guide-step" v-for="(step, idx) in pipelineSteps" :key="idx">
            <div class="step-badge">{{ String(idx + 1).padStart(2, '0') }}</div>
            <div class="step-info">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-items">
                <span v-for="(item, i) in step.items" :key="i">{{ item }}</span>
              </div>
            </div>
            <el-icon v-if="idx < pipelineSteps.length - 1" class="step-next"><ArrowRight /></el-icon>
          </div>
        </div>
      </el-collapse-transition>
    </div>

    <!-- 任务列表区域 -->
    <div class="task-section">
      <div class="task-layout">
        <!-- 左侧文件夹树 -->
        <div class="folder-sidebar">
          <div class="folder-search">
            <el-input
              v-model="folderSearch"
              placeholder="搜索任务/文件夹"
              size="small"
              clearable
              @input="filterFolderTree"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <el-tree
            :data="filteredFolderTree"
            :props="{ children: 'children', label: 'label' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="true"
            @node-click="handleFolderClick"
            class="folder-tree"
          >
            <template #default="{ data }">
              <span class="folder-tree-node">
                <el-icon :size="14"><Folder /></el-icon>
                <span>{{ data.label }}</span>
                <span class="folder-count" v-if="data.count != null">{{ data.count }}</span>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- 右侧主内容 -->
        <div class="task-main">
          <div class="section-title-bar">
            <div class="tab-switcher">
              <span class="tab-item" :class="{ active: activeTab === 'recent' }" @click="activeTab = 'recent'">最近编辑</span>
              <span class="tab-item" :class="{ active: activeTab === 'favorites' }" @click="activeTab = 'favorites'">我的收藏</span>
              <span class="tab-item" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部管道</span>
            </div>
            <div class="title-actions">
              <el-checkbox v-model="relatedToMe" size="small" @change="loadTaskList">与我相关</el-checkbox>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索管道名称..."
                size="small"
                clearable
                style="width: 220px"
                @input="loadTaskList"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
          </div>

          <el-table
            :data="taskList"
            stripe
            v-loading="loading"
            highlight-current-row
            @row-click="handleOpenTask"
            class="task-table"
          >
            <el-table-column prop="name" label="管道名称" min-width="220">
              <template #default="{ row }">
                <div class="task-name-cell">
                  <el-icon :size="16" class="task-type-icon">
                    <component :is="getTaskIcon(row.type)" />
                  </el-icon>
                  <span class="task-name">{{ row.name }}</span>
                  <el-tag
                    v-if="row.status"
                    :type="getStatusType(row.status)"
                    size="small"
                    effect="plain"
                  >{{ row.status }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="folderPath" label="所属目录" min-width="160">
              <template #default="{ row }">
                <span class="folder-path">
                  <el-icon :size="14"><Folder /></el-icon> {{ row.folderPath || '根目录' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="syncType" label="同步类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="plain">{{ row.syncType || 'CDC实时' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="数据源" width="160">
              <template #default="{ row }">
                <span class="source-target">
                  <span>{{ row.source || '-' }}</span>
                  <el-icon :size="12"><ArrowRight /></el-icon>
                  <span>{{ row.target || '-' }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="创建人" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="160" />
            <el-table-column prop="updatedBy" label="最近编辑" width="120" />
            <el-table-column prop="updatedAt" label="最后编辑时间" width="160" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <div class="row-actions" @click.stop>
                  <el-tooltip content="编辑" placement="top">
                    <el-button link type="primary" size="small" @click="handleOpenTask(row)">
                      <el-icon :size="16"><Edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="启动" placement="top">
                    <el-button link type="success" size="small" @click="handleRunPipeline(row)">
                      <el-icon :size="16"><CaretRight /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-dropdown trigger="click">
                    <el-button link size="small">
                      <el-icon :size="16"><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="handleCopyTask(row)">复制</el-dropdown-item>
                        <el-dropdown-item @click="handlePublishTask(row)">发布</el-dropdown-item>
                        <el-dropdown-item divided @click="handleDeleteTask(row)" style="color:#ff4d4f">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-footer">
            <span class="footer-info">显示最近 {{ taskList.length }} 条记录</span>
            <el-pagination
              v-if="total > 10"
              small
              layout="prev, pager, next"
              :total="total"
              :page-size="10"
              v-model:current-page="currentPage"
              @current-change="loadTaskList"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 创建管道任务对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建实时管道任务" width="600px" destroy-on-close>
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="管道名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入管道任务名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="同步类型">
          <el-radio-group v-model="createForm.syncType" size="small">
            <el-radio-button value="CDC">CDC实时同步</el-radio-button>
            <el-radio-button value="LOG">日志增量</el-radio-button>
            <el-radio-button value="BINLOG">Binlog解析</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="源端数据源" prop="sourceId">
          <el-select v-model="createForm.sourceId" placeholder="选择来源数据源" style="width:100%" filterable>
            <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标端数据源" prop="targetId">
          <el-select v-model="createForm.targetId" placeholder="选择目标数据源" style="width:100%" filterable>
            <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属目录">
          <el-select v-model="createForm.folderId" placeholder="选择目录" style="width:100%">
            <el-option label="根目录" :value="0" />
            <el-option label="00管道任务示例" :value="1" />
            <el-option label="生产环境管道" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入管道任务描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateConfirm" :loading="creating">确定创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { devAPI } from '@/api/dev'
import { getDatasourceList } from '@/api'

const router = useRouter()

const loading = ref(false)
const guideCollapsed = ref(false)
const activeTab = ref('recent')
const relatedToMe = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const total = ref(0)
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref(null)
const datasourceList = ref([])
const folderSearch = ref('')
const selectedFolderId = ref(null)

const folderTree = ref([
  { id: 1, label: '根目录', count: 12, children: [
    { id: 11, label: '00管道任务示例', count: 5 },
    { id: 12, label: '生产环境管道', count: 4 },
    { id: 13, label: '测试环境管道', count: 3 }
  ]},
  { id: 2, label: 'CDC实时同步', count: 8, children: [
    { id: 21, label: 'MySQL到Doris', count: 3 },
    { id: 22, label: 'Oracle到ES', count: 2 },
    { id: 23, label: 'PG到Kafka', count: 3 }
  ]},
  { id: 3, label: '离线T+1同步', count: 6, children: [
    { id: 31, label: '数仓ETL任务', count: 4 },
    { id: 32, label: '报表同步', count: 2 }
  ]},
  { id: 4, label: '归档备份', count: 3 }
])

const filteredFolderTree = ref([])

const filterFolderTreeData = (nodes, kw) => {
  if (!kw) return nodes
  const kl = kw.toLowerCase()
  return nodes.reduce((acc, node) => {
    const labelMatch = node.label.toLowerCase().includes(kl)
    const filteredChildren = node.children ? filterFolderTreeData(node.children, kw) : []
    if (labelMatch || filteredChildren.length > 0) {
      acc.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
    }
    return acc
  }, [])
}

const filterFolderTree = () => {
  filteredFolderTree.value = filterFolderTreeData(folderTree.value, folderSearch.value)
}

const handleFolderClick = (data) => {
  selectedFolderId.value = data.id
  loadTaskList()
}

const pipelineSteps = [
  { title: '环境准备', items: ['准备数据库、网络等资源', '检查数据源连接状态'] },
  { title: '选择数据源', items: ['选择来源端、去向端数据源', '配置数据源连接信息'] },
  { title: '表字段映射', items: ['选择来源表、目标表', '配置字段映射规则'] },
  { title: '管道控制', items: ['设置管道资源配置', '配置同步速率限制'] },
  { title: '任务运维', items: ['启动/停止管道任务', '查看监控与告警'] }
]

const overview = ref({
  runningCount: 5,
  remainingCapacity: 10,
  capacity: 15,
  myCreated: 3
})

const createForm = reactive({
  name: '',
  syncType: 'CDC',
  sourceId: null,
  targetId: null,
  folderId: 0,
  description: ''
})

const createRules = {
  name: [
    { required: true, message: '请输入管道名称', trigger: 'blur' },
    { min: 1, max: 50, message: '名称长度为1-50字符', trigger: 'blur' }
  ],
  sourceId: [{ required: true, message: '请选择来源数据源', trigger: 'change' }],
  targetId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }]
}

const taskList = ref([])

const getStatusType = (s) => {
  const map = { '运行中': 'success', '已停止': 'info', '异常': 'danger', '配置中': 'warning' }
  return map[s] || ''
}
const getTaskIcon = (t) => {
  const map = { CDC: 'Connection', LOG: 'Document', BINLOG: 'DataAnalysis' }
  return map[t] || 'Connection'
}

const loadTaskList = async () => {
  loading.value = true
  try {
    const res = await devAPI.getTaskList({
      pageNum: currentPage.value,
      pageSize: 10,
      tab: activeTab.value,
      relatedToMe: relatedToMe.value,
      name: searchKeyword.value || undefined,
      type: 'PIPELINE'
    })
    taskList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载管道任务列表失败', e)
    // fallback
  } finally {
    loading.value = false
  }
}

const loadOverview = async () => {
  try {
    const res = await devAPI.getDevStats()
    if (res.data) overview.value = { ...overview.value, ...res.data }
  } catch (e) { /* fallback defaults */ }
}

onMounted(async () => {
  filteredFolderTree.value = filterFolderTreeData(folderTree.value, '')
  try {
    const dsRes = await getDatasourceList()
    datasourceList.value = dsRes.data || []
  } catch (_) {}
  loadOverview()
  loadTaskList()
})

const handleCreatePipeline = () => {
  createForm.name = ''
  createForm.syncType = 'CDC'
  createForm.sourceId = null
  createForm.targetId = null
  createForm.folderId = 0
  createForm.description = ''
  createDialogVisible.value = true
}

const handleCreateConfirm = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true
    await devAPI.createTask({
      name: createForm.name,
      type: 'PIPELINE',
      folderId: createForm.folderId || 0,
      description: createForm.description,
      syncType: createForm.syncType,
      sourceId: createForm.sourceId,
      targetId: createForm.targetId
    })
    ElMessage.success('管道任务创建成功')
    createDialogVisible.value = false
    loadTaskList()
    loadOverview()
  } catch (e) {
    // validation error or API error
  } finally {
    creating.value = false
  }
}

const handleOpenTask = (row) => {
  router.push(`/dev/task/${row.id}`)
}

const handleRunPipeline = async (row) => {
  try {
    await ElMessageBox.confirm(`确定启动管道任务 "${row.name}"？`, '提示', { type: 'info' })
    await devAPI.runTask(row.id)
    ElMessage.success('管道任务已启动运行')
  } catch (e) { /* cancel */ }
}

const handleCopyTask = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新管道名称', '复制管道', {
      inputValue: row.name + '_副本',
      inputPattern: /^.{1,50}$/,
      inputErrorMessage: '名称长度为1-50字符'
    })
    if (value) {
      await devAPI.copyTask(row.id, { name: value })
      ElMessage.success('复制成功')
      loadTaskList()
    }
  } catch (e) { /* cancel */ }
}

const handlePublishTask = async (row) => {
  try {
    await ElMessageBox.confirm(`确定发布管道 "${row.name}"？`, '发布确认', { type: 'info' })
    await devAPI.publishTask(row.id)
    ElMessage.success('发布成功')
    loadTaskList()
  } catch (e) { /* cancel */ }
}

const handleDeleteTask = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除管道 "${row.name}"？此操作不可恢复。`, '警告', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
    await devAPI.deleteTask(row.id)
    ElMessage.success('已删除')
    loadTaskList()
    loadOverview()
  } catch (e) { /* cancel */ }
}
</script>

<style lang="scss" scoped>
.pipeline-home {
  padding: 4px;
}

// 欢迎横幅
.welcome-banner {
  background: linear-gradient(135deg, #13c2c2 0%, #08979c 50%, #006d75 100%);
  border-radius: 16px;
  padding: 32px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;

  .welcome-content {
    position: relative;
    z-index: 2;
    .welcome-title { color: #fff; font-size: 26px; font-weight: 700; margin: 0 0 8px; }
    .welcome-desc { color: rgba(255,255,255,0.85); font-size: 14px; margin: 0 0 20px; max-width: 560px; }
    .welcome-actions { display: flex; gap: 12px; }
  }

  .welcome-illustration {
    position: absolute;
    right: 60px;
    top: 50%;
    transform: translateY(-50%);
    width: 200px;
    height: 120px;

    .illustration-circle {
      position: absolute;
      border-radius: 50%;
      &.c1 { width: 60px; height: 60px; background: rgba(255,255,255,0.15); top: 0; left: 50px; }
      &.c2 { width: 40px; height: 40px; background: rgba(255,255,255,0.1); top: 30px; right: 20px; }
      &.c3 { width: 80px; height: 80px; background: rgba(255,255,255,0.08); bottom: 0; left: 0; }
    }
    .illustration-line {
      position: absolute;
      background: rgba(255,255,255,0.2);
      &.l1 { width: 120px; height: 2px; top: 40px; left: 30px; transform: rotate(-15deg); }
      &.l2 { width: 90px; height: 2px; top: 60px; left: 60px; transform: rotate(10deg); }
    }
  }
}

// 统计卡片
.stats-row { margin-bottom: 20px; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid #f0f0f0;

  &:hover {
    box-shadow: 0 6px 20px rgba(0,0,0,0.08);
    transform: translateY(-2px);
  }

  .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .stat-icon-wrap {
    width: 40px; height: 40px;
    border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    &.blue { background: #e6f4ff; color: #1890ff; }
    &.cyan { background: #e6fffb; color: #13c2c2; }
    &.purple { background: #f9f0ff; color: #722ed1; }
  }

  .stat-trend {
    font-size: 12px; font-weight: 500;
    display: flex; align-items: center; gap: 2px;
    &.up { color: #52c41a; }
  }
  .stat-info { font-size: 12px; color: #999; }

  .stat-value { font-size: 32px; font-weight: 700; color: #1a1a1a; line-height: 1.2; }
  .stat-label { font-size: 13px; color: #999; margin-top: 4px; }
}

// 操作指引
.guide-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  border: 1px solid #f0f0f0;
}

.section-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h3 { font-size: 16px; font-weight: 600; color: #1a1a1a; margin: 0; }
}

.guide-steps {
  display: flex; gap: 12px;

  .guide-step {
    flex: 1; display: flex; align-items: center; gap: 14px;
    padding: 18px 16px; background: #fafafa;
    border-radius: 10px; cursor: default;
    transition: all 0.25s; border: 1px solid transparent;

    &:hover {
      background: #e6fffb; border-color: #87e8de;
      .step-next { color: #13c2c2; }
    }
  }

  .step-badge {
    width: 44px; height: 44px;
    border-radius: 12px; background: linear-gradient(135deg, #13c2c2, #36cfc9);
    color: #fff; font-size: 18px; font-weight: 700;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
  }
  .step-info {
    flex: 1; min-width: 0;
    .step-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 6px; }
    .step-items {
      display: flex; flex-direction: column; gap: 2px;
      span { font-size: 12px; color: #888; }
    }
  }
  .step-next { color: #ccc; font-size: 18px; flex-shrink: 0; }
}

// 任务列表
.task-section {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  border: 1px solid #f0f0f0;

  .section-title-bar {
    .title-actions { display: flex; align-items: center; gap: 12px; }
  }
}

.task-layout {
  display: flex; gap: 20px;
}

// 左侧文件夹树
.folder-sidebar {
  width: 220px; flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
  padding-right: 16px;
}

.folder-search {
  margin-bottom: 12px;
}

.folder-tree {
  :deep(.el-tree-node__content) {
    height: 32px; border-radius: 6px;
    &:hover { background: #f5f7fa; }
  }
  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: #e6fffb; color: #13c2c2;
  }
}

.folder-tree-node {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #333;
  .el-icon { color: #faad14; flex-shrink: 0; }
}

.folder-count {
  margin-left: auto;
  font-size: 11px; color: #bbb; background: #f5f5f5;
  padding: 1px 6px; border-radius: 8px;
}

.task-main {
  flex: 1; min-width: 0;
}

.tab-switcher {
  display: flex; gap: 0;
  background: #f5f7fa; border-radius: 8px; padding: 3px;

  .tab-item {
    padding: 6px 16px; border-radius: 6px; font-size: 13px; font-weight: 500;
    color: #666; cursor: pointer; transition: all 0.2s;
    &.active { background: #fff; color: #13c2c2; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
    &:hover:not(.active) { color: #333; }
  }
}

.task-name-cell {
  display: flex; align-items: center; gap: 8px;
  .task-type-icon { color: #13c2c2; flex-shrink: 0; }
  .task-name { color: #13c2c2; cursor: pointer; font-weight: 500; }
}

.folder-path { color: #999; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.source-target { display: flex; align-items: center; gap: 4px; color: #666; font-size: 12px; }
.row-actions { display: flex; align-items: center; gap: 2px; }

.table-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid #f5f5f5;
  .footer-info { font-size: 12px; color: #999; }
}

.task-table {
  :deep(.el-table__row) { cursor: pointer; }
}
</style>
