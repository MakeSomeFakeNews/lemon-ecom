<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <a-tab-pane key="rules" :tab="$t('ecomAds.tabs.rules')">
          <RuleList @logs="openLogs" />
        </a-tab-pane>
        <a-tab-pane key="logs" :tab="$t('ecomAds.tabs.logs')">
          <RuleLogs :rule-id="logRuleId" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RuleList from './RuleList.vue'
import RuleLogs from './RuleLogs.vue'

/** 广告规则（独立页面）：规则列表（新建、预览、立即执行、启停）与执行日志（处理待执行的建议） */
const tab = ref('rules')
const logRuleId = ref()

function openLogs(ruleId) {
  logRuleId.value = ruleId
  tab.value = 'logs'
}
</script>
