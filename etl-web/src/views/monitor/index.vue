<template>
  <div class="monitor-page">
    <!-- 顶部时间范围选择 -->
    <div class="top-bar">
      <div class="left">
        <el-radio-group v-model="timeRange" size="default">
          <el-radio-button value="5m">5分钟</el-radio-button>
          <el-radio-button value="15m">15分钟</el-radio-button>
          <el-radio-button value="1h">1小时</el-radio-button>
          <el-radio-button value="6h">6小时</el-radio-button>
        </el-radio-group>
        <el-select v-model="targetTask" placeholder="选择任务" clearable style="width: 180px; margin-left: 12px;">
          <el-option label="全部任务" value="" />
          <el-option label="MySQL->Doris订单同步" value="task-1" />
          <el-option label="日志数据清洗ETL" value="task-2" />
          <el-option label="用户行为聚合" value="task-3" />
        </el-select>
      </div>
      <div class="right">
        <span class="refresh-info">
          <el-icon class="spin-icon"><Loading /></el-icon>
          每2秒自动更新
        </span>
        <el-button size="small" @click="refreshAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 实时数据概览 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :xs="12" :sm="6" v-for="card in overviewCards" :key="card.label">
        <div class="overview-card">
          <div class="ov-icon" :style="{ background: card.bg }"><el-icon :size="20"><component :is="card.icon" /></el-icon></div>
          <div class="ov-body">
            <div class="ov-value" :style="{ color: card.valueColor }">{{ card.value }}</div>
            <div class="ov-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 2x2 图表网格 -->
    <el-row :gutter="20" class="chart-grid">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon class="title-icon"><TrendCharts /></el-icon>吞吐量 (条/秒)</span>
              <el-tag size="small" type="success">实时</el-tag>
            </div>
          </template>
          <div ref="throughputChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon class="title-icon"><DataLine /></el-icon>同步延迟 (ms)</span>
              <el-radio-group v-model="delayView" size="small" @change="initDelayChart">
                <el-radio-button value="heatmap">热力图</el-radio-button>
                <el-radio-button value="line">曲线</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="delayChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon class="title-icon"><Monitor /></el-icon>资源使用率</span>
            </div>
          </template>
          <div ref="resourceChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon class="title-icon"><PieChart /></el-icon>操作类型分布</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 告警快照 -->
    <el-card class="alert-card">
      <template #header>
        <div class="card-header">
          <span class="card-title"><el-icon class="title-icon"><Bell /></el-icon>告警快照（最近10条）</span>
          <el-button text type="primary" @click="$router.push('/alert')">告警中心</el-button>
        </div>
      </template>
      <div class="alert-snapshots">
        <div v-for="alert in alertSnapshots" :key="alert.id" class="alert-snap-item">
          <div class="snap-left">
            <span class="snap-dot" :class="alert.severity?.toLowerCase()"></span>
            <div class="snap-info">
              <span class="snap-msg">{{ alert.message }}</span>
              <span class="snap-time">{{ alert.time }}</span>
            </div>
          </div>
          <el-tag :type="getAlertTag(alert.severity)" size="small">{{ alert.severity }}</el-tag>
        </div>
        <div v-if="alertSnapshots.length === 0" class="empty-state">暂无告警</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const timeRange = ref('15m')
const targetTask = ref('')
const delayView = ref('heatmap')

const throughputChartRef = ref(null)
const delayChartRef = ref(null)
const resourceChartRef = ref(null)
const pieChartRef = ref(null)

let throughputChart = null
let delayChart = null
let resourceChart = null
let pieChart = null
let refreshTimer = null

const overviewCards = reactive([
  { label: '当前TPS', value: '1,280', valueColor: '#4f6ef7', icon: 'TrendCharts', bg: 'linear-gradient(135deg, #ecf5ff, #d9ecff)' },
  { label: '平均延迟', value: '85ms', valueColor: '#52c41a', icon: 'Timer', bg: 'linear-gradient(135deg, #f0f9eb, #e1f3d8)' },
  { label: 'CPU使用率', value: '62%', valueColor: '#faad14', icon: 'Monitor', bg: 'linear-gradient(135deg, #fdf6ec, #fae6cc)' },
  { label: '内存使用率', value: '48%', valueColor: '#13c2c2', icon: 'DataAnalysis', bg: 'linear-gradient(135deg, #e6fffb, #d9f7f3)' }
])

const alertSnapshots = ref([
  { id: 1, severity: 'WARNING', message: 'MySQL->Doris订单同步延迟超过阈值(200ms)', time: '11:45:30' },
  { id: 2, severity: 'INFO', message: '日志ETL任务执行完成，处理89万条记录', time: '11:30:00' },
  { id: 3, severity: 'ERROR', message: 'API调用统计任务执行失败: 连接超时', time: '11:15:00' }
])

onMounted(async () => {
  await nextTick()
  initAllCharts()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
  throughputChart?.dispose()
  delayChart?.dispose()
  resourceChart?.dispose()
  pieChart?.dispose()
})

const initAllCharts = () => {
  initThroughputChart()
  initDelayChart()
  initResourceChart()
  initPieChart()
}

const initThroughputChart = () => {
  if (!throughputChartRef.value) return
  throughputChart = echarts.init(throughputChartRef.value)
  const data = []
  for (let i = 59; i >= 0; i--) {
    const time = new Date(Date.now() - i * 2000)
    data.push({
      time: time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      read: Math.floor(Math.random() * 800) + 200,
      write: Math.floor(Math.random() * 500) + 100
    })
  }

  throughputChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['读取', '写入'],
      top: 0,
      textStyle: { color: '#606266' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.time),
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399', fontSize: 10, interval: 9 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '读取',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: data.map(d => d.read),
        lineStyle: { color: '#4f6ef7', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(79, 110, 247, 0.15)' }, { offset: 1, color: 'rgba(79, 110, 247, 0.01)' }]) }
      },
      {
        name: '写入',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: data.map(d => d.write),
        lineStyle: { color: '#52c41a', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(82, 196, 26, 0.15)' }, { offset: 1, color: 'rgba(82, 196, 26, 0.01)' }]) }
      }
    ]
  })
}

const initDelayChart = () => {
  if (!delayChartRef.value) return
  delayChart?.dispose()
  delayChart = echarts.init(delayChartRef.value)

  if (delayView.value === 'heatmap') {
    const hours = ['00', '02', '04', '06', '08', '10', '12', '14', '16', '18', '20', '22']
    const tasks = ['订单同步', '日志ETL', '行为聚合', '质量检查', 'API统计', '商品同步']
    const hmData = []
    tasks.forEach((_, i) => hours.forEach((_, j) => hmData.push([j, i, Math.floor(Math.random() * 100)])))

    delayChart.setOption({
      tooltip: { position: 'top', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#e4e7ed' },
      grid: { left: '12%', right: '8%', top: '5%', bottom: '15%' },
      xAxis: { type: 'category', data: hours, axisLabel: { color: '#909399' } },
      yAxis: { type: 'category', data: tasks, axisLabel: { color: '#909399' } },
      visualMap: { min: 0, max: 100, calculable: true, orient: 'horizontal', left: 'center', bottom: '0%', inRange: { color: ['#52c41a', '#faad14', '#ff4d4f'] } },
      series: [{ name: '延迟', type: 'heatmap', data: hmData, label: { show: false } }]
    })
  } else {
    const lineData = []
    const now = new Date()
    for (let i = 59; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60000)
      lineData.push({
        time: time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        value: Math.floor(Math.random() * 80) + 20
      })
    }
    delayChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#e4e7ed', textStyle: { color: '#303133' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: 10, containLabel: true },
      xAxis: { type: 'category', data: lineData.map(d => d.time), axisLine: { lineStyle: { color: '#dcdfe6' } }, axisLabel: { color: '#909399', fontSize: 10, interval: 9 } },
      yAxis: { type: 'value', name: 'ms', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
      series: [{
        type: 'line', data: lineData.map(d => d.value), smooth: true, symbol: 'none',
        lineStyle: { color: '#faad14', width: 2 },
        markLine: { silent: true, data: [{ yAxis: 100, label: { formatter: '阈值: 100ms' }, lineStyle: { color: '#ff4d4f', type: 'dashed' } }] }
      }]
    })
  }
}

const initResourceChart = () => {
  if (!resourceChartRef.value) return
  resourceChart = echarts.init(resourceChartRef.value)

  resourceChart.setOption({
    tooltip: { backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#e4e7ed', textStyle: { color: '#303133' } },
    series: [
      {
        type: 'gauge', startAngle: 180, endAngle: 0, center: ['25%', '58%'], radius: '65%', min: 0, max: 100,
        progress: { show: true, width: 16 }, pointer: { show: false },
        axisLine: { lineStyle: { width: 16, color: [[0.6, '#4f6ef7'], [0.8, '#faad14'], [1, '#ff4d4f']] } },
        axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
        title: { show: true, offsetCenter: [0, '30%'], fontSize: 13, color: '#606266' },
        detail: { valueAnimation: true, fontSize: 20, fontWeight: 'bold', color: '#303133', offsetCenter: [0, '0%'], formatter: '{value}%' },
        data: [{ value: 62, name: 'CPU' }]
      },
      {
        type: 'gauge', startAngle: 180, endAngle: 0, center: ['75%', '58%'], radius: '65%', min: 0, max: 100,
        progress: { show: true, width: 16 }, pointer: { show: false },
        axisLine: { lineStyle: { width: 16, color: [[0.6, '#52c41a'], [0.8, '#faad14'], [1, '#ff4d4f']] } },
        axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
        title: { show: true, offsetCenter: [0, '30%'], fontSize: 13, color: '#606266' },
        detail: { valueAnimation: true, fontSize: 20, fontWeight: 'bold', color: '#303133', offsetCenter: [0, '0%'], formatter: '{value}%' },
        data: [{ value: 48, name: '内存' }]
      }
    ]
  })
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { color: '#606266' } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['42%', '50%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: [
        { value: 45, name: 'INSERT', itemStyle: { color: '#52c41a' } },
        { value: 30, name: 'UPDATE', itemStyle: { color: '#4f6ef7' } },
        { value: 15, name: 'DELETE', itemStyle: { color: '#ff4d4f' } },
        { value: 10, name: 'OTHER', itemStyle: { color: '#faad14' } }
      ]
    }]
  })
}

const updateCharts = () => {
  if (throughputChart) {
    const data = []
    for (let i = 59; i >= 0; i--) data.push({ read: Math.floor(Math.random() * 800) + 200, write: Math.floor(Math.random() * 500) + 100 })
    throughputChart.setOption({ series: [{ data: data.map(d => d.read) }, { data: data.map(d => d.write) }] })
  }
  if (resourceChart) {
    resourceChart.setOption({
      series: [
        { data: [{ value: Math.floor(Math.random() * 30) + 50, name: 'CPU' }] },
        { data: [{ value: Math.floor(Math.random() * 20) + 40, name: '内存' }] }
      ]
    })
  }
  overviewCards[0].value = (Math.floor(Math.random() * 500) + 1000).toLocaleString()
  overviewCards[1].value = (Math.floor(Math.random() * 50) + 50) + 'ms'
}

const startAutoRefresh = () => { refreshTimer = setInterval(updateCharts, 2000) }
const stopAutoRefresh = () => { if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null } }
const refreshAll = () => { updateCharts(); initDelayChart() }

const getAlertTag = (severity) => {
  const map = { INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' }
  return map[severity] || 'info'
}
</script>

<style lang="scss" scoped>
.monitor-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100%;

  .top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;

    .right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .refresh-info {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #909399;
      font-size: 13px;

      .spin-icon { animation: spin 2s linear infinite; color: #4f6ef7; }
    }
  }

  .overview-row { margin-bottom: 20px; }

  .overview-card {
    display: flex;
    align-items: center;
    gap: 14px;
    background: #fff;
    border-radius: 12px;
    padding: 18px 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    margin-bottom: 16px;

    .ov-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .ov-body {
      .ov-value { font-size: 22px; font-weight: 700; }
      .ov-label { font-size: 13px; color: #909399; margin-top: 2px; }
    }
  }

  .chart-grid {
    .chart-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
      margin-bottom: 20px;

      :deep(.el-card__header) { padding: 16px 20px; border-bottom: 1px solid #f0f2f5; }

      .card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .card-title {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
          display: flex;
          align-items: center;
          gap: 8px;

          .title-icon { color: #4f6ef7; }
        }
      }

      .chart-container { height: 280px; }
    }
  }

  .alert-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

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

        .title-icon { color: #4f6ef7; }
      }
    }

    .alert-snapshots {
      .alert-snap-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 0;
        border-bottom: 1px solid #f5f7fa;

        &:last-child { border-bottom: none; }

        .snap-left {
          display: flex;
          align-items: center;
          gap: 10px;
          flex: 1;
          min-width: 0;

          .snap-dot {
            width: 8px; height: 8px; border-radius: 50%;
            &.warning { background: #faad14; }
            &.error { background: #ff4d4f; }
            &.info { background: #4f6ef7; }
          }

          .snap-info {
            .snap-msg { font-size: 13px; color: #303133; }
            .snap-time { font-size: 12px; color: #909399; margin-left: 12px; }
          }
        }
      }
      .empty-state { text-align: center; padding: 24px 0; color: #909399; }
    }
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .monitor-page { padding: 16px; }
}
</style>
