<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('wms.material.createTitle')" :edit-title="$t('wms.material.editTitle')">
    <AiPrefillAlert :ai="ai" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.code')" name="code" :class="ai.cls('code')">
            <a-input v-model:value="form.code" :maxlength="64" :placeholder="$t('wms.material.codePh')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.name')" name="name" :class="ai.cls('name')">
            <a-input v-model:value="form.name" :maxlength="128" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.inout.spec')" name="spec">
            <a-input v-model:value="form.spec" :maxlength="128" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.unit')" name="unit">
            <a-auto-complete v-model:value="form.unit" :options="UNITS" :maxlength="16" :placeholder="$t('wms.material.unitPh')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.inout.category')" name="category" :class="ai.cls('category')">
            <MaterialCategorySelect v-model="form.category" usable-only :placeholder="$t('common.placeholder.select')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('pricing.goods.brand')" name="brand">
            <a-select
              v-model:value="form.brand"
              :placeholder="$t('wms.common.optional')"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="toOptions(dict.wms_material_brand)"
              :disabled="basicReadonly"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.stock.safetyStock')" name="safetyStock" :extra="$t('wms.material.safetyTip')">
            <a-input-number v-model:value="form.safetyStock" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item name="reorderPoint" :extra="$t('wms.material.reorderPointExtra')">
            <template #label>
              {{ $t('wms.material.reorderPoint') }}
              <a-tooltip :title="$t('wms.material.reorderPointTip')">
                <QuestionCircleOutlined class="label-help" />
              </a-tooltip>
            </template>
            <a-input-number v-model:value="form.reorderPoint" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.notInvolved')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.reorderQty')" name="reorderQty" :extra="$t('wms.material.reorderQtyTip')">
            <a-input-number v-model:value="form.reorderQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.common.optional')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.units.barcode')" name="barcode" :extra="$t('wms.material.barcodeTip')">
            <a-input v-model:value="form.barcode" :maxlength="64" :placeholder="$t('wms.common.optional')" allow-clear :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.refCost')" name="refCost" :extra="$t('wms.material.refCostTip')">
            <a-input-number v-model:value="form.refCost" :min="0" :precision="4" class="full" :placeholder="$t('wms.common.optional')" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.costMethod')" name="costMethod" :extra="$t('wms.material.costMethodTip')">
            <a-select v-model:value="form.costMethod" :options="toOptions(dict.wms_cost_method)" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.defaultWarehouse')" name="defaultWarehouseId">
            <WarehouseSelect v-model:value="form.defaultWarehouseId" :placeholder="$t('wms.common.optional')" :disabled="basicReadonly" @update:value="form.defaultLocationId = null" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="colSpan">
          <a-form-item :label="$t('wms.material.defaultLocation')" name="defaultLocationId" :extra="$t('wms.material.defaultLocationTip')">
            <LocationSelect v-model:value="form.defaultLocationId" :warehouse-id="form.defaultWarehouseId" :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
        <a-col v-if="enabledLevels.length" :span="24">
          <div class="sub-title">{{ $t('pricing.level.prices') }}</div>
          <a-row :gutter="16">
            <a-col v-for="l in enabledLevels" :key="l.level" :xs="12" :sm="6">
              <a-form-item :label="l.name" :name="l.field">
                <a-input-number v-model:value="form[l.field]" :min="0" :precision="4" class="full" :disabled="basicReadonly" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('wms.material.tracking')" :extra="$t('wms.material.trackingTip')">
            <a-space direction="vertical">
              <a-checkbox :checked="form.batchEnabled === 1" :disabled="basicReadonly" @change="(e) => (form.batchEnabled = e.target.checked ? 1 : 0)">
                {{ $t('wms.material.batchManaged') }}<span class="option-desc">{{ $t('wms.material.batchManagedDesc') }}</span>
              </a-checkbox>
              <a-checkbox :checked="form.serialEnabled === 1" :disabled="basicReadonly" @change="(e) => (form.serialEnabled = e.target.checked ? 1 : 0)">
                {{ $t('wms.material.serialManaged') }}<span class="option-desc">{{ $t('wms.material.serialManagedDesc') }}</span>
              </a-checkbox>
              <a-checkbox :checked="form.iqcRequired === 1" :disabled="basicReadonly" @change="(e) => (form.iqcRequired = e.target.checked ? 1 : 0)">
                {{ $t('wms.material.iqc') }}<span class="option-desc">{{ $t('wms.material.iqcDesc') }}</span>
              </a-checkbox>
            </a-space>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count :disabled="basicReadonly" />
          </a-form-item>
        </a-col>
      </a-row>

      <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('wms.material.customFields')" />
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { isZh } from '@/locales'
import { message } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import AiPrefillAlert from '@/components/ai/AiPrefillAlert.vue'
import { addMaterial, getMaterial, updateMaterial } from '@/api/wms/material'
import { useAiPrefill } from '@/composables/useAiPrefill'
import { useCustomFields } from '@/composables/useCustomFields'
import { useDict } from '@/composables/useDict'
import { asPrefilled, useFormDrawer } from '@/composables/useFormDrawer'
import { PRICE_FIELDS, usePriceLevels } from '@/components/wms/priceLevels'
import { CostMethod } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

/**
 * 物料新增 / 编辑（子页）：基本资料、价格列、批次 / 序列号 / 来料检验、自定义字段。
 * openCreate(defaults) / openEdit(record) / copy(record)（复制物料），applyAi(data) 用智能录入的结果预填；保存后触发 saved
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:material:edit'))
const dict = useDict('wms_material_brand', 'wms_cost_method')
const { enabledLevels } = usePriceLevels()
const cf = useCustomFields('wms_material')
const cfFields = cf.fields
const UNITS = (isZh() ? ['个', '件', '台', '箱', '包', '桶', '瓶', '袋', 'kg', 'g', 'L', 'm'] : ['pcs', 'set', 'box', 'pack', 'bag', 'bottle', 'drum', 'kg', 'g', 'L', 'm']).map((value) => ({ value })) // i18n-ignore

const rules = {
  code: [
    { required: true, message: t('wms.material.inputCode') },
    { pattern: /^[A-Za-z0-9_.-]{1,64}$/, message: t('wms.material.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('wms.material.inputName') }]
}
const colSpan = 12
const basicReadonly = computed(() => !!form.value.id && !canEdit.value)
const MATERIAL_FIELDS = [
  'code',
  'name',
  'spec',
  'unit',
  'category',
  'brand',
  ...PRICE_FIELDS,
  'batchEnabled',
  'serialEnabled',
  'iqcRequired',
  'safetyStock',
  'reorderPoint',
  'reorderQty',
  'barcode',
  'defaultWarehouseId',
  'defaultLocationId',
  'refCost',
  'costMethod',
  'remark'
]
const num = (v) => (v == null ? null : Number(v))

const drawerOptions = cf.wrap({
  emptyForm: () => ({
    id: undefined,
    code: '',
    name: '',
    spec: '',
    unit: undefined,
    category: undefined,
    brand: undefined,
    ...Object.fromEntries(PRICE_FIELDS.map((k) => [k, null])),
    batchEnabled: 0,
    serialEnabled: 0,
    iqcRequired: 0,
    safetyStock: null,
    reorderPoint: null,
    reorderQty: null,
    barcode: '',
    defaultWarehouseId: null,
    defaultLocationId: null,
    refCost: null,
    costMethod: CostMethod.WEIGHTED,
    remark: '',
    copyFrom: undefined
  }),
  fetch: async (record) => {
    const m = await getMaterial(record.id)
    const form = { ...m, safetyStock: num(m.safetyStock), reorderPoint: num(m.reorderPoint), reorderQty: num(m.reorderQty), refCost: num(m.refCost), barcode: m.barcode || '' }
    PRICE_FIELDS.forEach((k) => (form[k] = num(m[k])))
    return form
  },
  create: ({ material, copyFrom }) => addMaterial(material, copyFrom),
  update: (id, { material }) => updateMaterial(id, material),
  toPayload: (f) => ({
    copyFrom: f.copyFrom,
    material: {
      ...Object.fromEntries(MATERIAL_FIELDS.map((k) => [k, f[k]])),
      reorderPoint: f.reorderPoint ?? null,
      reorderQty: f.reorderQty ?? null,
      refCost: f.refCost ?? null,
      ...Object.fromEntries(PRICE_FIELDS.map((k) => [k, f[k] ?? null])),
      brand: f.brand || null,
      barcode: f.barcode?.trim() || null,
      defaultLocationId: f.defaultWarehouseId ? f.defaultLocationId || null : null
    }
  }),
  onSuccess: () => emit('saved')
})
const drawer = useFormDrawer(drawerOptions)
const { formRef, form } = drawer
// 智能录入：规格书、商品标签等识别结果打开新增物料并预填
const ai = useAiPrefill('material', (data) => drawer.openCreate(data), () => drawer.open.value)

// 复制：带出原物料的资料和自定义字段，编码留空（按编码规则生成）、条码不带；保存时后端复制辅助单位（不含包装条码）和图片
async function copyMaterial(record) {
  const src = await drawerOptions.fetch(record)
  const data = omitKeys(src, ['id', 'code', 'barcode', 'createTime', 'updateTime', 'createBy', 'status', 'isTemplate', 'templateId', 'variantText', 'imageUrl', 'searchKey'])
  if (record.isTemplate === 1 || record.templateId) message.info(t('wms.material.copyVariantTip'))
  asPrefilled(() => drawer.openCreate({ ...data, code: '', barcode: '', copyFrom: record.id }))
}
const omitKeys = (obj, keys) => Object.fromEntries(Object.entries(obj).filter(([k]) => !keys.includes(k)))

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit, copy: copyMaterial, applyAi: (data) => ai.apply(data) })
</script>

<style scoped>
.option-desc {
  color: var(--app-text-3);
  font-weight: normal;
  font-size: 12px;
}
.full {
  width: 100%;
}
.label-help {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: help;
}
.sub-title {
  font-weight: 500;
  margin-bottom: 8px;
}
</style>
