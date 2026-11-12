<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="packages" :pagination="false" :scroll="{ x: 1380 }" :locale="{ emptyText: $t('ecomOrder.ops.noPackages') }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'packageNo'">
        <a @click="router.push({ path: '/ecom/package', query: { keyword: record.packageNo } })">{{ record.packageNo }}</a>
      </template>
      <template v-else-if="column.key === 'status'">
        <DictTag type="ecom_package_status" :value="record.status" />
      </template>
      <template v-else-if="column.key === 'tracking'">
        <div>{{ record.trackingNo || '-' }}</div>
        <div v-if="record.carrierName" class="sub-text">{{ record.carrierName }}</div>
      </template>
      <template v-else-if="column.key === 'weight'">{{ record.weight != null ? Number(record.weight) : '-' }}</template>
      <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
      <template v-else-if="column.key === 'track'">
        <DictTag v-if="record.trackStatus" type="ecom_track_status" :value="record.trackStatus" />
        <DictTag v-if="record.trackAlert" type="ecom_track_alert" :value="record.trackAlert" />
        <span v-if="!record.trackStatus">-</span>
      </template>
      <template v-else-if="column.key === 'shipConfirm'">
        <a-tooltip v-if="record.shipConfirmMsg" :title="record.shipConfirmMsg">
          <a-tag :color="record.shipConfirm === 1 ? 'green' : record.shipConfirm === 0 ? 'red' : 'default'" :bordered="false">
            {{ record.shipConfirm === 1 ? $t('ecomOrd.pkg.confirmed') : record.shipConfirm === 0 ? $t('ecomOrd.pkg.confirmFailed') : $t('ecomOrd.pkg.confirmWaiting') }}
          </a-tag>
        </a-tooltip>
        <span v-else>-</span>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { qty } from '@/utils/number'

/** 订单详情中的包裹列表（含已取消），点包裹号到「包裹打单」查看 */
defineProps({ packages: { type: Array, default: () => [] } })
const { t } = useI18n()
const router = useRouter()

const columns = [
  { title: t('ecomOrder.pkg.packageNo'), key: 'packageNo', width: 170 },
  { title: t('ecomOrder.pkg.status'), key: 'status', width: 90 },
  { title: t('ecomOrder.common.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('ecomOrder.common.channel'), dataIndex: 'channelName', width: 140 },
  { title: t('ecomOrder.common.trackingNo'), key: 'tracking', width: 180 },
  { title: t('ecomOrder.pkg.orders'), dataIndex: 'orderCount', width: 70, align: 'right' },
  { title: t('ecomOrder.pkg.totalQty'), key: 'totalQty', width: 70, align: 'right' },
  { title: t('ecomOrder.pkg.weight'), key: 'weight', width: 90, align: 'right' },
  { title: t('ecomOrder.pkg.shipTime'), dataIndex: 'shipTime', width: 160 },
  { title: t('ecomOrd.track.status'), key: 'track', width: 150 },
  { title: t('ecomOrd.pkg.shipConfirm'), key: 'shipConfirm', width: 100 }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
