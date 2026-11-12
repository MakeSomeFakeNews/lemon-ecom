<template>
  <a-spin :spinning="loading">
    <div v-if="result" class="result">
      <div class="kpis">
        <div class="kpi main" :class="{ loss: Number(result.profit) < 0 }">
          <div class="label">{{ $t('ecomDev.calc.profit') }}</div>
          <div class="value">¥ {{ money(result.profit) }}</div>
          <div class="sub">{{ result.currency }} {{ money(result.profitSite) }}</div>
        </div>
        <div class="kpi" :class="marginClass(result.margin)">
          <div class="label">{{ $t('ecomDev.calc.margin') }}</div>
          <div class="value">{{ pct(result.margin) }}</div>
          <div class="sub">ROI {{ pct(result.roi) }}</div>
        </div>
        <div class="kpi">
          <div class="label">{{ $t('ecomDev.calc.salePriceResult') }}</div>
          <div class="value">{{ result.currency }} {{ money(result.salePrice) }}</div>
          <div class="sub">≈ ¥ {{ money(result.revenue) }} · {{ $t('ecomDev.calc.rateShort', { rate: Number(result.exchangeRate) }) }}</div>
        </div>
        <div class="kpi">
          <div class="label">{{ $t('ecomDev.calc.breakeven') }}</div>
          <div class="value">{{ result.breakevenPrice != null ? `${result.currency} ${money(result.breakevenPrice)}` : '-' }}</div>
          <div v-if="result.targetMargin != null" class="sub">
            {{ $t('ecomDev.calc.suggestPrice', { margin: Number(result.targetMargin) }) }}
            <b>{{ result.suggestPrice != null ? `${result.currency} ${money(result.suggestPrice)}` : '-' }}</b>
          </div>
        </div>
      </div>

      <a-alert v-for="(w, i) in result.warnings || []" :key="i" type="warning" show-icon :message="w" class="warn" />

      <a-table size="small" :columns="columns" :data-source="rows" :pagination="false" row-key="key" class="lines">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'site'">{{ record.site != null ? money(record.site) : '' }}</template>
          <template v-else-if="column.key === 'base'">{{ money(record.base) }}</template>
          <template v-else-if="column.key === 'ratio'">
            <a-progress :percent="ratio(record)" :show-info="true" size="small" :format="(p) => `${p}%`" />
          </template>
        </template>
        <template #summary>
          <a-table-summary-row>
            <a-table-summary-cell>{{ $t('ecomDev.calc.totalCost') }}</a-table-summary-cell>
            <a-table-summary-cell />
            <a-table-summary-cell align="right"><b>{{ money(result.totalCost) }}</b></a-table-summary-cell>
            <a-table-summary-cell />
          </a-table-summary-row>
        </template>
      </a-table>

      <a-descriptions size="small" :column="2" class="meta">
        <a-descriptions-item :label="$t('ecomDev.calc.commissionRate')">
          {{ Number(result.commissionRate || 0) }}%<span v-if="result.minCommission != null"> · {{ $t('ecomDev.calc.minShort', { v: Number(result.minCommission) }) }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('ecomDev.calc.tax')">
          {{ result.taxType ? `${dictText(result.taxType)} ${Number(result.taxRate || 0)}%` : $t('ecomDev.calc.taxNone') }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('ecomDev.calc.chargeWeight')">{{ Number(result.chargeWeightKg || 0) }} kg · {{ Number(result.volumeCbm || 0) }} m³</a-descriptions-item>
        <a-descriptions-item v-if="result.fbaTier || result.fbaTierName" :label="$t('ecomDev.calc.fbaTier')">
          {{ result.fbaTierName || $t(`ecomDev.calc.tier.${result.fbaTier}`) }}
          <span v-if="result.fbaShippingWeight != null"> · {{ Number(result.fbaShippingWeight) }} {{ result.fbaWeightUnit || '' }}</span>
        </a-descriptions-item>
      </a-descriptions>
    </div>
    <a-empty v-else :description="$t('ecomDev.calc.emptyResult')" />
  </a-spin>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { money } from '@/utils/number'
import { marginClass } from '@/views/ecom/dev-project/dev'

/**
 * 利润试算结果：利润 / 利润率 / ROI / 售价 / 保本价与建议售价，成本明细（站点币种与本位币、占收入比例），计费重、FBA 尺寸分段、提示
 */
const props = defineProps({ result: Object, loading: Boolean })
const { t } = useI18n()
const dict = useDict('ecom_tax_type')

const columns = [
  { title: t('ecomDev.calc.costItem'), dataIndex: 'label', width: 120 },
  { title: t('ecomDev.calc.siteAmount'), key: 'site', align: 'right', width: 100 },
  { title: t('ecomDev.calc.baseAmount'), key: 'base', align: 'right', width: 100 },
  { title: t('ecomDev.calc.ratio'), key: 'ratio' }
]
const rows = computed(() => (props.result?.lines || []).filter((l) => Number(l.base) !== 0).map((l) => ({ ...l, label: t(`ecomDev.calc.line.${l.key}`) })))
const ratio = (r) => (Number(props.result?.revenue) > 0 ? Math.round((Number(r.base) / Number(props.result.revenue)) * 1000) / 10 : 0)
const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(2)}%`)
const dictText = (v) => findDict(dict.ecom_tax_type, v)?.label || v
</script>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.kpi {
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 8px;
  padding: 10px 12px;
}
.kpi .label {
  color: var(--app-text-3);
  font-size: 12px;
}
.kpi .value {
  font-size: 20px;
  font-weight: 600;
  margin: 2px 0;
}
.kpi .sub {
  font-size: 12px;
  color: var(--app-text-3);
}
.kpi.main .value,
.kpi.good .value {
  color: #389e0d;
}
.kpi.low .value {
  color: #d48806;
}
.kpi.loss .value {
  color: #cf1322;
}
.warn {
  margin-bottom: 8px;
}
.lines {
  margin-bottom: 8px;
}
.meta {
  margin-top: 8px;
}
</style>
