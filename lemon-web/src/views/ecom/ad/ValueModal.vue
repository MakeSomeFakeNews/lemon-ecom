<template>
  <a-modal v-model:open="open" :title="title" :confirm-loading="saving" width="420px" @ok="submit">
    <a-alert v-if="tip" type="info" show-icon :message="tip" class="tip" />
    <a-form layout="vertical">
      <a-form-item :label="label">
        <a-input-number v-model:value="value" :min="0.01" :precision="2" :step="0.05" class="full" :addon-after="currency || undefined" autofocus />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { t } from '@/locales'

/**
 * 修改竞价 / 日预算：open({ title, label, value, currency, tip, submit: (value) => Promise<是否已同步到平台> })，成功后触发 success
 */
const emit = defineEmits(['success'])
const open = ref(false)
const saving = ref(false)
const title = ref('')
const label = ref('')
const tip = ref('')
const currency = ref('')
const value = ref()
let handler = null

function show(o) {
  title.value = o.title
  label.value = o.label || t('ecomAds.target.newValue')
  tip.value = o.tip || ''
  currency.value = o.currency || ''
  value.value = o.value == null ? undefined : Number(o.value)
  handler = o.submit
  open.value = true
}

async function submit() {
  if (!value.value) return
  saving.value = true
  try {
    const pushed = await handler(value.value)
    message.success(pushed ? t('ecomAds.common.pushed') : t('ecomAds.common.localOnly'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.full {
  width: 100%;
}
.tip {
  margin-bottom: 12px;
}
</style>
