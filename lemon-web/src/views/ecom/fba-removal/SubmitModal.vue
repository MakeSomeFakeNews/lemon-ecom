<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.removal.submitTitle', { no: record.docNo || '' })" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon :message="$t('ecomFba.removal.submitTip')" class="block" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item :label="$t('ecomFba.removal.platformOrderId')" name="platformOrderId" :rules="[{ required: true, whitespace: true }]">
        <a-input v-model:value="form.platformOrderId" :maxlength="64" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { submitRemoval } from '@/api/ecom/fba'
import { t } from '@/locales'

/** 提交移除订单（填写亚马逊移除订单号）：ref 调用 open(record)；@success */
const emit = defineEmits(['success'])
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const record = ref({})
const form = ref({ platformOrderId: '' })

function open(r) {
  record.value = r
  form.value = { platformOrderId: r.platformOrderId || '' }
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await submitRemoval(record.value.id, form.value.platformOrderId.trim())
    message.success(t('ecomFba.removal.submitted'))
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
</style>
