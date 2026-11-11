<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecom.logistics.add')" :edit-title="$t('ecom.logistics.edit')" width="min(1080px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.logistics.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('ecom.logistics.codePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.logistics.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('ecom.logistics.namePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.logistics.provider')" name="provider">
            <a-input v-model:value="form.provider" :maxlength="64" :placeholder="$t('ecom.logistics.providerPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.logistics.transportMode')" name="transportMode">
            <a-select v-model:value="form.transportMode" allow-clear :options="optionsWithCurrent(dict.ecom_transport_mode, form.transportMode)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecom.logistics.supplier')" name="supplierId" :extra="$t('ecom.logistics.supplierExtra')">
            <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierName" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecom.logistics.daysMin')" name="daysMin">
            <a-input-number v-model:value="form.daysMin" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecom.logistics.daysMax')" name="daysMax">
            <a-input-number v-model:value="form.daysMax" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('ecom.logistics.rates') }}</span>
        <span class="sub-text">{{ $t('ecom.logistics.ratesTip') }}</span>
        <a-button size="small" class="add-row" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
      </div>
      <LineItemsTable :items="form.rates" :columns="rateColumns" :empty-text="$t('ecom.logistics.noRates')">
        <template #cell="{ column, record, index }">
          <template v-if="column.key === 'country'">
            <a-form-item :name="['rates', index, 'country']" :rules="rateRules.country" class="cell-item">
              <a-input v-model:value="record.country" :maxlength="8" placeholder="*" @change="record.country = (record.country || '').toUpperCase()" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'minWeight'">
            <a-form-item :name="['rates', index, 'minWeight']" :rules="rateRules.minWeight" class="cell-item">
              <a-input-number v-model:value="record.minWeight" :min="0" :precision="3" class="full" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'maxWeight'">
            <a-form-item :name="['rates', index, 'maxWeight']" :rules="maxWeightRules(record)" class="cell-item">
              <a-input-number v-model:value="record.maxWeight" :min="0" :precision="3" :placeholder="$t('ecom.logistics.unlimited')" class="full" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'baseFee'">
            <a-input-number v-model:value="record.baseFee" :min="0" :precision="2" class="full" />
          </template>
          <template v-else-if="column.key === 'perKg'">
            <a-input-number v-model:value="record.perKg" :min="0" :precision="2" class="full" />
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
        </template>
      </LineItemsTable>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addLogistics, getLogistics, updateLogistics } from '@/api/ecom/logistics'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { optionsWithCurrent } from '@/utils/dict'

/**
 * 物流渠道新增 / 编辑（子页）：渠道信息 + 费率明细行（国家 × 重量段），保存时整体提交 rates。
 * openCreate() / openEdit(record) 打开，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_transport_mode')

const rules = {
  code: [
    { required: true, message: t('common.rule.required', { name: t('ecom.logistics.code') }) },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('ecom.logistics.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecom.logistics.name') }) }],
  daysMax: [
    {
      validator: (_r, v) => (v != null && form.value.daysMin != null && v < form.value.daysMin ? Promise.reject(t('ecom.logistics.daysMaxRule')) : Promise.resolve())
    }
  ]
}
const rateRules = {
  country: [{ required: true, whitespace: true, message: t('ecom.logistics.countryRequired') }],
  minWeight: [{ required: true, message: t('ecom.logistics.minWeightRequired') }]
}
/** 上限为空不限，填写时必须大于下限 */
const maxWeightRules = (record) => [
  {
    validator: (_r, v) =>
      v != null && record.minWeight != null && Number(v) <= Number(record.minWeight) ? Promise.reject(t('ecom.logistics.maxWeightRule')) : Promise.resolve()
  }
]

const rateColumns = [
  { title: t('ecom.logistics.country'), key: 'country', width: 120 },
  { title: t('ecom.logistics.minWeight'), key: 'minWeight', width: 130 },
  { title: t('ecom.logistics.maxWeight'), key: 'maxWeight', width: 130 },
  { title: t('ecom.logistics.baseFee'), key: 'baseFee', width: 130 },
  { title: t('ecom.logistics.perKg'), key: 'perKg', width: 130 },
  { title: t('common.field.remark'), key: 'remark', width: 200 },
  { title: '', key: 'action', width: 50 }
]

const newRate = (rates) => {
  const last = rates[rates.length - 1]
  // 新行默认接在上一段重量之后，国家与上一行相同
  return { country: last?.country || '*', minWeight: last?.maxWeight ?? 0, maxWeight: undefined, baseFee: undefined, perKg: undefined, remark: '' }
}
const lines = useLineItems(() => form.value.rates, { newRow: newRate, field: 'rates' })

const FIELDS = ['code', 'name', 'provider', 'transportMode', 'supplierId', 'daysMin', 'daysMax', 'remark']
const num = (v) => (v == null || v === '' ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), supplierName: undefined, rates: [] }),
  fetch: async (record) => {
    const d = await getLogistics(record.id)
    return {
      ...d,
      supplierName: d.supplierName || record.supplierName,
      rates: (d.rates || []).map((r) =>
        lines.withKey({ country: r.country, minWeight: num(r.minWeight), maxWeight: num(r.maxWeight), baseFee: num(r.baseFee), perKg: num(r.perKg), remark: r.remark || '' })
      )
    }
  },
  create: addLogistics,
  update: updateLogistics,
  toPayload: (f) => ({
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    // 明细整体替换，不带 id
    rates: f.rates.map((r) => ({
      country: (r.country || '*').trim().toUpperCase(),
      minWeight: r.minWeight ?? 0,
      maxWeight: r.maxWeight ?? null,
      baseFee: r.baseFee ?? 0,
      perKg: r.perKg ?? 0,
      remark: r.remark || null
    }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.add-row {
  margin-left: auto;
}
.cell-item {
  margin-bottom: 0;
}
</style>
