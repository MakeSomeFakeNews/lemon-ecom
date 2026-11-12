<template>
  <a-card :bordered="false" class="page-card todo-card">
    <template #title>
      <span>{{ $t('ecomDat.dash.todoTitle') }}</span>
      <span v-if="items.length" class="todo-total">{{ $t('ecomDat.dash.todoTotal', { n: items.length }) }}</span>
    </template>
    <div v-if="!loaded" class="todo-grid">
      <a-skeleton-button v-for="i in 6" :key="i" active block class="todo-skeleton" />
    </div>
    <div v-else-if="items.length" class="todo-grid">
      <div
        v-for="c in sorted"
        :key="c.key"
        class="todo"
        :class="[c.tone, { clickable: !!c.path }]"
        :title="c.title"
        @click="go(c)"
      >
        <span class="todo-icon"><component :is="iconOf(c.icon)" /></span>
        <div class="todo-body">
          <div class="todo-label">{{ c.title }}</div>
          <div class="todo-value">{{ num(c.count) }}<span class="todo-unit">{{ c.unit }}</span></div>
        </div>
        <RightOutlined v-if="c.path" class="todo-arrow" />
      </div>
    </div>
    <div v-else class="todo-done">
      <CheckCircleOutlined class="done-icon" />
      <span>{{ $t('ecomFix.dash.todoNone') }}</span>
    </div>
  </a-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertOutlined,
  AuditOutlined,
  BellOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloudUploadOutlined,
  DisconnectOutlined,
  DislikeOutlined,
  FieldTimeOutlined,
  FileExclamationOutlined,
  MessageOutlined,
  RightOutlined,
  SendOutlined,
  SoundOutlined,
  SyncOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import { num } from '@/utils/number'

/**
 * 待办与预警：后端各功能注册的待办（订单、货件、补货、差评、买家消息超时、店铺授权失效 / 同步失败、广告同步失败等），
 * 预警（danger）排在前面，点击跳转到对应页面（query 作为初始筛选）。items 为 /ecom/dashboard/todos 的结果
 */
const props = defineProps({ items: { type: Array, default: () => [] }, loaded: { type: Boolean, default: false } })
const router = useRouter()
/** 后端返回的图标名 → 组件（只引入用到的图标；不认识的显示铃铛） */
const ICONS = {
  AlertOutlined,
  AuditOutlined,
  ClockCircleOutlined,
  CloudUploadOutlined,
  DisconnectOutlined,
  DislikeOutlined,
  FieldTimeOutlined,
  FileExclamationOutlined,
  MessageOutlined,
  SendOutlined,
  SoundOutlined,
  SyncOutlined,
  WarningOutlined
}
const TONE_ORDER = { danger: 0, warning: 1, info: 2 }
const sorted = computed(() => [...props.items].sort((a, b) => (TONE_ORDER[a.tone] ?? 3) - (TONE_ORDER[b.tone] ?? 3)))
const iconOf = (name) => ICONS[name] || BellOutlined
const go = (c) => c.path && router.push({ path: c.path, query: c.query || {} })
</script>

<style scoped>
.todo-card :deep(.ant-card-body) {
  padding: 12px 16px 16px;
}
.todo-total {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--app-text-3);
}
.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}
.todo-skeleton {
  height: 58px !important;
}
.todo {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-hover);
  border: 1px solid transparent;
  transition: border-color 0.2s, background 0.2s;
}
.todo.clickable {
  cursor: pointer;
}
.todo.clickable:hover {
  border-color: var(--app-primary);
  background: var(--app-card);
}
.todo-body {
  flex: 1;
  min-width: 0;
}
.todo-label {
  font-size: 12px;
  color: var(--app-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.todo-value {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--app-text);
}
.todo.danger .todo-value {
  color: var(--status-critical, #cf1322);
}
.todo-unit {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: var(--app-text-3);
}
.todo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex: none;
}
.todo.warning .todo-icon {
  color: #d48806;
  background: rgba(250, 178, 25, 0.14);
}
.todo.danger .todo-icon {
  color: var(--status-critical, #cf1322);
  background: rgba(208, 59, 59, 0.1);
}
.todo.info .todo-icon {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.todo-arrow {
  color: var(--app-text-3);
  font-size: 11px;
}
.todo-done {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: var(--app-text-2);
}
.done-icon {
  color: #389e0d;
  font-size: 18px;
}
</style>

<style>
/* 暗色：待处理事项图标底色调淡（需要匹配根元素的 data-theme，不能放在 scoped 中） */
html[data-theme='dark'] .todo-card .todo.warning .todo-icon {
  color: #ffd666;
  background: rgba(250, 178, 25, 0.1);
}
html[data-theme='dark'] .todo-card .todo.danger .todo-icon {
  color: #ff7875;
  background: rgba(208, 59, 59, 0.14);
}
html[data-theme='dark'] .todo-card .todo.danger .todo-value {
  color: #ff7875;
}
</style>
