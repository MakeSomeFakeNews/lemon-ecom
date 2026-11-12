<template>
  <div class="panel advice">
    <div class="panel-title">
      <RobotOutlined /> {{ $t('ecomAsst.detail.advice') }}
      <DictTag type="ecom_ops_report_ai_status" :value="report.aiStatus" />
      <a-tag v-if="report.aiSample" color="gold">{{ $t('ecomAsst.report.sample') }}</a-tag>
      <span class="spacer" />
      <template v-if="canGenerate">
        <a-tooltip v-if="!aiStatus.available" :title="aiStatus.reason || $t('ecomAsst.common.aiUnavailable')">
          <a-button size="small" disabled><template #icon><ReloadOutlined /></template>{{ regenLabel }}</a-button>
        </a-tooltip>
        <a-button v-else size="small" :loading="running" @click="regenerate"><template #icon><ReloadOutlined /></template>{{ regenLabel }}</a-button>
      </template>
    </div>
    <div v-if="report.aiStatus === OpsReportAiStatus.SUCCESS && report.aiAdvice" class="advice-text">
      <p v-for="(line, i) in lines" :key="i" :class="{ bullet: line.startsWith('- ') }">{{ line.startsWith('- ') ? line.slice(2) : line }}</p>
    </div>
    <a-alert v-else-if="report.aiStatus === OpsReportAiStatus.FAILED" type="error" show-icon :message="$t('ecomAsst.detail.adviceFailed')" :description="report.aiError" />
    <div v-else class="advice-empty">
      <a-empty :image="simpleImage" :description="report.aiError ? $t('ecomAsst.detail.adviceNoneReason', { reason: report.aiError }) : $t('ecomAsst.detail.adviceNone')" />
      <div class="sub-text center">{{ $t('ecomAsst.detail.adviceNoneTip') }}</div>
      <div v-if="!aiStatus.available && canConfig" class="center"><a @click="router.push('/ai/settings')">{{ $t('ecomAsst.common.goAiSettings') }}</a></div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined, RobotOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { regenerateOpsAdvice } from '@/api/ecom/assistant'
import { useEcomAi } from '@/components/ecom/ai/useEcomAi'
import { OpsReportAiStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'

/** 日报的 AI 要点与建议：已生成时按行显示（「- 」开头的为建议条目），未生成时说明原因，可重新生成；触发 regenerated */
const props = defineProps({ report: { type: Object, required: true } })
const emit = defineEmits(['regenerated'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { status: aiStatus } = useEcomAi()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const running = ref(false)
const canGenerate = computed(() => userStore.hasPerm('ecom:opsReport:generate'))
const canConfig = computed(() => userStore.hasPerm('ai:config:list'))
const lines = computed(() => String(props.report.aiAdvice || '').split(/\r?\n/).map((l) => l.trim()).filter(Boolean))
const regenLabel = computed(() => (props.report.aiStatus === OpsReportAiStatus.SUCCESS ? t('ecomAsst.report.regenAdvice') : t('ecomAsst.detail.genAdvice')))

async function regenerate() {
  running.value = true
  try {
    await regenerateOpsAdvice(props.report.id)
    message.success(t('ecomAsst.report.adviceDone'))
    emit('regenerated')
  } finally {
    running.value = false
  }
}
</script>

<style scoped>
.panel {
  height: 100%;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-card);
}
.advice {
  background: linear-gradient(180deg, rgba(22, 119, 255, 0.04), transparent 120px), var(--app-card);
}
.panel-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}
.panel-title :deep(.ant-tag) {
  margin-inline-end: 0;
  font-weight: normal;
}
.spacer {
  flex: 1;
}
.advice-text p {
  margin: 0 0 6px;
  line-height: 1.7;
}
.advice-text p.bullet {
  position: relative;
  padding-left: 16px;
}
.advice-text p.bullet::before {
  content: '';
  position: absolute;
  top: 10px;
  left: 4px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #1677ff;
}
.advice-empty {
  padding: 8px 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.center {
  text-align: center;
  margin-top: 4px;
}
</style>
