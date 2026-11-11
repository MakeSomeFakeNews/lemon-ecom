<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecom.store.add')" :edit-title="$t('ecom.store.edit')" width="min(760px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('ecom.store.codePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('ecom.store.namePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.platform')" name="platform">
            <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.marketplace')" name="marketplace" :extra="$t('ecom.store.marketplaceExtra')">
            <a-select
              v-model:value="form.marketplace"
              show-search
              option-filter-prop="label"
              :placeholder="$t('common.placeholder.select')"
              :options="optionsWithCurrent(dict.ecom_marketplace, form.marketplace)"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.currency')" name="currency" :extra="$t('ecom.store.currencyExtra')">
            <a-select v-model:value="form.currency" show-search option-filter-prop="label" :options="currencyOptions(currencies)" :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.timezone')" name="timezone" :extra="$t('ecom.store.timezoneExtra')">
            <a-auto-complete v-model:value="form.timezone" :options="timezoneOptions" :filter-option="filterTimezone" :placeholder="$t('ecom.store.timezonePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.warehouse')" name="warehouseId" :extra="$t('ecom.store.warehouseExtra')">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.platformWarehouse')" name="platformWarehouseId" :extra="$t('ecom.store.platformWarehouseExtra')">
            <WarehouseSelect v-model:value="form.platformWarehouseId" :placeholder="$t('ecom.store.platformWarehousePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecom.store.commissionRate')" name="commissionRate" :extra="$t('ecom.store.commissionRateExtra')">
            <a-input-number v-model:value="form.commissionRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8">
          <a-form-item :label="$t('ecom.store.leadDays')" name="leadDays" :extra="$t('ecom.store.leadDaysExtra')">
            <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8">
          <a-form-item :label="$t('ecom.store.safetyDays')" name="safetyDays" :extra="$t('ecom.store.safetyDaysExtra')">
            <a-input-number v-model:value="form.safetyDays" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.owner')" name="ownerId">
            <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecom.store.sellerId')" name="sellerId" :extra="$t('ecom.store.sellerIdExtra')">
            <a-input v-model:value="form.sellerId" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecom.store.credentials')" name="credentials">
            <template #extra>
              <div v-if="credentialHint">{{ $t('ecom.store.credFields', { fields: credentialHint.fields }) }}</div>
              <div v-else>{{ $t('ecom.store.credUnsupported') }}</div>
              <div>{{ $t('ecom.store.credExtra') }}</div>
            </template>
            <a-alert v-if="isEdit && form.hasCredentials" type="success" show-icon class="cred-alert">
              <template #message>
                {{ $t('ecom.store.credConfigured') }}
                <a-button type="link" size="small" danger :loading="clearing" @click="clearCredentials(form.id)">{{ $t('ecom.store.clearCred') }}</a-button>
              </template>
            </a-alert>
            <a-textarea
              v-model:value="form.credentials"
              :rows="4"
              :maxlength="4000"
              class="cred-input"
              :placeholder="credentialHint ? credentialHint.sample : GENERIC_SAMPLE"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addStore, clearStoreCredentials, getStore, updateStore } from '@/api/ecom/store'
import { currencyOptions, useCurrencies } from '@/components/fin/currency'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { EcomPlatform } from '@/constants/enums'

/**
 * 平台店铺新增 / 编辑抽屉：openCreate() / openEdit(record) 打开，保存或清除授权后触发 success。
 * 渠道授权是 JSON 文本，详情只返回 hasCredentials：编辑时留空表示不修改，填写则覆盖，可点「清除授权」
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace')
const currencies = useCurrencies()

/** 各平台渠道授权需要的字段（见后端 StoreCredentials） */
const CREDENTIAL_HINTS = {
  [EcomPlatform.AMAZON]: {
    fields: 'region / marketplaceId / clientId / clientSecret / refreshToken',
    sample: '{"region": "NA", "marketplaceId": "ATVPDKIKX0DER", "clientId": "", "clientSecret": "", "refreshToken": ""}'
  },
  [EcomPlatform.ALIEXPRESS]: {
    fields: 'appKey / appSecret / accessToken',
    sample: '{"appKey": "", "appSecret": "", "accessToken": ""}'
  }
}
const credentialHint = computed(() => CREDENTIAL_HINTS[form.value.platform])
const GENERIC_SAMPLE = '{"key": "value"}'

/** 常见时区，也可以直接输入其他 IANA 时区 */
const TIMEZONES = ['America/Los_Angeles', 'America/New_York', 'America/Chicago', 'America/Toronto', 'America/Mexico_City', 'America/Sao_Paulo',
  'Europe/London', 'Europe/Berlin', 'Europe/Paris', 'Europe/Madrid', 'Europe/Rome', 'Europe/Amsterdam', 'Europe/Warsaw', 'Europe/Moscow',
  'Asia/Dubai', 'Asia/Riyadh', 'Asia/Tokyo', 'Asia/Seoul', 'Asia/Shanghai', 'Asia/Singapore', 'Asia/Bangkok', 'Asia/Jakarta', 'Asia/Manila',
  'Asia/Ho_Chi_Minh', 'Asia/Kuala_Lumpur', 'Australia/Sydney', 'UTC']
const timezoneOptions = TIMEZONES.map((z) => ({ value: z }))
const filterTimezone = (input, option) => option.value.toLowerCase().includes((input || '').toLowerCase())

/** 授权 JSON 必须是对象 */
const validateCredentials = (_rule, value) => {
  const text = (value || '').trim()
  if (!text) return Promise.resolve()
  try {
    const obj = JSON.parse(text)
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) throw new Error('not object')
    return Promise.resolve()
  } catch {
    return Promise.reject(t('ecom.store.credInvalid'))
  }
}

const rules = {
  code: [
    { required: true, message: t('common.rule.required', { name: t('ecom.store.code') }) },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('ecom.store.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecom.store.name') }) }],
  platform: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.store.platform') }) }],
  marketplace: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.store.marketplace') }) }],
  currency: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.store.currency') }) }],
  warehouseId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.store.warehouse') }) }],
  credentials: [{ validator: validateCredentials, trigger: 'blur' }]
}

const FIELDS = ['code', 'name', 'platform', 'marketplace', 'currency', 'timezone', 'warehouseId', 'platformWarehouseId', 'commissionRate',
  'leadDays', 'safetyDays', 'ownerId', 'sellerId', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    ...Object.fromEntries(FIELDS.map((f) => [f, undefined])),
    platform: EcomPlatform.AMAZON,
    credentials: '',
    hasCredentials: false,
    ownerName: undefined
  }),
  // 详情不返回 credentials，文本域留空；负责人名称用列表行带的
  fetch: async (record) => ({ ...(await getStore(record.id)), credentials: '', ownerName: record.ownerName }),
  create: addStore,
  update: updateStore,
  toPayload: (f) => ({
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    credentials: (f.credentials || '').trim() || null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form, isEdit } = drawer

const clearing = ref(false)
const clearCredentials = useAction(clearStoreCredentials, {
  confirm: () => ({ title: t('ecom.store.clearCredTitle'), content: t('ecom.store.clearCredContent'), okType: 'danger' }),
  successKey: 'ecom.store.credCleared',
  loading: clearing,
  reload: () => {
    form.value.hasCredentials = false
    emit('success')
  }
})

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.cred-alert {
  margin-bottom: 8px;
}
.cred-input {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
</style>
