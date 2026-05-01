<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="源字段">
        <el-select v-model="localConfig.sourceField" size="small" placeholder="选择源字段" style="width: 100%">
          <el-option v-for="field in stringFields" :key="field.name" :label="field.name" :value="field.name" />
        </el-select>
      </el-form-item>

      <el-divider />

      <el-form-item label="分割方式">
        <el-radio-group v-model="localConfig.splitType">
          <el-radio value="DELIMITER">分隔符分割</el-radio>
          <el-radio value="POSITION">位置分割</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="分隔符" v-if="localConfig.splitType === 'DELIMITER'">
        <el-input v-model="localConfig.delimiter" size="small" placeholder="如: , | - 等" style="width: 100px" />
      </el-form-item>

      <el-divider />

      <el-form-item label="输出字段">
        <el-input-number v-model="localConfig.maxParts" size="small" :min="1" :max="10" />
        <span style="margin-left: 8px">最多分割成几个字段</span>
      </el-form-item>

      <el-form-item label="字段命名">
        <el-input v-model="localConfig.fieldPrefix" size="small" placeholder="如: part_" style="width: 100px" />
        <span style="margin-left: 8px">+ 序号</span>
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
  get: () => props.config || { sourceField: '', splitType: 'DELIMITER', delimiter: ',', maxParts: 5, fieldPrefix: 'part_' },
  set: (val) => emit('update:config', val)
})

const stringFields = computed(() => {
  return props.inputFields.filter(f => ['VARCHAR', 'CHAR', 'TEXT', 'STRING'].includes(f.type?.toUpperCase()))
})
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}
</style>