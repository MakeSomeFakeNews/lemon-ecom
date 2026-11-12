<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomProfit.tax.add')" :edit-title="$t('ecomProfit.tax.edit')" width="min(520px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.tax.country')" name="country" :extra="$t('ecomProfit.tax.countryExtra')">
            <a-input v-model:value="form.country" :maxlength="8" placeholder="DE" @change="form.country = (form.country || '').toUpperCase()" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomReport.common.marketplace')" name="marketplace" :extra="$t('ecomProfit.tax.marketplaceExtra')">
            <a-select v-model:value="form.marketplace" allow-clear :placeholder="$t('ecomProfit.tax.allMarketplaces')" :options="optionsWithCurrent(dict.ecom_marketplace, form.marketplace)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.tax.taxType')" name="taxType">
            <a-select v-model:value="form.taxType" :options="toOptions(dict.ecom_tax_type)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.tax.rate')" name="rate">
            <a-input-number v-model:value="form.rate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item name="facilitator" :extra="$t('ecomProfit.tax.facilitatorExtra')">
        <a-checkbox :checked="form.facilitator === 1" @change="(e) => (form.facilitator = e.target.checked ? 1 : 0)">
          {{ $t('ecomProfit.tax.facilitator') }}
        </a-checkbox>
      </a-form-item>
      <a-alert type="info" show-icon :message="calcTip" class="calc-tip" />
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addTaxRate, updateTaxRate } from '@/api/ecom/tax'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { TaxType } from '@/constants/enums'

/** 税率新增 / 编辑抽屉：openCreate() / openEdit(record)，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_tax_type', 'ecom_marketplace')

const rules = {
  country: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomProfit.tax.country') }) }],
  taxType: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.tax.taxType') }) }],
  rate: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.tax.rate') }) }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, country: '', marketplace: undefined, taxType: TaxType.VAT, rate: undefined, facilitator: 0, remark: '', status: undefined }),
  create: addTaxRate,
  update: updateTaxRate,
  toPayload: (f) => ({
    country: f.country,
    marketplace: f.marketplace || '',
    taxType: f.taxType,
    rate: f.rate,
    facilitator: f.facilitator ? 1 : 0,
    remark: f.remark,
    status: f.status
  }),
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

/** 按税种与是否代扣说明订单如何拆税 */
const calcTip = computed(() => {
  const inclusive = form.value.taxType !== TaxType.SALES_TAX
  const how = inclusive ? t('ecomProfit.tax.inclusiveTip') : t('ecomProfit.tax.exclusiveTip')
  return `${how} ${form.value.facilitator === 1 ? t('ecomProfit.tax.withheldTip') : t('ecomProfit.tax.declareTip')}`
})

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.calc-tip {
  margin-bottom: 16px;
}
</style>
