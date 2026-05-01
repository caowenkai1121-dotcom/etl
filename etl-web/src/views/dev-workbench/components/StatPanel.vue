<template>
  <div class="stat-panel">
    <el-row :gutter="24">
      <el-col :span="6">
        <div class="stat-item success">
          <div class="stat-value">{{ stats.success }}</div>
          <div class="stat-label">成功</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item failed">
          <div class="stat-value">{{ stats.failed }}</div>
          <div class="stat-label">失败</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item interrupted">
          <div class="stat-value">{{ stats.interrupted }}</div>
          <div class="stat-label">中断</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item skipped">
          <div class="stat-value">{{ stats.skipped }}</div>
          <div class="stat-label">跳过</div>
        </div>
      </el-col>
    </el-row>

    <el-divider />

    <div class="stat-extra">
      <div class="stat-row">
        <span class="stat-key">运行时长</span>
        <span class="stat-val">{{ formatDuration(stats.duration) }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-key">开始时间</span>
        <span class="stat-val">{{ stats.startTime || '--' }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-key">结束时间</span>
        <span class="stat-val">{{ stats.endTime || '--' }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-key">处理行数</span>
        <span class="stat-val">{{ formatNumber(stats.totalRows) }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-key">成功率</span>
        <span class="stat-val">{{ formatPercent(stats.success, stats.success + stats.failed) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({
      success: 0,
      failed: 0,
      interrupted: 0,
      skipped: 0,
      duration: 0,
      totalRows: 0,
      startTime: '',
      endTime: ''
    })
  }
})

// 格式化时长
const formatDuration = (ms) => {
  if (!ms) return '0秒'
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (hours > 0) {
    return `${hours}小时${minutes % 60}分${seconds % 60}秒`
  } else if (minutes > 0) {
    return `${minutes}分${seconds % 60}秒`
  } else {
    return `${seconds}秒`
  }
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  return num.toLocaleString('zh-CN')
}

// 格式化百分比
const formatPercent = (part, total) => {
  if (!total || total === 0) return '0%'
  return ((part / total) * 100).toFixed(1) + '%'
}
</script>

<style lang="scss" scoped>
.stat-panel {
  padding: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  background: #f5f7fa;

  .stat-value {
    font-size: 32px;
    font-weight: 600;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 14px;
    color: #666;
    margin-top: 8px;
  }

  &.success {
    background: #f6ffed;
    .stat-value { color: #52c41a; }
  }

  &.failed {
    background: #fff2f0;
    .stat-value { color: #f5222d; }
  }

  &.interrupted {
    background: #fff7e6;
    .stat-value { color: #fa8c16; }
  }

  &.skipped {
    background: #e6f7ff;
    .stat-value { color: #1890ff; }
  }
}

.stat-extra {
  .stat-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px dashed #e8e8e8;

    &:last-child {
      border-bottom: none;
    }

    .stat-key {
      color: #666;
    }

    .stat-val {
      font-weight: 500;
      color: #333;
    }
  }
}
</style>