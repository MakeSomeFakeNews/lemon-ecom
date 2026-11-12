<template>
  <a-modal v-model:open="open" :title="$t('ecomOrder.ops.holdTitle')" :confirm-loading="saving" @ok="submit">
    <a-alert type="warning" show-icon class="tip" :message="$t('ecomOrder.ops.holdCount', { n: ids.length })" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item :label="$t('ecomOrder.ops.holdReason')" name="reason" :rules="[{ required: true, whitespace: true, message: $t('ecomOrder.ops.holdReasonPh') }]">
        <a-textarea v-model:value="form.reason" :maxlength="255" :auto-size="{ minRows: 2, maxRows: 4 }" :placeholder="$t('ecomOrder.ops.holdReasonPh')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { holdEcomOrders } from '@/api/ecom/order'

/** 挂起订单（人工拦截，原因必填）；ref 调用 open(ids)，完成后触发 done */
const emit = defineEmits(['done'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const ids = ref([])
const formRef = ref()
const form = reactive({ reason: '' })

function show(orderIds) {
  ids.value = [...orderIds]
  form.reason = ''
  open.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await holdEcomOrders(ids.value, form.reason.trim())
    message.success(t('ecomOrder.ops.held'))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
</style>
