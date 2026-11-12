<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomPublish.template.add')" :edit-title="$t('ecomPublish.template.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomPublish.item.secBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.template.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.common.platform')" name="platform">
            <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" @change="onPlatform" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.item.categoryAttrs') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('ecomPublish.template.lookupStore')" :extra="$t('ecomPublish.template.lookupStoreExtra')">
            <StoreSelect v-model:value="lookupStoreId" :platform="form.platform" :disabled="!form.platform" @update:value="loadCaps" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="16">
          <a-form-item :label="$t('ecomPublish.item.category')">
            <CategoryPicker v-model:id="form.categoryId" v-model:name="form.categoryName" :store-id="lookupStoreId" :remote="hasCap('CATEGORIES')" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.attributes')">
            <AttributesEditor v-model:value="form.attributes" :store-id="lookupStoreId" :category-id="form.categoryId" :remote="hasCap('ATTRIBUTES')" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.template.defaults') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.item.brand')" name="brand"><a-input v-model:value="form.brand" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.item.shippingTemplate')" name="shippingTemplate"><a-input v-model:value="form.shippingTemplate" :maxlength="128" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.item.returnPolicy')" name="returnPolicy"><a-input v-model:value="form.returnPolicy" :maxlength="128" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.item.handlingDays')" name="handlingDays">
            <a-input-number v-model:value="form.handlingDays" :min="0" :max="60" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.item.fulfillment')" name="fulfillment">
            <a-select v-model:value="form.fulfillment" allow-clear :options="toOptions(dict.ecom_fulfillment)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomPublish.item.priceTemplate')" name="priceTemplateId">
            <a-select v-model:value="form.priceTemplateId" allow-clear :options="priceOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.item.extra')" :extra="$t('ecomPublish.item.extraTip')">
            <KeyValueEditor v-model:value="form.extra" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect, { useStores } from '@/components/ecom/StoreSelect.vue'
import { addPublishTemplate, getPublishTemplate, priceTemplateOptions, publishCapabilities, updatePublishTemplate } from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import AttributesEditor from '../publish/AttributesEditor.vue'
import CategoryPicker from '../publish/CategoryPicker.vue'
import KeyValueEditor from '../publish/KeyValueEditor.vue'

/**
 * 刊登模板新增 / 编辑：选择一个该平台的店铺后，可经平台接口选择类目、加载类目属性（店铺只用于查询，不保存）；
 * 不能查询的平台手工填写类目与属性键值
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_fulfillment')
const stores = useStores()
const lookupStoreId = ref()
const caps = ref(null)
const priceTemplates = ref([])
const hasCap = (c) => !!lookupStoreId.value && !!caps.value?.capabilities?.includes(c)
const priceOptions = computed(() => priceTemplates.value.map((x) => ({ value: x.id, label: x.name })))

const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomPublish.template.name') }) }],
  platform: [{ required: true, message: t('common.rule.required', { name: t('ecomPublish.common.platform') }) }]
}
const FIELDS = ['name', 'platform', 'categoryId', 'categoryName', 'attributes', 'brand', 'shippingTemplate', 'returnPolicy', 'handlingDays', 'fulfillment',
  'priceTemplateId', 'extra', 'status', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', platform: undefined, attributes: {}, extra: {}, status: STATUS_ENABLED }),
  beforeOpen: async () => {
    priceTemplates.value = (await priceTemplateOptions()) || []
  },
  fetch: async (r) => {
    const tpl = await getPublishTemplate(r.id)
    pickStore(tpl.platform)
    return tpl
  },
  create: addPublishTemplate,
  update: updatePublishTemplate,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

/** 默认选该平台的第一个店铺用于查询类目 */
function pickStore(platform) {
  const s = stores.value.find((x) => x.platform === platform)
  lookupStoreId.value = s?.id
  loadCaps(s?.id)
}
function onPlatform(p) {
  form.value.categoryId = undefined
  form.value.categoryName = undefined
  pickStore(p)
}
async function loadCaps(id) {
  caps.value = id ? await publishCapabilities(id) : null
}

defineExpose({
  openCreate: (d) => {
    lookupStoreId.value = undefined
    caps.value = null
    return drawer.openCreate(d)
  },
  openEdit: drawer.openEdit
})
</script>

<style scoped>
.full {
  width: 100%;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
</style>
