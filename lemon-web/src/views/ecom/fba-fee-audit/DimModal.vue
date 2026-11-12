<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.dim.editTitle', { sku: form.sellerSku || '' })" :confirm-loading="saving" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.dim.length')" name="lengthCm" :rules="req"><a-input-number v-model:value="form.lengthCm" :min="0.01" :precision="2" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.dim.width')" name="widthCm" :rules="req"><a-input-number v-model:value="form.widthCm" :min="0.01" :precision="2" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.dim.height')" name="heightCm" :rules="req"><a-input-number v-model:value="form.heightCm" :min="0.01" :precision="2" class="full" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomFba.dim.weight')" name="weightKg" :rules="req"><a-input-number v-model:value="form.weightKg" :min="0.001" :precision="3" class="full" /></a-form-item>
        </a-col>
      </a-row>
      <div v-if="quote !== undefined" class="quote">
        <template v-if="quote">{{ $t('ecomFba.tier.quoteResult', { tier: quote.tierName, weight: quote.shippingWeight, unit: quote.weightUnit, fee: quote.fee, currency: quote.currency }) }}</template>
        <template v-else>{{ $t('ecomFba.tier.quoteNone') }}</template>
      </div>
    </a-form>
    <template #footer>
      <a-button @click="calc">{{ $t('ecomFba.tier.quoteBtn') }}</a-button>
      <a-button @click="visible = false">{{ $t('common.action.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" @click="submit">{{ $t('common.action.save') }}</a-button>
    </template>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { quoteFee, saveDim } from '@/api/ecom/fba'
import { t } from '@/locales'

/** 维护商品尺寸重量（cm / kg），可先试算配送费；ref 调用 open(row)；@success */
const emit = defineEmits(['success'])
const req = [{ required: true, message: t('ecomFba.dim.required') }]
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const quote = ref(undefined)

function open(row) {
  form.value = {
    storeId: row.storeId,
    sellerSku: row.sellerSku,
    marketplace: row.marketplace,
    lengthCm: row.lengthCm != null ? Number(row.lengthCm) : undefined,
    widthCm: row.widthCm != null ? Number(row.widthCm) : undefined,
    heightCm: row.heightCm != null ? Number(row.heightCm) : undefined,
    weightKg: row.weightKg != null ? Number(row.weightKg) : undefined
  }
  quote.value = undefined
  visible.value = true
}

async function calc() {
  await formRef.value.validate()
  const f = form.value
  quote.value = (await quoteFee({ marketplace: f.marketplace, length: f.lengthCm, width: f.widthCm, height: f.heightCm, weight: f.weightKg })) || null
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { marketplace: _m, ...data } = form.value
    await saveDim(data)
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
.quote {
  color: var(--app-text-2);
}
</style>
