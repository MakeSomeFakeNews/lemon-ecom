<template>
  <div v-if="list.length">
    <div class="section-title">{{ $t('ecomOwh.order.title') }}</div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :pagination="false" :scroll="{ x: 900 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'outboundNo'">
          <a @click="router.push({ path: '/ecom/owh-outbound', query: { id: record.id } })">{{ record.outboundNo }}</a>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="ecom_owh_outbound_status" :value="record.status" />
          <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
            <ExclamationCircleOutlined class="error-icon" />
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'warehouse'">
          <div>{{ record.warehouseName }}</div>
          <div class="sub-text">{{ record.accountName }} · {{ record.owhCode }}</div>
        </template>
        <template v-else-if="column.key === 'tracking'">
          <div>{{ record.trackingNo || '-' }}</div>
          <div v-if="record.carrierName" class="sub-text">{{ record.carrierName }}</div>
        </template>
        <template v-else-if="column.key === 'feeBase'">{{ record.feeBase != null ? `¥${money(record.feeBase)}` : '-' }}</template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { owhByOrder } from '@/api/ecom/owh'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'

/** 订单详情中的海外仓出库单（发货仓是海外仓的包裹推送后才有）：状态、服务商单号、运单号、费用；没有时不显示 */
const props = defineProps({ orderId: Number, refreshKey: { type: [Number, String], default: 0 } })
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const list = ref([])

const columns = [
  { title: t('ecomOwh.outbound.outboundNo'), key: 'outboundNo', width: 170 },
  { title: t('ecomOwh.outbound.packageNo'), dataIndex: 'packageNo', width: 160 },
  { title: t('ecomOwh.outbound.status'), key: 'status', width: 110 },
  { title: t('ecomOwh.common.warehouse'), key: 'warehouse', width: 170 },
  { title: t('ecomOwh.outbound.owhOrderNo'), dataIndex: 'owhOrderNo', width: 150 },
  { title: t('ecomOwh.outbound.trackingNo'), key: 'tracking', width: 160 },
  { title: t('ecomOwh.outbound.feeBase'), key: 'feeBase', width: 100, align: 'right' },
  { title: t('ecomOwh.outbound.pushTime'), dataIndex: 'pushTime', width: 160 }
]

async function load() {
  list.value = []
  if (!props.orderId || !(userStore.hasPerm('ecom:order:list') || userStore.hasPerm('ecom:owhOutbound:list'))) return
  list.value = (await owhByOrder(props.orderId)) || []
}
watch(() => [props.orderId, props.refreshKey], load, { immediate: true })
</script>

<style scoped>
.section-title {
  margin: 16px 0 12px;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-icon {
  margin-left: 4px;
  color: var(--ant-color-warning, #faad14);
  cursor: help;
}
</style>
