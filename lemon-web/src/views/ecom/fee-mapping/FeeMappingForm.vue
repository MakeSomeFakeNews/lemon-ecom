<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomProfit.fee.add')" :edit-title="$t('ecomProfit.fee.edit')" width="min(520px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomReport.common.platform')" name="platform" :extra="$t('ecomProfit.fee.platformExtra')">
        <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" />
      </a-form-item>
      <a-form-item :label="$t('ecomProfit.fee.rawType')" name="rawType" :extra="$t('ecomProfit.fee.rawTypeExtra')">
        <a-input v-model:value="form.rawType" :maxlength="128" placeholder="FBAPerUnitFulfillmentFee" />
      </a-form-item>
      <a-form-item :label="$t('ecomProfit.fee.itemType')" name="itemType">
        <a-select v-model:value="form.itemType" :options="toOptions(dict.ecom_settlement_item_type)" />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addFeeMapping, updateFeeMapping } from '@/api/ecom/feeMapping'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { EcomPlatform } from '@/constants/enums'

/** 费用映射新增 / 编辑抽屉：openCreate() / openEdit(record)，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_settlement_item_type')

const rules = {
  platform: [{ required: true, message: t('common.rule.required', { name: t('ecomReport.common.platform') }) }],
  rawType: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomProfit.fee.rawType') }) }],
  itemType: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.fee.itemType') }) }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, platform: EcomPlatform.AMAZON, rawType: '', itemType: undefined, remark: '', status: undefined }),
  create: addFeeMapping,
  update: updateFeeMapping,
  toPayload: (f) => ({ platform: f.platform, rawType: f.rawType, itemType: f.itemType, remark: f.remark, status: f.status }),
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>
