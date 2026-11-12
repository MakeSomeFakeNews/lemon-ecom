<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="page-tabs">
        <a-tab-pane key="rank" :tab="$t('ecomOper.perf.tabRank')">
          <RankTab v-if="loaded.rank" />
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
import RankTab from './RankTab.vue'
import AchievementTab from './AchievementTab.vue'

/** 运营绩效：运营人员绩效排行（销售额、利润、利润率、广告 ACOS、退货率、差评数）与月度达成率 */
const tab = ref('rank')
const loaded = reactive({ rank: true })
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
