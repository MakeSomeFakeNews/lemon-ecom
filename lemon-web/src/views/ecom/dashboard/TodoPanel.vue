<template>
  <div class="alerts">
    <div v-for="c in items" :key="c.key" class="page-card alert" :class="[c.tone, { clickable: !!c.path }]" @click="go(c)">
      <span class="alert-icon"><component :is="iconOf(c.icon)" /></span>
      <div class="alert-body">
        <div class="stat-label">{{ c.title }}</div>
        <div class="stat-value">{{ num(c.count) }}<span class="stat-unit">{{ c.unit }}</span></div>
      </div>
      <RightOutlined v-if="c.path" class="alert-arrow" />
    </div>
    <div v-if="loaded && !items.length" class="page-card alert done">
      <span class="alert-icon"><CheckCircleOutlined /></span>
      <div class="alert-body">
        <div class="stat-label">{{ $t('ecomFix.dash.todoNone') }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
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
 * 待处理事项：后端各功能注册的待办（订单、货件、补货、差评、买家消息超时、店铺授权失效 / 同步失败、广告同步失败等），
 * 点击跳转到对应页面（query 作为初始筛选）。items 为 /ecom/dashboard/todos 的结果
 */
defineProps({ items: { type: Array, default: () => [] }, loaded: { type: Boolean, default: false } })
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
const iconOf = (name) => ICONS[name] || BellOutlined
const go = (c) => c.path && router.push({ path: c.path, query: c.query || {} })
</script>

<style scoped>
.alerts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
}
.alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--app-card);
  transition: transform 0.2s, box-shadow 0.2s;
}
.alert.clickable {
  cursor: pointer;
}
.alert.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.alert-body {
  flex: 1;
  min-width: 0;
}
.stat-label {
  font-size: 13px;
  color: var(--app-text-3);
}
.stat-value {
  margin-top: 2px;
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text);
}
.stat-unit {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 400;
  color: var(--app-text-3);
}
.alert-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex: none;
}
.alert.warning .alert-icon {
  color: #d48806;
  background: rgba(250, 178, 25, 0.14);
}
.alert.danger .alert-icon {
  color: var(--status-critical, #cf1322);
  background: rgba(208, 59, 59, 0.1);
}
.alert.info .alert-icon {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.alert.done .alert-icon {
  color: #389e0d;
  background: rgba(82, 196, 26, 0.12);
}
.alert-arrow {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>

<style>
/* 暗色：待处理事项图标底色调淡（需要匹配根元素的 data-theme，不能放在 scoped 中） */
html[data-theme='dark'] .alerts .alert.warning .alert-icon {
  color: #ffd666;
  background: rgba(250, 178, 25, 0.1);
}
html[data-theme='dark'] .alerts .alert.danger .alert-icon {
  color: #ff7875;
  background: rgba(208, 59, 59, 0.14);
}
</style>
