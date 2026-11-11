<template>
  <a-modal :open="open" :title="$t('ecom.listing.mapTitle')" :confirm-loading="saving" :ok-text="$t('common.action.save')" @ok="submit" @cancel="open = false">
    <a-descriptions v-if="target" :column="1" size="small" class="block">
      <a-descriptions-item :label="$t('ecom.common.store')">{{ target.storeName || '-' }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecom.common.sellerSku')">{{ target.sellerSku }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecom.listing.title')">{{ target.title || '-' }}</a-descriptions-item>
    </a-descriptions>
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecom.common.material')" name="materialId" :extra="$t('ecom.listing.mapExtra')">
        <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
      </a-form-item>
      <a-form-item :label="$t('ecom.common.packQty')" name="packQty" :extra="$t('ecom.listing.packQtyExtra')">
        <a-input-number v-model:value="form.packQty" :min="0.0001" :precision="4" class="full" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { mapListing } from '@/api/ecom/listing'
import { materialLabel } from './listing'

/** 映射物料弹窗：只修改在线商品的物料与多件装数量（PUT /ecom/listing/{id}/map），open(record) 打开，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const target = ref()
const formRef = ref()
const form = reactive({ materialId: undefined, materialLabel: undefined, packQty: 1 })
const rules = {
  materialId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecom.common.material') }) }],
  packQty: [{ required: true, message: t('common.rule.required', { name: t('ecom.common.packQty') }) }]
}

function show(record) {
  target.value = record
  Object.assign(form, { materialId: record.materialId ?? undefined, materialLabel: materialLabel(record), packQty: Number(record.packQty) || 1 })
  open.value = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await mapListing(target.value.id, form.materialId, form.packQty)
    message.success(t('ecom.listing.mapped'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
</style>
