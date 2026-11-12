<template>
  <a-modal :open="open" :title="$t('ecomCs.message.edit')" :confirm-loading="saving" @ok="submit" @cancel="open = false">
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomCs.message.owner')">
        <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
      </a-form-item>
      <a-form-item :label="$t('ecomCs.message.tags')" :extra="$t('ecomCs.message.tagsExtra')">
        <a-select v-model:value="form.tags" mode="tags" :token-separators="[',', '，']" :options="presetTags" />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { updateCsThread } from '@/api/ecom/cs'

/**
 * 会话的负责人、标签（自由输入，常用标签可选）、备注。open(thread) 打开，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const form = reactive({ id: null, ownerId: undefined, ownerName: undefined, tags: [], remark: '' })
const presetTags = computed(() => ['logistics', 'refund', 'quality', 'urgent'].map((k) => ({ value: t(`ecomCs.message.tag.${k}`) })))

function show(thread) {
  Object.assign(form, {
    id: thread.id,
    ownerId: thread.ownerId || undefined,
    ownerName: thread.ownerName,
    tags: thread.tags ? thread.tags.split(',').filter(Boolean) : [],
    remark: thread.remark || ''
  })
  open.value = true
}

async function submit() {
  const tags = form.tags.join(',')
  if (tags.length > 255) return message.warning(t('ecomCs.message.tagsTooLong'))
  saving.value = true
  try {
    await updateCsThread(form.id, { ownerId: form.ownerId ?? null, tags: tags || null, remark: form.remark || null })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
