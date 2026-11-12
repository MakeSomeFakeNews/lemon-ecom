<template>
  <div class="toolbar">
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAds.campaign.keywordPh')" class="kw" @press-enter="load" />
    <a-button @click="load"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
    <span class="spacer" />
    <a-button v-perm="'ecom:ad:import'" @click="importRef.open(params.storeId)">
      <template #icon><ImportOutlined /></template>{{ $t('ecomAds.campaign.import') }}
    </a-button>
    <a-button v-perm="'ecom:ad:edit'" type="primary" @click="formRef.openCreate({ storeId: params.storeId })">
      <template #icon><PlusOutlined /></template>{{ $t('ecomAds.campaign.add') }}
    </a-button>
  </div>
  <a-table
    row-key="id"
    size="middle"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="(p) => applyPageChange(pagination, p)"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        <a @click="detailRef.open(record)">{{ record.name }}</a>
        <div class="sub-text">{{ record.storeName }}</div>
      </template>
      <template v-else-if="column.key === 'adType'"><DictTag type="ecom_ad_type" :value="record.adType" /></template>
      <template v-else-if="column.key === 'status'"><DictTag type="ecom_ad_status" :value="record.status" /></template>
      <template v-else-if="column.key === 'bidOrBudget'">{{ money(record.bidOrBudget) }} <span class="sub-text">{{ record.currency }}</span></template>
      <template v-else-if="column.kind"><MetricCell :record="record" :field="column.key" :kind="column.kind" /></template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="rowActions(record)" />
      </template>
    </template>
  </a-table>

  <CampaignForm ref="formRef" @success="load" />
  <ValueModal ref="valueRef" @success="load" />
  <AdReportImport ref="importRef" @success="load" />
  <AdCampaignDetail ref="detailRef" :params="params" @changed="load" />
  <ScheduleEditor ref="scheduleRef" @success="load" />
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ImportOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { adsHierarchy, changeAdStatus, changeBudget, deleteCampaigns } from '@/api/ecom/ads'
import { useAction } from '@/composables/useAction'
import { sumWidth } from '@/composables/useColumns'
import { EcomAdLevel, EcomAdStatus } from '@/constants/enums'
import { money } from '@/utils/number'
import { t } from '@/locales'
import { metricColumns } from './adMetrics'
import MetricCell from './MetricCell.vue'
import CampaignForm from './CampaignForm.vue'
import ValueModal from './ValueModal.vue'
import AdReportImport from './AdReportImport.vue'
import AdCampaignDetail from './AdCampaignDetail.vue'
import ScheduleEditor from './ScheduleEditor.vue'

/**
 * 活动页签：活动级层级报表（GET /ecom/ads/hierarchy?level=CAMPAIGN，含环比），点名称进入活动详情（广告组 → 投放）；
 * 新建 / 编辑活动、修改预算、暂停 / 启用、分时策略、删除，导入广告后台报告
 */
const props = defineProps({ params: { type: Object, required: true } })

const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const pagination = reactive(localPagination(20))
const columns = [
  { title: t('ecomAds.common.campaign'), key: 'name', width: 240, fixed: 'left' },
  { title: t('ecomAds.campaign.adType'), key: 'adType', width: 120 },
  { title: t('ecomAds.common.status'), key: 'status', width: 90 },
  { title: t('ecomAds.campaign.dailyBudget'), key: 'bidOrBudget', width: 130, align: 'right' },
  ...metricColumns(),
  actionColumn(4)
]
const scrollX = computed(() => sumWidth(columns))

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await adsHierarchy({ ...props.params, level: EcomAdLevel.CAMPAIGN, keyword: keyword.value?.trim() || undefined })
    if (current !== seq) return
    rows.value = res?.rows || []
  } finally {
    if (current === seq) loading.value = false
  }
}
watch(() => props.params, load, { immediate: true })

const formRef = ref()
const valueRef = ref()
const importRef = ref()
const detailRef = ref()
const scheduleRef = ref()

const toggle = useAction((r) => changeAdStatus(EcomAdLevel.CAMPAIGN, r.id, r.status === EcomAdStatus.ENABLED ? EcomAdStatus.PAUSED : EcomAdStatus.ENABLED), {
  successKey: (pushed) => (pushed ? 'ecomAds.common.pushed' : 'ecomAds.common.localOnly'),
  reload: () => load()
})
const remove = useAction((r) => deleteCampaigns(r.id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

function editBudget(r) {
  valueRef.value.open({
    title: t('ecomAds.campaign.changeBudget'),
    label: t('ecomAds.campaign.dailyBudget'),
    value: r.bidOrBudget,
    currency: r.currency,
    submit: (v) => changeBudget(r.id, v)
  })
}

const rowActions = (r) => [
  { type: 'detail', onClick: () => detailRef.value.open(r) },
  { type: 'edit', perm: 'ecom:ad:edit', onClick: () => formRef.value.openEdit(r) },
  { type: 'setting', label: t('ecomAds.campaign.changeBudget'), perm: 'ecom:ad:edit', show: r.status !== EcomAdStatus.ARCHIVED, onClick: () => editBudget(r) },
  {
    type: r.status === EcomAdStatus.ENABLED ? 'disable' : 'enable',
    label: r.status === EcomAdStatus.ENABLED ? t('ecomAds.campaign.pause') : t('ecomAds.campaign.enable'),
    perm: 'ecom:ad:edit',
    show: r.status !== EcomAdStatus.ARCHIVED,
    onClick: () => toggle(r)
  },
  { type: 'history', label: t('ecomAds.campaign.schedule'), perm: 'ecom:ad:schedule', onClick: () => scheduleRef.value.open(r.id) },
  { type: 'delete', perm: 'ecom:ad:edit', confirm: t('ecomAds.campaign.confirmDelete', { name: r.name }), onClick: () => remove(r) }
]
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.kw {
  width: 240px;
}
.spacer {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
