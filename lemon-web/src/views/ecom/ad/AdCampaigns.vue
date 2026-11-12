<template>
  <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAds.campaign.keywordPh')" class="kw" @press-enter="load">
      <template #prefix><SearchOutlined class="sub-text" /></template>
    </a-input>
    <a-select v-model:value="adType" allow-clear :placeholder="$t('ecomUiOps.ad.allTypes')" :options="toOptions(dict.ecom_ad_type)" class="filter" />
    <a-select v-model:value="status" allow-clear :placeholder="$t('ecomUiOps.ad.allStatus')" :options="toOptions(dict.ecom_ad_status)" class="filter" />
    <a-button v-perm="'ecom:ad:edit'" type="primary" @click="formRef.openCreate({ storeId: params.storeId })">
      <template #icon><PlusOutlined /></template>{{ $t('ecomAds.campaign.add') }}
    </a-button>
    <a-button v-perm="'ecom:ad:import'" @click="importRef.open(params.storeId)">
      <template #icon><ImportOutlined /></template>{{ $t('ecomAds.campaign.import') }}
    </a-button>
  </TableToolbar>
  <a-table
    row-key="id"
    :size="size"
    :columns="visibleColumns"
    :data-source="filtered"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="(p) => applyPageChange(pagination, p)"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        <a class="name-link" :title="record.name" @click="detailRef.open(record)">{{ record.name }}</a>
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
    <template #expandedRowRender="{ record }">
      <AdHierarchyTable :level="EcomAdLevel.AD_GROUP" :parent-id="record.id" :params="params" :metric-keys="metricKeys" />
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
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { adsHierarchy, changeAdStatus, changeBudget, deleteCampaigns } from '@/api/ecom/ads'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
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
import AdHierarchyTable from './AdHierarchyTable.vue'

/**
 * 活动页签：活动级层级报表（GET /ecom/ads/hierarchy?level=CAMPAIGN，含环比），点名称进入活动详情（广告组 → 投放）；
 * 新建 / 编辑活动、修改预算、暂停 / 启用、分时策略、删除，导入广告后台报告
 */
const props = defineProps({ params: { type: Object, required: true } })

const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const pagination = reactive(localPagination(20))
const dict = useDict('ecom_ad_type', 'ecom_ad_status')
const adType = ref()
const status = ref()
const size = ref('middle')
const columns = [
  { title: t('ecomAds.common.campaign'), key: 'name', width: 240, fixed: 'left' },
  { title: t('ecomAds.campaign.adType'), key: 'adType', width: 120 },
  { title: t('ecomAds.common.status'), key: 'status', width: 90 },
  { title: t('ecomAds.campaign.dailyBudget'), key: 'bidOrBudget', width: 130, align: 'right' },
  ...metricColumns(),
  actionColumn(4)
]
// 列设置（含指标列）记在本浏览器；展开的广告组 / 投放跟随显示的指标列
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-ad-campaigns')
const metricKeys = computed(() => checked.value)
/** 类型、状态在前端筛选（层级报表一次返回全部活动） */
const filtered = computed(() => rows.value.filter((r) => (!adType.value || r.adType === adType.value) && (!status.value || r.status === status.value)))
watch([adType, status], () => (pagination.current = 1))

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
.kw {
  width: 240px;
}
.filter {
  width: 140px;
}
.name-link {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
