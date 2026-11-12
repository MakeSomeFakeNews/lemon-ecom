<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.customer.add')" :edit-title="$t('scm.customer.edit')">
    <AiPrefillAlert :ai="ai" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('scm.customer.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('scm.customer.codePlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="12">
          <a-form-item :label="$t('scm.customer.name')" name="name" :class="ai.cls('name')">
            <a-input v-model:value="form.name" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.level')" name="level">
            <a-select v-model:value="form.level" allow-clear :options="toOptions(dict.scm_customer_level)" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.customer.sectionSales') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.salesChannel')" name="channel" :extra="$t('scm.customer.channelTip')">
            <a-radio-group v-model:value="form.channel">
              <a-radio v-for="o in dict.scm_channel" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.owner')" name="ownerId" :extra="$t('scm.customer.ownerTip')">
            <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.region')" name="region" :extra="$t('scm.customer.regionTip')">
            <a-select
              v-model:value="form.region"
              allow-clear
              show-search
              option-filter-prop="label"
              :placeholder="$t('scm.customer.regionPlaceholder')"
              :options="optionsWithCurrent(dict.scm_region, form.region)"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('pricing.level.priceLevel')" name="priceLevel" :extra="$t('pricing.level.priceLevelExtra')">
            <a-select v-model:value="form.priceLevel" :options="priceLevelOptions" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.customer.sectionFinance') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.paymentDays')" name="paymentDays" :extra="$t('scm.customer.paymentDaysTip')">
            <a-input-number v-model:value="form.paymentDays" :min="0" :max="365" :addon-after="$t('scm.customer.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.creditLimit')" name="creditLimit" :extra="$t('scm.customer.creditLimitTip')">
            <a-input-number v-model:value="form.creditLimit" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.taxNo')" name="taxNo" :class="ai.cls('taxNo')">
            <a-input v-model:value="form.taxNo" :maxlength="32" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.customer.sectionContact') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.contact')" name="contact">
            <a-input v-model:value="form.contact" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.customer.phone')" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('scm.customer.shipAddress')" name="address">
            <a-input v-model:value="form.address" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('scm.customer.sectionOther') }}</div>
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
          </a-form-item>
        </a-col>
      </a-row>
      <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('scm.customer.extInfo')" :lg="6" />
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import AiPrefillAlert from '@/components/ai/AiPrefillAlert.vue'
import { usePriceLevels } from '@/components/wms/priceLevels'
import { addPartner, getPartner, updatePartner } from '@/api/scm/partner'
import { useCustomFields } from '@/composables/useCustomFields'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useAiPrefill } from '@/composables/useAiPrefill'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { STATUS_ENABLED } from '@/utils/constants'

/**
 * 客户新增 / 编辑（子页）：基本信息、销售设置（渠道、负责销售、区域、价格等级）、结算与信用、联系方式、备注与自定义字段。
 * openCreate(defaults) / openEdit(record)，applyAi(result) 用智能录入的结果预填；保存后触发 saved
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('scm_customer_level', 'scm_channel', 'scm_region')
const cf = useCustomFields('scm_customer')
const cfFields = cf.fields

// 价格等级（启用的价格列；当前值所在的列已停用时仍列出）
const { levels } = usePriceLevels()
const priceLevelOptions = computed(() =>
  levels.value.filter((l) => l.status === STATUS_ENABLED || l.level === (form.value.priceLevel ?? 0)).map((l) => ({ value: l.level, label: l.name }))
)

const rules = {
  code: [
    { required: true, message: t('scm.customer.codeRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('scm.customer.codePattern') }
  ],
  name: [{ required: true, whitespace: true, message: t('scm.customer.nameRequired') }]
}
const FIELDS = ['code', 'name', 'level', 'priceLevel', 'contact', 'phone', 'address', 'taxNo', 'creditLimit', 'paymentDays', 'remark', 'channel', 'region',
  'ownerId']
const drawer = useFormDrawer(
  cf.wrap({
    emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), paymentDays: 30, channel: 'DIRECT', priceLevel: 0 }),
    fetch: async (record) => ({ ...(await getPartner('customer', record.id)), ownerName: record.ownerName }),
    create: (data) => addPartner('customer', data),
    update: (id, data) => updatePartner('customer', id, data),
    toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    onSuccess: () => emit('saved')
  })
)
const { formRef, form } = drawer
// 智能录入：名片、营业执照等识别结果打开新增表单并预填
const ai = useAiPrefill('customer', (data) => drawer.openCreate(data), () => drawer.open.value)

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
