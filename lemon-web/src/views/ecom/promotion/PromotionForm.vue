<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomOper.promo.addTitle')" :edit-title="$t('ecomOper.promo.editTitle', { name: form.name || '' })">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomOper.promo.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomOper.common.store')" name="storeId">
            <StoreSelect v-model:value="form.storeId" :label="form.storeName" :allow-clear="false" @select="onStore" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomOper.promo.promoType')" name="promoType">
            <a-select v-model:value="form.promoType" :options="optionsWithCurrent(dict.ecom_promo_type, form.promoType)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomOper.promo.period')" name="range">
            <a-range-picker v-model:value="form.range" show-time :format="'YYYY-MM-DD HH:mm'" value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomOper.promo.discountPercent')" name="discountPercent" :tooltip="$t('ecomOper.promo.discountExtra')">
            <a-input-number v-model:value="form.discountPercent" :min="0" :max="100" :precision="2" class="full" @change="applyDefaultDiscount" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomOper.promo.threshold')" name="thresholdAmount">
            <a-input-number v-model:value="form.thresholdAmount" :min="0" :precision="2" class="full" :addon-after="form.currency" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomOper.promo.reduce')" name="reduceAmount">
            <a-input-number v-model:value="form.reduceAmount" :min="0" :precision="2" class="full" :addon-after="form.currency" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomOper.promo.budget')" name="budget">
            <a-input-number v-model:value="form.budget" :min="0" :precision="2" class="full" :addon-after="form.currency" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomOper.promo.fee')" name="feeAmount" :tooltip="$t('ecomOper.promo.feeExtra')">
            <a-input-number v-model:value="form.feeAmount" :min="0" :precision="2" class="full" :addon-after="form.currency" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomOper.common.owner')" name="ownerId">
            <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="16">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomOper.promo.items') }}</span>
        <a-space :size="12" wrap>
          <span class="sub-text">{{ $t('ecomOper.promo.noItemsTip') }}</span>
          <a-button size="small" :disabled="!form.storeId" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <a-form-item name="items" :rules="itemRules" class="items-item">
        <a-form-item-rest>
        <LineItemsTable :items="form.items" :columns="itemColumns">
          <template #cell="{ column, record }">
            <template v-if="column.key === 'listingId'">
              <ListingSelect v-model:value="record.listingId" :label="record.listingLabel" :store-id="form.storeId" @select="(l) => onListing(record, l)" />
              <a-input v-if="record.listingId == null" v-model:value="record.sellerSku" :maxlength="64" size="small" :placeholder="$t('ecomOper.promo.sellerSku')" class="sku-input" />
            </template>
            <template v-else-if="column.key === 'originalPrice'">
              <a-input-number v-model:value="record.originalPrice" :min="0" :precision="2" class="full" @change="() => onPrice(record, 'orig')" />
            </template>
            <template v-else-if="column.key === 'discountPercent'">
              <a-input-number v-model:value="record.discountPercent" :min="0" :max="100" :precision="2" class="full" @change="() => onPrice(record, 'discount')" />
            </template>
            <template v-else-if="column.key === 'promoPrice'">
              <a-input-number v-model:value="record.promoPrice" :min="0" :precision="2" class="full" @change="() => onPrice(record, 'price')" />
            </template>
            <template v-else-if="column.key === 'promoQty'">
              <a-input-number v-model:value="record.promoQty" :min="0" :precision="0" class="full" />
            </template>
            <template v-else-if="column.key === 'limitPerBuyer'">
              <a-input-number v-model:value="record.limitPerBuyer" :min="0" :precision="0" class="full" />
            </template>
          </template>
        </LineItemsTable>
        </a-form-item-rest>
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import ListingSelect from '@/components/ecom/ListingSelect.vue'
import { addPromotion, getPromotion, updatePromotion } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { optionsWithCurrent } from '@/utils/dict'
import { round2 } from '@/utils/number'

/** 促销表单（页式）：店铺、类型、时间、折扣 / 满减、费用、参与 SKU（促销价与折扣按原价互算）。ref 调用 openCreate()、openEdit(record) */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_promo_type')

const itemColumns = [
  { title: t('ecomOper.promo.listing'), key: 'listingId', width: 340 },
  { title: t('ecomOper.promo.originalPrice'), key: 'originalPrice', width: 120 },
  { title: t('ecomOper.promo.itemDiscount'), key: 'discountPercent', width: 110 },
  { title: t('ecomOper.promo.promoPrice'), key: 'promoPrice', width: 120 },
  { title: t('ecomOper.promo.promoQty'), key: 'promoQty', width: 110 },
  { title: t('ecomOper.promo.limitPerBuyer'), key: 'limitPerBuyer', width: 110 },
  { title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }
]
const newRow = () => ({ listingId: undefined, listingLabel: '', sellerSku: '', title: '', originalPrice: undefined, discountPercent: undefined, promoPrice: undefined, promoQty: undefined, limitPerBuyer: undefined })
const lines = useLineItems(() => form.value.items, { newRow })
const num = (v) => (v == null || v === '' ? undefined : Number(v))

const drawer = useFormDrawer({
  emptyForm: () => ({
    name: '', storeId: undefined, storeName: undefined, currency: undefined, promoType: undefined, range: undefined, discountPercent: undefined,
    thresholdAmount: undefined, reduceAmount: undefined, budget: undefined, feeAmount: 0, ownerId: undefined, ownerName: undefined, remark: '', items: []
  }),
  fetch: async (record) => {
    const d = await getPromotion(record.id)
    return {
      ...d,
      range: [d.startTime, d.endTime],
      discountPercent: num(d.discountPercent), thresholdAmount: num(d.thresholdAmount), reduceAmount: num(d.reduceAmount),
      budget: num(d.budget), feeAmount: num(d.feeAmount),
      items: (d.items || []).map((i) => lines.withKey({
        listingId: i.listingId,
        listingLabel: `${i.sellerSku}${i.title ? `  ${i.title}` : ''}`,
        sellerSku: i.sellerSku,
        title: i.title,
        originalPrice: num(i.originalPrice), discountPercent: num(i.discountPercent), promoPrice: num(i.promoPrice),
        promoQty: i.promoQty ?? undefined, limitPerBuyer: i.limitPerBuyer ?? undefined
      }))
    }
  },
  create: addPromotion,
  update: updatePromotion,
  toPayload: (f) => ({
    name: f.name,
    storeId: f.storeId,
    promoType: f.promoType,
    startTime: f.range?.[0],
    endTime: f.range?.[1],
    discountPercent: f.discountPercent ?? null,
    thresholdAmount: f.thresholdAmount ?? null,
    reduceAmount: f.reduceAmount ?? null,
    budget: f.budget ?? null,
    feeAmount: f.feeAmount ?? 0,
    ownerId: f.ownerId ?? null,
    remark: f.remark || null,
    items: f.items.filter((i) => i.sellerSku).map((i) => ({
      listingId: i.listingId ?? null, sellerSku: i.sellerSku, title: i.title || null, originalPrice: i.originalPrice ?? null,
      discountPercent: i.discountPercent ?? null, promoPrice: i.promoPrice ?? null, promoQty: i.promoQty ?? null, limitPerBuyer: i.limitPerBuyer ?? null
    }))
  }),
  onSuccess: (id) => emit('success', id)
})
const { formRef, form } = drawer

const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomOper.promo.name') }) }],
  storeId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecomOper.common.store') }) }],
  promoType: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecomOper.promo.promoType') }) }],
  range: [{ required: true, type: 'array', message: t('common.rule.requiredSelect', { name: t('ecomOper.promo.period') }) }]
}
const itemRules = [
  { validator: () => (form.value.items.some((i) => !i.sellerSku && i.listingId == null && (i.promoPrice != null || i.discountPercent != null))
    ? Promise.reject(t('ecomOper.promo.inputSku')) : Promise.resolve()) }
]

function onStore(store) {
  form.value.storeName = store.name
  form.value.currency = store.currency
  form.value.items.forEach((i) => Object.assign(i, newRow()))
}
function onListing(record, l) {
  record.listingLabel = `${l.sellerSku}${l.title ? `  ${l.title}` : ''}`
  record.sellerSku = l.sellerSku
  record.title = l.title
  record.originalPrice = l.price == null ? undefined : round2(Number(l.price))
  if (record.discountPercent == null && form.value.discountPercent != null) record.discountPercent = form.value.discountPercent
  onPrice(record, 'discount')
}

/** 促销价与折扣按原价互算 */
function onPrice(record, changed) {
  const orig = Number(record.originalPrice)
  if (!(orig > 0)) return
  if ((changed === 'discount' || changed === 'orig') && record.discountPercent != null) {
    record.promoPrice = round2((orig * (100 - Number(record.discountPercent))) / 100)
  } else if (changed === 'price' && record.promoPrice != null) {
    record.discountPercent = round2(((orig - Number(record.promoPrice)) / orig) * 100)
  }
}
/** 修改默认折扣：还没有填折扣的行按默认折扣计算 */
function applyDefaultDiscount() {
  form.value.items.forEach((i) => {
    if (i.discountPercent == null && i.promoPrice == null && form.value.discountPercent != null) {
      i.discountPercent = form.value.discountPercent
      onPrice(i, 'discount')
    }
  })
}

const openCreate = () => drawer.openCreate({ items: [lines.withKey(newRow())] })
defineExpose({ openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.sku-input {
  margin-top: 4px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.items-item :deep(.ant-form-item-control-input) {
  min-height: 0;
}
</style>
