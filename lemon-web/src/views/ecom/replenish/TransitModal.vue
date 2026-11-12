<template>
  <a-modal v-model:open="visible" :title="$t('ecomPurchase.transit.title', { name })" :footer="null" width="min(960px, 100vw)">
    <a-spin :spinning="loading">
      <div class="section-title">{{ $t('ecomPurchase.transit.purchases') }}</div>
      <a-table row-key="orderNo" size="small" :columns="purchaseColumns" :data-source="data.purchases" :pagination="false" :scroll="{ x: 760 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <a v-if="can('scm:purchase:list')" @click="go('/scm/purchase', record.orderId)">{{ record.orderNo }}</a>
            <span v-else>{{ record.orderNo }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_order_status" :value="record.status" />
          </template>
          <template v-else-if="QTY.includes(column.key)">{{ qty(record[column.key]) }}</template>
        </template>
        <template #emptyText>{{ $t('ecomPurchase.transit.empty') }}</template>
      </a-table>

      <div class="section-title">{{ $t('ecomPurchase.transit.shipments') }}</div>
      <a-table row-key="key" size="small" :columns="shipmentColumns" :data-source="shipments" :pagination="false" :scroll="{ x: 860 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a v-if="can('ecom:inbound:list')" @click="go('/ecom/inbound', record.inboundId)">{{ record.docNo }}</a>
            <span v-else>{{ record.docNo }}</span>
          </template>
          <template v-else-if="QTY.includes(column.key)">{{ qty(record[column.key]) }}</template>
        </template>
        <template #emptyText>{{ $t('ecomPurchase.transit.empty') }}</template>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { replenishTransit } from '@/api/ecom/replenish'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'

/**
 * 在途构成：物料的采购在途（已审核、部分入库的采购订单）与头程在途（已发运未签收的货件），单号可跳转到单据。
 * ref 调用 open({ materialId, storeId, name })，storeId 为空时列出全部店铺的货件
 */
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const QTY = ['qty', 'doneQty', 'openQty', 'receivedQty']
const purchaseColumns = [
  { title: t('ecomPurchase.transit.orderNo'), key: 'orderNo', width: 160 },
  { title: t('ecomPurchase.transit.supplier'), dataIndex: 'supplierName', width: 160, ellipsis: true },
  { title: t('ecomPurchase.transit.orderDate'), dataIndex: 'orderDate', width: 110 },
  { title: t('ecomPurchase.transit.deliveryDate'), dataIndex: 'deliveryDate', width: 110 },
  { title: t('ecomPurchase.transit.status'), key: 'status', width: 100 },
  { title: t('ecomPurchase.transit.qty'), key: 'qty', width: 90, align: 'right' },
  { title: t('ecomPurchase.transit.doneQty'), key: 'doneQty', width: 90, align: 'right' },
  { title: t('ecomPurchase.transit.openQty'), key: 'openQty', width: 90, align: 'right' }
]
const shipmentColumns = [
  { title: t('ecomPurchase.transit.docNo'), key: 'docNo', width: 160 },
  { title: t('ecomPurchase.transit.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('ecomPurchase.transit.shipmentId'), dataIndex: 'shipmentId', width: 140 },
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 130 },
  { title: t('ecomPurchase.transit.shipDate'), dataIndex: 'shipDate', width: 110 },
  { title: t('ecomPurchase.transit.etaDate'), dataIndex: 'etaDate', width: 110 },
  { title: t('ecomPurchase.transit.shipQty'), key: 'qty', width: 80, align: 'right' },
  { title: t('ecomPurchase.transit.receivedQty'), key: 'receivedQty', width: 80, align: 'right' },
  { title: t('ecomPurchase.transit.shipOpenQty'), key: 'openQty', width: 80, align: 'right' }
]

const visible = ref(false)
const loading = ref(false)
const name = ref('')
const data = ref({ purchases: [], shipments: [] })
const shipments = computed(() => (data.value.shipments || []).map((s, i) => ({ ...s, key: `${s.inboundId}-${i}` })))

async function open({ materialId, storeId, name: n }) {
  name.value = n || ''
  data.value = { purchases: [], shipments: [] }
  visible.value = true
  loading.value = true
  try {
    data.value = (await replenishTransit({ materialId, storeId: storeId || undefined })) || { purchases: [], shipments: [] }
  } finally {
    loading.value = false
  }
}

function go(path, id) {
  visible.value = false
  router.push({ path, query: { id } })
}

defineExpose({ open })
</script>

<style scoped>
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.section-title:first-child {
  margin-top: 0;
}
</style>
