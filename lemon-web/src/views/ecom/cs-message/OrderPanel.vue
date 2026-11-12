<template>
  <div class="order-panel">
    <a-spin :spinning="loading">
      <div class="section">
        <div class="section-title">{{ $t('ecomCs.order.title') }}</div>
        <template v-if="order">
          <div class="no">
            <router-link :to="{ path: '/ecom/order', query: { id: order.id } }">{{ order.platformOrderNo }}</router-link>
          </div>
          <div class="tags">
            <DictTag type="ecom_order_status" :value="order.status" />
            <DictTag type="ecom_local_status" :value="order.localStatus" />
            <DictTag type="ecom_fulfillment" :value="order.fulfillment" />
          </div>
          <a-descriptions :column="1" size="small" class="desc">
            <a-descriptions-item :label="$t('ecomCs.order.orderTime')">{{ order.orderTime }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomCs.order.total')">{{ fcMoney(order.currency, order.totalAmount) }}</a-descriptions-item>
            <a-descriptions-item v-if="Number(order.refundAmount)" :label="$t('ecomCs.order.refunded')">{{ fcMoney(order.currency, order.refundAmount) }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomCs.order.receiver')">
              {{ [order.shipName, order.shipPhone].filter(Boolean).join(' / ') || '-' }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomCs.order.address')">
              {{ [order.shipAddress, order.shipCity, order.shipState, order.shipPostcode, order.shipCountry].filter(Boolean).join(', ') || '-' }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomCs.order.tracking')">
              <span v-if="order.trackingNo">{{ order.carrier }} {{ order.trackingNo }}</span>
              <span v-else class="sub-text">{{ $t('ecomCs.order.noTracking') }}</span>
            </a-descriptions-item>
            <a-descriptions-item v-if="order.shipTime" :label="$t('ecomCs.order.shipTime')">{{ order.shipTime }}</a-descriptions-item>
          </a-descriptions>
          <div class="items">
            <div v-for="it in order.items || []" :key="it.id" class="item">
              <div class="item-title" :title="it.title">{{ it.title || it.sellerSku }}</div>
              <div class="sub-text">{{ it.sellerSku }} × {{ qty(it.qty) }} · {{ fcMoney(order.currency, it.amount) }}</div>
            </div>
          </div>
        </template>
        <div v-else class="sub-text none">{{ $t('ecomCs.order.notLinked') }}</div>
        <div v-perm="'ecom:csMessage:edit'" class="link">
          <a-input-search
            v-model:value="linkNo"
            size="small"
            :placeholder="$t('ecomCs.order.linkPh')"
            :enter-button="$t('ecomCs.order.link')"
            :loading="linking"
            @search="link"
          />
        </div>
      </div>

      <div v-if="recent.length" class="section">
        <div class="section-title">{{ $t('ecomCs.order.recent') }}</div>
        <div v-for="o in recent" :key="o.id" class="recent">
          <router-link :to="{ path: '/ecom/order', query: { id: o.id } }">{{ o.platformOrderNo }}</router-link>
          <span class="sub-text">{{ o.orderTime?.slice(0, 10) }} · {{ fcMoney(o.currency, o.totalAmount) }}</span>
          <DictTag type="ecom_local_status" :value="o.localStatus" />
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { getCsThreadOrder, linkCsThreadOrder } from '@/api/ecom/cs'
import { qty } from '@/utils/number'
import { fcMoney } from '../order/order'

/**
 * 订单侧栏：会话关联的平台订单（状态、金额、收件人、运单、明细，按订单的数据权限读取）、该买家在店铺中的最近订单；
 * 可按平台订单号关联 / 改关联订单（留空取消关联）。threadId、refreshKey（变化时重新加载），关联后触发 linked
 */
const props = defineProps({ threadId: { type: Number, required: true }, refreshKey: Number })
const emit = defineEmits(['linked'])
const { t } = useI18n()

const panel = ref(null)
const loading = ref(false)
const linking = ref(false)
const linkNo = ref('')
const order = computed(() => panel.value?.order)
const recent = computed(() => (panel.value?.recentOrders || []).filter((o) => o.id !== order.value?.id))

async function load() {
  loading.value = true
  try {
    panel.value = await getCsThreadOrder(props.threadId)
    linkNo.value = ''
  } finally {
    loading.value = false
  }
}

watch(() => [props.threadId, props.refreshKey], load, { immediate: true })

async function link() {
  linking.value = true
  try {
    await linkCsThreadOrder(props.threadId, linkNo.value?.trim() || null)
    message.success(linkNo.value?.trim() ? t('ecomCs.order.linked') : t('ecomCs.order.unlinked'))
    await load()
    emit('linked')
  } finally {
    linking.value = false
  }
}
</script>

<style scoped>
.order-panel {
  height: 100%;
  overflow-y: auto;
  padding: 12px 14px;
}
.section + .section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--app-border);
}
.section-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.no {
  font-size: 15px;
  font-weight: 500;
}
.tags {
  margin: 6px 0;
}
.desc :deep(.ant-descriptions-item) {
  padding-bottom: 4px;
}
.items {
  margin-top: 8px;
}
.item {
  padding: 6px 0;
  border-top: 1px dashed var(--app-border);
}
.item-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.none {
  margin-bottom: 8px;
}
.link {
  margin-top: 10px;
}
.recent {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}
</style>
