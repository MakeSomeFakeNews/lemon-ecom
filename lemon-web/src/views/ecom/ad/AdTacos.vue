<template>
  <a-alert type="info" show-icon :message="$t('ecomAds.tacos.tip')" class="tip" />
  <div class="toolbar">
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAds.tacos.keywordPh')" class="kw" @press-enter="load" />
    <a-button @click="load"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
  </div>
  <a-table
    :row-key="(r) => `${r.storeId}|${r.sellerSku}`"
    size="middle"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="(p) => applyPageChange(pagination, p)"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'sellerSku'">
        {{ record.sellerSku }}
        <div class="sub-text">{{ record.storeName }}</div>
      </template>
      <template v-else-if="column.key === 'tacos'">
        <a-tag v-if="record.tacos != null" :color="tacosColor(record.tacos)">{{ fmt('percent', record.tacos) }}</a-tag>
        <span v-else>-</span>
      </template>
      <template v-else-if="column.kind">{{ fmt(column.kind, record[column.key]) }}</template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { adsTacos } from '@/api/ecom/ads'
import { sumWidth } from '@/composables/useColumns'
import { t } from '@/locales'
import { fmt } from './adMetrics'

/** SKU 与 TACOS 页签：订单销售额、利润（利润分析）、广告花费 / 销售额、ACOS、TACOS（本位币） */
const props = defineProps({ params: { type: Object, required: true } })
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const pagination = reactive(localPagination(20))
const col = (key, label, kind, width = 120) => ({ title: label, key, dataIndex: key, kind, width, align: 'right' })
const columns = [
  { title: t('ecomAds.tacos.sku'), key: 'sellerSku', width: 200, fixed: 'left' },
  { title: t('ecomAds.tacos.title'), key: 'title', dataIndex: 'title', width: 220, ellipsis: true },
  col('sales', t('ecomAds.tacos.sales'), 'money'),
  col('profit', t('ecomAds.tacos.profit'), 'money'),
  col('adSpend', t('ecomAds.tacos.adSpend'), 'money'),
  col('adSales', t('ecomAds.tacos.adSales'), 'money'),
  col('adClicks', t('ecomAds.metric.clicks'), 'int', 90),
  col('adOrders', t('ecomAds.metric.orders'), 'int', 90),
  col('acos', t('ecomAds.metric.acos'), 'percent', 90),
  { title: t('ecomAds.metric.tacos'), key: 'tacos', width: 100, align: 'right' }
]
const scrollX = computed(() => sumWidth(columns))
/** TACOS 不超过 10% 绿色、不超过 20% 橙色、超过红色 */
const tacosColor = (v) => (Number(v) <= 10 ? 'green' : Number(v) <= 20 ? 'orange' : 'red')

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await adsTacos({ ...props.params, keyword: keyword.value?.trim() || undefined })
    if (current !== seq) return
    rows.value = res || []
    pagination.current = 1
  } finally {
    if (current === seq) loading.value = false
  }
}
watch(() => props.params, load, { immediate: true })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.kw {
  width: 240px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
