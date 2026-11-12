<template>
  <div>
    <a-alert type="info" show-icon class="tip" :message="$t('ecomDev.compare.tip')" />
    <a-form layout="vertical">
      <a-row :gutter="12">
        <a-col :xs="12" :md="4">
          <a-form-item :label="$t('ecomDev.calc.purchasePrice')" required>
            <a-input-number v-model:value="base.purchasePrice" :min="0" :precision="2" class="full" addon-after="CNY" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="4">
          <a-form-item :label="$t('ecomDev.calc.otherCost')">
            <a-input-number v-model:value="base.otherCost" :min="0" :precision="2" class="full" addon-after="CNY" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="4">
          <a-form-item :label="$t('ecomDev.calc.weightKg')">
            <a-input-number v-model:value="base.weightKg" :min="0" :precision="3" class="full" addon-after="kg" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="6">
          <a-form-item :label="$t('ecomDev.compare.size')">
            <a-input-group compact class="size">
              <a-input-number v-model:value="base.lengthCm" :min="0" :placeholder="$t('ecomDev.calc.lengthCm')" />
              <a-input-number v-model:value="base.widthCm" :min="0" :placeholder="$t('ecomDev.calc.widthCm')" />
              <a-input-number v-model:value="base.heightCm" :min="0" :placeholder="$t('ecomDev.calc.heightCm')" />
            </a-input-group>
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="3">
          <a-form-item :label="$t('ecomDev.compare.headPerKg')">
            <a-input-number v-model:value="base.headUnitPrice" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="3">
          <a-form-item :label="$t('ecomDev.calc.targetMargin')">
            <a-input-number v-model:value="base.targetMargin" :max="99" :precision="2" class="full" addon-after="%" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="4">
          <a-form-item :label="$t('ecomDev.calc.adRate')">
            <a-input-number v-model:value="base.adRate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="4">
          <a-form-item :label="$t('ecomDev.calc.returnRate')">
            <a-input-number v-model:value="base.returnRate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="4">
          <a-form-item :label="$t('ecomDev.calc.fbaAuto')">
            <a-switch v-model:checked="base.fbaAuto" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <div class="targets-head">
      <span class="section-title">{{ $t('ecomDev.compare.targets') }}</span>
      <a-space>
        <a-button size="small" @click="addTarget"><template #icon><PlusOutlined /></template>{{ $t('ecomDev.compare.addTarget') }}</a-button>
        <a-button size="small" type="primary" :loading="loading" @click="run"><template #icon><CalculatorOutlined /></template>{{ $t('ecomDev.compare.run') }}</a-button>
      </a-space>
    </div>
    <a-table row-key="key" size="small" :columns="targetColumns" :data-source="targets" :pagination="false" :scroll="{ x: 1500 }">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'platform'">
          <a-select v-model:value="record.platform" :options="toOptions(dict.ecom_platform)" class="full" />
        </template>
        <template v-else-if="column.key === 'marketplace'">
          <a-select v-model:value="record.marketplace" allow-clear show-search :options="toOptions(dict.ecom_marketplace)" class="full" />
        </template>
        <template v-else-if="column.key === 'salePrice'">
          <a-input-number v-model:value="record.salePrice" :min="0" :precision="2" class="full" :placeholder="$t('ecomDev.compare.byMargin')" />
        </template>
        <template v-else-if="column.key === 'fulfillmentFee'">
          <a-input-number v-model:value="record.fulfillmentFee" :min="0" :precision="2" class="full" :placeholder="$t('ecomDev.compare.feePh')" />
        </template>
        <template v-else-if="column.key === 'result'">
          <template v-if="record.result">
            <template v-if="record.result.profit != null">
              <span class="price">{{ record.result.currency }} {{ money(record.result.salePrice) }}</span>
              <span :class="['margin', marginClass(record.result.margin)]">{{ $t('ecomDev.compare.resultText', {
                profit: money(record.result.profit), margin: Number(record.result.margin), roi: Number(record.result.roi) }) }}</span>
              <div class="sub-text">
                {{ $t('ecomDev.compare.detailText', { commission: Number(record.result.commissionRate || 0), breakeven: record.result.breakevenPrice != null ? money(record.result.breakevenPrice) : '-' }) }}
                <span v-if="record.result.fbaTier || record.result.fbaTierName"> · {{ record.result.fbaTierName || $t(`ecomDev.calc.tier.${record.result.fbaTier}`) }}</span>
              </div>
            </template>
            <div v-for="(w, i) in record.result.warnings || []" :key="i" class="warn">{{ w }}</div>
          </template>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a class="danger" @click="targets.splice(index, 1)"><DeleteOutlined /></a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CalculatorOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { compareProfit } from '@/api/ecom/dev'
import { DevHeadPricing, EcomPlatform } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { marginClass } from '@/views/ecom/dev-project/dev'

/**
 * 多平台对比：同一产品（采购价、尺寸重量、头程每公斤单价、广告占比、退货率、目标利润率）在多个平台 / 站点按各自的佣金、币种、汇率、
 * 税率试算；填写售价时算利润，不填时按目标利润率反算建议售价
 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace')
const loading = ref(false)
let seq = 0

const base = reactive({ purchasePrice: 38, otherCost: 1.5, weightKg: 0.3, lengthCm: 15, widthCm: 10, heightCm: 4, headUnitPrice: 38, targetMargin: 20,
  adRate: 10, returnRate: 3, fbaAuto: true })
const target = (platform, marketplace, salePrice, fulfillmentFee) => ({ key: ++seq, platform, marketplace, salePrice, fulfillmentFee, result: null })
const targets = ref([
  target(EcomPlatform.AMAZON, 'US', 25.99),
  target(EcomPlatform.AMAZON, 'DE', undefined, 3.4),
  target(EcomPlatform.AMAZON, 'JP', undefined, 434),
  target(EcomPlatform.WALMART, 'US', undefined, 5.2),
  target(EcomPlatform.TIKTOK, 'US', undefined, 4.6)
])

const targetColumns = [
  { title: t('ecomDev.calc.platform'), key: 'platform', width: 150 },
  { title: t('ecomDev.calc.marketplace'), key: 'marketplace', width: 130 },
  { title: t('ecomDev.calc.salePrice'), key: 'salePrice', width: 140 },
  { title: t('ecomDev.calc.fulfillmentFee'), key: 'fulfillmentFee', width: 150 },
  { title: t('ecomDev.compare.result'), key: 'result' },
  { title: '', key: 'action', width: 50, align: 'center' }
]

function addTarget() {
  targets.value.push(target(EcomPlatform.AMAZON, undefined))
}

async function run() {
  if (base.purchasePrice == null) {
    message.warning(t('ecomDev.calc.needInput'))
    return
  }
  if (!targets.value.length) {
    message.warning(t('ecomDev.compare.noTarget'))
    return
  }
  const b = { platform: EcomPlatform.AMAZON, headPricing: DevHeadPricing.PER_KG, transportMode: 'AIR' }
  Object.entries(base).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') b[k] = v
  })
  loading.value = true
  try {
    const rows = await compareProfit({
      base: b,
      targets: targets.value.map((x) => ({
        platform: x.platform,
        marketplace: x.marketplace || null,
        salePrice: x.salePrice ?? null,
        fulfillmentFee: x.fulfillmentFee ?? null
      }))
    })
    rows.forEach((r, i) => (targets.value[i].result = r))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.size {
  display: flex !important;
}
.size :deep(.ant-input-number) {
  width: 33.3%;
}
.targets-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 8px;
}
.section-title {
  font-weight: 600;
}
.price {
  font-weight: 600;
  margin-right: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn {
  color: #d48806;
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
.margin.good {
  color: #389e0d;
}
.margin.low {
  color: #d48806;
}
.margin.loss {
  color: #cf1322;
}
</style>
