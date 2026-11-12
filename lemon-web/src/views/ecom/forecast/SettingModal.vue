<template>
  <a-modal :open="open" :title="$t('ecomAnalytics.forecast.setting')" :confirm-loading="saving" @ok="save" @cancel="open = false">
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('ecomAnalytics.forecast.alpha')" name="alpha" :extra="$t('ecomAnalytics.forecast.alphaExtra')">
              <a-input-number v-model:value="form.alpha" :min="0.01" :max="1" :step="0.05" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomAnalytics.forecast.beta')" name="beta" :extra="$t('ecomAnalytics.forecast.betaExtra')">
              <a-input-number v-model:value="form.beta" :min="0" :max="1" :step="0.05" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomAnalytics.forecast.windowDays')" name="windowDays" :extra="$t('ecomAnalytics.forecast.windowExtra')">
              <a-input-number v-model:value="form.windowDays" :min="7" :max="90" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomAnalytics.forecast.seasonWeeks')" name="seasonWeeks" :extra="$t('ecomAnalytics.forecast.seasonExtra')">
              <a-input-number v-model:value="form.seasonWeeks" :min="2" :max="16" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFix.stockout.exclude')" name="excludeStockout" :extra="$t('ecomFix.stockout.excludeExtra')">
              <a-switch v-model:checked="form.excludeStockout" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFix.stockout.threshold')" name="stockoutThreshold" :extra="$t('ecomFix.stockout.thresholdExtra')">
              <a-input-number v-model:value="form.stockoutThreshold" :min="0" :max="100000" :precision="0" :disabled="!form.excludeStockout" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="sub-text">{{ $t('ecomAnalytics.forecast.settingTip') }}</div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getForecastSetting, saveForecastSetting } from '@/api/ecom/analytics'
import { t } from '@/locales'

/** 预测参数：Holt 的 α / β、移动平均天数、星期系数周数、断货日处理（下次生成时生效，补货建议的加权日均也按断货设置排除） */
const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ alpha: 0.3, beta: 0.1, windowDays: 28, seasonWeeks: 8, excludeStockout: true, stockoutThreshold: 0 })
const required = [{ required: true, message: t('ecomAnalytics.common.required') }]
const rules = { alpha: required, beta: required, windowDays: required, seasonWeeks: required }

async function show() {
  open.value = true
  loading.value = true
  try {
    const s = await getForecastSetting()
    Object.assign(form, {
      alpha: Number(s.alpha),
      beta: Number(s.beta),
      windowDays: s.windowDays,
      seasonWeeks: s.seasonWeeks,
      excludeStockout: s.excludeStockout !== false,
      stockoutThreshold: Number(s.stockoutThreshold || 0)
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await saveForecastSetting({ ...form })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
