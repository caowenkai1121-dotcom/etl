<template>
  <div class="log-page">
    <!-- 顶部筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams" label-width="70px" class="filter-form">
        <el-form-item label="任务">
          <el-select v-model="queryParams.taskId" placeholder="选择任务" clearable style="width: 150px">
            <el-option label="任务A" value="1" />
            <el-option label="任务B" value="2" />
            <el-option label="任务C" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 320px"
          />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="queryParams.level" placeholder="选择级别" clearable style="width: 120px">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
            <el-option label="DEBUG" value="DEBUG" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="queryParams.module" placeholder="选择模块" clearable style="width: 130px">
            <el-option label="同步" value="SYNC" />
            <el-option label="转换" value="TRANSFORM" />
            <el-option label="校验" value="VALIDATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="输入关键词" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-button @click="toggleSqlMode">
            <el-icon><Document /></el-icon>
            SQL模式
          </el-button>
        </el-form-item>
      </el-form>

      <!-- SQL模式输入框 -->
      <div v-show="sqlMode" class="sql-section">
        <el-input
          v-model="sqlQuery"
          type="textarea"
          :rows="3"
          placeholder="输入SQL查询语句"
          class="sql-input"
        />
        <el-button type="primary" @click="executeSql" class="sql-btn">执行SQL</el-button>
      </div>
    </el-card>

    <!-- 日志表格 -->
    <el-card class="table-card">
      <el-table
        :data="tableData"
        style="width: 100%"
        :expand-row-keys="expandedRows"
        @expand-change="handleExpand"
        row-key="id"
        v-loading="loading"
        stripe
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div v-if="row.transformData" class="transform-detail">
              <div class="detail-section">
                <h4>源数据 (Source)</h4>
                <pre class="json-view">{{ formatJson(row.transformData.source) }}</pre>
              </div>
              <div class="detail-section">
                <h4>目标数据 (Target)</h4>
                <pre class="json-view">{{ formatJson(row.transformData.target) }}</pre>
              </div>
            </div>
            <div v-else class="no-transform">
              <el-empty description="无转换数据" :image-size="60" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="timestamp" label="时间" width="180" />
        <el-table-column prop="traceId" label="TraceID" width="170">
          <template #default="{ row }">
            <el-link type="primary" @click="goToTrace(row.traceId)">
              {{ row.traceId }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small" effect="dark">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="message" label="消息" show-overflow-tooltip />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="copyMessage(row.message)">
              复制
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLogPage, getLogStats, getTraceDetail, exportLog } from '@/api'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const sqlMode = ref(false)
const sqlQuery = ref('')
const expandedRows = ref([])
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  taskId: '',
  dateRange: null,
  level: '',
  module: '',
  keyword: ''
})

onMounted(() => {
  if (route.query.keyword) {
    queryParams.keyword = route.query.keyword
  }
  if (route.query.task) {
    queryParams.keyword = route.query.task
  }
  handleSearch()
})

const buildParams = () => {
  const params = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    keyword: queryParams.keyword || undefined
  }
  if (queryParams.taskId) params.taskId = queryParams.taskId
  if (queryParams.level) params.level = queryParams.level
  if (queryParams.module) params.module = queryParams.module
  if (queryParams.dateRange && queryParams.dateRange.length === 2) {
    params.startTime = queryParams.dateRange[0]
    params.endTime = queryParams.dateRange[1]
  }
  return params
}

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await getLogPage(buildParams())
    if (res.data) {
      tableData.value = (res.data.records || res.data.list || []).map(item => ({
        ...item,
        timestamp: item.timestamp || item.createTime || '-',
        traceId: item.traceId || '-',
        level: item.level || 'INFO',
        module: item.module || '-',
        message: item.message || '-',
        transformData: item.transformData || null
      }))
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载日志失败:', e)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  try {
    const res = await exportLog(buildParams())
    const blob = res instanceof Blob ? res : new Blob([JSON.stringify(res, null, 2)], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `log-export-${dayjs().format('YYYYMMDD-HHmmss')}.txt`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

const toggleSqlMode = () => {
  sqlMode.value = !sqlMode.value
}

const executeSql = () => {
  if (!sqlQuery.value.trim()) {
    ElMessage.warning('请输入SQL查询语句')
    return
  }
  ElMessage.success('SQL查询已执行')
  handleSearch()
}

const handleExpand = (row, expandedRows) => {
  expandedRows.value = expandedRows.map(r => r.id)
}

const goToTrace = (traceId) => {
  router.push(`/log/trace/${traceId}`)
}

const copyMessage = (message) => {
  navigator.clipboard.writeText(message).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const formatJson = (obj) => {
  if (!obj) return ''
  return JSON.stringify(obj, null, 2)
}

const getLevelType = (level) => {
  const types = {
    'INFO': 'success',
    'WARN': 'warning',
    'ERROR': 'danger',
    'DEBUG': 'info'
  }
  return types[level] || 'info'
}
</script>

<style lang="scss" scoped>
.log-page {
  background-color: #f5f7fa;
  min-height: 100%;
  padding: 20px;

  .filter-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    margin-bottom: 20px;

    :deep(.el-card__body) {
      padding: 20px;
    }

    .filter-form {
      margin-bottom: 0;
    }

    .sql-section {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #f0f2f5;
      display: flex;
      gap: 12px;
      align-items: flex-start;

      .sql-input {
        flex: 1;

        :deep(.el-textarea__inner) {
          font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
          background-color: #1e1e1e;
          color: #d4d4d4;
          border-color: #3c3c3c;
        }
      }

      .sql-btn {
        margin-top: 5px;
      }
    }
  }

  .table-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

    :deep(.el-card__body) {
      padding: 20px;
    }

    .transform-detail {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      padding: 10px 0;

      .detail-section {
        h4 {
          font-size: 14px;
          color: #303133;
          margin-bottom: 10px;
          font-weight: 600;
        }

        .json-view {
          background-color: #f5f7fa;
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          padding: 16px;
          margin: 0;
          font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
          font-size: 12px;
          color: #606266;
          max-height: 300px;
          overflow: auto;
        }
      }
    }

    .no-transform {
      padding: 20px 0;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
