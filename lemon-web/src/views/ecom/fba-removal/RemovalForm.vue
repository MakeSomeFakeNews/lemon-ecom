<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomFba.removal.add')" :edit-title="$t('ecomFba.removal.editTitle', { no: form.docNo || '' })">
    <a-alert type="info" show-icon :message="$t('ecomFba.removal.flowTip')" class="block" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomFba.common.store')" name="storeId">
            <StoreSelect v-model:value="form.storeId" :platform="EcomPlatform.AMAZON" :label="form.storeName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomFba.removal.type')" name="removalType">
            <a-radio-group v-model:value="form.removalType" button-style="solid">
              <a-radio-button v-for="o in dict.ecom_fba_removal_type" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomFba.removal.platformOrderId')" name="platformOrderId">
            <a-input v-model:value="form.platformOrderId" :maxlength="64" :placeholder="$t('ecomFba.removal.platformOrderIdPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomFba.removal.requestDate')" name="requestDate">
            <a-date-picker v-model:value="form.requestDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <template v-if="form.removalType === FbaRemovalType.RETURN">
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('ecomFba.removal.warehouse')" name="warehouseId">
              <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('ecomFba.removal.warehousePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('ecomFba.removal.defectiveWarehouse')" name="defectiveWarehouseId">
              <WarehouseSelect v-model:value="form.defectiveWarehouseId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-form-item :label="$t('ecomFba.removal.shipAddress')" name="shipAddress">
              <a-input v-model:value="form.shipAddress" :maxlength="255" />
            </a-form-item>
          </a-col>
        </template>
        <a-col :xs="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomFba.removal.items') }}</span>
        <a-space :size="16" wrap>
          <span>{{ $t('ecomFba.removal.requested') }} <b>{{ qty(totalQty) }}</b></span>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <a-form-item name="items" :rules="itemRules" class="items-item">
        <LineItemsTable :items="form.items" :columns="itemColumns">
          <template #cell="{ column, record }">
            <template v-if="column.key === 'sellerSku'">
              <a-input v-model:value="record.sellerSku" :maxlength="64" />
            </template>
            <template v-else-if="column.key === 'disposition'">
              <a-select v-model:value="record.disposition" :options="toOptions(dict.ecom_fba_disposition)" class="full" />
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-model:value="record.qty" :min="0" :precision="0" class="full" />
            </template>
          </template>
        </LineItemsTable>
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addRemoval, getRemoval, updateRemoval } from '@/api/ecom/fba'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { toOptions } from '@/utils/dict'
import { qty, round4 } from '@/utils/number'
import { EcomPlatform, FbaDisposition, FbaRemovalType } from '@/constants/enums'
import { t } from '@/locales'

/** 移除计划表单（页式）：ref 调用 openCreate(defaults?)、openEdit(record) */
const emit = defineEmits(['success'])
const dict = useDict('ecom_fba_removal_type', 'ecom_fba_disposition')

const itemColumns = [
  { title: t('ecomFba.common.sku'), key: 'sellerSku', width: 260 },
  { title: t('ecomFba.removal.disposition'), key: 'disposition', width: 150 },
  { title: t('ecomFba.removal.requested'), key: 'qty', width: 140 },
  { title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }
]
const emptyForm = () => ({
  storeId: undefined,
  removalType: FbaRemovalType.RETURN,
  platformOrderId: '',
  warehouseId: undefined,
  defectiveWarehouseId: undefined,
  shipAddress: '',
  requestDate: dayjs().format('YYYY-MM-DD'),
  remark: '',
  items: []
})
const newRow = () => ({ sellerSku: '', disposition: FbaDisposition.SELLABLE, qty: undefined })
const lines = useLineItems(() => form.value.items, { newRow })

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const d = await getRemoval(record.id)
    return {
      ...d,
      warehouseId: d.warehouseId ?? undefined,
      defectiveWarehouseId: d.defectiveWarehouseId ?? undefined,
      items: (d.items || []).map((i) => lines.withKey({ id: i.id, sellerSku: i.sellerSku, disposition: i.disposition, qty: Number(i.requestedQty) }))
    }
  },
  create: addRemoval,
  update: updateRemoval,
  toPayload: (f) => ({
    storeId: f.storeId,
    removalType: f.removalType,
    platformOrderId: f.platformOrderId || null,
    warehouseId: f.warehouseId ?? null,
    defectiveWarehouseId: f.defectiveWarehouseId ?? null,
    shipAddress: f.shipAddress || null,
    requestDate: f.requestDate || null,
    remark: f.remark || null,
    items: f.items.map((i) => ({ sellerSku: i.sellerSku.trim(), disposition: i.disposition, qty: i.qty }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const totalQty = lines.total('qty', round4)

const rules = {
  storeId: [{ required: true, message: t('ecomFba.removal.selectStore') }],
  removalType: [{ required: true, message: t('ecomFba.removal.selectType') }]
}
const itemRules = [
  {
    validator: () => {
      const items = form.value.items
      if (!items.length) return Promise.reject(t('ecomFba.removal.noItems'))
      if (items.some((i) => !i.sellerSku?.trim() || !(Number(i.qty) > 0))) return Promise.reject(t('ecomFba.removal.itemInvalid'))
      return Promise.resolve()
    }
  }
]

/** defaults.items：[{ sku, qty }]（库存健康勾选生成） */
function openCreate(defaults = {}) {
  const items = (defaults.items || []).map((i) => lines.withKey({ ...newRow(), sellerSku: i.sku, qty: i.qty }))
  return drawer.openCreate({ ...emptyForm(), storeId: defaults.storeId, items: items.length ? items : [lines.withKey(newRow())] })
}
const openEdit = (record) => drawer.openEdit(record)

defineExpose({ openCreate, openEdit })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
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
