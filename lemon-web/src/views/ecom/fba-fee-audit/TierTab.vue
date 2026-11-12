<template>
  <div class="tab-body tier-tab">
    <a-alert type="info" show-icon :message="$t('ecomFba.tier.tip')" class="block" />
    <div class="filters">
      <a-select v-model:value="marketplace" allow-clear :placeholder="$t('ecomFba.tier.marketplace')" :options="toOptions(dict.ecom_marketplace)" class="w160" @change="load" />
      <a-button v-perm="'ecom:fbaFee:edit'" type="primary" @click="formRef.openCreate(marketplace)"><template #icon><PlusOutlined /></template>{{ $t('ecomFba.tier.add') }}</a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 1000 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sizeCategory'"><DictTag type="ecom_fba_size_category" :value="record.sizeCategory" /></template>
        <template v-else-if="column.key === 'unitSystem'">{{ $t(`ecomFba.unitSystem.${record.unitSystem}`) }}</template>
        <template v-else-if="column.key === 'limits'">
          {{ [record.maxLong, record.maxMedian, record.maxShort].map((v) => (v == null ? '∞' : n0(v))).join(' / ') }}
          <span v-if="record.maxGirth != null" class="sub-text">· {{ $t('ecomFba.tier.maxGirth') }} {{ n0(record.maxGirth) }}</span>
          <span v-if="record.maxSidesSum != null" class="sub-text">· {{ $t('ecomFba.tier.maxSidesSum') }} {{ n0(record.maxSidesSum) }}</span>
        </template>
        <template v-else-if="column.key === 'maxWeight'">{{ record.maxWeight == null ? '∞' : n0(record.maxWeight) }}</template>
        <template v-else-if="column.key === 'rates'">
          <a-popover :title="record.name">
            <template #content>
              <div v-for="r in record.rates" :key="r.id" class="rate-line">
                ({{ n0(r.weightFrom) }}, {{ r.weightTo == null ? '∞' : n0(r.weightTo) }}] → {{ r.fee }} {{ r.currency }}
                <span v-if="r.extraFrom != null"> + {{ r.extraFee }} / {{ n0(r.extraUnit) }} &gt; {{ n0(r.extraFrom) }}</span>
              </div>
            </template>
            <a>{{ $t('ecomFba.tier.rateCount', { n: (record.rates || []).length }) }}</a>
          </a-popover>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'edit', perm: 'ecom:fbaFee:edit', onClick: () => formRef.openEdit(record) },
              { type: 'delete', perm: 'ecom:fbaFee:edit', confirm: $t('ecomFba.tier.deleteConfirm', { code: record.code }), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>

    <a-card size="small" :title="$t('ecomFba.tier.quote')" class="quote-card">
      <a-space wrap>
        <a-select v-model:value="calc.marketplace" :options="toOptions(dict.ecom_marketplace)" class="w120" />
        <a-input-number v-model:value="calc.length" :min="0" :placeholder="$t('ecomFba.dim.length')" class="w140" />
        <a-input-number v-model:value="calc.width" :min="0" :placeholder="$t('ecomFba.dim.width')" class="w140" />
        <a-input-number v-model:value="calc.height" :min="0" :placeholder="$t('ecomFba.dim.height')" class="w140" />
        <a-input-number v-model:value="calc.weight" :min="0" :placeholder="$t('ecomFba.dim.weight')" class="w140" />
        <a-button :disabled="!calcReady" @click="doQuote">{{ $t('ecomFba.tier.quoteBtn') }}</a-button>
        <span v-if="quote !== undefined">
          <template v-if="quote">{{ $t('ecomFba.tier.quoteResult', { tier: quote.tierName, weight: quote.shippingWeight, unit: quote.weightUnit, fee: quote.fee, currency: quote.currency }) }}</template>
          <template v-else>{{ $t('ecomFba.tier.quoteNone') }}</template>
        </span>
      </a-space>
    </a-card>

    <TierForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import TierForm from './TierForm.vue'
import { deleteTier, listTiers, quoteFee } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { n0 } from '../fba-health/fba'

/** 尺寸分段与配送费费率（按站点），附配送费试算 */
const dict = useDict('ecom_marketplace')
const columns = [
  { title: t('ecomFba.tier.marketplace'), dataIndex: 'marketplace', width: 70 },
  { title: t('ecomFba.tier.sort'), dataIndex: 'sort', width: 70, align: 'right' },
  { title: t('ecomFba.tier.code'), dataIndex: 'code', width: 120 },
  { title: t('ecomFba.tier.name'), dataIndex: 'name', width: 160 },
  { title: t('ecomFba.tier.sizeCategory'), key: 'sizeCategory', width: 90 },
  { title: t('ecomFba.tier.unitSystem'), key: 'unitSystem', width: 100 },
  { title: t('ecomFba.tier.limits'), key: 'limits', width: 220 },
  { title: t('ecomFba.tier.maxWeight'), key: 'maxWeight', width: 90, align: 'right' },
  { title: t('ecomFba.tier.dimDivisor'), dataIndex: 'dimDivisor', width: 100, align: 'right' },
  { title: t('ecomFba.tier.rates'), key: 'rates', width: 90 },
  actionColumn(2)
]
const marketplace = ref('US')
const list = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    list.value = (await listTiers(marketplace.value || undefined)) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)
const formRef = ref()
const remove = useAction(deleteTier, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const calc = reactive({ marketplace: 'US', length: undefined, width: undefined, height: undefined, weight: undefined })
const calcReady = computed(() => calc.marketplace && calc.length > 0 && calc.width > 0 && calc.height > 0 && calc.weight > 0)
const quote = ref(undefined)
async function doQuote() {
  quote.value = (await quoteFee({ ...calc })) || null
}
</script>

<style scoped>
.tier-tab {
  overflow: auto;
}
.block {
  margin-bottom: 12px;
}
.filters {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.w120 {
  width: 120px;
}
.w140 {
  width: 140px;
}
.w160 {
  width: 160px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.rate-line {
  font-size: 12px;
}
.quote-card {
  margin-top: 12px;
}
</style>
