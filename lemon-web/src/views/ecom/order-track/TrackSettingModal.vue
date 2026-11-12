<template>
  <a-modal v-model:open="open" :title="$t('ecomOrd.setting.title')" width="min(640px, 100vw)" :confirm-loading="saving" @ok="submit">
    <a-form layout="vertical">
      <div class="section-title">{{ $t('ecomOrd.setting.address') }}</div>
      <a-form-item :extra="$t('ecomOrd.setting.addressHint')">
        <a-checkbox v-model:checked="form.addressCheck">{{ $t('ecomOrd.setting.addressCheck') }}</a-checkbox>
      </a-form-item>
      <div class="section-title">{{ $t('ecomOrd.setting.track') }}</div>
      <a-form-item>
        <a-checkbox v-model:checked="form.trackEnabled">{{ $t('ecomOrd.setting.trackEnabled') }}</a-checkbox>
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.setting.trackKey')" :extra="$t('ecomOrd.setting.trackKeyHint')">
        <a-input-password v-model:value="form.trackKey" :maxlength="128" autocomplete="new-password"
                          :placeholder="hasKey ? $t('ecomOrd.setting.keySaved') : $t('ecomOrd.setting.keyPh')" />
        <a v-if="hasKey" class="clear" @click="clearKey = true; form.trackKey = ''">{{ clearKey ? $t('ecomOrd.setting.keyWillClear') : $t('ecomOrd.setting.clearKey') }}</a>
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.setting.trackApiUrl')" :extra="$t('ecomOrd.setting.trackApiUrlHint')">
        <a-input v-model:value="form.trackApiUrl" :maxlength="255" placeholder="https://api.17track.net/track/v2.2" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOrd.setting.noScanHours')">
            <a-input-number v-model:value="form.noScanHours" :min="1" :max="720" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOrd.setting.transitDays')">
            <a-input-number v-model:value="form.transitDays" :min="1" :max="180" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOrd.setting.staleDays')">
            <a-input-number v-model:value="form.staleDays" :min="1" :max="180" :precision="0" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getOrderSetting, saveOrderSetting } from '@/api/ecom/orderEnhance'

/** 订单作业设置：审核前地址校验、定时拉取轨迹、17TRACK 密钥（只写，保存后不回显）与自定义地址、预警时限。open() 打开 */
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const hasKey = ref(false)
const clearKey = ref(false)
const form = reactive({ addressCheck: true, trackEnabled: true, trackKey: '', trackApiUrl: '', noScanHours: 72, transitDays: 20, staleDays: 7 })

async function show() {
  const s = await getOrderSetting()
  hasKey.value = !!s.hasTrackKey
  clearKey.value = false
  Object.assign(form, { addressCheck: s.addressCheck === 1, trackEnabled: s.trackEnabled === 1, trackKey: '', trackApiUrl: s.trackApiUrl || '',
    noScanHours: s.noScanHours, transitDays: s.transitDays, staleDays: s.staleDays })
  open.value = true
}

async function submit() {
  saving.value = true
  try {
    await saveOrderSetting({
      addressCheck: form.addressCheck ? 1 : 0,
      trackEnabled: form.trackEnabled ? 1 : 0,
      // 空串不修改，空格清除
      trackKey: form.trackKey ? form.trackKey : clearKey.value ? ' ' : null,
      trackApiUrl: form.trackApiUrl || null,
      noScanHours: form.noScanHours,
      transitDays: form.transitDays,
      staleDays: form.staleDays
    })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.section-title {
  margin: 4px 0 8px;
  font-weight: 600;
}
.full {
  width: 100%;
}
.clear {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
}
</style>
