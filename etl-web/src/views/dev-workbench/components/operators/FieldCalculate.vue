<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="计算规则">
        <el-table :data="localConfig.calculateRules" stripe size="small">
          <el-table-column label="输出字段" width="150">
            <template #default="{ row }">
              <el-input v-model="row.outputField" size="small" placeholder="新字段名" />
            </template>
          </el-table-column>
          <el-table-column label="计算表达式" width="300">
            <template #default="{ row }">
              <el-input v-model="row.expression" size="small" placeholder="如: field1 + field2" />
            </template>
          </el-table-column>
          <el-table-column label="输出类型" width="100">
            <template #default="{ row }">
              <el-select v-model="row.outputType" size="small">
                <el-option label="字符串" value="STRING" />
                <el-option label="整数" value="INT" />
                <el-option label="小数" value="DECIMAL" />
                <el-option label="日期" value="DATE" />
              </el-select>
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
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="small" @click="addRule">添加计算规则</el-button>
      </el-form-item>

      <el-divider>可用字段</el-divider>

      <el-form-item label="可用字段">
        <div class="field-list">
          <el-tag v-for="field in inputFields" :key="field.name" size="small" class="field-tag" @click="insertField(field.name)">
            {{ field.name }}
          </el-tag>
        </div>
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
  get: () => props.config || { calculateRules: [] },
  set: (val) => emit('update:config', val)
})

const addRule = () => {
  if (!localConfig.value.calculateRules) {
    localConfig.value.calculateRules = []
  }
  localConfig.value.calculateRules.push({
    outputField: '',
    expression: '',
    outputType: 'STRING'
  })
  emit('update:config', localConfig.value)
}

const removeRule = (index) => {
  localConfig.value.calculateRules?.splice(index, 1)
  emit('update:config', localConfig.value)
}

const insertField = (fieldName) => {
  if (localConfig.value.calculateRules?.length > 0) {
    const lastRule = localConfig.value.calculateRules[localConfig.value.calculateRules.length - 1]
    lastRule.expression = (lastRule.expression || '') + fieldName
    emit('update:config', localConfig.value)
  }
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}

.field-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.field-tag {
  cursor: pointer;
  &:hover {
    background: #e6f4ff;
  }
}
</style>