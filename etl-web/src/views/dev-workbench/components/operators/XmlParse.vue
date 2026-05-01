<template>
  <div class="operator-config">
    <el-form label-width="80px">
      <el-form-item label="XML字段">
        <el-select v-model="localConfig.sourceField" placeholder="选择包含XML的字段" style="width:100%" @change="emitUpdate">
          <el-option v-for="f in inputFields" :key="f.name" :label="f.name" :value="f.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="根节点XPath">
        <el-input v-model="localConfig.rootPath" placeholder="/root/item" @input="emitUpdate" />
      </el-form-item>
      <el-form-item label="字段映射">
        <div v-for="(m, i) in localConfig.fieldMappings" :key="i" style="display:flex;gap:8px;margin-bottom:8px">
          <el-input v-model="m.xpath" placeholder="XPath: name/text()" size="small" style="flex:1" @input="emitUpdate" />
          <el-input v-model="m.alias" placeholder="输出字段" size="small" style="width:120px" @input="emitUpdate" />
          <el-button size="small" type="danger" @click="removeMapping(i)">×</el-button>
        </div>
        <el-button size="small" @click="addMapping">+ 添加映射</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ config: { type: Object, default: () => ({}) }, inputFields: { type: Array, default: () => [] } })
const emit = defineEmits(['update:config'])
const localConfig = computed({
  get: () => ({ sourceField: '', rootPath: '/', fieldMappings: [{ xpath: '', alias: '' }], ...props.config }),
  set: (v) => emit('update:config', v)
})
const emitUpdate = () => emit('update:config', { ...localConfig.value })
const addMapping = () => { localConfig.value.fieldMappings.push({ xpath: '', alias: '' }); emitUpdate() }
const removeMapping = (i) => { localConfig.value.fieldMappings.splice(i, 1); emitUpdate() }
</script>
<style scoped>.operator-config { padding: 8px 0; }</style>
