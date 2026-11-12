<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomCs.template.add')" :edit-title="$t('ecomCs.template.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomCs.template.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomCs.template.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('ecomCs.template.language')" name="language">
            <a-select v-model:value="form.language" :options="optionsWithCurrent(dict.ecom_cs_language, form.language)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('ecomCs.template.platform')" name="platform" :extra="$t('ecomCs.template.platformExtra')">
            <a-select v-model:value="form.platform" allow-clear :options="toOptions(dict.ecom_platform)" :placeholder="$t('ecomCs.template.anyPlatform')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="5">
          <a-form-item :label="$t('ecomCs.template.scene')" name="scene">
            <a-auto-complete v-model:value="form.scene" :maxlength="64" :options="sceneOptions" :placeholder="$t('ecomCs.template.scenePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="12" :lg="3">
          <a-form-item :label="$t('ecomCs.template.sort')" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" :max="9999" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="12" :lg="2">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomCs.template.sectionContent') }}</div>
      <a-form-item name="content">
        <a-textarea ref="textRef" v-model:value="form.content" :maxlength="2000" show-count :auto-size="{ minRows: 8, maxRows: 18 }" />
      </a-form-item>
      <a-row :gutter="[16, 16]" class="panels">
        <a-col :xs="24" :lg="9">
          <section class="panel">
            <div class="panel-title">{{ $t('ecomCs.template.variables') }}<span class="sub-text">{{ $t('ecomCs.template.variablesTip') }}</span></div>
            <div class="vars">
              <div v-for="v in VARIABLES" :key="v" class="var" role="button" tabindex="0" @mousedown.prevent @click="insert(v)" @keydown.enter="insert(v)">
                <code>{{ token(v) }}</code>
                <span class="var-label">{{ $t(`ecomCs.template.var.${v}`) }}</span>
              </div>
            </div>
          </section>
        </a-col>
        <a-col :xs="24" :lg="15">
          <section class="panel">
            <div class="panel-title">{{ $t('ecomCs.template.preview') }}<span class="sub-text">{{ $t('ecomCs.template.previewTip') }}</span></div>
            <div v-if="form.content" class="preview"><template v-for="(p, i) in previewParts" :key="i"><mark v-if="p.var" class="var-value">{{ p.text }}</mark><template v-else>{{ p.text }}</template></template></div>
            <div v-else class="sub-text">{{ $t('ecomCs.template.previewEmpty') }}</div>
          </section>
        </a-col>
      </a-row>

      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
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
 * 回复模板新增 / 编辑（子页）：名称 + 语言唯一，可限定平台；点击变量插入到内容的光标处，旁边按示例数据预览替换结果。openCreate() / openEdit(record)，保存后触发 success
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

/** 预览：已知变量替换为示例值（高亮），其余原样显示 */
const previewParts = computed(() =>
  (form.value.content || '').split(/(\{\w+\})/).filter(Boolean).map((text) => {
    const name = text.slice(1, -1)
    return text.startsWith('{') && VARIABLES.includes(name) ? { text: t(`ecomCs.template.sample.${name}`), var: true } : { text }
  })
)

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
.section-title {
  margin: 4px 0 12px;
  font-weight: 600;
}
.panels {
  margin-bottom: 16px;
}
.panel {
  height: 100%;
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.panel-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
  font-weight: 500;
}
.vars {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 6px;
}
.var {
  display: flex;
  flex-direction: column;
  padding: 6px 10px;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.var:hover,
.var:focus-visible {
  border-color: var(--app-primary, #1677ff);
  background: var(--app-hover);
  outline: none;
}
.var code {
  color: var(--app-primary, #1677ff);
  font-size: 12px;
}
.var-label {
  color: var(--app-text-2);
  font-size: 12px;
}
.preview {
  min-height: 120px;
  max-height: 360px;
  overflow: auto;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--app-hover);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.var-value {
  padding: 0 2px;
  border-radius: 3px;
  background: rgba(22, 119, 255, 0.12);
  color: inherit;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.full {
  width: 100%;
}
</style>
