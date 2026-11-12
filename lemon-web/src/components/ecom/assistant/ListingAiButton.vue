<template>
  <span v-if="can">
    <a-tooltip v-if="!status.available" :title="status.reason || $t('ecomAsst.common.aiUnavailable')">
      <a-button size="small" disabled><template #icon><RobotOutlined /></template>{{ $t('ecomAsst.listing.button') }}</a-button>
    </a-tooltip>
    <a-button v-else size="small" class="ai-btn" @click="modalRef.open()"><template #icon><RobotOutlined /></template>{{ $t('ecomAsst.listing.button') }}</a-button>
    <ListingAiModal ref="modalRef" :item-id="itemId" :form="form" :words="words" @apply="(v) => emit('apply', v)" />
  </span>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RobotOutlined } from '@ant-design/icons-vue'
import { useEcomAi } from '@/components/ecom/ai/useEcomAi'
import { useUserStore } from '@/store/user'
import ListingAiModal from './ListingAiModal.vue'

/**
 * 刊登编辑页的「AI 生成 / 优化」：需要 ecom:publish:ai 权限；AI 未配置或没有开启「AI 写作」时禁用并提示原因。
 * 生成结果确认后触发 apply({ title?, bullets?, description? })（只含选择应用的部分）
 */
defineProps({
  itemId: Number,
  form: { type: Object, required: true },
  words: { type: Array, default: () => [] }
})
const emit = defineEmits(['apply'])
const user = useUserStore()
const can = computed(() => user.hasPerm('ecom:publish:ai'))
const { status } = useEcomAi()
const modalRef = ref()
</script>

<style scoped>
.ai-btn {
  border-color: #9254de;
  color: #722ed1;
}
</style>
