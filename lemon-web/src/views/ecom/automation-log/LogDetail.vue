<template>
  <SubPage :open="visible" @close="visible = false">
    <template #title>
      <span>{{ $t('ecomAuto.log.detail') }}</span>
      <span v-if="log" class="title-obj">{{ log.objectLabel }}</span>
      <DictTag v-if="log" type="ecom_auto_result" :value="log.result" />
    </template>
    <template #extra>
      <a-space>
        <a-button v-if="log?.link" @click="go(log.link)"><template #icon><ExportOutlined /></template>{{ $t('ecomAuto.log.openObject') }}</a-button>
        <a-button v-if="canRetry" v-perm="'ecom:automationLog:retry'" type="primary" :loading="retrying" @click="retry">
          <template #icon><RedoOutlined /></template>{{ $t('ecomAuto.log.retry') }}
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading" class="auto-log-detail">
      <template v-if="log">
        <a-descriptions :column="{ xs: 1, md: 2, xl: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('ecomAuto.log.rule')">{{ log.ruleName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAuto.common.trigger')"><DictTag type="ecom_auto_trigger" :value="log.trigger" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAuto.log.time')">{{ log.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAuto.log.object')">{{ log.objectLabel }} <span class="mono sub-text">{{ log.objectKey }}</span></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAuto.log.cost')">{{ $t('ecomAuto.log.costMs', { n: log.costMs ?? 0 }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAuto.log.retry')">{{ log.retryCount ? $t('ecomAuto.log.retried', { n: log.retryCount }) : '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="log.errorMsg" :label="$t('ecomAuto.log.error')" :span="3"><span class="error">{{ log.errorMsg }}</span></a-descriptions-item>
        </a-descriptions>

        <a-row :gutter="16">
          <a-col :xs="24" :xl="12">
            <div class="section-title">{{ $t('ecomAuto.log.event') }}</div>
            <a-descriptions :column="{ xs: 1, md: 2 }" size="small" bordered class="block" :label-style="{ width: '120px', whiteSpace: 'nowrap' }">
              <a-descriptions-item v-for="(v, k) in fields" :key="k" :label="fieldLabel(k)">
                <DictTag v-if="fieldDict(k)" :type="fieldDict(k)" :value="v" />
                <span v-else class="field-value" :title="fmt(v)">{{ fmt(v) }}</span>
              </a-descriptions-item>
            </a-descriptions>
            <div class="section-title">{{ $t('ecomAuto.log.match') }}</div>
            <div class="match">
              <div v-for="(line, i) in matchLines" :key="i" class="match-line">{{ line }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :xl="12">
            <div class="section-title">{{ $t('ecomAuto.log.actionList') }}</div>
            <a-empty v-if="!log.actions?.length" :description="$t('ecomAuto.log.noActions')" />
            <a-timeline v-else class="timeline">
              <a-timeline-item v-for="a in log.actions" :key="a.index" :color="statusColor(a.status)">
                <div class="act-head">
                  <span class="act-name">{{ a.index + 1 }}. {{ findDict(dict.ecom_auto_action, a.type)?.label || a.type }}</span>
                  <DictTag type="ecom_auto_action_status" :value="a.status" />
                  <span class="sub-text">{{ $t('ecomAuto.log.costMs', { n: a.costMs }) }}</span>
                </div>
                <div class="act-msg" :class="{ error: a.status === AutoActionStatus.FAILED }">{{ a.message || '-' }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-col>
        </a-row>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ExportOutlined, RedoOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { getAutoLog, retryAutoLog } from '@/api/ecom/automation'
import { useDict } from '@/composables/useDict'
import { AutoActionStatus, AutoResult } from '@/constants/enums'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'
import { fieldOf, useCatalog } from '../automation/autoText'

/** 执行详情（子页）：规则、触发器、对象、耗时、失败原因，事件字段值，条件判断（每个条件的实际值与结果），每个动作的结果、说明与耗时；失败的可重试 */
const emit = defineEmits(['retried'])
const router = useRouter()
const catalog = useCatalog()
const dict = useDict('ecom_auto_action', 'ecom_auto_result', 'ecom_auto_action_status')
const visible = ref(false)
const loading = ref(false)
const retrying = ref(false)
const log = ref(null)

const canRetry = computed(() => log.value?.result === AutoResult.FAILED || log.value?.result === AutoResult.PARTIAL)
const fields = computed(() => Object.fromEntries(Object.entries(log.value?.fields || {}).filter(([, v]) => v !== null && v !== '')))
const matchLines = computed(() => (log.value?.matchDetail || '').split('; ').filter(Boolean))
const fieldLabel = (k) => fieldOf(catalog.value, log.value?.trigger, k)?.label || k
const fieldDict = (k) => fieldOf(catalog.value, log.value?.trigger, k)?.dict
const statusColor = (s) => (s === AutoActionStatus.SUCCESS ? 'green' : s === AutoActionStatus.FAILED ? 'red' : s === AutoActionStatus.SUGGESTED ? 'blue' : 'gray')

function fmt(v) {
  if (Array.isArray(v)) return v.length ? v.join(', ') : '-'
  if (v === true) return t('common.status.yes')
  if (v === false) return t('common.status.no')
  return String(v)
}

function go(link) {
  visible.value = false
  router.push(link)
}

async function load(id) {
  loading.value = true
  try {
    log.value = await getAutoLog(id)
  } finally {
    loading.value = false
  }
}

async function retry() {
  retrying.value = true
  try {
    log.value = await retryAutoLog(log.value.id)
    message.success(t('ecomAuto.log.retryDone', { result: findDict(dict.ecom_auto_result, log.value.result)?.label || log.value.result }))
    emit('retried')
  } finally {
    retrying.value = false
  }
}

defineExpose({
  open(id) {
    log.value = null
    visible.value = true
    load(id)
  }
})
</script>

<style scoped>
.title-obj {
  margin: 0 8px;
  font-weight: normal;
  color: var(--app-text-2);
}
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 4px 0 8px;
  font-weight: 600;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.sub-text {
  margin-left: 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
.error {
  color: #cf1322;
}
.field-value {
  word-break: break-all;
}
.match {
  margin-bottom: 16px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--app-hover);
}
.match-line {
  padding: 2px 0;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}
.timeline {
  margin-top: 8px;
}
.act-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.act-name {
  font-weight: 500;
}
.act-msg {
  margin-top: 2px;
  color: var(--app-text-2);
  font-size: 13px;
  word-break: break-word;
}
</style>

<style>
html[data-theme='dark'] .auto-log-detail .error {
  color: #ff7875;
}
</style>
