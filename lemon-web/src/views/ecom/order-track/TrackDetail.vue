<template>
  <SubPage :open="open" :title="titleText" @close="open = false">
    <template v-if="pkg" #title>
      <span class="head-title">
        {{ titleText }}
        <a-typography-text :copyable="{ text: pkg.packageNo, tooltip: false }" />
      </span>
      <DictTag type="ecom_track_status" :value="pkg.trackStatus || EcomTrackStatus.NOT_FOUND" />
      <DictTag v-if="pkg.trackAlert" type="ecom_track_alert" :value="pkg.trackAlert" />
    </template>
    <template v-if="pkg && can('ecom:track:sync')" #extra>
      <a-button type="primary" :loading="pulling" @click="pull">
        <template #icon><SyncOutlined /></template>{{ $t('ecomOrd.track.pull') }}
      </a-button>
    </template>

    <a-spin :spinning="loading">
      <a-skeleton v-if="!pkg" active :paragraph="{ rows: 10 }" />
      <template v-else>
        <a-alert v-if="pkg.trackError" type="warning" show-icon class="block" :message="pkg.trackError" />
        <div class="stats">
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrd.track.lastEvent') }}</div>
            <div class="stat-value stat-text" :title="pkg.trackLastEvent">{{ pkg.trackLastEvent || '-' }}</div>
            <div class="stat-sub">{{ pkg.trackLastTime || '-' }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrd.track.days') }}</div>
            <div class="stat-value">{{ days }}</div>
            <div class="stat-sub">{{ pkg.trackDeliveredTime ? $t('ecomOrdUi.track.daysDelivered') : $t('ecomOrdUi.track.daysToNow') }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrd.track.shipTime') }}</div>
            <div class="stat-value">{{ short(pkg.shipTime) }}</div>
            <div class="stat-sub">{{ $t('ecomOrd.track.firstTime') }} {{ short(pkg.trackFirstTime) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrd.track.deliveredTime') }}</div>
            <div class="stat-value">{{ short(pkg.trackDeliveredTime) }}</div>
            <div class="stat-sub">{{ pkg.trackPullTime ? $t('ecomOrd.track.pulledAt', { time: pkg.trackPullTime }) : '-' }}</div>
          </div>
        </div>

        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :xl="16">
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrd.track.title') }}</div>
              <TrackTimeline :pkg="pkg" :events="events" />
            </section>
          </a-col>
          <a-col :xs="24" :xl="8">
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.track.package') }}</div>
              <a-descriptions :column="1" size="small" class="side-desc">
                <a-descriptions-item :label="$t('ecomOrder.pkg.packageNo')">{{ pkg.packageNo }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.carrier')">{{ pkg.carrierName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.trackingNo')">
                  <a-typography-text v-if="pkg.trackingNo" :copyable="{ tooltip: false }">{{ pkg.trackingNo }}</a-typography-text><span v-else>-</span>
                </a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ pkg.channelName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.warehouse')">{{ pkg.warehouseName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.pkg.weight')">{{ pkg.weight != null ? Number(pkg.weight) : '-' }}</a-descriptions-item>
              </a-descriptions>
            </section>
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.track.order') }}</div>
              <a-descriptions :column="1" size="small" class="side-desc">
                <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ pkg.storeName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.platformOrderNo')">
                  <template v-if="orderNo">
                    <a @click="router.push({ path: '/ecom/order', query: { keyword: orderNo } })">{{ orderNo }}</a>
                    <span v-if="pkg.orderCount > 1" class="sub-text"> +{{ pkg.orderCount - 1 }}</span>
                  </template>
                  <span v-else>-</span>
                </a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.pkg.totalQty')">{{ pkg.totalQty != null ? Number(pkg.totalQty) : '-' }}</a-descriptions-item>
              </a-descriptions>
            </section>
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.track.receiver') }}</div>
              <a-descriptions :column="1" size="small" class="side-desc">
                <a-descriptions-item :label="$t('ecomOrd.track.receiver')">{{ pkg.shipName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.country')">
                  <span v-if="pkg.shipCountry" class="country">{{ pkg.shipCountry }}</span><span v-else>-</span>
                </a-descriptions-item>
              </a-descriptions>
            </section>
          </a-col>
        </a-row>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SyncOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import TrackTimeline from './TrackTimeline.vue'
import { getPackageTracks, pullPackageTracks } from '@/api/ecom/orderEnhance'
import { useUserStore } from '@/store/user'
import { EcomTrackStatus } from '@/constants/enums'

/**
 * 包裹轨迹详情（子页）：顶部包裹号 + 轨迹状态 / 预警，左侧轨迹时间线，右侧包裹、订单、收件信息。
 * ref 调用 open(列表行)、update(拉取结果)；在详情中拉取轨迹后触发 changed
 */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const open = ref(false)
const loading = ref(false)
const pulling = ref(false)
const row = ref()
const data = ref()

// 详情接口不返回订单号，取列表行上的
const pkg = computed(() => (data.value ? { ...data.value.pkg, orderNos: row.value?.orderNos, orderCount: row.value?.orderCount ?? data.value.pkg.orderCount } : undefined))
const events = computed(() => data.value?.events || [])
const orderNo = computed(() => pkg.value?.orderNos)
const titleText = computed(() => (row.value ? t('ecomOrder.pkg.detailTitle', { no: row.value.packageNo }) : t('ecomOrd.track.title')))
const days = computed(() => (pkg.value?.shipTime ? dayjs(pkg.value.trackDeliveredTime || undefined).diff(dayjs(pkg.value.shipTime), 'day') : '-'))
const short = (v) => (v ? String(v).slice(0, 16) : '-')

async function show(record) {
  row.value = record
  data.value = undefined
  open.value = true
  loading.value = true
  try {
    data.value = await getPackageTracks(record.id)
  } finally {
    loading.value = false
  }
}

/** 列表中拉取了同一包裹的轨迹时更新详情 */
function update(res) {
  if (open.value && res && data.value?.pkg.id === res.pkg?.id) data.value = res
}

async function pull() {
  pulling.value = true
  try {
    data.value = await pullPackageTracks(data.value.pkg.id)
    message.success(t('ecomOrd.track.pulled'))
    emit('changed')
  } finally {
    pulling.value = false
  }
}

defineExpose({ open: show, update })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.head-title {
  margin-right: 8px;
  font-weight: 600;
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  margin: 4px 0 2px;
  overflow: hidden;
  font-size: 20px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.stat-text {
  font-size: 15px;
  line-height: 30px;
}
.stat-sub {
  overflow: hidden;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
@media (max-width: 991px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
.panel {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.panel-title {
  margin-bottom: 12px;
  font-weight: 600;
}
.side-desc :deep(.ant-descriptions-item-label) {
  min-width: 84px;
  color: var(--app-text-3);
  white-space: nowrap;
}
.country {
  padding: 0 4px;
  border: 1px solid var(--app-border);
  border-radius: 3px;
  font-size: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
