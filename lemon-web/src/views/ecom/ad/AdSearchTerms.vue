<template>
  <a-alert type="info" show-icon :message="$t('ecomAds.searchTerm.tip')" class="tip" />
  <div class="toolbar">
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAds.searchTerm.keywordPh')" class="kw" @press-enter="load" />
    <a-checkbox v-model:checked="wasteOnly" @change="load">{{ $t('ecomAds.searchTerm.wasteOnly') }}</a-checkbox>
    <span class="sub-text">{{ $t('ecomAds.searchTerm.minClicks') }}</span>
    <a-input-number v-model:value="minClicks" :min="1" :precision="0" class="num" />
    <span class="sub-text">{{ $t('ecomAds.searchTerm.minSpend') }}</span>
    <a-input-number v-model:value="minSpend" :min="0" :precision="2" class="num" />
    <a-button @click="load"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
    <span class="spacer" />
    <a-radio-group v-model:value="negateType" size="small">
      <a-radio-button :value="EcomAdMatchType.NEGATIVE_EXACT">{{ $t('ecomAds.searchTerm.negateExact') }}</a-radio-button>
      <a-radio-button :value="EcomAdMatchType.NEGATIVE_PHRASE">{{ $t('ecomAds.searchTerm.negatePhrase') }}</a-radio-button>
    </a-radio-group>
    <a-button v-perm="'ecom:ad:edit'" type="primary" :disabled="!selected.length" :loading="negating" @click="negate(selectedRows())">
      <template #icon><StopOutlined /></template>{{ $t('ecomAds.searchTerm.negateN', { n: selected.length }) }}
    </a-button>
  </div>
  <a-table
    :row-key="rowKey"
    size="middle"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :row-selection="{ selectedRowKeys: selected, onChange: (k) => (selected = k), getCheckboxProps: (r) => ({ disabled: r.negated || !r.adGroupId }) }"
    :scroll="{ x: scrollX }"
    @change="(p) => applyPageChange(pagination, p)"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'searchTerm'">
        {{ record.searchTerm }}
        <a-tag v-if="record.negated" color="default">{{ $t('ecomAds.searchTerm.negated') }}</a-tag>
        <a-tag v-else-if="record.waste" color="red">{{ $t('ecomAds.searchTerm.waste') }}</a-tag>
      </template>
      <template v-else-if="column.key === 'campaignName'">
        {{ record.campaignName }}
        <div class="sub-text">{{ record.adGroupName || '-' }}</div>
      </template>
      <template v-else-if="column.key === 'keywordText'">
        {{ record.keywordText || '-' }}
        <div class="sub-text">{{ matchLabel(record.matchType) }}</div>
      </template>
      <template v-else-if="column.kind">{{ fmt(column.kind, record[column.key]) }}</template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="[{ type: 'disable', label: $t('ecomUiOps.ad.negate'), tip: $t('ecomAds.searchTerm.negate'), perm: 'ecom:ad:edit', show: !record.negated && !!record.adGroupId, onClick: () => negate([record]) }]" />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, StopOutlined } from '@ant-design/icons-vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { adsSearchTerms, negateSearchTerms } from '@/api/ecom/ads'
import { sumWidth } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { EcomAdMatchType } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'
import { fmt, metricColumns } from './adMetrics'

/**
 * 搜索词页签：按 活动 × 广告组 × 投放 × 搜索词 汇总（花费从高到低），标出零转化的词；勾选或单条「添加为否定关键词」
 * （有广告接口时同步到平台）
 */
const props = defineProps({ params: { type: Object, required: true } })
const dict = useDict('ecom_ad_match_type', 'ecom_ad_type')
const matchLabel = (v) => (v ? findDict(dict.ecom_ad_match_type, v)?.label || t(`ecomAds.enum.targetType.${v}`) : '-')

const keyword = ref('')
const wasteOnly = ref(true)
const minClicks = ref(10)
const minSpend = ref()
const negateType = ref(EcomAdMatchType.NEGATIVE_EXACT)
const rows = ref([])
const selected = ref([])
const loading = ref(false)
const negating = ref(false)
const pagination = reactive(localPagination(20))
const rowKey = (r) => [r.campaignId, r.adGroupId, r.keywordText, r.matchType, r.searchTerm].join('|')
const columns = [
  { title: t('ecomAds.searchTerm.term'), key: 'searchTerm', width: 240, fixed: 'left' },
  { title: t('ecomAds.common.campaign'), key: 'campaignName', width: 200, ellipsis: true },
  { title: t('ecomAds.searchTerm.keywordText'), key: 'keywordText', width: 180, ellipsis: true },
  ...metricColumns(),
  { title: t('ecomAds.common.currency'), dataIndex: 'currency', key: 'currency', width: 70 },
  actionColumn(1, [t('ecomUiOps.ad.negate')])
]
const scrollX = computed(() => sumWidth(columns))

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await adsSearchTerms({ ...props.params, keyword: keyword.value?.trim() || undefined, wasteOnly: wasteOnly.value, minClicks: minClicks.value || undefined,
      minSpend: minSpend.value || undefined })
    if (current !== seq) return
    rows.value = res || []
    selected.value = []
    pagination.current = 1
  } finally {
    if (current === seq) loading.value = false
  }
}
watch(() => props.params, load, { immediate: true })

const selectedRows = () => rows.value.filter((r) => selected.value.includes(rowKey(r)))

async function negate(list) {
  if (!list.length) return
  negating.value = true
  try {
    const r = await negateSearchTerms({ matchType: negateType.value, items: list.map((x) => ({ adGroupId: x.adGroupId, text: x.searchTerm })) })
    message.success(r.message)
    await load()
  } finally {
    negating.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.kw {
  width: 200px;
}
.num {
  width: 90px;
}
.spacer {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
