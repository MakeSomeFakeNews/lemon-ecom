<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.claim.addTitle')" :confirm-loading="saving" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.common.store')" name="storeId" :rules="[{ required: true, message: $t('ecomFba.report.selectStore') }]">
            <StoreSelect v-model:value="form.storeId" :platform="EcomPlatform.AMAZON" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.type')" name="claimType" :rules="[{ required: true, message: $t('ecomFba.claim.typeRequired') }]">
            <a-select v-model:value="form.claimType" :options="toOptions(dict.ecom_fba_claim_type)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.common.sku')"><a-input v-model:value="form.sellerSku" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.sourceRef')"><a-input v-model:value="form.sourceRef" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.qty')" name="qty" :rules="[{ required: true, message: $t('ecomFba.claim.qtyRequired') }]">
            <a-input-number v-model:value="form.qty" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.unitValue')"><a-input-number v-model:value="form.unitValue" :min="0" :precision="2" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.eventDate')"><a-date-picker v-model:value="form.eventDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.claim.deadline')"><a-date-picker v-model:value="form.deadline" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { addClaim } from '@/api/ecom/fba'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { EcomPlatform, FbaClaimType } from '@/constants/enums'
import { t } from '@/locales'

/** 手工登记索赔：ref 调用 open(storeId?)；@success */
const emit = defineEmits(['success'])
const dict = useDict('ecom_fba_claim_type')
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})

function open(storeId) {
  form.value = { storeId: storeId || undefined, claimType: FbaClaimType.OTHER, sellerSku: '', sourceRef: '', qty: 1, unitValue: undefined, eventDate: undefined, deadline: undefined, remark: '' }
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    await addClaim({ ...f, sellerSku: f.sellerSku || null, sourceRef: f.sourceRef || null, remark: f.remark || null })
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
