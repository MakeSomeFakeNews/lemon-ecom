<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomOrder.inbound.addTitle')" :edit-title="editTitle">
    <a-alert v-if="costsMode" type="info" show-icon class="block" :message="$t('ecomOrder.inbound.costsHint')" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.common.store')" name="storeId" :extra="costsMode ? undefined : $t('ecomOrder.inbound.warehouseAuto')">
            <StoreSelect v-model:value="form.storeId" :label="form.storeName" :allow-clear="false" :disabled="costsMode" @select="onStore" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.inbound.shipmentId')" name="shipmentId">
            <a-input v-model:value="form.shipmentId" :maxlength="64" :placeholder="$t('ecomOrder.inbound.shipmentIdPh')" :disabled="costsMode" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.inbound.fromWarehouse')" name="fromWarehouseId">
            <WarehouseSelect v-model:value="form.fromWarehouseId" :allow-clear="false" :disabled="costsMode" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.inbound.toWarehouse')" name="toWarehouseId">
            <WarehouseSelect v-model:value="form.toWarehouseId" :allow-clear="false" :disabled="costsMode" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.common.channel')" name="channelId">
            <ChannelSelect v-model:value="form.channelId" :label="form.channelName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.common.carrier')" name="carrier">
            <a-input v-model:value="form.carrier" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.common.trackingNo')" name="trackingNo">
            <a-input v-model:value="form.trackingNo" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="3">
          <a-form-item :label="$t('ecomOrder.inbound.shipDate')" name="shipDate">
            <a-date-picker v-model:value="form.shipDate" value-format="YYYY-MM-DD" class="full" :disabled="costsMode" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="3">
          <a-form-item :label="$t('ecomOrder.inbound.etaDate')" name="etaDate">
            <a-date-picker v-model:value="form.etaDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomOrder.inbound.boxCount')" name="boxCount">
            <a-input-number v-model:value="form.boxCount" :min="0" :max="99999" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomOrder.inbound.weight')" name="weight">
            <a-input-number v-model:value="form.weight" :min="0" :precision="3" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomOrder.inbound.volume')" name="volume">
            <a-input-number v-model:value="form.volume" :min="0" :precision="4" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="`${$t('ecomOrder.inbound.freight')} ${$t('ecomOrder.inbound.costBase')}`" name="freight">
            <a-input-number v-model:value="form.freight" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="`${$t('ecomOrder.inbound.duty')} ${$t('ecomOrder.inbound.costBase')}`" name="duty">
            <a-input-number v-model:value="form.duty" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="`${$t('ecomOrder.inbound.otherCost')} ${$t('ecomOrder.inbound.costBase')}`" name="otherCost">
            <a-input-number v-model:value="form.otherCost" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="`${$t('ecomProfit.inbound.importVat')} ${$t('ecomOrder.inbound.costBase')}`" name="importVat" :extra="$t('ecomProfit.inbound.importVatExtra')">
            <a-input-number v-model:value="form.importVat" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomOrder.inbound.items') }}</span>
        <a-space :size="16" wrap>
          <span>{{ $t('ecomOrder.inbound.totalQty') }} <b>{{ qty(totalQty) }}</b></span>
          <span>{{ $t('ecomOrder.inbound.totalCost') }} <b>{{ baseMoney(costTotal) }}</b></span>
          <a-tooltip :title="$t('ecomOrder.inbound.unitLandedHint')">
            <span class="dashed">{{ $t('ecomOrder.inbound.unitLanded') }} <b>¥{{ price(unitLanded) }}</b></span>
          </a-tooltip>
          <a-button v-if="!costsMode" size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <a-form-item name="items" :rules="itemRules" class="items-item">
        <LineItemsTable :items="form.items" :columns="itemColumns">
          <template #cell="{ column, record }">
            <template v-if="column.key === 'materialId'">
              <MaterialSelect v-if="!costsMode" v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => onMaterial(record, m)" />
              <span v-else>{{ record.materialLabel }}</span>
            </template>
            <template v-else-if="column.key === 'sellerSku'">
              <a-input v-if="!costsMode" v-model:value="record.sellerSku" :maxlength="64" @change="record.listingId = undefined" />
              <span v-else>{{ record.sellerSku || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-if="!costsMode" v-model:value="record.qty" :min="0" :precision="4" class="full" :addon-after="record.unit || undefined" />
              <span v-else>{{ qty(record.qty) }} {{ record.unit || '' }}</span>
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-if="!costsMode" v-model:value="record.remark" :maxlength="255" />
              <span v-else>{{ record.remark || '-' }}</span>
            </template>
          </template>
        </LineItemsTable>
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import { addEcomInbound, getEcomInbound, updateEcomInbound, updateEcomInboundCosts } from '@/api/ecom/inbound'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { price, qty, round4, sumBy } from '@/utils/number'
import { baseMoney } from '../order/order'
import { totalCost, unitLandedOf } from './inbound'

/**
 * 头程货件表单（页式）：新增 / 修改草稿（InboundRequest），选店铺后带出店铺的默认发货仓（调出）与平台仓（调入）；
 * costs 模式（已发运 / 已签收后「补录费用」，PUT /ecom/inbound/{id}/costs）只允许改物流与费用字段，其余只读但仍随表单提交（后端只取物流与费用）。
 * ref 调用 openCreate()、openEdit(record)、openCosts(record)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const mode = ref('edit')
const costsMode = computed(() => mode.value === 'costs')

/** 补录费用时明细只读，不显示删除列 */
const itemColumns = computed(() => [
  { title: t('ecomOrder.common.material'), key: 'materialId', width: 320 },
  { title: t('ecomOrder.common.sellerSku'), key: 'sellerSku', width: 160 },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 160 },
  { title: t('common.field.remark'), key: 'remark', width: 200 },
  ...(costsMode.value ? [] : [{ title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }])
])

const emptyForm = () => ({
  storeId: undefined,
  storeName: undefined,
  shipmentId: '',
  fromWarehouseId: undefined,
  toWarehouseId: undefined,
  channelId: undefined,
  channelName: undefined,
  carrier: '',
  trackingNo: '',
  shipDate: undefined,
  etaDate: undefined,
  boxCount: undefined,
  weight: undefined,
  volume: undefined,
  freight: undefined,
  duty: undefined,
  importVat: undefined,
  otherCost: undefined,
  remark: '',
  items: []
})
const newRow = () => ({ materialId: undefined, materialLabel: '', unit: '', sellerSku: '', qty: undefined, remark: '' })
const lines = useLineItems(() => form.value.items, { newRow })

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const d = await getEcomInbound(record.id)
    return {
      ...d,
      items: (d.items || []).map((i) =>
        lines.withKey({
          id: i.id,
          materialId: i.materialId,
          materialLabel: `${i.materialCode || ''} ${i.materialName || ''}${i.materialSpec ? `（${i.materialSpec}）` : ''}`.trim(),
          unit: i.unit,
          sellerSku: i.sellerSku,
          qty: Number(i.qty),
          remark: i.remark,
          listingId: i.listingId,
          planItemId: i.planItemId
        })
      )
    }
  },
  create: addEcomInbound,
  update: (id, payload) => (costsMode.value ? updateEcomInboundCosts(id, payload) : updateEcomInbound(id, payload)),
  toPayload: (f) => ({
    storeId: f.storeId,
    shipmentId: f.shipmentId || null,
    fromWarehouseId: f.fromWarehouseId,
    toWarehouseId: f.toWarehouseId,
    channelId: f.channelId ?? null,
    carrier: f.carrier || null,
    trackingNo: f.trackingNo || null,
    shipDate: f.shipDate || null,
    etaDate: f.etaDate || null,
    boxCount: f.boxCount ?? null,
    weight: f.weight ?? null,
    volume: f.volume ?? null,
    freight: f.freight ?? null,
    duty: f.duty ?? null,
    importVat: f.importVat ?? null,
    otherCost: f.otherCost ?? null,
    remark: f.remark || null,
    // 由发货计划生成的明细带着在线商品与计划明细，原样提交（改了平台 SKU 的行清空在线商品，后端按新 SKU 识别）
    items: f.items.map((i) => ({ materialId: i.materialId, sellerSku: i.sellerSku || null, qty: i.qty, remark: i.remark || null,
      listingId: i.listingId ?? null, planItemId: i.planItemId ?? null }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

const editTitle = computed(() =>
  costsMode.value ? t('ecomOrder.inbound.costsTitle', { no: form.value.docNo || '' }) : t('ecomOrder.inbound.editTitle', { no: form.value.docNo || '' })
)

const totalQty = lines.total('qty', round4)
const costTotal = computed(() => totalCost(form.value))
const unitLanded = computed(() => unitLandedOf(form.value, totalQty.value))

const rules = {
  storeId: [{ required: true, message: t('ecomOrder.inbound.selectStore') }],
  fromWarehouseId: [{ required: true, message: t('ecomOrder.inbound.selectFrom') }],
  toWarehouseId: [
    { required: true, message: t('ecomOrder.inbound.selectTo') },
    { validator: (_, v) => (v != null && v === form.value.fromWarehouseId ? Promise.reject(t('ecomOrder.inbound.sameWarehouse')) : Promise.resolve()) }
  ]
}
const itemRules = [
  {
    validator: () => {
      const items = form.value.items
      if (!items.length) return Promise.reject(t('ecomOrder.inbound.noItems'))
      if (items.some((i) => i.materialId == null)) return Promise.reject(t('ecomOrder.inbound.selectMaterial'))
      if (items.some((i) => !(Number(i.qty) > 0))) return Promise.reject(t('ecomOrder.inbound.inputQty'))
      return Promise.resolve()
    }
  }
]

/** 选店铺：带出店铺的默认发货仓（调出）与平台仓（调入） */
function onStore(store) {
  form.value.storeName = store.name
  if (store.warehouseId) form.value.fromWarehouseId = store.warehouseId
  if (store.platformWarehouseId) form.value.toWarehouseId = store.platformWarehouseId
}

function onMaterial(record, m) {
  record.materialLabel = m.label
  record.unit = m.unit
}

function openCreate() {
  mode.value = 'edit'
  return drawer.openCreate({ items: [lines.withKey(newRow())] })
}
function openEdit(record) {
  mode.value = 'edit'
  return drawer.openEdit(record)
}
function openCosts(record) {
  mode.value = 'costs'
  return drawer.openEdit(record)
}

defineExpose({ openCreate, openEdit, openCosts })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
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
.dashed {
  border-bottom: 1px dashed var(--app-text-3);
  cursor: help;
}
.items-item :deep(.ant-form-item-control-input) {
  min-height: 0;
}
</style>
