<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="去重字段">
        <el-select v-model="localConfig.deduplicateFields" multiple placeholder="选择去重字段" style="width: 100%">
          <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name" />
        </el-select>
      </el-form-item>

      <el-divider />

      <el-form-item label="去重策略">
        <el-radio-group v-model="localConfig.strategy">
          <el-radio value="FIRST">保留第一条</el-radio>
          <el-radio value="LAST">保留最后一条</el-radio>
          <el-radio value="RANDOM">随机保留</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-divider />

      <el-form-item label="高级选项">
        <el-checkbox v-model="localConfig.caseSensitive">区分大小写</el-checkbox>
        <el-checkbox v-model="localConfig.trimWhitespace">去除空格后比较</el-checkbox>
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
  get: () => props.config || { deduplicateFields: [], strategy: 'FIRST', caseSensitive: false, trimWhitespace: false },
  set: (val) => emit('update:config', val)
})
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>