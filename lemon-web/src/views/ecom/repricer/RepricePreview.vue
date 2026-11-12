<template>
  <a-modal v-model:open="visible" :title="title" :footer="null" width="min(1100px, 100vw)">
    <a-spin :spinning="loading">
      <a-alert v-if="result" type="info" show-icon class="block" :message="result.message" />
      <a-table row-key="listingId" size="small" :columns="columns" :data-source="result?.rows || []" :pagination="{ pageSize: 20 }" :scroll="{ x: 1000 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div>{{ record.sellerSku }}</div>
            <div class="sub-text">{{ record.storeName }}</div>
          </template>
          <template v-else-if="column.key === 'change'">
            {{ price(record.oldPrice) }}
            <template v-if="record.newPrice != null"> → <b :class="Number(record.newPrice) > Number(record.oldPrice) ? 'up' : 'down'">{{ price(record.newPrice) }}</b></template>
            <span class="sub-text"> {{ record.currency }}</span>
          </template>
          <template v-else-if="column.key === 'market'">
            <div>{{ $t('ecomPublish.repricer.lowest') }} {{ record.lowestPrice != null ? price(record.lowestPrice) : '-' }}</div>
            <div class="sub-text">{{ $t('ecomPublish.repricer.buybox') }} {{ record.buyboxPrice != null ? price(record.buyboxPrice) : '-' }} · {{ $t('ecomPublish.repricer.floor') }} {{ record.floorPrice != null ? price(record.floorPrice) : '-' }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag v-if="record.status" type="ecom_reprice_log_status" :value="record.status" />
            <span v-else class="sub-text">{{ record.newPrice != null ? $t('ecomPublish.repricer.willChange') : $t('ecomPublish.repricer.unchanged') }}</span>
            <div v-if="record.message" class="sub-text">{{ record.message }}</div>
          </template>
        </template>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { previewRepriceRule, runRepriceRule } from '@/api/ecom/publish'
import { price } from '@/utils/number'

/** 规则预览（只计算，不拉取竞品价格、不保存）与立即执行的结果。preview(rule) / run(rule)，执行后 done */
const emit = defineEmits(['done'])
const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const title = ref('')
const result = ref(null)
const columns = [
  { title: t('ecomPublish.common.sellerSku'), key: 'sku', width: 180 },
  { title: t('ecomPublish.repricer.priceChange'), key: 'change', width: 180 },
  { title: t('ecomPublish.repricer.market'), key: 'market', width: 220 },
  { title: t('ecomPublish.repricer.reason'), dataIndex: 'reason', width: 280 },
  { title: t('common.field.status'), key: 'status', width: 160 }
]

async function show(rule, fn, key) {
  visible.value = true
  title.value = t(key, { name: rule.name })
  result.value = null
  loading.value = true
  try {
    result.value = await fn(rule.id)
  } finally {
    loading.value = false
  }
}
const preview = (rule) => show(rule, previewRepriceRule, 'ecomPublish.repricer.previewTitle')
function run(rule) {
  Modal.confirm({
    title: t('ecomPublish.repricer.runConfirm', { name: rule.name }),
    content: rule.suggestOnly ? t('ecomPublish.repricer.runSuggestTip') : t('ecomPublish.repricer.runPushTip'),
    onOk: async () => {
      await show(rule, runRepriceRule, 'ecomPublish.repricer.runTitle')
      emit('done')
    }
  })
}

defineExpose({ preview, run })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.up {
  color: #cf1322;
}
.down {
  color: #389e0d;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
