<template>
  <el-dialog
    v-model="visible"
    title="调度配置"
    width="600px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
      <el-form-item label="调度类型" prop="scheduleType">
        <el-radio-group v-model="form.scheduleType">
          <el-radio value="CRON">周期调度</el-radio>
          <el-radio value="MANUAL">手动触发</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 周期调度配置 -->
      <template v-if="form.scheduleType === 'CRON'">
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="请输入Cron表达式">
            <template #append>
              <el-button @click="showCronHelper = true">生成器</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="下次执行">
          <span class="next-run-time">{{ nextRunTime }}</span>
        </el-form-item>
      </template>

      <el-divider />

      <el-form-item label="生效时间">
        <el-date-picker
          v-model="form.effectiveTime"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 100%"
          size="small"
        />
      </el-form-item>

      <el-form-item label="失败重试">
        <el-switch v-model="form.retryEnabled" />
      </el-form-item>
      <template v-if="form.retryEnabled">
        <el-form-item label="重试次数">
          <el-input-number v-model="form.retryTimes" :min="1" :max="10" size="small" />
        </el-form-item>
        <el-form-item label="重试间隔">
          <el-input-number v-model="form.retryInterval" :min="1" :max="300" size="small" />
          <span class="unit">秒</span>
        </el-form-item>
      </template>

      <el-form-item label="超时时间">
        <el-input-number v-model="form.timeout" :min="60" :max="86400" size="small" />
        <span class="unit">秒</span>
      </el-form-item>

      <el-form-item label="告警通知">
        <el-checkbox-group v-model="form.alertChannels">
          <el-checkbox label="EMAIL">邮件</el-checkbox>
          <el-checkbox label="DINGTALK">钉钉</el-checkbox>
          <el-checkbox label="WECHAT">企业微信</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button size="small" @click="visible = false">取消</el-button>
      <el-button type="primary" size="small" @click="handleConfirm">确定</el-button>
    </template>

    <!-- Cron表达式生成器 -->
    <el-dialog
      v-model="showCronHelper"
      title="Cron表达式生成器"
      width="500px"
      append-to-body
    >
      <CronHelper @select="handleCronSelect" />
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { devAPI } from '@/api/dev'
import CronHelper from './CronHelper.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  taskId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:visible', 'confirm'])

const formRef = ref(null)
const showCronHelper = ref(false)
const loading = ref(false)

const form = ref({
  scheduleType: 'MANUAL',
  cronExpression: '',
  dependencyTasks: [],
  triggerConditions: ['SUCCESS'],
  effectiveTime: [],
  retryEnabled: false,
  retryTimes: 3,
  retryInterval: 60,
  timeout: 3600,
  alertChannels: ['EMAIL']
})

const rules = {
  cronExpression: [
    { required: true, message: '请输入Cron表达式', trigger: 'blur' }
  ]
}

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 下次执行时间
const nextRunTime = computed(() => {
  if (!form.value.cronExpression || form.value.scheduleType !== 'CRON') return '--'
  // 简化展示，实际应由后端计算
  return '待计算'
})

// 监听taskId变化，加载配置
watch(() => props.taskId, async (id) => {
  if (id && props.visible) {
    await loadScheduleConfig(id)
  }
})

watch(() => props.visible, async (val) => {
  if (val && props.taskId) {
    await loadScheduleConfig(props.taskId)
  }
})

// 加载调度配置
const loadScheduleConfig = async (taskId) => {
  try {
    loading.value = true
    const res = await devAPI.getSchedule(taskId)
    const info = res.data
    if (info && info.scheduled && info.cronExpression) {
      form.value.scheduleType = 'CRON'
      form.value.cronExpression = info.cronExpression
    } else {
      form.value.scheduleType = 'MANUAL'
      form.value.cronExpression = ''
    }
  } catch (e) {
    console.error('加载调度配置失败', e)
  } finally {
    loading.value = false
  }
}

// 选择Cron表达式
const handleCronSelect = (cron) => {
  form.value.cronExpression = cron
  showCronHelper.value = false
}

// 确认
const handleConfirm = async () => {
  try {
    if (form.value.scheduleType === 'CRON' && !form.value.cronExpression) {
      ElMessage.warning('请输入Cron表达式')
      return
    }
    await formRef.value?.validate()
    emit('confirm', form.value)
    visible.value = false
  } catch (e) {
    // 表单校验失败
  }
}
</script>

<style lang="scss" scoped>
.next-run-time {
  color: #52c41a;
  font-weight: 500;
}

.unit {
  margin-left: 8px;
  color: #666;
  font-size: 12px;
}
</style>
