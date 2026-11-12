<template>
  <a-modal :open="open" :footer="null" width="min(900px, 100vw)" @cancel="open = false">
    <template #title><CalculatorOutlined /> {{ $t('ecom.logistics.estimateTitle') }}</template>
    <div class="sub-text tip">{{ $t('ecom.logistics.estimateTip') }}</div>
    <a-form layout="inline" :model="form" class="estimate-form" @finish="run">
      <a-form-item :label="$t('ecom.logistics.country')" name="country" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('ecom.logistics.country') }) }]">
        <a-input v-model:value="form.country" :maxlength="8" :placeholder="$t('ecom.logistics.countryPh')" class="country" @change="form.country = form.country.toUpperCase()" />
      </a-form-item>
      <a-form-item :label="$t('ecom.logistics.weight')" name="weight" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('ecom.logistics.weight') }) }]">
        <a-input-number v-model:value="form.weight" :min="0.001" :precision="3" addon-after="kg" class="weight" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">{{ $t('ecom.logistics.estimate') }}</a-button>
      </a-form-item>
    </a-form>
    <a-empty v-if="!rows" :image="simpleImage" :description="$t('ecom.logistics.estimateEmpty')" class="placeholder" />
    <a-table
      v-else
      row-key="channelId"
      size="small"
      class="estimate-table"
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      :locale="{ emptyText: $t('ecom.logistics.noChannel') }"
      :scroll="{ x: sumWidth(columns), y: 420 }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'channel'">
          <div>{{ record.channelName }}</div>
          <div class="sub-text">{{ record.channelCode }}</div>
        </template>
        <template v-else-if="column.key === 'transportMode'">
          <DictTag type="ecom_transport_mode" :value="record.transportMode" />
        </template>
        <template v-else-if="column.key === 'days'">{{ daysText(record) }}</template>
        <template v-else-if="column.key === 'fee'">
          <b :class="{ cheapest: index === 0 }">{{ money(record.fee) }}</b>
          <a-tag v-if="index === 0 && rows.length > 1" color="success" :bordered="false" class="cheapest-tag">{{ $t('ecom.logistics.cheapest') }}</a-tag>
          <a-tag v-if="record.channelId === fastestId && rows.length > 1" color="processing" :bordered="false" class="cheapest-tag">{{ $t('ecom.logistics.fastest') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'country'">
          {{ record.country === '*' ? $t('ecom.logistics.generalRate') : record.country }}
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { Empty } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CalculatorOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { estimateFreight } from '@/api/ecom/logistics'
import { sumWidth } from '@/composables/useColumns'
import { money } from '@/utils/number'
import { daysText } from './logistics'

/** 运费试算：目的国 + 重量 → 各启用渠道匹配到的费率（运费为本位币，按运费从低到高排列） */
const { t } = useI18n()

const open = ref(false)
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const form = reactive({ country: '', weight: 1 })
const loading = ref(false)
const rows = ref(null)

const columns = [
  { title: t('ecom.logistics.channel'), key: 'channel', width: 180 },
  { title: t('ecom.logistics.provider'), dataIndex: 'provider', width: 130, ellipsis: true },
  { title: t('ecom.logistics.transportMode'), key: 'transportMode', width: 110 },
  { title: t('ecom.logistics.days'), key: 'days', width: 120 },
  { title: t('ecom.logistics.fee'), key: 'fee', width: 170, align: 'right' },
  { title: t('ecom.logistics.matchedCountry'), key: 'country', width: 120 }
]

/** 时效最短的渠道（按最短天数，其次最长天数） */
const fastestId = computed(() => {
  const list = (rows.value || []).filter((r) => r.daysMin != null || r.daysMax != null)
  if (!list.length) return undefined
  const key = (r) => (r.daysMin ?? r.daysMax) * 1000 + (r.daysMax ?? r.daysMin)
  return list.reduce((a, b) => (key(b) < key(a) ? b : a)).channelId
})

async function run() {
  loading.value = true
  try {
    const res = await estimateFreight(form.country.trim().toUpperCase(), form.weight)
    rows.value = [...(res || [])].sort((a, b) => Number(a.fee) - Number(b.fee))
  } finally {
    loading.value = false
  }
}
defineExpose({ open: () => (open.value = true) })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.placeholder {
  margin: 24px 0 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.estimate-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}
.country {
  width: 120px;
}
.weight {
  width: 160px;
}
.estimate-table {
  margin-top: 8px;
}
.cheapest {
  color: var(--ant-color-success, #52c41a);
}
.cheapest-tag {
  margin-left: 6px;
}
</style>
