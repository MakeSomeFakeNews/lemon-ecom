<template>
  <a-modal v-model:open="visible" :title="$t('ecomOwh.cancel.title', { no: record?.outboundNo })" :confirm-loading="saving" ok-type="danger" @ok="submit">
    <p class="tip">{{ $t('ecomOwh.cancel.tip') }}</p>
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOwh.cancel.reason')">
        <a-input v-model:value="reason" :maxlength="255" />
      </a-form-item>
      <a-form-item>
        <a-checkbox v-model:checked="cancelPackage">{{ $t('ecomOwh.cancel.cancelPackage') }}</a-checkbox>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { cancelOwhOutbound } from '@/api/ecom/owh'
import { OwhOutboundStatus } from '@/constants/enums'

/** 取消 / 截单：服务商已开始作业时为截单中，之后同步结果；可同时取消包裹（订单回到待审核）。open(record) */
const emit = defineEmits(['done'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const record = ref(null)
const reason = ref('')
const cancelPackage = ref(false)

function open(r) {
  record.value = r
  reason.value = ''
  cancelPackage.value = false
  visible.value = true
}

async function submit() {
  saving.value = true
  try {
    const st = await cancelOwhOutbound(record.value.id, { reason: reason.value || null, cancelPackage: cancelPackage.value })
    message.success(st === OwhOutboundStatus.INTERCEPTING ? t('ecomOwh.cancel.intercepting') : t('ecomOwh.cancel.cancelled'))
    visible.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  color: var(--app-text-3);
}
</style>
