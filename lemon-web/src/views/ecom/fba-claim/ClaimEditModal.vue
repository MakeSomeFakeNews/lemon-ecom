<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.claim.editTitle', { no: record.claimNo || '' })" :confirm-loading="saving" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item :label="$t('common.field.status')" name="status" :rules="[{ required: true, message: $t('ecomFba.claim.statusRequired') }]">
            <a-select v-model:value="form.status" :options="toOptions(dict.ecom_fba_claim_status)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.caseNo')" name="caseNo" :rules="caseRules">
            <a-input v-model:value="form.caseNo" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.submitDate')"><a-date-picker v-model:value="form.submitDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.deadline')"><a-date-picker v-model:value="form.deadline" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.qty')"><a-input-number v-model:value="form.qty" :min="0" :precision="0" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.unitValue')">
            <a-input-number v-model:value="form.unitValue" :min="0" :precision="2" class="full" :addon-after="record.currency" />
          </a-form-item>
        </a-col>
        <template v-if="form.status === FbaClaimStatus.PAID">
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.claim.paidAmount')" name="paidAmount" :rules="[{ required: true, message: $t('ecomFba.claim.paidRequired') }]">
              <a-input-number v-model:value="form.paidAmount" :min="0" :precision="2" class="full" :addon-after="record.currency" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.claim.paidDate')"><a-date-picker v-model:value="form.paidDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
          </a-col>
        </template>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')"><a-textarea v-model:value="form.remark" :maxlength="255" :rows="2" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { updateClaim } from '@/api/ecom/fba'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { FbaClaimStatus } from '@/constants/enums'
import { t } from '@/locales'

/** 处理索赔：状态、Case、提交日期、估算、截止日期、手工登记赔付；ref 调用 open(record)；@success */
const emit = defineEmits(['success'])
const dict = useDict('ecom_fba_claim_status')
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const record = ref({})
const form = ref({})
const caseRules = computed(() => (form.value.status === FbaClaimStatus.SUBMITTED ? [{ required: true, whitespace: true, message: t('ecomFba.claim.caseRequired') }] : []))

function open(r) {
  record.value = r
  form.value = {
    status: r.status === FbaClaimStatus.PENDING ? FbaClaimStatus.SUBMITTED : r.status,
    caseNo: r.caseNo || '',
    submitDate: r.submitDate || undefined,
    deadline: r.deadline || undefined,
    qty: r.qty != null ? Number(r.qty) : undefined,
    unitValue: r.unitValue != null ? Number(r.unitValue) : undefined,
    paidAmount: Number(r.paidAmount) || undefined,
    paidDate: r.paidDate || undefined,
    remark: r.remark || ''
  }
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    await updateClaim(record.value.id, {
      status: f.status,
      caseNo: f.caseNo || null,
      submitDate: f.submitDate || null,
      qty: f.qty ?? null,
      unitValue: f.unitValue ?? null,
      deadline: f.deadline || null,
      paidAmount: f.status === FbaClaimStatus.PAID ? f.paidAmount ?? null : null,
      paidDate: f.status === FbaClaimStatus.PAID ? f.paidDate || null : null,
      remark: f.remark || null
    })
    message.success(t('common.msg.saveSuccess'))
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
