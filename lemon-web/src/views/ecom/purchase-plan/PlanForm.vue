<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomPurchase.plan.add')" :edit-title="$t('ecomPurchase.plan.editTitle', { no: form.docNo || '' })">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPurchase.plan.planDate')" name="planDate">
            <a-date-picker v-model:value="form.planDate" value-format="YYYY-MM-DD" class="full" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPurchase.plan.warehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('ecomPurchase.plan.warehousePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomPurchase.plan.items') }}</span>
        <a-space :size="16" wrap>
          <span>{{ $t('ecomPurchase.plan.totalQty') }} <b>{{ qty(totalQty) }}</b></span>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <a-form-item name="items" :rules="itemRules" class="items-item">
        <LineItemsTable :items="form.items" :columns="itemColumns">
          <template #cell="{ column, record }">
            <template v-if="column.key === 'materialId'">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => onMaterial(record, m)" />
              <div v-if="demandList(record.demand).length" class="sub-text">
                {{ $t('ecomPurchase.plan.demand') }}: {{ demandList(record.demand).map(demandText).join('; ') }}
              </div>
            </template>
            <template v-else-if="column.key === 'suggestQty'">{{ record.suggestQty != null ? qty(record.suggestQty) : '-' }}</template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-model:value="record.qty" :min="0" :precision="4" class="full" :addon-after="record.unit || undefined" />
            </template>
            <template v-else-if="column.key === 'supplierId'">
              <PartnerSelect v-model:value="record.supplierId" type="supplier" :label="record.supplierName" @select="(p) => (record.supplierName = p.name)" />
            </template>
            <template v-else-if="column.key === 'price'">
              <a-input-number v-model:value="record.price" :min="0" :precision="4" class="full" :placeholder="$t('ecomPurchase.plan.pricePh')" />
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" class="full" :placeholder="$t('ecomPurchase.plan.taxRatePh')" />
            </template>
            <template v-else-if="column.key === 'arrivalDate'">
              <a-date-picker v-model:value="record.arrivalDate" value-format="YYYY-MM-DD" class="full" />
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
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addPurchasePlan, getPurchasePlan, updatePurchasePlan } from '@/api/ecom/purchasePlan'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { qty, round4 } from '@/utils/number'
import { demandList, demandText } from '../replenish/replenish'

/**
 * 采购计划表单（页式）：新增 / 修改草稿（PurchasePlanRequest）。由补货建议生成的明细带有需求构成（demand），修改时原样提交。
 * ref 调用 openCreate()、openEdit(record)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const itemColumns = [
  { title: t('ecomPurchase.plan.material'), key: 'materialId', width: 300 },
  { title: t('ecomPurchase.plan.suggestQty'), key: 'suggestQty', width: 90, align: 'right' },
  { title: t('ecomPurchase.plan.qty'), key: 'qty', width: 160 },
  { title: t('ecomPurchase.plan.supplier'), key: 'supplierId', width: 220 },
  { title: t('ecomPurchase.plan.price'), key: 'price', width: 130 },
  { title: t('ecomPurchase.plan.taxRate'), key: 'taxRate', width: 100 },
  { title: t('ecomPurchase.plan.arrivalDate'), key: 'arrivalDate', width: 150 },
  { title: t('common.field.remark'), key: 'remark', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }
]

const emptyForm = () => ({ planDate: dayjs().format('YYYY-MM-DD'), warehouseId: undefined, remark: '', items: [] })
const newRow = () => ({
  materialId: undefined,
  materialLabel: '',
  unit: '',
  suggestQty: null,
  qty: undefined,
  supplierId: undefined,
  supplierName: undefined,
  price: undefined,
  taxRate: undefined,
  arrivalDate: undefined,
  demand: null,
  remark: ''
})
const lines = useLineItems(() => form.value.items, { newRow })

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const d = await getPurchasePlan(record.id)
    return {
      ...d,
      items: (d.items || []).map((i) =>
        lines.withKey({
          id: i.id,
          materialId: i.materialId,
          materialLabel: `${i.materialCode || ''} ${i.materialName || ''}${i.materialSpec ? ` (${i.materialSpec})` : ''}`.trim(),
          unit: i.unit,
          suggestQty: i.suggestQty,
          qty: Number(i.qty),
          supplierId: i.supplierId ?? undefined,
          supplierName: i.supplierName,
          price: i.price != null ? Number(i.price) : undefined,
          taxRate: i.taxRate != null ? Number(i.taxRate) : undefined,
          arrivalDate: i.arrivalDate || undefined,
          demand: i.demand,
          remark: i.remark
        })
      )
    }
  },
  create: addPurchasePlan,
  update: updatePurchasePlan,
  toPayload: (f) => ({
    planDate: f.planDate,
    warehouseId: f.warehouseId ?? null,
    remark: f.remark || null,
    items: f.items.map((i) => ({
      materialId: i.materialId,
      qty: i.qty,
      suggestQty: i.suggestQty ?? null,
      supplierId: i.supplierId ?? null,
      price: i.price ?? null,
      taxRate: i.taxRate ?? null,
      arrivalDate: i.arrivalDate || null,
      demand: i.demand || null,
      remark: i.remark || null
    }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

const totalQty = lines.total('qty', round4)

const rules = { planDate: [{ required: true, message: t('ecomPurchase.plan.selectPlanDate') }] }
const itemRules = [
  {
    validator: () => {
      const items = form.value.items
      if (!items.length) return Promise.reject(t('ecomPurchase.plan.noItems'))
      if (items.some((i) => i.materialId == null)) return Promise.reject(t('ecomPurchase.plan.selectMaterial'))
      if (items.some((i) => !(Number(i.qty) > 0))) return Promise.reject(t('ecomPurchase.plan.inputQty'))
      const ids = items.map((i) => i.materialId)
      if (new Set(ids).size < ids.length) return Promise.reject(t('ecomPurchase.plan.duplicate'))
      return Promise.resolve()
    }
  }
]

function onMaterial(record, m) {
  record.materialLabel = m.label
  record.unit = m.unit
}

const openCreate = () => drawer.openCreate({ ...emptyForm(), items: [lines.withKey(newRow())] })
const openEdit = (record) => drawer.openEdit(record)

defineExpose({ openCreate, openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-top: 2px;
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
