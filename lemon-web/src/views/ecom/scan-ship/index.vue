<template>
  <div class="scan-page">
    <a-card :bordered="false" class="page-card">
      <a-alert type="info" show-icon :message="$t('ecomOrder.scan.hint')" class="tip" />
      <a-input
        ref="inputRef"
        v-model:value="code"
        size="large"
        allow-clear
        :maxlength="64"
        :disabled="busy"
        :placeholder="$t('ecomOrder.scan.placeholder')"
        @press-enter="scan"
      >
        <template #prefix><ScanOutlined /></template>
      </a-input>
      <div class="summary">{{ $t('ecomOrder.scan.count', { n: shippedCount }) }}</div>
    </a-card>

    <a-card v-if="last" :bordered="false" class="page-card" :title="$t('ecomOrder.scan.lastResult')">
      <a-result
        :status="last.ok ? 'success' : 'error'"
        :title="last.ok ? `${$t('ecomOrder.scan.success')} · ${last.pkg.packageNo}` : `${$t('ecomOrder.scan.failed')} · ${last.code}`"
        :sub-title="last.ok ? subTitle(last.pkg) : last.message"
        class="result"
      />
      <a-table v-if="last.ok" row-key="id" size="small" :columns="itemColumns" :data-source="last.pkg.items || []" :pagination="false" :scroll="{ x: 700 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" class="page-card" :title="$t('ecomOrder.scan.history')">
      <template #extra>
        <a-button size="small" :disabled="!history.length" @click="history = []">{{ $t('ecomOrder.scan.clear') }}</a-button>
      </template>
      <a-table row-key="seq" size="small" :columns="historyColumns" :data-source="history" :pagination="{ pageSize: 20 }" :scroll="{ x: 700 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'result'">
            <a-tag :color="record.ok ? 'success' : 'error'" :bordered="false">{{ record.ok ? $t('ecomOrder.scan.success') : $t('ecomOrder.scan.failed') }}</a-tag>
            <span>{{ record.ok ? record.pkg.packageNo : record.message }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { ScanOutlined } from '@ant-design/icons-vue'
import { scanShipEcomPackage } from '@/api/ecom/package'
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
const historyColumns = [
  { title: t('ecomOrder.scan.time'), dataIndex: 'time', width: 100 },
  { title: t('ecomOrder.scan.code'), dataIndex: 'code', width: 200 },
  { title: t('ecomOrder.scan.result'), key: 'result', width: 400 }
]

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
  }
}

onMounted(focus)
</script>

<style scoped>
.scan-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.tip {
  margin-bottom: 16px;
}
.summary {
  margin-top: 8px;
  color: var(--app-text-3);
}
.result {
  padding: 8px 0 16px;
}
</style>
