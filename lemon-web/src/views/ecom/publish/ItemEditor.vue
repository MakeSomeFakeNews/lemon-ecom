<template>
  <FormDrawer page :drawer="drawer" :edit-title="title" width="100%">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="readonly">
      <a-alert v-if="caps?.note" type="info" show-icon class="block" :message="caps.note" />
      <a-alert v-if="form.errorMessage" type="error" show-icon class="block" :message="$t('ecomPublish.item.lastError')" :description="form.errorMessage" />

      <a-divider orientation="left">{{ $t('ecomPublish.item.content') }}</a-divider>
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.title')" name="title">
            <a-input v-model:value="form.title" :maxlength="255" show-count />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.bullets')">
            <div v-for="(b, i) in form.bullets" :key="i" class="bullet-row">
              <span class="bullet-no">{{ i + 1 }}</span>
              <a-input v-model:value="form.bullets[i]" :maxlength="500" />
              <a-button v-if="!readonly" type="text" danger @click="form.bullets.splice(i, 1)"><template #icon><DeleteOutlined /></template></a-button>
            </div>
            <a-button v-if="!readonly && form.bullets.length < 10" type="dashed" size="small" @click="form.bullets.push('')">
              <template #icon><PlusOutlined /></template>{{ $t('ecomPublish.item.addBullet') }}
            </a-button>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.description')" name="description">
            <a-textarea v-model:value="form.description" :maxlength="20000" show-count :auto-size="{ minRows: 4, maxRows: 12 }" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomPublish.item.brand')" name="brand">
            <a-input v-model:value="form.brand" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomPublish.item.barcodeType')" name="barcodeType">
            <a-select v-model:value="form.barcodeType" allow-clear :options="barcodeOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomPublish.item.upcExempt')" name="upcExempt">
            <a-switch v-model:checked="form.upcExempt" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.upcExempt === 1" :span="24">
          <a-form-item :label="$t('ecomPublish.item.exemptionNote')" name="exemptionNote">
            <a-input v-model:value="form.exemptionNote" :maxlength="255" :placeholder="$t('ecomPublish.item.exemptionNotePh')" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider orientation="left">{{ $t('ecomPublish.item.images') }}</a-divider>
      <ImagesEditor v-model:value="form.images" :material-id="form.materialId" :disabled="readonly" />

      <a-divider orientation="left">{{ $t('ecomPublish.item.variants') }}</a-divider>
      <div v-if="!readonly" class="variant-toolbar">
        <a-select v-model:value="form.priceTemplateId" allow-clear :options="priceOptions" :placeholder="$t('ecomPublish.item.priceTemplate')" class="pt-select" />
        <a-button :disabled="!form.priceTemplateId" :loading="quoting" @click="fillSuggested">{{ $t('ecomPublish.item.fillSuggested') }}</a-button>
        <a-button :disabled="!form.priceTemplateId" :loading="quoting" @click="quoteAll">{{ $t('ecomPublish.item.quote') }}</a-button>
        <span class="spacer" />
        <MaterialSelect v-model:value="addMaterialId" class="add-material" :placeholder="$t('ecomPublish.item.addVariantPh')" @select="addVariant" />
      </div>
      <a-table row-key="key" size="small" :columns="variantColumns" :data-source="form.variants" :pagination="false" :scroll="{ x: 1200 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }}</div>
            <div class="sub-text">{{ record.materialName }}</div>
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
          <template v-else-if="column.key === 'platform'">
            <div>{{ record.platformSkuId || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'ops'">
            <a-button v-if="!readonly" type="text" danger :disabled="form.variants.length <= 1" @click="form.variants.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
      <a-form-item v-if="form.variants.length > 1" :label="$t('ecomPublish.item.parentSku')" name="parentSku" class="parent-sku" :extra="$t('ecomPublish.item.parentSkuExtra')">
        <a-input v-model:value="form.parentSku" :maxlength="64" />
      </a-form-item>

      <a-divider orientation="left">{{ $t('ecomPublish.item.categoryAttrs') }}</a-divider>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPublish.item.publishTemplate')">
            <a-select v-model:value="form.publishTemplateId" allow-clear :options="templateOptions" @change="applyTemplate" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPublish.item.category')" :required="caps?.categoryRequired">
            <CategoryPicker v-model:id="form.categoryId" v-model:name="form.categoryName" :store-id="form.storeId" :remote="hasCap('CATEGORIES')" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.attributes')">
            <AttributesEditor v-model:value="form.attributes" :store-id="form.storeId" :category-id="form.categoryId" :remote="hasCap('ATTRIBUTES')" :disabled="readonly" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider orientation="left">{{ $t('ecomPublish.item.logistics') }}</a-divider>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('ecomPublish.item.fulfillment')" name="fulfillment">
            <a-select v-model:value="form.fulfillment" allow-clear :options="toOptions(dict.ecom_fulfillment)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('ecomPublish.item.shippingTemplate')" name="shippingTemplate">
            <a-input v-model:value="form.shippingTemplate" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('ecomPublish.item.returnPolicy')" name="returnPolicy">
            <a-input v-model:value="form.returnPolicy" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('ecomPublish.item.handlingDays')" name="handlingDays">
            <a-input-number v-model:value="form.handlingDays" :min="0" :max="60" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.extra')" :extra="$t('ecomPublish.item.extraTip')">
            <KeyValueEditor v-model:value="form.extra" :disabled="readonly" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <template #actions="{ saving, loading }">
      <a-button v-if="!readonly" type="primary" :loading="saving" :disabled="loading" @click="drawer.submit">{{ $t('common.action.save') }}</a-button>
    </template>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { getPublishItem, getPublishTemplate, priceTemplateOptions, publishCapabilities, publishTemplateOptions, quotePrice, updatePublishItem } from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { PublishBarcodeType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { money, price } from '@/utils/number'
import AttributesEditor from './AttributesEditor.vue'
import CategoryPicker from './CategoryPicker.vue'
import ImagesEditor from './ImagesEditor.vue'
import KeyValueEditor from './KeyValueEditor.vue'
import { EDITABLE, attrsText, parseAttrs } from './publish'

/**
 * 一个店铺的刊登内容（子页）：标题、五点描述、描述、品牌、条码 / UPC 豁免、图片、变体（平台 SKU、变体属性、条码、售价、库存，
 * 按价格模板填入建议价、试算利润率）、平台类目与属性（能查询的平台从接口选择）、配送与政策、其他平台参数。
 * openEdit(record) 编辑（草稿 / 失败 / 已下架），openView(record) 只读查看，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_fulfillment')
const readonly = ref(false)
const caps = ref(null)
const templates = ref([])
const priceTemplates = ref([])
const quoting = ref(false)
const addMaterialId = ref()
let seq = 0

const barcodeOptions = Object.values(PublishBarcodeType).map((v) => ({ value: v, label: v }))
const rules = { title: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomPublish.item.title') }) }] }
const hasCap = (c) => !!caps.value?.capabilities?.includes(c)
const priceOptions = computed(() => priceTemplates.value.map((x) => ({ value: x.id, label: x.name })))
const templateOptions = computed(() => templates.value.map((x) => ({ value: x.id, label: x.name })))

const variantColumns = [
  { title: t('ecomPublish.item.material'), key: 'material', width: 180, fixed: 'left' },
  { title: t('ecomPublish.common.sellerSku'), key: 'sellerSku', width: 170 },
  { title: t('ecomPublish.item.variantAttrs'), key: 'attrs', width: 210 },
  { title: t('ecomPublish.item.barcode'), key: 'barcode', width: 140 },
  { title: t('ecomPublish.item.price'), key: 'price', width: 150 },
  { title: t('ecomPublish.item.qty'), key: 'qty', width: 100 },
  { title: t('ecomPublish.item.suggested'), key: 'suggested', width: 130, align: 'right' },
  { title: t('ecomPublish.item.margin'), key: 'margin', width: 120, align: 'right' },
  { title: t('ecomPublish.item.platformSkuId'), key: 'platform', width: 130 },
  { title: '', key: 'ops', width: 50 }
]

const emptyForm = () => ({ id: undefined, title: '', bullets: [], description: '', images: [], variants: [], attributes: {}, extra: {} })
const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const item = await getPublishItem(record.id)
    const [c, ts, ps] = await Promise.all([publishCapabilities(item.storeId), publishTemplateOptions(item.platform), priceTemplateOptions(item.platform)])
    caps.value = c
    templates.value = ts || []
    priceTemplates.value = ps || []
    if (!EDITABLE.includes(item.status)) readonly.value = true
    return {
      ...item,
      bullets: item.bullets || [],
      images: item.images || [],
      attributes: item.attributes || {},
      extra: item.extra || {},
      variants: (item.variants || []).map((v) => ({ ...v, key: ++seq, attrsText: attrsText(v.variantAttrs) }))
    }
  },
  update: updatePublishItem,
  toPayload: (f) => ({
    title: f.title,
    bullets: f.bullets.filter((b) => b && b.trim()),
    description: f.description || null,
    images: f.images,
    brand: f.brand || null,
    barcodeType: f.barcodeType || null,
    upcExempt: f.upcExempt ? 1 : 0,
    exemptionNote: f.exemptionNote || null,
    categoryId: f.categoryId || null,
    categoryName: f.categoryName || null,
    attributes: f.attributes,
    publishTemplateId: f.publishTemplateId || null,
    priceTemplateId: f.priceTemplateId || null,
    fulfillment: f.fulfillment || null,
    shippingTemplate: f.shippingTemplate || null,
    returnPolicy: f.returnPolicy || null,
    handlingDays: f.handlingDays ?? null,
    extra: f.extra,
    parentSku: f.parentSku || null,
    variants: f.variants.map((v) => ({
      materialId: v.materialId,
      sellerSku: v.sellerSku,
      variantText: v.variantText || Object.values(parseAttrs(v.attrsText)).join(' / ') || null,
      variantAttrs: parseAttrs(v.attrsText),
      barcode: v.barcode || null,
      price: v.price,
      qty: v.qty ?? 0
    }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const title = computed(() => (form.value.storeName ? `${form.value.storeName} · ${t(readonly.value ? 'ecomPublish.item.viewTitle' : 'ecomPublish.item.editTitle')}` : ''))

function openEdit(record) {
  readonly.value = false
  return drawer.openEdit(record)
}
function openView(record) {
  readonly.value = true
  return drawer.openEdit(record)
}

/** 选择刊登模板：带入类目、属性（合并）、运费模板、退货政策、备货天数、配送方式、价格模板、其他参数 */
async function applyTemplate(id) {
  if (!id) return
  const tpl = await getPublishTemplate(id)
  const f = form.value
  if (tpl.categoryId) {
    f.categoryId = tpl.categoryId
    f.categoryName = tpl.categoryName
  }
  f.attributes = { ...(tpl.attributes || {}), ...(f.attributes || {}) }
  f.extra = { ...(tpl.extra || {}), ...(f.extra || {}) }
  f.shippingTemplate = tpl.shippingTemplate || f.shippingTemplate
  f.returnPolicy = tpl.returnPolicy || f.returnPolicy
  f.handlingDays = tpl.handlingDays ?? f.handlingDays
  f.fulfillment = tpl.fulfillment || f.fulfillment
  if (!f.brand && tpl.brand) f.brand = tpl.brand
  if (tpl.priceTemplateId) f.priceTemplateId = tpl.priceTemplateId
}

function addVariant(m) {
  if (!m || m.isTemplate === 1) return
  form.value.variants.push({ key: ++seq, materialId: m.id, materialCode: m.code, materialName: m.name, sellerSku: m.code, barcode: m.barcode, attrsText: '', qty: 0 })
  addMaterialId.value = undefined
}

/** 逐个变体按价格模板试算：fill 时把售价改为建议价 */
async function quote(fill) {
  quoting.value = true
  try {
    for (const v of form.value.variants) {
      const q = await quotePrice({ priceTemplateId: form.value.priceTemplateId, storeId: form.value.storeId, materialId: v.materialId, price: fill ? undefined : v.price })
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
const fillSuggested = () => quote(true)
const quoteAll = () => quote(false)

defineExpose({ openEdit, openView })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.bullet-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.bullet-no {
  width: 20px;
  color: var(--app-text-3);
  text-align: right;
}
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
}
.spacer {
  flex: 1;
}
.full {
  width: 100%;
}
.parent-sku {
  margin-top: 12px;
  max-width: 360px;
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
