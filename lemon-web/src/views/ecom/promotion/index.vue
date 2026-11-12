<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="page-tabs">
        <a-tab-pane key="list" :tab="$t('ecomOper.promo.tabList')">
          <PromotionTab v-if="loaded.list" ref="listRef" @detail="openDetail" @edit="(r) => formRef.openEdit(r)" @create="formRef.openCreate()" />
        </a-tab-pane>
        <a-tab-pane key="calendar" :tab="$t('ecomOper.promo.tabCalendar')">
          <CalendarTab v-if="loaded.calendar" ref="calendarRef" @detail="openDetail" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <PromotionForm ref="formRef" @success="onChanged" />
    <PromotionDetail ref="detailRef" @changed="onChanged" @edit="(r) => formRef.openEdit(r)" />
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import PromotionTab from './PromotionTab.vue'
import CalendarTab from './CalendarTab.vue'
import PromotionForm from './PromotionForm.vue'
import PromotionDetail from './PromotionDetail.vue'

/**
 * 促销管理：活动列表（优惠券、秒杀、Prime 折扣、虾皮折扣、TikTok 闪购、满减等，草稿 → 生效 → 取消，平台支持时推送）、促销日历；
 * 详情中看效果分析（活动前 / 中 / 后同等天数的销量、销售额、利润对比，活动费用计入利润）
 */
const tab = ref('list')
const loaded = reactive({ list: true })
watch(tab, (v) => (loaded[v] = true))

const formRef = ref()
const detailRef = ref()
const listRef = ref()
const calendarRef = ref()
const openDetail = (id) => detailRef.value.open(id)
function onChanged() {
  listRef.value?.reload()
  calendarRef.value?.reload()
  detailRef.value?.reload()
}
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
