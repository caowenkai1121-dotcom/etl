<template>
  <div class="transform-log-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Edit /></el-icon>
            转换日志
          </span>
          <div class="header-actions">
            <el-switch v-model="autoRefresh" active-text="自动刷新" @change="toggleAutoRefresh" />
            <el-button type="primary" @click="fetchData">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="queryParams" label-width="70">
          <el-form-item label="任务ID">
            <el-input v-model="queryParams.taskId" placeholder="请输入任务ID" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item label="规则类型">
            <el-select v-model="queryParams.ruleType" placeholder="请选择" clearable style="width: 120px">
              <el-option label="字段映射" value="FIELD_MAPPING" />
              <el-option label="类型转换" value="TYPE_CONVERT" />
              <el-option label="值替换" value="VALUE_REPLACE" />
              <el-option label="表达式计算" value="EXPRESSION" />
              <el-option label="数据脱敏" value="DATA_MASK" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 100px">
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
              <el-option label="跳过" value="SKIPPED" />
            </el-select>
          </el-form-item>
          <el-form-item label="TraceID">
            <el-input v-model="queryParams.traceId" placeholder="TraceID" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 340px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        class="data-table"
        max-height="600"
        @expand-change="handleExpandChange"
        :row-class-name="({ row }) => row.expanded ? 'expanded-row' : ''"
      >
        <el-table-column type="expand" width="40">
          <template #default="{ row }">
            <div class="expand-detail">
              <div class="detail-section">
                <h4>转换前（源值）</h4>
                <pre class="json-block custom-scrollbar">{{ formatJson(row.sourceValue) }}</pre>
              </div>
              <div class="detail-section">
                <h4>转换后（目标值）</h4>
                <pre class="json-block custom-scrollbar">{{ formatJson(row.targetValue) }}</pre>
              </div>
              <div v-if="row.errorMessage" class="detail-section">
                <h4 style="color: #f85149;">错误信息</h4>
                <pre class="json-block error-block custom-scrollbar">{{ row.errorMessage }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskId" label="任务ID" width="70" />
        <el-table-column label="TraceID" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button v-if="row.traceId" type="primary" link size="small" @click.stop="goToTrace(row.traceId)">
              {{ row.traceId }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ruleName" label="规则名称" width="130" show-overflow-tooltip />
        <el-table-column prop="ruleType" label="规则类型" width="100" />
        <el-table-column prop="sourceValue" label="源值" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-ellipsis">{{ truncateStr(row.sourceValue, 40) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="targetValue" label="目标值" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-ellipsis">{{ truncateStr(row.targetValue, 40) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : row.status === 'FAILED' ? '失败' : '跳过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click.stop="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
        class="pagination-wrap"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="转换日志详情" width="800px" class="dark-dialog">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="任务ID">{{ currentLog.taskId }}</el-descriptions-item>
        <el-descriptions-item label="TraceID">
          <el-button v-if="currentLog.traceId" type="primary" link size="small" @click="goToTrace(currentLog.traceId)">
            {{ currentLog.traceId }}
          </el-button>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="规则名称">{{ currentLog.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="规则类型">{{ currentLog.ruleType }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 'SUCCESS' ? 'success' : currentLog.status === 'FAILED' ? 'danger' : 'info'" size="small">
            {{ currentLog.status === 'SUCCESS' ? '成功' : currentLog.status === 'FAILED' ? '失败' : '跳过' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentLog.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-compare">
        <div class="compare-section">
          <h4>源值</h4>
          <pre class="json-block custom-scrollbar">{{ formatJson(currentLog.sourceValue) }}</pre>
        </div>
        <div class="compare-arrow">
          <el-icon :size="32" color="#6366f1"><Right /></el-icon>
        </div>
        <div class="compare-section">
          <h4>目标值</h4>
          <pre class="json-block custom-scrollbar">{{ formatJson(currentLog.targetValue) }}</pre>
        </div>
      </div>

      <div v-if="currentLog.errorMessage" class="log-error-section">
        <h4>错误信息</h4>
        <pre class="error-block custom-scrollbar">{{ currentLog.errorMessage }}</pre>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTransformLogPage, getTransformLogDetail } from '@/api'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const autoRefresh = ref(false)
const refreshTimer = ref(null)
const detailVisible = ref(false)
const currentLog = ref({})

const queryParams = reactive({
  pageNum: 1, pageSize: 20,
  taskId: '', ruleType: '', status: '', traceId: '', dateRange: null
})

onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  if (refreshTimer.value) clearInterval(refreshTimer.value)
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.dateRange && params.dateRange.length === 2) {
      params.startTime = params.dateRange[0]
      params.endTime = params.dateRange[1]
      delete params.dateRange
    }
    const res = await getTransformLogPage(params)
    tableData.value = (res.data?.list || []).map(row => ({ ...row, expanded: false }))
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.taskId = ''
  queryParams.ruleType = ''
  queryParams.status = ''
  queryParams.traceId = ''
  queryParams.dateRange = null
  fetchData()
}

const toggleAutoRefresh = (val) => {
  if (val) {
    refreshTimer.value = setInterval(fetchData, 5000)
    ElMessage.success('已开启自动刷新')
  } else {
    if (refreshTimer.value) { clearInterval(refreshTimer.value); refreshTimer.value = null }
    ElMessage.info('已关闭自动刷新')
  }
}

const handleExpandChange = (row) => {
  row.expanded = !row.expanded
}

const showDetail = async (row) => {
  try {
    const res = await getTransformLogDetail(row.id)
    currentLog.value = res.data || row
  } catch (e) {
    currentLog.value = row
  }
  detailVisible.value = true
}

const goToTrace = (traceId) => {
  if (traceId) {
    router.push(`/log/trace/${traceId}`)
  }
}

const formatJson = (val) => {
  if (!val) return '-'
  if (typeof val === 'string') {
    try {
      return JSON.stringify(JSON.parse(val), null, 2)
    } catch {
      return val
    }
  }
  return JSON.stringify(val, null, 2)
}

const truncateStr = (val, maxLen) => {
  if (!val) return '-'
  const str = typeof val === 'string' ? val : JSON.stringify(val)
  return str.length > maxLen ? str.substring(0, maxLen) + '...' : str
}
</script>

<style lang="scss" scoped>
.transform-log-page {
  .main-card {
    border-radius: 16px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary);

      .title-icon {
        color: var(--primary-light);
        font-size: 20px;
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }

  .search-bar {
    padding: 20px;
    margin-bottom: 20px;
    background: rgba(99, 102, 241, 0.05);
    border-radius: 12px;
    border: 1px solid var(--border-color);
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) { display: none; }

    :deep(.expanded-row > td) {
      background: rgba(99, 102, 241, 0.08) !important;
    }

    .cell-ellipsis {
      max-width: 200px;
      display: inline-block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .expand-detail {
    padding: 16px 40px;
    display: flex;
    gap: 16px;
    flex-wrap: wrap;

    .detail-section {
      flex: 1;
      min-width: 280px;

      h4 {
        color: var(--text-primary);
        font-size: 14px;
        margin-bottom: 8px;
      }

      .json-block {
        background: #0d1117;
        border: 1px solid #30363d;
        border-radius: 8px;
        padding: 12px;
        font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
        font-size: 12px;
        color: #c9d1d9;
        max-height: 250px;
        overflow: auto;
        margin: 0;
        white-space: pre-wrap;
        word-break: break-all;
      }

      .error-block {
        border-color: rgba(239, 68, 68, 0.3);
        color: #f85149;
      }
    }
  }

  .detail-compare {
    margin-top: 20px;
    display: flex;
    gap: 16px;
    align-items: flex-start;

    .compare-section {
      flex: 1;

      h4 {
        color: var(--text-primary);
        font-size: 14px;
        margin-bottom: 8px;
      }

      .json-block {
        background: #0d1117;
        border: 1px solid #30363d;
        border-radius: 8px;
        padding: 12px;
        font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
        font-size: 12px;
        color: #c9d1d9;
        max-height: 350px;
        overflow: auto;
        margin: 0;
        white-space: pre-wrap;
        word-break: break-all;
      }
    }

    .compare-arrow {
      display: flex;
      align-items: center;
      padding-top: 30px;
    }
  }

  .log-error-section {
    margin-top: 16px;

    h4 {
      color: #f85149;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .error-block {
      background: #0d1117;
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 8px;
      padding: 12px;
      font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
      font-size: 12px;
      color: #f85149;
      max-height: 200px;
      overflow: auto;
      margin: 0;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}
</style>
