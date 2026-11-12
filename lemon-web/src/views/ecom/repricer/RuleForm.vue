<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomPublish.repricer.add')" :edit-title="$t('ecomPublish.repricer.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomPublish.repricer.sectionScope') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('ecomPublish.repricer.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :lg="16">
          <a-form-item :label="$t('ecomPublish.repricer.stores')" name="storeList" :extra="$t('ecomPublish.repricer.storesExtra')">
            <a-select v-model:value="form.storeList" mode="multiple" allow-clear :options="storeOptions" :placeholder="$t('ecomPublish.repricer.allStores')" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomPublish.repricer.skus')" name="skus" :extra="$t('ecomPublish.repricer.skusExtra')">
            <a-textarea v-model:value="form.skus" :auto-size="{ minRows: 2, maxRows: 5 }" :maxlength="1000" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.repricer.sectionStrategy') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomPublish.repricer.strategy')" name="strategy">
            <a-select v-model:value="form.strategy" :options="toOptions(dict.ecom_reprice_strategy)" />
          </a-form-item>
        </a-col>
        <template v-if="needsAdjust">
          <a-col :xs="12" :sm="6" :lg="4">
            <a-form-item :label="$t('ecomPublish.repricer.adjustMode')" name="adjustMode">
              <a-select v-model:value="form.adjustMode" :options="adjustOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6" :lg="4">
            <a-form-item :label="$t('ecomPublish.repricer.adjustValue')" name="adjustValue">
              <a-input-number v-model:value="form.adjustValue" :min="0" :precision="2" class="full" :addon-after="form.adjustMode === PriceAdjustMode.PERCENT ? '%' : undefined" />
            </a-form-item>
          </a-col>
        </template>
        <a-col v-if="form.strategy === RepriceStrategy.TARGET_MARGIN" :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.repricer.targetMargin')" name="targetMargin" :extra="$t('ecomPublish.repricer.targetMarginExtra')">
            <a-input-number v-model:value="form.targetMargin" :min="-50" :max="90" :precision="2" class="full" addon-after="%" />
          </a-form-item>
        </a-col>
        <template v-if="form.strategy === RepriceStrategy.BY_STOCK">
          <a-col :xs="12" :sm="6" :lg="4">
            <a-form-item :label="$t('ecomPublish.repricer.highDays')" name="stockHighDays">
              <a-input-number v-model:value="form.stockHighDays" :min="1" :max="999" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6" :lg="4">
            <a-form-item :label="$t('ecomPublish.repricer.lowDays')" name="stockLowDays">
              <a-input-number v-model:value="form.stockLowDays" :min="0" :max="999" :precision="0" class="full" />
            </a-form-item>
          </a-col>
        </template>
        <a-col :span="24"><div class="sub-text strategy-tip">{{ $t(`ecomPublish.repricer.strategyTip.${form.strategy}`) }}</div></a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.repricer.sectionLimit') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomPublish.repricer.floorMode')" name="floorMode">
            <a-radio-group v-model:value="form.floorMode" :options="toOptions(dict.ecom_reprice_floor_mode)" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.floorMode === RepriceFloorMode.FIXED" :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.repricer.minPrice')" name="minPrice">
            <a-input-number v-model:value="form.minPrice" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-else :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.repricer.minMargin')" name="minMargin">
            <a-input-number v-model:value="form.minMargin" :min="-50" :max="90" :precision="2" class="full" addon-after="%" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.repricer.maxPrice')" name="maxPrice">
            <a-input-number v-model:value="form.maxPrice" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomPublish.item.priceTemplate')" name="priceTemplateId" :required="needsTemplate" :extra="$t('ecomPublish.repricer.priceTemplateExtra')">
            <a-select v-model:value="form.priceTemplateId" allow-clear :options="priceOptions" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomPublish.repricer.sectionRun') }}</div>
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.repricer.interval')" name="intervalMinutes">
            <a-select v-model:value="form.intervalMinutes" :options="intervalOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('ecomPublish.repricer.suggestOnly')" name="suggestOnly">
            <a-switch v-model:checked="form.suggestOnly" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="16">
          <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { addRepriceRule, getRepriceRule, priceTemplateOptions, updateRepriceRule } from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { PriceAdjustMode, RepriceFloorMode, RepriceStrategy } from '@/constants/enums'
import { findDict, toOptions } from '@/utils/dict'

/** 调价规则新增 / 编辑 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_reprice_strategy', 'ecom_reprice_floor_mode', 'ecom_price_adjust_mode', 'ecom_platform')
const stores = useStores()
const priceTemplates = ref([])
const storeOptions = computed(() => stores.value.map((s) => ({ value: String(s.id), label: `${s.name}（${findDict(dict.ecom_platform, s.platform)?.label || s.platform}）` })))
const priceOptions = computed(() => priceTemplates.value.map((x) => ({ value: x.id, label: x.name })))
const adjustOptions = computed(() => toOptions(dict.ecom_price_adjust_mode).filter((o) => o.value === PriceAdjustMode.AMOUNT || o.value === PriceAdjustMode.PERCENT))
const intervalOptions = [15, 30, 60, 120, 240, 360, 720, 1440].map((n) => ({ value: n, label: t('ecomPublish.repricer.everyMinutes', { n }) }))

const needsAdjust = computed(() => form.value.strategy === RepriceStrategy.BEAT_LOWEST || form.value.strategy === RepriceStrategy.BY_STOCK)
const needsTemplate = computed(() => form.value.strategy === RepriceStrategy.TARGET_MARGIN || form.value.floorMode === RepriceFloorMode.COST_MARGIN)
const req = (key) => [{ required: true, message: t('common.rule.required', { name: t(key) }) }]
const rules = computed(() => ({
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomPublish.repricer.name') }) }],
  strategy: req('ecomPublish.repricer.strategy'),
  floorMode: req('ecomPublish.repricer.floorMode'),
  adjustMode: needsAdjust.value ? req('ecomPublish.repricer.adjustMode') : [],
  adjustValue: needsAdjust.value ? req('ecomPublish.repricer.adjustValue') : [],
  minMargin: form.value.floorMode === RepriceFloorMode.COST_MARGIN ? req('ecomPublish.repricer.minMargin') : [],
  priceTemplateId: needsTemplate.value ? req('ecomPublish.item.priceTemplate') : []
}))

const FIELDS = ['name', 'skus', 'strategy', 'adjustMode', 'adjustValue', 'targetMargin', 'floorMode', 'minPrice', 'maxPrice', 'minMargin', 'priceTemplateId',
  'stockHighDays', 'stockLowDays', 'intervalMinutes', 'suggestOnly', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', storeList: [], skus: '', strategy: RepriceStrategy.MATCH_BUYBOX, adjustMode: PriceAdjustMode.AMOUNT,
    adjustValue: 0.1, floorMode: RepriceFloorMode.COST_MARGIN, minMargin: 10, intervalMinutes: 60, suggestOnly: 1 }),
  beforeOpen: async () => {
    priceTemplates.value = (await priceTemplateOptions()) || []
  },
  fetch: async (r) => {
    const rule = await getRepriceRule(r.id)
    return { ...rule, storeList: rule.storeIds ? rule.storeIds.split(',') : [] }
  },
  create: addRepriceRule,
  update: updateRepriceRule,
  toPayload: (f) => ({
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    storeIds: f.storeList?.length ? f.storeList.join(',') : null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
.full {
  width: 100%;
}
.strategy-tip {
  margin: -8px 0 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
