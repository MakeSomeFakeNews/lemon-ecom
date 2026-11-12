<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.supplier.add')" :edit-title="$t('scm.supplier.edit')">
    <AiPrefillAlert :ai="ai" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('scm.supplier.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('scm.supplier.codePlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="12">
          <a-form-item :label="$t('scm.supplier.name')" name="name" :class="ai.cls('name')">
            <a-input v-model:value="form.name" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.level')" name="level" :extra="$t('scm.supplier.levelTip')">
            <a-select v-model:value="form.level" allow-clear :options="toOptions(dict.scm_supplier_level)" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.supplier.sectionContact') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.contact')" name="contact">
            <a-input v-model:value="form.contact" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.phone')" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="12">
          <a-form-item :label="$t('common.field.email')" name="email" :class="ai.cls('email')">
            <a-input v-model:value="form.email" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('scm.supplier.address')" name="address">
            <a-input v-model:value="form.address" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.supplier.sectionFinance') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.paymentDays')" name="paymentDays" :extra="$t('scm.supplier.paymentDaysTip')">
            <a-input-number v-model:value="form.paymentDays" :min="0" :max="365" :addon-after="$t('scm.supplier.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.taxNo')" name="taxNo" :class="ai.cls('taxNo')">
            <a-input v-model:value="form.taxNo" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.bankName')" name="bankName">
            <a-input v-model:value="form.bankName" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.supplier.bankAccount')" name="bankAccount">
            <a-input v-model:value="form.bankAccount" :maxlength="64" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.supplier.sectionBusiness') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('scm.supplier.linkedCustomer')" name="customerId" :extra="$t('scm.supplier.linkedCustomerExtra')">
            <PartnerSelect v-model:value="form.customerId" :label="form.customerName" type="customer" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('scm.supplier.type')" name="logistics" :extra="$t('scm.supplier.logisticsExtra')">
            <a-checkbox :checked="form.logistics === 1" @update:checked="(v) => (form.logistics = v ? 1 : 0)">
              {{ $t('scm.supplier.logisticsLabel') }}
            </a-checkbox>
          </a-form-item>
        </a-col>
        <a-col v-if="form.logistics === 1" :span="24">
          <a-form-item :label="$t('scm.supplier.routes')" name="routes">
            <a-textarea
              v-model:value="form.routes"
              :auto-size="{ minRows: 2, maxRows: 6 }"
              :maxlength="1000"
              show-count
              :placeholder="$t('scm.supplier.routesPlaceholder')"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.supplier.sectionOther') }}</div>
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
          </a-form-item>
        </a-col>
      </a-row>
      <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('scm.supplier.extInfo')" :lg="6" />
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import AiPrefillAlert from '@/components/ai/AiPrefillAlert.vue'
import { addPartner, getPartner, updatePartner } from '@/api/scm/partner'
import { useCustomFields } from '@/composables/useCustomFields'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useAiPrefill } from '@/composables/useAiPrefill'
import { toOptions } from '@/utils/dict'

/**
 * 供应商新增 / 编辑（子页）：基本信息、联系方式、结算与开票、业务设置（关联客户、物流公司与营运路线）、备注与自定义字段。
 * openCreate(defaults) / openEdit(record)，applyAi(result) 用智能录入的结果预填；保存后触发 saved
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('scm_supplier_level')
const cf = useCustomFields('scm_supplier')
const cfFields = cf.fields

const rules = {
  code: [
    { required: true, message: t('scm.supplier.codeRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('scm.supplier.codePattern') }
  ],
  name: [{ required: true, whitespace: true, message: t('scm.supplier.nameRequired') }],
  email: [{ type: 'email', message: t('common.rule.email') }]
}
const FIELDS = ['code', 'name', 'level', 'contact', 'phone', 'email', 'address', 'taxNo', 'bankName', 'bankAccount', 'paymentDays', 'logistics', 'routes', 'customerId', 'remark']
const drawer = useFormDrawer(
  cf.wrap({
    emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), paymentDays: 30, logistics: 0 }),
    fetch: (record) => getPartner('supplier', record.id),
    create: (data) => addPartner('supplier', data),
    update: (id, data) => updatePartner('supplier', id, data),
    toPayload: (f) => ({
      ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' ? null : f[k]])),
      logistics: f.logistics === 1 ? 1 : 0,
      routes: f.logistics === 1 ? f.routes || null : null
    }),
    onSuccess: () => emit('saved')
  })
)
const { formRef, form } = drawer
// 智能录入：名片、营业执照等识别结果打开新增表单并预填
const ai = useAiPrefill('supplier', (data) => drawer.openCreate(data), () => drawer.open.value)

defineExpose({ openCreate: (defaults) => drawer.openCreate(defaults), openEdit: drawer.openEdit, applyAi: (result) => ai.apply(result) })
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
.full {
  width: 100%;
}
</style>
