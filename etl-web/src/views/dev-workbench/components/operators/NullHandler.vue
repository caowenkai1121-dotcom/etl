<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="空值检测">
        <el-checkbox-group v-model="localConfig.detectTypes">
          <el-checkbox label="NULL">NULL值</el-checkbox>
          <el-checkbox label="EMPTY">空字符串</el-checkbox>
          <el-checkbox label="BLANK">空白字符串</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-divider>处理规则</el-divider>

      <el-form-item label="处理方式">
        <el-radio-group v-model="localConfig.handleType">
          <el-radio value="REPLACE">替换为指定值</el-radio>
          <el-radio value="DELETE">删除空值行</el-radio>
          <el-radio value="KEEP">保留空值</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="替换值" v-if="localConfig.handleType === 'REPLACE'">
        <el-table :data="localConfig.replaceRules" stripe size="small">
          <el-table-column label="字段" width="150">
            <template #default="{ row }">
              <el-select v-model="row.field" size="small" placeholder="选择字段">
                <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="替换值" width="150">
            <template #default="{ row }">
              <el-input v-model="row.replaceValue" size="small" placeholder="替换值" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ $index }">
              <el-button type="danger" size="small" text @click="removeRule($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" size="small" @click="addRule" style="margin-top: 8px">添加替换规则</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  config: {
    type: Object,
    default: () => ({})
  },
  inputFields: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:config'])

const localConfig = computed({
  get: () => props.config || { detectTypes: ['NULL', 'EMPTY'], handleType: 'REPLACE', replaceRules: [] },
  set: (val) => emit('update:config', val)
})

const addRule = () => {
  if (!localConfig.value.replaceRules) {
    localConfig.value.replaceRules = []
  }
  localConfig.value.replaceRules.push({
    field: '',
    replaceValue: ''
  })
  emit('update:config', localConfig.value)
}

const removeRule = (index) => {
  localConfig.value.replaceRules?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>