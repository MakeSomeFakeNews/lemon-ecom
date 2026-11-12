<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomCs.template.add')" :edit-title="$t('ecomCs.template.edit')" width="min(720px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomCs.template.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomCs.template.language')" name="language">
            <a-select v-model:value="form.language" :options="optionsWithCurrent(dict.ecom_cs_language, form.language)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomCs.template.platform')" name="platform" :extra="$t('ecomCs.template.platformExtra')">
            <a-select v-model:value="form.platform" allow-clear :options="toOptions(dict.ecom_platform)" :placeholder="$t('ecomCs.template.anyPlatform')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomCs.template.scene')" name="scene">
            <a-auto-complete v-model:value="form.scene" :maxlength="64" :options="sceneOptions" :placeholder="$t('ecomCs.template.scenePh')" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomCs.template.content')" name="content">
            <div class="vars">
              <span class="sub-text">{{ $t('ecomCs.template.insertVar') }}</span>
              <a-tag v-for="v in VARIABLES" :key="v" class="var" color="blue" :bordered="false" @click="insert(v)">{{ token(v) }} {{ $t(`ecomCs.template.var.${v}`) }}</a-tag>
            </div>
            <a-textarea ref="textRef" v-model:value="form.content" :maxlength="2000" show-count :auto-size="{ minRows: 6, maxRows: 14 }" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8">
          <a-form-item :label="$t('ecomCs.template.sort')" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" :max="9999" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="8">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addCsTemplate, getCsTemplate, updateCsTemplate } from '@/api/ecom/cs'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { VARIABLES } from './template'

/**
 * 回复模板新增 / 编辑（抽屉）：名称 + 语言唯一，可限定平台；内容中点击变量插入到光标处。openCreate() / openEdit(record)，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_cs_language', 'ecom_platform')
const textRef = ref()

const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomCs.template.name') }) }],
  language: [{ required: true, message: t('common.rule.required', { name: t('ecomCs.template.language') }) }],
  content: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomCs.template.content') }) }]
}
const sceneOptions = computed(() => ['logistics', 'review', 'negative', 'refund', 'thanks'].map((k) => ({ value: t(`ecomCs.template.sceneOption.${k}`) })))

const FIELDS = ['name', 'language', 'platform', 'scene', 'content', 'sort', 'status', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', language: 'en', platform: undefined, scene: '', content: '', sort: 0, status: STATUS_ENABLED, remark: '' }),
  fetch: (record) => getCsTemplate(record.id),
  create: addCsTemplate,
  update: updateCsTemplate,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

/** 变量的写法 {name} */
const token = (name) => '{' + name + '}'

/** 在光标处插入变量 */
async function insert(name) {
  const el = textRef.value?.resizableTextArea?.textArea || textRef.value?.$el?.querySelector?.('textarea')
  const text = form.value.content || ''
  const pos = el && typeof el.selectionStart === 'number' ? el.selectionStart : text.length
  form.value.content = text.slice(0, pos) + token(name) + text.slice(pos)
  await nextTick()
  if (el) {
    el.focus()
    el.setSelectionRange(pos + token(name).length, pos + token(name).length)
  }
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.vars {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}
.var {
  cursor: pointer;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
