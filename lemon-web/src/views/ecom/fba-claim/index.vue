<template>
  <div class="table-page ecom-fba-claim">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-space wrap>
          <span>{{ $t('ecomFba.common.store') }}</span>
          <StoreSelect v-model:value="storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomFba.common.allStores')" class="store" @change="loadSummary" />
        </a-space>
        <a-space wrap>
          <a-button v-perm="'ecom:fbaClaim:add'" type="primary" @click="detect(storeId)">
            <template #icon><ThunderboltOutlined /></template>{{ $t('ecomFba.claim.detect') }}
          </a-button>
          <a-dropdown v-if="userStore.hasPerm('ecom:fbaClaim:sync')">
            <a-button><template #icon><CloudSyncOutlined /></template>{{ $t('ecomFba.claim.dataOps') }} <DownOutlined /></a-button>
            <template #overlay>
              <a-menu @click="onDataMenu">
                <a-menu-item key="sync" :disabled="!storeId" :title="storeId ? '' : $t('ecomFba.common.pickStoreFirst')">
                  <CloudSyncOutlined /> {{ $t('ecomFba.claim.sync') }}
                </a-menu-item>
                <a-menu-item key="upload"><UploadOutlined /> {{ $t('ecomFba.common.uploadReport') }}</a-menu-item>
                <a-menu-item v-if="can('ecom:fbaClaim:edit')" key="match"><LinkOutlined /> {{ $t('ecomFba.claim.match') }}</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </div>
      <a-spin :spinning="summaryLoading">
        <div class="kpis">
          <div class="kpi ok">
            <div class="kpi-label"><SafetyCertificateOutlined /> {{ $t('ecomFba.claim.summaryClaimable') }}</div>
            <div class="kpi-value">{{ symMoney(summary.claimableBase ?? 0) }}</div>
            <div class="kpi-sub">{{ $t('ecomFba.claim.openCount', { p: statusCount(FbaClaimStatus.PENDING), s: statusCount(FbaClaimStatus.SUBMITTED) }) }}</div>
          </div>
          <div class="kpi">
            <div class="kpi-label"><CheckCircleOutlined /> {{ $t('ecomFba.claim.summaryPaid') }}</div>
            <div class="kpi-value">{{ symMoney(summary.paidBase ?? 0) }}</div>
            <div class="kpi-sub">{{ $t('ecomFba.claim.paidCount', { n: statusCount(FbaClaimStatus.PAID), r: statusCount(FbaClaimStatus.REJECTED) }) }}</div>
          </div>
          <div class="kpi warn clickable" :class="{ hot: summary.expiringSoon > 0 }" @click="filterExpiring(nearest.count30 && !summary.expiringSoon ? 30 : 7)">
            <div class="kpi-label"><ClockCircleOutlined /> {{ $t('ecomFba.claim.deadlineCard') }}</div>
            <div class="kpi-value">
              {{ summary.expiringSoon || 0 }}<span class="kpi-unit">{{ $t('ecomFba.claim.within7') }}</span>
              <span class="kpi-second">{{ nearest.count30 }}<span class="kpi-unit">{{ $t('ecomFba.claim.within30') }}</span></span>
            </div>
            <div class="kpi-sub">
              <template v-if="nearest.deadline">{{ $t('ecomFba.claim.nearest', { date: nearest.deadline, n: nearest.daysLeft }) }}</template>
              <template v-else>{{ $t('ecomFba.claim.noExpiring') }}</template>
            </div>
          </div>
          <div class="kpi wide">
            <div class="kpi-label"><PieChartOutlined /> {{ $t('ecomFba.claim.summaryByType') }}</div>
            <div class="types">
              <div v-for="r in byType" :key="r.claimType" class="type-row">
                <DictTag type="ecom_fba_claim_type" :value="r.claimType" />
                <span class="type-num">{{ r.cnt }} · {{ symMoney(r.amountBase) }}</span>
              </div>
              <span v-if="!byType.length" class="sub-text">-</span>
            </div>
          </div>
        </div>
      </a-spin>
      <a-alert v-if="summary.missingRates?.length" type="warning" show-icon class="alert" :message="$t('ecomFba.common.missingRates', { list: summary.missingRates.join(', ') })" />
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="claim-tabs">
        <a-tab-pane key="claims" :tab="$t('ecomFba.claim.tabs.claims')" />
        <a-tab-pane key="ledger" :tab="$t('ecomFba.claim.tabs.ledger')" />
        <a-tab-pane key="reimbursement" :tab="$t('ecomFba.claim.tabs.reimbursement')" />
        <a-tab-pane key="receipt" :tab="$t('ecomFba.claim.tabs.receipt')" />
      </a-tabs>
      <ClaimList v-if="tab === 'claims'" ref="listRef" :store-id="storeId" :summary="summary" @changed="loadSummary" />
      <LedgerList v-else-if="tab === 'ledger'" :store-id="storeId" />
      <ReimbursementList v-else-if="tab === 'reimbursement'" ref="reimbRef" :store-id="storeId" />
      <ReceiptList v-else :store-id="storeId" />
    </a-card>

    <AmzReportUploadModal ref="uploadRef" :types="[AmzReportType.LEDGER_DETAIL, AmzReportType.REIMBURSEMENTS]" @success="afterReport" />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  CheckCircleOutlined, ClockCircleOutlined, CloudSyncOutlined, DownOutlined, LinkOutlined, PieChartOutlined, SafetyCertificateOutlined, ThunderboltOutlined, UploadOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import AmzReportUploadModal from '@/components/ecom/AmzReportUploadModal.vue'
import ClaimList from './ClaimList.vue'
import LedgerList from './LedgerList.vue'
import ReimbursementList from './ReimbursementList.vue'
import ReceiptList from './ReceiptList.vue'
import { claimSummary, detectClaims, matchReimbursements, pageClaim, syncClaims } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { money, symMoney } from '@/utils/number'
import { AmzReportType, EcomPlatform, FbaClaimStatus } from '@/constants/enums'
import { useCan } from '../fba-health/fba'
import { t } from '@/locales'
import { useUserStore } from '@/store/user'

/** a-dropdown 的根不是元素，v-perm 不生效，改用 v-if 判断权限 */
const userStore = useUserStore()

/**
 * FBA 索赔：汇总（可索赔、已赔付、即将到期、按类型）+ 索赔清单 / 库存流水 / 赔偿记录 / 货件对账。店铺筛选对各标签页共用
 */
const storeId = ref()
const tab = ref('claims')
const summary = ref({})
const listRef = ref()
const reimbRef = ref()
const uploadRef = ref()

const can = useCan()
const summaryLoading = ref(false)
/** 30 天内到期（未处理）的条数与最近的截止日期：清单按未处理优先、截止日期升序，取第一条 */
const nearest = ref({ count30: 0 })
const byType = computed(() => summary.value.byType || [])
const statusCount = (s) => (summary.value.byStatus || []).find((x) => x.status === s)?.cnt || 0

async function loadSummary() {
  summaryLoading.value = true
  try {
    const params = { storeId: storeId.value || undefined }
    const [sum, near] = await Promise.all([claimSummary(params), pageClaim({ ...params, expiringDays: 30, current: 1, size: 1 })])
    summary.value = sum || {}
    const first = near?.records?.[0]
    nearest.value = { count30: near?.total || 0, deadline: first?.deadline, daysLeft: first?.daysLeft }
  } finally {
    summaryLoading.value = false
  }
}

/** 点击「临近截止」卡：切到索赔清单并筛选 n 天内到期 */
async function filterExpiring(days) {
  tab.value = 'claims'
  await nextTick()
  listRef.value?.filterExpiring(days)
}

function onDataMenu({ key }) {
  if (key === 'sync') sync(storeId.value)
  else if (key === 'upload') uploadRef.value.open(storeId.value, AmzReportType.LEDGER_DETAIL)
  else match(storeId.value)
}
onMounted(loadSummary)

function afterReport() {
  loadSummary()
  listRef.value?.reload()
  reimbRef.value?.reload()
}

const detect = useAction((id) => detectClaims(id || undefined), {
  confirm: t('ecomFba.claim.detectConfirm'),
  successKey: (r) => t('ecomFba.claim.detectDone', { n: r?.total || 0 }),
  reload: afterReport
})
const sync = useAction(syncClaims, { successKey: (ids) => t('ecomFba.common.syncCreated', { n: (ids || []).length }) })
const match = useAction((id) => matchReimbursements(id || undefined), {
  successKey: (n) => t('ecomFba.claim.matchDone', { n: n || 0 }),
  reload: afterReport
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.store {
  width: 240px;
}
.kpis {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1.2fr) minmax(0, 1.6fr);
  gap: 12px;
}
@media (max-width: 1200px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 560px) {
  .kpis {
    grid-template-columns: minmax(0, 1fr);
  }
}
.kpi {
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
  border-left: 3px solid var(--app-primary);
}
.kpi.ok {
  border-left-color: #52c41a;
}
.kpi.warn {
  border-left-color: var(--status-warning);
}
.kpi.hot {
  border-left-color: var(--status-critical);
  background: rgba(245, 34, 45, 0.04);
}
.kpi.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.kpi.clickable:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.kpi-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-text-2);
  font-size: 13px;
}
.kpi-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.3;
  font-variant-numeric: tabular-nums;
}
.kpi.ok .kpi-value {
  color: #389e0d;
}
.kpi.warn .kpi-value {
  color: #d46b08;
}
.kpi.hot .kpi-value {
  color: #cf1322;
}
.kpi-unit {
  margin-left: 4px;
  font-size: 12px;
  font-weight: normal;
  color: var(--app-text-3);
}
.kpi-second {
  margin-left: 16px;
  font-size: 18px;
}
.kpi-sub {
  margin-top: 2px;
  color: var(--app-text-3);
  font-size: 12px;
}
.types {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 6px 16px;
  margin-top: 8px;
}
.type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.type-row :deep(.ant-tag) {
  margin: 0;
  min-width: 76px;
  text-align: center;
}
.type-num {
  flex: 1;
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
}
.alert {
  margin-top: 10px;
}
.claim-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
</style>

<style>
.ecom-fba-claim .tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
html[data-theme='dark'] .ecom-fba-claim .kpi.hot {
  background: rgba(245, 34, 45, 0.08);
}
html[data-theme='dark'] .ecom-fba-claim .kpi.ok .kpi-value {
  color: #6abe39;
}
html[data-theme='dark'] .ecom-fba-claim .kpi.warn .kpi-value {
  color: #f0a64b;
}
</style>
