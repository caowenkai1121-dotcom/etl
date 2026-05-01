<template>
  <div class="ops-center-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-trend" :class="card.trendClass">
              <el-icon><component :is="card.trendIcon" /></el-icon>
              {{ card.trend }}
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 任务依赖关系可视化 + 实时执行状态 -->
    <el-row :gutter="20" class="middle-row">
      <el-col :xs="24" :lg="14">
        <el-card class="relation-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Share /></el-icon> 任务依赖关系</span>
              <el-radio-group v-model="relationView" size="small">
                <el-radio-button value="graph">图谱</el-radio-button>
                <el-radio-button value="tree">树形</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div v-if="relationView === 'graph'" ref="relationChartRef" class="chart-container"></div>
          <div v-else class="tree-container">
            <el-tree
              :data="treeData"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              default-expand-all
              :expand-on-click-node="false"
            >
              <template #default="{ node }">
                <div class="tree-node">
                  <span class="node-name">{{ node.label }}</span>
                  <el-tag :type="node.status === 'RUNNING' ? 'primary' : node.status === 'SUCCESS' ? 'success' : 'info'" size="small">
                    {{ getStatusLabel(node.status) }}
                  </el-tag>
                </div>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon class="pulse-icon"><Loading /></el-icon>
                实时执行状态
              </span>
              <el-tag type="success" size="small">{{ runningCount }}个运行中</el-tag>
            </div>
          </template>
          <div class="realtime-list">
            <div v-for="item in runningInstances" :key="item.id" class="realtime-item">
              <div class="item-status-indicator" :class="item.status?.toLowerCase()"></div>
              <div class="item-info">
                <div class="item-name">{{ item.taskName }}</div>
                <div class="item-detail">
                  <span>{{ item.startTime }}</span>
                  <span>已运行 {{ item.duration }}</span>
                </div>
                <el-progress
                  :percentage="item.progress || 0"
                  :stroke-width="4"
                  :color="item.status === 'FAILED' ? '#ff4d4f' : '#4f6ef7'"
                  style="margin-top: 6px"
                />
              </div>
              <el-tag :type="getStatusTagType(item.status)" size="small">{{ getStatusLabel(item.status) }}</el-tag>
            </div>
            <div v-if="runningInstances.length === 0" class="empty-state">暂无运行中实例</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务运行实例列表 -->
    <el-card class="instance-card">
      <template #header>
        <div class="card-header">
          <span class="card-title"><el-icon><List /></el-icon> 任务运行实例</span>
          <div class="header-actions">
            <el-input v-model="searchKeyword" placeholder="搜索任务名称" clearable style="width: 200px; margin-right: 12px">
              <template #append><el-button @click="fetchData"><el-icon><Search /></el-icon></el-button></template>
            </el-input>
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 120px" @change="fetchData">
              <el-option label="全部" value="" />
              <el-option label="运行中" value="RUNNING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
              <el-option label="等待中" value="PENDING" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="task-name-link">{{ row.taskName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerType" label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.triggerType || '手动' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="duration" label="耗时" width="100" />
        <el-table-column prop="syncRows" label="处理行数" width="110">
          <template #default="{ row }">
            {{ formatNumber(row.syncRows || 0) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewLog(row)">
              <el-icon><Tickets /></el-icon>
              日志
            </el-button>
            <el-button v-if="row.status === 'RUNNING'" size="small" type="danger" @click="handleKill(row)">
              <el-icon><SwitchButton /></el-icon>
              终止
            </el-button>
            <el-button v-if="row.status === 'FAILED'" size="small" type="primary" @click="handleRetry(row)">
              <el-icon><RefreshRight /></el-icon>
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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

    <!-- 告警管理 + 操作日志 -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :xs="24" :lg="12">
        <el-card class="alert-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Bell /></el-icon> 告警管理</span>
              <el-button text type="primary" @click="$router.push('/alert')">告警中心</el-button>
            </div>
          </template>
          <div class="alert-list">
            <div v-for="item in alerts" :key="item.id" class="alert-item">
              <div class="alert-left">
                <span class="alert-severity-dot" :class="item.severity?.toLowerCase()"></span>
                <div class="alert-info">
                  <div class="alert-title">{{ item.alertType }} - {{ item.title }}</div>
                  <div class="alert-time">{{ item.createTime }}</div>
                </div>
              </div>
              <el-tag :type="getAlertTagType(item.severity)" size="small">{{ item.severity }}</el-tag>
            </div>
            <div v-if="alerts.length === 0" class="empty-state">暂无告警</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="log-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Document /></el-icon> 操作日志</span>
              <el-button text type="primary" @click="router.push('/log')">更多</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="item in operationLogs"
              :key="item.id"
              :timestamp="item.time"
              :color="item.color"
              placement="top"
            >
              <div class="log-item">
                <span class="log-operator">{{ item.operator }}</span>
                <span class="log-action">{{ item.action }}</span>
                <span class="log-target">{{ item.target }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')
const relationView = ref('graph')
const relationChartRef = ref(null)
let relationChart = null
let refreshTimer = null

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const statCards = ref([
  { icon: 'Timer', value: 156, label: '今日执行总数', trend: '+12.5%', trendIcon: 'Top', trendClass: 'positive', color: 'linear-gradient(135deg, #4f6ef7, #7088f9)' },
  { icon: 'CircleCheck', value: '98.2%', label: '成功率', trend: '+0.8%', trendIcon: 'Top', trendClass: 'positive', color: 'linear-gradient(135deg, #52c41a, #73d13d)' },
  { icon: 'Warning', value: 3, label: '活跃告警', trend: '-2', trendIcon: 'Bottom', trendClass: 'positive', color: 'linear-gradient(135deg, #faad14, #ffc53d)' },
  { icon: 'Loading', value: 5, label: '运行中实例', trend: '实时', trendIcon: 'Loading', trendClass: 'neutral', color: 'linear-gradient(135deg, #13c2c2, #36cfc9)' }
])

const runningCount = computed(() => runningInstances.value.filter(i => i.status === 'RUNNING').length)

const runningInstances = ref([
  { id: 1, taskName: 'MySQL->Doris订单同步', status: 'RUNNING', startTime: '10:30:00', duration: '1h23m', progress: 75 },
  { id: 2, taskName: '日志数据清洗ETL', status: 'RUNNING', startTime: '11:00:00', duration: '53m', progress: 45 },
  { id: 3, taskName: '用户行为数据聚合', status: 'SUCCESS', startTime: '09:00:00', duration: '12m', progress: 100 },
  { id: 4, taskName: '数据质量检查', status: 'RUNNING', startTime: '11:30:00', duration: '23m', progress: 20 },
  { id: 5, taskName: 'API调用统计', status: 'FAILED', startTime: '10:45:00', duration: '5m', progress: 15 }
])

const alerts = ref([
  { id: 1, alertType: '任务失败', title: 'API调用统计任务执行失败', severity: 'ERROR', createTime: '2024-12-20 11:15:00' },
  { id: 2, alertType: '延迟告警', title: 'MySQL->Doris同步延迟超过阈值', severity: 'WARNING', createTime: '2024-12-20 10:50:00' },
  { id: 3, alertType: '资源告警', title: 'CPU使用率超过80%', severity: 'WARNING', createTime: '2024-12-20 10:30:00' }
])

const operationLogs = ref([
  { id: 1, operator: 'admin', action: '创建任务', target: 'MySQL->Doris实时同步', time: '2024-12-20 11:00', color: '#52c41a' },
  { id: 2, operator: 'zhangsan', action: '修改调度配置', target: '日志ETL清洗任务', time: '2024-12-20 10:45', color: '#4f6ef7' },
  { id: 3, operator: 'admin', action: '下线API', target: '/api/user/query', time: '2024-12-20 10:30', color: '#faad14' },
  { id: 4, operator: 'lisi', action: '发布工作流', target: '订单数据工作流 v2.3', time: '2024-12-20 10:00', color: '#52c41a' },
  { id: 5, operator: 'admin', action: '删除数据源', target: '测试MySQL(已下线)', time: '2024-12-20 09:30', color: '#ff4d4f' }
])

const treeData = ref([
  {
    id: 'root',
    name: '订单数据工作流',
    status: 'SUCCESS',
    children: [
      {
        id: '1', name: '订单增量抽取', status: 'SUCCESS',
        children: [
          { id: '1-1', name: 'MySQL源CDC读取', status: 'SUCCESS' },
          { id: '1-2', name: '字段映射转换', status: 'SUCCESS' }
        ]
      },
      {
        id: '2', name: '订单数据清洗', status: 'RUNNING',
        children: [
          { id: '2-1', name: '数据去重', status: 'SUCCESS' },
          { id: '2-2', name: '空值处理', status: 'RUNNING' },
          { id: '2-3', name: '格式标准化', status: 'PENDING' }
        ]
      },
      {
        id: '3', name: '写入Doris', status: 'PENDING',
        children: [
          { id: '3-1', name: '分区计算', status: 'PENDING' },
          { id: '3-2', name: '批量写入', status: 'PENDING' }
        ]
      }
    ]
  }
])

onMounted(() => {
  fetchData()
  initRelationChart()
  startAutoRefresh()
})

onUnmounted(() => {
  relationChart?.dispose()
  if (refreshTimer) clearInterval(refreshTimer)
})

const fetchData = async () => {
  loading.value = true
  try {
    // 模拟数据 - 实际应调用API
    tableData.value = [
      { id: 1, taskName: 'MySQL->Doris订单同步', status: 'RUNNING', triggerType: '定时', startTime: '2024-12-20 10:30:00', endTime: '-', duration: '1h23m', syncRows: 1520000 },
      { id: 2, taskName: '日志数据清洗ETL', status: 'RUNNING', triggerType: '定时', startTime: '2024-12-20 11:00:00', endTime: '-', duration: '53m', syncRows: 890000 },
      { id: 3, taskName: '用户行为数据聚合', status: 'SUCCESS', triggerType: '手动', startTime: '2024-12-20 09:00:00', endTime: '2024-12-20 09:12:00', duration: '12m', syncRows: 560000 },
      { id: 4, taskName: '数据质量检查', status: 'RUNNING', triggerType: '定时', startTime: '2024-12-20 11:30:00', endTime: '-', duration: '23m', syncRows: 320000 },
      { id: 5, taskName: 'API调用统计', status: 'FAILED', triggerType: '定时', startTime: '2024-12-20 10:45:00', endTime: '2024-12-20 10:50:00', duration: '5m', syncRows: 45000 },
      { id: 6, taskName: '商品信息全量同步', status: 'SUCCESS', triggerType: '手动', startTime: '2024-12-20 08:00:00', endTime: '2024-12-20 08:35:00', duration: '35m', syncRows: 3200000 },
      { id: 7, taskName: '库存实时CDC同步', status: 'RUNNING', triggerType: '实时', startTime: '2024-12-20 00:00:00', endTime: '-', duration: '11h53m', syncRows: 8900000 },
      { id: 8, taskName: '数据血缘更新', status: 'SUCCESS', triggerType: '定时', startTime: '2024-12-20 07:00:00', endTime: '2024-12-20 07:05:00', duration: '5m', syncRows: 12000 }
    ]
    total.value = 45
  } catch (e) {
    console.error('获取实例列表失败:', e)
  } finally {
    loading.value = false
  }
}

const initRelationChart = async () => {
  await nextTick()
  if (!relationChartRef.value) return
  relationChart = echarts.init(relationChartRef.value)

  const nodes = [
    { id: '0', name: '订单抽取', x: 100, y: 150, itemStyle: { color: '#52c41a' }, symbolSize: 50 },
    { id: '1', name: '数据去重', x: 300, y: 50, itemStyle: { color: '#4f6ef7' }, symbolSize: 45 },
    { id: '2', name: '空值处理', x: 300, y: 250, itemStyle: { color: '#4f6ef7' }, symbolSize: 40 },
    { id: '3', name: '字段转换', x: 500, y: 80, itemStyle: { color: '#4f6ef7' }, symbolSize: 40 },
    { id: '4', name: '格式校验', x: 500, y: 220, itemStyle: { color: '#faad14' }, symbolSize: 40 },
    { id: '5', name: '质量检查', x: 700, y: 100, itemStyle: { color: '#13c2c2' }, symbolSize: 40 },
    { id: '6', name: '写入Doris', x: 700, y: 200, itemStyle: { color: '#52c41a' }, symbolSize: 45 }
  ]

  const links = [
    { source: '0', target: '1' },
    { source: '0', target: '2' },
    { source: '1', target: '3' },
    { source: '2', target: '4' },
    { source: '3', target: '5' },
    { source: '4', target: '5' },
    { source: '5', target: '6' }
  ]

  relationChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    series: [{
      type: 'graph',
      layout: 'none',
      roam: true,
      draggable: true,
      data: nodes.map(n => ({ ...n, name: n.name, value: n.symbolSize })),
      links: links,
      lineStyle: {
        color: '#c0c4cc',
        curveness: 0.2
      },
      label: {
        show: true,
        position: 'bottom',
        fontSize: 12,
        color: '#606266'
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3 }
      }
    }]
  })
}

const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    // 模拟数据刷新
    runningInstances.value.forEach(item => {
      if (item.status === 'RUNNING') {
        item.progress = Math.min(100, (item.progress || 0) + Math.floor(Math.random() * 3))
        if (item.progress >= 100) {
          item.status = 'SUCCESS'
        }
      }
    })
  }, 5000)
}

const handleViewLog = (row) => {
  router.push({ path: '/log', query: { task: row.taskName } })
}

const handleKill = async (row) => {
  try {
    await ElMessageBox.confirm(`确定终止任务 "${row.taskName}"？`, '危险操作', { type: 'warning', confirmButtonText: '确定终止' })
    ElMessage.success(`任务 "${row.taskName}" 已终止`)
  } catch (e) {
    // 取消操作
  }
}

const handleRetry = (row) => {
  ElMessage.success(`任务 "${row.taskName}" 已重新提交`)
}

const getStatusLabel = (status) => {
  const map = { RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败', PENDING: '等待中' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { RUNNING: 'primary', SUCCESS: 'success', FAILED: 'danger', PENDING: 'info' }
  return map[status] || 'info'
}

const getAlertTagType = (severity) => {
  const map = { INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' }
  return map[severity] || 'info'
}

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}
</script>

<style lang="scss" scoped>
.ops-center-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100%;

  .stats-row { margin-bottom: 20px; }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
    }

    .stat-icon {
      width: 52px;
      height: 52px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }

    .stat-content {
      flex: 1;
      min-width: 0;

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }
      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
      .stat-trend {
        font-size: 12px;
        margin-top: 4px;
        display: flex;
        align-items: center;
        gap: 4px;

        &.positive { color: #52c41a; }
        &.negative { color: #ff4d4f; }
        &.neutral { color: #4f6ef7; }
      }
    }
  }

  .middle-row { margin-bottom: 20px; }

  .relation-card, .status-card, .instance-card, .alert-card, .log-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    margin-bottom: 20px;

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

      .header-actions {
        display: flex;
        align-items: center;
      }
    }

    .chart-container {
      height: 320px;
    }
  }

  .tree-container {
    padding: 16px;
    max-height: 320px;
    overflow-y: auto;

    .tree-node {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      padding-right: 12px;

      .node-name {
        font-size: 14px;
        color: #303133;
      }
    }
  }

  .realtime-list {
    max-height: 360px;
    overflow-y: auto;

    .realtime-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 0;
      border-bottom: 1px solid #f0f2f5;

      &:last-child { border-bottom: none; }

      .item-status-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-top: 6px;
        flex-shrink: 0;

        &.running { background: #4f6ef7; animation: pulse 1.5s infinite; }
        &.success { background: #52c41a; }
        &.failed { background: #ff4d4f; }
      }

      .item-info {
        flex: 1;
        min-width: 0;

        .item-name {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
        }
        .item-detail {
          display: flex;
          gap: 16px;
          font-size: 12px;
          color: #909399;
          margin-top: 4px;
        }
      }
    }

    .empty-state {
      text-align: center;
      padding: 40px 0;
      color: #909399;
      font-size: 14px;
    }
  }

  .task-name-link {
    color: #4f6ef7;
    cursor: pointer;
    &:hover { text-decoration: underline; }
  }

  .pagination-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .alert-list {
    .alert-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f2f5;

      &:last-child { border-bottom: none; }

      .alert-left {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;
        min-width: 0;

        .alert-severity-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          flex-shrink: 0;

          &.critical, &.error { background: #ff4d4f; }
          &.warning { background: #faad14; }
          &.info { background: #4f6ef7; }
        }

        .alert-info {
          flex: 1;
          min-width: 0;
          .alert-title { font-size: 14px; color: #303133; }
          .alert-time { font-size: 12px; color: #909399; margin-top: 2px; }
        }
      }
    }

    .empty-state {
      text-align: center;
      padding: 32px 0;
      color: #909399;
    }
  }

  .log-item {
    .log-operator {
      font-weight: 600;
      color: #303133;
      margin-right: 8px;
    }
    .log-action {
      color: #4f6ef7;
      margin-right: 8px;
    }
    .log-target {
      color: #909399;
    }
  }

  .bottom-row { margin-bottom: 20px; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 768px) {
  .ops-center-page {
    padding: 16px;
  }
}
</style>
