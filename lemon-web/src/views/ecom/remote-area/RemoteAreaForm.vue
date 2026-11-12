<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomOrd.remote.add')" :edit-title="$t('ecomOrd.remote.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomOrder.order.country')" name="country">
        <a-input v-model:value="form.country" :maxlength="8" :placeholder="$t('ecomOrder.order.countryPh')" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrd.remote.from')" name="postcodeFrom">
            <a-input v-model:value="form.postcodeFrom" :maxlength="16" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrd.remote.to')" name="postcodeTo" :extra="$t('ecomOrd.remote.toHint')">
            <a-input v-model:value="form.postcodeTo" :maxlength="16" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('ecomOrd.remote.name')" name="name">
        <a-input v-model:value="form.name" :maxlength="64" />
      </a-form-item>
      <a-form-item :label="$t('common.field.status')" name="status">
        <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
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
import { addRemoteArea, updateRemoteArea } from '@/api/ecom/orderEnhance'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

/** 偏远地区邮编段新增 / 编辑：国家、邮编起止（止为空表示按起前缀匹配）、名称 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const required = (key) => [{ required: true, whitespace: true, message: t('common.rule.required', { name: t(key) }) }]
const rules = { country: required('ecomOrder.order.country'), postcodeFrom: required('ecomOrd.remote.from') }

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, country: '', postcodeFrom: '', postcodeTo: '', name: '', status: STATUS_ENABLED, remark: '' }),
  create: addRemoteArea,
  update: updateRemoteArea,
  toPayload: (f) => ({ country: f.country.trim().toUpperCase(), postcodeFrom: f.postcodeFrom.trim(), postcodeTo: f.postcodeTo?.trim() || null,
    name: f.name || null, status: f.status, remark: f.remark || null }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>
