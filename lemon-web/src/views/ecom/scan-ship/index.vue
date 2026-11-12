<template>
  <div class="scan-page">
    <div class="scan-main">
      <a-card :bordered="false" class="page-card">
        <div class="scan-box">
          <a-input
            ref="inputRef"
            v-model:value="code"
            size="large"
            allow-clear
            :maxlength="64"
            :disabled="busy"
            :placeholder="$t('ecomOrder.scan.placeholder')"
            class="scan-input"
            @press-enter="scan"
          >
            <template #prefix><ScanOutlined class="scan-icon" /></template>
          </a-input>
          <a-button type="primary" size="large" :loading="busy" @click="scan">{{ $t('ecomOrdUi.scan.ship') }}</a-button>
        </div>
        <div class="hint"><InfoCircleOutlined /> {{ $t('ecomOrder.scan.hint') }}</div>
        <div class="stats">
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrdUi.scan.waiting') }}</div>
            <div class="stat-value">{{ waiting ?? '-' }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrdUi.scan.okCount') }}</div>
            <div class="stat-value ok">{{ shippedCount }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrdUi.scan.failCount') }}</div>
            <div class="stat-value fail">{{ history.length - shippedCount }}</div>
          </div>
        </div>
      </a-card>

      <a-card :bordered="false" class="page-card result-card">
        <template v-if="last">
          <div class="result" :class="last.ok ? 'is-ok' : 'is-fail'">
            <component :is="last.ok ? CheckCircleFilled : CloseCircleFilled" class="result-icon" />
            <div class="result-main">
              <div class="result-title">{{ last.ok ? `${$t('ecomOrder.scan.success')} · ${last.pkg.packageNo}` : `${$t('ecomOrder.scan.failed')} · ${last.code}` }}</div>
              <div class="result-sub">{{ last.ok ? subTitle(last.pkg) : last.message }}</div>
            </div>
          </div>
          <a-table v-if="last.ok" row-key="id" size="small" :columns="itemColumns" :data-source="last.pkg.items || []" :pagination="false" :scroll="{ x: 640 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'"><b>{{ qty(record.qty) }}</b></template>
            </template>
          </a-table>
        </template>
        <a-empty v-else :description="$t('ecomOrdUi.scan.ready')">
          <template #image><ScanOutlined class="empty-icon" /></template>
        </a-empty>
      </a-card>
    </div>

    <a-card :bordered="false" class="page-card history-card" :title="$t('ecomOrder.scan.history')">
      <template #extra>
        <a-button size="small" type="text" :disabled="!history.length" @click="history = []">{{ $t('ecomOrder.scan.clear') }}</a-button>
      </template>
      <div v-if="history.length" class="history">
        <div v-for="h in history" :key="h.seq" class="history-item">
          <component :is="h.ok ? CheckCircleFilled : CloseCircleFilled" :class="h.ok ? 'ok' : 'fail'" />
          <div class="history-main">
            <div class="history-code">{{ h.ok ? h.pkg.packageNo : h.code }}</div>
            <div class="history-sub" :title="h.ok ? subTitle(h.pkg) : h.message">{{ h.ok ? [h.pkg.trackingNo, h.pkg.shipName].filter(Boolean).join(' · ') : h.message }}</div>
          </div>
          <span class="history-time">{{ h.time }}</span>
        </div>
      </div>
      <a-empty v-else :image="simpleImage" :description="$t('ecomOrdUi.scan.noHistory')" />
    </a-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { Empty } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { CheckCircleFilled, CloseCircleFilled, InfoCircleOutlined, ScanOutlined } from '@ant-design/icons-vue'
import { ecomPackageStatusCounts, scanShipEcomPackage } from '@/api/ecom/package'
import { EcomPackageStatus } from '@/constants/enums'
import { qty } from '@/utils/number'

/**
 * 扫描发货：扫描枪输入运单号（或包裹号）+ 回车 → 后端校验包裹（待发货、订单未挂起 / 未取消）后发货（生成销售出库、回传平台）。
 * 每次扫描后清空并聚焦输入框，结果和本次记录保留在页面上（刷新后清空）
 */
const { t } = useI18n()

const inputRef = ref()
const code = ref('')
const busy = ref(false)
const last = ref()
const history = ref([])
let seq = 0

const shippedCount = computed(() => history.value.filter((h) => h.ok).length)
const subTitle = (p) => [p.storeName, p.channelName, p.carrierName, p.trackingNo, p.shipName].filter(Boolean).join(' · ')

const itemColumns = [
  { title: t('ecomOrder.pkg.orderNo'), dataIndex: 'platformOrderNo', width: 180 },
  { title: t('ecomOrder.pkg.sku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.pkg.material'), key: 'material', width: 240 },
  { title: t('ecomOrder.pkg.qty'), key: 'qty', width: 80, align: 'right' }
]

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
/** 待发货包裹数（每次扫描后刷新） */
const waiting = ref()
async function loadWaiting() {
  const counts = (await ecomPackageStatusCounts({})) || {}
  waiting.value = counts[EcomPackageStatus.LABELED] || 0
}

const focus = () => nextTick(() => inputRef.value?.focus())

async function scan() {
  const c = (code.value || '').trim()
  if (!c || busy.value) return
  busy.value = true
  const entry = { seq: ++seq, code: c, time: dayjs().format('HH:mm:ss') }
  try {
    entry.pkg = await scanShipEcomPackage(c)
    entry.ok = true
  } catch (e) {
    entry.ok = false
    entry.message = e?.message || ''
  } finally {
    busy.value = false
    code.value = ''
    last.value = entry
    history.value = [entry, ...history.value]
    focus()
    if (entry.ok) loadWaiting()
  }
}

onMounted(() => {
  focus()
  loadWaiting()
})
</script>

<style scoped>
.scan-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  height: 100%;
}
.scan-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}
.scan-main > .page-card + .page-card {
  margin-top: 0;
}
.scan-box {
  display: flex;
  gap: 12px;
}
.scan-input {
  flex: 1;
  height: 56px;
  font-size: 20px;
}
.scan-box :deep(.ant-btn-lg) {
  height: 56px;
  padding-inline: 28px;
}
.scan-icon {
  margin-right: 8px;
  color: var(--app-primary, #1677ff);
  font-size: 22px;
}
.hint {
  margin-top: 10px;
  color: var(--app-text-3);
  font-size: 12px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
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
  margin-top: 4px;
  font-size: 24px;
  font-weight: 600;
}
.ok {
  color: var(--ant-color-success, #52c41a);
}
.fail {
  color: var(--ant-color-error, #ff4d4f);
}
.result-card {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.result {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding: 20px 24px;
  border-radius: 8px;
}
.is-ok {
  background: rgba(82, 196, 26, 0.08);
}
.is-fail {
  background: rgba(255, 77, 79, 0.08);
}
.result-icon {
  font-size: 40px;
}
.is-ok .result-icon {
  color: var(--ant-color-success, #52c41a);
}
.is-fail .result-icon {
  color: var(--ant-color-error, #ff4d4f);
}
.result-title {
  font-size: 18px;
  font-weight: 600;
}
.result-sub {
  margin-top: 4px;
  color: var(--app-text-2);
}
.empty-icon {
  color: var(--app-text-3);
  font-size: 56px;
}
.history-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.history-card :deep(.ant-card-body) {
  flex: 1;
  min-height: 0;
  padding: 0;
  overflow: auto;
}
.history-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid var(--app-border);
}
.history-main {
  flex: 1;
  min-width: 0;
}
.history-code {
  font-weight: 500;
}
.history-sub {
  overflow: hidden;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.history-time {
  color: var(--app-text-3);
  font-size: 12px;
}
.history-card :deep(.ant-empty) {
  margin: 48px 0;
}
@media (max-width: 991px) {
  .scan-page {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>
