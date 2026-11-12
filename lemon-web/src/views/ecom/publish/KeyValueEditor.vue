<template>
  <div class="kv">
    <div v-for="(row, i) in rows" :key="i" class="kv-row">
      <a-input v-model:value="row.key" :placeholder="keyPlaceholder || $t('ecomPublish.kv.key')" class="kv-key" :disabled="disabled" @change="emitValue" />
      <a-input v-model:value="row.value" :placeholder="$t('ecomPublish.kv.value')" class="kv-value" :disabled="disabled" @change="emitValue" />
      <a-button v-if="!disabled" type="text" danger @click="removeRow(i)"><template #icon><DeleteOutlined /></template></a-button>
    </div>
    <a-button v-if="!disabled" type="dashed" size="small" @click="addRow"><template #icon><PlusOutlined /></template>{{ addText || $t('ecomPublish.kv.add') }}</a-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { toObject, toRows } from './publish'

/**
 * 键值对编辑（平台属性、其他平台参数）：v-model:value 为对象 { 键: 值 }，空键 / 空值的行不输出
 */
const props = defineProps({ value: { type: Object, default: () => ({}) }, keyPlaceholder: String, addText: String, disabled: Boolean })
const emit = defineEmits(['update:value'])
const rows = ref(toRows(props.value))
let self = false
watch(
  () => props.value,
  (v) => {
    if (self) {
      self = false
      return
    }
    rows.value = toRows(v)
  }
)
function emitValue() {
  self = true
  emit('update:value', toObject(rows.value))
}
function addRow() {
  rows.value.push({ key: '', value: '' })
}
function removeRow(i) {
  rows.value.splice(i, 1)
  emitValue()
}
</script>

<style scoped>
.kv-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.kv-key {
  flex: 2;
}
.kv-value {
  flex: 3;
}
</style>
