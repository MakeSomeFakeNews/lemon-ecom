<template>
  <SubPage :open="open" :title="detail ? $t('ecomOwh.tr.detailTitle', { no: detail.transferNo }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_owh_transfer_status" :value="detail.status" />
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.errorMsg" type="warning" show-icon class="block" :message="$t('ecomOwh.common.errorMsg')" :description="detail.errorMsg" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOwh.tr.from')">{{ whText(detail.fromWarehouseName, detail.fromOwhCode) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.to')">{{ whText(detail.toWarehouseName, detail.toOwhCode) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.product')">{{ detail.productCode || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.owhOutNo')">{{ detail.owhOutNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.owhStatus')">{{ detail.owhStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.tracking')">{{ [detail.carrier, detail.trackingNo].filter(Boolean).join(' · ') || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.transferDoc')">{{ detail.transferDocNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.inbound')">
            <template v-if="detail.inboundId">
              <a @click="router.push({ path: '/ecom/owh-inbound', query: { id: detail.inboundId } })">{{ detail.inboundNo }}</a>
              <DictTag v-if="detail.inboundStatus" type="ecom_owh_inbound_status" :value="detail.inboundStatus" class="tag" />
            </template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.shipTime')">{{ detail.shipTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.tr.receiveTime')">{{ detail.receiveTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('ecomOwh.tr.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }} {{ record.materialSpec || '' }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
          </template>
        </a-table>
        <OwhDiffList :source-type="OwhDiffSource.TRANSFER" :source-id="detail.id" :refresh-key="detail.updateTime" />
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
import { getOwhTransfer } from '@/api/ecom/owh'
import OwhDiffList from '@/components/ecom/owh/OwhDiffList.vue'
import { OwhDiffSource, OwhTransferStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'
import { canCancelTransfer, canReceiveTransfer, canSyncTransfer } from '../owh-outbound/owh'

/** 海外仓调拨单详情：调出 / 调入仓、调出服务商出库单、ERP 在途调拨单、调入海外仓入库单。open(id)、reload()；操作交给页面（事件带调拨单） */
const emit = defineEmits(['edit', 'submit', 'sync', 'receive', 'cancel'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const open = ref(false)
const loading = ref(false)
const detail = ref(null)
let currentId = null
const itemColumns = [
  { title: t('ecomOwh.stock.material'), key: 'material', width: 300 },
  { title: t('ecomOwh.tr.qty'), key: 'qty', width: 120, align: 'right' }
]
const whText = (name, code) => (code ? `${name}（${code}）` : `${name || '-'}（${t('ecomOwh.tr.local')}）`)

async function load(id) {
  loading.value = true
  try {
    detail.value = await getOwhTransfer(id)
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
  const draft = d?.status === OwhTransferStatus.DRAFT
  if (canCancelTransfer(d) && userStore.hasPerm('ecom:owhTransfer:cancel')) out.push({ key: 'cancel', label: t('ecomOwh.tr.cancel'), danger: true, run: () => emit('cancel', d) })
  if (draft && userStore.hasPerm('ecom:owhTransfer:add')) out.push({ key: 'edit', label: t('ecomOwh.tr.edit'), run: () => emit('edit', d) })
  if (draft && userStore.hasPerm('ecom:owhTransfer:push')) out.push({ key: 'submit', label: t('ecomOwh.tr.submit'), primary: true, run: () => emit('submit', d) })
  if (canSyncTransfer(d) && userStore.hasPerm('ecom:owhTransfer:sync')) out.push({ key: 'sync', label: t('ecomOwh.tr.sync'), primary: true, run: () => emit('sync', d) })
  if (canReceiveTransfer(d) && userStore.hasPerm('ecom:owhTransfer:receive')) out.push({ key: 'receive', label: t('ecomOwh.tr.receive'), primary: true, run: () => emit('receive', d) })
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
.tag {
  margin-left: 6px;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
