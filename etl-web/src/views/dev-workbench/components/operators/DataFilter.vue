<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="过滤条件">
        <div class="filter-rules">
          <div v-for="(rule, index) in localConfig.filterRules" :key="index" class="filter-rule-item">
            <el-select v-model="rule.field" size="small" placeholder="字段" style="width: 120px">
              <el-option v-for="field in inputFields" :key="field.name" :label="field.name" :value="field.name" />
            </el-select>
            <el-select v-model="rule.operator" size="small" placeholder="操作符" style="width: 100px">
              <el-option label="等于" value="=" />
              <el-option label="不等于" value="!=" />
              <el-option label="大于" value=">" />
              <el-option label="小于" value="<" />
              <el-option label="大于等于" value=">="/>
              <el-option label="小于等于" value="<="/>
              <el-option label="包含" value="LIKE" />
              <el-option label="不包含" value="NOT LIKE" />
              <el-option label="为空" value="IS NULL" />
              <el-option label="不为空" value="IS NOT NULL" />
            </el-select>
            <el-input v-model="rule.value" size="small" placeholder="值" style="width: 150px" :disabled="rule.operator === 'IS NULL' || rule.operator === 'IS NOT NULL'" />
            <el-button type="danger" size="small" text @click="removeRule(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="small" @click="addRule">添加过滤条件</el-button>
      </el-form-item>

      <el-divider />

      <el-form-item label="条件组合">
        <el-radio-group v-model="localConfig.combineType">
          <el-radio value="AND">满足所有条件</el-radio>
          <el-radio value="OR">满足任一条件</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="过滤方式">
        <el-radio-group v-model="localConfig.filterType">
          <el-radio value="KEEP">保留匹配数据</el-radio>
          <el-radio value="REMOVE">排除匹配数据</el-radio>
        </el-radio-group>
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
  get: () => props.config || { filterRules: [], combineType: 'AND', filterType: 'KEEP' },
  set: (val) => emit('update:config', val)
})

const addRule = () => {
  if (!localConfig.value.filterRules) {
    localConfig.value.filterRules = []
  }
  localConfig.value.filterRules.push({
    field: '',
    operator: '=',
    value: ''
  })
  emit('update:config', localConfig.value)
}

const removeRule = (index) => {
  localConfig.value.filterRules?.splice(index, 1)
  emit('update:config', localConfig.value)
}
</script>

<style lang="scss" scoped>
.operator-config {
  padding: 16px;
}

.filter-rules {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>