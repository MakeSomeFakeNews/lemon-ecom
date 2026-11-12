<template>
  <SubPage :open="open" :title="detail ? $t('ecomOrder.pkg.detailTitle', { no: detail.packageNo }) : $t('ecomOrder.pkg.title')" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_package_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.errorMsg" type="warning" show-icon class="block" :message="$t('ecomOrder.pkg.errorMsg')" :description="detail.errorMsg" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.warehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ detail.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.receiver')">{{ [detail.shipName, detail.shipCountry].filter(Boolean).join(' · ') || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.carrierName')">
            {{ detail.carrierName || '-' }}
            <a-tag v-if="detail.trackingNo" :bordered="false" :color="detail.carrierOrderNo ? 'blue' : 'default'" class="mode-tag">
              {{ detail.carrierOrderNo ? $t('ecomOrder.pkg.online') : $t('ecomOrder.pkg.manual') }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.carrierOrderNo')">{{ detail.carrierOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.weight')">{{ detail.weight != null ? Number(detail.weight) : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.shippingCost')">{{ detail.shippingCost != null ? `${symMoney(detail.shippingCost)}` : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.labelTime')">{{ detail.labelTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.printCount')">{{ detail.printCount ?? 0 }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.pkg.shipTime')">{{ detail.shipTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOrder.pkg.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 1000 }" class="block">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'orderNo'">
              <a @click="router.push({ path: '/ecom/order', query: { id: record.orderId } })">{{ record.platformOrderNo }}</a>
            </template>
            <template v-else-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.materialSpec }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'materialQty'">{{ `${qty(record.materialQty)} ${record.unit || ''}` }}</template>
            <template v-else-if="column.key === 'cost'">{{ record.cost != null ? `${symMoney(record.cost)}` : '-' }}</template>
          </template>
        </a-table>

        <template v-if="canTrack(detail)">
          <div class="section-title">
            {{ $t('ecomOrder.pkg.track') }}
            <a-button size="small" type="link" :loading="tracking" @click="loadTrack"><ReloadOutlined /></a-button>
          </div>
          <a-timeline v-if="tracks?.length">
            <a-timeline-item v-for="(e, i) in tracks" :key="i">
              <div>{{ e.description }}</div>
              <div class="sub-text">{{ [e.time, e.location].filter(Boolean).join(' · ') }}</div>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else-if="tracks" :description="$t('ecomOrder.pkg.noTrack')" />
        </template>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { CarOutlined, EditOutlined, FilePdfOutlined, NumberOutlined, PrinterOutlined, ReloadOutlined, StopOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getEcomPackage, trackEcomPackage } from '@/api/ecom/package'
import { useUserStore } from '@/store/user'
import { money, qty, symMoney } from '@/utils/number'
import { canCancelPackage, canEditPackage, canLabel, canRefetch, canShipPackage, canTrack, canTracking } from './package'

/**
 * 包裹详情子页：包裹信息、明细（订单号、SKU、物料、件数、出库成本与出库单）、物流轨迹。
 * 操作交给页面处理（事件带包裹）：label 申请运单号、tracking 填写运单号、edit 修改、viewLabel 查看面单、refetch 重新获取面单、ship 发货、cancel 取消。
 * ref 调用 open(id)、reload()
 */
const emit = defineEmits(['label', 'tracking', 'edit', 'viewLabel', 'refetch', 'ship', 'cancel'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const itemColumns = [
  { title: t('ecomOrder.pkg.orderNo'), key: 'orderNo', width: 180 },
  { title: t('ecomOrder.pkg.sku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.order.itemTitle'), dataIndex: 'title', width: 220, ellipsis: true },
  { title: t('ecomOrder.pkg.material'), key: 'material', width: 220 },
  { title: t('ecomOrder.pkg.qty'), key: 'qty', width: 80, align: 'right' },
  { title: t('ecomOrder.pkg.materialQty'), key: 'materialQty', width: 110, align: 'right' },
  { title: t('ecomOrder.pkg.cost'), key: 'cost', width: 110, align: 'right' },
  { title: t('ecomOrder.pkg.tradeDoc'), dataIndex: 'tradeDocNo', width: 160 }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
const tracks = ref()
const tracking = ref(false)
let currentId

async function load(id) {
  loading.value = true
  try {
    detail.value = await getEcomPackage(id)
  } finally {
    loading.value = false
  }
}

async function loadTrack() {
  tracking.value = true
  try {
    tracks.value = (await trackEcomPackage(currentId)) || []
  } finally {
    tracking.value = false
  }
}

function show(id) {
  currentId = id
  detail.value = undefined
  tracks.value = undefined
  open.value = true
  return load(id)
}

const reload = () => (open.value && currentId ? load(currentId) : undefined)

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  const add = (show, perm, key, label, icon, extra = {}) => show && can(perm) && list.push({ key, label, icon, run: () => emit(key, d), ...extra })
  add(canCancelPackage(d), 'ecom:package:label', 'cancel', t('ecomOrder.pkg.cancel'), StopOutlined, { danger: true })
  add(canEditPackage(d), 'ecom:package:label', 'edit', t('ecomOrder.pkg.edit'), EditOutlined)
  add(canRefetch(d), 'ecom:package:label', 'refetch', t('ecomOrder.pkg.refetch'), ReloadOutlined)
  add(!!d.hasLabel, 'ecom:package:list', 'viewLabel', t('ecomOrder.pkg.viewLabel'), FilePdfOutlined)
  add(canTracking(d), 'ecom:package:label', 'tracking', t('ecomOrder.pkg.tracking'), NumberOutlined)
  add(canLabel(d), 'ecom:package:label', 'label', t('ecomOrder.pkg.label'), PrinterOutlined, { primary: true })
  add(canShipPackage(d), 'ecom:package:ship', 'ship', t('ecomOrder.pkg.ship'), CarOutlined, { primary: true })
  return list
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.mode-tag {
  margin-left: 6px;
}
</style>
