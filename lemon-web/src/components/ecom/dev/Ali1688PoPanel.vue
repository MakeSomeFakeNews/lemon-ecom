<template>
  <div>
    <div class="tools">
      <span class="sub-text">{{ $t('ecomDev.po.tip') }}</span>
      <a-space>
        <a-button v-if="canSync && list.length" size="small" :loading="syncing" @click="sync"><template #icon><SyncOutlined /></template>{{ $t('ecomDev.order.sync') }}</a-button>
        <a-button v-if="canCreate && orderable" size="small" type="primary" @click="modal.open(orderId)">
          <template #icon><ShoppingCartOutlined /></template>{{ $t('ecomDev.po.create') }}
        </a-button>
      </a-space>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 900 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'aliOrderId'">
          <a @click="router.push({ path: '/ecom/ali1688-order', query: { id: record.id } })">{{ record.aliOrderId }}</a>
        </template>
        <template v-else-if="column.key === 'amount'">
          <b>¥ {{ money(record.totalAmount) }}</b>
          <div class="sub-text">{{ $t('ecomDev.order.amountText', { product: money(record.productAmount), fee: money(record.shippingFee) }) }}</div>
        </template>
        <template v-else-if="column.key === 'logistics'">{{ record.logisticsCompany || '' }} {{ record.logisticsNo || '-' }}</template>
        <template v-else-if="column.key === 'status'"><DictTag type="ecom_1688_order_status" :value="record.status" /></template>
      </template>
      <template #summary>
        <a-table-summary-row v-if="list.length">
          <a-table-summary-cell :col-span="2">{{ $t('ecomDev.po.paidTotal') }}</a-table-summary-cell>
          <a-table-summary-cell align="right"><b>¥ {{ money(paidTotal) }}</b></a-table-summary-cell>
          <a-table-summary-cell :col-span="3" />
        </a-table-summary-row>
      </template>
    </a-table>
    <Ali1688OrderModal ref="modal" @success="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ShoppingCartOutlined, SyncOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import Ali1688OrderModal from './Ali1688OrderModal.vue'
import { ali1688OrdersByPo, syncAli1688Orders } from '@/api/ecom/dev'
import { Ali1688OrderStatus, OrderStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'

/**
 * 采购订单详情中的「1688 采购」页签：本订单生成的 1688 采购单（状态、实付含运费、物流），一键下单到 1688（已审核 / 部分入库的订单）、同步
 */
const props = defineProps({ orderId: Number, orderStatus: String })
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const canCreate = computed(() => userStore.hasPerm('ecom:ali1688Order:create'))
const canSync = computed(() => userStore.hasPerm('ecom:ali1688Order:sync'))
const orderable = computed(() => props.orderStatus === OrderStatus.APPROVED || props.orderStatus === OrderStatus.PARTIAL)

const list = ref([])
const loading = ref(false)
const syncing = ref(false)
const modal = ref()
const CLOSED = [Ali1688OrderStatus.CANCEL, Ali1688OrderStatus.TERMINATED]
const paidTotal = computed(() => list.value.filter((o) => !CLOSED.includes(o.status)).reduce((s, o) => s + Number(o.totalAmount || 0), 0))

const columns = [
  { title: t('ecomDev.order.aliOrderId'), key: 'aliOrderId', width: 190 },
  { title: t('ecomDev.order.seller'), dataIndex: 'sellerName', width: 180, ellipsis: true },
  { title: t('ecomDev.order.totalAmount'), key: 'amount', width: 170, align: 'right' },
  { title: t('ecomDev.order.logistics'), key: 'logistics', width: 180 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('ecomDev.order.syncTime'), dataIndex: 'syncTime', width: 160 }
]

async function load() {
  if (!props.orderId) return
  loading.value = true
  try {
    list.value = await ali1688OrdersByPo(props.orderId)
  } finally {
    loading.value = false
  }
}
async function sync() {
  syncing.value = true
  try {
    await syncAli1688Orders(list.value.map((o) => o.id))
    message.success(t('ecomDev.order.synced'))
    load()
  } finally {
    syncing.value = false
  }
}
watch(() => props.orderId, load)
onMounted(load)
</script>

<style scoped>
.tools {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
  flex-wrap: wrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
