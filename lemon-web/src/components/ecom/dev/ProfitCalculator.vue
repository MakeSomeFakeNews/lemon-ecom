<template>
  <div class="profit-calc">
    <a-row :gutter="16">
      <a-col :xs="24" :xl="compact ? 24 : 13" :xxl="compact ? 12 : 13">
        <a-form :model="form" layout="vertical" class="calc-form">
          <div class="group-title">{{ $t('ecomDev.calc.groupPlatform') }}</div>
          <a-row :gutter="12">
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.platform')">
                <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" @change="loadDefaults" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.marketplace')">
                <a-select v-model:value="form.marketplace" allow-clear show-search :options="toOptions(dict.ecom_marketplace)" @change="onMarketplace" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.category')">
                <a-auto-complete v-model:value="form.category" :options="categoryOptions" allow-clear @change="loadDefaults" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.currencyRate')">
                <a-input-group compact class="rate-group">
                  <a-input v-model:value="form.currency" :placeholder="defaults.currency || $t('ecomDev.calc.currency')" class="cur" :maxlength="8" />
                  <a-input-number v-model:value="form.exchangeRate" :min="0" :precision="6" :placeholder="rateText(defaults.exchangeRate)" class="rate" />
                </a-input-group>
              </a-form-item>
            </a-col>
          </a-row>

          <div class="group-title">{{ $t('ecomDev.calc.groupPrice') }}</div>
          <a-row :gutter="12">
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.salePrice')" :extra="$t('ecomDev.calc.salePriceExtra')">
                <a-input-number v-model:value="form.salePrice" :min="0" :precision="2" class="full" :addon-after="currencyText" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.targetMargin')">
                <a-input-number v-model:value="form.targetMargin" :max="99" :precision="2" class="full" addon-after="%" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.purchasePrice')" required>
                <a-input-number v-model:value="form.purchasePrice" :min="0" :precision="2" class="full" addon-after="CNY" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.otherCost')" :extra="$t('ecomDev.calc.otherCostExtra')">
                <a-input-number v-model:value="form.otherCost" :min="0" :precision="2" class="full" addon-after="CNY" />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="group-title">{{ $t('ecomDev.calc.groupSize') }}</div>
          <a-row :gutter="12">
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.weightKg')">
                <a-input-number v-model:value="form.weightKg" :min="0" :precision="3" class="full" addon-after="kg" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.lengthCm')">
                <a-input-number v-model:value="form.lengthCm" :min="0" :precision="1" class="full" addon-after="cm" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.widthCm')">
                <a-input-number v-model:value="form.widthCm" :min="0" :precision="1" class="full" addon-after="cm" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.heightCm')">
                <a-input-number v-model:value="form.heightCm" :min="0" :precision="1" class="full" addon-after="cm" />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="group-title">{{ $t('ecomDev.calc.groupHead') }}</div>
          <a-row :gutter="12">
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.headPricing')">
                <a-select v-model:value="form.headPricing" :options="headOptions" />
              </a-form-item>
            </a-col>
            <a-col v-if="form.headPricing === DevHeadPricing.CHANNEL" :xs="12" :md="12">
              <a-form-item :label="$t('ecomDev.calc.channel')">
                <a-select v-model:value="form.logisticsChannelId" :options="channelOptions" show-search option-filter-prop="label" />
              </a-form-item>
            </a-col>
            <template v-else-if="form.headPricing === DevHeadPricing.PER_KG || form.headPricing === DevHeadPricing.PER_CBM">
              <a-col :xs="12" :md="6">
                <a-form-item :label="$t('ecomDev.calc.transportMode')">
                  <a-select v-model:value="form.transportMode" allow-clear :options="toOptions(dict.ecom_transport_mode)" />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :md="6">
                <a-form-item :label="$t('ecomDev.calc.headUnitPrice')">
                  <a-input-number v-model:value="form.headUnitPrice" :min="0" :precision="2" class="full"
                                  :addon-after="form.headPricing === DevHeadPricing.PER_KG ? 'CNY/kg' : 'CNY/m³'" />
                </a-form-item>
              </a-col>
            </template>
            <a-col v-else :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.headFee')">
                <a-input-number v-model:value="form.headFee" :min="0" :precision="2" class="full" addon-after="CNY" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.dutyRate')">
                <a-input-number v-model:value="form.dutyRate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="group-title">{{ $t('ecomDev.calc.groupFees') }}</div>
          <a-row :gutter="12">
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.commissionRate')">
                <a-input-number v-model:value="form.commissionRate" :min="0" :max="100" :precision="2" class="full" addon-after="%"
                                :placeholder="defaults.commissionRate != null ? String(Number(defaults.commissionRate)) : ''" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.minCommission')">
                <a-input-number v-model:value="form.minCommission" :min="0" :precision="2" class="full" :addon-after="currencyText"
                                :placeholder="defaults.minCommission != null ? String(Number(defaults.minCommission)) : ''" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.fixedFee')">
                <a-input-number v-model:value="form.fixedFee" :min="0" :precision="2" class="full" :addon-after="currencyText"
                                :placeholder="defaults.fixedFee != null ? String(Number(defaults.fixedFee)) : ''" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.fulfillmentFee')">
                <div class="fee-row">
                  <a-checkbox v-model:checked="form.fbaAuto" class="fba-check">{{ $t('ecomDev.calc.fbaAuto') }}</a-checkbox>
                  <a-input-number v-if="!form.fbaAuto" v-model:value="form.fulfillmentFee" :min="0" :precision="2" class="fee" />
                </div>
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.adRate')">
                <a-input-number v-model:value="form.adRate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.returnRate')" :extra="$t('ecomDev.calc.returnRateExtra')">
                <a-input-number v-model:value="form.returnRate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item :label="$t('ecomDev.calc.tax')">
                <a-input-group compact class="rate-group">
                  <a-select v-model:value="form.taxType" allow-clear :options="toOptions(dict.ecom_tax_type)" class="tax-type"
                            :placeholder="defaults.taxType ? dictLabel(defaults.taxType) : $t('ecomDev.calc.taxNone')" />
                  <a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" class="tax-rate"
                                  :placeholder="defaults.taxRate != null ? `${Number(defaults.taxRate)}%` : '%'" />
                </a-input-group>
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item :label="$t('ecomDev.calc.otherSiteFee')" :extra="$t('ecomDev.calc.otherSiteFeeExtra')">
                <a-input-number v-model:value="form.otherSiteFee" :min="0" :precision="2" class="full" :addon-after="currencyText" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-space wrap>
            <a-button type="primary" :loading="loading" @click="calculate()"><template #icon><CalculatorOutlined /></template>{{ $t('ecomDev.calc.calculate') }}</a-button>
            <a-button v-if="canSave" :disabled="!result" @click="openSave"><template #icon><SaveOutlined /></template>{{ $t('ecomDev.calc.saveScheme') }}</a-button>
            <a-button @click="reset"><template #icon><ReloadOutlined /></template>{{ $t('common.action.reset') }}</a-button>
          </a-space>
        </a-form>
      </a-col>

      <a-col :xs="24" :xl="compact ? 24 : 11" :xxl="compact ? 12 : 11">
        <div class="result-panel">
          <div class="result-title">{{ $t('ecomDev.calc.resultTitle') }}</div>
          <ProfitResultView :result="result" :loading="loading" />
        </div>
      </a-col>
    </a-row>

    <a-modal v-model:open="saveOpen" :title="$t('ecomDev.calc.saveScheme')" :confirm-loading="saving" @ok="save">
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomDev.scheme.name')" required>
          <a-input v-model:value="schemeName" :maxlength="64" />
        </a-form-item>
        <a-alert v-if="projectId" type="info" show-icon :message="$t('ecomDev.calc.saveToProject')" />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CalculatorOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons-vue'
import ProfitResultView from './ProfitResultView.vue'
import { calcProfit, listCommissionRate, profitDefaults, saveProfitScheme } from '@/api/ecom/dev'
import { logisticsOptions } from '@/api/ecom/logistics'
import { DevHeadPricing, EcomPlatform } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'

/**
 * 利润试算器：平台 / 站点 / 类目带出默认佣金、币种、汇率、税率（留空即用默认值，显示为占位符），
 * 输入采购价、尺寸重量、头程、配送费、广告占比、退货率等后试算（参数变化后自动重算）；可按目标利润率反算售价、保存为方案。
 * 试算页面和开发项目详情共用。ref 调用 load(params) 载入方案参数、params() 取当前参数
 *
 * @prop initial   初始参数（如开发项目的平台、站点、目标售价、采购价、尺寸重量）
 * @prop projectId 开发项目（保存方案时关联，回写项目的试算利润率）
 * @prop compact   嵌入详情时左右排列改为上下
 */
const props = defineProps({
  initial: { type: Object, default: () => ({}) },
  projectId: Number,
  compact: Boolean
})
const emit = defineEmits(['saved', 'result'])
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('ecom_platform', 'ecom_marketplace', 'ecom_transport_mode', 'ecom_tax_type')

const canSave = computed(() => userStore.hasPerm('ecom:profitCalc:save') || (props.projectId && userStore.hasPerm('ecom:devProject:edit')))

const empty = () => ({
  platform: EcomPlatform.AMAZON,
  marketplace: 'US',
  category: undefined,
  currency: undefined,
  exchangeRate: undefined,
  salePrice: undefined,
  targetMargin: 25,
  purchasePrice: undefined,
  otherCost: undefined,
  weightKg: undefined,
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  transportMode: 'AIR',
  headPricing: DevHeadPricing.PER_KG,
  logisticsChannelId: undefined,
  headUnitPrice: 38,
  headFee: undefined,
  dutyRate: undefined,
  fbaAuto: true,
  fulfillmentFee: undefined,
  commissionRate: undefined,
  minCommission: undefined,
  fixedFee: undefined,
  adRate: 10,
  returnRate: 3,
  taxType: undefined,
  taxRate: undefined,
  otherSiteFee: undefined
})
const form = reactive({ ...empty(), ...clean(props.initial) })
const defaults = ref({})
const result = ref()
const loading = ref(false)
const channels = ref([])
const categories = ref([])

/** 头程计费方式：渠道费率 / 每公斤 / 每立方 / 手填单件（MANUAL 为前端值，提交时 headPricing 为空） */
const MANUAL = 'MANUAL'
const headOptions = computed(() => [
  { value: DevHeadPricing.CHANNEL, label: t('ecomDev.calc.head.CHANNEL') },
  { value: DevHeadPricing.PER_KG, label: t('ecomDev.calc.head.PER_KG') },
  { value: DevHeadPricing.PER_CBM, label: t('ecomDev.calc.head.PER_CBM') },
  { value: MANUAL, label: t('ecomDev.calc.head.MANUAL') }
])
const channelOptions = computed(() => channels.value.map((c) => ({ value: c.id, label: `${c.code} ${c.name}` })))
const categoryOptions = computed(() =>
  [...new Set(categories.value.filter((r) => r.platform === form.platform && r.category).map((r) => r.category))].map((c) => ({ value: c }))
)
const currencyText = computed(() => form.currency || defaults.value.currency || undefined)
const dictLabel = (v) => findDict(dict.ecom_tax_type, v)?.label || v
const rateText = (r) => (r != null ? String(Number(r)) : t('ecomDev.calc.rateMissing'))

function clean(obj) {
  const out = {}
  Object.entries(obj || {}).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') out[k] = typeof v === 'string' && !isNaN(v) && k !== 'marketplace' && k !== 'category' && k !== 'currency' ? Number(v) : v
  })
  return out
}

async function loadDefaults() {
  if (!form.platform) return
  try {
    defaults.value = await profitDefaults({ platform: form.platform, marketplace: form.marketplace || undefined, category: form.category || undefined })
  } catch {
    defaults.value = {}
  }
}

function onMarketplace() {
  form.currency = undefined
  form.exchangeRate = undefined
  form.taxRate = undefined
  form.taxType = undefined
  loadDefaults()
}

/** 提交参数：空值去掉，MANUAL 头程改为 headPricing 为空 */
function params() {
  const p = {}
  Object.entries(form).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p[k] = v
  })
  if (p.headPricing === MANUAL) delete p.headPricing
  if (p.headPricing !== DevHeadPricing.CHANNEL) delete p.logisticsChannelId
  if (p.fbaAuto) delete p.fulfillmentFee
  if (props.projectId) p.projectId = props.projectId
  return p
}

const ready = () => form.platform && form.purchasePrice != null && (form.salePrice != null || form.targetMargin != null)
  && (form.headPricing !== DevHeadPricing.CHANNEL || form.logisticsChannelId)

async function calculate(silent = false) {
  if (!ready()) {
    if (!silent) message.warning(t('ecomDev.calc.needInput'))
    return
  }
  loading.value = true
  try {
    result.value = await calcProfit(params())
    emit('result', result.value)
  } catch {
    if (silent) result.value = undefined
  } finally {
    loading.value = false
  }
}

let timer
watch(form, () => {
  clearTimeout(timer)
  timer = setTimeout(() => calculate(true), 500)
})

function reset() {
  Object.assign(form, empty(), clean(props.initial))
  result.value = undefined
  loadDefaults()
}

/** 载入方案参数 */
function load(p) {
  Object.assign(form, empty(), clean(p))
  if (p && p.headPricing == null) form.headPricing = MANUAL
  loadDefaults()
}

const saveOpen = ref(false)
const saving = ref(false)
const schemeName = ref('')
function openSave() {
  schemeName.value = `${findDict(dict.ecom_platform, form.platform)?.label || form.platform} ${form.marketplace || ''} ${result.value?.salePrice ?? ''}`.trim()
  saveOpen.value = true
}
async function save() {
  if (!schemeName.value.trim()) {
    message.warning(t('ecomDev.scheme.nameRequired'))
    return
  }
  saving.value = true
  try {
    await saveProfitScheme({ name: schemeName.value.trim(), params: params() })
    message.success(t('ecomDev.calc.saved'))
    saveOpen.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (props.initial && props.initial.headPricing === null) form.headPricing = MANUAL
  loadDefaults()
  logisticsOptions().then((list) => (channels.value = list || [])).catch(() => {})
  listCommissionRate({}).then((list) => (categories.value = list || [])).catch(() => {})
  calculate(true)
})

defineExpose({ load, params, calculate })
</script>

<style scoped>
.full {
  width: 100%;
}
.group-title {
  font-weight: 600;
  margin: 4px 0 8px;
  color: var(--app-text-2);
}
.calc-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}
.rate-group {
  display: flex !important;
  align-items: center;
}
.rate-group .cur {
  width: 40%;
}
.rate-group .rate {
  width: 60%;
}
.rate-group .tax-type {
  width: 62%;
}
.rate-group .tax-rate {
  width: 38%;
}
.fee-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
}
.fba-check {
  white-space: nowrap;
}
.result-panel {
  position: sticky;
  top: 0;
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-card);
}
.result-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--app-text-2);
}
@media (max-width: 1199px) {
  .result-panel {
    position: static;
    margin-top: 8px;
  }
}
.fee-row .fee {
  flex: 1;
  min-width: 0;
}
</style>
