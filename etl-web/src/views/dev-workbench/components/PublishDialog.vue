<template>
  <el-dialog
    v-model="visible"
    title="任务发布"
    width="600px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="发布类型">
        <el-radio-group v-model="form.publishType">
          <el-radio value="FULL">全量发布</el-radio>
          <el-radio value="UPDATE">更新发布</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="变更说明" prop="changeLog">
        <el-input
          type="textarea"
          v-model="form.changeLog"
          :rows="4"
          placeholder="请输入本次发布的变更说明"
        />
      </el-form-item>

      <el-divider>发布范围</el-divider>

      <el-form-item label="生效环境">
        <el-checkbox-group v-model="form.environments">
          <el-checkbox label="DEV">开发环境</el-checkbox>
          <el-checkbox label="TEST">测试环境</el-checkbox>
          <el-checkbox label="PROD">生产环境</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-divider>版本信息</el-divider>

      <el-form-item label="当前版本">
        <el-tag>{{ currentVersion }}</el-tag>
      </el-form-item>

      <el-form-item label="发布版本">
        <el-tag type="success">{{ nextVersion }}</el-tag>
      </el-form-item>

      <el-divider>变更预览</el-divider>

      <div class="change-preview">
        <div class="preview-item">
          <el-icon color="#52c41a"><SuccessFilled /></el-icon>
          <span>新增节点: {{ changeSummary.addedNodes || 0 }} 个</span>
        </div>
        <div class="preview-item">
          <el-icon color="#1890ff"><Edit /></el-icon>
          <span>修改节点: {{ changeSummary.modifiedNodes || 0 }} 个</span>
        </div>
        <div class="preview-item">
          <el-icon color="#f5222d"><Delete /></el-icon>
          <span>删除节点: {{ changeSummary.deletedNodes || 0 }} 个</span>
        </div>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      >
        <template #title>
          发布后将在选中的环境中生效，请确认变更内容无误。
        </template>
      </el-alert>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="publishing" @click="handlePublish">
        确认发布
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { devAPI } from '@/api/dev'

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
const publishing = ref(false)

const form = ref({
  publishType: 'FULL',
  changeLog: '',
  environments: ['PROD']
})

const rules = {
  changeLog: [
    { required: true, message: '请输入变更说明', trigger: 'blur' }
  ]
}

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const currentVersion = computed(() => 'v1.0')
const nextVersion = computed(() => 'v1.1')

const changeSummary = ref({
  addedNodes: 0,
  modifiedNodes: 0,
  deletedNodes: 0
})

// 监听taskId变化
watch(() => props.taskId, async (id) => {
  if (id) {
    // TODO: 加载任务版本信息和变更摘要
  }
})

// 发布
const handlePublish = async () => {
  try {
    await formRef.value.validate()
    publishing.value = true

    await devAPI.publishTask(props.taskId, form.value)
    ElMessage.success('发布成功')
    emit('confirm', form.value)
    visible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}
</script>

<style lang="scss" scoped>
.change-preview {
  display: flex;
  gap: 24px;

  .preview-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
  }
}
</style>