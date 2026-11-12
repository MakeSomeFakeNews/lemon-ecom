<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomAds.rule.add')" :edit-title="$t('ecomAds.rule.edit')">
    <a-row :gutter="24">
      <a-col :xs="24" :xl="17">
        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
          <div class="section-title">{{ $t('ecomAds.rule.sectionBasic') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :lg="12">
              <a-form-item :label="$t('ecomAds.rule.name')" name="name">
                <a-input v-model:value="form.name" :maxlength="64" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :lg="6">
              <a-form-item :label="$t('ecomAds.rule.level')" name="level">
                <a-select v-model:value="form.level" :options="levelOptions" @change="onLevelChange" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :lg="6">
              <a-form-item :label="$t('ecomAds.rule.window')" name="windowDays">
                <a-input-number v-model:value="form.windowDays" :min="1" :max="90" :precision="0" :addon-after="$t('ecomAds.rule.windowSuffix')" class="full" />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="section-title">{{ $t('ecomAds.rule.scope') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :lg="12">
              <a-form-item :label="$t('ecomAds.rule.stores')" name="storeIds">
                <a-select v-model:value="form.storeIds" mode="multiple" allow-clear :options="storeOptions" option-filter-prop="label" :placeholder="$t('ecomAds.rule.any')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item :label="$t('ecomAds.rule.adType')" name="adType">
                <a-select v-model:value="form.adType" allow-clear :options="toOptions(dict.ecom_ad_type)" :placeholder="$t('ecomAds.rule.any')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item :label="$t('ecomAds.rule.campaignKeyword')" name="campaignKeyword">
                <a-input v-model:value="form.campaignKeyword" :maxlength="128" :placeholder="$t('ecomAds.rule.any')" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item :label="$t('ecomAds.rule.tags')" name="tags">
                <a-select v-model:value="form.tags" mode="tags" :open="false" :token-separators="[',']" :placeholder="$t('ecomAds.rule.any')" />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="section-title">{{ $t('ecomAds.rule.conditions') }}</div>
          <div class="cond-box">
            <div v-for="(c, i) in form.conditions" :key="i" class="cond">
              <span class="cond-no">{{ i + 1 }}</span>
              <a-select v-model:value="c.metric" :options="metricOptions" class="metric" />
              <a-select v-model:value="c.operator" :options="operatorOptions" class="op" />
              <a-input-number v-model:value="c.value" :min="0" class="val" />
              <a-button type="text" danger :disabled="form.conditions.length <= 1" @click="form.conditions.splice(i, 1)"><DeleteOutlined /></a-button>
            </div>
            <a-button type="dashed" size="small" :disabled="form.conditions.length >= 10" @click="form.conditions.push(emptyCondition())">
              <template #icon><PlusOutlined /></template>{{ $t('ecomAds.rule.addCondition') }}
            </a-button>
          </div>

          <div class="section-title">{{ $t('ecomAds.rule.action') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item :label="$t('ecomAds.rule.action')" name="action">
                <a-select v-model:value="form.action" :options="actionOptions" />
              </a-form-item>
            </a-col>
            <template v-if="hasValue">
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item :label="$t('ecomAds.rule.actionValue')" name="actionValue" :extra="isPercent ? $t('ecomAds.rule.percentSuffix') : undefined">
                  <a-input-number v-model:value="form.actionValue" :precision="2" class="full" :addon-after="isPercent ? '%' : undefined" />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :lg="5">
                <a-form-item :label="$t('ecomAds.rule.min')" name="minValue">
                  <a-input-number v-model:value="form.minValue" :min="0" :precision="2" class="full" />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :lg="5">
                <a-form-item :label="$t('ecomAds.rule.max')" name="maxValue">
                  <a-input-number v-model:value="form.maxValue" :min="0" :precision="2" class="full" />
                </a-form-item>
              </a-col>
            </template>
          </a-row>

          <div class="section-title">{{ $t('ecomAds.rule.runMode') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item :label="$t('ecomAds.rule.runMode')" name="runMode">
                <a-radio-group v-model:value="form.runMode" :options="runModeOptions" />
              </a-form-item>
            </a-col>
            <a-col v-if="form.runMode === EcomAdRunMode.DAILY" :xs="24" :sm="12" :lg="6">
              <a-form-item :label="$t('ecomAds.rule.runHour')" name="runHour">
                <a-select v-model:value="form.runHour" :options="hourOptions" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="10">
              <a-form-item :label="$t('ecomAds.rule.owner')" name="ownerId" :extra="$t('ecomAds.rule.ownerTip')">
                <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item name="autoApply" :extra="$t('ecomAds.rule.autoApplyTip')">
                <a-checkbox v-model:checked="form.autoApply" :disabled="form.action === EcomAdAction.NOTIFY">{{ $t('ecomAds.rule.autoApply') }}</a-checkbox>
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item :label="$t('common.field.remark')" name="remark">
                <a-input v-model:value="form.remark" :maxlength="255" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-col>

      <a-col :xs="24" :xl="7">
        <section class="summary">
          <div class="section-title">{{ $t('ecomAds.rule.summaryTitle') }}</div>
          <a-descriptions :column="1" size="small" class="summary-desc">
            <a-descriptions-item :label="$t('ecomAds.rule.level')">
              {{ $t(`ecomAds.enum.level.${form.level}`) }} · {{ $t('ecomAds.rule.windowDays', { n: form.windowDays || '-' }) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomAds.rule.conditionsShort')">{{ conditionText(form) || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomAds.rule.action')">{{ form.action ? actionText(form, actionLabel) : '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomAds.rule.runMode')">
              {{ $t(`ecomAds.enum.runMode.${form.runMode}`) }}<template v-if="form.runMode === EcomAdRunMode.DAILY"> · {{ $t('ecomAds.schedule.hour', { h: form.runHour }) }}</template>
              · {{ form.autoApply && form.action !== EcomAdAction.NOTIFY ? $t('ecomAds.rule.autoApply') : $t('ecomAds.rule.suggestOnly') }}
            </a-descriptions-item>
          </a-descriptions>
          <template v-if="form.id">
            <a-button block @click="previewRef.open({ id: form.id, name: form.name })">
              <template #icon><EyeOutlined /></template>{{ $t('ecomAds.rule.preview') }}
            </a-button>
            <div class="sub-text preview-tip">{{ $t('ecomAds.rule.previewSavedTip') }}</div>
          </template>
          <div v-else class="sub-text">{{ $t('ecomAds.rule.previewAfterSave') }}</div>
        </section>
      </a-col>
    </a-row>
    <RulePreview ref="previewRef" />
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { DeleteOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { addAdRule, getAdRule, updateAdRule } from '@/api/ecom/adRule'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { EcomAdAction, EcomAdLevel, EcomAdMetric, EcomAdOperator, EcomAdRunMode } from '@/constants/enums'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'
import RulePreview from './RulePreview.vue'
import { actionText, conditionText } from './ruleText'

/**
 * 广告规则新增 / 编辑（子页）：对象层级与统计窗口、对象范围（店铺、广告类型、活动名称包含、标签）、条件（全部满足）、
 * 动作（竞价 / 预算按百分比或固定值、上下限，暂停 / 启用，仅提醒）、执行方式（手动 / 每天整点）、自动执行与负责人；
 * 右侧按当前填写显示规则摘要，编辑时可按已保存的规则预览命中对象
 */
const emit = defineEmits(['success'])
const dict = useDict('ecom_ad_type', 'ecom_ad_rule_action')
const stores = useStores()
const storeOptions = computed(() => stores.value.map((s) => ({ value: s.id, label: s.name })))
const levelOptions = Object.values(EcomAdLevel).map((v) => ({ value: v, label: t(`ecomAds.enum.level.${v}`) }))
const metricOptions = Object.values(EcomAdMetric).map((v) => ({ value: v, label: t(`ecomAds.enum.metric.${v}`) }))
const operatorOptions = Object.values(EcomAdOperator).map((v) => ({ value: v, label: t(`ecomAds.enum.operator.${v}`) }))
const runModeOptions = Object.values(EcomAdRunMode).map((v) => ({ value: v, label: t(`ecomAds.enum.runMode.${v}`) }))
const hourOptions = Array.from({ length: 24 }, (_, h) => ({ value: h, label: t('ecomAds.schedule.hour', { h }) }))
const previewRef = ref()
const actionLabel = (v) => findDict(dict.ecom_ad_rule_action, v)?.label || v
const BID = [EcomAdAction.BID_PERCENT, EcomAdAction.BID_SET]
const BUDGET = [EcomAdAction.BUDGET_PERCENT, EcomAdAction.BUDGET_SET]

/** 活动只能调整预算，广告组 / 投放只能调整竞价 */
const actionOptions = computed(() =>
  toOptions(dict.ecom_ad_rule_action).filter((o) => (form.value.level === EcomAdLevel.CAMPAIGN ? !BID.includes(o.value) : !BUDGET.includes(o.value)))
)
const hasValue = computed(() => BID.includes(form.value.action) || BUDGET.includes(form.value.action))
const isPercent = computed(() => form.value.action === EcomAdAction.BID_PERCENT || form.value.action === EcomAdAction.BUDGET_PERCENT)

const emptyCondition = () => ({ metric: EcomAdMetric.ACOS, operator: EcomAdOperator.GT, value: 30 })
const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomAds.rule.name') }) }],
  level: [{ required: true, message: t('common.rule.required', { name: t('ecomAds.rule.level') }) }],
  action: [{ required: true, message: t('common.rule.required', { name: t('ecomAds.rule.action') }) }]
}

function onLevelChange(level) {
  const ok = level === EcomAdLevel.CAMPAIGN ? !BID.includes(form.value.action) : !BUDGET.includes(form.value.action)
  if (!ok) form.value.action = level === EcomAdLevel.CAMPAIGN ? EcomAdAction.BUDGET_PERCENT : EcomAdAction.BID_PERCENT
}

const num = (v) => (v == null ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: '',
    level: EcomAdLevel.TARGET,
    windowDays: 7,
    storeIds: [],
    adType: undefined,
    campaignKeyword: '',
    tags: [],
    conditions: [emptyCondition(), { metric: EcomAdMetric.CLICKS, operator: EcomAdOperator.GTE, value: 10 }],
    action: EcomAdAction.BID_PERCENT,
    actionValue: -10,
    minValue: undefined,
    maxValue: undefined,
    runMode: EcomAdRunMode.MANUAL,
    runHour: 6,
    autoApply: false,
    ownerId: undefined,
    ownerName: undefined,
    remark: ''
  }),
  fetch: async (record) => {
    const d = await getAdRule(record.id)
    return {
      ...d,
      ownerName: record.ownerName,
      storeIds: d.storeIds ? d.storeIds.split(',').map(Number) : [],
      tags: d.tags ? d.tags.split(',') : [],
      campaignKeyword: d.campaignKeyword || '',
      conditions: (d.conditions || []).map((c) => ({ ...c, value: num(c.value) })),
      actionValue: num(d.actionValue),
      minValue: num(d.minValue),
      maxValue: num(d.maxValue),
      autoApply: d.autoApply === 1,
      remark: d.remark || ''
    }
  },
  create: addAdRule,
  update: updateAdRule,
  toPayload: (f) => ({
    name: f.name.trim(),
    level: f.level,
    windowDays: f.windowDays,
    storeIds: f.storeIds?.length ? f.storeIds.join(',') : null,
    adType: f.adType || null,
    campaignKeyword: f.campaignKeyword?.trim() || null,
    tags: f.tags?.length ? f.tags.join(',') : null,
    conditions: f.conditions.filter((c) => c.metric && c.operator && c.value != null),
    action: f.action,
    actionValue: hasValue.value ? (f.actionValue ?? null) : null,
    minValue: hasValue.value ? (f.minValue ?? null) : null,
    maxValue: hasValue.value ? (f.maxValue ?? null) : null,
    runMode: f.runMode,
    runHour: f.runHour,
    autoApply: f.autoApply && f.action !== EcomAdAction.NOTIFY ? 1 : 0,
    ownerId: f.ownerId ?? null,
    remark: f.remark || null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.section-title {
  margin: 4px 0 12px;
  font-weight: 600;
}
.cond-box {
  margin-bottom: 24px;
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.cond {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.cond-no {
  flex: none;
  width: 20px;
  color: var(--app-text-3);
  text-align: center;
}
.metric {
  flex: 2;
  min-width: 0;
  max-width: 280px;
}
.op {
  flex: 1;
  min-width: 0;
  max-width: 160px;
}
.val {
  flex: 1;
  min-width: 0;
  max-width: 180px;
}
.summary {
  position: sticky;
  top: 0;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-hover);
}
.summary-desc {
  margin-bottom: 12px;
}
.summary-desc :deep(.ant-descriptions-item-label) {
  min-width: 72px;
  color: var(--app-text-3);
  white-space: nowrap;
}
.preview-tip {
  margin-top: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
