<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomDev.account.add')" :edit-title="$t('ecomDev.account.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomDev.account.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomDev.account.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('common.field.status')">
            <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomDev.account.default')">
            <a-switch :checked="form.isDefault === 1" @change="(v) => (form.isDefault = v ? 1 : 0)" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomDev.account.sectionApp') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="AppKey" name="appKey"><a-input v-model:value="form.appKey" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="AppSecret" :extra="form.secretSet ? $t('ecomDev.account.secretSet') : ''">
            <a-input-password v-model:value="form.appSecret" :maxlength="128" autocomplete="new-password" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomDev.account.apiUrl')" :extra="$t('ecomDev.account.apiUrlExtra')">
            <a-input v-model:value="form.apiUrl" :maxlength="255" placeholder="https://gw.open.1688.com/openapi" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomDev.account.receiver') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomDev.account.receiverName')"><a-input v-model:value="form.receiverName" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomDev.account.receiverMobile')"><a-input v-model:value="form.receiverMobile" :maxlength="32" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomDev.account.postCode')"><a-input v-model:value="form.postCode" :maxlength="16" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6"><a-form-item :label="$t('ecomDev.account.province')"><a-input v-model:value="form.province" :maxlength="32" /></a-form-item></a-col>
        <a-col :xs="12" :sm="6"><a-form-item :label="$t('ecomDev.account.city')"><a-input v-model:value="form.city" :maxlength="32" /></a-form-item></a-col>
        <a-col :xs="12" :sm="6"><a-form-item :label="$t('ecomDev.account.district')"><a-input v-model:value="form.district" :maxlength="32" /></a-form-item></a-col>
        <a-col :xs="12" :sm="6"><a-form-item :label="$t('ecomDev.account.town')"><a-input v-model:value="form.town" :maxlength="32" /></a-form-item></a-col>
        <a-col :span="24"><a-form-item :label="$t('ecomDev.account.address')"><a-input v-model:value="form.address" :maxlength="255" /></a-form-item></a-col>
      </a-row>

      <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addAli1688Account, getAli1688Account, updateAli1688Account } from '@/api/ecom/dev'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'

/**
 * 1688 账号新增 / 编辑（子页）：AppKey / AppSecret（加密保存，留空表示不修改）、接口地址、默认收货地址。
 * openCreate(defaults) / openEdit(record)，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('sys_common_status')

const FIELDS = ['name', 'appKey', 'appSecret', 'apiUrl', 'receiverName', 'receiverMobile', 'province', 'city', 'district', 'town', 'address', 'postCode',
  'isDefault', 'status', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ name: '', appKey: '', appSecret: '', status: STATUS_ENABLED, isDefault: 0 }),
  fetch: async (r) => ({ ...(await getAli1688Account(r.id)), appSecret: '' }),
  create: addAli1688Account,
  update: updateAli1688Account,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const rules = {
  name: [{ required: true, message: t('ecomDev.account.nameRequired') }],
  appKey: [{ required: true, message: t('ecomDev.account.appKeyRequired') }]
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
</style>
