<template>
  <div class="attrs">
    <div v-if="remote" class="toolbar">
      <a-button size="small" :disabled="!categoryId" :loading="loading" @click="loadDefs">{{ $t('ecomPublish.attr.load') }}</a-button>
      <span class="sub-text">{{ categoryId ? $t('ecomPublish.attr.loadTip') : $t('ecomPublish.attr.needCategory') }}</span>
    </div>
    <template v-if="defs.length">
      <a-checkbox v-model:checked="requiredOnly" class="required-only">{{ $t('ecomPublish.attr.requiredOnly') }}</a-checkbox>
      <div class="def-grid">
        <div v-for="d in shownDefs" :key="d.key" class="def">
          <div class="def-label">
            <span v-if="d.required" class="required">*</span>{{ d.name }}
            <a-tooltip v-if="d.description" :title="d.description"><QuestionCircleOutlined class="help" /></a-tooltip>
            <span class="sub-text"> {{ d.key }}</span>
          </div>
          <a-select v-if="d.inputType === 'SELECT' || d.inputType === 'MULTI'" :value="selectValue(d)" :mode="d.inputType === 'MULTI' ? 'multiple' : undefined"
                    allow-clear show-search :options="d.options.map((o) => ({ value: o, label: o }))" :disabled="disabled" class="full"
                    @change="(v) => setValue(d.key, Array.isArray(v) ? v.join(',') : v)" />
          <a-input v-else :value="value[d.key]" :disabled="disabled" @update:value="(v) => setValue(d.key, v)" />
        </div>
      </div>
      <a-divider class="divider">{{ $t('ecomPublish.attr.custom') }}</a-divider>
    </template>
    <KeyValueEditor :value="custom" :disabled="disabled" :add-text="$t('ecomPublish.attr.addCustom')" @update:value="setCustom" />
    <div class="sub-text">{{ $t('ecomPublish.attr.tip') }}</div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { publishAttributes } from '@/api/ecom/publish'
import KeyValueEditor from './KeyValueEditor.vue'

/**
 * 平台属性：能查询类目属性的平台加载类目的属性定义（必填标 *，下拉 / 多选按可选值），其余属性以键值对填写；
 * 不能查询的平台全部手工填写键值。v-model:value 为对象 { 属性键: 值 }（多选用英文逗号分隔）
 */
const props = defineProps({ value: { type: Object, default: () => ({}) }, storeId: Number, categoryId: String, remote: Boolean, disabled: Boolean })
const emit = defineEmits(['update:value'])

const defs = ref([])
const loading = ref(false)
const requiredOnly = ref(false)
const defKeys = computed(() => new Set(defs.value.map((d) => d.key)))
const shownDefs = computed(() => (requiredOnly.value ? defs.value.filter((d) => d.required) : defs.value))
const custom = computed(() => Object.fromEntries(Object.entries(props.value || {}).filter(([k]) => !defKeys.value.has(k))))

watch(
  () => props.categoryId,
  () => (defs.value = [])
)

async function loadDefs() {
  loading.value = true
  try {
    defs.value = (await publishAttributes(props.storeId, props.categoryId)) || []
  } finally {
    loading.value = false
  }
}
const selectValue = (d) => {
  const v = props.value?.[d.key]
  if (d.inputType === 'MULTI') return v ? String(v).split(',').filter(Boolean) : []
  return v || undefined
}
function setValue(key, v) {
  const next = { ...props.value }
  if (v == null || v === '') delete next[key]
  else next[key] = v
  emit('update:value', next)
}
function setCustom(obj) {
  const fixed = Object.fromEntries(Object.entries(props.value || {}).filter(([k]) => defKeys.value.has(k)))
  emit('update:value', { ...fixed, ...obj })
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.required-only {
  margin-bottom: 8px;
}
.def-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 8px 16px;
}
.def-label {
  margin-bottom: 4px;
}
.required {
  color: #ff4d4f;
  margin-right: 2px;
}
.help {
  color: var(--app-text-3);
  margin-left: 4px;
}
.full {
  width: 100%;
}
.divider {
  font-size: 13px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
