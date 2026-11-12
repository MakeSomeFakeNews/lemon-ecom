<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomPublish.price.add')" :edit-title="$t('ecomPublish.price.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomPublish.item.secBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.price.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.common.platform')" name="platform">
            <a-select v-model:value="form.platform" allow-clear :options="toOptions(dict.ecom_platform)" :placeholder="$t('ecomPublish.price.anyPlatform')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.price.params') }}</div>
      <a-alert type="info" show-icon class="block" :message="$t('ecomPublish.price.formula')" />
      <a-row :gutter="16">
        <a-col v-for="f in fields" :key="f.key" :xs="12" :sm="6" :lg="6" :xl="3">
          <a-form-item :label="$t(`ecomPublish.price.${f.key}`)" :name="f.key" :extra="f.extra ? $t(f.extra) : undefined">
            <a-input-number v-model:value="form[f.key]" :min="f.min ?? 0" :max="f.max" :precision="f.precision ?? 2" class="full" :addon-after="f.unit" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.price.calculator') }}</div>
      <a-row :gutter="16" align="bottom">
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.price.calcCurrency')">
            <a-select v-model:value="calc.currency" :options="currencyOptions" show-search />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.price.calcCost')"><a-input-number v-model:value="calc.cost" :min="0" :precision="2" class="full" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.price.calcPrice')"><a-input-number v-model:value="calc.price" :min="0" :precision="2" class="full" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item><a-button :loading="calcLoading" :disabled="!form.id" @click="runCalc">{{ $t('ecomPublish.price.calc') }}</a-button></a-form-item>
        </a-col>
      </a-row>
      <div v-if="!form.id" class="sub-text">{{ $t('ecomPublish.price.calcSaveFirst') }}</div>
      <a-descriptions v-if="quote" size="small" bordered :column="{ xs: 1, sm: 2, lg: 4 }" class="block">
        <a-descriptions-item :label="$t('ecomPublish.price.rate')">{{ quote.rate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('ecomPublish.price.suggested')"><b>{{ quote.currency }} {{ price(quote.suggestedPrice) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('ecomPublish.price.fees')">{{ money(quote.fees) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('ecomPublish.price.profit')">
          <span :class="quote.profit < 0 ? 'loss' : 'gain'">{{ money(quote.profit) }}（{{ quote.margin }}%）</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { addPriceTemplate, getPriceTemplate, quotePrice, updatePriceTemplate } from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { money, price } from '@/utils/number'

/**
 * 价格模板新增 / 编辑，带定价试算：选择币种、填写成本（本位币）可得建议价，填写售价可得该售价下的利润与利润率（保存后试算，按当前模板参数）
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform')
const stores = useStores()
const fields = [
  { key: 'firstLegCost', extra: 'ecomPublish.price.baseCurrency' },
  { key: 'otherCost', extra: 'ecomPublish.price.baseCurrency' },
  { key: 'commissionRate', unit: '%', max: 99 },
  { key: 'otherRate', unit: '%', max: 99 },
  { key: 'fixedFee', extra: 'ecomPublish.price.storeCurrency' },
  { key: 'targetMargin', unit: '%', min: -50, max: 90 },
  { key: 'priceTail', max: 0.99, extra: 'ecomPublish.price.priceTailExtra' },
  { key: 'minPrice', extra: 'ecomPublish.price.storeCurrency' }
]
const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomPublish.price.name') }) }],
  firstLegCost: [{ required: true, type: 'number', message: t('common.rule.required', { name: t('ecomPublish.price.firstLegCost') }) }],
  otherCost: [{ required: true, type: 'number', message: t('common.rule.required', { name: t('ecomPublish.price.otherCost') }) }],
  commissionRate: [{ required: true, type: 'number', message: t('common.rule.required', { name: t('ecomPublish.price.commissionRate') }) }],
  otherRate: [{ required: true, type: 'number', message: t('common.rule.required', { name: t('ecomPublish.price.otherRate') }) }],
  fixedFee: [{ required: true, type: 'number', message: t('common.rule.required', { name: t('ecomPublish.price.fixedFee') }) }],
  targetMargin: [{ required: true, type: 'number', message: t('common.rule.required', { name: t('ecomPublish.price.targetMargin') }) }]
}
const FIELDS = ['name', 'platform', 'firstLegCost', 'otherCost', 'commissionRate', 'otherRate', 'fixedFee', 'targetMargin', 'priceTail', 'minPrice', 'status', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', platform: undefined, firstLegCost: 0, otherCost: 0, commissionRate: 15, otherRate: 0, fixedFee: 0, targetMargin: 20,
    priceTail: 0.99, minPrice: undefined, status: STATUS_ENABLED, remark: '' }),
  fetch: (r) => getPriceTemplate(r.id),
  create: addPriceTemplate,
  update: updatePriceTemplate,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

const currencyOptions = computed(() => [...new Set(['USD', ...stores.value.map((s) => s.currency).filter(Boolean)])].map((c) => ({ value: c, label: c })))
const calc = ref({ currency: 'USD', cost: 30, price: undefined })
const quote = ref(null)
const calcLoading = ref(false)
async function runCalc() {
  calcLoading.value = true
  try {
    quote.value = await quotePrice({ priceTemplateId: form.value.id, currency: calc.value.currency, cost: calc.value.cost, price: calc.value.price })
  } finally {
    calcLoading.value = false
  }
}

defineExpose({
  openCreate: (d) => {
    quote.value = null
    return drawer.openCreate(d)
  },
  openEdit: (r) => {
    quote.value = null
    return drawer.openEdit(r)
  }
})
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
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
  margin-bottom: 8px;
}
</style>
