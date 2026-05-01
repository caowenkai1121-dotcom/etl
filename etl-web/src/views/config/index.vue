<template>
  <div class="config-page">
    <!-- 数据库配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">数据库配置</span>
        </div>
      </template>
      <el-form :model="dbConfig" label-width="120px" class="config-form">
        <el-form-item label="数据库类型">
          <el-tag size="small">MySQL</el-tag>
        </el-form-item>
        <el-form-item label="主机地址">
          <el-input v-model="dbConfig.host" disabled />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="dbConfig.port" disabled />
        </el-form-item>
        <el-form-item label="数据库名">
          <el-input v-model="dbConfig.database" disabled />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="dbConfig.username" disabled />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="dbConfig.password" type="password" disabled placeholder="••••••••" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="testDbConnection">
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 缓存配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">缓存配置</span>
        </div>
      </template>
      <el-form :model="cacheConfig" label-width="120px" class="config-form">
        <el-form-item label="Redis主机">
          <el-input v-model="cacheConfig.host" disabled />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="cacheConfig.port" disabled />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="cacheConfig.password" type="password" disabled placeholder="••••••••" />
        </el-form-item>
        <el-form-item label="数据库">
          <el-input v-model="cacheConfig.database" disabled />
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input v-model="cacheConfig.timeout" disabled />
          <span class="suffix">ms</span>
        </el-form-item>
        <el-form-item label="命中率">
          <el-progress :percentage="cacheStats.hitRate" :status="getRateStatus(cacheStats.hitRate)" :stroke-width="8" />
          <span class="stat-value">{{ cacheStats.hitRate }}%</span>
        </el-form-item>
        <el-form-item label="键数量">
          <el-input-number :model-value="cacheStats.keyCount" disabled style="width: 100%" />
        </el-form-item>
        <el-form-item label="内存使用">
          <el-progress :percentage="cacheStats.memoryUsage" :status="getRateStatus(cacheStats.memoryUsage)" :stroke-width="8" />
          <span class="stat-value">{{ formatBytes(cacheStats.memoryUsed) }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="clearCache">
            <el-icon><Refresh /></el-icon>
            清理缓存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 调度配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">调度配置</span>
          <el-button type="primary" @click="saveScheduleConfig">保存配置</el-button>
        </div>
      </template>
      <el-form :model="scheduleConfig" label-width="140px" class="config-form">
        <el-form-item label="调度线程池大小">
          <el-input-number v-model="scheduleConfig.corePoolSize" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="Cron线程数">
          <el-input-number v-model="scheduleConfig.cronThreadPoolSize" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="任务超时时间">
          <el-input-number v-model="scheduleConfig.taskTimeout" :min="60" :max="3600" />
          <span class="suffix">秒</span>
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="scheduleConfig.retryCount" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="重试间隔">
          <el-input-number v-model="scheduleConfig.retryInterval" :min="0" :max="300" />
          <span class="suffix">秒</span>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 系统维护 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统维护</span>
        </div>
      </template>
      <el-form label-width="120px" class="config-form">
        <el-form-item label="日志归档">
          <el-input-number v-model="maintenanceConfig.logArchiveDays" :min="1" :max="365" />
          <span class="suffix">天</span>
          <el-button type="danger" @click="archiveLogs" class="action-btn">
            <el-icon><DocumentDelete /></el-icon>
            归档
          </el-button>
        </el-form-item>
        <el-form-item label="数据库备份">
          <el-button type="warning" @click="backupDb">
            <el-icon><Download /></el-icon>
            备份
          </el-button>
        </el-form-item>
        <el-form-item label="系统信息">
          <div class="system-info">
            <p>版本: {{ systemInfo.version }}</p>
            <p>运行时间: {{ formatDuration(systemInfo.uptime) }}</p>
            <p>JVM版本: {{ systemInfo.jvmVersion }}</p>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 数据库配置
const dbConfig = reactive({
  host: '192.168.1.100',
  port: '3306',
  database: 'etl_db',
  username: 'etl_user',
  password: '••••••••'
})

// 缓存配置
const cacheConfig = reactive({
  host: '192.168.1.100',
  port: '6379',
  password: '••••••••',
  database: '0',
  timeout: '10000'
})

const cacheStats = reactive({
  hitRate: 85,
  keyCount: 12450,
  memoryUsage: 38,
  memoryUsed: 256000000
})

// 调度配置
const scheduleConfig = reactive({
  corePoolSize: 10,
  cronThreadPoolSize: 5,
  taskTimeout: 300,
  retryCount: 3,
  retryInterval: 60
})

// 系统维护
const maintenanceConfig = reactive({
  logArchiveDays: 30
})

const systemInfo = reactive({
  version: '1.2.3',
  uptime: 158000,
  jvmVersion: '17'
})

const testDbConnection = async () => {
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('连接成功')
  } catch (e) {
    ElMessage.error('连接失败')
  }
}

const clearCache = async () => {
  try {
    await ElMessageBox.confirm('确定要清理缓存吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('缓存已清理')
  } catch (e) {
    // 忽略取消操作
  }
}

const saveScheduleConfig = () => {
  try {
    localStorage.setItem('etl-schedule-config', JSON.stringify(scheduleConfig))
    localStorage.setItem('etl-maintenance-config', JSON.stringify(maintenanceConfig))
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

function loadSavedConfig() {
  try {
    const savedSchedule = localStorage.getItem('etl-schedule-config')
    if (savedSchedule) Object.assign(scheduleConfig, JSON.parse(savedSchedule))
    const savedMaintenance = localStorage.getItem('etl-maintenance-config')
    if (savedMaintenance) Object.assign(maintenanceConfig, JSON.parse(savedMaintenance))
  } catch (e) { /* ignore corrupt localStorage */ }
}
loadSavedConfig()

const archiveLogs = async () => {
  try {
    await ElMessageBox.confirm(`确定要归档 ${maintenanceConfig.logArchiveDays} 天前的日志吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await new Promise(resolve => setTimeout(resolve, 800))
    ElMessage.success('日志归档成功')
  } catch (e) {
    // 忽略取消操作
  }
}

const backupDb = async () => {
  try {
    await ElMessageBox.confirm('确定要备份数据库吗？这可能需要几分钟时间。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await new Promise(resolve => setTimeout(resolve, 1500))
    ElMessage.success('数据库备份成功')
  } catch (e) {
    // 忽略取消操作
  }
}

const getRateStatus = (percentage) => {
  if (percentage >= 80) return 'success'
  if (percentage >= 50) return 'warning'
  return 'exception'
}

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

const formatDuration = (seconds) => {
  const days = Math.floor(seconds / (24 * 3600))
  const hours = Math.floor((seconds % (24 * 3600)) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${minutes}分钟`
  if (minutes > 0) return `${minutes}分钟${secs}秒`
  return `${secs}秒`
}
</script>

<style lang="scss" scoped>
.config-page {
  background-color: #f5f7fa;
  min-height: 100%;
  padding: 20px;

  .config-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    margin-bottom: 20px;

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f0f2f5;
    }

    :deep(.el-card__body) {
      padding: 24px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .config-form {
      :deep(.el-form-item) {
        margin-bottom: 20px;
      }

      :deep(.el-form-item__label) {
        color: #606266;
        font-weight: 500;
      }

      :deep(.el-input.is-disabled .el-input__wrapper) {
        background-color: #f5f7fa;
        color: #909399;
      }

      .stat-value {
        display: inline-block;
        margin-left: 12px;
        font-weight: 600;
        color: #303133;
      }

      .suffix {
        margin-left: 8px;
        color: #909399;
      }

      .action-btn {
        margin-left: 12px;
      }

      .system-info {
        p {
          margin: 8px 0;
          color: #606266;
        }
      }
    }
  }
}
</style>
