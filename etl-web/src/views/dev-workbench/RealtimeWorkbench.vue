<template>
  <div class="realtime-workbench">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner realtime">
      <div class="welcome-content">
        <h1 class="welcome-title">实时任务开发</h1>
        <p class="welcome-desc">基于CDC的实时数据同步管道，支持MySQL Binlog、Kafka等实时数据源的低延迟同步</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" @click="$router.push('/realtime/task/new')">
            <el-icon><Plus /></el-icon> 新建实时管道
          </el-button>
          <el-button size="large" @click="$router.push('/cdc-config')">
            <el-icon><Setting /></el-icon> CDC配置
          </el-button>
          <el-button size="large" @click="$router.push('/monitor')">
            <el-icon><Monitor /></el-icon> 实时监控
          </el-button>
        </div>
      </div>
      <div class="welcome-illustration">
        <div class="pulse-ring r1"></div>
        <div class="pulse-ring r2"></div>
        <div class="pulse-ring r3"></div>
      </div>
    </div>

    <!-- 概览统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap blue"><el-icon :size="20"><Connection /></el-icon></div>
            <span class="stat-trend up">+3 <el-icon><Top /></el-icon></span>
          </div>
          <div class="stat-value">{{ overview.totalPipelines }}</div>
          <div class="stat-label">管道总数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap green"><el-icon :size="20"><VideoPlay /></el-icon></div>
            <span class="stat-info">运行中</span>
          </div>
          <div class="stat-value">{{ overview.runningPipelines }}</div>
          <div class="stat-label">运行中管道</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap orange"><el-icon :size="20"><TrendCharts /></el-icon></div>
            <span class="stat-trend up">+12%</span>
          </div>
          <div class="stat-value">{{ overview.avgTps }}</div>
          <div class="stat-label">平均TPS</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-icon-wrap cyan"><el-icon :size="20"><Timer /></el-icon></div>
            <span class="stat-trend down">-8ms</span>
          </div>
          <div class="stat-value">{{ overview.avgLatency }}</div>
          <div class="stat-label">平均延迟</div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="quick-actions-row">
      <el-col :span="8">
        <div class="quick-action-card" @click="$router.push('/realtime/task/new')">
          <div class="qa-icon mysql"><el-icon :size="24"><Connection /></el-icon></div>
          <div class="qa-info">
            <div class="qa-title">MySQL Binlog管道</div>
            <div class="qa-desc">基于MySQL Binlog的CDC实时同步通道</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="quick-action-card" @click="$router.push('/realtime/task/new?type=kafka')">
          <div class="qa-icon kafka"><el-icon :size="24"><Promotion /></el-icon></div>
          <div class="qa-info">
            <div class="qa-title">Kafka消息管道</div>
            <div class="qa-desc">Kafka Topic到目标库的实时流处理</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="quick-action-card" @click="$router.push('/realtime/task/new?type=oracle')">
          <div class="qa-icon oracle"><el-icon :size="24"><Coin /></el-icon></div>
          <div class="qa-info">
            <div class="qa-title">Oracle LogMiner管道</div>
            <div class="qa-desc">Oracle数据库的增量日志挖掘同步</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 管道列表区域 -->
    <div class="task-section">
      <div class="section-title-bar">
        <div class="tab-switcher">
          <span class="tab-item" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部管道</span>
          <span class="tab-item" :class="{ active: activeTab === 'running' }" @click="activeTab = 'running'">运行中</span>
          <span class="tab-item" :class="{ active: activeTab === 'stopped' }" @click="activeTab = 'stopped'">已停止</span>
          <span class="tab-item" :class="{ active: activeTab === 'error' }" @click="activeTab = 'error'">异常</span>
        </div>
        <div class="title-actions">
          <el-input v-model="searchKeyword" placeholder="搜索管道..." size="small" clearable style="width: 200px">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
      </div>

      <el-table :data="pipeList" stripe v-loading="loading" class="task-table" @row-click="openTask">
        <el-table-column prop="name" label="管道名称" min-width="200">
          <template #default="{ row }">
            <div class="pipe-name-cell">
              <span class="status-dot" :class="row.status === 'RUNNING' ? 'running' : row.status === 'STOPPED' ? 'stopped' : 'error'"></span>
              <span class="pipe-name">{{ row.name }}</span>
              <el-tag v-if="row.syncType" size="small" effect="plain" type="info">{{ row.syncType }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="源→目标" min-width="240">
          <template #default="{ row }">
            <div class="source-target">
              <el-tag size="small" effect="plain">{{ row.sourceName }}</el-tag>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
              <el-tag size="small" effect="plain">{{ row.targetName }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="syncType" label="同步模式" width="110">
          <template #default="{ row }">
            <el-tag :type="row.syncType === 'CDC' ? 'warning' : 'success'" size="small" effect="light">
              {{ row.syncType || 'CDC' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentTps" label="当前TPS" width="100" sortable>
          <template #default="{ row }">
            <span class="tps-value">{{ row.currentTps || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="latency" label="同步延迟" width="110" sortable>
          <template #default="{ row }">
            <span :class="row.latency > 200 ? 'latency-high' : row.latency > 100 ? 'latency-warn' : 'latency-ok'">
              {{ row.latency }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="uptime" label="运行时长" width="120">
          <template #default="{ row }">{{ row.uptime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }" @click.stop>
            <div class="row-actions">
              <template v-if="row.status === 'RUNNING'">
                <el-button size="small" type="warning" @click.stop="stopPipe(row)">
                  <el-icon><VideoPause /></el-icon> 停止
                </el-button>
                <el-button size="small" @click.stop="restartPipe(row)">
                  <el-icon><Refresh /></el-icon> 重启
                </el-button>
              </template>
              <template v-else>
                <el-button size="small" type="success" @click.stop="startPipe(row)">
                  <el-icon><VideoPlay /></el-icon> 启动
                </el-button>
              </template>
              <el-button size="small" @click.stop="editTask(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
                <el-button size="small"><el-icon><MoreFilled /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="monitor">监控面板</el-dropdown-item>
                    <el-dropdown-item command="log">查看日志</el-dropdown-item>
                    <el-dropdown-item command="copy">复制管道</el-dropdown-item>
                    <el-dropdown-item command="alarm">告警配置</el-dropdown-item>
                    <el-dropdown-item divided command="delete" style="color:#ff4d4f">删除管道</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span class="footer-info">共 {{ total }} 条记录</span>
        <el-pagination
          v-if="total > 10"
          small layout="prev, pager, next"
          :total="total" :page-size="10"
          v-model:current-page="currentPage"
          @current-change="loadPipeList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const activeTab = ref('all')
const searchKeyword = ref('')
const currentPage = ref(1)
const total = ref(0)

const overview = reactive({
  totalPipelines: 42,
  runningPipelines: 28,
  avgTps: '1,850',
  avgLatency: '65ms'
})

const pipeList = ref([
  { id: 1, name: '订单数据实时同步', sourceName: 'MySQL生产库', targetName: 'Doris分析库', syncType: 'CDC', status: 'RUNNING', currentTps: 850, latency: 45, uptime: '15天 8小时', createTime: '2024-12-01 10:00' },
  { id: 2, name: '用户行为流处理', sourceName: 'Kafka-Topic', targetName: 'ClickHouse', syncType: 'STREAM', status: 'RUNNING', currentTps: 3200, latency: 12, uptime: '30天 2小时', createTime: '2024-11-15 14:30' },
  { id: 3, name: '商品库存CDC同步', sourceName: 'MySQL商品库', targetName: 'Redis缓存', syncType: 'CDC', status: 'RUNNING', currentTps: 420, latency: 85, uptime: '7天 16小时', createTime: '2024-12-18 09:00' },
  { id: 4, name: '日志数据管道', sourceName: 'Kafka-Log', targetName: 'Elasticsearch', syncType: 'STREAM', status: 'STOPPED', currentTps: 0, latency: 0, uptime: '-', createTime: '2024-12-10 11:00' },
  { id: 5, name: 'Oracle财务同步', sourceName: 'Oracle ERP', targetName: 'Doris财务库', syncType: 'CDC', status: 'ERROR', currentTps: 0, latency: 999, uptime: '2天 5小时', createTime: '2024-12-05 08:30' },
  { id: 6, name: '会员数据管道', sourceName: 'MySQL会员库', targetName: 'HBase', syncType: 'CDC', status: 'RUNNING', currentTps: 180, latency: 120, uptime: '45天 12小时', createTime: '2024-10-15 16:00' }
])

onMounted(() => { loadPipeList() })

const loadPipeList = async () => {
  loading.value = true
  try {
    // In production: const res = await realtimeAPI.getPipeList({ ... })
    const filtered = activeTab.value === 'all'
      ? pipeList.value
      : pipeList.value.filter(p => p.status.toLowerCase() === activeTab.value)
    total.value = filtered.length
  } catch (e) {
    console.error('加载管道列表失败:', e)
  } finally {
    loading.value = false
  }
}

const openTask = (row) => router.push(`/realtime/task/${row.id}`)
const editTask = (row) => router.push(`/realtime/task/${row.id}`)

const startPipe = async (row) => {
  try {
    await ElMessageBox.confirm(`确定启动管道 "${row.name}"？`, '启动确认', { type: 'info' })
    row.status = 'RUNNING'
    row.currentTps = Math.floor(Math.random() * 500) + 100
    ElMessage.success('管道已启动')
  } catch (e) { /* cancel */ }
}

const stopPipe = async (row) => {
  try {
    await ElMessageBox.confirm(`确定停止管道 "${row.name}"？停止后数据同步将中断。`, '停止确认', { type: 'warning' })
    row.status = 'STOPPED'
    row.currentTps = 0
    ElMessage.success('管道已停止')
  } catch (e) { /* cancel */ }
}

const restartPipe = async (row) => {
  try {
    await ElMessageBox.confirm(`确定重启管道 "${row.name}"？`, '重启确认', { type: 'info' })
    ElMessage.success('管道正在重启...')
  } catch (e) { /* cancel */ }
}

const handleCommand = (cmd, row) => {
  switch (cmd) {
    case 'monitor': router.push(`/monitor`); break
    case 'log': router.push(`/log`); break
    case 'copy': ElMessage.success('已复制管道'); break
    case 'alarm': router.push('/alert'); break
    case 'delete':
      ElMessageBox.confirm(`确定删除管道 "${row.name}"？`, '警告', { type: 'warning' }).then(() => {
        ElMessage.success('已删除')
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
.realtime-workbench { padding: 4px; }

.welcome-banner {
  border-radius: 16px;
  padding: 32px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #13c2c2 0%, #08979c 50%, #006d75 100%);

  &.realtime {
    .welcome-content {
      position: relative; z-index: 2;
      .welcome-title { color: #fff; font-size: 26px; font-weight: 700; margin: 0 0 8px; }
      .welcome-desc { color: rgba(255,255,255,0.8); font-size: 14px; margin: 0 0 20px; max-width: 520px; }
      .welcome-actions { display: flex; gap: 12px; }
    }
  }

  .welcome-illustration {
    position: absolute;
    right: 80px; top: 50%;
    transform: translateY(-50%);
    width: 160px; height: 160px;

    .pulse-ring {
      position: absolute;
      border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.3);
      animation: pulse 2s ease-out infinite;

      &.r1 { width: 160px; height: 160px; top: 0; left: 0; animation-delay: 0s; }
      &.r2 { width: 120px; height: 120px; top: 20px; left: 20px; animation-delay: 0.5s; }
      &.r3 { width: 80px; height: 80px; top: 40px; left: 40px; animation-delay: 1.0s; }
    }
  }
}

@keyframes pulse {
  0% { transform: scale(0.5); opacity: 1; }
  100% { transform: scale(1.5); opacity: 0; }
}

.stats-row { margin-bottom: 20px; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid #f0f0f0;
  margin-bottom: 16px;

  &:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }

  .stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
  .stat-icon-wrap {
    width: 40px; height: 40px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    &.blue { background: #e6f4ff; color: #1890ff; }
    &.green { background: #f0f9eb; color: #52c41a; }
    &.orange { background: #fff7e6; color: #fa8c16; }
    &.cyan { background: #e6fffb; color: #13c2c2; }
  }
  .stat-trend {
    font-size: 12px; font-weight: 500; display: flex; align-items: center; gap: 2px;
    &.up { color: #52c41a; }
    &.down { color: #ff4d4f; }
  }
  .stat-info { font-size: 12px; color: #999; }
  .stat-value { font-size: 32px; font-weight: 700; color: #1a1a1a; line-height: 1.2; }
  .stat-label { font-size: 13px; color: #999; margin-top: 4px; }
}

.quick-actions-row { margin-bottom: 20px; }

.quick-action-card {
  background: #fff; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  cursor: pointer; transition: all 0.25s; border: 1px solid #f0f0f0;
  margin-bottom: 16px;

  &:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }

  .qa-icon {
    width: 52px; height: 52px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    &.mysql { background: #e6fffb; color: #13c2c2; }
    &.kafka { background: #f9f0ff; color: #722ed1; }
    &.oracle { background: #fff7e6; color: #fa8c16; }
  }
  .qa-info {
    .qa-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
    .qa-desc { font-size: 13px; color: #999; }
  }
}

.task-section {
  background: #fff; border-radius: 12px; padding: 20px 24px; border: 1px solid #f0f0f0;
}

.section-title-bar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .title-actions { display: flex; align-items: center; gap: 12px; }
}

.tab-switcher {
  display: flex; gap: 0;
  background: #f5f7fa; border-radius: 8px; padding: 3px;
  .tab-item {
    padding: 6px 16px; border-radius: 6px; font-size: 13px; font-weight: 500;
    color: #666; cursor: pointer; transition: all 0.2s;
    &.active { background: #fff; color: #13c2c2; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
    &:hover:not(.active) { color: #333; }
  }
}

.pipe-name-cell {
  display: flex; align-items: center; gap: 8px;
  .status-dot {
    width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
    &.running { background: #52c41a; animation: breathe 2s infinite; }
    &.stopped { background: #dcdfe6; }
    &.error { background: #ff4d4f; animation: blink 1s infinite; }
  }
  .pipe-name { color: #13c2c2; cursor: pointer; font-weight: 500; }
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.source-target {
  display: flex; align-items: center; gap: 8px;
  .arrow-icon { color: #c0c4cc; }
}

.tps-value { font-weight: 600; font-family: 'Fira Code', monospace; }
.latency-high { color: #ff4d4f; font-weight: 600; }
.latency-warn { color: #faad14; font-weight: 500; }
.latency-ok { color: #52c41a; }

.row-actions { display: flex; align-items: center; gap: 4px; }

.table-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid #f5f5f5;
  .footer-info { font-size: 12px; color: #999; }
}

.task-table {
  :deep(.el-table__row) { cursor: pointer; }
}

@media (max-width: 768px) {
  .welcome-banner { padding: 20px; }
  .welcome-illustration { display: none; }
}
</style>
