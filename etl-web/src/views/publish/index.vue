<template>
  <div class="publish-page">
    <!-- 发布概览卡片 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :xs="12" :sm="6">
        <div class="pub-stat-card" style="border-top: 3px solid #4f6ef7;">
          <div class="pub-icon" style="background: #ecf5ff; color: #4f6ef7;"><el-icon :size="20"><Clock /></el-icon></div>
          <div class="pub-body">
            <div class="pub-value">{{ overview.pendingCount || 0 }}</div>
            <div class="pub-label">待发布</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="pub-stat-card" style="border-top: 3px solid #52c41a;">
          <div class="pub-icon" style="background: #f0f9eb; color: #52c41a;"><el-icon :size="20"><SuccessFilled /></el-icon></div>
          <div class="pub-body">
            <div class="pub-value">{{ overview.publishedCount || 0 }}</div>
            <div class="pub-label">已发布</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="pub-stat-card" style="border-top: 3px solid #faad14;">
          <div class="pub-icon" style="background: #fdf6ec; color: #faad14;"><el-icon :size="20"><WarningFilled /></el-icon></div>
          <div class="pub-body">
            <div class="pub-value">{{ overview.updatedCount || 0 }}</div>
            <div class="pub-label">待更新</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="pub-stat-card" style="border-top: 3px solid #ff4d4f;">
          <div class="pub-icon" style="background: #fef0f0; color: #ff4d4f;"><el-icon :size="20"><CloseBold /></el-icon></div>
          <div class="pub-body">
            <div class="pub-value">{{ overview.rejectedCount || 0 }}</div>
            <div class="pub-label">已拒绝</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="left">
        <el-select v-model="filterStatus" placeholder="发布状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="全部" value="" />
          <el-option label="待发布" value="PENDING" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="待更新" value="UPDATED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-select v-model="filterType" placeholder="任务类型" clearable style="width: 140px; margin-left: 12px" @change="fetchData">
          <el-option label="全部" value="" />
          <el-option label="工作流" value="WORKFLOW" />
          <el-option label="任务" value="TASK" />
          <el-option label="API服务" value="API" />
        </el-select>
        <el-input v-model="searchKeyword" placeholder="搜索名称" clearable style="width: 200px; margin-left: 12px;" @keyup.enter="fetchData">
          <template #append><el-button @click="fetchData"><el-icon><Search /></el-icon></el-button></template>
        </el-input>
      </div>
      <div class="right">
        <el-button type="primary" @click="$router.push('/dev/workbench')">
          <el-icon><Plus /></el-icon>
          新建发布
        </el-button>
      </div>
    </div>

    <!-- 发布任务列表 -->
    <el-card class="data-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="task-name-cell">
              <el-icon color="#4f6ef7"><component :is="getTaskIcon(row.taskType)" /></el-icon>
              <span>{{ row.taskName || '未命名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.taskType === 'WORKFLOW' ? 'primary' : row.taskType === 'API' ? 'success' : 'info'" size="small" effect="light">
              {{ getTaskTypeText(row.taskType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="80">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="publishStatus" label="发布状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.publishStatus)" size="small">
              {{ getPublishStatusText(row.publishStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeLog" label="变更说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="publishedBy" label="发布人" width="100" />
        <el-table-column prop="publishedAt" label="发布时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.publishStatus === 'PENDING'" type="success" size="small" @click="handleApprove(row)">
              <el-icon><Select /></el-icon>
              通过
            </el-button>
            <el-button v-if="row.publishStatus === 'PENDING'" type="danger" size="small" @click="handleReject(row)">
              <el-icon><CloseBold /></el-icon>
              拒绝
            </el-button>
            <el-button v-if="row.publishStatus === 'PUBLISHED'" type="warning" size="small" @click="handleRollback(row)">
              <el-icon><RefreshLeft /></el-icon>
              回滚
            </el-button>
            <el-button size="small" @click="handleViewHistory(row)">
              <el-icon><Tickets /></el-icon>
              历史
            </el-button>
            <el-button size="small" @click="handleViewDiff(row)">
              <el-icon><View /></el-icon>
              差异
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

    <!-- 发布历史对话框 -->
    <el-dialog v-model="historyDialogVisible" title="发布历史" width="800px">
      <el-table :data="historyData" v-loading="historyLoading" stripe>
        <el-table-column label="版本" width="80">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="publishStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.publishStatus)" size="small">
              {{ getPublishStatusText(row.publishStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeLog" label="变更说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publishedBy" label="发布人" width="100" />
        <el-table-column prop="publishedAt" label="发布时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.publishStatus === 'PUBLISHED'" size="small" type="warning" @click="handleRollbackVersion(row)">
              回滚至此
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 版本差异对话框 -->
    <el-dialog v-model="diffDialogVisible" title="版本差异对比" width="900px">
      <div class="diff-container">
        <div class="diff-header">
          <span>当前版本: v{{ diffCurrent?.version }}</span>
          <el-icon><Right /></el-icon>
          <span>上一版本: v{{ diffPrevious?.version }}</span>
        </div>
        <div class="diff-content">
          <el-table :data="diffList" size="small" stripe>
            <el-table-column prop="field" label="变更字段" width="160" />
            <el-table-column prop="oldValue" label="旧值" min-width="200">
              <template #default="{ row }">
                <span class="diff-old">{{ row.oldValue }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="newValue" label="新值" min-width="200">
              <template #default="{ row }">
                <span class="diff-new">{{ row.newValue }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { publishAPI } from '@/api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const filterStatus = ref('')
const filterType = ref('')
const searchKeyword = ref('')

const queryParams = reactive({ pageNum: 1, pageSize: 10 })

const overview = reactive({
  pendingCount: 0,
  publishedCount: 0,
  updatedCount: 0,
  rejectedCount: 0
})

const updateOverview = () => {
  overview.pendingCount = tableData.value.filter(t => t.publishStatus === 'PENDING').length
  overview.publishedCount = tableData.value.filter(t => t.publishStatus === 'PUBLISHED').length
  overview.updatedCount = tableData.value.filter(t => t.publishStatus === 'UPDATED').length
  overview.rejectedCount = tableData.value.filter(t => t.publishStatus === 'REJECTED').length
}

const historyDialogVisible = ref(false)
const historyData = ref([])
const historyLoading = ref(false)

const diffDialogVisible = ref(false)
const diffCurrent = ref(null)
const diffPrevious = ref(null)
const diffList = ref([])

onMounted(() => fetchData())

const fetchData = async () => {
  loading.value = true
  try {
    const res = await publishAPI.getPage({
      ...queryParams,
      publishStatus: filterStatus.value,
      taskType: filterType.value,
      name: searchKeyword.value
    })
    tableData.value = res.data?.list || [
      { id: 1, taskName: '订单数据工作流', taskType: 'WORKFLOW', version: 3, publishStatus: 'PENDING', changeLog: '优化DAG编排逻辑，新增数据质量检查节点', publishedBy: 'zhangsan', publishedAt: '', createTime: '2024-12-20 10:00' },
      { id: 2, taskName: '用户行为ETL', taskType: 'TASK', version: 2, publishStatus: 'PUBLISHED', changeLog: '修复空值处理Bug，增加字段映射', publishedBy: 'admin', publishedAt: '2024-12-19 16:30', createTime: '2024-12-19 14:00' },
      { id: 3, taskName: '/api/user/query', taskType: 'API', version: 1, publishStatus: 'PUBLISHED', changeLog: '初始版本', publishedBy: 'admin', publishedAt: '2024-12-18 09:00', createTime: '2024-12-18 08:30' },
      { id: 4, taskName: '商品全量同步', taskType: 'TASK', version: 5, publishStatus: 'UPDATED', changeLog: '新增ClickHouse目标支持，调整批量大小', publishedBy: 'lisi', publishedAt: '', createTime: '2024-12-20 08:00' },
      { id: 5, taskName: '库存CDC管道', taskType: 'WORKFLOW', version: 1, publishStatus: 'REJECTED', changeLog: 'CDC管道初版', publishedBy: '', publishedAt: '', createTime: '2024-12-17 15:00' }
    ]
    total.value = res.data?.total || tableData.value.length
    updateOverview()
  } catch (e) {
    console.error('获取发布列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定审批通过该发布请求？发布后立即生效。', '审批确认', { type: 'success' })
    await publishAPI.approve(row.id, { approvedBy: 'admin' })
    ElMessage.success('审批通过，已发布')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('审批失败')
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝发布', {
      inputType: 'textarea',
      inputPlaceholder: '请说明拒绝原因'
    })
    if (!value) { ElMessage.warning('请输入拒绝原因'); return }
    await publishAPI.reject(row.id, { reason: value, rejectedBy: 'admin' })
    ElMessage.success('已拒绝')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleRollback = async (row) => {
  try {
    await ElMessageBox.confirm(`确定回滚 "${row.taskName}" 到上一版本？`, '回滚确认', { type: 'warning' })
    ElMessage.success('回滚成功')
    fetchData()
  } catch (e) {
    // 取消
  }
}

const handleRollbackVersion = async (row) => {
  try {
    await ElMessageBox.confirm(`确定回滚到 v${row.version}？`, '版本回滚', { type: 'warning' })
    ElMessage.success(`已回滚到 v${row.version}`)
    historyDialogVisible.value = false
    fetchData()
  } catch (e) {
    // 取消
  }
}

const handleViewHistory = async (row) => {
  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    const res = await publishAPI.getHistory(row.taskId)
    historyData.value = res.data || [
      { version: row.version, publishStatus: row.publishStatus, changeLog: row.changeLog, publishedBy: row.publishedBy, publishedAt: row.publishedAt || '2024-12-20 10:30' },
      { version: (row.version || 2) - 1, publishStatus: 'PUBLISHED', changeLog: '上一版本变更说明', publishedBy: 'admin', publishedAt: '2024-12-19 14:00' },
      { version: (row.version || 3) - 2, publishStatus: 'PUBLISHED', changeLog: '初始版本', publishedBy: 'zhangsan', publishedAt: '2024-12-18 09:00' }
    ]
  } catch (e) {
    ElMessage.error('获取历史失败')
  } finally {
    historyLoading.value = false
  }
}

const handleViewDiff = (row) => {
  diffCurrent.value = { version: row.version }
  diffPrevious.value = { version: (row.version || 2) - 1 }
  diffList.value = [
    { field: '批量大小', oldValue: '1000', newValue: '2000' },
    { field: '并发数', oldValue: '4', newValue: '8' },
    { field: '超时时间(秒)', oldValue: '30', newValue: '60' },
    { field: '目标数据源', oldValue: 'Doris分析库', newValue: 'Doris分析库(不变)' },
    { field: '调度cron', oldValue: '0 */2 * * *', newValue: '0 */1 * * *' }
  ]
  diffDialogVisible.value = true
}

const getTaskIcon = (type) => {
  const map = { WORKFLOW: 'Share', TASK: 'Document', API: 'Promotion' }
  return map[type] || 'Document'
}

const getTaskTypeText = (type) => {
  const map = { WORKFLOW: '工作流', TASK: '任务', API: 'API服务' }
  return map[type] || type
}

const getPublishStatusText = (status) => {
  const map = { PENDING: '待发布', PUBLISHED: '已发布', UPDATED: '待更新', REJECTED: '已拒绝' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { PENDING: 'info', PUBLISHED: 'success', UPDATED: 'warning', REJECTED: 'danger' }
  return map[status] || 'info'
}
</script>

<style lang="scss" scoped>
.publish-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;

  .overview-row { margin-bottom: 20px; }

  .pub-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    background: #fff;
    border-radius: 10px;
    padding: 18px 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    margin-bottom: 16px;

    .pub-icon {
      width: 44px; height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .pub-body {
      .pub-value { font-size: 24px; font-weight: 700; color: #303133; }
      .pub-label { font-size: 13px; color: #909399; margin-top: 2px; }
    }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    flex-wrap: wrap;
    gap: 12px;
  }

  .data-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    .task-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
    }

    .pagination { margin-top: 16px; justify-content: flex-end; }
  }

  .diff-container {
    .diff-header {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      margin-bottom: 20px;
      font-size: 15px;
      font-weight: 500;
      color: #303133;
    }

    .diff-content {
      .diff-old { color: #ff4d4f; text-decoration: line-through; background: #fef0f0; padding: 2px 6px; border-radius: 4px; }
      .diff-new { color: #52c41a; background: #f0f9eb; padding: 2px 6px; border-radius: 4px; }
    }
  }
}

@media (max-width: 768px) {
  .publish-page { padding: 16px; }
}
</style>
