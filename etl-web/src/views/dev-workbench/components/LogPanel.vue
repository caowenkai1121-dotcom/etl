<template>
  <div class="log-panel">
    <div class="log-content" ref="logContainerRef">
      <div
        v-for="(log, index) in logs"
        :key="index"
        class="log-item"
        :class="`log-${(log.level || 'info').toLowerCase()}`"
      >
        <span class="log-time">{{ formatTime(log.time || log.timestamp || log.startTime) }}</span>
        <el-tag :type="getLevelType(log.level || log.status)" size="small" class="log-level">
          {{ log.level || log.status || 'INFO' }}
        </el-tag>
        <span class="log-node" v-if="log.nodeName">[{{ log.nodeName }}]</span>
        <span class="log-message">{{ log.message || log.logContent }}</span>
      </div>
      <div v-if="logs.length === 0" class="log-empty">
        暂无运行日志
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  logs: {
    type: Array,
    default: () => []
  }
})

const logContainerRef = ref(null)

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  try {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    return String(timestamp)
  }
}

// 获取日志级别类型
const getLevelType = (level) => {
  const types = {
    'DEBUG': 'info',
    'INFO': '',
    'WARN': 'warning',
    'ERROR': 'danger',
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'RUNNING': 'primary',
    'SKIPPED': 'info'
  }
  return types[level?.toUpperCase()] || ''
}

// 自动滚动到底部
watch(() => props.logs, () => {
  nextTick(() => {
    if (logContainerRef.value) {
      logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
    }
  })
}, { deep: true })
</script>

<style lang="scss" scoped>
.log-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #1e1e1e;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 4px 0;
  color: #d4d4d4;
  line-height: 1.5;

  &.log-debug {
    color: #888;
  }

  &.log-info {
    color: #d4d4d4;
  }

  &.log-warn {
    color: #e6a23c;
  }

  &.log-error, &.log-failed {
    color: #f56c6c;
  }

  &.log-success {
    color: #67c23a;
  }
}

.log-time {
  color: #888;
  flex-shrink: 0;
  min-width: 64px;
}

.log-level {
  flex-shrink: 0;
}

.log-node {
  color: #4fc3f7;
  flex-shrink: 0;
}

.log-message {
  word-break: break-all;
}

.log-empty {
  color: #666;
  text-align: center;
  padding: 24px;
}
</style>
