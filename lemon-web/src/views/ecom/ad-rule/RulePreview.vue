<template>
  <a-modal v-model:open="open" :title="$t('ecomAds.rule.previewTitle', { name: rule?.name || '' })" :footer="null" width="min(1100px, 96vw)">
    <a-alert type="info" show-icon :message="$t('ecomAds.rule.previewTip')" class="tip" />
    <a-table
      :row-key="(r) => `${r.level}-${r.entityId}`"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="{ pageSize: 20 }"
      :scroll="{ x: 1200 }"
      :locale="{ emptyText: $t('ecomAds.rule.previewEmpty') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'entityName'">
          {{ record.entityName }}
          <div class="sub-text">{{ record.storeName }} · {{ record.campaignName }}</div>
        </template>
        <template v-else-if="column.key === 'change'">{{ changeText(record) }}</template>
        <template v-else-if="column.metric">{{ fmt(column.kind, record.metrics?.[column.metric]) }}</template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { previewAdRule } from '@/api/ecom/adRule'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'
import { fmt } from '../ad/adMetrics'

/** 规则预览（只计算不保存）：命中的对象、指标与调整（原值 → 新值，有分时系数时显示平台竞价）。open(规则) 打开 */
const dict = useDict('ecom_ad_status', 'ecom_ad_rule_action')
const open = ref(false)
const loading = ref(false)
const rule = ref(null)
const rows = ref([])
const m = (metric, kind) => ({ title: t(`ecomAds.metric.${metric}`), key: metric, metric, kind, width: 90, align: 'right' })
const columns = [
  { title: t('ecomAds.log.object'), key: 'entityName', width: 260, fixed: 'left' },
  { title: t('ecomAds.log.change'), key: 'change', width: 220 },
  m('impressions', 'int'),
  m('clicks', 'int'),
  m('spend', 'money'),
  m('sales', 'money'),
  m('orders', 'int'),
  m('acos', 'percent'),
  m('roas', 'ratio'),
  m('ctr', 'percent'),
  m('cpc', 'money'),
  m('cvr', 'percent')
]

/** 调整说明：竞价 / 预算 原值 → 新值（平台竞价），状态 原 → 新，仅提醒显示动作名称 */
function changeText(r) {
  const action = findDict(dict.ecom_ad_rule_action, r.action)?.label || r.action
  if (r.newValue != null) {
    const eff = r.effectiveBid != null && Number(r.effectiveBid) !== Number(r.newValue) ? ` (${t('ecomAds.rule.effectiveBid')} ${fmt('money', r.effectiveBid)})` : ''
    return `${action}: ${fmt('money', r.oldValue)} → ${fmt('money', r.newValue)}${eff}`
  }
  if (r.newStatus) {
    const label = (s) => findDict(dict.ecom_ad_status, s)?.label || s
    return `${label(r.oldStatus)} → ${label(r.newStatus)}`
  }
  return action
}

async function show(r) {
  rule.value = r
  rows.value = []
  open.value = true
  loading.value = true
  try {
    rows.value = (await previewAdRule(r.id)) || []
  } finally {
    loading.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
