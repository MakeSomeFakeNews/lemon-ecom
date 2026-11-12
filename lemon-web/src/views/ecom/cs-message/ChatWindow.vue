<template>
  <div class="chat-root">
    <a-spin :spinning="loading && !detail">
      <div v-if="thread" class="chat">
        <div class="chat-head">
          <div class="who">
            <span class="buyer">{{ thread.buyerName || thread.conversationId }}</span>
            <span class="sub-text">{{ thread.storeName }}</span>
            <DictTag type="ecom_platform" :value="thread.platform" />
            <a-tag v-if="thread.overdue" color="red" :bordered="false">{{ $t('ecomCs.message.overdue') }}</a-tag>
            <DictTag v-else type="ecom_cs_thread_status" :value="thread.status" />
            <a-tag v-for="tag in tags" :key="tag" :bordered="false">{{ tag }}</a-tag>
          </div>
          <div class="meta sub-text">
            <span v-if="thread.ownerName">{{ $t('ecomCs.message.owner') }}: {{ thread.ownerName }}</span>
            <span v-if="thread.status === CsThreadStatus.PENDING && thread.replyDeadline" :class="{ late: thread.overdue }">
              {{ $t('ecomCs.message.deadline') }} {{ thread.replyDeadline }}（{{ $t('ecomCs.message.replyHours', { n: detail.replyHours }) }}）
            </span>
          </div>
          <a-space class="actions" wrap>
            <a-button v-perm="'ecom:csMessage:edit'" size="small" @click="editModal.open(thread)">
              <template #icon><TagsOutlined /></template>{{ $t('ecomCs.message.edit') }}
            </a-button>
            <a-button v-if="thread.status !== CsThreadStatus.CLOSED" v-perm="'ecom:csMessage:edit'" size="small" @click="close(thread.id)">
              <template #icon><CheckOutlined /></template>{{ $t('ecomCs.message.close') }}
            </a-button>
            <a-button v-else v-perm="'ecom:csMessage:edit'" size="small" @click="reopen(thread.id)">
              <template #icon><UndoOutlined /></template>{{ $t('ecomCs.message.reopen') }}
            </a-button>
            <a-button v-perm="'ecom:csTicket:add'" size="small" @click="emit('after-sale', thread)">
              <template #icon><ToolOutlined /></template>{{ $t('ecomCs.ticket.action') }}
            </a-button>
          </a-space>
        </div>

        <a-alert v-if="detail.tickets?.length" type="success" class="tickets" :show-icon="false" banner>
          <template #message>
            <span v-for="k in detail.tickets" :key="k.id" class="ticket">
              <DictTag type="ecom_cs_ticket_type" :value="k.ticketType" />
              <router-link v-if="k.returnId" :to="{ path: '/ecom/return', query: { id: k.returnId } }">{{ k.returnNo }}</router-link>
              <router-link v-else-if="k.resendOrderId" :to="{ path: '/ecom/order', query: { keyword: k.resendOrderNo } }">{{ k.resendOrderNo }}</router-link>
            </span>
          </template>
        </a-alert>

        <div ref="scroller" class="messages">
          <div v-if="!messages.length" class="empty"><a-empty :description="$t('ecomCs.message.noMessages')" /></div>
          <div v-for="m in messages" :key="m.id" class="msg" :class="m.direction === CsDirection.SELLER ? 'mine' : 'theirs'">
            <div class="bubble" :class="{ failed: m.sendStatus === CsSendStatus.FAILED }">
              <div v-if="m.content" class="text">{{ m.content }}</div>
              <div v-if="translations[m.id]" class="translation">{{ translations[m.id] }}</div>
              <div v-for="url in attachments(m)" :key="url" class="attach">
                <a :href="url" target="_blank" rel="noopener noreferrer"><PaperClipOutlined /> {{ fileName(url) }}</a>
              </div>
              <div v-if="!m.content && !attachments(m).length" class="sub-text">[{{ m.msgType }}]</div>
            </div>
            <div class="info sub-text">
              <span>{{ m.sentTime }}</span>
              <span v-if="m.senderName"> · {{ m.senderName }}</span>
              <template v-if="m.direction === CsDirection.SELLER && m.sendStatus && m.sendStatus !== CsSendStatus.SENT">
                · <DictTag type="ecom_cs_send_status" :value="m.sendStatus" />
              </template>
              <span v-if="m.direction === CsDirection.SELLER && m.sendStatus === CsSendStatus.SENT && !m.platformMessageId"> · {{ $t('ecomCs.message.recorded') }}</span>
              <template v-if="canAi && aiStatus.available && m.content && m.direction === CsDirection.BUYER">
                · <a v-if="!translations[m.id]" class="translate-link" @click="translateMessage(m)">
                  <LoadingOutlined v-if="translating[m.id]" /> {{ $t('ecomFix.ai.translate') }}</a>
                <a v-else class="translate-link" @click="hideTranslation(m)">{{ $t('ecomFix.ai.hideTranslation') }}</a>
              </template>
            </div>
            <div v-if="m.sendStatus === CsSendStatus.FAILED" class="failed-line">
              <span class="error">{{ m.errorMessage }}</span>
              <a-button v-perm="'ecom:csMessage:reply'" type="link" size="small" @click="resend(m.id)">{{ $t('ecomCs.message.resend') }}</a-button>
            </div>
          </div>
        </div>

        <div v-if="canReply" class="composer">
          <div class="tools">
            <TemplatePicker :platform="thread.platform" :thread-id="thread.id" @apply="applyTemplate" />
            <template v-if="canAi">
              <AiTextButton :label="$t('ecomFix.ai.draft')" with-options :action="draft" @done="applyDraft" />
              <AiTextButton :label="$t('ecomFix.ai.toBuyer')" :action="toBuyer" @done="(text) => (content = text)" />
            </template>
            <a-tooltip :title="detail.canSend ? $t('ecomCs.message.recordOnlyTip') : $t('ecomCs.message.cannotSendTip')">
              <a-checkbox v-model:checked="recordOnly" :disabled="!detail.canSend">{{ $t('ecomCs.message.recordOnly') }}</a-checkbox>
            </a-tooltip>
            <span class="sub-text counter">{{ content.length }} / 2000</span>
          </div>
          <a-textarea
            v-model:value="content"
            :maxlength="2000"
            :auto-size="{ minRows: 3, maxRows: 8 }"
            :placeholder="$t('ecomCs.message.replyPh')"
            @keydown="onKeydown"
          />
          <div class="send-row">
            <span class="sub-text">{{ $t('ecomCs.message.sendHint') }}</span>
            <a-button type="primary" :loading="sending" :disabled="!content.trim()" @click="send">
              <template #icon><SendOutlined /></template>{{ recordOnly ? $t('ecomCs.message.record') : $t('ecomCs.message.send') }}
            </a-button>
          </div>
        </div>
      </div>
      <ThreadEditModal ref="editModal" @success="afterChange" />
    </a-spin>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CheckOutlined, LoadingOutlined, PaperClipOutlined, SendOutlined, TagsOutlined, ToolOutlined, UndoOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TemplatePicker from '@/components/ecom/cs/TemplatePicker.vue'
import AiTextButton from '@/components/ecom/ai/AiTextButton.vue'
import { useEcomAi } from '@/components/ecom/ai/useEcomAi'
import { draftThreadReply, translateCsText } from '@/api/ecom/aiText'
import { closeCsThread, getCsThread, readCsThread, reopenCsThread, replyCsThread, resendCsMessage } from '@/api/ecom/cs'
import { useAction } from '@/composables/useAction'
import { useUserStore } from '@/store/user'
import { CsDirection, CsSendStatus, CsThreadStatus } from '@/constants/enums'
import ThreadEditModal from './ThreadEditModal.vue'

/**
 * 聊天窗口：会话头（买家、店铺、状态、回复截止、负责人与标签、关闭 / 重新打开、转售后）、消息（买家在左、卖家在右，发送失败可重发）、
 * 回复框（快捷回复模板、只登记、Ctrl / ⌘ + Enter 发送）。打开会话时标记已读。
 * - threadId；@changed 会话有变化，@after-sale(thread) 转售后
 */
const props = defineProps({ threadId: { type: Number, required: true } })
const emit = defineEmits(['changed', 'after-sale'])
const { t, locale } = useI18n()
const user = useUserStore()
const { status: aiStatus } = useEcomAi()
const canAi = computed(() => user.hasPerm('ecom:csMessage:ai'))
/** 买家消息的译文（按消息 ID，切换会话时清空） */
const translations = reactive({})
const translating = reactive({})

const detail = ref(null)
const loading = ref(false)
const thread = computed(() => detail.value?.thread)
const messages = computed(() => detail.value?.messages || [])
const tags = computed(() => (thread.value?.tags ? thread.value.tags.split(',').filter(Boolean) : []))
const canReply = computed(() => user.hasPerm('ecom:csMessage:reply'))
const content = ref('')
const templateId = ref(null)
const recordOnly = ref(false)
const sending = ref(false)
const scroller = ref()
const editModal = ref()

async function load(markRead = false) {
  loading.value = true
  try {
    detail.value = await getCsThread(props.threadId)
    recordOnly.value = !detail.value.canSend
    await nextTick()
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
    if (markRead && thread.value.unread > 0) {
      await readCsThread(props.threadId)
      emit('changed')
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => props.threadId,
  () => {
    content.value = ''
    templateId.value = null
    Object.keys(translations).forEach((k) => delete translations[k])
    load(true)
  },
  { immediate: true }
)

function applyTemplate({ templateId: id, content: text }) {
  templateId.value = id
  content.value = content.value.trim() ? `${content.value.trim()}\n${text}` : text
}

/** AI 草拟回复（默认用买家的语言，可指定语言、补充要求），结果放入回复框由人工确认 */
async function draft(params) {
  const r = await draftThreadReply(props.threadId, params)
  return r?.text
}
function applyDraft(text) {
  templateId.value = null
  content.value = text
  message.success(t('ecomFix.ai.drafted'))
}
/** 把回复框中的文字译成买家的语言 */
async function toBuyer() {
  if (!content.value.trim()) {
    message.warning(t('ecomFix.ai.contentFirst'))
    return null
  }
  const r = await translateCsText({ text: content.value, threadId: props.threadId })
  return r?.text
}
const hideTranslation = (m) => delete translations[m.id]
/** 买家消息译成界面语言 */
async function translateMessage(m) {
  if (translating[m.id]) return
  translating[m.id] = true
  try {
    const r = await translateCsText({ text: m.content, target: String(locale.value || 'zh-CN').split('-')[0] })
    translations[m.id] = r?.text || ''
  } finally {
    delete translating[m.id]
  }
}

/** Ctrl / ⌘ + Enter 发送（合并成一个监听：两个 @keydown 会合并成数组，antd 的 onKeydown 只接受函数会报警告） */
function onKeydown(e) {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    send()
  }
}

async function send() {
  if (!content.value.trim() || sending.value) return
  sending.value = true
  try {
    const m = await replyCsThread(props.threadId, { content: content.value, templateId: templateId.value, recordOnly: recordOnly.value })
    if (m.sendStatus === CsSendStatus.FAILED) message.error(t('ecomCs.message.sendFailed', { msg: m.errorMessage || '' }))
    else message.success(recordOnly.value ? t('ecomCs.message.recordedOk') : t('ecomCs.message.sent'))
    content.value = ''
    templateId.value = null
    await afterChange()
  } finally {
    sending.value = false
  }
}

async function afterChange() {
  await load()
  emit('changed')
}

async function resend(id) {
  const m = await resendCsMessage(id)
  if (m.sendStatus === CsSendStatus.FAILED) message.error(t('ecomCs.message.sendFailed', { msg: m.errorMessage || '' }))
  else message.success(t('ecomCs.message.sent'))
  await afterChange()
}

const close = useAction(closeCsThread, { successKey: 'ecomCs.message.closed', reload: () => afterChange() })
const reopen = useAction(reopenCsThread, { successKey: 'ecomCs.message.reopened', reload: () => afterChange() })

function attachments(m) {
  if (!m.attachments) return []
  try {
    const list = JSON.parse(m.attachments)
    return Array.isArray(list) ? list.filter((u) => /^https?:\/\//i.test(u)) : []
  } catch {
    return []
  }
}
const fileName = (url) => url.split('?')[0].split('/').pop() || url
</script>

<style scoped>
.chat-root,
.chat-root > :deep(.ant-spin-nested-loading),
.chat-root > :deep(.ant-spin-nested-loading > .ant-spin-container) {
  height: 100%;
}
.chat {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.chat-head {
  padding: 12px 16px;
  border-bottom: 1px solid var(--app-border);
}
.who {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.buyer {
  font-size: 15px;
  font-weight: 600;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin: 4px 0 8px;
}
.late {
  color: #ff4d4f;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tickets .ticket {
  margin-right: 12px;
}
.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  background: var(--app-bg);
}
.empty {
  padding-top: 60px;
}
.msg {
  display: flex;
  flex-direction: column;
  margin-bottom: 14px;
}
.msg.mine {
  align-items: flex-end;
}
.msg.theirs {
  align-items: flex-start;
}
.bubble {
  max-width: 72%;
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--app-card);
  box-shadow: var(--app-shadow);
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.mine .bubble {
  background: var(--app-primary-bg);
}
.bubble.failed {
  border: 1px solid #ff4d4f;
}
.info {
  margin-top: 2px;
}
.translation {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--app-border);
  color: var(--app-text-2);
}
.translate-link {
  font-size: 12px;
}
.failed-line {
  font-size: 12px;
}
.failed-line .error {
  color: #ff4d4f;
}
.composer {
  border-top: 1px solid var(--app-border);
  padding: 10px 16px 12px;
}
.tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 8px;
}
.tools > * {
  white-space: nowrap;
}
.counter {
  margin-left: auto;
}
.send-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
</style>
