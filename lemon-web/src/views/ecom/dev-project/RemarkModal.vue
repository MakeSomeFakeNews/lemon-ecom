<template>
  <a-modal v-model:open="open" :title="title" :confirm-loading="saving" @ok="ok">
    <a-form layout="vertical">
      <a-form-item v-if="statuses.length" :label="$t('ecomDev.move.target')" required>
        <a-radio-group v-model:value="status" button-style="solid">
          <a-radio-button v-for="s in statuses" :key="s" :value="s">{{ statusLabel(s) }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item :label="remarkLabel" :required="remarkRequired">
        <a-textarea v-model:value="remark" :maxlength="255" :rows="3" show-count />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DevProjectStatus } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

/**
 * 填写说明的确认弹窗：流转（选择目标状态，放弃时原因必填）、审批通过（意见）、驳回（原因必填）。
 * ref 调用 open({ title, statuses, status, remarkLabel, remarkRequired, onOk(remark, status) })
 */
const { t } = useI18n()
const dict = useDict('ecom_dev_status')
const open = ref(false)
const saving = ref(false)
const remark = ref('')
const status = ref()
const opts = ref({})

const title = computed(() => opts.value.title)
const statuses = computed(() => opts.value.statuses || [])
const remarkLabel = computed(() =>
  status.value === DevProjectStatus.ABANDONED ? t('ecomDev.move.abandonReason') : opts.value.remarkLabel || t('common.field.remark')
)
const remarkRequired = computed(() => !!opts.value.remarkRequired || status.value === DevProjectStatus.ABANDONED)
const statusLabel = (s) => findDict(dict.ecom_dev_status, s)?.label || s

function show(o) {
  opts.value = o
  remark.value = ''
  status.value = o.status || (o.statuses || [])[0]
  open.value = true
}

async function ok() {
  if (remarkRequired.value && !remark.value.trim()) {
    message.warning(t('ecomDev.move.remarkRequired'))
    return
  }
  saving.value = true
  try {
    await opts.value.onOk(remark.value.trim() || null, status.value)
    open.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
