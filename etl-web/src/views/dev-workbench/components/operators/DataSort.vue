<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="排序规则">
        <el-table :data="localConfig.sortRules" stripe size="small">
          <el-table-column label="排序字段" width="150">
            <template #default="{ row }">
              <el-select v-model="row.field" size="small" placeholder="选择字段">
                <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="排序方式" width="100">
            <template #default="{ row }">
              <el-select v-model="row.direction" size="small">
                <el-option label="升序" value="ASC" />
                <el-option label="降序" value="DESC" />
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
        <el-button type="primary" size="small" @click="addRule">添加排序规则</el-button>
      </el-form-item>

      <el-divider />

      <el-form-item label="限制条数">
        <el-input-number v-model="localConfig.limit" :min="0" size="small" />
        <span style="margin-left: 8px">0 表示不限制</span>
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
  get: () => props.config || { sortRules: [], limit: 0 },
  set: (val) => emit('update:config', val)
})

const addRule = () => {
  if (!localConfig.value.sortRules) {
    localConfig.value.sortRules = []
  }
  localConfig.value.sortRules.push({
    field: '',
    direction: 'ASC'
  })
  emit('update:config', localConfig.value)
}

const removeRule = (index) => {
  localConfig.value.sortRules?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>