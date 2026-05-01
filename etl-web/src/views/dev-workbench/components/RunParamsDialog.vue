<template>
  <el-dialog
    v-model="visible"
    title="运行参数配置"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form label-width="100px" size="small">
      <el-form-item label="运行模式">
        <el-radio-group v-model="form.runMode">
          <el-radio value="NORMAL">普通运行</el-radio>
          <el-radio value="DEBUG">调试模式</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-divider>自定义参数</el-divider>

      <div v-for="(param, index) in form.params" :key="index" class="param-row">
        <el-input v-model="param.key" placeholder="参数名" style="width: 140px" />
        <el-input v-model="param.value" placeholder="参数值" style="width: 180px; margin-left: 8px" />
        <el-button type="danger" size="small" circle @click="removeParam(index)" style="margin-left: 8px">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <el-button type="primary" size="small" @click="addParam" style="margin-top: 8px">
        <el-icon><Plus /></el-icon>添加参数
      </el-button>
    </el-form>

    <template #footer>
      <el-button size="small" @click="visible = false">取消</el-button>
      <el-button type="primary" size="small" @click="handleConfirm">确定运行</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  taskId: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:visible', 'confirm'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const form = ref({
  runMode: 'NORMAL',
  params: []
})

const addParam = () => {
  form.value.params.push({ key: '', value: '' })
}

const removeParam = (index) => {
  form.value.params.splice(index, 1)
}

const handleConfirm = () => {
  const params = {}
  form.value.params.forEach(p => {
    if (p.key) params[p.key] = p.value
  })
  emit('confirm', { runMode: form.value.runMode, ...params })
  visible.value = false
}

watch(() => props.visible, (val) => {
  if (val) {
    form.value = { runMode: 'NORMAL', params: [] }
  }
})
</script>

<style lang="scss" scoped>
.param-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
</style>
