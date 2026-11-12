<template>
  <FormDrawer :drawer="drawer" :edit-title="$t('ecomOrder.order.editTitle')" :create-title="$t('ecomOrder.order.editTitle')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.shipName')" name="shipName">
            <a-input v-model:value="form.shipName" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.shipPhone')" name="shipPhone">
            <a-input v-model:value="form.shipPhone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.shipCountry')" name="shipCountry">
            <a-input v-model:value="form.shipCountry" :maxlength="8" :placeholder="$t('ecomOrder.order.countryPh')" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.shipState')" name="shipState">
            <a-input v-model:value="form.shipState" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.shipCity')" name="shipCity">
            <a-input v-model:value="form.shipCity" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.shipPostcode')" name="shipPostcode">
            <a-input v-model:value="form.shipPostcode" :maxlength="16" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomOrder.order.shipAddress')" name="shipAddress">
            <a-textarea v-model:value="form.shipAddress" :maxlength="255" :auto-size="{ minRows: 2, maxRows: 4 }" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.order.fulfillment')" name="fulfillment">
            <a-select v-model:value="form.fulfillment" :options="toOptions(dict.ecom_fulfillment)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrder.ops.weight')" name="weight">
            <a-input-number v-model:value="form.weight" :min="0" :precision="3" :addon-after="$t('ecomOrder.common.kg')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :maxlength="255" :auto-size="{ minRows: 2, maxRows: 4 }" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { getEcomOrder, updateEcomOrder } from '@/api/ecom/order'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'

/** 修改平台订单的收件信息、配送方式、预估重量、备注（未打单、未发货时，PUT /ecom/order/{id}）；ref 调用 open(order) */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_fulfillment')

const FIELDS = ['shipName', 'shipPhone', 'shipCountry', 'shipState', 'shipCity', 'shipPostcode', 'shipAddress', 'fulfillment', 'weight', 'remark']
const emptyForm = () => Object.fromEntries(FIELDS.map((k) => [k, undefined]))
const rules = {
  shipCountry: [{ max: 8, message: t('common.rule.maxLength', { name: t('ecomOrder.order.shipCountry'), max: 8 }) }],
  shipAddress: [{ max: 255, message: t('common.rule.maxLength', { name: t('ecomOrder.order.shipAddress'), max: 255 }) }]
}

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const d = await getEcomOrder(record.id)
    return { ...d, weight: d.weight == null ? undefined : Number(d.weight) }
  },
  create: () => Promise.reject(new Error('unsupported')),
  update: updateEcomOrder,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' ? null : f[k]])),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ open: (order) => drawer.openEdit(order) })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
