<template>
  <div>
    <OrderPackages :packages="detail.packages || []" />
    <OwhOrderOutbounds :order-id="detail.id" :refresh-key="detail.updateTime" class="block-top" />
    <template v-if="tracks.length">
      <div class="sub-title">{{ $t('ecomOrd.track.title') }}</div>
      <a-collapse :default-active-key="tracks.map((x) => x.pkg.id)" ghost class="tracks">
        <a-collapse-panel v-for="tr in tracks" :key="tr.pkg.id" :header="`${tr.pkg.packageNo} · ${tr.pkg.trackingNo || '-'}`">
          <TrackTimeline :pkg="tr.pkg" :events="tr.events">
            <template #extra>
              <a v-if="can('ecom:track:sync')" @click="emit('pull', tr.pkg.id)">{{ $t('ecomOrd.track.pull') }}</a>
              <a v-if="tr.pkg.shipConfirm === 0 && can('ecom:package:ship')" @click="emit('reconfirm', tr.pkg.id)">{{ $t('ecomOrd.track.reconfirm') }}</a>
              <span v-if="tr.pkg.shipConfirmMsg" class="sub" :title="tr.pkg.shipConfirmMsg">{{ tr.pkg.shipConfirmMsg }}</span>
            </template>
          </TrackTimeline>
        </a-collapse-panel>
      </a-collapse>
    </template>
  </div>
</template>

<script setup>
import OrderPackages from './OrderPackages.vue'
import TrackTimeline from '../order-track/TrackTimeline.vue'
import OwhOrderOutbounds from '@/components/ecom/owh/OwhOrderOutbounds.vue'
import { useUserStore } from '@/store/user'

/** 订单详情的包裹与物流：包裹列表、海外仓出库单、各包裹的物流轨迹（拉取轨迹、重新回传触发 pull / reconfirm） */
defineProps({
  detail: { type: Object, required: true },
  tracks: { type: Array, default: () => [] }
})
const emit = defineEmits(['pull', 'reconfirm'])
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
</script>

<style scoped>
.block-top {
  margin-top: 12px;
}
.sub-title {
  margin: 16px 0 4px;
  font-weight: 500;
}
.tracks :deep(.ant-collapse-header) {
  padding-inline: 0 !important;
}
.tracks :deep(.ant-collapse-content-box) {
  padding-inline: 0 !important;
}
.sub {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
