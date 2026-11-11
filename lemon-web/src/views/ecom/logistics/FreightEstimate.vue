<template>
  <a-card :bordered="false" class="page-card">
    <div class="estimate-head">
      <span class="section-title"><CalculatorOutlined /> {{ $t('ecom.logistics.estimateTitle') }}</span>
      <span class="sub-text">{{ $t('ecom.logistics.estimateTip') }}</span>
    </div>
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
    <a-table
      v-if="rows"
      row-key="channelId"
      size="small"
      class="estimate-table"
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      :locale="{ emptyText: $t('ecom.logistics.noChannel') }"
      :scroll="{ x: sumWidth(columns) }"
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
        </template>
        <template v-else-if="column.key === 'country'">
          {{ record.country === '*' ? $t('ecom.logistics.generalRate') : record.country }}
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CalculatorOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { estimateFreight } from '@/api/ecom/logistics'
import { sumWidth } from '@/composables/useColumns'
import { money } from '@/utils/number'
import { daysText } from './logistics'

/** 运费试算：目的国 + 重量 → 各启用渠道匹配到的费率（运费为本位币，按运费从低到高排列） */
const { t } = useI18n()

const form = reactive({ country: '', weight: 1 })
const loading = ref(false)
const rows = ref(null)

const columns = [
  { title: t('ecom.logistics.channel'), key: 'channel', width: 200 },
  { title: t('ecom.logistics.provider'), dataIndex: 'provider', width: 150 },
  { title: t('ecom.logistics.transportMode'), key: 'transportMode', width: 110 },
  { title: t('ecom.logistics.days'), key: 'days', width: 120 },
  { title: t('ecom.logistics.fee'), key: 'fee', width: 160 },
  { title: t('ecom.logistics.matchedCountry'), key: 'country', width: 120 }
]

async function run() {
  loading.value = true
  try {
    const res = await estimateFreight(form.country.trim().toUpperCase(), form.weight)
    rows.value = [...(res || [])].sort((a, b) => Number(a.fee) - Number(b.fee))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.estimate-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
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
