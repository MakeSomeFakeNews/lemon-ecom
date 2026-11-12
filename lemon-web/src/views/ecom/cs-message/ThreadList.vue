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
        <div class="line1">
          <a-badge :count="item.unread" :overflow-count="99" size="small">
            <span class="buyer">{{ item.buyerName || item.conversationId }}</span>
          </a-badge>
          <span class="time">{{ shortTime(item.lastMessageTime) }}</span>
        </div>
        <div class="line2">{{ item.lastMessage || '-' }}</div>
        <div class="line3">
          <span class="store">{{ item.storeName }}</span>
          <DictTag type="ecom_platform" :value="item.platform" />
          <a-tag v-if="item.overdue" color="red" :bordered="false">{{ $t('ecomCs.message.overdue') }}</a-tag>
          <DictTag v-else type="ecom_cs_thread_status" :value="item.status" />
          <a-tag v-for="tag in splitTags(item.tags)" :key="tag" :bordered="false">{{ tag }}</a-tag>
        </div>
        <div v-if="item.status === CsThreadStatus.PENDING && item.replyDeadline" class="deadline" :class="{ late: item.overdue }">
          {{ $t('ecomCs.message.deadline') }} {{ item.replyDeadline }}
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
const emit = defineEmits(['select'])

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
}

async function loadMore() {
  const res = await fetch(current + 1, PAGE)
  current++
  const ids = new Set(list.value.map((t) => t.id))
  list.value.push(...(res.records || []).filter((t) => !ids.has(t.id)))
  total.value = res.total || 0
}

watch(() => props.query, () => reload(), { immediate: true })

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
  padding: 10px 14px;
  border-bottom: 1px solid var(--app-border);
  cursor: pointer;
}
.thread:hover {
  background: var(--app-hover);
}
.thread.active {
  background: var(--app-primary-bg);
}
.thread.overdue {
  border-left: 3px solid #ff4d4f;
}
.line1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.buyer {
  font-weight: 500;
  padding-right: 10px;
}
.time {
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
}
.line2 {
  color: var(--app-text-2);
  font-size: 13px;
  margin: 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.line3 {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.store {
  color: var(--app-text-3);
  font-size: 12px;
  margin-right: 4px;
}
.deadline {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-3);
}
.deadline.late {
  color: #ff4d4f;
}
.more {
  text-align: center;
  padding: 8px;
}
</style>
