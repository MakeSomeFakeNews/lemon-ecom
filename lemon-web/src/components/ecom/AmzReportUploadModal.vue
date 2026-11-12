<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.upload.title')" :confirm-loading="saving" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item :label="$t('ecomFba.common.store')" name="storeId" :rules="[{ required: true, message: $t('ecomFba.report.selectStore') }]">
        <StoreSelect v-model:value="form.storeId" :platform="EcomPlatform.AMAZON" />
      </a-form-item>
      <a-form-item :label="$t('ecomFba.report.reportType')" name="reportType" :rules="[{ required: true, message: $t('ecomFba.report.selectType') }]">
        <a-select v-model:value="form.reportType" :options="typeOptions" />
      </a-form-item>
      <a-form-item :label="$t('ecomFba.upload.file')" :extra="$t('ecomFba.upload.fileTip')">
        <a-upload :before-upload="pick" :show-upload-list="false" accept=".txt,.tsv,.csv,.tab">
          <a-button><template #icon><UploadOutlined /></template>{{ $t('ecomFba.upload.pickFile') }}</a-button>
        </a-upload>
        <span v-if="fileName" class="file-name">{{ fileName }}</span>
      </a-form-item>
      <a-form-item :label="$t('ecomFba.upload.snapshotDate')" :extra="$t('ecomFba.upload.snapshotTip')">
        <a-date-picker v-model:value="form.snapshotDate" value-format="YYYY-MM-DD" class="full" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { uploadAmzReport } from '@/api/ecom/fba'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { AmzReportType, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 上传亚马逊报告文件（没有接口授权时使用）：读取为文本后提交 /ecom/amz-report/upload，后端按报告类型解析写入。
 * - types：可选的报告类型（默认全部）
 * ref 调用 open(storeId?, type?)；@success(taskId) 处理完成后触发
 */
const props = defineProps({ types: { type: Array, default: () => Object.values(AmzReportType) } })
const emit = defineEmits(['success'])
const dict = useDict('ecom_amz_report_type')

const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const content = ref('')
const fileName = ref('')
const typeOptions = computed(() => props.types.map((v) => ({ value: v, label: findDict(dict.ecom_amz_report_type, v)?.label || v })))

function open(storeId, type) {
  form.value = { storeId: storeId || undefined, reportType: type || props.types[0], snapshotDate: undefined }
  content.value = ''
  fileName.value = ''
  visible.value = true
}

function pick(file) {
  const reader = new FileReader()
  reader.onload = () => {
    content.value = String(reader.result || '')
    fileName.value = file.name
  }
  reader.readAsText(file)
  return false
}

async function submit() {
  await formRef.value.validate()
  if (!content.value) {
    message.warning(t('ecomFba.upload.selectFile'))
    return
  }
  saving.value = true
  try {
    const id = await uploadAmzReport({ ...form.value, content: content.value })
    message.success(t('common.msg.operationSuccess'))
    visible.value = false
    emit('success', id)
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
.file-name {
  margin-left: 8px;
  color: var(--app-text-3);
}
</style>
