<template>
  <a-modal v-model:open="open" :title="form.id ? $t('ecomDev.sample.editTitle') : $t('ecomDev.sample.add')" :confirm-loading="saving" width="720px" @ok="save">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomDev.sample.supplier')" :extra="$t('ecomDev.sample.supplierExtra')">
            <PartnerSelect v-model:value="form.supplierId" type="supplier" allow-clear :label="form.supplierLabel" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomDev.sample.supplierName')">
            <a-input v-model:value="form.supplierName" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomDev.sample.sampleUrl')">
            <a-input v-model:value="form.sampleUrl" :maxlength="500" placeholder="https://detail.1688.com/offer/..." />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.sample.sampleQty')">
            <a-input-number v-model:value="form.sampleQty" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.sample.sampleFee')">
            <a-input-number v-model:value="form.sampleFee" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.sample.quotePrice')">
            <a-input-number v-model:value="form.quotePrice" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.sample.result')">
            <a-select v-model:value="form.result" :options="toOptions(dict.ecom_dev_sample_result)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.sample.requestDate')">
            <a-date-picker v-model:value="form.requestDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.sample.arrivalDate')">
            <a-date-picker v-model:value="form.arrivalDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomDev.sample.evaluation')">
            <a-textarea v-model:value="form.evaluation" :maxlength="1000" :rows="3" show-count />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addDevSample, updateDevSample } from '@/api/ecom/dev'
import { DevSampleResult } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/** 样品记录弹窗：open(projectId, sample?)，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_dev_sample_result')

const open = ref(false)
const saving = ref(false)
const form = ref({})
let projectId

const num = (v) => (v == null ? undefined : Number(v))

function show(pid, s) {
  projectId = pid
  form.value = s
    ? {
        id: s.id,
        supplierId: s.supplierId ?? undefined,
        supplierLabel: s.supplierRecordName,
        supplierName: s.supplierName,
        sampleUrl: s.sampleUrl,
        sampleQty: s.sampleQty ?? undefined,
        sampleFee: num(s.sampleFee),
        quotePrice: num(s.quotePrice),
        requestDate: s.requestDate || undefined,
        arrivalDate: s.arrivalDate || undefined,
        result: s.result,
        evaluation: s.evaluation
      }
    : { result: DevSampleResult.PENDING, sampleQty: 1, requestDate: new Date().toISOString().slice(0, 10) }
  open.value = true
}

async function save() {
  const f = form.value
  if (!f.supplierId && !f.supplierName?.trim()) {
    message.warning(t('ecomDev.sample.supplierRequired'))
    return
  }
  const payload = {
    supplierId: f.supplierId ?? null,
    supplierName: f.supplierName || null,
    sampleUrl: f.sampleUrl || null,
    sampleQty: f.sampleQty ?? null,
    sampleFee: f.sampleFee ?? null,
    quotePrice: f.quotePrice ?? null,
    requestDate: f.requestDate || null,
    arrivalDate: f.arrivalDate || null,
    result: f.result,
    evaluation: f.evaluation || null
  }
  saving.value = true
  try {
    if (f.id) await updateDevSample(f.id, payload)
    else await addDevSample(projectId, payload)
    message.success(t('common.msg.saveSuccess'))
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
</style>
