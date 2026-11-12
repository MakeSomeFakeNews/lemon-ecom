<template>
  <a-table
    :row-key="rowKey"
    size="small"
    :columns="columns"
    :data-source="rows"
    :pagination="rows.length > 10 ? { pageSize: 10, size: 'small', showSizeChanger: false } : false"
    :scroll="{ x: scrollX }"
    :locale="{ emptyText: $t(`ecomAsst.detail.empty.${kind}`) }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'store'">
        <div>{{ record.storeName }}</div>
        <div v-if="record.platform" class="sub-text"><DictTag type="ecom_platform" :value="record.platform" /> {{ record.marketplace }}</div>
      </template>
      <template v-else-if="column.key === 'sku'">
        <div class="mono">{{ record.sellerSku || '-' }}</div>
        <div v-if="record.title" class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
      </template>
      <template v-else-if="column.money">
        <span :class="{ danger: Number(record[column.dataIndex]) < 0 }">{{ money(record[column.dataIndex]) }}</span>
      </template>
      <template v-else-if="column.pct">{{ pct(record[column.dataIndex]) }}</template>
      <template v-else-if="column.trend">
        <span :class="trendClass(record[column.dataIndex], column.reverse)">{{ signedPct(record[column.dataIndex]) }}</span>
      </template>
      <template v-else-if="column.key === 'abnormalType'"><DictTag type="ecom_ops_abnormal_type" :value="record.type" /></template>
      <template v-else-if="column.key === 'compare'">
        <template v-if="record.type === OpsAbnormalType.RETURN_RATE_UP">
          {{ pct(record.current) }} <span class="sub-text">/ {{ pct(record.baseline) }}</span>
        </template>
        <template v-else-if="record.type === OpsAbnormalType.PROFIT_DROP">
          {{ money(record.current) }} <span class="sub-text">/ {{ money(record.baseline) }}</span>
        </template>
        <template v-else>{{ num(record.current, 2) }} <span class="sub-text">/ {{ num(record.baseline, 2) }}</span></template>
      </template>
      <template v-else-if="column.key === 'abChange'">
        <span class="trend-down">{{ record.type === OpsAbnormalType.RETURN_RATE_UP ? `+${num(record.changePercent, 2)}pp` : signedPct(record.changePercent) }}</span>
      </template>
      <template v-else-if="column.key === 'clues'">
        <span class="sub-text">{{ clues(record) }}</span>
      </template>
      <template v-else-if="column.key === 'rating'"><a-rate :value="record.rating" disabled class="rate" /></template>
      <template v-else-if="column.key === 'review'">
        <div v-if="record.title" class="review-title">{{ record.title }}</div>
        <a-tooltip :title="record.content" placement="topLeft"><div class="ellipsis-2">{{ record.content || '-' }}</div></a-tooltip>
      </template>
      <template v-else-if="column.key === 'level'"><DictTag type="ecom_replenish_level" :value="record.level" /></template>
      <template v-else-if="column.key === 'coverDays'">
        <span :class="{ danger: record.coverDays != null && Number(record.coverDays) < 7 }">{{ record.coverDays == null ? '-' : num(record.coverDays, 1) }}</span>
      </template>
      <template v-else-if="column.qty">{{ record[column.dataIndex] == null ? '-' : num(record[column.dataIndex], 2) }}</template>
      <template v-else-if="column.key === 'adType'"><DictTag type="ecom_ops_ad_alert_type" :value="record.type" /></template>
      <template v-else-if="column.key === 'campaign'">
        <div class="ellipsis" :title="record.campaignName">{{ record.campaignName }}</div>
        <div class="sub-text">{{ record.storeName }}</div>
      </template>
      <template v-else-if="column.cur">{{ record[column.dataIndex] == null ? '-' : `${money(record[column.dataIndex])} ${record.currency || ''}` }}</template>
      <template v-else-if="column.key === 'acos'">
        {{ pct(record.acosPercent) }} <span v-if="record.avgAcosPercent != null" class="sub-text">/ {{ pct(record.avgAcosPercent) }}</span>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { OpsAbnormalType } from '@/constants/enums'
import { money, num } from '@/utils/number'
import { pct, signedPct, trendClass } from './report'

/** 日报详情中的表格：kind = stores / abnormal / reviews / stock / ads */
const props = defineProps({
  kind: { type: String, required: true },
  rows: { type: Array, default: () => [] }
})
const { t } = useI18n()
const col = (key, title, width, extra = {}) => ({ key, dataIndex: key, title: t(title), width, ...extra })

const defs = {
  stores: [
    col('store', 'ecomAsst.common.store', 200),
    col('orders', 'ecomAsst.report.orders', 80, { align: 'right' }),
    col('sales', 'ecomAsst.report.sales', 120, { align: 'right', money: true }),
    col('salesChange', 'ecomAsst.detail.salesChange', 100, { align: 'right', trend: true }),
    col('profit', 'ecomAsst.report.profit', 120, { align: 'right', money: true }),
    col('profitChange', 'ecomAsst.detail.profitChange', 100, { align: 'right', trend: true }),
    col('marginPercent', 'ecomAsst.detail.marginCol', 90, { align: 'right', pct: true }),
    col('adSpend', 'ecomAsst.detail.adSpend', 110, { align: 'right', money: true }),
    col('acosPercent', 'ecomAsst.detail.acos', 80, { align: 'right', pct: true })
  ],
  abnormal: [
    col('abnormalType', 'ecomAsst.detail.abnormalType', 110),
    col('storeName', 'ecomAsst.common.store', 150),
    col('sku', 'ecomAsst.detail.sku', 240),
    col('compare', 'ecomAsst.detail.compare', 170, { align: 'right' }),
    col('abChange', 'ecomAsst.detail.change', 100, { align: 'right' }),
    col('clues', 'ecomAsst.detail.clues', 280)
  ],
  reviews: [
    col('rating', 'ecomAsst.detail.rating', 130),
    col('storeName', 'ecomAsst.common.store', 150),
    col('sellerSku', 'ecomAsst.detail.sku', 150),
    col('review', 'ecomAsst.detail.review', 420),
    col('reviewDate', 'ecomAsst.detail.reviewDate', 110)
  ],
  stock: [
    col('level', 'ecomAsst.detail.level', 90),
    col('storeName', 'ecomAsst.common.store', 150),
    col('sku', 'ecomAsst.detail.sku', 240),
    col('dailySales', 'ecomAsst.detail.dailySales', 90, { align: 'right', qty: true }),
    col('platformQty', 'ecomAsst.detail.platformQty', 90, { align: 'right', qty: true }),
    col('inTransitQty', 'ecomAsst.detail.inTransitQty', 90, { align: 'right', qty: true }),
    col('coverDays', 'ecomAsst.detail.coverDays', 90, { align: 'right' }),
    col('stockoutDate', 'ecomAsst.detail.stockoutDate', 110),
    col('suggestQty', 'ecomAsst.detail.suggestQty', 100, { align: 'right', qty: true })
  ],
  ads: [
    col('adType', 'ecomAsst.detail.adType', 110),
    col('campaign', 'ecomAsst.detail.campaign', 260),
    col('spend', 'ecomAsst.detail.spend', 120, { align: 'right', cur: true }),
    col('budget', 'ecomAsst.detail.budget', 120, { align: 'right', cur: true }),
    col('sales', 'ecomAsst.detail.adSales', 120, { align: 'right', cur: true }),
    col('orders', 'ecomAsst.report.orders', 70, { align: 'right' }),
    col('acos', 'ecomAsst.detail.acosVsAvg', 140, { align: 'right' })
  ]
}
const columns = computed(() => defs[props.kind] || [])
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))
const rowKey = (r) => `${r.storeId || ''}|${r.sellerSku || r.campaignId || r.id || ''}|${r.type || ''}`

/** 异常 SKU 的原因线索：件数、广告花费、退款的变化 */
function clues(r) {
  const parts = []
  if (r.avgQty != null) parts.push(t('ecomAsst.detail.clueQty', { cur: num(r.qty, 2), avg: num(r.avgQty, 2) }))
  if (r.avgAdSpend != null && (Number(r.adSpend) || Number(r.avgAdSpend))) parts.push(t('ecomAsst.detail.clueAd', { cur: money(r.adSpend), avg: money(r.avgAdSpend) }))
  if (Number(r.refund)) parts.push(t('ecomAsst.detail.clueRefund', { v: money(r.refund) }))
  if (r.type === OpsAbnormalType.RETURN_RATE_UP && r.qty != null) parts.push(t('ecomAsst.detail.clueReturns', { n: num(r.qty) }))
  return parts.join(t('ecomAsst.common.clueSep')) || '-'
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.ellipsis {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.review-title {
  font-weight: 600;
}
.rate {
  font-size: 12px;
}
.rate :deep(.ant-rate-star:not(:last-child)) {
  margin-inline-end: 2px;
}
.danger,
.trend-down {
  color: #cf1322;
}
.trend-up {
  color: #389e0d;
}
</style>
