<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="page-tabs">
        <a-tab-pane key="setting" :tab="$t('ecomOper.target.tabSetting')">
          <TargetTab v-if="loaded.setting" />
        </a-tab-pane>
        <a-tab-pane key="achievement" :tab="$t('ecomOper.target.tabAchievement')">
          <AchievementTab v-if="loaded.achievement" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import TargetTab from './TargetTab.vue'
import AchievementTab from '../ops-performance/AchievementTab.vue'

/** 运营目标：按月为店铺 / 运营人员设置销售额、利润、订单数目标（可复制上月），达成率按店铺、负责人、平台统计 */
const tab = ref('setting')
const loaded = reactive({ setting: true })
watch(tab, (v) => (loaded[v] = true))
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
