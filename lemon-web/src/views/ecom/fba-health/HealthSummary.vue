<template>
  <a-card :bordered="false" class="page-card fba-health-summary">
    <a-spin :spinning="loading">
      <div class="kpis">
        <div class="kpi">
          <div class="kpi-label"><ShopOutlined /> {{ $t('ecomFba.health.available') }}<span class="kpi-tag">{{ $t('ecomFba.health.skuN', { n: data.skuCount ?? 0 }) }}</span></div>
          <div class="kpi-value">{{ n0(data.availableQty ?? 0) }}</div>
          <div class="kpi-sub">
            {{ $t('ecomFba.health.inbound') }} {{ n0(data.inboundQty ?? 0) }} · {{ $t('ecomFba.health.reserved') }} {{ n0(data.reservedQty ?? 0) }} ·
            {{ $t('ecomFba.health.unfulfillable') }} {{ n0(data.unfulfillableQty ?? 0) }} · {{ $t('ecomFba.health.researching') }} {{ n0(data.researchingQty ?? 0) }}
          </div>
        </div>
        <div class="kpi warn clickable" :class="{ active: agedOnly }" @click="emit('filter', 'aged')">
          <div class="kpi-label"><FieldTimeOutlined /> {{ $t('ecomFba.health.agedCard') }}</div>
          <div class="kpi-value">{{ n0(data.agedQty ?? 0) }}</div>
          <div class="kpi-sub">{{ $t('ecomFba.health.agedShare', { p: pct(data.agedQty, ageTotal).toFixed(1) }) }}</div>
        </div>
        <div class="kpi warn clickable" :class="{ active: excessOnly }" @click="emit('filter', 'excess')">
          <div class="kpi-label"><InboxOutlined /> {{ $t('ecomFba.health.excessQty') }}</div>
          <div class="kpi-value">{{ n0(data.excessQty ?? 0) }}</div>
          <div class="kpi-sub">{{ $t('ecomFba.health.excessSkus', { n: data.excessSkus || 0 }) }}</div>
        </div>
        <div class="kpi danger">
          <div class="kpi-label"><DollarOutlined /> {{ $t('ecomFba.health.storageFeeCard') }}</div>
          <div class="kpi-value">¥{{ money(nz(data.monthlyFeeBase) + nz(data.agedFeeBase)) }}</div>
          <div class="kpi-sub">{{ $t('ecomFba.health.feeBreak', { m: money(data.monthlyFeeBase ?? 0), a: money(data.agedFeeBase ?? 0) }) }}</div>
        </div>
      </div>

      <div v-if="!showPanels" class="panels-toggle">
        <a @click="togglePanels">{{ $t('ecomFba.health.showCharts') }} <DownOutlined class="toggle-icon" /></a>
      </div>
      <div v-show="showPanels" class="panels">
        <div class="panel">
          <div class="panel-title">
            {{ $t('ecomFba.health.ageDistribution') }}
            <a class="hide-link" @click="togglePanels">{{ $t('ecomFba.health.hideCharts') }} <DownOutlined class="toggle-icon up" /></a>
          </div>
          <EChart :option="ageOption" height="136px" />
        </div>
        <div class="panel">
          <div class="panel-title">
            {{ $t('ecomFba.health.suggestions') }}
            <router-link v-if="can('ecom:fbaClaim:list') && nz(data.claimableBase) > 0" to="/ecom/fba-claim" class="claim-link">
              <SafetyCertificateOutlined /> {{ $t('ecomFba.health.claimableLink', { amount: money(data.claimableBase) }) }}
            </router-link>
          </div>
          <div class="advices">
            <div v-for="a in ADVICES" :key="a" class="advice" :class="{ active: advice === a }" @click="emit('advice', advice === a ? undefined : a)">
              <DictTag type="ecom_fba_advice" :value="a" />
              <span class="advice-count">{{ data.adviceCounts?.[a] || 0 }}</span>
              <span class="advice-desc">{{ $t(`ecomFba.health.adviceDesc.${a}`) }}</span>
              <RightOutlined class="advice-arrow" />
            </div>
          </div>
        </div>
      </div>

      <a-alert v-if="data.missingDims" type="warning" show-icon class="alert">
        <template #message>
          {{ $t('ecomFba.health.missingDims', { n: data.missingDims }) }}
          <router-link v-if="can('ecom:fbaFee:list')" to="/ecom/fba-fee-audit?tab=dims">{{ $t('ecomFba.health.maintainDims') }}</router-link>
        </template>
      </a-alert>
      <a-alert v-if="data.missingRates?.length" type="warning" show-icon class="alert" :message="$t('ecomFba.common.missingRates', { list: data.missingRates.join(', ') })" />
    </a-spin>
  </a-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import { DollarOutlined, DownOutlined, FieldTimeOutlined, InboxOutlined, RightOutlined, SafetyCertificateOutlined, ShopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { fbaHealthSummary } from '@/api/ecom/fba'
import { money, nz } from '@/utils/number'
import { FbaHealthAdvice } from '@/constants/enums'
import { t } from '@/locales'
import { n0, useCan } from './fba'

/**
 * 库存健康汇总：可售、超龄（181 天以上）、冗余、预估仓储费（本位币）四张卡，库龄分布柱图与处理建议。
 * ref 调用 load(params)；@advice(值) 点击建议筛选（再次点击取消），@filter('aged' | 'excess') 点击卡片切换只看超龄 / 冗余
 */
defineProps({ advice: { type: String, default: undefined }, agedOnly: Boolean, excessOnly: Boolean })
const emit = defineEmits(['advice', 'filter'])
const can = useCan()
const ADVICES = [FbaHealthAdvice.REMOVE, FbaHealthAdvice.PRICE_DOWN, FbaHealthAdvice.PROMOTE, FbaHealthAdvice.HEALTHY]
const AGE_LABELS = [t('ecomFba.age.a0'), t('ecomFba.age.a91'), t('ecomFba.age.a181'), t('ecomFba.age.a271'), t('ecomFba.age.a365')]
const AGE_COLORS = ['#52c41a', '#a0d911', '#faad14', '#fa8c16', '#f5222d']

const PANELS_KEY = 'ecom-fba-health-panels'
const readPanels = () => {
  try {
    const v = localStorage.getItem(PANELS_KEY)
    if (v) return v === '1'
  } catch {
    // 读不到存储时按窗口高度决定
  }
  // 默认：窗口足够高时展开，矮屏收起以便表格显示更多行
  return window.innerHeight >= 960
}
/** 库龄分布与建议操作可收起（记住选择），收起后表格显示更多行 */
const showPanels = ref(readPanels())
function togglePanels() {
  showPanels.value = !showPanels.value
  try {
    localStorage.setItem(PANELS_KEY, showPanels.value ? '1' : '0')
  } catch {
    // 浏览器禁用存储时只在本次生效
  }
}

const data = ref({})
const loading = ref(false)
const buckets = computed(() => data.value.buckets || [0, 0, 0, 0, 0])
const ageTotal = computed(() => buckets.value.reduce((s, b) => s + nz(b), 0))
const pct = (v, total) => (total ? (nz(v) / total) * 100 : 0)

const ageOption = computed(() => ({
  grid: { left: 8, right: 16, top: 24, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${n0(v)} (${pct(v, ageTotal.value).toFixed(1)}%)` },
  xAxis: { type: 'category', data: AGE_LABELS, axisTick: { show: false } },
  yAxis: { type: 'value', splitNumber: 3 },
  series: [
    {
      type: 'bar',
      barMaxWidth: 44,
      data: buckets.value.map((v, i) => ({ value: nz(v), itemStyle: { color: AGE_COLORS[i], borderRadius: [4, 4, 0, 0] } })),
      label: { show: true, position: 'top', formatter: (p) => `${n0(p.value)} · ${pct(p.value, ageTotal.value).toFixed(0)}%`, fontSize: 11, textBorderWidth: 0 }
    }
  ]
}))

async function load(params) {
  loading.value = true
  try {
    data.value = (await fbaHealthSummary(params)) || {}
  } finally {
    loading.value = false
  }
}

defineExpose({ load })
</script>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}
.kpi {
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
  border-left: 3px solid var(--app-primary);
  transition: box-shadow 0.2s, border-color 0.2s;
}
.kpi.warn {
  border-left-color: var(--status-warning);
}
.kpi.danger {
  border-left-color: var(--status-critical);
}
.kpi.clickable {
  cursor: pointer;
}
.kpi.clickable:hover,
.kpi.active {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-color: var(--status-warning);
}
.kpi-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-text-2);
  font-size: 13px;
}
.kpi-tag {
  margin-left: auto;
  color: var(--app-text-3);
  font-size: 12px;
}
.kpi-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;
}
.kpi.warn .kpi-value {
  color: #d46b08;
}
.kpi.danger .kpi-value {
  color: #cf1322;
}
.kpi-sub {
  margin-top: 2px;
  color: var(--app-text-3);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panels-toggle {
  display: flex;
  justify-content: center;
  margin: 10px 0 -8px;
  font-size: 12px;
}
.hide-link {
  margin-left: 12px;
  font-size: 12px;
  font-weight: normal;
}
.toggle-icon {
  font-size: 10px;
  transition: transform 0.2s;
}
.toggle-icon.up {
  transform: rotate(180deg);
}
.panels {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 16px;
  margin-top: 14px;
}
@media (max-width: 1100px) {
  .panels {
    grid-template-columns: minmax(0, 1fr);
  }
}
.panel-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  margin-bottom: 6px;
}
.claim-link {
  margin-left: auto;
  font-weight: normal;
  font-size: 12px;
}
.advices {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.advice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px;
  border-radius: 6px;
  border: 1px solid var(--app-border);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.advice:hover {
  background: var(--app-hover);
}
.advice.active {
  border-color: var(--app-primary);
  background: var(--app-primary-bg, var(--app-hover));
}
.advice :deep(.ant-tag) {
  margin: 0;
  min-width: 72px;
  text-align: center;
}
.advice-count {
  min-width: 32px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.advice-desc {
  flex: 1;
  min-width: 0;
  color: var(--app-text-3);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.advice-arrow {
  color: var(--app-text-3);
  font-size: 10px;
}
.alert {
  margin-top: 10px;
}
</style>

<style>
html[data-theme='dark'] .fba-health-summary .kpi.clickable:hover,
html[data-theme='dark'] .fba-health-summary .kpi.active {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}
html[data-theme='dark'] .fba-health-summary .kpi.warn .kpi-value {
  color: #f0a64b;
}
html[data-theme='dark'] .fba-health-summary .kpi.danger .kpi-value {
  color: #f36d6d;
}
</style>
