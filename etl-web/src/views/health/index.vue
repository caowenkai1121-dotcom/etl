<template>
  <div class="health-page">
    <!-- 系统状态大卡片 -->
    <el-card class="status-card">
      <div class="system-status">
        <div class="status-content">
          <el-icon :size="48" :class="systemStatus === 'UP' ? 'status-success' : 'status-error'">
            {{ systemStatus === 'UP' ? 'Check' : 'Close' }}
          </el-icon>
          <div class="status-text">
            <h1>{{ systemStatus === 'UP' ? '系统正常' : '系统异常' }}</h1>
            <p>最后检查: {{ lastCheckTime }}</p>
          </div>
        </div>
        <div class="refresh-control">
          <el-button type="primary" @click="refreshData" :loading="refreshing">
            <el-icon><Refresh /></el-icon>
            手动刷新
          </el-button>
          <span class="auto-refresh">
            <el-switch v-model="autoRefresh" size="small" active-text="自动刷新" @change="toggleAutoRefresh" />
          </span>
        </div>
      </div>
    </el-card>

    <!-- 2x2 状态卡片 -->
    <el-row :gutter="20" class="health-cards">
      <!-- 数据库连接 -->
      <el-col :xs="24" :lg="12">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Coin /></el-icon>
                数据库连接
              </span>
            </div>
          </template>
          <div class="status-grid">
            <div class="status-item">
              <span class="item-label">MySQL</span>
              <el-tag :type="dbStatus.mysql === 'UP' ? 'success' : 'danger'">{{ dbStatus.mysql === 'UP' ? 'UP' : 'DOWN' }}</el-tag>
            </div>
            <div class="status-item">
              <span class="item-label">PostgreSQL</span>
              <el-tag :type="dbStatus.postgres === 'UP' ? 'success' : 'danger'">{{ dbStatus.postgres === 'UP' ? 'UP' : 'DOWN' }}</el-tag>
            </div>
            <div class="status-item">
              <span class="item-label">Redis</span>
              <el-tag :type="dbStatus.redis === 'UP' ? 'success' : 'danger'">{{ dbStatus.redis === 'UP' ? 'UP' : 'DOWN' }}</el-tag>
            </div>
            <div class="status-item">
              <span class="item-label">连接时间</span>
              <span class="item-value">{{ dbStatus.connectTime }}ms</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 连接池 -->
      <el-col :xs="24" :lg="12">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Connection /></el-icon>
                连接池
              </span>
            </div>
          </template>
          <div class="pool-info">
            <div class="progress-item">
              <div class="progress-label">
                <span>活跃连接</span>
                <span>{{ poolStats.active }}/{{ poolStats.max }}</span>
              </div>
              <el-progress :percentage="poolStats.usage" :stroke-width="8" :color="getPoolColor(poolStats.usage)" />
            </div>
            <div class="progress-item">
              <div class="progress-label">
                <span>空闲连接</span>
                <span>{{ poolStats.idle }}</span>
              </div>
              <el-progress :percentage="poolStats.idlePercent" :stroke-width="8" color="#e4e7ed" />
            </div>
            <div class="stat-item">
              <span class="stat-label">等待线程</span>
              <span class="stat-value" :class="poolStats.waiting > 5 ? 'text-danger' : ''">
                {{ poolStats.waiting }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- JVM -->
      <el-col :xs="24" :lg="12">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Cpu /></el-icon>
                JVM
              </span>
            </div>
          </template>
          <div class="jvm-info">
            <div class="progress-item">
              <div class="progress-label">
                <span>堆内存</span>
                <span>{{ formatBytes(jvmStats.used) }}/{{ formatBytes(jvmStats.max) }}</span>
              </div>
              <el-progress :percentage="jvmStats.usage" :stroke-width="8" :color="getJvmColor(jvmStats.usage)" />
            </div>
            <div class="stat-item">
              <span class="stat-label">线程数</span>
              <span class="stat-value">{{ jvmStats.threads }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">GC次数</span>
              <span class="stat-value">{{ jvmStats.gcCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">GC时间</span>
              <span class="stat-value">{{ jvmStats.gcTime }}ms</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 磁盘空间 -->
      <el-col :xs="24" :lg="12">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><FolderOpened /></el-icon>
                磁盘空间
              </span>
            </div>
          </template>
          <div class="disk-info">
            <div class="progress-item">
              <div class="progress-label">
                <span>使用量</span>
                <span>{{ formatBytes(diskStats.used) }}/{{ formatBytes(diskStats.total) }}</span>
              </div>
              <el-progress :percentage="diskStats.usage" :stroke-width="8" :color="getDiskColor(diskStats.usage)" />
            </div>
            <div class="stat-item">
              <span class="stat-label">可用空间</span>
              <span class="stat-value" :class="diskStats.freePercent < 10 ? 'text-danger' : 'text-success'">
                {{ formatBytes(diskStats.free) }}
              </span>
            </div>
            <div class="stat-item">
              <span class="stat-label">剩余空间</span>
              <span class="stat-value">{{ diskStats.freePercent }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getHealthDetail, getPoolStatus, getThreadPoolStatus, getSystemInfo } from '@/api'

const systemStatus = ref('UP')
const lastCheckTime = ref('')
const refreshing = ref(false)
const autoRefresh = ref(true)
let refreshTimer = null

// 数据库状态
const dbStatus = reactive({
  mysql: 'UP',
  postgres: 'UP',
  redis: 'UP',
  connectTime: 32
})

// 连接池状态
const poolStats = reactive({
  active: 12,
  idle: 8,
  max: 20,
  waiting: 0,
  usage: 60,
  idlePercent: 40
})

// JVM状态
const jvmStats = reactive({
  used: 2147483648,
  max: 4294967296,
  usage: 50,
  threads: 156,
  gcCount: 42,
  gcTime: 12450
})

// 磁盘空间
const diskStats = reactive({
  used: 150000000000,
  free: 50000000000,
  total: 200000000000,
  usage: 75,
  freePercent: 25
})

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

const getPoolColor = (usage) => {
  if (usage > 80) return '#ff4d4f'
  if (usage > 60) return '#faad14'
  return '#52c41a'
}

const getJvmColor = (usage) => {
  if (usage > 85) return '#ff4d4f'
  if (usage > 70) return '#faad14'
  return '#52c41a'
}

const getDiskColor = (usage) => {
  if (usage > 90) return '#ff4d4f'
  if (usage > 75) return '#faad14'
  return '#52c41a'
}

const updateTime = () => {
  lastCheckTime.value = new Date().toLocaleString('zh-CN')
}

const refreshData = async () => {
  refreshing.value = true
  try {
    const [healthRes, poolRes, sysRes] = await Promise.all([
      getHealthDetail().catch(() => ({})),
      getPoolStatus().catch(() => ({})),
      getSystemInfo().catch(() => ({}))
    ])

    const healthData = healthRes.data || healthRes || {}
    const poolData = poolRes.data || poolRes || {}
    const sysData = sysRes.data || sysRes || {}

    systemStatus.value = healthData.status || 'UP'
    dbStatus.mysql = healthData.mysql || healthData.mysqlStatus || dbStatus.mysql
    dbStatus.redis = healthData.redis || healthData.redisStatus || dbStatus.redis

    if (poolData.active !== undefined) poolStats.active = poolData.active
    if (poolData.idle !== undefined) poolStats.idle = poolData.idle
    if (poolData.max !== undefined) poolStats.max = poolData.max
    if (poolData.waiting !== undefined) poolStats.waiting = poolData.waiting
    poolStats.usage = Math.floor((poolStats.active / Math.max(poolStats.max, 1)) * 100)
    poolStats.idlePercent = 100 - poolStats.usage

    if (sysData.heapUsed !== undefined) jvmStats.used = sysData.heapUsed
    if (sysData.heapMax !== undefined) jvmStats.max = sysData.heapMax
    jvmStats.usage = Math.floor((jvmStats.used / Math.max(jvmStats.max, 1)) * 100)
    if (sysData.threads !== undefined) jvmStats.threads = sysData.threads
    if (sysData.gcCount !== undefined) jvmStats.gcCount = sysData.gcCount

    if (sysData.diskUsed !== undefined) diskStats.used = sysData.diskUsed
    if (sysData.diskTotal !== undefined) diskStats.total = sysData.diskTotal
    diskStats.free = diskStats.total - diskStats.used
    diskStats.usage = Math.floor((diskStats.used / Math.max(diskStats.total, 1)) * 100)
    diskStats.freePercent = 100 - diskStats.usage

    updateTime()
    ElMessage.success('数据已刷新')
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

const toggleAutoRefresh = (value) => {
  if (value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  if (!refreshTimer) {
    refreshTimer = setInterval(() => {
      refreshData()
    }, 30000)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  updateTime()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style lang="scss" scoped>
.health-page {
  background-color: #f5f7fa;
  min-height: 100%;
  padding: 20px;

  .status-card {
    border-radius: 16px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    margin-bottom: 20px;

    :deep(.el-card__body) {
      padding: 0;
    }

    .system-status {
      padding: 32px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      @media (max-width: 768px) {
        flex-direction: column;
        gap: 20px;
        text-align: center;
      }

      .status-content {
        display: flex;
        align-items: center;
        gap: 20px;

        .status-success {
          color: #52c41a;
        }

        .status-error {
          color: #ff4d4f;
        }

        .status-text {
          h1 {
            margin: 0;
            font-size: 36px;
            font-weight: 700;
            color: #303133;
          }

          p {
            margin: 8px 0 0 0;
            color: #909399;
          }
        }
      }

      .refresh-control {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .auto-refresh {
          display: flex;
          align-items: center;
          justify-content: center;
          color: #606266;
          font-size: 14px;
        }
      }
    }
  }

  .health-cards {
    .info-card {
      border-radius: 12px;
      border: 1px solid #ebeef5;
      height: 100%;

      :deep(.el-card__header) {
        padding: 14px 16px;
        border-bottom: 1px solid #f0f2f5;
      }

      :deep(.el-card__body) {
        padding: 20px;
      }

      .card-header {
        .card-title {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }

      .status-grid {
        .status-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 12px 0;
          border-bottom: 1px solid #f5f7fa;

          &:last-child {
            border-bottom: none;
          }

          .item-label {
            color: #606266;
            font-weight: 500;
          }
        }
      }

      .progress-item {
        margin-bottom: 16px;

        .progress-label {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          color: #606266;
          font-size: 14px;

          span:first-child {
            color: #606266;
          }

          span:last-child {
            font-weight: 600;
            color: #303133;
          }
        }

        &:last-child {
          margin-bottom: 0;
        }
      }

      .stat-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 0;
        border-top: 1px solid #f5f7fa;

        .stat-label {
          color: #606266;
        }

        .stat-value {
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }
}

.text-danger {
  color: #ff4d4f;
}

.text-success {
  color: #52c41a;
}
</style>
