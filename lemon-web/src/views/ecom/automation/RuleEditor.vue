<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomAuto.editor.createTitle')" :edit-title="$t('ecomAuto.editor.editTitle')">
    <a-row :gutter="20" class="editor">
      <a-col :xs="24" :xl="17">
        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
          <!-- ① 触发器 -->
          <section class="step">
            <div class="step-head">
              <span class="step-no">1</span>
              <span class="step-title">{{ $t('ecomAuto.editor.step1') }}</span>
              <span class="step-desc">{{ $t('ecomAuto.editor.step1Desc') }}</span>
            </div>
            <a-form-item name="trigger" class="no-label">
              <a-form-item-rest>
              <a-segmented v-model:value="category" :options="categoryOptions" class="cats" />
              <div class="trigger-grid">
                <div
                  v-for="tr in categoryTriggers"
                  :key="tr.code"
                  class="trigger-card"
                  :class="{ active: form.trigger === tr.code }"
                  @click="pickTrigger(tr.code)"
                >
                  <div class="trigger-name">
                    <CheckCircleFilled v-if="form.trigger === tr.code" class="check" />
                    {{ tr.name }}
                    <a-tag :color="tr.mode === AutoScanMode.REALTIME ? 'green' : 'blue'" :bordered="false" class="mode">{{ $t(`ecomAuto.mode.${tr.mode}`) }}</a-tag>
                  </div>
                  <div class="trigger-desc">{{ tr.description }}</div>
                </div>
              </div>
              </a-form-item-rest>
            </a-form-item>
          </section>

          <!-- ② 范围与条件 -->
          <section class="step">
            <div class="step-head">
              <span class="step-no">2</span>
              <span class="step-title">{{ $t('ecomAuto.editor.step2') }}</span>
              <span class="step-desc">{{ $t('ecomAuto.editor.step2Desc') }}</span>
            </div>
            <div class="sub-title">{{ $t('ecomAuto.editor.scope') }} <span class="sub-tip">{{ $t('ecomAuto.editor.scopeTip') }}</span></div>
            <a-row :gutter="12">
              <a-col :xs="24" :lg="12">
                <a-form-item :label="$t('ecomAuto.editor.stores')">
                  <a-select v-model:value="form.scope.storeIds" mode="multiple" allow-clear :options="storeOptions" option-filter-prop="label" :max-tag-count="3"
                    :placeholder="$t('ecomAuto.common.any')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :lg="12">
                <a-form-item :label="$t('ecomAuto.editor.platforms')">
                  <a-select v-model:value="form.scope.platforms" mode="multiple" allow-clear :options="toOptions(dict.ecom_platform)" :max-tag-count="3"
                    :placeholder="$t('ecomAuto.common.any')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :lg="8">
                <a-form-item :label="$t('ecomAuto.editor.sites')">
                  <a-select v-model:value="form.scope.sites" mode="tags" :open="false" :token-separators="[',', ' ']" :placeholder="$t('ecomAuto.editor.sitesPh')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :lg="8">
                <a-form-item :label="$t('ecomAuto.editor.skus')">
                  <a-select v-model:value="form.scope.skus" mode="tags" :open="false" :token-separators="[',', ' ']" :placeholder="$t('ecomAuto.editor.skusPh')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :lg="8">
                <a-form-item :label="$t('ecomAuto.editor.tags')">
                  <a-select v-model:value="form.scope.tags" mode="tags" :options="tagNameOptions" :token-separators="[',']" :placeholder="$t('ecomAuto.editor.tagsPh')" />
                </a-form-item>
              </a-col>
            </a-row>
            <div class="sub-title">{{ $t('ecomAuto.editor.condition') }}</div>
            <ConditionGroup v-if="currentTrigger" :group="form.condition" :fields="currentTrigger.fields" :operators="catalog?.operators || {}" />
            <div v-else class="empty-tip">{{ $t('ecomAuto.editor.pickTrigger') }}</div>
          </section>

          <!-- ③ 动作 -->
          <section class="step">
            <div class="step-head">
              <span class="step-no">3</span>
              <span class="step-title">{{ $t('ecomAuto.editor.step3') }}</span>
              <span class="step-desc">{{ $t('ecomAuto.editor.step3Desc') }}</span>
            </div>
            <a-form-item name="actions" class="no-label">
              <a-form-item-rest>
              <div class="actions">
                <ActionEditor
                  v-for="(a, i) in form.actions"
                  :key="a._key"
                  :action="a"
                  :index="i"
                  :total="form.actions.length"
                  :trigger="currentTrigger"
                  :catalog="catalog"
                  :roles="roles"
                  :tag-options="tagOptions"
                  :user-names="userNames"
                  @up="move(i, -1)"
                  @down="move(i, 1)"
                  @remove="form.actions.splice(i, 1)"
                  @focus="(f) => (lastFocus = f)"
                />
                <a-dropdown :disabled="!currentTrigger || form.actions.length >= 10">
                  <a-button type="dashed" block :disabled="!currentTrigger || form.actions.length >= 10">
                    <template #icon><PlusOutlined /></template>{{ $t('ecomAuto.editor.addAction') }}
                  </a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => addAction(key)">
                      <a-menu-item v-for="a in availableActions" :key="a.type">
                        <div class="menu-name">{{ a.name }}</div>
                        <div class="menu-desc">{{ a.description }}</div>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
              </a-form-item-rest>
            </a-form-item>
          </section>

          <!-- 设置 -->
          <section class="step">
            <div class="step-head">
              <SettingOutlined class="step-icon" />
              <span class="step-title">{{ $t('ecomAuto.editor.settings') }}</span>
            </div>
            <a-row :gutter="12">
              <a-col :xs="24" :lg="12">
                <a-form-item :label="$t('ecomAuto.editor.name')" name="name">
                  <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('ecomAuto.editor.namePh')" />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :lg="6">
                <a-form-item :label="$t('ecomAuto.editor.debounce')" :tooltip="$t('ecomAuto.editor.debounceTip')">
                  <a-input-number v-model:value="form.debounceHours" :min="0" :max="720" :precision="0" :addon-after="$t('ecomAuto.common.hours')" class="full" />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :lg="6">
                <a-form-item :label="$t('ecomAuto.editor.dailyLimit')" :tooltip="$t('ecomAuto.editor.dailyLimitTip')">
                  <a-input-number v-model:value="form.dailyLimit" :min="0" :max="10000" :precision="0" :addon-after="$t('ecomAuto.common.times')" class="full" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :lg="18">
                <a-form-item :label="$t('ecomAuto.editor.remark')">
                  <a-input v-model:value="form.remark" :maxlength="255" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :lg="6">
                <a-form-item label=" " :colon="false">
                  <a-switch :checked="form.status === STATUS_ENABLED" @change="(v) => (form.status = v ? STATUS_ENABLED : STATUS_DISABLED)" />
                  <span class="switch-label">{{ $t('ecomAuto.editor.enabled') }}</span>
                </a-form-item>
              </a-col>
            </a-row>
          </section>
        </a-form>
      </a-col>

      <!-- 右侧：预览、变量、试运行 -->
      <a-col :xs="24" :xl="7">
        <div class="side">
          <section class="side-card">
            <div class="side-title"><EyeOutlined /> {{ $t('ecomAuto.editor.preview') }}</div>
            <div class="sentence">{{ sentence }}</div>
            <a-button v-perm="'ecom:automation:test'" type="primary" ghost block :loading="testing" @click="runTest">
              <template #icon><ExperimentOutlined /></template>{{ $t('ecomAuto.editor.testRun') }}
            </a-button>
            <div class="side-tip">{{ $t('ecomAuto.editor.testTip') }}</div>
          </section>
          <section v-if="currentTrigger" class="side-card">
            <div class="side-title"><CodeOutlined /> {{ $t('ecomAuto.editor.variables') }}</div>
            <div class="side-tip">{{ $t('ecomAuto.editor.variablesTip') }}</div>
            <div class="vars">
              <a-tooltip v-for="v in variables" :key="v.key" :title="varToken(v.key)">
                <a-tag class="var" @click="insertVar(v.key)">{{ v.label }}</a-tag>
              </a-tooltip>
            </div>
          </section>
        </div>
      </a-col>
    </a-row>
    <TestRunPanel ref="testRef" />
  </FormDrawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleFilled, CodeOutlined, ExperimentOutlined, EyeOutlined, PlusOutlined, SettingOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { addAutoRule, automationRoles, getAutoRule, testAutoRule, updateAutoRule } from '@/api/ecom/automation'
import { orderTagOptions } from '@/api/ecom/orderEnhance'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { AutoCategory, AutoScanMode } from '@/constants/enums'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'
import ActionEditor from './ActionEditor.vue'
import ConditionGroup from './ConditionGroup.vue'
import TestRunPanel from './TestRunPanel.vue'
import { applyActionDefaults, emptyGroup, emptyScope, ruleSentence, triggerOf, useCatalog, varToken } from './autoText'

/**
 * 自动化规则编辑（子页，三段式）：① 触发器（按分类选择，显示产生方式）② 对象范围（店铺、平台、站点、SKU、标签）与条件（且 / 或、条件组）
 * ③ 动作（按顺序、可调整顺序，按触发器过滤可用动作）；设置（名称、防抖、每日上限、启用）。
 * 右侧实时预览「当 … 且 … 时，执行 …」，插入变量到最近编辑的标题 / 内容，试运行（未保存的编辑内容也可以）
 */
const emit = defineEmits(['success'])
const catalog = useCatalog()
const dict = useDict('ecom_platform', 'ecom_auto_category')
const stores = useStores()
const storeOptions = computed(() => stores.value.map((s) => ({ value: s.id, label: s.name })))
const storeName = (id) => stores.value.find((s) => s.id === id)?.name || `#${id}`
const roles = ref([])
const tagOptions = ref([])
const tagNameOptions = computed(() => tagOptions.value.map((o) => ({ value: o.label, label: o.label })))
const userNames = ref({})
const lastFocus = ref(null)
const testRef = ref()
const testing = ref(false)
let keySeq = 0

const emptyForm = () => ({
  id: undefined,
  name: '',
  trigger: undefined,
  status: STATUS_ENABLED,
  scope: emptyScope(),
  condition: emptyGroup(),
  actions: [],
  debounceHours: 24,
  dailyLimit: 0,
  remark: '',
  templateCode: undefined
})

/** 后端的规则 → 表单（补齐缺省的范围 / 条件、给动作加本地 key） */
function toForm(r) {
  const f = { ...emptyForm(), ...r }
  f.scope = { ...emptyScope(), ...(r.scope || {}) }
  f.condition = r.condition ? { logic: r.condition.logic, items: [...(r.condition.items || [])], groups: (r.condition.groups || []).map((g) => ({ ...g, items: [...(g.items || [])] })) } : emptyGroup()
  f.actions = (r.actions || []).map((a) => ({ ...a, _key: ++keySeq }))
  return f
}

const drawer = useFormDrawer({
  emptyForm,
  beforeOpen: async () => {
    const [r, tags] = await Promise.all([roles.value.length ? roles.value : automationRoles(), orderTagOptions()])
    roles.value = r || []
    tagOptions.value = (tags || []).map((x) => ({ value: x.id, label: x.name }))
  },
  fetch: async (record) => {
    const r = await getAutoRule(record.id)
    userNames.value = r.userNames || {}
    return toForm(r)
  },
  create: addAutoRule,
  update: updateAutoRule,
  toPayload: payload,
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

function payload(f = form.value) {
  return {
    name: f.name,
    trigger: f.trigger,
    status: f.status,
    scope: f.scope,
    condition: f.condition,
    actions: f.actions.map(({ _key, ...a }) => a),
    debounceHours: f.debounceHours ?? 0,
    dailyLimit: f.dailyLimit ?? 0,
    remark: f.remark,
    templateCode: f.templateCode
  }
}

const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomAuto.editor.name') }) }],
  trigger: [{ required: true, message: t('ecomAuto.editor.pickTrigger') }],
  actions: [{ validator: () => (form.value.actions.length ? Promise.resolve() : Promise.reject(t('ecomAuto.editor.noAction'))) }]
}

const category = ref(AutoCategory.ORDER)
const categoryOptions = computed(() =>
  Object.values(AutoCategory).map((c) => ({ value: c, label: findDict(dict.ecom_auto_category, c)?.label || c }))
)
const categoryTriggers = computed(() => (catalog.value?.triggers || []).filter((x) => x.category === category.value))
const currentTrigger = computed(() => triggerOf(catalog.value, form.value.trigger))
const availableActions = computed(() => (catalog.value?.actions || []).filter((a) => currentTrigger.value?.actions.includes(a.type)))
const variables = computed(() => [...(catalog.value?.variables || []), ...(currentTrigger.value?.fields || [])].map((v) => ({ key: v.key, label: v.label })))
const sentence = computed(() => ruleSentence(form.value, catalog.value, storeName))

// 打开已有规则 / 模板时切到它的分类
watch(
  () => [form.value.trigger, catalog.value],
  () => {
    if (currentTrigger.value) category.value = currentTrigger.value.category
  },
  { immediate: true }
)

/** 换触发器：去掉新触发器没有的字段条件（动作保留，不可用的会标红提示） */
function pickTrigger(code) {
  if (form.value.trigger === code) return
  form.value.trigger = code
  const keys = new Set((triggerOf(catalog.value, code)?.fields || []).map((f) => f.key))
  form.value.condition.items = form.value.condition.items.filter((c) => !c.field || keys.has(c.field))
  form.value.condition.groups = (form.value.condition.groups || []).map((g) => ({ ...g, items: g.items.filter((c) => !c.field || keys.has(c.field)) }))
  if (!form.value.name) form.value.name = triggerOf(catalog.value, code)?.name || ''
  formRef.value?.validateFields(['trigger']).catch(() => {})
}

function addAction(type) {
  form.value.actions.push(applyActionDefaults({ type, _key: ++keySeq }))
  setTimeout(() => formRef.value?.validateFields(['actions']).catch(() => {}))
}

function move(i, d) {
  const list = form.value.actions
  const [a] = list.splice(i, 1)
  list.splice(i + d, 0, a)
}

/** 插入变量：有最近编辑的标题 / 内容时追加到末尾，否则复制到剪贴板 */
async function insertVar(key) {
  const token = varToken(key)
  const f = lastFocus.value
  if (f && form.value.actions.includes(f.action)) {
    f.action[f.key] = `${f.action[f.key] || ''}${token}`
    return
  }
  try {
    await navigator.clipboard.writeText(token)
  } catch {
    // 剪贴板不可用时只提示
  }
  message.success(t('ecomAuto.editor.copied', { v: token }))
}

async function runTest() {
  if (!form.value.trigger || !form.value.actions.length) {
    message.warning(t('ecomAuto.editor.saveFirst'))
    return
  }
  testing.value = true
  try {
    const res = await testAutoRule({ ...payload(), name: form.value.name || currentTrigger.value?.name, id: form.value.id })
    testRef.value.open(res, form.value)
  } finally {
    testing.value = false
  }
}

defineExpose({
  openCreate: (defaults) => {
    userNames.value = {}
    return drawer.openCreate(defaults ? toForm(defaults) : undefined)
  },
  openEdit: drawer.openEdit
})
</script>

<style scoped>
.step {
  margin-bottom: 16px;
  padding: 16px 16px 4px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
}
.step-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}
.step-no {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--app-primary);
  color: #fff;
  font-weight: 600;
  line-height: 24px;
  text-align: center;
  align-self: center;
}
.step-icon {
  color: var(--app-primary);
  font-size: 18px;
  align-self: center;
}
.step-title {
  font-size: 15px;
  font-weight: 600;
}
.step-desc {
  color: var(--app-text-3);
  font-size: 13px;
}
.no-label > :deep(.ant-form-item-row > .ant-form-item-label) {
  display: none;
}
.cats {
  margin-bottom: 12px;
  max-width: 100%;
  overflow-x: auto;
}
.trigger-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}
.trigger-card {
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.trigger-card:hover {
  border-color: var(--app-primary-border, var(--app-primary));
}
.trigger-card.active {
  border-color: var(--app-primary);
  background: var(--app-primary-bg);
}
.trigger-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.check {
  color: var(--app-primary);
}
.mode {
  margin-left: auto;
  margin-right: 0;
  font-size: 11px;
}
.trigger-desc {
  margin-top: 4px;
  color: var(--app-text-3);
  font-size: 12px;
  line-height: 1.5;
}
.sub-title {
  margin: 4px 0 8px;
  font-weight: 500;
}
.sub-tip {
  margin-left: 8px;
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.empty-tip {
  padding: 8px 12px 16px;
  color: var(--app-text-3);
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.menu-name {
  font-weight: 500;
}
.menu-desc {
  max-width: 360px;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: normal;
}
.full {
  width: 100%;
}
.switch-label {
  margin-left: 8px;
}
.side {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.side-card {
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
}
.side-title {
  margin-bottom: 8px;
  font-weight: 600;
}
.sentence {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-primary-bg);
  line-height: 1.8;
  word-break: break-word;
}
.side-tip {
  margin-top: 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
.vars {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  max-height: 320px;
  overflow-y: auto;
}
.var {
  margin: 0;
  cursor: pointer;
}
</style>
