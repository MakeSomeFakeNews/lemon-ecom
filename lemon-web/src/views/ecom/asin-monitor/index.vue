<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="page-tabs">
        <a-tab-pane key="monitor" :tab="$t('ecomOper.monitor.tabList')">
          <MonitorTab v-if="loaded.monitor" @alerts="openAlerts" />
        </a-tab-pane>
        <a-tab-pane key="alert">
          <template #tab>
            {{ $t('ecomOper.monitor.tabAlert') }}
            <a-badge v-if="alertTotal" :count="alertTotal" :overflow-count="99" class="tab-badge" />
          </template>
          <AlertTab v-if="loaded.alert" :monitor-id="alertMonitorId" @changed="loadAlertTotal" @clear-monitor="alertMonitorId = null" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { monitorAlertCounts } from '@/api/ecom/oper'
import MonitorTab from './MonitorTab.vue'
import AlertTab from './AlertTab.vue'

/**
 * ASIN / 商品监控：监控列表（自己的商品与竞品的排名、评分、评论、跟卖、标题主图、上下架，接口取数 / 手工录入 / 导入快照，详情看趋势），
 * 变化预警（排名大跌、评分下降、新增跟卖、标题主图五点被改、被下架，处理 / 忽略）。站内消息跳转带 ?tab=alert&monitorId=
 */
const route = useRoute()
const router = useRouter()
const TABS = ['monitor', 'alert']
const tab = ref(TABS.includes(route.query.tab) ? String(route.query.tab) : 'monitor')
const loaded = reactive({ [tab.value]: true })
const alertMonitorId = ref(Number(route.query.monitorId) || null)
const alertTotal = ref(0)
watch(tab, (v) => (loaded[v] = true))
watch(
  () => route.query.monitorId,
  (id) => {
    if (!id) return
    alertMonitorId.value = Number(id)
    tab.value = 'alert'
    router.replace({ query: { ...route.query, monitorId: undefined, tab: undefined } })
  }
)

function openAlerts(monitorId) {
  alertMonitorId.value = monitorId
  tab.value = 'alert'
}

async function loadAlertTotal() {
  const counts = await monitorAlertCounts({})
  alertTotal.value = (counts || []).reduce((s, c) => s + Number(c.count || 0), 0)
}

onMounted(() => {
  loadAlertTotal()
  if (route.query.tab || route.query.monitorId) router.replace({ query: { ...route.query, monitorId: undefined, tab: undefined } })
})
</script>

<style scoped>
.page-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.page-tabs :deep(.ant-tabs-content-holder),
.page-tabs :deep(.ant-tabs-content),
.page-tabs :deep(.ant-tabs-tabpane-active) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tab-badge {
  margin-left: 4px;
}
.tab-badge :deep(.ant-badge-count) {
  box-shadow: none;
}
</style>
