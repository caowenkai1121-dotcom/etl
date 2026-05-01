<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="重命名规则">
        <el-table :data="localConfig.renameRules" stripe size="small">
          <el-table-column prop="originalName" label="原字段名" width="150">
            <template #default="{ row }">
              <el-select v-model="row.originalName" size="small" placeholder="选择字段">
                <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="newName" label="新字段名" width="150">
            <template #default="{ row }">
              <el-input v-model="row.newName" size="small" placeholder="输入新名称" />
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
        <el-button type="primary" size="small" @click="addRule">添加重命名规则</el-button>
      </el-form-item>

      <el-divider />

      <el-form-item label="批量规则">
        <el-checkbox v-model="localConfig.autoRename">自动添加序号</el-checkbox>
        <el-input-number v-if="localConfig.autoRename" v-model="localConfig.startIndex" :min="1" size="small" />
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
  get: () => props.config || { renameRules: [], autoRename: false, startIndex: 1 },
  set: (val) => emit('update:config', val)
})

const addRule = () => {
  if (!localConfig.value.renameRules) {
    localConfig.value.renameRules = []
  }
  localConfig.value.renameRules.push({
    originalName: '',
    newName: ''
  })
  emit('update:config', localConfig.value)
}

const removeRule = (index) => {
  localConfig.value.renameRules?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>