<template>
  <SubPage :open="open" :title="campaign?.name" @close="close">
    <template #extra>
      <span class="sub-text">{{ $t('ecomAds.campaign.period', { range: `${params.beginDate || ''} ~ ${params.endDate || ''}` }) }}</span>
    </template>
    <a-alert v-if="typeTip" type="info" show-icon :message="typeTip" class="type-tip" />
    <div class="section-head">
      <span class="section-title">{{ $t('ecomAds.campaign.groups') }}</span>
      <a-button v-perm="'ecom:ad:edit'" size="small" @click="groupFormRef.openCreate({ campaignId: campaign.id })">
        <template #icon><PlusOutlined /></template>{{ $t('ecomAds.group.add') }}
      </a-button>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="groupColumns"
      :data-source="groups"
      :loading="loadingGroups"
      :pagination="false"
      :scroll="{ x: sumWidth(groupColumns) }"
      :row-class-name="(r) => (r.id === groupId ? 'row-selected' : '')"
      :custom-row="(r) => ({ onClick: () => selectGroup(r) })"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'"><DictTag type="ecom_ad_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'bidOrBudget'">{{ money(record.bidOrBudget) }}</template>
        <template v-else-if="column.kind"><MetricCell :record="record" :field="column.key" :kind="column.kind" /></template>
        <template v-else-if="column.key === 'action'">
          <span @click.stop><TableActions :actions="groupActions(record)" /></span>
        </template>
      </template>
    </a-table>

    <div class="section-head targets-head">
      <span class="section-title">{{ group ? $t('ecomAds.campaign.targetList', { name: group.name }) : $t('ecomAds.campaign.targets') }}</span>
      <a-space>
        <a-input v-model:value="targetKeyword" size="small" allow-clear :placeholder="$t('ecomAds.common.keyword')" class="kw" />
        <a-button v-perm="'ecom:ad:edit'" size="small" :disabled="!group" @click="targetFormRef.openCreate({ adGroupId: group.id })">
          <template #icon><PlusOutlined /></template>{{ $t('ecomAds.target.add') }}
        </a-button>
      </a-space>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="targetColumns"
      :data-source="filteredTargets"
      :loading="loadingTargets"
      :pagination="{ pageSize: 20, showSizeChanger: true }"
      :scroll="{ x: sumWidth(targetColumns) }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          {{ record.name }}
          <div class="sub-text">{{ $t(`ecomAds.enum.targetType.${record.targetType}`) }}<template v-if="record.matchType"> · {{ matchLabel(record.matchType) }}</template></div>
        </template>
        <template v-else-if="column.key === 'status'"><DictTag type="ecom_ad_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'bidOrBudget'">
          {{ money(record.bidOrBudget) }}
          <div v-if="record.baseBid != null && Number(record.baseBid) !== Number(record.bidOrBudget)" class="sub-text">
            {{ $t('ecomAds.target.baseBid') }} {{ money(record.baseBid) }}
          </div>
        </template>
        <template v-else-if="column.kind"><MetricCell :record="record" :field="column.key" :kind="column.kind" /></template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="targetActions(record)" />
        </template>
      </template>
    </a-table>

    <div v-if="group && campaign?.adType !== EcomAdType.SD" class="negatives">
      <span class="section-title">{{ $t('ecomAds.enum.targetType.NEGATIVE') }}</span>
      <a-tag
        v-for="n in negatives"
        :key="n.id"
        :closable="userStore.hasPerm('ecom:ad:edit')"
        @close.prevent="removeTarget({ id: n.id, name: n.targetText })"
      >
        {{ n.targetText }} <span class="sub-text">{{ matchLabel(n.matchType) }}</span>
      </a-tag>
      <span v-if="!negatives.length" class="sub-text">-</span>
    </div>

    <GroupForm ref="groupFormRef" @success="reloadAll" />
    <TargetForm ref="targetFormRef" @success="loadTargets" />
    <ValueModal ref="valueRef" @success="reloadAll" />
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { adsHierarchy, changeAdStatus, changeBid, deleteGroup, deleteTargets, pageTargets } from '@/api/ecom/ads'
import { useAction } from '@/composables/useAction'
import { sumWidth } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { EcomAdLevel, EcomAdStatus, EcomAdTargetType, EcomAdType } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'
import { t } from '@/locales'
import { metricColumns } from './adMetrics'
import MetricCell from './MetricCell.vue'
import GroupForm from './GroupForm.vue'
import TargetForm from './TargetForm.vue'
import ValueModal from './ValueModal.vue'

/**
 * 活动详情（子页）：广告组层级报表（点击行查看其投放），投放层级报表；新建 / 编辑广告组和投放、修改竞价、暂停 / 启用、删除。
 * 竞价、状态的修改有广告接口时同步到平台。open(活动行) 打开，有修改时触发 changed
 */
const props = defineProps({ params: { type: Object, required: true } })
const emit = defineEmits(['changed'])
const dict = useDict('ecom_ad_match_type')
const userStore = useUserStore()
const negatives = ref([])
const matchLabel = (v) => findDict(dict.ecom_ad_match_type, v)?.label || v

const open = ref(false)
const campaign = ref(null)
const groups = ref([])
const targets = ref([])
const group = ref(null)
const groupId = computed(() => group.value?.id)
const loadingGroups = ref(false)
const loadingTargets = ref(false)
const targetKeyword = ref('')
/** 品牌推广 / 展示型推广的接口限制说明（亚马逊 SB 广告组没有默认竞价，SD 没有关键词与否定关键词） */
const typeTip = computed(() => {
  if (campaign.value?.adType === EcomAdType.SB) return t('ecomFix.ads.sbTip')
  if (campaign.value?.adType === EcomAdType.SD) return t('ecomFix.ads.sdTip')
  return ''
})
let changed = false

const groupColumns = [
  { title: t('ecomAds.group.name'), dataIndex: 'name', key: 'name', width: 200, fixed: 'left', ellipsis: true },
  { title: t('ecomAds.common.status'), key: 'status', width: 80 },
  { title: t('ecomAds.group.defaultBid'), key: 'bidOrBudget', width: 100, align: 'right' },
  ...metricColumns(),
  actionColumn(4)
]
const targetColumns = [
  { title: t('ecomAds.target.text'), key: 'name', width: 240, fixed: 'left' },
  { title: t('ecomAds.common.status'), key: 'status', width: 80 },
  { title: t('ecomAds.target.bid'), key: 'bidOrBudget', width: 110, align: 'right' },
  ...metricColumns(),
  actionColumn(3)
]
const filteredTargets = computed(() => {
  const k = targetKeyword.value?.trim().toLowerCase()
  return k ? targets.value.filter((r) => r.name?.toLowerCase().includes(k)) : targets.value
})

async function show(row) {
  campaign.value = row
  group.value = null
  targets.value = []
  changed = false
  open.value = true
  await loadGroups()
  if (groups.value.length) selectGroup(groups.value[0])
}

function close() {
  open.value = false
  if (changed) emit('changed')
}

async function loadGroups() {
  loadingGroups.value = true
  try {
    const res = await adsHierarchy({ ...props.params, level: EcomAdLevel.AD_GROUP, campaignId: campaign.value.id })
    groups.value = res?.rows || []
    if (group.value) group.value = groups.value.find((g) => g.id === group.value.id) || null
  } finally {
    loadingGroups.value = false
  }
}

async function loadTargets() {
  if (!group.value) return
  loadingTargets.value = true
  try {
    const [res, neg] = await Promise.all([
      adsHierarchy({ ...props.params, level: EcomAdLevel.TARGET, adGroupId: group.value.id }),
      pageTargets({ current: 1, size: 200, adGroupId: group.value.id, targetType: EcomAdTargetType.NEGATIVE })
    ])
    targets.value = res?.rows || []
    negatives.value = neg?.records || []
  } finally {
    loadingTargets.value = false
  }
}

function selectGroup(g) {
  group.value = g
  loadTargets()
}

async function reloadAll() {
  changed = true
  await loadGroups()
  await loadTargets()
}

const pushedKey = (pushed) => (pushed ? 'ecomAds.common.pushed' : 'ecomAds.common.localOnly')
const toggle = useAction((level, r) => changeAdStatus(level, r.id, r.status === EcomAdStatus.ENABLED ? EcomAdStatus.PAUSED : EcomAdStatus.ENABLED), {
  successKey: pushedKey,
  reload: () => reloadAll()
})
const removeGroup = useAction((r) => deleteGroup(r.id), {
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    group.value = null
    targets.value = []
    return reloadAll()
  }
})
const removeTarget = useAction((r) => deleteTargets(r.id), { successKey: 'common.msg.deleteSuccess', reload: () => reloadAll() })

const valueRef = ref()
const groupFormRef = ref()
const targetFormRef = ref()

function editBid(level, r) {
  valueRef.value.open({
    title: t('ecomAds.target.changeBid'),
    label: level === EcomAdLevel.TARGET ? t('ecomAds.target.baseBid') : t('ecomAds.group.defaultBid'),
    value: level === EcomAdLevel.TARGET ? (r.baseBid ?? r.bidOrBudget) : r.bidOrBudget,
    currency: r.currency,
    tip: level === EcomAdLevel.TARGET ? t('ecomAds.target.bidTip') : '',
    submit: (v) => changeBid(level, r.id, v)
  })
}

const statusAction = (level, r) => ({
  type: r.status === EcomAdStatus.ENABLED ? 'disable' : 'enable',
  label: r.status === EcomAdStatus.ENABLED ? t('ecomAds.campaign.pause') : t('ecomAds.campaign.enable'),
  perm: 'ecom:ad:edit',
  show: r.status !== EcomAdStatus.ARCHIVED,
  onClick: () => toggle(level, r)
})

const groupActions = (r) => [
  {
    type: 'setting',
    label: t('ecomAds.target.changeBid'),
    perm: 'ecom:ad:edit',
    show: r.status !== EcomAdStatus.ARCHIVED && campaign.value?.adType !== EcomAdType.SB,
    onClick: () => editBid(EcomAdLevel.AD_GROUP, r)
  },
  statusAction(EcomAdLevel.AD_GROUP, r),
  { type: 'edit', perm: 'ecom:ad:edit', onClick: () => groupFormRef.value.openEdit({ id: r.id, campaignId: campaign.value.id, name: r.name, platformAdGroupId: r.platformId || '' }) },
  { type: 'delete', perm: 'ecom:ad:edit', confirm: t('ecomAds.group.confirmDelete', { name: r.name }), onClick: () => removeGroup(r) }
]
const targetActions = (r) => [
  {
    type: 'setting',
    label: t('ecomAds.target.changeBid'),
    perm: 'ecom:ad:edit',
    show: r.status !== EcomAdStatus.ARCHIVED && r.targetType !== EcomAdTargetType.NEGATIVE,
    onClick: () => editBid(EcomAdLevel.TARGET, r)
  },
  statusAction(EcomAdLevel.TARGET, r),
  { type: 'edit', perm: 'ecom:ad:edit', onClick: () => targetFormRef.value.openEdit({ ...r, adGroupId: group.value.id }) },
  { type: 'delete', perm: 'ecom:ad:edit', confirm: t('ecomAds.common.deleteConfirm', { name: r.name }), onClick: () => removeTarget(r) }
]

defineExpose({ open: show })
</script>

<style scoped>
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.targets-head {
  margin-top: 20px;
}
.section-title {
  font-weight: 600;
}
.kw {
  width: 180px;
}
.type-tip {
  margin-bottom: 12px;
}
.negatives {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.row-selected > td) {
  background: var(--app-hover) !important;
}
</style>
