<template>
  <a-modal v-model:open="open" :title="$t('ecomAsst.report.generateTitle')" :confirm-loading="saving" :ok-text="$t('ecomAsst.report.generateOk')" width="min(520px, 100vw)" @ok="submit">
    <a-alert type="info" show-icon class="block" :message="$t('ecomAsst.report.generateTip')" />
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomAsst.report.date')" required>
        <a-date-picker v-model:value="form.date" value-format="YYYY-MM-DD" :disabled-date="(d) => d && d.isAfter(dayjs(), 'day')" class="full" />
      </a-form-item>
      <a-form-item :label="$t('ecomAsst.common.store')" :extra="$t('ecomAsst.report.storeExtra')">
        <StoreMultiSelect v-model:value="form.storeIds" />
      </a-form-item>
      <a-form-item>
        <a-tooltip :title="aiStatus.available ? '' : aiStatus.reason || $t('ecomAsst.common.aiUnavailable')">
          <a-checkbox v-model:checked="form.ai" :disabled="!aiStatus.available">{{ $t('ecomAsst.report.withAi') }}</a-checkbox>
        </a-tooltip>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { generateOpsReport } from '@/api/ecom/assistant'
import { useEcomAi } from '@/components/ecom/ai/useEcomAi'
import StoreMultiSelect from './StoreMultiSelect.vue'

/** 立即生成日报：日期（默认昨天）、店铺范围（为空为全部可见店铺）、是否生成 AI 建议；完成后触发 done(日报 ID) */
const emit = defineEmits(['done'])
const { t } = useI18n()
const { status: aiStatus } = useEcomAi()
const open = ref(false)
const saving = ref(false)
const form = reactive({ date: undefined, storeIds: [], ai: true })

function show() {
  Object.assign(form, { date: dayjs().subtract(1, 'day').format('YYYY-MM-DD'), storeIds: [], ai: aiStatus.value.available })
  open.value = true
}

async function submit() {
  if (!form.date) {
    message.warning(t('common.rule.required', { name: t('ecomAsst.report.date') }))
    return
  }
  saving.value = true
  try {
    const id = await generateOpsReport({ date: form.date, storeIds: form.storeIds, ai: form.ai && aiStatus.value.available })
    message.success(t('ecomAsst.report.generated'))
    open.value = false
    emit('done', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
