<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="page-tabs">
        <a-tab-pane key="list" :tab="$t('ecomAsst.report.tabList')">
          <ReportTab ref="listRef" @detail="openDetail" @generate="generateRef.open()" />
        </a-tab-pane>
        <a-tab-pane v-if="canSubscribe" key="sub" :tab="$t('ecomAsst.sub.tab')">
          <SubTab v-if="loaded.sub" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <GenerateModal ref="generateRef" @done="onGenerated" />
    <ReportDetail ref="detailRef" @changed="listRef?.reload()" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import ReportTab from './ReportTab.vue'
import ReportDetail from './ReportDetail.vue'
import GenerateModal from './GenerateModal.vue'
import SubTab from './SubTab.vue'

/**
 * 运营日报：每天定时（默认 8:30）按店铺范围汇总昨日核心指标与环比、异常 SKU、新差评、断货风险、广告异常、待处理事项，
 * AI 已配置时附「要点与建议」；可手动立即生成，详情为指标卡 + 表格 + AI 建议。订阅设置：订阅人、店铺范围、推送渠道。
 * 地址带 ?id= 时直接打开该日报（推送消息中的链接）
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canSubscribe = computed(() => userStore.hasPerm('ecom:opsReport:subscribe'))
const tab = ref('list')
const loaded = reactive({ list: true })
watch(tab, (v) => (loaded[v] = true))

const listRef = ref()
const detailRef = ref()
const generateRef = ref()
const openDetail = (id) => detailRef.value.open(id)
function onGenerated(id) {
  listRef.value?.reload()
  openDetail(id)
}

function openFromQuery() {
  const id = Number(route.query.id)
  if (id) {
    openDetail(id)
    router.replace({ query: { ...route.query, id: undefined } })
  }
}
onMounted(openFromQuery)
watch(() => route.query.id, (v) => v && openFromQuery())
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
</style>
