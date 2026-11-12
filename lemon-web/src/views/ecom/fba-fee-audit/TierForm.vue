<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomFba.tier.add')" :edit-title="$t('ecomFba.tier.editTitle', { code: form.code || '' })">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title section-gap">{{ $t('ecomFba.tier.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomFba.tier.marketplace')" name="marketplace">
            <a-select v-model:value="form.marketplace" :options="toOptions(dict.ecom_marketplace)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomFba.tier.code')" name="code"><a-input v-model:value="form.code" :maxlength="32" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="7">
          <a-form-item :label="$t('ecomFba.tier.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="3">
          <a-form-item :label="$t('ecomFba.tier.sizeCategory')" name="sizeCategory">
            <a-select v-model:value="form.sizeCategory" :options="toOptions(dict.ecom_fba_size_category)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="3">
          <a-form-item :label="$t('ecomFba.tier.unitSystem')" name="unitSystem">
            <a-select v-model:value="form.unitSystem" :options="unitOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="3">
          <a-form-item :label="$t('ecomFba.tier.sort')"><a-input-number v-model:value="form.sort" :min="0" class="full" /></a-form-item>
        </a-col>
      </a-row>

      <div class="section-title section-gap">{{ $t('ecomFba.tier.sectionLimits') }}</div>
      <div class="limits">
        <a-form-item :label="$t('ecomFba.tier.dimDivisor')" :tooltip="$t('ecomFba.tier.dimDivisorTip')">
          <a-input-number v-model:value="form.dimDivisor" :min="0" class="full" />
        </a-form-item>
        <a-form-item v-for="k in LIMITS" :key="k" :label="`${$t(`ecomFba.tier.${k}`)} (${unitLabel(k)})`">
          <a-input-number v-model:value="form[k]" :min="0" class="full" />
        </a-form-item>
      </div>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomFba.tier.rates') }}</span>
        <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
      </div>
      <a-form-item name="rates" :rules="rateRules" class="items-item">
        <a-form-item-rest>
        <LineItemsTable :items="form.rates" :columns="rateColumns" :min-rows="0">
          <template #cell="{ column, record }">
            <a-input v-if="column.key === 'currency'" v-model:value="record.currency" :maxlength="8" />
            <a-input-number v-else v-model:value="record[column.key]" :min="0" :precision="column.key.includes('ee') ? 2 : 3" class="full" />
          </template>
        </LineItemsTable>
        </a-form-item-rest>
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import { addTier, updateTier } from '@/api/ecom/fba'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useLineItems } from '@/composables/useLineItems'
import { toOptions } from '@/utils/dict'
import { FbaSizeCategory, FbaUnitSystem } from '@/constants/enums'
import { t } from '@/locales'

/** 尺寸分段与重量段费率表单（子页）：ref 调用 openCreate(marketplace?)、openEdit(tier)；@success */
const emit = defineEmits(['success'])
const dict = useDict('ecom_marketplace', 'ecom_fba_size_category')
const LIMITS = ['maxLong', 'maxMedian', 'maxShort', 'maxGirth', 'maxSidesSum', 'maxWeight']
const unitOptions = Object.values(FbaUnitSystem).map((v) => ({ value: v, label: t(`ecomFba.unitSystem.${v}`) }))
const unitLabel = (k) => {
  const imperial = form.value.unitSystem !== FbaUnitSystem.METRIC
  if (k === 'maxWeight') return imperial ? 'lb' : 'kg'
  return imperial ? 'in' : 'cm'
}
const rateColumns = [
  { title: t('ecomFba.tier.weightFrom'), key: 'weightFrom', width: 120 },
  { title: t('ecomFba.tier.weightTo'), key: 'weightTo', width: 120 },
  { title: t('ecomFba.tier.fee'), key: 'fee', width: 110 },
  { title: t('ecomFba.tier.extraFrom'), key: 'extraFrom', width: 110 },
  { title: t('ecomFba.tier.extraUnit'), key: 'extraUnit', width: 110 },
  { title: t('ecomFba.tier.extraFee'), key: 'extraFee', width: 110 },
  { title: t('ecomFba.common.currency'), key: 'currency', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }
]
const emptyForm = () => ({
  marketplace: 'US',
  code: '',
  name: '',
  sizeCategory: FbaSizeCategory.STANDARD,
  unitSystem: FbaUnitSystem.IMPERIAL,
  sort: 1,
  dimDivisor: undefined,
  maxLong: undefined,
  maxMedian: undefined,
  maxShort: undefined,
  maxGirth: undefined,
  maxSidesSum: undefined,
  maxWeight: undefined,
  rates: []
})
const lines = useLineItems(() => form.value.rates, {
  field: 'rates',
  newRow: (arr) => ({ weightFrom: arr.length ? arr[arr.length - 1].weightTo ?? 0 : 0, weightTo: undefined, fee: undefined, currency: arr[0]?.currency || 'USD' })
})
const num = (v) => (v == null || v === '' ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => ({
    ...record,
    ...Object.fromEntries(LIMITS.map((k) => [k, num(record[k])])),
    dimDivisor: num(record.dimDivisor),
    rates: (record.rates || []).map((r) =>
      lines.withKey({
        id: r.id,
        weightFrom: num(r.weightFrom),
        weightTo: num(r.weightTo),
        fee: num(r.fee),
        extraFrom: num(r.extraFrom),
        extraUnit: num(r.extraUnit),
        extraFee: num(r.extraFee),
        currency: r.currency
      })
    )
  }),
  create: addTier,
  update: updateTier,
  toPayload: (f) => ({
    marketplace: f.marketplace,
    code: f.code,
    name: f.name,
    sizeCategory: f.sizeCategory,
    unitSystem: f.unitSystem,
    sort: f.sort ?? 0,
    dimDivisor: f.dimDivisor ?? null,
    ...Object.fromEntries(LIMITS.map((k) => [k, f[k] ?? null])),
    rates: f.rates.map((r) => ({
      weightFrom: r.weightFrom ?? 0,
      weightTo: r.weightTo ?? null,
      fee: r.fee,
      extraFrom: r.extraFrom ?? null,
      extraUnit: r.extraUnit ?? null,
      extraFee: r.extraFee ?? null,
      currency: r.currency || 'USD'
    }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const rules = {
  marketplace: [{ required: true, message: t('ecomFba.tier.marketplaceRequired') }],
  code: [{ required: true, whitespace: true, message: t('ecomFba.tier.codeRequired') }],
  name: [{ required: true, whitespace: true, message: t('ecomFba.tier.nameRequired') }]
}
const rateRules = [
  {
    validator: () => (form.value.rates.some((r) => r.weightFrom == null || r.fee == null) ? Promise.reject(t('ecomFba.tier.rateInvalid')) : Promise.resolve())
  }
]

const openCreate = (marketplace) => drawer.openCreate({ ...emptyForm(), marketplace: marketplace || 'US' })
const openEdit = (tier) => drawer.openEdit(tier)
defineExpose({ openCreate, openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
}
.section-gap {
  margin: 4px 0 12px;
}
.limits {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(136px, 1fr));
  column-gap: 16px;
  margin-bottom: 8px;
}
.items-item :deep(.ant-form-item-control-input) {
  min-height: 0;
}
</style>
