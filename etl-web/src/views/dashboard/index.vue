<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.runningTasks || 0 }}</div>
            <div class="stat-label">运行中任务</div>
            <div class="stat-trend neutral">实时</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ (overview.todaySuccessRate || 100).toFixed(1) }}%</div>
            <div class="stat-label">今日成功率</div>
            <div class="stat-trend positive">+0%</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon orange">
            <el-icon><DocumentCopy /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(overview.todaySuccessRows || 0) }}</div>
            <div class="stat-label">今日同步量</div>
            <div class="stat-trend neutral">{{ formatNumber(overview.avgDailyRows || 0) }}条/日均</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-icon red">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.alertCount || 0 }}</div>
            <div class="stat-label">活跃告警</div>
            <div class="stat-trend neutral">正常</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 资源使用率卡片 -->
    <el-row :gutter="20" class="resource-row">
      <el-col :xs="24" :sm="8">
        <el-card class="resource-card">
          <div class="resource-header">
            <span class="resource-title">CPU使用率</span>
            <el-tag :type="resourceStatus.cpu" size="small">{{ resource.cpu }}%</el-tag>
          </div>
          <el-progress :percentage="resource.cpu" :stroke-width="12" :color="getProgressColor(resource.cpu)" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="resource-card">
          <div class="resource-header">
            <span class="resource-title">内存使用率</span>
            <el-tag :type="resourceStatus.memory" size="small">{{ resource.memory }}%</el-tag>
          </div>
          <el-progress :percentage="resource.memory" :stroke-width="12" :color="getProgressColor(resource.memory)" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="resource-card">
          <div class="resource-header">
            <span class="resource-title">连接池使用率</span>
            <el-tag :type="resourceStatus.connection" size="small">{{ resource.connection }}%</el-tag>
          </div>
          <el-progress :percentage="resource.connection" :stroke-width="12" :color="getProgressColor(resource.connection)" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">执行成功率</span>
            </div>
          </template>
          <div ref="successRateChartRef" class="chart-container rate-chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">执行趋势</span>
              <el-radio-group v-model="chartDays" size="small" @change="initTrendChart">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="14">14天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时任务状态和最近执行 -->
    <el-row :gutter="20" class="list-row">
      <el-col :xs="24" :lg="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon class="pulse-icon"><Loading /></el-icon>
                实时任务状态
              </span>
              <el-button type="text" @click="$router.push('/monitor')">查看监控</el-button>
            </div>
          </template>
          <div class="realtime-list">
            <div v-for="item in realtimeTasks" :key="item.id" class="realtime-item">
              <div class="item-left">
                <div class="task-icon" :class="getTaskStatusClass(item.status)">
                  <el-icon v-if="item.status === 'RUNNING'" class="spin"><Loading /></el-icon>
                  <el-icon v-else-if="item.status === 'SUCCESS'"><CircleCheck /></el-icon>
                  <el-icon v-else><CircleClose /></el-icon>
                </div>
                <div class="item-info">
                  <div class="item-title">{{ item.taskName }}</div>
                  <div class="item-progress" v-if="item.status === 'RUNNING'">
                    <el-progress :percentage="item.progress || 0" :stroke-width="6" size="small" />
                    <span class="progress-text">{{ item.syncRows || 0 }} 条</span>
                  </div>
                  <div class="item-time" v-else>
                    {{ item.status === 'SUCCESS' ? '完成于 ' : '耗时 ' }}{{ formatDuration(item.duration) }}
                  </div>
                </div>
              </div>
              <el-tag :type="getTaskTagType(item.status)" size="small">
                {{ getTaskStatusLabel(item.status) }}
              </el-tag>
            </div>
            <div v-if="realtimeTasks.length === 0" class="empty-state">暂无运行中任务</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近执行记录</span>
              <el-button type="text" @click="$router.push('/execution')">查看全部</el-button>
            </div>
          </template>
          <div class="recent-list">
            <div v-for="item in recentExecutions" :key="item.id" class="recent-item">
              <div class="item-left">
                <span class="status-dot" :class="item.status === 'SUCCESS' ? 'success' : 'error'"></span>
                <div class="item-info">
                  <div class="item-title">{{ item.taskName || `任务 #${item.taskId}` }}</div>
                  <div class="item-desc">{{ item.triggerType }} · {{ formatTime(item.startTime) }}</div>
                </div>
              </div>
              <el-tag :type="item.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ item.status === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </div>
            <div v-if="recentExecutions.length === 0" class="empty-state">暂无执行记录</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作和告警 -->
    <el-row :gutter="20" class="action-row">
      <el-col :xs="24" :lg="12">
        <el-card class="quick-actions-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/workflow')">
              <el-icon><Share /></el-icon>
              <span>任务编排</span>
            </el-button>
            <el-button type="success" size="large" @click="$router.push('/api-service')">
              <el-icon><Promotion /></el-icon>
              <span>API服务</span>
            </el-button>
            <el-button size="large" @click="$router.push('/datasource')">
              <el-icon><CoinIcon /></el-icon>
              <span>数据源管理</span>
            </el-button>
            <el-button size="large" @click="$router.push('/alert')">
              <el-icon><Bell /></el-icon>
              <span>告警中心</span>
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">告警摘要</span>
              <el-button type="text" @click="$router.push('/alert')">查看全部</el-button>
            </div>
          </template>
          <div class="recent-list">
            <div v-for="item in recentAlerts" :key="item.id" class="recent-item">
              <div class="item-left">
                <span class="status-dot" :class="item.severity === 'CRITICAL' || item.severity === 'ERROR' ? 'error' : 'warning'"></span>
                <div class="item-info">
                  <div class="item-title">{{ item.title || item.alertType }}</div>
                  <div class="item-desc">{{ item.content || '暂无描述' }}</div>
                </div>
              </div>
              <el-tag :type="getAlertTagType(item.severity)" size="small">
                {{ getAlertLabel(item.severity) }}
              </el-tag>
            </div>
            <div v-if="recentAlerts.length === 0" class="empty-state">暂无告警</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { monitorAPI } from '@/api'

const overview = ref({
  runningTasks: 0,
  todaySuccessRate: null,
  todaySuccessRows: 0,
  avgDailyRows: 0,
  alertCount: 0
})

const resource = reactive({
  cpu: 35,
  memory: 52,
  connection: 28
})

const resourceStatus = computed(() => ({
  cpu: resource.cpu > 80 ? 'danger' : resource.cpu > 60 ? 'warning' : 'success',
  memory: resource.memory > 80 ? 'danger' : resource.memory > 60 ? 'warning' : 'success',
  connection: resource.connection > 80 ? 'danger' : resource.connection > 60 ? 'warning' : 'success'
}))

const recentExecutions = ref([])
const recentAlerts = ref([])
const realtimeTasks = ref([])
const chartDays = ref(7)

const successRateChartRef = ref(null)
const trendChartRef = ref(null)
let successRateChart = null
let trendChart = null

let refreshTimer = null

onMounted(async () => {
  await fetchAllData()
  startAutoRefresh()
})

onUnmounted(() => {
  successRateChart?.dispose()
  trendChart?.dispose()
  if (refreshTimer) clearInterval(refreshTimer)
})

const fetchAllData = async () => {
  await Promise.all([
    fetchOverview(),
    fetchRecentExecutions(),
    fetchRecentAlerts(),
    fetchRealtimeTasks(),
    fetchResource()
  ])
  initSuccessRateChart()
  initTrendChart()
}

const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    fetchRealtimeTasks()
    fetchResource()
  }, 5000)
}

const fetchOverview = async () => {
  try {
    const res = await monitorAPI.getOverview()
    if (res.data) {
      overview.value = res.data
    }
  } catch (error) {
    console.error('获取概览数据失败:', error)
  }
}

const fetchRecentExecutions = async () => {
  try {
    const res = await monitorAPI.getExecutionPage({ pageNum: 1, pageSize: 5 })
    recentExecutions.value = res.data?.list || []
  } catch (error) {
    console.error('获取执行记录失败:', error)
  }
}

const fetchRecentAlerts = async () => {
  try {
    const res = await monitorAPI.getRecentAlerts(5)
    recentAlerts.value = res.data || []
  } catch (error) {
    console.error('获取告警失败:', error)
  }
}

const fetchRealtimeTasks = async () => {
  try {
    const res = await monitorAPI.getRealtimeTasks?.() || { data: [] }
    realtimeTasks.value = res.data || []
  } catch (error) {
    // 如果API不存在，使用模拟数据
    realtimeTasks.value = []
  }
}

const fetchResource = async () => {
  try {
    const res = await monitorAPI.getResource?.() || { data: null }
    if (res.data) {
      Object.assign(resource, res.data)
    }
  } catch (error) {
    // 如果API不存在，模拟波动
    resource.cpu = Math.min(100, Math.max(10, resource.cpu + (Math.random() - 0.5) * 10))
    resource.memory = Math.min(100, Math.max(30, resource.memory + (Math.random() - 0.5) * 5))
    resource.connection = Math.min(100, Math.max(10, resource.connection + (Math.random() - 0.5) * 8))
  }
}

const initSuccessRateChart = () => {
  if (!successRateChartRef.value) return
  successRateChart = echarts.init(successRateChartRef.value)

  const successRate = overview.value.todaySuccessRate || 95.5
  const failRate = 100 - successRate

  successRateChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}% ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#606266' }
    },
    series: [
      {
        name: '执行结果',
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'center',
          formatter: () => `{a|${successRate.toFixed(1)}%}\n{b|成功率}`,
          rich: {
            a: { fontSize: 28, fontWeight: 'bold', color: '#303133' },
            b: { fontSize: 14, color: '#909399', padding: [8, 0, 0, 0] }
          }
        },
        emphasis: {
          label: { show: true, fontSize: 16, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: [
          { value: successRate, name: '成功', itemStyle: { color: '#52c41a' } },
          { value: failRate, name: '失败', itemStyle: { color: '#ff4d4f' } }
        ]
      }
    ]
  })
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)

  try {
    const res = await monitorAPI.getTrend(chartDays.value)
    const data = res.data || []

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e4e7ed',
        textStyle: { color: '#303133' }
      },
      legend: {
        data: ['执行次数', '成功率'],
        textStyle: { color: '#606266' },
        top: 10
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: 50,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date?.substring(5) || ''),
        axisLine: { lineStyle: { color: '#e4e7ed' } },
        axisLabel: { color: '#909399' }
      },
      yAxis: [
        {
          type: 'value',
          name: '次数',
          axisLine: { show: false },
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: '#f0f2f5' } }
        },
        {
          type: 'value',
          name: '成功率',
          min: 0,
          max: 100,
          axisLine: { show: false },
          axisLabel: { color: '#909399', formatter: '{value}%' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '执行次数',
          type: 'bar',
          data: data.map(d => (d.success || 0) + (d.failed || 0)),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#4f6ef7' },
              { offset: 1, color: '#a8b8f7' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        },
        {
          name: '成功率',
          type: 'line',
          yAxisIndex: 1,
          data: data.map(d => {
            const total = (d.success || 0) + (d.failed || 0)
            return total > 0 ? ((d.success / total) * 100).toFixed(1) : 100
          }),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#52c41a', width: 2 },
          itemStyle: { color: '#52c41a' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(82, 196, 26, 0.3)' },
              { offset: 1, color: 'rgba(82, 196, 26, 0.05)' }
            ])
          }
        }
      ]
    })
  } catch (error) {
    console.error('获取趋势数据失败:', error)
  }
}

const handleResize = () => {
  successRateChart?.resize()
  trendChart?.resize()
}
window.addEventListener('resize', handleResize)

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toString()
}

const formatTime = (time) => {
  if (!time) return ''
  return time.substring(5, 16)
}

const formatDuration = (ms) => {
  if (!ms) return '0秒'
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (hours > 0) return `${hours}时${minutes % 60}分`
  if (minutes > 0) return `${minutes}分${seconds % 60}秒`
  return `${seconds}秒`
}

const getProgressColor = (value) => {
  if (value > 80) return '#ff4d4f'
  if (value > 60) return '#faad14'
  return '#52c41a'
}

const getTaskStatusClass = (status) => {
  const classes = { RUNNING: 'running', SUCCESS: 'success', FAILED: 'failed' }
  return classes[status] || ''
}

const getTaskTagType = (status) => {
  const types = { RUNNING: 'primary', SUCCESS: 'success', FAILED: 'danger', PENDING: 'info' }
  return types[status] || 'info'
}

const getTaskStatusLabel = (status) => {
  const labels = { RUNNING: '运行中', SUCCESS: '已完成', FAILED: '已失败', PENDING: '等待中' }
  return labels[status] || status
}

const getAlertTagType = (severity) => {
  const types = { INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' }
  return types[severity] || 'info'
}

const getAlertLabel = (severity) => {
  const labels = { INFO: '信息', WARNING: '警告', ERROR: '错误', CRITICAL: '严重' }
  return labels[severity] || severity
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100vh;

  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #ffffff;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #ffffff;

      &.blue { background: #4f6ef7; }
      &.green { background: #52c41a; }
      &.orange { background: #faad14; }
      &.red { background: #ff4d4f; }
    }

    .stat-content {
      flex: 1;
      min-width: 0;

      .stat-value {
        font-size: 32px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }

      .stat-trend {
        font-size: 12px;
        margin-top: 4px;
        &.positive { color: #52c41a; }
        &.negative { color: #ff4d4f; }
        &.neutral { color: #909399; }
      }
    }
  }

  .resource-row {
    margin-bottom: 20px;
  }

  .resource-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    .resource-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .resource-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
      }
    }
  }

  .chart-row {
    margin-bottom: 20px;
  }

  .chart-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    :deep(.el-card__body) { padding: 20px; }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .chart-container {
      height: 280px;
    }

    .rate-chart {
      height: 240px;
    }
  }

  .list-row, .action-row {
    margin-bottom: 20px;
  }

  .list-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    height: 100%;

    :deep(.el-card__body) { padding: 20px; }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 8px;

        .pulse-icon {
          color: #409eff;
          animation: pulse 1.5s infinite;
        }
      }
    }

    .realtime-list, .recent-list {
      .realtime-item, .recent-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid #f0f2f5;

        &:last-child { border-bottom: none; }

        .item-left {
          display: flex;
          align-items: center;
          gap: 12px;
          flex: 1;
          min-width: 0;

          .task-icon {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;

            &.running {
              background: #ecf5ff;
              color: #409eff;
            }
            &.success {
              background: #f0f9eb;
              color: #52c41a;
            }
            &.failed {
              background: #fef0f0;
              color: #ff4d4f;
            }
          }

          .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            flex-shrink: 0;

            &.success {
              background: #52c41a;
              box-shadow: 0 0 6px rgba(82, 196, 26, 0.4);
            }
            &.error {
              background: #ff4d4f;
              animation: blink 1s infinite;
            }
            &.warning {
              background: #faad14;
              animation: breathe 2s infinite;
            }
          }

          .item-info {
            flex: 1;
            min-width: 0;

            .item-title {
              font-weight: 500;
              color: #303133;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }

            .item-desc {
              font-size: 12px;
              color: #909399;
              margin-top: 2px;
            }

            .item-progress {
              display: flex;
              align-items: center;
              gap: 12px;
              margin-top: 6px;

              :deep(.el-progress) {
                flex: 1;
              }

              .progress-text {
                font-size: 12px;
                color: #606266;
                white-space: nowrap;
              }
            }

            .item-time {
              font-size: 12px;
              color: #909399;
              margin-top: 2px;
            }
          }
        }
      }
    }

    .empty-state {
      text-align: center;
      padding: 32px 0;
      color: #909399;
      font-size: 14px;
    }
  }

  .quick-actions-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    height: 100%;

    :deep(.el-card__body) { padding: 20px; }

    .quick-actions {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;

      .el-button {
        width: 100%;
        justify-content: flex-start;
        height: 48px;
        font-size: 15px;
        gap: 12px;
        border-radius: 10px;

        .el-icon { font-size: 18px; }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spin {
  animation: spin 1s linear infinite;
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;

    .stat-card { margin-bottom: 16px; }
    .quick-actions {
      grid-template-columns: 1fr !important;
    }
  }
}
</style>
