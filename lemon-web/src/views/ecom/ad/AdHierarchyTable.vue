<template>
  <div class="hier">
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="rows.length > 20 ? { pageSize: 20, size: 'small' } : false"
      :scroll="{ x: sumWidth(columns) }"
      :locale="{ emptyText: level === EcomAdLevel.AD_GROUP ? $t('ecomUiOps.ad.noGroups') : $t('ecomUiOps.ad.noTargets') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="name-cell" :title="record.name">
            <span class="level-tag">{{ levelLabel }}</span>
            <span class="name-text">{{ record.name }}</span>
          </div>
          <div v-if="level === EcomAdLevel.TARGET" class="sub-text">
            {{ $t(`ecomAds.enum.targetType.${record.targetType}`) }}<template v-if="record.matchType"> · {{ matchLabel(record.matchType) }}</template>
          </div>
        </template>
        <template v-else-if="column.key === 'status'"><DictTag type="ecom_ad_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'bidOrBudget'">{{ money(record.bidOrBudget) }} <span class="sub-text">{{ record.currency }}</span></template>
        <template v-else-if="column.kind"><MetricCell :record="record" :field="column.key" :kind="column.kind" /></template>
      </template>
      <template v-if="level === EcomAdLevel.AD_GROUP" #expandedRowRender="{ record }">
        <AdHierarchyTable :level="EcomAdLevel.TARGET" :parent-id="record.id" :params="params" :metric-keys="metricKeys" />
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { adsHierarchy } from '@/api/ecom/ads'
import { useDict } from '@/composables/useDict'
import { sumWidth } from '@/composables/useColumns'
import { EcomAdLevel } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { money } from '@/utils/number'
import { t } from '@/locales'
import { metricColumns } from './adMetrics'
import MetricCell from './MetricCell.vue'

/**
 * 活动表格展开的下级：广告组（再展开为投放）或投放的指标报表（GET /ecom/ads/hierarchy，含环比），
 * 指标列跟随活动表格的列设置（metricKeys）；维护操作在活动详情中
 */
defineOptions({ name: 'AdHierarchyTable' })
const props = defineProps({
  level: { type: String, required: true },
  parentId: { type: [Number, String], required: true },
  params: { type: Object, required: true },
  metricKeys: { type: Array, default: () => [] }
})
const dict = useDict('ecom_ad_match_type')
const matchLabel = (v) => findDict(dict.ecom_ad_match_type, v)?.label || v
const levelLabel = computed(() => t(props.level === EcomAdLevel.AD_GROUP ? 'ecomAds.campaign.groups' : 'ecomAds.campaign.targets'))

const columns = computed(() => [
  { title: t(props.level === EcomAdLevel.AD_GROUP ? 'ecomAds.group.name' : 'ecomAds.target.text'), key: 'name', width: 260 },
  { title: t('ecomAds.common.status'), key: 'status', width: 90 },
  { title: t(props.level === EcomAdLevel.AD_GROUP ? 'ecomAds.group.defaultBid' : 'ecomAds.target.bid'), key: 'bidOrBudget', width: 120, align: 'right' },
  ...metricColumns().filter((c) => !props.metricKeys.length || props.metricKeys.includes(c.key))
])

const rows = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const parent = props.level === EcomAdLevel.AD_GROUP ? { campaignId: props.parentId } : { adGroupId: props.parentId }
    const res = await adsHierarchy({ ...props.params, level: props.level, ...parent })
    rows.value = res?.rows || []
  } finally {
    loading.value = false
  }
}
watch(() => [props.params, props.parentId], load, { immediate: true })
</script>

<style scoped>
.hier {
  padding: 4px 0 4px 8px;
}
.name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.level-tag {
  flex: none;
  padding: 0 4px;
  border-radius: 3px;
  background: var(--app-hover);
  color: var(--app-text-3);
  font-size: 11px;
  line-height: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
