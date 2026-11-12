<template>
  <a-modal v-model:open="open" :title="$t('ecomDev.material.title')" :confirm-loading="saving" @ok="ok">
    <a-alert type="info" show-icon :message="$t('ecomDev.material.tip')" class="tip" />
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomDev.material.code')" :extra="$t('ecomDev.material.codeExtra')">
        <a-input v-model:value="form.code" :maxlength="64" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomDev.material.unit')">
            <a-input v-model:value="form.unit" :maxlength="16" :placeholder="$t('ecomDev.material.unitPh')" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomDev.material.spec')">
            <a-input v-model:value="form.spec" :maxlength="128" />
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
import { generateDevMaterial } from '@/api/ecom/dev'

/** 生成物料：编码（留空自动）、单位、规格。ref 调用 open(projectId)，成功后触发 success(物料 ID) */
const emit = defineEmits(['success'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const form = ref({})
let projectId

function show(id) {
  projectId = id
  form.value = { code: '', unit: '', spec: '' }
  open.value = true
}

async function ok() {
  saving.value = true
  try {
    const materialId = await generateDevMaterial(projectId, {
      code: form.value.code?.trim() || null,
      unit: form.value.unit?.trim() || null,
      spec: form.value.spec?.trim() || null
    })
    message.success(t('ecomDev.material.generated'))
    open.value = false
    emit('success', materialId)
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
</style>
