<template>
  <div class="cover">
    <template v-if="days != null">
      <div class="cover-head">
        <b :class="['cover-days', tone]">{{ qty(days, 1) }}</b>
        <span class="sub-text">{{ $t('ecomUiOps.replenish.dayUnit') }}</span>
      </div>
      <a-tooltip v-if="target > 0" :title="$t('ecomUiOps.replenish.coverTip', { days: qty(days, 1), target })">
        <div class="cover-bar">
          <div :class="['cover-fill', tone]" :style="{ width: percent + '%' }" />
          <div v-if="leadMark" class="cover-mark" :style="{ left: leadMark + '%' }" />
        </div>
      </a-tooltip>
    </template>
    <span v-else class="sub-text">{{ $t('ecom.replenish.noSales') }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { qty } from '@/utils/number'
import { ReplenishLevel } from '@/constants/enums'

/**
 * 可售天数单元格：天数（按紧急程度着色）+ 覆盖进度条（相对目标天数 = 补货周期 + 安全天数，竖线为补货周期），
 * 一眼看出离断货还有多久
 */
const props = defineProps({
  days: { type: [Number, String], default: null },
  level: String,
  /** 补货 / 采购周期（天） */
  lead: { type: [Number, String], default: 0 },
  /** 安全天数 */
  safety: { type: [Number, String], default: 0 }
})
const target = computed(() => (Number(props.lead) || 0) + (Number(props.safety) || 0))
const percent = computed(() => (target.value > 0 ? Math.max(3, Math.min(100, (Number(props.days) / target.value) * 100)) : 0))
const leadMark = computed(() => (target.value > 0 && Number(props.safety) > 0 ? Math.min(100, (Number(props.lead) / target.value) * 100) : 0))
const tone = computed(() => (props.level === ReplenishLevel.URGENT ? 'urgent' : props.level === ReplenishLevel.WARN ? 'warn' : 'ok'))
</script>

<style scoped>
.cover {
  min-width: 0;
}
.cover-head {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 2px;
}
.cover-days {
  font-size: 14px;
}
.cover-bar {
  position: relative;
  height: 4px;
  margin-top: 4px;
  border-radius: 2px;
  background: var(--app-border, #f0f0f0);
}
.cover-fill {
  height: 100%;
  border-radius: 2px;
}
.cover-mark {
  position: absolute;
  top: -2px;
  width: 1px;
  height: 8px;
  background: var(--app-text-3);
}
.urgent {
  color: #ff4d4f;
}
.warn {
  color: #fa8c16;
}
.cover-fill.urgent {
  background: #ff4d4f;
}
.cover-fill.warn {
  background: #faad14;
}
.cover-fill.ok {
  background: #52c41a;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
