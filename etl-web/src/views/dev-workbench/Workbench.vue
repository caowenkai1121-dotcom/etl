<template>
  <div class="workbench-home">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1 class="welcome-title">数据开发工作台</h1>
        <p class="welcome-desc">一站式数据集成开发环境，支持可视化ETL任务设计、调度配置与运维监控</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" @click="$router.push('/dev/task/new')">
            <el-icon><Plus /></el-icon> 新建任务
          </el-button>
          <el-button size="large" @click="$router.push('/datasource')">
            <el-icon><Coin /></el-icon> 新建数据源
          </el-button>
          <el-button size="large" @click="$router.push('/scheduler')">
            <el-icon><Timer /></el-icon> 查看调度
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

    <!-- 任务类型切换 -->
    <div class="task-type-switch">
      <el-radio-group v-model="taskSubType" @change="handleTaskTypeChange" size="default">
        <el-radio-button value="scheduled">离线任务</el-radio-button>
        <el-radio-button value="realtime">实时任务</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 概览统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" @click="$router.push('/publish')">
          <div class="stat-header">
            <div class="stat-icon-wrap blue">
              <el-icon :size="20"><SuccessFilled /></el-icon>
            </div>
            <span class="stat-trend up">+12 <el-icon><Top /></el-icon></span>
          </div>
          <div class="stat-value">{{ overview.published }}</div>
          <div class="stat-label">已发布任务</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" @click="$router.push('/dev/workbench')">
          <div class="stat-header">
            <div class="stat-icon-wrap orange">
              <el-icon :size="20"><Clock /></el-icon>
            </div>
            <span class="stat-trend up">+5 <el-icon><Top /></el-icon></span>
          </div>
          <div class="stat-value">{{ overview.pending }}</div>
          <div class="stat-label">待发布任务</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap cyan">
              <el-icon :size="20"><Refresh /></el-icon>
            </div>
            <span class="stat-trend down">-3 <el-icon><Bottom /></el-icon></span>
          </div>
          <div class="stat-value">{{ overview.updated }}</div>
          <div class="stat-label">待更新任务</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap purple">
              <el-icon :size="20"><User /></el-icon>
            </div>
            <span class="stat-info">本周</span>
          </div>
          <div class="stat-value">{{ overview.myCreated }}</div>
          <div class="stat-label">我创建的任务</div>
        </div>
      </el-col>
    </el-row>

    <!-- 操作流程指引 -->
    <div class="guide-section">
      <div class="section-title-bar">
        <h3>操作流程指引</h3>
        <el-button text size="small" @click="guideCollapsed = !guideCollapsed">
          {{ guideCollapsed ? '展开' : '收起' }}
          <el-icon><ArrowUp v-if="!guideCollapsed" /><ArrowDown v-else /></el-icon>
        </el-button>
      </div>
      <el-collapse-transition>
        <div v-show="!guideCollapsed" class="guide-steps">
          <div class="guide-step" v-for="(step, idx) in guideSteps" :key="idx" @click="goToStep(step.target)">
            <div class="step-badge">{{ String(idx + 1).padStart(2, '0') }}</div>
            <div class="step-info">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-items">
                <span v-for="(item, i) in step.items" :key="i">{{ item }}</span>
              </div>
            </div>
            <el-icon class="step-next"><ArrowRight /></el-icon>
          </div>
        </div>
      </el-collapse-transition>
    </div>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="quick-actions-row">
      <el-col :span="8">
        <div class="quick-action-card" @click="$router.push('/dev/task/new')">
          <div class="qa-icon sql">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div class="qa-info">
            <div class="qa-title">SQL脚本任务</div>
            <div class="qa-desc">编写和执行SQL数据处理脚本</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="quick-action-card" @click="$router.push('/scheduler/dag')">
          <div class="qa-icon flow">
            <el-icon :size="24"><Connection /></el-icon>
          </div>
          <div class="qa-info">
            <div class="qa-title">DAG任务编排</div>
            <div class="qa-desc">可视化拖拽设计数据流程</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="quick-action-card" @click="$router.push('/api-service')">
          <div class="qa-icon api">
            <el-icon :size="24"><Promotion /></el-icon>
          </div>
          <div class="qa-info">
            <div class="qa-title">API数据服务</div>
            <div class="qa-desc">快速构建数据API接口</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 任务列表区域 -->
    <div class="task-section">
      <div class="section-title-bar">
        <div class="tab-switcher">
          <span
            class="tab-item"
            :class="{ active: activeTab === 'recent' }"
            @click="activeTab = 'recent'"
          >最近编辑</span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'favorites' }"
            @click="activeTab = 'favorites'"
          >我的收藏</span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'all' }"
            @click="activeTab = 'all'"
          >全部任务</span>
        </div>
        <div class="title-actions">
          <el-checkbox v-model="relatedToMe" size="small" @change="loadTaskList">只看我相关</el-checkbox>
          <el-input
            v-model="searchTaskName"
            placeholder="搜索任务..."
            size="small"
            clearable
            style="width: 200px"
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
        @row-click="openTask"
        class="task-table"
      >
        <el-table-column prop="name" label="任务名称" min-width="220">
          <template #default="{ row }">
            <div class="task-name-cell">
              <el-icon :size="16" class="task-type-icon">
                <component :is="getTaskIcon(row.type)" />
              </el-icon>
              <span class="task-name">{{ row.name }}</span>
              <el-tag
                v-if="row.publishStatus"
                :type="getStatusType(row.publishStatus)"
                size="small"
                effect="plain"
              >{{ getStatusText(row.publishStatus) }}</el-tag>
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
        <el-table-column prop="type" label="任务类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.type || 'ETL任务' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedBy" label="最近编辑" width="120" />
        <el-table-column prop="updatedAt" label="最后编辑时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastPublishTime" label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.lastPublishTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <div class="row-actions" @click.stop>
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" size="small" @click="openTask(row)">
                  <el-icon :size="16"><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="运行" placement="top">
                <el-button link type="success" size="small" @click="runTask(row)">
                  <el-icon :size="16"><CaretRight /></el-icon>
                </el-button>
              </el-tooltip>
              <el-dropdown trigger="click">
                <el-button link size="small">
                  <el-icon :size="16"><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="copyTask(row)">复制</el-dropdown-item>
                    <el-dropdown-item @click="publishTask(row)">发布</el-dropdown-item>
                    <el-dropdown-item divided @click="deleteTask(row)" style="color:#ff4d4f">删除</el-dropdown-item>
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { devAPI } from '@/api/dev'

const router = useRouter()

// 状态
const loading = ref(false)
const guideCollapsed = ref(false)
const activeTab = ref('recent')
const relatedToMe = ref(false)
const searchTaskName = ref('')
const taskSubType = ref('scheduled')
const currentPage = ref(1)
const total = ref(0)

const formatDateTime = (val) => {
  if (!val) return '-'
  try {
    const d = new Date(val)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch { return String(val) }
}

// 概览数据
const overview = ref({
  published: 179,
  pending: 106,
  updated: 44,
  myCreated: 12
})

// 操作指引
const guideSteps = [
  { title: '连接数据源', items: ['创建数据源连接', '验证表和字段'], target: '/datasource' },
  { title: '设计任务', items: ['新建ETL任务', '拖拽设计流程'], target: '/dev/task/new' },
  { title: '试运行调试', items: ['试运行任务', '查看输出数据'], target: '/dev/workbench' },
  { title: '调度配置', items: ['设置定时周期', '配置依赖关系'], target: '/scheduler' },
  { title: '运维监控', items: ['查看执行记录', '日志分析'], target: '/execution' }
]

// 任务列表
const taskList = ref([])

// 加载任务列表
const loadTaskList = async () => {
  loading.value = true
  try {
    const res = await devAPI.getTaskList({
      pageNum: currentPage.value,
      pageSize: 10,
      tab: activeTab.value,
      relatedToMe: relatedToMe.value,
      name: searchTaskName.value || undefined
    })
    taskList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载任务列表失败', e)
  } finally {
    loading.value = false
  }
}

// 加载概览统计
const loadOverview = async () => {
  try {
    const res = await devAPI.getDevStats()
    if (res.data) overview.value = { ...overview.value, ...res.data }
  } catch (e) { /* fallback defaults */ }
}

onMounted(() => {
  loadOverview()
  loadTaskList()
})

// 任务类型切换
const handleTaskTypeChange = (val) => {
  if (val === 'realtime') {
    router.push('/realtime/workbench')
  }
}

// 状态映射
const getStatusType = (s) => ({ PUBLISHED: 'success', PENDING: 'warning', UPDATED: 'info', DRAFT: '' })[s] || ''
const getStatusText = (s) => ({ PUBLISHED: '已发布', PENDING: '待发布', UPDATED: '待更新', DRAFT: '草稿' })[s] || s
const getTaskIcon = (t) => ({ SQL: 'Document', DAG: 'Connection', API: 'Promotion' })[t] || 'Calendar'

// 操作
const openTask = (row) => router.push(`/dev/task/${row.id}`)
const goToStep = (path) => { if (path) router.push(path) }

const runTask = async (row) => {
  try {
    await ElMessageBox.confirm(`确定运行任务 "${row.name}"？`, '提示', { type: 'info' })
    await devAPI.runTask(row.id)
    ElMessage.success('任务已开始运行')
  } catch (e) { /* cancel */ }
}

const copyTask = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新任务名称', '复制任务', {
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

const publishTask = async (row) => {
  try {
    await ElMessageBox.confirm(`确定发布任务 "${row.name}"？`, '发布确认', { type: 'info' })
    await devAPI.publishTask(row.id)
    ElMessage.success('发布成功')
    loadTaskList()
  } catch (e) { /* cancel */ }
}

const deleteTask = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除任务 "${row.name}"？此操作不可恢复。`, '警告', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
    await devAPI.deleteTask(row.id)
    ElMessage.success('已删除')
    loadTaskList()
  } catch (e) { /* cancel */ }
}
</script>

<style lang="scss" scoped>
.workbench-home {
  padding: 4px;
}

// 欢迎横幅
.welcome-banner {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 50%, #0050b3 100%);
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
    .welcome-desc { color: rgba(255,255,255,0.8); font-size: 14px; margin: 0 0 20px; }
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

// 统计卡片行
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
    &.orange { background: #fff7e6; color: #fa8c16; }
    &.cyan { background: #e6fffb; color: #13c2c2; }
    &.purple { background: #f9f0ff; color: #722ed1; }
  }

  .stat-trend {
    font-size: 12px; font-weight: 500;
    display: flex; align-items: center; gap: 2px;
    &.up { color: #52c41a; }
    &.down { color: #ff4d4f; }
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
    border-radius: 10px; cursor: pointer;
    transition: all 0.25s; border: 1px solid transparent;

    &:hover {
      background: #e6f4ff; border-color: #91caff;
      .step-next { color: #1890ff; }
    }
  }

  .step-badge {
    width: 44px; height: 44px;
    border-radius: 12px; background: linear-gradient(135deg, #1890ff, #40a9ff);
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
  .step-next { color: #ccc; font-size: 18px; }
}

// 快捷入口
.quick-actions-row { margin-bottom: 20px; }

.quick-action-card {
  background: #fff; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  cursor: pointer; transition: all 0.25s; border: 1px solid #f0f0f0;

  &:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }

  .qa-icon {
    width: 52px; height: 52px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
    &.sql { background: #e6f4ff; color: #1890ff; }
    &.flow { background: #f9f0ff; color: #722ed1; }
    &.api { background: #e6fffb; color: #13c2c2; }
  }

  .qa-info {
    .qa-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
    .qa-desc { font-size: 13px; color: #999; }
  }
}

// 任务列表区域
.task-section {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  border: 1px solid #f0f0f0;

  .section-title-bar {
    .title-actions { display: flex; align-items: center; gap: 12px; }
  }
}

.tab-switcher {
  display: flex; gap: 0;
  background: #f5f7fa; border-radius: 8px; padding: 3px;

  .tab-item {
    padding: 6px 16px; border-radius: 6px; font-size: 13px; font-weight: 500;
    color: #666; cursor: pointer; transition: all 0.2s;
    &.active { background: #fff; color: #1890ff; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
    &:hover:not(.active) { color: #333; }
  }
}

.task-name-cell {
  display: flex; align-items: center; gap: 8px;
  .task-type-icon { color: #1890ff; flex-shrink: 0; }
  .task-name { color: #1890ff; cursor: pointer; font-weight: 500; }
}

.folder-path { color: #999; font-size: 13px; display: flex; align-items: center; gap: 4px; }

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
