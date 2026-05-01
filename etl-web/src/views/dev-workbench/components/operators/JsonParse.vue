<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="JSON字段">
        <el-select v-model="localConfig.sourceField" placeholder="选择包含JSON的字段" style="width:100%" @change="emitUpdate">
          <el-option v-for="f in inputFields" :key="f.name" :label="f.name" :value="f.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="解析模式">
        <el-radio-group v-model="localConfig.parseMode" @change="emitUpdate">
          <el-radio value="AUTO">自动解析</el-radio>
          <el-radio value="MANUAL">手动指定路径</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="localConfig.parseMode === 'MANUAL'">
        <el-form-item label="输出字段">
          <div v-for="(field, idx) in localConfig.outputFields" :key="idx" style="display:flex;gap:8px;margin-bottom:8px">
            <el-input v-model="field.path" placeholder="JSON路径: $.data.name" size="small" style="flex:1" @input="emitUpdate" />
            <el-input v-model="field.alias" placeholder="别名" size="small" style="width:100px" @input="emitUpdate" />
            <el-button size="small" type="danger" @click="removeField(idx)">×</el-button>
          </div>
          <el-button size="small" @click="addField">+ 添加路径</el-button>
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ config: { type: Object, default: () => ({}) }, inputFields: { type: Array, default: () => [] } })
const emit = defineEmits(['update:config'])
const localConfig = computed({
  get: () => ({ sourceField: '', parseMode: 'AUTO', outputFields: [{ path: '', alias: '' }], ...props.config }),
  set: (v) => emit('update:config', v)
})
const emitUpdate = () => emit('update:config', { ...localConfig.value })
const addField = () => { localConfig.value.outputFields.push({ path: '', alias: '' }); emitUpdate() }
const removeField = (i) => { localConfig.value.outputFields.splice(i, 1); emitUpdate() }
</script>
<style scoped>.operator-config { padding: 8px 0; }</style>
