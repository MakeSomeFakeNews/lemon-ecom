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
          <a-tooltip :title="storeId ? '' : $t('ecomFba.common.pickStoreFirst')">
            <a-button v-perm="'ecom:fbaClaim:sync'" :disabled="!storeId" @click="sync(storeId)">
              <template #icon><CloudSyncOutlined /></template>{{ $t('ecomFba.claim.sync') }}
            </a-button>
          </a-tooltip>
          <a-button v-perm="'ecom:fbaClaim:sync'" @click="uploadRef.open(storeId, AmzReportType.LEDGER_DETAIL)">
            <template #icon><UploadOutlined /></template>{{ $t('ecomFba.common.uploadReport') }}
          </a-button>
          <a-button v-perm="'ecom:fbaClaim:edit'" @click="match(storeId)">
            <template #icon><LinkOutlined /></template>{{ $t('ecomFba.claim.match') }}
          </a-button>
        </a-space>
      </div>
      <div class="cards">
        <div class="stat ok">
          <div class="label">{{ $t('ecomFba.claim.summaryClaimable') }}</div>
          <div class="value">{{ money(summary.claimableBase) }}</div>
        </div>
        <div class="stat">
          <div class="label">{{ $t('ecomFba.claim.summaryPaid') }}</div>
          <div class="value">{{ money(summary.paidBase) }}</div>
        </div>
        <div class="stat warn">
          <div class="label">{{ $t('ecomFba.claim.summaryExpiring') }}</div>
          <div class="value">{{ summary.expiringSoon || 0 }}</div>
        </div>
        <div class="stat wide">
          <div class="label">{{ $t('ecomFba.claim.summaryByType') }}</div>
          <a-space wrap class="types">
            <span v-for="r in summary.byType || []" :key="r.claimType">
              <DictTag type="ecom_fba_claim_type" :value="r.claimType" /> {{ r.cnt }} · {{ money(r.amountBase) }}
            </span>
            <span v-if="!(summary.byType || []).length" class="sub-text">-</span>
          </a-space>
        </div>
      </div>
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
import { onMounted, ref } from 'vue'
import { CloudSyncOutlined, LinkOutlined, ThunderboltOutlined, UploadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import AmzReportUploadModal from '@/components/ecom/AmzReportUploadModal.vue'
import ClaimList from './ClaimList.vue'
import LedgerList from './LedgerList.vue'
import ReimbursementList from './ReimbursementList.vue'
import ReceiptList from './ReceiptList.vue'
import { claimSummary, detectClaims, matchReimbursements, syncClaims } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { money } from '@/utils/number'
import { AmzReportType, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'

/**
 * FBA 索赔：汇总（可索赔、已赔付、即将到期、按类型）+ 索赔清单 / 库存流水 / 赔偿记录 / 货件对账。店铺筛选对各标签页共用
 */
const storeId = ref()
const tab = ref('claims')
const summary = ref({})
const listRef = ref()
const reimbRef = ref()
const uploadRef = ref()

async function loadSummary() {
  summary.value = (await claimSummary({ storeId: storeId.value || undefined })) || {}
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
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}
.stat {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-fill, #fafafa);
}
.stat.wide {
  grid-column: span 2;
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
.stat.ok .value {
  color: #389e0d;
}
.stat.warn .value {
  color: #d46b08;
}
.types {
  margin-top: 6px;
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
@media (max-width: 480px) {
  .stat.wide {
    grid-column: auto;
  }
}
</style>

<style>
.ecom-fba-claim .tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
html[data-theme='dark'] .ecom-fba-claim .stat {
  background: rgba(255, 255, 255, 0.04);
}
</style>
