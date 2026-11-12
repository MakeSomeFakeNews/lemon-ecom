<template>
  <div class="thread-list">
    <a-spin :spinning="loading && !list.length" class="spin">
      <div v-if="!list.length && !loading" class="empty"><a-empty :description="$t('ecomCs.message.noThreads')" /></div>
      <div
        v-for="item in list"
        :key="item.id"
        class="thread"
        :class="{ active: item.id === selectedId, overdue: item.overdue }"
        @click="emit('select', item)"
      >
        <a-badge :count="item.unread" :overflow-count="99" size="small" :offset="[-2, 2]">
          <span class="avatar" :style="{ background: avatarColor(item.buyerName || item.conversationId) }">{{ initial(item.buyerName || item.conversationId) }}</span>
        </a-badge>
        <div class="thread-main">
          <div class="line1">
            <span class="buyer" :class="{ unread: item.unread }">{{ item.buyerName || item.conversationId }}</span>
            <span class="time">{{ shortTime(item.lastMessageTime) }}</span>
          </div>
          <div class="line2" :class="{ unread: item.unread }">{{ item.lastMessage || '-' }}</div>
          <div class="line3">
            <DictTag type="ecom_platform" :value="item.platform" />
            <span class="store" :title="item.storeName">{{ item.storeName }}</span>
            <a-tag v-if="item.overdue" color="red" :bordered="false">{{ $t('ecomCs.message.overdue') }}</a-tag>
            <a-tag v-for="tag in splitTags(item.tags)" :key="tag" :bordered="false">{{ tag }}</a-tag>
          </div>
          <div v-if="item.status === CsThreadStatus.PENDING && item.replyDeadline" class="deadline" :class="{ late: item.overdue }">
            <ClockCircleOutlined /> {{ $t('ecomCs.message.deadline') }} {{ shortTime(item.replyDeadline) }}
          </div>
        </div>
      </div>
      <div v-if="list.length < total" class="more">
        <a-button type="link" :loading="loading" @click="loadMore">{{ $t('ecomCs.message.loadMore', { n: total - list.length }) }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ClockCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { pageCsThread } from '@/api/ecom/cs'
import { CsThreadStatus } from '@/constants/enums'

/**
 * 会话列表：按条件分页加载（滚动到底点「加载更多」），待回复的在前、按最近消息时间倒序；超时的标红。
 * - query：列表条件（变化时重新加载第一页）
 * - selectedId：当前会话
 * - @select(thread)；reload(keep) 重新加载（keep 为 true 时保留已加载的条数）
 */
const props = defineProps({
  query: { type: Object, required: true },
  selectedId: Number
})
const emit = defineEmits(['select', 'loaded'])

const PAGE = 30
const list = ref([])
const total = ref(0)
const loading = ref(false)
let current = 1

async function fetch(page, size) {
  loading.value = true
  try {
    const res = await pageCsThread({ ...props.query, current: page, size })
    return res
  } finally {
    loading.value = false
  }
}

async function reload(keep = false) {
  const size = keep ? Math.min(Math.max(list.value.length, PAGE), 300) : PAGE
  const res = await fetch(1, size)
  list.value = res.records || []
  total.value = res.total || 0
  current = Math.ceil(list.value.length / PAGE) || 1
  emit('loaded', list.value)
}

async function loadMore() {
  const res = await fetch(current + 1, PAGE)
  current++
  const ids = new Set(list.value.map((t) => t.id))
  list.value.push(...(res.records || []).filter((t) => !ids.has(t.id)))
  total.value = res.total || 0
}

watch(() => props.query, () => reload(), { immediate: true })

const AVATAR_COLORS = ['#5b8ff9', '#61ddaa', '#f6bd16', '#7262fd', '#78d3f8', '#9661bc', '#f6903d', '#008685', '#f08bb4']
/** 头像：买家名称首字母，颜色按名称固定 */
const initial = (name) => String(name || '?').trim().charAt(0).toUpperCase()
const avatarColor = (name) => AVATAR_COLORS[[...String(name || '')].reduce((s, c) => s + c.charCodeAt(0), 0) % AVATAR_COLORS.length]
const splitTags = (tags) => (tags ? tags.split(',').filter(Boolean).slice(0, 3) : [])
/** 今天只显示时间，今年显示月日 */
function shortTime(v) {
  if (!v) return ''
  const d = dayjs(v)
  if (d.isSame(dayjs(), 'day')) return d.format('HH:mm')
  return d.isSame(dayjs(), 'year') ? d.format('MM-DD HH:mm') : d.format('YYYY-MM-DD')
}

defineExpose({ reload })
</script>

<style scoped>
.thread-list {
  height: 100%;
  overflow-y: auto;
}
.spin {
  min-height: 200px;
}
.empty {
  padding-top: 80px;
}
.thread {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--app-border);
  border-left: 3px solid transparent;
  cursor: pointer;
}
.thread:hover {
  background: var(--app-hover);
}
.thread.active {
  border-left-color: var(--app-primary, #1677ff);
  background: var(--app-primary-bg);
}
.thread.overdue:not(.active) {
  border-left-color: var(--ant-color-error, #ff4d4f);
}
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  font-weight: 600;
}
.thread-main {
  flex: 1;
  min-width: 0;
}
.line1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.buyer {
  overflow: hidden;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.time {
  flex: none;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
}
.line2 {
  margin: 2px 0 4px;
  overflow: hidden;
  color: var(--app-text-3);
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.line2.unread {
  color: var(--app-text);
}
.line3 {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
}
.line3 :deep(.ant-tag) {
  margin-inline-end: 0;
  padding: 0 4px;
  font-size: 11px;
  line-height: 16px;
}
.store {
  overflow: hidden;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.deadline {
  margin-top: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
.deadline.late {
  color: var(--ant-color-error, #ff4d4f);
}
.more {
  padding: 8px;
  text-align: center;
}
</style>
