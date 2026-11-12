<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomOwh.tr.add')" :edit-title="$t('ecomOwh.tr.edit')" width="min(880px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.tr.from')" name="fromWarehouseId" :extra="ownerText(form.fromWarehouseId)">
            <WarehouseSelect v-model:value="form.fromWarehouseId" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.tr.to')" name="toWarehouseId" :extra="ownerText(form.toWarehouseId)">
            <WarehouseSelect v-model:value="form.toWarehouseId" />
          </a-form-item>
        </a-col>
        <a-col v-if="fromMapping" :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.tr.product')" name="productCode" :extra="$t('ecomOwh.tr.productExtra')">
            <a-select
              v-model:value="form.productCode"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="products.map((p) => ({ value: p.productCode, label: `${p.productName || ''} (${p.productCode})` }))"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="fromMapping ? 12 : 24">
          <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
      <div class="items-head">
        <span class="section-title">{{ $t('ecomOwh.tr.items') }}</span>
        <a-button size="small" class="add-row" @click="form.items.push({ key: ++seq, materialId: undefined, qty: undefined })">
          <template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}
        </a-button>
      </div>
      <a-table :row-key="(r) => r.key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <MaterialSelect v-model:value="record.materialId" :label="record.label" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0" :precision="0" class="full" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a v-if="form.items.length > 1" class="danger" @click="form.items.splice(index, 1)"><DeleteOutlined /></a>
          </template>
        </template>
      </a-table>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addOwhTransfer, getOwhTransfer, owhAllMappings, owhProducts, updateOwhTransfer } from '@/api/ecom/owh'
import { useFormDrawer } from '@/composables/useFormDrawer'

/** 新建 / 修改海外仓调拨单（草稿）：调出、调入仓至少一个是海外仓；调出仓是海外仓时选择物流产品。openCreate() / openEdit(record) */
const emit = defineEmits(['success'])
const { t } = useI18n()
let seq = 0

const rules = {
  fromWarehouseId: [{ required: true, message: t('common.rule.required', { name: t('ecomOwh.tr.from') }) }],
  toWarehouseId: [{ required: true, message: t('common.rule.required', { name: t('ecomOwh.tr.to') }) }]
}
const itemColumns = [
  { title: t('ecomOwh.stock.material'), key: 'material', width: 360 },
  { title: t('ecomOwh.tr.qty'), key: 'qty', width: 140 },
  { title: '', key: 'action', width: 50 }
]

const mappings = ref([])
const products = ref([])
const mappingOf = (wh) => mappings.value.find((m) => m.warehouseId === wh)
const ownerText = (wh) => {
  const m = mappingOf(wh)
  return wh ? (m ? t('ecomOwh.tr.overseas', { name: m.accountName, code: m.owhCode }) : t('ecomOwh.tr.local')) : ''
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, fromWarehouseId: undefined, toWarehouseId: undefined, productCode: undefined, remark: '', items: [{ key: ++seq, materialId: undefined, qty: undefined }] }),
  beforeOpen: async () => {
    if (!mappings.value.length) mappings.value = (await owhAllMappings()) || []
  },
  fetch: async (record) => {
    const d = await getOwhTransfer(record.id)
    return { ...d, items: d.items.map((i) => ({ key: ++seq, materialId: i.materialId, label: `${i.materialCode} ${i.materialName || ''}`, qty: Number(i.qty) })) }
  },
  create: addOwhTransfer,
  update: updateOwhTransfer,
  toPayload: (f) => {
    const items = f.items.filter((i) => i.materialId && i.qty > 0).map((i) => ({ materialId: i.materialId, qty: i.qty }))
    if (!items.length) {
      message.warning(t('ecomOwh.tr.itemsRequired'))
      throw new Error('items')
    }
    return { fromWarehouseId: f.fromWarehouseId, toWarehouseId: f.toWarehouseId, productCode: f.productCode || null, remark: f.remark || null, items }
  },
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const fromMapping = computed(() => mappingOf(form.value.fromWarehouseId))
watch(fromMapping, async (m) => {
  products.value = m ? ((await owhProducts(m.accountId)) || []).filter((p) => p.status === 1) : []
})

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.items-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.add-row {
  margin-left: auto;
}
.full {
  width: 100%;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
