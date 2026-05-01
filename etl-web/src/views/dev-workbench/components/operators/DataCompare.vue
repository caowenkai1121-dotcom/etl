<template>
  <div class="operator-config">
    <el-form label-width="90px">
      <el-form-item label="比对表">
        <el-input v-model="localConfig.compareTable" placeholder="要对比的数据表名" @input="emitUpdate" />
      </el-form-item>
      <el-form-item label="比对键">
        <el-select v-model="localConfig.compareKeys" multiple placeholder="选择比对主键字段" style="width:100%" @change="emitUpdate">
          <el-option v-for="f in inputFields" :key="f.name" :label="f.name" :value="f.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="比对字段">
        <el-select v-model="localConfig.compareFields" multiple placeholder="选择需要比对的字段" style="width:100%" @change="emitUpdate">
          <el-option v-for="f in inputFields" :key="f.name" :label="f.name" :value="f.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="输出标记">
        <el-checkbox-group v-model="localConfig.outputFlags" @change="emitUpdate">
          <el-checkbox label="NEW">新增的数据</el-checkbox>
          <el-checkbox label="UPDATED">更新的数据</el-checkbox>
          <el-checkbox label="DELETED">删除的数据</el-checkbox>
          <el-checkbox label="SAME">相同的数据</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ config: { type: Object, default: () => ({}) }, inputFields: { type: Array, default: () => [] } })
const emit = defineEmits(['update:config'])
const localConfig = computed({
  get: () => ({ compareTable: '', compareKeys: [], compareFields: [], outputFlags: ['NEW', 'UPDATED'], ...props.config }),
  set: (v) => emit('update:config', v)
})
const emitUpdate = () => emit('update:config', { ...localConfig.value })
</script>
<style scoped>.operator-config { padding: 8px 0; }</style>
