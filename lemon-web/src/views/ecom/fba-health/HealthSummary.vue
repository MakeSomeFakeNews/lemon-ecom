<template>
  <a-card :bordered="false" class="page-card fba-health-summary">
    <a-spin :spinning="loading">
      <div class="cards">
        <div class="stat">
          <div class="label">{{ $t('ecomFba.health.skuCount') }}</div>
          <div class="value">{{ data.skuCount ?? 0 }}</div>
        </div>
        <div class="stat">
          <div class="label">{{ $t('ecomFba.health.available') }} / {{ $t('ecomFba.health.inbound') }}</div>
          <div class="value">{{ n0(data.availableQty) }} <span class="sub">/ {{ n0(data.inboundQty) }}</span></div>
        </div>
        <div class="stat">
          <div class="label">{{ $t('ecomFba.health.reserved') }} / {{ $t('ecomFba.health.unfulfillable') }} / {{ $t('ecomFba.health.researching') }}</div>
          <div class="value">{{ n0(data.reservedQty) }} <span class="sub">/ {{ n0(data.unfulfillableQty) }} / {{ n0(data.researchingQty) }}</span></div>
        </div>
        <div class="stat warn">
          <div class="label">{{ $t('ecomFba.health.excessQty') }}</div>
          <div class="value">{{ n0(data.excessQty) }}</div>
          <div class="sub">{{ $t('ecomFba.health.excessSkus', { n: data.excessSkus || 0 }) }}</div>
        </div>
        <div class="stat warn">
          <div class="label">{{ $t('ecomFba.health.agedCard') }}</div>
          <div class="value">{{ n0(data.agedQty) }}</div>
        </div>
        <div class="stat">
          <div class="label">{{ $t('ecomFba.health.monthlyFeeBase') }}</div>
          <div class="value">{{ money(data.monthlyFeeBase) }}</div>
        </div>
        <div class="stat danger">
          <div class="label">{{ $t('ecomFba.health.agedFeeBase') }}</div>
          <div class="value">{{ money(data.agedFeeBase) }}</div>
        </div>
        <div class="stat ok">
          <div class="label">{{ $t('ecomFba.health.claimable') }}</div>
          <div class="value">
            <router-link v-if="can('ecom:fbaClaim:list')" to="/ecom/fba-claim">{{ money(data.claimableBase) }}</router-link>
            <span v-else>{{ money(data.claimableBase) }}</span>
          </div>
        </div>
      </div>

      <div class="bars">
        <div class="bar-block">
          <div class="bar-title">{{ $t('ecomFba.health.ageDistribution') }}</div>
          <div class="bar">
            <a-tooltip v-for="(b, i) in buckets" :key="i" :title="`${AGE_LABELS[i]}: ${n0(b)}`">
              <div class="seg" :class="`age-${i}`" :style="{ width: pct(b, ageTotal) + '%' }" />
            </a-tooltip>
          </div>
          <div class="legend">
            <span v-for="(b, i) in buckets" :key="i"><i :class="`dot age-${i}`" />{{ AGE_LABELS[i] }} {{ n0(b) }}</span>
          </div>
        </div>
        <div class="bar-block">
          <div class="bar-title">{{ $t('ecomFba.health.adviceDistribution') }}</div>
          <a-space wrap>
            <span v-for="a in ADVICES" :key="a" class="advice-item" @click="emit('advice', a)">
              <DictTag type="ecom_fba_advice" :value="a" /> {{ data.adviceCounts?.[a] || 0 }}
            </span>
          </a-space>
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
import DictTag from '@/components/DictTag.vue'
import { fbaHealthSummary } from '@/api/ecom/fba'
import { money, nz } from '@/utils/number'
import { FbaHealthAdvice } from '@/constants/enums'
import { t } from '@/locales'
import { n0, useCan } from './fba'

/** 库存健康汇总：件数、冗余、超龄、预估仓储费（本位币）、可索赔金额、库龄分布与建议分布。ref 调用 load(params)；@advice(值) 点击建议筛选 */
const emit = defineEmits(['advice'])
const can = useCan()
const ADVICES = Object.values(FbaHealthAdvice)
const AGE_LABELS = [t('ecomFba.age.a0'), t('ecomFba.age.a91'), t('ecomFba.age.a181'), t('ecomFba.age.a271'), t('ecomFba.age.a365')]

const data = ref({})
const loading = ref(false)
const buckets = computed(() => data.value.buckets || [0, 0, 0, 0, 0])
const ageTotal = computed(() => buckets.value.reduce((s, b) => s + nz(b), 0))
const pct = (v, total) => (total ? (nz(v) / total) * 100 : 0)

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
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}
.stat {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-fill, #fafafa);
}
.stat .label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat .value {
  font-size: 20px;
  font-weight: 600;
  margin-top: 2px;
}
.stat .sub {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.stat.warn .value {
  color: #d46b08;
}
.stat.danger .value {
  color: #cf1322;
}
.stat.ok .value {
  color: #389e0d;
}
.bars {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 14px;
}
.bar-block {
  flex: 1 1 320px;
  min-width: 0;
}
.bar-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.bar {
  display: flex;
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
  background: var(--app-fill, #f0f0f0);
}
.seg {
  height: 100%;
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-2);
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}
.age-0 {
  background: #52c41a;
}
.age-1 {
  background: #a0d911;
}
.age-2 {
  background: #faad14;
}
.age-3 {
  background: #fa8c16;
}
.age-4 {
  background: #f5222d;
}
.advice-item {
  cursor: pointer;
}
.alert {
  margin-top: 10px;
}
</style>

<style>
html[data-theme='dark'] .fba-health-summary .stat {
  background: rgba(255, 255, 255, 0.04);
}
</style>
