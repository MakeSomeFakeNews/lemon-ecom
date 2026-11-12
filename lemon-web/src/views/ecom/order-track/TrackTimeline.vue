<template>
  <div class="track">
    <div class="track-head">
      <DictTag type="ecom_track_status" :value="pkg.trackStatus || EcomTrackStatus.NOT_FOUND" />
      <DictTag v-if="pkg.trackAlert" type="ecom_track_alert" :value="pkg.trackAlert" />
      <span class="no">{{ pkg.carrierName || '-' }} {{ pkg.trackingNo }}</span>
      <span v-if="pkg.trackSource" class="sub">{{ $t('ecomOrd.track.source.' + pkg.trackSource) }}</span>
      <span v-if="pkg.trackPullTime" class="sub">{{ $t('ecomOrd.track.pulledAt', { time: pkg.trackPullTime }) }}</span>
      <slot name="extra" />
    </div>
    <a-alert v-if="pkg.trackError" type="warning" show-icon :message="pkg.trackError" class="error" />
    <a-timeline v-if="events.length" class="timeline">
      <a-timeline-item v-for="(e, i) in events" :key="e.id || i" :color="color(e.status, i)">
        <div class="event">
          <span class="time">{{ e.eventTime || e.timeText || '-' }}</span>
          <DictTag type="ecom_track_status" :value="e.status" />
        </div>
        <div>{{ e.location ? `${e.location} · ` : '' }}{{ e.description }}</div>
      </a-timeline-item>
    </a-timeline>
    <a-empty v-else :image="simpleImage" :description="$t('ecomOrd.track.noEvents')" />
  </div>
</template>

<script setup>
import { Empty } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { EcomTrackStatus } from '@/constants/enums'

/** 包裹的物流轨迹时间线（新的在前）：pkg 为包裹（含 trackStatus、trackAlert、trackSource 等汇总），events 为轨迹 */
defineProps({ pkg: { type: Object, required: true }, events: { type: Array, default: () => [] } })
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

function color(status, index) {
  if (status === EcomTrackStatus.EXCEPTION || status === EcomTrackStatus.RETURNED) return 'red'
  if (status === EcomTrackStatus.DELIVERED) return 'green'
  return index === 0 ? 'blue' : 'gray'
}
</script>

<style scoped>
.track-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.no {
  font-weight: 500;
}
.sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.error {
  margin-bottom: 12px;
}
.event {
  display: flex;
  align-items: center;
  gap: 8px;
}
.time {
  color: var(--app-text-3);
  font-size: 12px;
}
.timeline {
  margin-top: 8px;
}
</style>
