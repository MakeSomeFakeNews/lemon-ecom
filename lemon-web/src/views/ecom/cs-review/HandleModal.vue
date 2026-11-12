<template>
  <a-modal :open="open" :title="$t('ecomCs.review.handleTitle')" :confirm-loading="saving" @ok="submit" @cancel="open = false">
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomCs.review.handleStatus')" required>
        <a-radio-group v-model:value="form.handleStatus" :options="toOptions(dict.ecom_cs_review_status)" option-type="button" button-style="solid" />
      </a-form-item>
      <a-form-item :label="$t('ecomCs.review.handler')">
        <IdentityUserSelect v-model:value="form.handlerId" :label="form.handlerName" />
      </a-form-item>
      <a-form-item :label="$t('ecomCs.review.handleNote')">
        <a-textarea v-model:value="form.handleNote" :maxlength="500" show-count :auto-size="{ minRows: 3, maxRows: 6 }" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { handleCsFeedback, handleCsReview } from '@/api/ecom/cs'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/**
 * 处理评价 / 店铺反馈：处理状态、处理人、处理说明。open(kind: 'review' | 'feedback', record)，成功后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_cs_review_status')
const open = ref(false)
const saving = ref(false)
const kind = ref('review')
const form = reactive({ id: null, handleStatus: undefined, handlerId: undefined, handlerName: undefined, handleNote: '' })

function show(k, r) {
  kind.value = k
  Object.assign(form, { id: r.id, handleStatus: r.handleStatus, handlerId: r.handlerId || undefined, handlerName: r.handlerName, handleNote: r.handleNote || '' })
  open.value = true
}

async function submit() {
  if (!form.handleStatus) return message.warning(t('ecomCs.review.handleStatusRequired'))
  saving.value = true
  try {
    const body = { handleStatus: form.handleStatus, handlerId: form.handlerId ?? null, handleNote: form.handleNote || null }
    await (kind.value === 'review' ? handleCsReview(form.id, body) : handleCsFeedback(form.id, body))
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
