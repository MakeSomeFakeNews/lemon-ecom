<template>
  <a-modal :open="open" :title="kind === 'review' ? $t('ecomCs.review.replyTitle') : $t('ecomCs.feedback.replyTitle')" width="min(680px, 96vw)"
           :confirm-loading="saving" :ok-text="okText" :ok-button-props="{ disabled: !canReply }" @ok="submit" @cancel="open = false">
    <template v-if="record">
      <div class="head">
        <a-rate :value="record.rating" disabled class="rate" />
        <span class="sub-text">{{ record.storeName }} · {{ kind === 'review' ? record.reviewDate : record.feedbackDate }}
          · {{ (kind === 'review' ? record.reviewer : record.rater) || '-' }}</span>
      </div>
      <div v-if="kind === 'review' && (record.sellerSku || record.listingTitle)" class="sub-text">{{ record.sellerSku }} {{ record.listingTitle }}</div>
      <div class="content">
        <b v-if="record.title">{{ record.title }}</b>
        <div>{{ (kind === 'review' ? record.content : record.comments) || '-' }}</div>
        <div v-if="translated" class="translation">{{ translated }}</div>
      </div>
      <div v-if="existing" class="existing">
        <div class="sub-text">{{ $t('ecomCs.review.currentReply') }}</div>
        <div>{{ existing }}</div>
      </div>
      <template v-if="canReply">
        <div class="tools">
          <TemplatePicker v-if="kind === 'review'" :platform="record.platform" :review-id="record.id" @apply="applyTemplate" />
          <TemplatePicker v-else :platform="record.platform" :order-id="record.orderId" @apply="applyTemplate" />
          <template v-if="kind === 'review' && canAi">
            <AiTextButton :label="$t('ecomFix.ai.draft')" with-options :action="draft" @done="applyDraft" />
            <AiTextButton v-if="!translated" :label="$t('ecomFix.ai.translateReview')" :action="translateReview" @done="(text) => (translated = text)" />
          </template>
          <a-tooltip :title="viaPlatform ? $t('ecomCs.review.recordOnlyTip') : $t('ecomCs.review.recordOnlyForced')">
            <a-checkbox v-model:checked="recordOnly" :disabled="!viaPlatform">{{ $t('ecomCs.message.recordOnly') }}</a-checkbox>
          </a-tooltip>
        </div>
        <a-textarea v-model:value="content" :maxlength="2000" show-count :auto-size="{ minRows: 4, maxRows: 10 }" :placeholder="$t('ecomCs.review.replyPh')" />
      </template>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import TemplatePicker from '@/components/ecom/cs/TemplatePicker.vue'
import AiTextButton from '@/components/ecom/ai/AiTextButton.vue'
import { draftReviewReply, translateReviewText } from '@/api/ecom/aiText'
import { replyCsFeedback, replyCsReview } from '@/api/ecom/cs'
import { useUserStore } from '@/store/user'
import { CsRecordSource } from '@/constants/enums'

/**
 * 查看与回复评价 / 店铺反馈：显示评分与内容、已有回复，选择快捷回复模板（按关联订单替换变量）后回复。
 * 平台同步来的评价默认调用平台接口回复，可改为只登记；导入的评价、店铺反馈只登记回复内容（在平台后台回复后记录）。
 * open(kind: 'review' | 'feedback', record)，成功后触发 success
 */
const emit = defineEmits(['success'])
const { t, locale } = useI18n()
const user = useUserStore()

const open = ref(false)
const saving = ref(false)
const kind = ref('review')
const record = ref(null)
const content = ref('')
const recordOnly = ref(false)

const canReply = computed(() => user.hasPerm('ecom:csReview:reply'))
const canAi = computed(() => user.hasPerm('ecom:csReview:ai'))
const translated = ref('')
const existing = computed(() => (kind.value === 'review' ? record.value?.replyContent : record.value?.response))
/** 同步来的评价可以通过平台接口回复 */
const viaPlatform = computed(() => kind.value === 'review' && record.value?.source === CsRecordSource.SYNC)
const okText = computed(() => (viaPlatform.value && !recordOnly.value ? t('ecomCs.message.send') : t('ecomCs.message.record')))

function show(k, r) {
  kind.value = k
  record.value = r
  content.value = ''
  translated.value = ''
  recordOnly.value = !(k === 'review' && r.source === CsRecordSource.SYNC)
  open.value = true
}

function applyTemplate({ content: text }) {
  content.value = content.value.trim() ? `${content.value.trim()}\n${text}` : text
}

/** AI 草拟评价回复（默认用评价的语言），放入回复框由人工确认 */
async function draft(params) {
  const r = await draftReviewReply(record.value.id, params)
  return r?.text
}
function applyDraft(text) {
  content.value = text
  message.success(t('ecomFix.ai.drafted'))
}
/** 评价内容译成界面语言 */
async function translateReview() {
  const text = [record.value.title, record.value.content].filter(Boolean).join('\n')
  if (!text) return null
  const r = await translateReviewText({ text, target: String(locale.value || 'zh-CN').split('-')[0] })
  return r?.text
}

async function submit() {
  if (!content.value.trim()) return message.warning(t('ecomCs.review.replyRequired'))
  saving.value = true
  try {
    if (kind.value === 'review') await replyCsReview(record.value.id, { content: content.value, recordOnly: recordOnly.value })
    else await replyCsFeedback(record.value.id, { content: content.value, recordOnly: true })
    message.success(t('ecomCs.review.replied'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rate {
  font-size: 14px;
}
.content {
  margin: 10px 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-bg);
  white-space: pre-wrap;
  word-break: break-word;
}
.translation {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--app-border);
  color: var(--app-text-2);
}
.existing {
  margin-bottom: 10px;
  padding: 8px 12px;
  border-left: 3px solid var(--app-primary);
  white-space: pre-wrap;
}
.tools {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
