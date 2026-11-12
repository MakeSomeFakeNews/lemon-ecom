<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecom.listing.add')" :edit-title="$t('ecom.listing.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecom.listing.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.common.store')" name="storeId">
            <StoreSelect v-model:value="form.storeId" :label="form.storeName" :allow-clear="false" @select="onStoreSelect" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.common.sellerSku')" name="sellerSku" :extra="$t('ecom.listing.sellerSkuExtra')">
            <a-input v-model:value="form.sellerSku" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.common.platformItemId')" name="platformItemId" :extra="$t('ecom.listing.platformItemIdExtra')">
            <a-input v-model:value="form.platformItemId" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.listing.parentItemId')" name="parentItemId">
            <a-input v-model:value="form.parentItemId" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="16">
          <a-form-item :label="$t('ecom.listing.title')" name="title">
            <a-input v-model:value="form.title" :maxlength="255" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('ecom.listing.variant')" name="variant" :extra="$t('ecom.listing.variantExtra')">
            <a-input v-model:value="form.variant" :maxlength="128" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecom.listing.sectionSales') }}</div>
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecom.listing.price')" name="price">
            <a-input-number v-model:value="form.price" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecom.store.currency')" name="currency" :extra="$t('ecom.listing.currencyExtra')">
            <a-select v-model:value="form.currency" allow-clear show-search option-filter-prop="label" :options="currencyOptions(currencies)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('ecom.listing.fulfillment')" name="fulfillment">
            <a-select v-model:value="form.fulfillment" :options="toOptions(dict.ecom_fulfillment)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-select v-model:value="form.status" :options="toOptions(dict.ecom_listing_status)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item label="FNSKU" name="fnsku" :extra="$t('ecomCatalog.listing.fnskuExtra')">
            <a-input v-model:value="form.fnsku" :maxlength="32" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecom.listing.sectionMapping') }}</div>
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('ecomCatalog.listing.composeType')" name="composeType" :extra="$t('ecomCatalog.listing.composeTypeExtra')">
            <a-radio-group v-model:value="form.composeType" :options="toOptions(dict.ecom_compose_type)" option-type="button" />
          </a-form-item>
        </a-col>
        <template v-if="form.composeType === ListingComposeType.BUNDLE">
          <a-col :span="24">
            <a-form-item :label="$t('ecomCatalog.bundle.items')" name="bundleItems" :rules="bundleRules">
              <BundleItemsEditor :items="form.bundleItems" />
            </a-form-item>
          </a-col>
        </template>
        <template v-else>
          <a-col :xs="24" :sm="16" :lg="12">
            <a-form-item :label="$t('ecom.common.material')" name="materialId" :extra="$t('ecom.listing.mapExtra')">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8" :lg="6">
            <a-form-item :label="$t('ecom.common.packQty')" name="packQty" :extra="$t('ecom.listing.packQtyExtra')">
              <a-input-number v-model:value="form.packQty" :min="0.0001" :precision="4" class="full" />
            </a-form-item>
          </a-col>
        </template>
      </a-row>

      <div class="section-title">{{ $t('ecom.listing.sectionOther') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecom.listing.imageUrl')" name="imageUrl">
            <a-input v-model:value="form.imageUrl" :maxlength="255" placeholder="https://" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecom.listing.url')" name="url">
            <a-input v-model:value="form.url" :maxlength="255" placeholder="https://" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addListing, getListing, updateListing } from '@/api/ecom/listing'
import { currencyOptions, useCurrencies } from '@/components/fin/currency'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { FulfillmentType, ListingComposeType, ListingStatus } from '@/constants/enums'
import { lineKey, rowKey } from '@/composables/useLineItems'
import BundleItemsEditor from './BundleItemsEditor.vue'
import { materialLabel } from './listing'

/** 在线商品新增 / 编辑子页：openCreate() / openEdit(record) 打开，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_fulfillment', 'ecom_listing_status', 'ecom_compose_type')
const currencies = useCurrencies()

const rules = {
  storeId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.common.store') }) }],
  sellerSku: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecom.common.sellerSku') }) }]
}

const FIELDS = ['storeId', 'sellerSku', 'platformItemId', 'parentItemId', 'title', 'variant', 'price', 'currency', 'fulfillment', 'status',
  'materialId', 'packQty', 'imageUrl', 'url', 'remark', 'composeType', 'fnsku']

/** 组合商品至少一个组件，物料必选、用量大于 0、不重复 */
const bundleRules = [
  {
    validator: () => {
      const items = form.value.bundleItems || []
      if (!items.length) return Promise.reject(t('ecomCatalog.bundle.empty'))
      if (items.some((i) => i.materialId == null)) return Promise.reject(t('ecomCatalog.bundle.selectMaterial'))
      if (items.some((i) => !(Number(i.qty) > 0))) return Promise.reject(t('ecomCatalog.bundle.inputQty'))
      if (new Set(items.map((i) => i.materialId)).size !== items.length) return Promise.reject(t('ecomCatalog.bundle.duplicate'))
      return Promise.resolve()
    }
  }
]
const newBundleRow = () => ({ key: rowKey(), materialId: undefined, materialLabel: '', unit: '', qty: 1 })
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    ...Object.fromEntries(FIELDS.map((f) => [f, undefined])),
    fulfillment: FulfillmentType.SELLER,
    status: ListingStatus.ACTIVE,
    packQty: 1,
    composeType: ListingComposeType.SINGLE,
    bundleItems: [newBundleRow()],
    storeName: undefined,
    materialLabel: undefined
  }),
  fetch: async (record) => {
    const d = await getListing(record.id)
    const bundleItems = (d.bundleItems || []).map((b) => ({
      key: lineKey(b),
      materialId: b.materialId,
      materialLabel: `${b.materialCode || ''} ${b.materialName || ''}${b.materialSpec ? `（${b.materialSpec}）` : ''}`.trim(),
      unit: b.unit,
      qty: Number(b.qty)
    }))
    return {
      ...d,
      price: d.price != null ? Number(d.price) : undefined,
      packQty: d.packQty != null ? Number(d.packQty) : 1,
      composeType: d.composeType || ListingComposeType.SINGLE,
      bundleItems: bundleItems.length ? bundleItems : [newBundleRow()],
      materialLabel: materialLabel(d)
    }
  },
  create: addListing,
  update: updateListing,
  toPayload: (f) => {
    const p = Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]]))
    if (f.composeType === ListingComposeType.BUNDLE) {
      p.materialId = null
      p.packQty = 1
      p.bundleItems = f.bundleItems.map((b) => ({ materialId: b.materialId, qty: b.qty }))
    }
    return p
  },
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

/** 选择店铺后币种默认为店铺币种 */
function onStoreSelect(store) {
  if (!form.value.currency && store.currency) form.value.currency = store.currency
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
</style>
