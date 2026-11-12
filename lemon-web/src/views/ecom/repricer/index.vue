<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <a-tab-pane key="rules" :tab="$t('ecomPublish.repricer.rules')">
          <RuleList @logs="openLogs" />
        </a-tab-pane>
        <a-tab-pane key="logs" :tab="$t('ecomPublish.repricer.logs')">
          <RepriceLogs :rule-id="logRuleId" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RepriceLogs from './RepriceLogs.vue'
import RuleList from './RuleList.vue'

/** 自动调价：规则（适用店铺 / SKU、策略、底价与最高价、执行间隔、仅建议）与调价日志（建议可应用或忽略） */
const tab = ref('rules')
const logRuleId = ref()
function openLogs(ruleId) {
  logRuleId.value = ruleId
  tab.value = 'logs'
}
</script>
