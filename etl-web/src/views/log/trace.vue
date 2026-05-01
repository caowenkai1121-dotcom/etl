<template>
  <div class="trace-page page-container">
    <!-- 头部概览 -->
    <el-card class="trace-header-card">
      <div class="trace-header">
        <div class="trace-info">
          <div class="trace-label">链路追踪</div>
          <div class="trace-id-display">
            <el-tag size="large" class="trace-id-tag">
              <el-icon><Link /></el-icon>
              {{ traceId }}
            </el-tag>
            <el-button size="small" @click="copyTraceId" text>
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="trace-meta" v-if="traceOverview">
          <div class="meta-item">
            <span class="meta-label">步骤总数</span>
            <span class="meta-value">{{ traceOverview.stepCount || stepList.length }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">总耗时</span>
            <span class="meta-value text-warning">{{ formatDuration(totalCostTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">状态</span>
            <el-tag :type="overallStatus === 'SUCCESS' ? 'success' : overallStatus === 'FAILED' ? 'danger' : 'warning'" size="small">
              {{ overallStatus === 'SUCCESS' ? '全部成功' : overallStatus === 'FAILED' ? '有失败步骤' : '处理中' }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 甘特图 -->
    <el-card class="chart-card">
      <template #header>
        <span class="card-title">
          <el-icon class="title-icon"><Histogram /></el-icon>
          步骤耗时甘特图
        </span>
      </template>
      <div ref="ganttChartRef" class="chart-container"></div>
    </el-card>

    <!-- 时间线步骤列表 -->
    <el-card class="timeline-card">
      <template #header>
        <span class="card-title">
          <el-icon class="title-icon"><Timer /></el-icon>
          步骤时间线
        </span>
      </template>

      <div v-loading="loading" class="timeline-container">
        <div v-if="stepList.length === 0" class="empty-state">
          <el-icon :size="48" color="rgba(99, 102, 241, 0.3)"><Search /></el-icon>
          <p>暂无追踪数据</p>
        </div>

        <div v-for="(step, index) in stepList" :key="index" class="timeline-item" :style="{ animationDelay: index * 0.08 + 's' }">
          <div class="timeline-dot" :class="getStepStatusClass(step.status)"></div>
          <div v-if="index < stepList.length - 1" class="timeline-line"></div>
          <div class="timeline-body">
            <div class="step-header">
              <span class="step-name">{{ step.stageName || step.stepName || '步骤' + (index + 1) }}</span>
              <div class="step-badges">
                <el-tag :type="getStepStatusTag(step.status)" size="small" effect="dark">
                  {{ step.status === 'SUCCESS' ? '成功' : step.status === 'FAILED' ? '失败' : '运行中' }}
                </el-tag>
                <el-tag size="small" type="info" effect="plain" v-if="step.costTime">
                  {{ formatDuration(step.costTime) }}
                </el-tag>
              </div>
            </div>
            <div class="step-details" v-if="step.recordCount !== undefined || step.message">
              <div class="detail-badge" v-if="step.recordCount !== undefined">
                <el-icon><Document /></el-icon> 记录数：{{ step.recordCount }}
              </div>
              <div class="detail-badge" v-if="step.rowCount !== undefined">
                <el-icon><DataAnalysis /></el-icon> 行数：{{ step.rowCount }}
              </div>
              <div class="detail-badge" v-if="step.errorCount">
                <el-icon><CircleClose /></el-icon> 错误：<span class="text-danger">{{ step.errorCount }}</span>
              </div>
            </div>
            <div class="step-time" v-if="step.startTime || step.createdAt">
              {{ step.startTime || step.createdAt }}
            </div>
            <div v-if="step.message" class="step-message">
              {{ step.message }}
            </div>
            <div v-if="step.errorMessage" class="step-error">
              <el-icon><WarningFilled /></el-icon> {{ step.errorMessage }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getLogByTraceId } from '@/api'

const route = useRoute()
const traceId = ref(route.params.traceId || '')
const loading = ref(false)
const stepList = ref([])
const traceOverview = ref(null)
const ganttChartRef = ref(null)
let ganttChart = null

const totalCostTime = computed(() => {
  return stepList.value.reduce((sum, s) => sum + (s.costTime || 0), 0)
})

const overallStatus = computed(() => {
  if (stepList.value.length === 0) return ''
  const hasFailed = stepList.value.some(s => s.status === 'FAILED')
  const hasRunning = stepList.value.some(s => s.status === 'RUNNING' || s.status === 'PENDING')
  if (hasFailed) return 'FAILED'
  if (hasRunning) return 'RUNNING'
  return 'SUCCESS'
})

onMounted(async () => {
  await fetchTraceData()
  initGanttChart()
})

onUnmounted(() => {
  ganttChart?.dispose()
})

const fetchTraceData = async () => {
  if (!traceId.value) return
  loading.value = true
  try {
    const res = await getLogByTraceId(traceId.value)
    const data = res.data || {}
    traceOverview.value = data.overview || null
    stepList.value = data.steps || data.list || []
  } catch (e) {
    console.error('获取链路追踪数据失败', e)
    ElMessage.error('获取链路追踪数据失败')
  } finally {
    loading.value = false
  }
}

const initGanttChart = () => {
  if (stepList.value.length === 0) return

  ganttChart = echarts.init(ganttChartRef.value)

  const stepNames = stepList.value.map((s, i) => s.stageName || s.stepName || '步骤' + (i + 1))
  const costTimes = stepList.value.map(s => s.costTime || 0)
  const statusColors = stepList.value.map(s =>
    s.status === 'SUCCESS' ? '#10b981' : s.status === 'FAILED' ? '#ef4444' : '#f59e0b'
  )

  // 计算累积起始时间
  let cumulative = 0
  const startTimes = stepList.value.map(() => {
    const start = cumulative
    cumulative += costTimes[startTimes ? startTimes.length : 0] || 0
    return start
  })

  // 重新计算startTimes
  let acc = 0
  const startData = stepList.value.map((s, i) => {
    const start = acc
    acc += s.costTime || 0
    return start
  })

  ganttChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(15, 23, 42, 0.95)',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      textStyle: { color: '#fff' },
      formatter: (params) => {
        const p = params[0]
        if (!p) return ''
        const idx = p.dataIndex
        const step = stepList.value[idx]
        return `<strong>${step.stageName || step.stepName || '步骤' + (idx + 1)}</strong><br/>
                耗时: ${formatDuration(step.costTime)}<br/>
                状态: ${step.status === 'SUCCESS' ? '成功' : step.status === 'FAILED' ? '失败' : '运行中'}<br/>
                ${step.recordCount !== undefined ? '记录数: ' + step.recordCount : ''}`
      }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', top: 10,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(255,255,255,0.6)', formatter: '{value}ms' },
      splitLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.1)' } },
      axisLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.3)' } }
    },
    yAxis: {
      type: 'category',
      data: stepNames.slice().reverse(),
      axisLine: { lineStyle: { color: 'rgba(99, 102, 241, 0.3)' } },
      axisLabel: { color: 'rgba(255,255,255,0.7)', fontWeight: 'bold' }
    },
    series: [{
      type: 'bar',
      data: costTimes.slice().reverse().map((val, idx) => ({
        value: val,
        itemStyle: {
          color: statusColors.slice().reverse()[idx],
          borderRadius: [0, 6, 6, 0],
          shadowColor: 'rgba(99, 102, 241, 0.3)',
          shadowBlur: 6
        }
      })),
      barWidth: 20,
      label: {
        show: true,
        position: 'right',
        color: 'rgba(255,255,255,0.7)',
        formatter: (p) => formatDuration(p.value)
      }
    }]
  })
}

const getStepStatusClass = (status) => {
  if (status === 'SUCCESS') return 'dot-success'
  if (status === 'FAILED') return 'dot-failed'
  return 'dot-running'
}

const getStepStatusTag = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const formatDuration = (ms) => {
  if (!ms) return '0ms'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  const min = Math.floor(ms / 60000)
  const sec = Math.floor((ms % 60000) / 1000)
  return min + 'm' + sec + 's'
}

const copyTraceId = () => {
  navigator.clipboard.writeText(traceId.value).then(() => {
    ElMessage.success('TraceID已复制')
  })
}
</script>

<style lang="scss" scoped>
.trace-page {
  .trace-header-card {
    border-radius: 16px;
    margin-bottom: 24px;
  }

  .trace-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .trace-label {
      font-size: 14px;
      color: var(--text-muted);
      margin-bottom: 8px;
    }

    .trace-id-display {
      display: flex;
      align-items: center;
      gap: 8px;

      .trace-id-tag {
        font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
        font-size: 16px;
        padding: 8px 16px;
        background: rgba(99, 102, 241, 0.15);
        border-color: rgba(99, 102, 241, 0.3);
        color: #a5b4fc;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .trace-meta {
      display: flex;
      gap: 24px;
      align-items: center;

      .meta-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;

        .meta-label {
          font-size: 12px;
          color: var(--text-muted);
        }

        .meta-value {
          font-size: 20px;
          font-weight: 700;
          color: var(--text-primary);
        }
      }
    }
  }

  .chart-card {
    border-radius: 16px;
    margin-bottom: 24px;

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);

      .title-icon {
        color: var(--primary-light);
      }
    }

    .chart-container {
      height: 280px;
    }
  }

  .timeline-card {
    border-radius: 16px;

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);

      .title-icon {
        color: var(--primary-light);
      }
    }
  }

  .timeline-container {
    position: relative;
    padding: 8px 0;
    min-height: 200px;

    .empty-state {
      text-align: center;
      padding: 60px 0;
      color: var(--text-muted);
      p { margin-top: 12px; }
    }
  }

  .timeline-item {
    display: flex;
    position: relative;
    padding-left: 32px;
    padding-bottom: 28px;
    animation: fadeIn 0.4s ease both;

    &:last-child {
      padding-bottom: 0;
    }

    .timeline-dot {
      position: absolute;
      left: 0;
      top: 4px;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      z-index: 2;
      border: 3px solid var(--bg-card);

      &.dot-success {
        background: #10b981;
        box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
      }

      &.dot-failed {
        background: #ef4444;
        box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
      }

      &.dot-running {
        background: #f59e0b;
        box-shadow: 0 0 8px rgba(245, 158, 11, 0.5);
        animation: breathe 1.5s infinite;
      }
    }

    .timeline-line {
      position: absolute;
      left: 7px;
      top: 20px;
      bottom: 0;
      width: 2px;
      background: linear-gradient(180deg, rgba(99, 102, 241, 0.3), rgba(99, 102, 241, 0.05));
    }

    .timeline-body {
      flex: 1;
      background: rgba(99, 102, 241, 0.04);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 16px;
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--primary-color);
        background: rgba(99, 102, 241, 0.08);
      }

      .step-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .step-name {
          font-weight: 600;
          font-size: 15px;
          color: var(--text-primary);
        }

        .step-badges {
          display: flex;
          gap: 8px;
        }
      }

      .step-details {
        display: flex;
        gap: 16px;
        flex-wrap: wrap;
        margin-bottom: 6px;

        .detail-badge {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 13px;
          color: var(--text-muted);
        }
      }

      .step-time {
        font-size: 12px;
        color: var(--text-muted);
        margin-bottom: 4px;
      }

      .step-message {
        font-size: 13px;
        color: var(--text-secondary);
        margin-top: 8px;
        padding: 8px 12px;
        background: rgba(99, 102, 241, 0.06);
        border-radius: 6px;
      }

      .step-error {
        font-size: 13px;
        color: #f85149;
        margin-top: 8px;
        padding: 8px 12px;
        background: rgba(239, 68, 68, 0.08);
        border-radius: 6px;
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }
  }
}

.text-warning { color: var(--warning-color); }
.text-danger { color: var(--danger-color); }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
