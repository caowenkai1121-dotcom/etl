<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="分组字段">
        <el-select v-model="localConfig.groupFields" multiple placeholder="选择分组字段" style="width: 100%">
          <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name" />
        </el-select>
      </el-form-item>

      <el-divider>聚合计算</el-divider>

      <el-form-item label="聚合函数">
        <el-table :data="localConfig.aggregateRules" stripe size="small">
          <el-table-column label="聚合字段" width="150">
            <template #default="{ row }">
              <el-select v-model="row.field" size="small" placeholder="选择字段">
                <el-option v-for="field in numericFields" :key="field.name" :label="field.name" :value="field.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="聚合类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.function" size="small">
                <el-option label="求和(SUM)" value="SUM" />
                <el-option label="平均值(AVG)" value="AVG" />
                <el-option label="最大值(MAX)" value="MAX" />
                <el-option label="最小值(MIN)" value="MIN" />
                <el-option label="计数(COUNT)" value="COUNT" />
                <el-option label="去重计数" value="COUNT_DISTINCT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="输出别名" width="150">
            <template #default="{ row }">
              <el-input v-model="row.alias" size="small" placeholder="输出字段名" />
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
        <el-button type="primary" size="small" @click="addRule">添加聚合规则</el-button>
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
  get: () => props.config || { groupFields: [], aggregateRules: [] },
  set: (val) => emit('update:config', val)
})

const numericFields = computed(() => {
  return props.inputFields.filter(f => ['INT', 'BIGINT', 'DECIMAL', 'DOUBLE', 'FLOAT'].includes(f.type?.toUpperCase()))
})

const addRule = () => {
  if (!localConfig.value.aggregateRules) {
    localConfig.value.aggregateRules = []
  }
  localConfig.value.aggregateRules.push({
    field: '',
    function: 'SUM',
    alias: ''
  })
  emit('update:config', localConfig.value)
}

const removeRule = (index) => {
  localConfig.value.aggregateRules?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>