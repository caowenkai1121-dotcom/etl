<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="关联类型">
        <el-radio-group v-model="localConfig.joinType">
          <el-radio value="INNER">内关联</el-radio>
          <el-radio value="LEFT">左关联</el-radio>
          <el-radio value="RIGHT">右关联</el-radio>
          <el-radio value="FULL">全关联</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-divider>关联条件</el-divider>

      <el-form-item label="关联键">
        <div class="join-keys">
          <div v-for="(key, index) in localConfig.joinKeys" :key="index" class="join-key-item">
            <el-input v-model="key.mainKey" size="small" placeholder="主表字段" style="width: 150px" />
            <span class="join-symbol">=</span>
            <el-input v-model="key.joinKey" size="small" placeholder="关联表字段" style="width: 150px" />
            <el-button type="danger" size="small" text @click="removeKey(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="small" @click="addKey">添加关联键</el-button>
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
  availableTables: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:config'])

const localConfig = computed({
  get: () => props.config || { joinType: 'INNER', joinKeys: [] },
  set: (val) => emit('update:config', val)
})

const addKey = () => {
  if (!localConfig.value.joinKeys) {
    localConfig.value.joinKeys = []
  }
  localConfig.value.joinKeys.push({
    mainKey: '',
    joinKey: ''
  })
  emit('update:config', localConfig.value)
}

const removeKey = (index) => {
  localConfig.value.joinKeys?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}

.join-keys {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.join-key-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.join-symbol {
  font-weight: bold;
  color: #666;
}
</style>