<template>
  <div>
    <div v-if="can('ecom:order:tag')" class="note-input">
      <a-input v-model:value="note" :maxlength="500" :placeholder="$t('ecomOrd.note.placeholder')" @press-enter="submit" />
      <a-button type="primary" :loading="adding" :disabled="!note.trim()" @click="submit">{{ $t('ecomOrd.note.add') }}</a-button>
    </div>
    <a-timeline v-if="logs.length" class="timeline">
      <a-timeline-item v-for="l in logs" :key="l.id" :color="colorOf(l.action)">
        <div class="event-head">
          <span class="event-title">{{ $t(`ecomOrdUi.logAction.${l.action}`) }}</span>
          <span class="sub">{{ l.createTime }}</span>
          <span class="sub">· {{ l.createByName || $t('ecomOrdUi.timeline.system') }}</span>
        </div>
        <div v-if="l.content" class="event-content" :class="{ note: l.action === EcomOrderLogAction.NOTE }">{{ l.content }}</div>
      </a-timeline-item>
    </a-timeline>
    <a-empty v-else :image="simpleImage" :description="$t('ecomOrdUi.timeline.empty')" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Empty } from 'ant-design-vue'
import { useUserStore } from '@/store/user'
import { EcomOrderLogAction } from '@/constants/enums'

/**
 * 订单详情的处理记录：读订单操作日志（ecom_order_log，新的在前，带操作人，系统执行的显示「系统」），
 * 顶部可添加内部备注（触发 add(content, done)，成功后调用 done 清空输入）
 */
defineProps({
  logs: { type: Array, default: () => [] },
  adding: Boolean
})
const emit = defineEmits(['add'])
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const note = ref('')

function submit() {
  const content = note.value.trim()
  if (!content) return
  emit('add', content, () => (note.value = ''))
}

const A = EcomOrderLogAction
const COLORS = {
  green: [A.AUDIT, A.SHIP, A.SHIP_CONFIRM, A.UNHOLD, A.RECHECK],
  red: [A.EXCEPTION, A.OUT_OF_STOCK, A.CANCEL, A.HOLD],
  orange: [A.NOTE, A.RETURN, A.REFUND, A.RESEND],
  blue: [A.LABEL, A.PRINT, A.OUTBOUND, A.PACKAGE, A.ASSIGN, A.EDIT, A.RULE, A.REMATCH]
}
const colorOf = (action) => Object.keys(COLORS).find((c) => COLORS[c].includes(action)) || 'gray'
</script>

<style scoped>
.note-input {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.timeline {
  max-height: 520px;
  padding-top: 4px;
  overflow-y: auto;
}
.event-head {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: baseline;
}
.event-title {
  font-weight: 500;
}
.event-content {
  margin-top: 2px;
  color: var(--app-text-2);
  white-space: pre-wrap;
  word-break: break-word;
}
.event-content.note {
  padding: 4px 8px;
  border-radius: 4px;
  background: var(--app-hover);
}
.sub {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
