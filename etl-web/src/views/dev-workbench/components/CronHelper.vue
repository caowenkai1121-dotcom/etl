<template>
  <div class="cron-helper">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="分钟" name="minute">
        <div class="preset-list">
          <div class="preset-item" @click="select('0 * * * * ?')">每小时的第0分钟</div>
          <div class="preset-item" @click="select('0/5 * * * * ?')">每5分钟</div>
          <div class="preset-item" @click="select('0/10 * * * * ?')">每10分钟</div>
          <div class="preset-item" @click="select('0/30 * * * * ?')">每30分钟</div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="小时" name="hour">
        <div class="preset-list">
          <div class="preset-item" @click="select('0 0 * * * ?')">每小时</div>
          <div class="preset-item" @click="select('0 0/2 * * * ?')">每2小时</div>
          <div class="preset-item" @click="select('0 0/6 * * * ?')">每6小时</div>
          <div class="preset-item" @click="select('0 0/12 * * * ?')">每12小时</div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="天" name="day">
        <div class="preset-list">
          <div class="preset-item" @click="select('0 0 0 * * ?')">每天0点</div>
          <div class="preset-item" @click="select('0 30 8 * * ?')">每天8:30</div>
          <div class="preset-item" @click="select('0 0 12 * * ?')">每天12:00</div>
          <div class="preset-item" @click="select('0 0 20 * * ?')">每天20:00</div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="周" name="week">
        <div class="preset-list">
          <div class="preset-item" @click="select('0 0 0 ? * MON')">每周一0点</div>
          <div class="preset-item" @click="select('0 0 0 ? * FRI')">每周五0点</div>
          <div class="preset-item" @click="select('0 30 8 ? * MON')">每周一8:30</div>
          <div class="preset-item" @click="select('0 0 0 ? * SUN')">每周日0点</div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="月" name="month">
        <div class="preset-list">
          <div class="preset-item" @click="select('0 0 0 1 * ?')">每月1日0点</div>
          <div class="preset-item" @click="select('0 0 0 15 * ?')">每月15日0点</div>
          <div class="preset-item" @click="select('0 30 8 1 * ?')">每月1日8:30</div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <div class="custom-section">
      <el-input v-model="customCron" placeholder="自定义Cron表达式" size="small">
        <template #append>
          <el-button size="small" @click="select(customCron)">使用</el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['select'])

const activeTab = ref('day')
const customCron = ref('')

const select = (cron) => {
  emit('select', cron)
}
</script>

<style lang="scss" scoped>
.cron-helper {
  .preset-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .preset-item {
    padding: 10px 12px;
    background: #f5f7fa;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    color: #333;
    transition: all 0.2s;

    &:hover {
      background: #e6f4ff;
      color: #1890ff;
    }
  }

  .custom-section {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #e8e8e8;
  }
}
</style>
