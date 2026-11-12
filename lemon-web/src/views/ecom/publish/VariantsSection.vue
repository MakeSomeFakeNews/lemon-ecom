<template>
  <div>
    <div v-if="!readonly" class="variant-toolbar">
      <a-select v-model:value="form.priceTemplateId" allow-clear :options="priceOptions" :placeholder="$t('ecomPublish.item.priceTemplate')" class="pt-select" />
      <a-button :disabled="!form.priceTemplateId" :loading="quoting" @click="quote(true)">{{ $t('ecomPublish.item.fillSuggested') }}</a-button>
      <a-button :disabled="!form.priceTemplateId" :loading="quoting" @click="quote(false)">{{ $t('ecomPublish.item.quote') }}</a-button>
      <span class="spacer" />
      <MaterialSelect v-model:value="addMaterialId" class="add-material" :placeholder="$t('ecomPublish.item.addVariantPh')" @select="addVariant" />
    </div>
    <a-table row-key="key" size="small" :columns="variantColumns" :data-source="form.variants" :pagination="false" :scroll="{ x: 1200 }">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }}</div>
          <div class="sub-text cell-ellipsis" :title="record.materialName">{{ record.materialName }}</div>
        </template>
        <template v-else-if="column.key === 'sellerSku'">
          <a-input v-model:value="record.sellerSku" :maxlength="64" />
        </template>
        <template v-else-if="column.key === 'attrs'">
          <a-input v-model:value="record.attrsText" :placeholder="$t('ecomPublish.item.attrsPh')" />
        </template>
        <template v-else-if="column.key === 'barcode'">
          <a-input v-model:value="record.barcode" :maxlength="32" />
        </template>
        <template v-else-if="column.key === 'price'">
          <a-input-number v-model:value="record.price" :min="0" :precision="2" class="full" :addon-after="form.currency" />
        </template>
        <template v-else-if="column.key === 'qty'">
          <a-input-number v-model:value="record.qty" :min="0" :max="999999" :precision="0" class="full" />
        </template>
        <template v-else-if="column.key === 'suggested'">
          <span v-if="record.suggestedPrice != null">{{ price(record.suggestedPrice) }}</span>
          <span v-else class="sub-text">-</span>
          <div v-if="record.cost != null" class="sub-text">{{ $t('ecomPublish.item.costBase', { cost: money(record.cost) }) }}</div>
        </template>
        <template v-else-if="column.key === 'margin'">
          <template v-if="record.estMargin != null">
            <span :class="record.estMargin < 0 ? 'loss' : 'gain'">{{ record.estMargin }}%</span>
            <div class="sub-text">{{ $t('ecomPublish.item.profitOf', { profit: money(record.estProfit) }) }}</div>
          </template>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'platform'">{{ record.platformSkuId || '-' }}</template>
        <template v-else-if="column.key === 'ops'">
          <a-tooltip v-if="!readonly" :title="$t('common.action.delete')">
            <a-button type="text" danger size="small" :disabled="form.variants.length <= 1" @click="form.variants.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-tooltip>
        </template>
      </template>
    </a-table>
    <div class="variant-summary sub-text">
      {{ $t('ecomPublish.item.variantSummary', { n: form.variants.length, qty: totalQty }) }}
    </div>
    <a-form-item v-if="form.variants.length > 1" :label="$t('ecomPublish.item.parentSku')" name="parentSku" class="parent-sku" :extra="$t('ecomPublish.item.parentSkuExtra')">
      <a-input v-model:value="form.parentSku" :maxlength="64" />
    </a-form-item>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { quotePrice } from '@/api/ecom/publish'
import { money, price } from '@/utils/number'

/**
 * 刊登工作台「变体价格库存」分区：变体行（平台 SKU、变体属性、条码、售价、库存），按价格模板填入建议价 / 试算利润率，
 * 选择物料增加变体。form 为刊登内容表单（直接修改其 variants、priceTemplateId、parentSku）
 */
const props = defineProps({
  form: { type: Object, required: true },
  readonly: Boolean,
  priceTemplates: { type: Array, default: () => [] },
  nextKey: { type: Function, required: true }
})
const { t } = useI18n()
const quoting = ref(false)
const addMaterialId = ref()
const priceOptions = computed(() => props.priceTemplates.map((x) => ({ value: x.id, label: x.name })))
const totalQty = computed(() => props.form.variants.reduce((s, v) => s + (Number(v.qty) || 0), 0))

const variantColumns = [
  { title: t('ecomPublish.item.material'), key: 'material', width: 180, fixed: 'left' },
  { title: t('ecomPublish.common.sellerSku'), key: 'sellerSku', width: 170 },
  { title: t('ecomPublish.item.variantAttrs'), key: 'attrs', width: 210 },
  { title: t('ecomPublish.item.barcode'), key: 'barcode', width: 140 },
  { title: t('ecomPublish.item.price'), key: 'price', width: 160 },
  { title: t('ecomPublish.item.qty'), key: 'qty', width: 100 },
  { title: t('ecomPublish.item.suggested'), key: 'suggested', width: 130, align: 'right' },
  { title: t('ecomPublish.item.margin'), key: 'margin', width: 120, align: 'right' },
  { title: t('ecomPublish.item.platformSkuId'), key: 'platform', width: 130 },
  { title: '', key: 'ops', width: 50, fixed: 'right', align: 'center' }
]

function addVariant(m) {
  if (!m || m.isTemplate === 1) return
  props.form.variants.push({ key: props.nextKey(), materialId: m.id, materialCode: m.code, materialName: m.name, sellerSku: m.code, barcode: m.barcode, attrsText: '', qty: 0 })
  addMaterialId.value = undefined
}

/** 逐个变体按价格模板试算：fill 时把售价改为建议价 */
async function quote(fill) {
  const f = props.form
  quoting.value = true
  try {
    for (const v of f.variants) {
      const q = await quotePrice({ priceTemplateId: f.priceTemplateId, storeId: f.storeId, materialId: v.materialId, price: fill ? undefined : v.price })
      v.cost = q.cost
      v.suggestedPrice = q.suggestedPrice
      if (fill && q.suggestedPrice != null) v.price = q.suggestedPrice
      v.estProfit = q.profit
      v.estMargin = q.margin
    }
  } finally {
    quoting.value = false
  }
}
</script>

<style scoped>
.variant-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.pt-select {
  width: 220px;
}
.add-material {
  width: 280px;
  max-width: 100%;
}
.spacer {
  flex: 1;
}
.full {
  width: 100%;
}
.variant-summary {
  margin-top: 8px;
}
.parent-sku {
  margin-top: 12px;
  max-width: 360px;
}
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.loss {
  color: #cf1322;
}
.gain {
  color: #389e0d;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
