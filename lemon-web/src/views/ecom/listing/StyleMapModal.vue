<template>
  <a-modal :open="open" :title="$t('ecomCatalog.style.title')" width="min(1000px, 100vw)" :ok-text="$t('ecomCatalog.style.apply')"
           :ok-button-props="{ disabled: !selected.length }" :confirm-loading="saving" @ok="apply" @cancel="open = false">
    <a-alert type="info" show-icon class="block" :message="$t('ecomCatalog.style.hint')" />
    <a-form layout="inline" class="block filters">
      <a-form-item :label="$t('ecom.common.store')">
        <StoreSelect v-model:value="form.storeId" :allow-clear="false" class="w200" />
      </a-form-item>
      <a-form-item :label="$t('ecom.listing.parentItemId')">
        <a-input v-model:value="form.parentItemId" :maxlength="64" allow-clear class="w160" />
      </a-form-item>
      <a-form-item :label="$t('ecomCatalog.style.template')">
        <a-select v-model:value="form.templateId" show-search :filter-option="false" :options="templates" class="w240"
                  :placeholder="$t('ecomCatalog.style.templatePh')" @search="searchTemplates" @focus="searchTemplates('')" />
      </a-form-item>
      <a-form-item :label="$t('ecomCatalog.style.rule')">
        <a-radio-group v-model:value="form.rule" :options="ruleOptions" option-type="button" />
      </a-form-item>
      <template v-if="form.rule === StyleMatchRule.SKU">
        <a-form-item :label="$t('ecomCatalog.style.prefix')"><a-input v-model:value="form.prefix" :maxlength="32" class="w120" /></a-form-item>
        <a-form-item :label="$t('ecomCatalog.style.suffix')"><a-input v-model:value="form.suffix" :maxlength="32" class="w120" /></a-form-item>
      </template>
      <a-form-item>
        <a-checkbox v-model:checked="form.includeMapped">{{ $t('ecomCatalog.style.includeMapped') }}</a-checkbox>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="loading" :disabled="!form.templateId || !form.storeId" @click="preview">{{ $t('ecomCatalog.style.preview') }}</a-button>
      </a-form-item>
    </a-form>
    <a-table row-key="listingId" size="small" :columns="columns" :data-source="rows" :pagination="false" :scroll="{ x: 900, y: 360 }"
             :row-selection="{ selectedRowKeys: selected, onChange: (k) => (selected = k), getCheckboxProps: (r) => ({ disabled: !r.materialId }) }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'current'">{{ record.currentMaterialCode || '-' }}</template>
        <template v-else-if="column.key === 'match'">
          <template v-if="record.materialId">
            <div>{{ record.materialCode }}</div>
            <div class="sub-text">{{ record.variantText }}</div>
          </template>
          <a-tag v-else :bordered="false">{{ $t('ecomCatalog.style.noMatch') }}</a-tag>
        </template>
      </template>
    </a-table>
    <div class="sub-text">{{ $t('ecomCatalog.style.summary', { n: rows.length, m: rows.filter((r) => r.materialId).length }) }}</div>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { mapListingBatch, previewStyleMap } from '@/api/ecom/listing'
import { templateOptions } from '@/api/wms/material'
import { StyleMatchRule } from '@/constants/enums'

/**
 * 按款式批量映射：选一个多规格款式，按变体属性或 SKU 编码规则把店铺（或某个父商品）下的在线商品配对到款式的 SKU 物料，
 * 预览后勾选确认。ref 调用 open({ storeId, parentItemId })，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const selected = ref([])
const templates = ref([])
const form = reactive({ storeId: undefined, parentItemId: undefined, templateId: undefined, rule: StyleMatchRule.ATTRIBUTE, prefix: '', suffix: '', includeMapped: false })
const ruleOptions = [
  { value: StyleMatchRule.ATTRIBUTE, label: t('ecomCatalog.style.ruleAttribute') },
  { value: StyleMatchRule.SKU, label: t('ecomCatalog.style.ruleSku') }
]
const columns = [
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 180 },
  { title: t('ecom.listing.variant'), dataIndex: 'variant', width: 140 },
  { title: t('ecomCatalog.common.title'), dataIndex: 'title', ellipsis: true },
  { title: t('ecomCatalog.style.current'), key: 'current', width: 150 },
  { title: t('ecomCatalog.style.matched'), key: 'match', width: 200 }
]

async function searchTemplates(keyword) {
  const list = (await templateOptions(keyword || undefined)) || []
  templates.value = list.map((m) => ({ value: m.id, label: `${m.code} ${m.name}` }))
}

async function preview() {
  loading.value = true
  try {
    rows.value = (await previewStyleMap({ ...form, parentItemId: form.parentItemId || null, prefix: form.prefix || null, suffix: form.suffix || null })) || []
    selected.value = rows.value.filter((r) => r.materialId).map((r) => r.listingId)
  } finally {
    loading.value = false
  }
}

async function apply() {
  const pairs = rows.value.filter((r) => r.materialId && selected.value.includes(r.listingId)).map((r) => ({ listingId: r.listingId, materialId: r.materialId }))
  saving.value = true
  try {
    const n = await mapListingBatch(pairs)
    message.success(t('ecomCatalog.style.applied', { n }))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

function show({ storeId, parentItemId } = {}) {
  Object.assign(form, { storeId, parentItemId, templateId: undefined, prefix: '', suffix: '', includeMapped: false })
  rows.value = []
  selected.value = []
  open.value = true
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.filters {
  row-gap: 8px;
}
.w120 {
  width: 120px;
}
.w160 {
  width: 160px;
}
.w200 {
  width: 200px;
}
.w240 {
  width: 240px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
