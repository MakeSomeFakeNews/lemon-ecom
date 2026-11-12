<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecom.store.add')" :edit-title="$t('ecom.store.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecom.store.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('ecom.store.codePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('ecom.store.namePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.platform')" name="platform">
            <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.owner')" name="ownerId">
            <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.marketplace')" name="marketplace" :extra="$t('ecom.store.marketplaceExtra')">
            <a-select
              v-model:value="form.marketplace"
              show-search
              option-filter-prop="label"
              :placeholder="$t('common.placeholder.select')"
              :options="optionsWithCurrent(dict.ecom_marketplace, form.marketplace)"
              @change="onMarketplace"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.currency')" name="currency" :extra="$t('ecom.store.currencyExtra')">
            <a-select v-model:value="form.currency" show-search option-filter-prop="label" :options="currencyOptions(currencies)" :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.timezone')" name="timezone" :extra="$t('ecom.store.timezoneExtra')">
            <a-auto-complete v-model:value="form.timezone" :options="timezoneOptions" :filter-option="filterTimezone" :placeholder="$t('ecom.store.timezonePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.sellerId')" name="sellerId" :extra="$t('ecom.store.sellerIdExtra')">
            <a-input v-model:value="form.sellerId" :maxlength="64" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecom.hub.form.authSection') }}</div>
      <template v-if="connector">
        <div class="connector-meta">
          <div class="meta-item">
            <span class="meta-label">{{ $t('ecom.hub.form.authType') }}</span>
            <span>
              <a-tag color="blue" :bordered="false">{{ authTypeName(connector.authType) }}</a-tag>
              <span class="sub-text">{{ authTypeTip(connector.authType) }}</span>
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">{{ $t('ecom.hub.form.capabilities') }}</span>
            <span>
              <a-tag v-for="c in connector.capabilities" :key="c" :bordered="false">{{ capLabel(c) }}</a-tag>
            </span>
          </div>
          <div v-if="connector.docUrl" class="meta-item">
            <span class="meta-label">{{ $t('ecom.hub.form.docs') }}</span>
            <a class="meta-link" :href="connector.docUrl" target="_blank" rel="noopener noreferrer">{{ connector.docUrl }}</a>
          </div>
          <div v-if="connector.perStoreBaseUrl" class="meta-item">
            <span class="meta-label">{{ $t('ecom.hub.form.perStoreBaseUrl') }}</span>
            <span class="sub-text">{{ $t('ecom.hub.form.perStoreBaseUrlTip') }}</span>
          </div>
          <div v-if="connector.redirectNote" class="meta-item">
            <span class="meta-label">{{ $t('ecom.hub.form.redirectNote') }}</span>
            <span class="sub-text">{{ connector.redirectNote }}</span>
          </div>
          <div v-if="connector.orderWindowNote" class="meta-item">
            <span class="meta-label">{{ $t('ecom.hub.form.orderWindowNote') }}</span>
            <span class="sub-text">{{ connector.orderWindowNote }}</span>
          </div>
        </div>
        <a-alert v-if="isEdit && form.hasCredentials" type="success" show-icon class="cred-alert">
          <template #message>
            {{ $t('ecom.store.credConfigured') }}
            <a-button type="link" size="small" danger :loading="clearing" @click="clearCredentials(form.id)">{{ $t('ecom.store.clearCred') }}</a-button>
          </template>
        </a-alert>
        <div class="sub-title">{{ $t('ecom.store.sectionCredFields') }}</div>
        <a-row :gutter="16">
          <a-col v-for="f in connector.fields" :key="f.key" :xs="24" :sm="12" :lg="8">
            <a-form-item :label="f.label" :required="f.required && !f.oauth" :extra="f.oauth ? $t('ecom.hub.form.oauthFieldTip') : undefined">
              <template v-if="f.oauth">
                <a-tag v-if="credentialKeys.includes(f.key)" color="success" :bordered="false"><CheckCircleOutlined /> {{ $t('ecom.hub.form.oauthDone') }}</a-tag>
                <a-tag v-else :bordered="false">{{ $t('ecom.hub.form.oauthPending') }}</a-tag>
              </template>
              <a-input-password
                v-else-if="f.secret"
                v-model:value="form.fields[f.key]"
                autocomplete="new-password"
                :placeholder="credentialKeys.includes(f.key) ? $t('ecom.hub.form.keepPh') : undefined"
              />
              <a-input v-else v-model:value="form.fields[f.key]" :placeholder="credentialKeys.includes(f.key) ? $t('ecom.hub.form.keepPh') : undefined">
                <template v-if="credentialKeys.includes(f.key)" #suffix>
                  <span class="sub-text">{{ $t('ecom.hub.form.fieldSet') }}</span>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
        </a-row>
        <div class="sub-title">{{ $t('ecom.store.sectionEndpoint') }}</div>
        <a-row :gutter="16">
          <a-col :xs="24" :lg="8">
            <a-form-item :label="$t('ecom.hub.form.env')" name="sandbox">
              <a-radio-group v-model:value="form.sandbox" button-style="solid">
                <a-radio-button :value="0">{{ $t('ecom.hub.form.envProd') }}</a-radio-button>
                <a-radio-button :value="1">{{ $t('ecom.hub.form.envSandbox') }}</a-radio-button>
              </a-radio-group>
              <div v-if="form.sandbox === 1" class="env-tip">
                <a-alert v-if="!connector.hasSandbox" type="warning" show-icon :message="$t('ecom.hub.form.noSandbox')" />
                <span v-else class="sub-text">{{ $t('ecom.hub.form.sandboxUrl', { url: connector.sandboxBaseUrl }) }}</span>
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :lg="16">
            <a-form-item :label="$t('ecom.hub.form.apiBaseUrl')" name="apiBaseUrl" :extra="$t('ecom.hub.form.apiBaseUrlExtra', { url: defaultBaseUrl })">
              <a-input v-model:value="form.apiBaseUrl" :maxlength="255" placeholder="https://" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>
      </template>
      <template v-else>
        <a-alert type="info" show-icon class="cred-alert" :message="$t('ecom.hub.form.unsupported')" />
        <a-alert v-if="isEdit && form.hasCredentials" type="success" show-icon class="cred-alert">
          <template #message>
            {{ $t('ecom.store.credConfigured') }}
            <a-button type="link" size="small" danger :loading="clearing" @click="clearCredentials(form.id)">{{ $t('ecom.store.clearCred') }}</a-button>
          </template>
        </a-alert>
        <a-form-item :label="$t('ecom.store.credentials')" name="credentials" :extra="$t('ecom.store.credExtra')">
          <a-textarea v-model:value="form.credentials" :rows="4" :maxlength="4000" class="cred-input" placeholder='{"key": "value"}' />
        </a-form-item>
      </template>

      <div class="section-title">{{ $t('ecom.store.sectionWarehouse') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.warehouse')" name="warehouseId" :extra="$t('ecom.store.warehouseExtra')">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecom.store.platformWarehouse')" name="platformWarehouseId" :extra="$t('ecom.store.platformWarehouseExtra')">
            <WarehouseSelect v-model:value="form.platformWarehouseId" :placeholder="$t('ecom.store.platformWarehousePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecom.store.commissionRate')" name="commissionRate" :extra="$t('ecom.store.commissionRateExtra')">
            <a-input-number v-model:value="form.commissionRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecom.store.leadDays')" name="leadDays" :extra="$t('ecom.store.leadDaysExtra')">
            <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-form-item :label="$t('ecom.store.safetyDays')" name="safetyDays" :extra="$t('ecom.store.safetyDaysExtra')">
            <a-input-number v-model:value="form.safetyDays" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecom.hub.form.syncSection') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecom.hub.form.orderSync')" name="orderSync" :extra="orderSyncExtra">
            <a-switch v-model:checked="form.orderSync" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecom.hub.form.inventorySync')" name="inventorySync" :extra="inventorySyncExtra">
            <a-switch v-model:checked="form.inventorySync" :checked-value="1" :un-checked-value="0" :disabled="!canPush && form.inventorySync !== 1" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.inventorySync === 1" :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecom.hub.form.inventoryBuffer')" name="inventoryBuffer" :extra="$t('ecom.hub.form.inventoryBufferExtra')">
            <a-input-number v-model:value="form.inventoryBuffer" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecom.store.sectionOther') }}</div>
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckCircleOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addStore, clearStoreCredentials, getStore, storeCredentialKeys, updateStore } from '@/api/ecom/store'
import { currencyOptions, useCurrencies } from '@/components/fin/currency'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { ConnectorCapability, EcomPlatform } from '@/constants/enums'
import { supports, useConnectors } from './connectors'

/**
 * 平台店铺新增 / 编辑子页：openCreate() / openEdit(record) 打开，保存或清除授权后触发 success。
 * 授权字段按平台连接器声明的字段生成（GET /ecom/store/connectors），只写不读：已保存的显示「已设置」、留空表示不修改；
 * OAuth 字段由授权回调写入，只显示是否已授权。没有接口的平台保留 JSON 文本登记
 */
const emit = defineEmits(['success'])
const { t, te } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace')
const currencies = useCurrencies()
const { byPlatform } = useConnectors()

/** 常见时区，也可以直接输入其他 IANA 时区 */
const TIMEZONES = ['America/Los_Angeles', 'America/New_York', 'America/Chicago', 'America/Toronto', 'America/Mexico_City', 'America/Sao_Paulo',
  'Europe/London', 'Europe/Berlin', 'Europe/Paris', 'Europe/Madrid', 'Europe/Rome', 'Europe/Amsterdam', 'Europe/Warsaw', 'Europe/Moscow',
  'Asia/Dubai', 'Asia/Riyadh', 'Asia/Tokyo', 'Asia/Seoul', 'Asia/Shanghai', 'Asia/Singapore', 'Asia/Bangkok', 'Asia/Jakarta', 'Asia/Manila',
  'Asia/Ho_Chi_Minh', 'Asia/Kuala_Lumpur', 'Australia/Sydney', 'UTC']
const timezoneOptions = TIMEZONES.map((z) => ({ value: z }))
const filterTimezone = (input, option) => option.value.toLowerCase().includes((input || '').toLowerCase())

/** 没有接口的平台：授权 JSON 必须是对象 */
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
  credentials: [{ validator: validateCredentials, trigger: 'blur' }],
  apiBaseUrl: [{ pattern: /^https?:\/\/.+/i, message: t('ecom.hub.form.apiBaseUrlRule') }]
}

const FIELDS = ['code', 'name', 'platform', 'marketplace', 'currency', 'timezone', 'warehouseId', 'platformWarehouseId', 'commissionRate',
  'leadDays', 'safetyDays', 'ownerId', 'sellerId', 'sandbox', 'apiBaseUrl', 'orderSync', 'inventorySync', 'inventoryBuffer', 'remark']
/** 已保存的授权字段名（编辑时加载，不含值） */
const credentialKeys = ref([])

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    ...Object.fromEntries(FIELDS.map((f) => [f, undefined])),
    platform: EcomPlatform.AMAZON,
    sandbox: 0,
    orderSync: 1,
    inventorySync: 0,
    inventoryBuffer: 0,
    fields: {},
    credentials: '',
    hasCredentials: false,
    ownerName: undefined
  }),
  beforeOpen: async (record) => {
    credentialKeys.value = record?.id ? (await storeCredentialKeys(record.id)) || [] : []
  },
  // 详情不返回授权信息，输入框留空；负责人名称用列表行带的
  fetch: async (record) => ({ ...(await getStore(record.id)), fields: {}, credentials: '', ownerName: record.ownerName }),
  create: addStore,
  update: updateStore,
  toPayload: (f) => ({
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    credentials: credentialsPayload(f)
  }),
  onSuccess: () => emit('success')
})
const { formRef, form, isEdit } = drawer

const connector = computed(() => byPlatform.value[form.value.platform])
const canPush = computed(() => supports(connector.value, ConnectorCapability.INVENTORY_PUSH))
const defaultBaseUrl = computed(() => {
  const c = connector.value
  if (!c) return ''
  return form.value.sandbox === 1 && c.hasSandbox ? c.sandboxBaseUrl : c.productionBaseUrl
})
const orderSyncExtra = computed(() =>
  connector.value && !supports(connector.value, ConnectorCapability.ORDERS) ? t('ecom.hub.form.noOrders') : t('ecom.hub.form.orderSyncExtra')
)
const inventorySyncExtra = computed(() => (connector.value && !canPush.value ? t('ecom.hub.form.noInventoryPush') : t('ecom.hub.form.inventorySyncExtra')))

/**
 * 提交的授权 JSON：有接口的平台只提交本次填写的字段（与已保存的合并，留空的不修改）；没有接口的平台提交 JSON 文本；都没有填写为 null
 */
function credentialsPayload(f) {
  if (!connector.value) return (f.credentials || '').trim() || null
  const filled = Object.fromEntries(
    connector.value.fields.filter((x) => !x.oauth).map((x) => [x.key, (f.fields[x.key] || '').trim()]).filter(([, v]) => v)
  )
  return Object.keys(filled).length ? JSON.stringify(filled) : null
}

const authTypeName = (v) => (te(`ecom.hub.form.authTypeName.${v}`) ? t(`ecom.hub.form.authTypeName.${v}`) : v)
const authTypeTip = (v) => (te(`ecom.hub.form.authTypeTip.${v}`) ? t(`ecom.hub.form.authTypeTip.${v}`) : '')
const capLabel = (v) => (te(`ecom.hub.cap.${v}`) ? t(`ecom.hub.cap.${v}`) : v)

const clearing = ref(false)
const clearCredentials = useAction(clearStoreCredentials, {
  confirm: () => ({ title: t('ecom.store.clearCredTitle'), content: t('ecom.store.clearCredContent'), okType: 'danger' }),
  successKey: 'ecom.store.credCleared',
  loading: clearing,
  reload: () => {
    form.value.hasCredentials = false
    credentialKeys.value = []
    emit('success')
  }
})

/** 站点对应的常用币种与时区：选择站点时带出（币种需已在币种管理中启用） */
const MARKET_DEFAULTS = {
  US: ['USD', 'America/Los_Angeles'],
  CA: ['CAD', 'America/Toronto'],
  MX: ['MXN', 'America/Mexico_City'],
  BR: ['BRL', 'America/Sao_Paulo'],
  UK: ['GBP', 'Europe/London'],
  DE: ['EUR', 'Europe/Berlin'],
  FR: ['EUR', 'Europe/Paris'],
  IT: ['EUR', 'Europe/Rome'],
  ES: ['EUR', 'Europe/Madrid'],
  NL: ['EUR', 'Europe/Amsterdam'],
  PL: ['PLN', 'Europe/Warsaw'],
  SE: ['SEK', 'Europe/Stockholm'],
  AE: ['AED', 'Asia/Dubai'],
  SA: ['SAR', 'Asia/Riyadh'],
  JP: ['JPY', 'Asia/Tokyo'],
  AU: ['AUD', 'Australia/Sydney'],
  SG: ['SGD', 'Asia/Singapore'],
  MY: ['MYR', 'Asia/Kuala_Lumpur'],
  TH: ['THB', 'Asia/Bangkok'],
  PH: ['PHP', 'Asia/Manila'],
  VN: ['VND', 'Asia/Ho_Chi_Minh'],
  ID: ['IDR', 'Asia/Jakarta']
}
function onMarketplace(v) {
  const [currency, timezone] = MARKET_DEFAULTS[v] || []
  if (currency && currencyOptions(currencies.value).some((o) => o.value === currency)) form.value.currency = currency
  if (timezone) form.value.timezone = timezone
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
.sub-title {
  margin: 4px 0 8px;
  color: var(--app-text-2);
  font-size: 13px;
}
.connector-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 8px 24px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--app-bg);
  border: 1px solid var(--app-border);
  border-radius: 6px;
}
.meta-item {
  display: flex;
  align-items: baseline;
  min-width: 0;
}
.meta-label {
  flex: none;
  min-width: 96px;
  margin-right: 8px;
  color: var(--app-text-3);
}
.meta-link {
  min-width: 0;
  overflow-wrap: anywhere;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cred-alert {
  margin-bottom: 12px;
}
.cred-input {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}
.env-tip {
  margin-top: 8px;
}
@media (max-width: 575px) {
  .connector-meta {
    grid-template-columns: 1fr;
  }
}
</style>
