<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomCatalog.plan.addTitle')" :edit-title="$t('ecomCatalog.plan.editTitle', { no: form.planNo || '' })">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.common.store')" name="storeId">
            <StoreSelect v-model:value="form.storeId" :label="form.storeName" :allow-clear="false" @select="onStore" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomCatalog.plan.fromWarehouse')" name="fromWarehouseId">
            <WarehouseSelect v-model:value="form.fromWarehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomCatalog.plan.toWarehouse')" name="toWarehouseId">
            <WarehouseSelect v-model:value="form.toWarehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomCatalog.plan.planDate')" name="planDate">
            <a-date-picker v-model:value="form.planDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomCatalog.plan.items') }}</span>
        <a-space :size="16" wrap>
          <span>{{ $t('ecomCatalog.plan.totalQty') }} <b>{{ qty(totalQty) }}</b></span>
          <a-button size="small" :disabled="!form.storeId" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <a-form-item name="items" :rules="itemRules" class="items-item">
        <LineItemsTable :items="form.items" :columns="itemColumns">
          <template #cell="{ column, record }">
            <template v-if="column.key === 'listingId'">
              <ListingSelect v-model:value="record.listingId" :label="record.listingLabel" :store-id="form.storeId" @select="(l) => onListing(record, l)" />
            </template>
            <template v-else-if="column.key === 'composition'">{{ record.composition || '-' }}</template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-model:value="record.qty" :min="0" :precision="4" class="full" />
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" />
            </template>
          </template>
        </LineItemsTable>
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import ListingSelect from '@/components/ecom/ListingSelect.vue'
import { addShipmentPlan, getShipmentPlan, updateShipmentPlan } from '@/api/ecom/shipmentPlan'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { qty, round4 } from '@/utils/number'
import { compositionText } from './plan'

/** 发货计划表单（页式）：新增 / 修改草稿，明细为平台 SKU 与计划件数。ref 调用 openCreate()、openEdit(record) */
const emit = defineEmits(['success'])
const { t } = useI18n()

const itemColumns = [
  { title: t('ecomCatalog.plan.listing'), key: 'listingId', width: 360 },
  { title: t('ecomCatalog.plan.composition'), key: 'composition', width: 180 },
  { title: t('ecomCatalog.plan.qty'), key: 'qty', width: 140 },
  { title: t('common.field.remark'), key: 'remark', width: 200 },
  { title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }
]
const newRow = () => ({ listingId: undefined, listingLabel: '', composition: '', qty: undefined, remark: '' })
const lines = useLineItems(() => form.value.items, { newRow })
const bundleLabel = t('ecomCatalog.listing.bundle')

const drawer = useFormDrawer({
  emptyForm: () => ({ storeId: undefined, storeName: undefined, fromWarehouseId: undefined, toWarehouseId: undefined, planDate: undefined, remark: '', items: [] }),
  fetch: async (record) => {
    const d = await getShipmentPlan(record.id)
    return {
      ...d,
      items: (d.items || []).map((i) => lines.withKey({
        id: i.id,
        listingId: i.listingId,
        listingLabel: `${i.sellerSku}${i.title ? `  ${i.title}` : ''}`,
        composition: compositionText(i, bundleLabel),
        qty: Number(i.qty),
        remark: i.remark
      }))
    }
  },
  create: addShipmentPlan,
  update: updateShipmentPlan,
  toPayload: (f) => ({
    storeId: f.storeId,
    fromWarehouseId: f.fromWarehouseId ?? null,
    toWarehouseId: f.toWarehouseId ?? null,
    planDate: f.planDate || null,
    remark: f.remark || null,
    items: f.items.map((i) => ({ listingId: i.listingId, qty: i.qty, remark: i.remark || null }))
  }),
  onSuccess: (id) => emit('success', id)
})
const { formRef, form } = drawer
const totalQty = lines.total('qty', round4)

const rules = {
  storeId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.common.store') }) }],
  fromWarehouseId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecomCatalog.plan.fromWarehouse') }) }],
  toWarehouseId: [
    { required: true, message: t('common.rule.requiredSelect', { name: t('ecomCatalog.plan.toWarehouse') }) },
    { validator: (_, v) => (v != null && v === form.value.fromWarehouseId ? Promise.reject(t('ecomCatalog.plan.sameWarehouse')) : Promise.resolve()) }
  ]
}
const itemRules = [
  {
    validator: () => {
      const items = form.value.items
      if (!items.length) return Promise.reject(t('ecomCatalog.plan.noItems'))
      if (items.some((i) => i.listingId == null)) return Promise.reject(t('ecomCatalog.common.selectListing'))
      if (items.some((i) => !(Number(i.qty) > 0))) return Promise.reject(t('ecomCatalog.plan.inputQty'))
      return Promise.resolve()
    }
  }
]

/** 选店铺：带出默认发货仓与平台仓 */
function onStore(store) {
  form.value.storeName = store.name
  if (store.warehouseId) form.value.fromWarehouseId = store.warehouseId
  if (store.platformWarehouseId) form.value.toWarehouseId = store.platformWarehouseId
}
function onListing(record, l) {
  record.listingLabel = `${l.sellerSku}${l.title ? `  ${l.title}` : ''}`
  record.composition = compositionText(l, bundleLabel)
}

const openCreate = () => drawer.openCreate({ items: [lines.withKey(newRow())] })
defineExpose({ openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
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
.items-item :deep(.ant-form-item-control-input) {
  min-height: 0;
}
</style>
