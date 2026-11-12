<template>
  <a-modal v-model:open="open" :title="$t('ecomAds.import.title')" :confirm-loading="saving" :ok-button-props="{ disabled: !file || !form.storeId }" width="620px" @ok="submit">
    <a-alert type="info" show-icon :message="$t('ecomAds.import.tip')" class="tip" />
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.import.store')" required>
            <StoreSelect v-model:value="form.storeId" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.import.reportType')">
            <a-select v-model:value="form.reportType" :options="typeOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.import.adType')">
            <a-select v-model:value="form.adType" :options="toOptions(dict.ecom_ad_type)" allow-clear />
          </a-form-item>
        </a-col>
      </a-row>
      <a-upload-dragger :before-upload="pick" :show-upload-list="false" accept=".xlsx,.csv,.tsv,.txt">
        <p class="ant-upload-drag-icon"><InboxOutlined /></p>
        <p>{{ file ? `${file.name} (${$t('ecomAds.import.rows', { n: data.rows.length })})` : $t('ecomAds.import.fileTip') }}</p>
      </a-upload-dragger>
    </a-form>
    <a-alert v-if="errors.length" type="error" :message="$t('ecomAds.import.failed')" class="errors">
      <template #description>
        <div v-for="(e, i) in errors" :key="i">{{ e }}</div>
      </template>
    </a-alert>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { importAdReport } from '@/api/ecom/ads'
import { useDict } from '@/composables/useDict'
import { EcomAdReportType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { readReportFile } from './reportFile'

/** 导入亚马逊广告后台报告（xlsx / csv，英文表头），open(店铺) 打开，导入成功后触发 success */
const emit = defineEmits(['success'])
const dict = useDict('ecom_ad_type')
const typeOptions = [
  { value: '', label: t('ecomAds.import.auto') },
  ...Object.values(EcomAdReportType).map((v) => ({ value: v, label: t(`ecomAds.enum.reportType.${v}`) }))
]
const open = ref(false)
const saving = ref(false)
const file = ref(null)
const data = ref({ headers: [], rows: [] })
const errors = ref([])
const form = reactive({ storeId: undefined, reportType: '', adType: undefined })

function show(storeId) {
  form.storeId = storeId
  form.reportType = ''
  file.value = null
  data.value = { headers: [], rows: [] }
  errors.value = []
  open.value = true
}

async function pick(f) {
  errors.value = []
  try {
    data.value = await readReportFile(f)
    file.value = f
  } catch (e) {
    message.error(t('ecomAds.import.readFailed', { msg: e.message }))
  }
  return false
}

async function submit() {
  saving.value = true
  try {
    const r = await importAdReport({
      storeId: form.storeId,
      reportType: form.reportType || null,
      adType: form.adType || null,
      headers: data.value.headers,
      rows: data.value.rows,
      firstRowNum: 2
    })
    if (r.errors?.length) {
      errors.value = r.errors
      return
    }
    message.success(t('ecomAds.import.done', {
      type: t(`ecomAds.enum.reportType.${r.reportType}`), rows: r.rows, written: r.written, c: r.campaignsCreated, g: r.adGroupsCreated, t: r.targetsCreated
    }))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.errors {
  margin-top: 12px;
}
</style>
