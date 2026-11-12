<template>
  <SubPage :open="open" :title="order ? $t('ecomDev.order.detailTitle', { no: order.aliOrderId }) : $t('ecomDev.order.title')" @close="open = false">
    <template v-if="order" #extra><DictTag type="ecom_1688_order_status" :value="order.status" /></template>
    <a-spin :spinning="loading">
      <template v-if="order">
        <a-alert v-if="order.syncMessage" type="error" show-icon class="block" :message="$t('ecomDev.order.syncFailed', { msg: order.syncMessage })" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomDev.order.aliOrderId')">{{ order.aliOrderId }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.po')">
            <a v-if="order.scmOrderId && can('scm:purchase:list')" @click="router.push({ path: '/scm/purchase', query: { id: order.scmOrderId } })">{{ order.scmOrderNo }}</a>
            <span v-else>{{ order.scmOrderNo || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.seller')">{{ order.sellerName || order.sellerLoginId || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.productAmount')">¥ {{ money(order.productAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.shippingFee')">¥ {{ money(order.shippingFee) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.totalAmount')"><b>¥ {{ money(order.totalAmount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.account')">{{ order.accountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.createTime')">{{ order.aliCreateTime || order.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.payTime')">{{ order.payTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.logistics')">
            {{ order.logisticsCompany || '' }} {{ order.logisticsNo || '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.shipTime')">{{ order.shipTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.receiveTime')">{{ order.receiveTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.syncTime')">{{ order.syncTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="order.message" :label="$t('ecomDev.order.message')" :span="2">{{ order.message }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomDev.order.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="order.items || []" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
            <template v-else-if="column.key === 'offer'">
              <div class="cell">
                <img v-if="record.imageUrl" :src="record.imageUrl" class="thumb" alt="" />
                <div>
                  <a :href="offerUrl(record.offerId)" target="_blank" rel="noopener">{{ record.title || record.offerId }}</a>
                  <div class="sub-text">{{ record.specText || record.specId || '' }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}<div v-if="record.materialQty != null" class="sub-text">{{ $t('ecomDev.order.materialQty', { n: qty(record.materialQty) }) }}</div></template>
            <template v-else-if="column.key === 'price'">{{ record.price != null ? price(record.price) : '-' }}</template>
            <template v-else-if="column.key === 'amount'">{{ record.amount != null ? money(record.amount) : '-' }}</template>
          </template>
        </a-table>

        <div class="section-title">{{ $t('ecomDev.order.trace') }}</div>
        <a-timeline v-if="trace.length" class="timeline">
          <a-timeline-item v-for="(s, i) in [...trace].reverse()" :key="i" :color="i === 0 ? 'green' : 'gray'">
            <div>{{ s.remark }}</div>
            <div class="sub-text">{{ s.time }}</div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else :image="simpleImage" :description="$t('ecomDev.order.noTrace')" />
      </template>
    </a-spin>
    <template v-if="order" #footer>
      <div class="drawer-footer">
        <a-button v-if="order.status === Ali1688OrderStatus.WAIT_BUYER_PAY && can('ecom:ali1688Order:cancel')" danger @click="cancel">
          <template #icon><CloseCircleOutlined /></template>{{ $t('ecomDev.order.cancel') }}
        </a-button>
        <a-button v-if="can('ecom:ali1688Order:sync')" :loading="syncing" @click="sync"><template #icon><SyncOutlined /></template>{{ $t('ecomDev.order.sync') }}</a-button>
        <a-button v-if="order.status === Ali1688OrderStatus.WAIT_BUYER_PAY && can('ecom:ali1688Order:create')" type="primary" @click="pay">
          <template #icon><PayCircleOutlined /></template>{{ $t('ecomDev.order.pay') }}
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CloseCircleOutlined, PayCircleOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { ali1688PayUrl, cancelAli1688Order, getAli1688Order, syncAli1688Order } from '@/api/ecom/dev'
import { Ali1688OrderStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { money, price, qty } from '@/utils/number'

/** 1688 采购单详情：金额（货款、运费、实付）、明细、物流轨迹；同步、付款（打开 1688 收银台）、取消。ref 调用 open(id) */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const can = (p) => userStore.hasPerm(p)
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const open = ref(false)
const loading = ref(false)
const syncing = ref(false)
const order = ref()
const trace = ref([])
let currentId

const offerUrl = (id) => `https://detail.1688.com/offer/${id}.html`
const itemColumns = [
  { title: t('ecomDev.order.material'), key: 'material', width: 200 },
  { title: t('ecomDev.order.offer'), key: 'offer', width: 320 },
  { title: t('ecomDev.order.aliQty'), key: 'qty', width: 110, align: 'right' },
  { title: t('ecomDev.order.price'), key: 'price', width: 90, align: 'right' },
  { title: t('ecomDev.order.amount'), key: 'amount', width: 100, align: 'right' }
]

async function load(id) {
  loading.value = true
  try {
    const d = await getAli1688Order(id)
    order.value = d.order
    trace.value = d.trace || []
  } finally {
    loading.value = false
  }
}
function show(id) {
  currentId = id
  order.value = undefined
  open.value = true
  return load(id)
}

async function sync() {
  syncing.value = true
  try {
    await syncAli1688Order(currentId)
    message.success(t('ecomDev.order.synced'))
    await load(currentId)
    emit('changed')
  } finally {
    syncing.value = false
  }
}
async function pay() {
  const url = await ali1688PayUrl([currentId])
  window.open(url, '_blank')
}
function cancel() {
  Modal.confirm({
    title: t('ecomDev.order.cancelConfirm', { no: order.value.aliOrderId }),
    okType: 'danger',
    async onOk() {
      await cancelAli1688Order(currentId)
      message.success(t('ecomDev.order.cancelled'))
      await load(currentId)
      emit('changed')
    }
  })
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.cell {
  display: flex;
  gap: 8px;
  align-items: center;
}
.thumb {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.timeline {
  padding-top: 8px;
}
</style>
