<template>
  <template v-if="info">
    <a @click="router.push({ path: '/ecom/owh-outbound', query: { id: info.outboundId } })">
      <DictTag type="ecom_owh_outbound_status" :value="info.status" />
    </a>
    <a-tooltip v-if="info.errorMsg" :title="info.errorMsg">
      <ExclamationCircleOutlined class="error-icon" />
    </a-tooltip>
    <div v-if="info.owhOrderNo" class="sub-text">{{ info.owhOrderNo }}</div>
  </template>
  <span v-else>-</span>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'

/** 包裹列表中的海外仓出库状态（点状态到「海外仓出库」查看），info 为 useOwhPackages 返回的一项 */
defineProps({ info: Object })
const router = useRouter()
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-icon {
  margin-left: 4px;
  color: var(--ant-color-warning, #faad14);
  cursor: help;
}
</style>
