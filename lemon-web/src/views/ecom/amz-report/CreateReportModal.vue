<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.report.createTitle')" :confirm-loading="saving" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item :label="$t('ecomFba.common.store')" name="storeId" :rules="[{ required: true, message: $t('ecomFba.report.selectStore') }]">
        <StoreSelect v-model:value="form.storeId" :platform="EcomPlatform.AMAZON" />
      </a-form-item>
      <a-form-item :label="$t('ecomFba.report.reportType')" name="reportType" :rules="[{ required: true, message: $t('ecomFba.report.selectType') }]">
        <a-select v-model:value="form.reportType" :options="toOptions(dict.ecom_amz_report_type)" />
      </a-form-item>
      <a-form-item v-if="dated" :label="$t('ecomFba.report.dataRange')" :extra="$t('ecomFba.report.rangeTip')">
        <a-range-picker v-model:value="form.range" value-format="YYYY-MM-DD" class="full" />
      </a-form-item>
      <a-alert v-else type="info" show-icon :message="$t('ecomFba.report.noDateTip')" />
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { createAmzReport } from '@/api/ecom/fba'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { AmzReportType, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'
import { DATED_TYPES } from '../fba-health/fba'

/** 创建亚马逊报告任务：ref 调用 open(storeId?)，@success 后刷新列表 */
const emit = defineEmits(['success'])
const dict = useDict('ecom_amz_report_type')
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const dated = computed(() => DATED_TYPES.includes(form.value.reportType))

function open(storeId) {
  form.value = { storeId: storeId || undefined, reportType: AmzReportType.FBA_INVENTORY_PLANNING, range: undefined }
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    await createAmzReport({ storeId: f.storeId, reportType: f.reportType, dataStart: dated.value ? f.range?.[0] : undefined, dataEnd: dated.value ? f.range?.[1] : undefined })
    message.success(t('ecomFba.report.created'))
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
