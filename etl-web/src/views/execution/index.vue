<template>
  <div class="execution-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Document /></el-icon>
            执行记录
          </span>
          <div class="header-actions">
            <el-switch v-model="autoRefresh" active-text="自动刷新" @change="toggleAutoRefresh" />
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 320px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择" clearable>
              <el-option label="运行中" value="RUNNING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
              <el-option label="已跳过" value="SKIPPED" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务名称">
            <el-input v-model="queryParams.taskName" placeholder="请输入" clearable style="width: 160px" />
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
        @row-click="handleRowClick"
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <!-- 进度条 -->
              <div class="progress-section">
                <div class="progress-header">
                  <span class="progress-title">执行进度</span>
                  <span class="progress-text">{{ row.progress || 0 }}%</span>
                </div>
                <el-progress :percentage="row.progress || 0" :status="getProgressStatus(row.status)" :stroke-width="10" />
                <div class="progress-stats">
                  <div class="stat-item">
                    <span class="stat-label">已抽取</span>
                    <span class="stat-value">{{ row.extractedCount || 0 }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">已转换</span>
                    <span class="stat-value">{{ row.transformedCount || 0 }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">已加载</span>
                    <span class="stat-value">{{ row.loadedCount || 0 }}</span>
                  </div>
                </div>
              </div>

              <!-- 甘特图 -->
              <div class="gantt-section">
                <div class="section-title">执行阶段</div>
                <div class="gantt-chart">
                  <div class="gantt-row" v-for="stage in getStageData(row)" :key="stage.name">
                    <div class="gantt-label">{{ stage.name }}</div>
                    <div class="gantt-bar-container">
                      <div
                        class="gantt-bar"
                        :class="stage.status"
                        :style="{ left: stage.offset + '%', width: stage.width + '%' }"
                      ></div>
                    </div>
                    <div class="gantt-time">{{ stage.duration }}ms</div>
                  </div>
                </div>
              </div>

              <!-- 实时日志 -->
              <div class="log-section">
                <div class="section-header">
                  <span class="section-title">执行日志</span>
                  <el-button size="small" @click="showFullLog(row)">查看完整日志</el-button>
                </div>
                <div class="log-terminal" ref="logTerminalRef">
                  <div class="terminal-header">
                    <span class="terminal-dot red"></span>
                    <span class="terminal-dot yellow"></span>
                    <span class="terminal-dot green"></span>
                    <span class="terminal-title">SYNC LOG - {{ row.executionNo }}</span>
                  </div>
                  <div class="terminal-body custom-scrollbar">
                    <div v-for="(log, index) in getLogs(row)" :key="index" class="terminal-line" :class="'log-' + log.level.toLowerCase()">
                      <span class="line-time">{{ formatTime(log.timestamp) }}</span>
                      <span class="line-level" :class="log.level.toLowerCase()">[{{ log.level }}]</span>
                      <span v-if="log.table" class="line-table">[{{ log.table }}]</span>
                      <span class="line-message">{{ log.message }}</span>
                    </div>
                    <div v-if="getLogs(row).length === 0" class="terminal-empty">暂无日志</div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="executionNo" label="执行ID" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <div class="status-indicator">
              <span class="status-dot" :class="getStatusDotClass(row.status)"></span>
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="duration" label="耗时" width="100">
          <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column prop="totalRows" label="处理量" width="100">
          <template #default="{ row }">{{ row.totalRows || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'FAILED'" size="small" type="warning" @click="handleRetry(row)">重试</el-button>
            <el-button v-if="row.status === 'FAILED'" size="small" type="danger" @click="handleRollback(row)">回滚</el-button>
            <el-button size="small" @click="showFullLog(row)">日志</el-button>
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

    <!-- 完整日志对话框 -->
    <el-dialog v-model="logVisible" title="执行日志" width="900px" :close-on-click-modal="false" class="dark-dialog">
      <div class="terminal-area">
        <div class="terminal-header">
          <span class="terminal-dot red"></span>
          <span class="terminal-dot yellow"></span>
          <span class="terminal-dot green"></span>
          <span class="terminal-title">SYNC LOG - {{ currentExecutionNo }}</span>
        </div>
        <div class="terminal-body custom-scrollbar" ref="fullLogRef">
          <div v-for="(log, index) in fullLogs" :key="index" class="terminal-line" :class="'log-' + log.level.toLowerCase()">
            <span class="line-time">{{ formatTime(log.timestamp) }}</span>
            <span class="line-level" :class="log.level.toLowerCase()">[{{ log.level }}]</span>
            <span v-if="log.table" class="line-table">[{{ log.table }}]</span>
            <span class="line-message">{{ log.message }}</span>
          </div>
          <div v-if="fullLogs.length === 0" class="terminal-empty">暂无日志</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="logVisible = false">关闭</el-button>
        <el-button @click="exportLogs">导出日志</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { monitorAPI } from '@/api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const autoRefresh = ref(false)
const refreshTimer = ref(null)
const dateRange = ref([])
const logVisible = ref(false)
const currentExecutionNo = ref('')
const fullLogs = ref([])
const fullLogRef = ref(null)
const logTerminalRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  taskName: '',
  startTime: '',
  endTime: ''
})

// 模拟日志数据
const logCache = reactive({})

// 模拟执行记录数据
const mockData = [
  { id: 1, taskName: '用户数据同步', executionNo: 'EXEC-20240424-001', status: 'SUCCESS', startTime: '2024-04-24 02:00:00', duration: 12345, totalRows: 1568, progress: 100, extractedCount: 1568, transformedCount: 1568, loadedCount: 1568 },
  { id: 2, taskName: '订单数据同步', executionNo: 'EXEC-20240424-002', status: 'RUNNING', startTime: '2024-04-24 14:30:00', duration: 45000, totalRows: 5000, progress: 60, extractedCount: 5000, transformedCount: 3000, loadedCount: 1000 },
  { id: 3, taskName: '产品数据同步', executionNo: 'EXEC-20240424-003', status: 'FAILED', startTime: '2024-04-24 10:15:00', duration: 8765, totalRows: 200, progress: 45, extractedCount: 200, transformedCount: 90, loadedCount: 0 },
  { id: 4, taskName: '日志数据同步', executionNo: 'EXEC-20240424-004', status: 'SUCCESS', startTime: '2024-04-24 00:00:00', duration: 23456, totalRows: 12000, progress: 100, extractedCount: 12000, transformedCount: 12000, loadedCount: 12000 },
  { id: 5, taskName: '报表数据同步', executionNo: 'EXEC-20240424-005', status: 'SKIPPED', startTime: '2024-04-24 06:00:00', duration: 0, totalRows: 0, progress: 0 },
]

// 初始化日志
onMounted(() => {
  mockData.forEach(row => {
    logCache[row.id] = generateMockLogs(row)
  })
  fetchData()
})

onUnmounted(() => {
  if (refreshTimer.value) clearInterval(refreshTimer.value)
})

const fetchData = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    tableData.value = mockData
    total.value = mockData.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleAutoRefresh = (val) => {
  if (val) {
    refreshTimer.value = setInterval(fetchData, 5000)
  } else {
    if (refreshTimer.value) {
      clearInterval(refreshTimer.value)
      refreshTimer.value = null
    }
  }
}

const resetQuery = () => {
  queryParams.pageNum = 1
  queryParams.status = ''
  queryParams.taskName = ''
  queryParams.startTime = ''
  queryParams.endTime = ''
  dateRange.value = []
  fetchData()
}

const handleRowClick = (row) => {
  // 点击行展开，不需要额外处理
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? '' : ''
}

const getProgressStatus = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'exception'
  return ''
}

const getStatusDotClass = (status) => {
  const classes = { RUNNING: 'running', SUCCESS: 'success', FAILED: 'error', SKIPPED: 'skipped' }
  return classes[status] || 'pending'
}

const getStatusText = (status) => {
  const texts = { RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败', SKIPPED: '已跳过' }
  return texts[status] || status
}

const formatDuration = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  const min = Math.floor(ms / 60000)
  const sec = Math.floor((ms % 60000) / 1000)
  return min + 'm' + sec + 's'
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
}

// 获取模拟日志
const generateMockLogs = (row) => {
  const logs = [
    { timestamp: Date.now() - 60000, level: 'INFO', message: '开始执行任务: ' + row.taskName },
    { timestamp: Date.now() - 55000, level: 'INFO', table: 'users', message: '开始抽取数据' },
    { timestamp: Date.now() - 50000, level: 'INFO', table: 'users', message: '抽取完成，共 ' + (row.extractedCount || 0) + ' 条数据' },
    { timestamp: Date.now() - 45000, level: 'INFO', table: 'users', message: '开始转换数据' },
  ]
  if (row.status === 'FAILED') {
    logs.push({ timestamp: Date.now() - 30000, level: 'ERROR', table: 'users', message: '转换失败: 数据格式错误' })
  } else if (row.status === 'SUCCESS') {
    logs.push({ timestamp: Date.now() - 40000, level: 'INFO', table: 'users', message: '转换完成' })
    logs.push({ timestamp: Date.now() - 35000, level: 'INFO', table: 'users', message: '开始加载数据' })
    logs.push({ timestamp: Date.now() - 10000, level: 'INFO', table: 'users', message: '加载完成' })
    logs.push({ timestamp: Date.now() - 5000, level: 'INFO', message: '任务执行完成' })
  }
  return logs
}

const getLogs = (row) => {
  if (!logCache[row.id]) {
    logCache[row.id] = generateMockLogs(row)
  }
  return logCache[row.id]
}

// 获取甘特图数据
const getStageData = (row) => {
  if (row.status === 'SKIPPED') return []
  const stages = [
    { name: '抽取', status: 'success', duration: 3000, offset: 0, width: 30 },
    { name: '转换', status: row.status === 'FAILED' ? 'error' : 'success', duration: 4000, offset: 30, width: 40 },
    { name: '加载', status: row.status === 'FAILED' ? 'pending' : 'success', duration: 5000, offset: 70, width: 30 },
  ]
  return stages
}

// 重试
const handleRetry = async (row) => {
  try {
    await ElMessageBox.confirm('确定重试该任务？', '提示', { type: 'warning' })
    ElMessage.success('已开始重试')
  } catch (e) {}
}

// 回滚
const handleRollback = async (row) => {
  try {
    await ElMessageBox.confirm('确定回滚该任务？', '提示', { type: 'warning' })
    ElMessage.success('已开始回滚')
  } catch (e) {}
}

// 显示完整日志
const showFullLog = (row) => {
  currentExecutionNo.value = row.executionNo
  fullLogs.value = getLogs(row)
  logVisible.value = true
}

// 导出日志
const exportLogs = () => {
  ElMessage.success('日志导出功能')
}
</script>

<style lang="scss" scoped>
.execution-page {
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
  }

  .search-bar {
    padding: 20px;
    margin-bottom: 20px;
    background: rgba(79, 110, 247, 0.05);
    border-radius: 12px;
    border: 1px solid var(--border-color);
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) { display: none; }
  }

  .expand-content {
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;

    .progress-section {
      margin-bottom: 20px;
      padding: 16px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid var(--border-color);

      .progress-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;
      }

      .progress-title {
        font-weight: 600;
        color: var(--text-primary);
      }

      .progress-stats {
        display: flex;
        gap: 40px;
        margin-top: 16px;

        .stat-item {
          text-align: center;

          .stat-label {
            display: block;
            font-size: 12px;
            color: var(--text-muted);
            margin-bottom: 4px;
          }

          .stat-value {
            font-size: 18px;
            font-weight: 600;
            color: var(--text-primary);
          }
        }
      }
    }

    .gantt-section {
      margin-bottom: 20px;
      padding: 16px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid var(--border-color);

      .section-title {
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: 16px;
      }

      .gantt-chart {
        .gantt-row {
          display: flex;
          align-items: center;
          gap: 16px;
          margin-bottom: 12px;

          .gantt-label {
            width: 60px;
            font-size: 13px;
            color: var(--text-secondary);
            text-align: right;
          }

          .gantt-bar-container {
            flex: 1;
            height: 20px;
            background: #f1f5f9;
            border-radius: 4px;
            position: relative;

            .gantt-bar {
              height: 100%;
              border-radius: 4px;
              position: absolute;
              transition: all 0.3s;

              &.success {
                background: linear-gradient(90deg, #52c41a, #95de64);
              }

              &.error {
                background: linear-gradient(90deg, #ff4d4f, #ff7875);
              }

              &.pending {
                background: #e2e8f0;
              }
            }
          }

          .gantt-time {
            width: 80px;
            font-size: 13px;
            color: var(--text-muted);
            text-align: right;
          }
        }
      }
    }

    .log-section {
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .section-title {
          font-weight: 600;
          color: var(--text-primary);
        }
      }
    }
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.running { background: #faad14; animation: breathe 2s infinite; }
      &.success { background: #52c41a; box-shadow: 0 0 6px rgba(82, 196, 26, 0.5); }
      &.error { background: #ff4d4f; animation: blink 1s infinite; }
      &.skipped { background: #94a3b8; }
      &.pending { background: var(--text-muted); }
    }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .log-terminal {
    .terminal-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      background: #2d3748;
      border-bottom: 1px solid #4a5568;
      border-radius: 8px 8px 0 0;

      .terminal-dot {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        &.red { background: #f85149; }
        &.yellow { background: #d29922; }
        &.green { background: #3fb950; }
      }

      .terminal-title {
        flex: 1;
        text-align: center;
        color: #a0aec0;
        font-size: 12px;
      }
    }

    .terminal-body {
      padding: 16px;
      background: #1a202c;
      border-radius: 0 0 8px 8px;
      max-height: 300px;
      overflow-y: auto;

      .terminal-line {
        display: flex;
        gap: 10px;
        padding: 4px 0;
        font-family: 'Monaco', 'Consolas', monospace;
        font-size: 13px;

        .line-time {
          color: #718096;
          min-width: 80px;
        }

        .line-level {
          min-width: 60px;
          font-weight: 600;

          &.info { color: #63b3ed; }
          &.warn { color: #ecc94b; }
          &.error { color: #fc8181; }
          &.debug { color: #9ae6b4; }
        }

        .line-table {
          color: #90cdf4;
          min-width: 100px;
        }

        .line-message {
          flex: 1;
          color: #e2e8f0;
        }
      }

      .terminal-empty {
        text-align: center;
        padding: 20px 0;
        color: #718096;
        font-size: 14px;
      }
    }
  }

  .terminal-area {
    .terminal-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      background: #2d3748;
      border-bottom: 1px solid #4a5568;
      border-radius: 8px 8px 0 0;

      .terminal-dot {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        &.red { background: #f85149; }
        &.yellow { background: #d29922; }
        &.green { background: #3fb950; }
      }

      .terminal-title {
        flex: 1;
        text-align: center;
        color: #a0aec0;
        font-size: 12px;
      }
    }

    .terminal-body {
      padding: 16px;
      background: #1a202c;
      border-radius: 0 0 8px 8px;
      height: 500px;
      overflow-y: auto;

      .terminal-line {
        display: flex;
        gap: 10px;
        padding: 4px 0;
        font-family: 'Monaco', 'Consolas', monospace;
        font-size: 13px;

        .line-time {
          color: #718096;
          min-width: 80px;
        }

        .line-level {
          min-width: 60px;
          font-weight: 600;

          &.info { color: #63b3ed; }
          &.warn { color: #ecc94b; }
          &.error { color: #fc8181; }
          &.debug { color: #9ae6b4; }
        }

        .line-table {
          color: #90cdf4;
          min-width: 100px;
        }

        .line-message {
          flex: 1;
          color: #e2e8f0;
        }
      }

      .terminal-empty {
        text-align: center;
        padding: 60px 0;
        color: #718096;
        font-size: 14px;
      }
    }
  }
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
