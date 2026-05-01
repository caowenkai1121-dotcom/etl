<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="输入字段">
        <el-select v-model="config.sourceFields" multiple placeholder="选择输入字段" style="width: 100%">
          <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name">
            <span>{{ field.name }}</span>
            <span style="color: #999; margin-left: 8px">{{ field.type }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="输出字段">
        <el-table :data="config.selectedFields" stripe size="small">
          <el-table-column prop="name" label="字段名" width="150" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column label="别名" width="150">
            <template #default="{ row }">
              <el-input v-model="row.alias" size="small" placeholder="可选" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ $index }">
              <el-button type="danger" size="small" text @click="removeField($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="small" @click="addSelectedFields">添加选中字段</el-button>
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
  get: () => props.config || { sourceFields: [], selectedFields: [] },
  set: (val) => emit('update:config', val)
})

const addSelectedFields = () => {
  const fieldsToAdd = props.inputFields.filter(f => localConfig.value.sourceFields?.includes(f.name))
  fieldsToAdd.forEach(field => {
    const exists = localConfig.value.selectedFields?.find(f => f.name === field.name)
    if (!exists) {
      if (!localConfig.value.selectedFields) {
        localConfig.value.selectedFields = []
      }
      localConfig.value.selectedFields.push({
        name: field.name,
        type: field.type,
        alias: ''
      })
    }
  })
  emit('update:config', localConfig.value)
}

const removeField = (index) => {
  localConfig.value.selectedFields?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>