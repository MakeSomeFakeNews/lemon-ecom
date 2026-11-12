<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="review-tabs">
        <a-tab-pane key="review" :tab="$t('ecomCs.review.tabList')">
          <ReviewTab v-if="loaded.review" :focus-id="tab === 'review' ? focusId : null" @after-sale="openAfterSale" />
        </a-tab-pane>
        <a-tab-pane key="stats" :tab="$t('ecomCs.review.tabStats')">
          <ReviewStatsTab v-if="loaded.stats" />
        </a-tab-pane>
        <a-tab-pane key="feedback" :tab="$t('ecomCs.review.tabFeedback')">
          <FeedbackTab v-if="loaded.feedback" :focus-id="tab === 'feedback' ? focusId : null" @after-sale="openAfterSale" />
        </a-tab-pane>
        <a-tab-pane key="request" :tab="$t('ecomCs.review.tabRequest')">
          <RequestTab v-if="loaded.request" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <AfterSaleModal ref="afterSale" />
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AfterSaleModal from '@/components/ecom/cs/AfterSaleModal.vue'
import ReviewTab from './ReviewTab.vue'
import ReviewStatsTab from './ReviewStatsTab.vue'
import FeedbackTab from './FeedbackTab.vue'
import RequestTab from './RequestTab.vue'

/**
 * 评价管理：评价列表（导入 / 同步、回复、处理、转售后；低分评价自动待处理并提醒店铺负责人）、评价统计（概况、近 30 天评分趋势、按 SKU 平均分与差评率）、
 * 店铺反馈（导入、登记回复、处理）、请求评价（亚马逊发货 5–30 天的订单）。
 * 站内消息、售后工单跳转过来时带 ?tab=review|feedback&id= 打开对应记录；页签切换过去时才加载
 */
const route = useRoute()
const router = useRouter()
const TABS = ['review', 'stats', 'feedback', 'request']
const tab = ref(TABS.includes(route.query.tab) ? String(route.query.tab) : 'review')
const focusId = ref(Number(route.query.id) || null)
const loaded = reactive({ [tab.value]: true })
watch(tab, (v) => (loaded[v] = true))
watch(
  () => route.query.id,
  (id) => {
    if (!id) return
    if (TABS.includes(route.query.tab)) tab.value = String(route.query.tab)
    focusId.value = Number(id)
    router.replace({ query: { ...route.query, id: undefined, tab: undefined } })
  },
  { immediate: true }
)

const afterSale = ref()
const openAfterSale = (payload) => afterSale.value.open(payload)
</script>

<style scoped>
.review-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.review-tabs :deep(.ant-tabs-content-holder),
.review-tabs :deep(.ant-tabs-content),
.review-tabs :deep(.ant-tabs-tabpane-active) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
