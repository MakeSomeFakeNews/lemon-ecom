<template>
  <SubPage :open="open" :title="detail ? $t('ecomOwh.ret.detailTitle', { no: detail.returnNo }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_owh_return_status" :value="detail.status" />
      <a-tag v-if="detail.erpReceived === 1" color="green" :bordered="false">{{ $t('ecomOwh.ret.erpReceived') }}</a-tag>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.errorMsg" type="warning" show-icon class="block" :message="$t('ecomOwh.common.errorMsg')" :description="detail.errorMsg" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOwh.ret.ecomReturnNo')">
            <a @click="router.push({ path: '/ecom/return', query: { keyword: detail.ecomReturnNo } })">{{ detail.ecomReturnNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.orderNo')">{{ detail.platformOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.account')">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.warehouse')">{{ detail.warehouseName }}（{{ detail.owhCode }}）</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.owhOrderNo')">{{ detail.owhOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.carrier')">{{ detail.carrier || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.owhStatus')">{{ detail.owhStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.result')">{{ $t('ecomOwh.ret.resultText', { good: qty(detail.goodQty), bad: qty(detail.badQty), total: qty(detail.totalQty) }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.defectDoc')">{{ detail.defectDocNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.ret.receiveTime')">{{ detail.receiveTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('ecomOwh.ret.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 700 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }}</div>
            </template>
            <template v-else-if="['qty', 'goodQty', 'badQty'].includes(column.key)">{{ qty(record[column.key]) }}</template>
          </template>
        </a-table>
        <OwhDiffList :source-type="OwhDiffSource.RETURN" :source-id="detail.id" :refresh-key="detail.updateTime" />
      </template>
    </a-spin>
    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">{{ a.label }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getOwhReturn } from '@/api/ecom/owh'
import OwhDiffList from '@/components/ecom/owh/OwhDiffList.vue'
import { OwhDiffSource } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'
import { canSyncReturn, isReturnDraft, isReturnPushed, useOwhCaps } from '../owh-outbound/owh'

/** 海外仓退件单详情：ERP 退货单、服务商单号、退件运单、质检结果（良品 / 不良品）与不良品处理单。open(id)、reload() */
const emit = defineEmits(['push', 'sync', 'cancel', 'void'])
const hasCap = useOwhCaps()
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const open = ref(false)
const loading = ref(false)
const detail = ref(null)
let currentId = null

const itemColumns = [
  { title: t('ecomOwh.ret.sku'), dataIndex: 'sku', width: 160 },
  { title: t('ecomOwh.stock.material'), key: 'material', width: 220 },
  { title: t('ecomOwh.ret.qty'), key: 'qty', width: 90, align: 'right' },
  { title: t('ecomOwh.ret.goodQty'), key: 'goodQty', width: 90, align: 'right' },
  { title: t('ecomOwh.ret.badQty'), key: 'badQty', width: 90, align: 'right' }
]

async function load(id) {
  loading.value = true
  try {
    detail.value = await getOwhReturn(id)
  } finally {
    loading.value = false
  }
}
function show(id) {
  currentId = id
  detail.value = null
  open.value = true
  load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)

const actions = computed(() => {
  const d = detail.value
  const out = []
  if (isReturnDraft(d) && userStore.hasPerm('ecom:owhReturn:cancel')) out.push({ key: 'cancel', label: t('ecomOwh.ret.cancel'), danger: true, run: () => emit('cancel', d) })
  if (isReturnPushed(d) && userStore.hasPerm('ecom:owhReturn:cancel')) {
    out.push(hasCap(d.provider, 'CANCEL_RETURN')
      ? { key: 'cancel', label: t('ecomOwh.ret.cancelPushed'), danger: true, run: () => emit('cancel', d) }
      : { key: 'void', label: t('ecomOwh.common.markVoided'), danger: true, run: () => emit('void', d) })
  }
  if (isReturnDraft(d) && userStore.hasPerm('ecom:owhReturn:push')) out.push({ key: 'push', label: t('ecomOwh.ret.push'), primary: true, run: () => emit('push', d) })
  if (canSyncReturn(d) && userStore.hasPerm('ecom:owhReturn:sync')) out.push({ key: 'sync', label: t('ecomOwh.ret.sync'), primary: true, run: () => emit('sync', d) })
  return out
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 8px 0 12px;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
