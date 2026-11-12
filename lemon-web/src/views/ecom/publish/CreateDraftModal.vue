<template>
  <a-modal v-model:open="visible" :title="$t('ecomPublish.create.title')" :confirm-loading="saving" width="min(720px, 100vw)" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-alert type="info" show-icon class="tip" :message="$t('ecomPublish.create.tip')" />
      <a-form-item :label="$t('ecomPublish.create.material')" name="materialId" :rules="[{ required: true, message: $t('ecomPublish.create.materialRequired') }]"
                   :extra="$t('ecomPublish.create.materialExtra')">
        <MaterialSelect v-model:value="form.materialId" @select="onMaterial" />
      </a-form-item>
      <a-form-item :label="$t('ecomPublish.item.title')" name="title">
        <a-input v-model:value="form.title" :maxlength="255" show-count :placeholder="$t('ecomPublish.create.titlePh')" />
      </a-form-item>
      <a-form-item :label="$t('ecomPublish.create.stores')" name="storeIds" :rules="[{ required: true, type: 'array', min: 1, message: $t('ecomPublish.create.storesRequired') }]">
        <a-select v-model:value="form.storeIds" mode="multiple" show-search :filter-option="filterStore" :options="storeOptions"
                  :placeholder="$t('ecomPublish.create.storesPh')" />
      </a-form-item>
      <a-form-item v-for="p in platforms" :key="p" :label="$t('ecomPublish.create.templateFor', { platform: platformLabel(p) })">
        <a-select v-model:value="form.templateIds[p]" allow-clear :options="templateOptions(p)" :placeholder="$t('ecomPublish.create.templatePh')" />
      </a-form-item>
      <a-form-item :label="$t('ecomPublish.item.priceTemplate')" name="priceTemplateId" :extra="$t('ecomPublish.create.priceTemplateExtra')">
        <a-select v-model:value="form.priceTemplateId" allow-clear :options="priceOptions" :placeholder="$t('ecomPublish.create.priceTemplatePh')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { createPublishDraft, priceTemplateOptions, publishTemplateOptions } from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

/**
 * 新建刊登：选择物料（多规格款式即 SPU，变体为其下的 SKU）和要刊登的店铺，每个平台可选一个刊登模板（带入类目、属性、运费模板等），
 * 价格模板用于计算建议价。保存后打开草稿（主内容与各店铺记录），success(draftId)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform')
const stores = useStores()

const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({ materialId: undefined, title: '', storeIds: [], templateIds: {}, priceTemplateId: undefined })
const templates = ref([])
const priceTemplates = ref([])

const platformLabel = (p) => findDict(dict.ecom_platform, p)?.label || p
const storeOptions = computed(() => stores.value.map((s) => ({ value: s.id, label: `${s.name}（${platformLabel(s.platform)} ${s.marketplace || ''}）`, code: s.code })))
const filterStore = (input, option) => option.label.toLowerCase().includes(input.toLowerCase()) || (option.code || '').toLowerCase().includes(input.toLowerCase())
/** 所选店铺涉及的平台 */
const platforms = computed(() => [...new Set(form.value.storeIds.map((id) => stores.value.find((s) => s.id === id)?.platform).filter(Boolean))])
const templateOptions = (p) => templates.value.filter((x) => x.platform === p).map((x) => ({ value: x.id, label: x.name }))
const priceOptions = computed(() => priceTemplates.value.map((x) => ({ value: x.id, label: x.name })))

async function open(defaults = {}) {
  form.value = { materialId: undefined, title: '', storeIds: [], templateIds: {}, priceTemplateId: undefined, ...defaults }
  visible.value = true
  ;[templates.value, priceTemplates.value] = await Promise.all([publishTemplateOptions(), priceTemplateOptions()])
}

function onMaterial(m) {
  if (!form.value.title && m?.name) form.value.title = m.name
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const templateIds = Object.fromEntries(Object.entries(form.value.templateIds).filter(([p, v]) => v && platforms.value.includes(p)))
    const id = await createPublishDraft({ ...form.value, templateIds, title: form.value.title || null })
    message.success(t('ecomPublish.create.created'))
    visible.value = false
    emit('success', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
</style>
