<template>
  <div class="recycle-bin-page">
    <!-- 概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="bin-stat-card">
          <div class="stat-value" style="color: #4f6ef7;">{{ recycleData.length }}</div>
          <div class="stat-label">待处理项</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="bin-stat-card">
          <div class="stat-value" style="color: #faad14;">{{ daysUntilCleanup }}</div>
          <div class="stat-label">天后自动清理</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="bin-stat-card">
          <div class="stat-value" style="color: #52c41a;">{{ expireDays }}</div>
          <div class="stat-label">保留天数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="bin-stat-card">
          <div class="stat-value" style="color: #ff4d4f;">{{ totalStorage }}</div>
          <div class="stat-label">占用存储</div>
        </div>
      </el-col>
    </el-row>

    <!-- 标签页切换 -->
    <el-card class="main-card">
      <el-tabs v-model="activeTab" @tab-change="fetchData">
        <el-tab-pane label="已删除任务" name="task">
          <template #label>
            <span>
              已删除任务
              <el-badge :value="taskData.length" :max="99" class="tab-badge" />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已删除文件夹" name="folder">
          <template #label>
            <span>
              已删除文件夹
              <el-badge :value="folderData.length" :max="99" class="tab-badge" />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已删除API" name="api">
          <template #label>
            <span>
              已删除API
              <el-badge :value="apiData.length" :max="99" class="tab-badge" />
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="left">
          <el-input v-model="searchName" placeholder="搜索名称" clearable style="width: 220px">
            <template #append><el-button @click="fetchData"><el-icon><Search /></el-icon></el-button></template>
          </el-input>
        </div>
        <div class="right">
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>
            永久删除 ({{ selectedRows.length }})
          </el-button>
          <el-button type="primary" :disabled="selectedRows.length === 0" @click="handleBatchRestore">
            <el-icon><RefreshLeft /></el-icon>
            批量恢复 ({{ selectedRows.length }})
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="currentData"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
        empty-text="回收站为空"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <div class="item-name">
              <el-icon><component :is="getItemIcon(row.type)" /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originalPath" label="原始路径" min-width="200">
          <template #default="{ row }">
            <div class="original-path">
              <el-icon><FolderOpened /></el-icon>
              <span>{{ row.originalPath }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deletedBy" label="删除人" width="100" />
        <el-table-column prop="deletedAt" label="删除时间" width="180" />
        <el-table-column label="剩余时间" width="160">
          <template #default="{ row }">
            <div class="remaining-time">
              <el-icon v-if="getRemainingDays(row) <= 3" color="#ff4d4f"><Warning /></el-icon>
              <span :class="{ 'expiring': getRemainingDays(row) <= 3 }">
                {{ getRemainingDays(row) }}天后自动清除
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleRestore(row)">
              <el-icon><RefreshLeft /></el-icon>
              恢复
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              永久删除
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
        class="pagination-wrap"
      />
    </el-card>

    <!-- 恢复确认 -->
    <el-dialog v-model="restoreDialogVisible" title="恢复确认" width="450px">
      <div class="restore-dialog">
        <div class="restore-warning" v-if="hasConflict">
          <el-alert type="warning" :closable="false" show-icon>
            该资源原路径已存在同名资源，恢复后将自动重命名
          </el-alert>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ restoreItem?.name }}</el-descriptions-item>
          <el-descriptions-item label="原始路径">{{ restoreItem?.originalPath }}</el-descriptions-item>
          <el-descriptions-item label="删除时间">{{ restoreItem?.deletedAt }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="restoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRestore">确认恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('task')
const loading = ref(false)
const selectedRows = ref([])
const searchName = ref('')
const restoreDialogVisible = ref(false)
const restoreItem = ref(null)
const hasConflict = ref(false)
const expireDays = ref(30)
const daysUntilCleanup = ref(7)
const totalStorage = ref('2.5 GB')

const taskData = ref([])
const folderData = ref([])
const apiData = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const total = ref(0)

const recycleData = computed(() => {
  const map = { task: taskData.value, folder: folderData.value, api: apiData.value }
  return map[activeTab.value] || []
})

const currentData = computed(() => {
  const all = recycleData.value
  if (!searchName.value) return all
  return all.filter(item => item.name.includes(searchName.value))
})

onMounted(() => {
  fetchData()
})

const fetchData = () => {
  loading.value = true
  try {
    taskData.value = [
      { id: 1, name: '用户数据清洗ETL', type: 'task', originalPath: '/数据开发/数据清洗/', deletedBy: 'zhangsan', deletedAt: '2024-12-18 15:30:00' },
      { id: 2, name: '商品信息全量同步', type: 'task', originalPath: '/数据开发/数据同步/', deletedBy: 'admin', deletedAt: '2024-12-17 09:15:00' },
      { id: 3, name: '日志归档任务', type: 'task', originalPath: '/数据开发/日志处理/', deletedBy: 'lisi', deletedAt: '2024-12-15 11:20:00' },
    ]
    folderData.value = [
      { id: 4, name: '测试项目', type: 'folder', originalPath: '/数据开发/', deletedBy: 'admin', deletedAt: '2024-12-19 08:00:00' },
      { id: 5, name: '旧版ETL规则', type: 'folder', originalPath: '/数据转换/', deletedBy: 'zhangsan', deletedAt: '2024-12-10 14:00:00' },
    ]
    apiData.value = [
      { id: 6, name: '用户查询接口', type: 'api', originalPath: '/数据服务/标准接口/', deletedBy: 'admin', deletedAt: '2024-12-16 10:00:00' },
    ]
    total.value = currentData.value.length
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const getRemainingDays = (row) => {
  if (!row.deletedAt) return 0
  const deleted = new Date(row.deletedAt)
  const expire = new Date(deleted.getTime() + expireDays.value * 24 * 60 * 60 * 1000)
  const now = new Date()
  const remaining = Math.ceil((expire - now) / (24 * 60 * 60 * 1000))
  return Math.max(0, remaining)
}

const getItemIcon = (type) => {
  const map = { task: 'Document', folder: 'Folder', api: 'Promotion' }
  return map[type] || 'Document'
}

const handleRestore = (row) => {
  restoreItem.value = row
  hasConflict.value = Math.random() > 0.5
  restoreDialogVisible.value = true
}

const confirmRestore = async () => {
  try {
    // 实际应调用API
    const data = recycleData.value
    const idx = data.findIndex(i => i.id === restoreItem.value.id)
    if (idx > -1) data.splice(idx, 1)
    ElMessage.success(`"${restoreItem.value.name}" 已恢复`)
    restoreDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('恢复失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定永久删除 "${row.name}"？此操作不可恢复！`,
      '危险操作',
      { type: 'warning', confirmButtonText: '永久删除' }
    )
    const data = recycleData.value
    const idx = data.findIndex(i => i.id === row.id)
    if (idx > -1) data.splice(idx, 1)
    ElMessage.success('已永久删除')
    fetchData()
  } catch (e) {
    // 取消
  }
}

const handleBatchRestore = async () => {
  const names = selectedRows.value.map(r => r.name).join(', ')
  try {
    await ElMessageBox.confirm(`确定恢复选中的 ${selectedRows.value.length} 项？`, '批量恢复')
    const data = recycleData.value
    selectedRows.value.forEach(row => {
      const idx = data.findIndex(i => i.id === row.id)
      if (idx > -1) data.splice(idx, 1)
    })
    selectedRows.value = []
    ElMessage.success('批量恢复成功')
    fetchData()
  } catch (e) {
    // 取消
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定永久删除选中的 ${selectedRows.value.length} 项？此操作不可恢复！`,
      '批量永久删除',
      { type: 'warning', confirmButtonText: '永久删除' }
    )
    const data = recycleData.value
    selectedRows.value.forEach(row => {
      const idx = data.findIndex(i => i.id === row.id)
      if (idx > -1) data.splice(idx, 1)
    })
    selectedRows.value = []
    ElMessage.success('批量永久删除成功')
    fetchData()
  } catch (e) {
    // 取消
  }
}
</script>

<style lang="scss" scoped>
.recycle-bin-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100%;

  .stats-row { margin-bottom: 20px; }

  .bin-stat-card {
    background: #fff;
    border-radius: 10px;
    padding: 18px 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    margin-bottom: 16px;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
    }
    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }

  .main-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    .tab-badge {
      margin-left: 8px;
    }

    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .pagination-wrap {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .item-name {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #4f6ef7;
    font-weight: 500;
  }

  .original-path {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #909399;
    font-size: 13px;
  }

  .remaining-time {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    .expiring { color: #ff4d4f; font-weight: 500; }
  }

  .restore-dialog {
    .restore-warning {
      margin-bottom: 16px;
    }
  }
}

@media (max-width: 768px) {
  .recycle-bin-page { padding: 16px; }
}
</style>
