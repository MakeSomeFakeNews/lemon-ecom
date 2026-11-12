<template>
  <a-modal v-model:open="visible" :title="$t('ecomPublish.batch.title', { n: listings.length })" width="min(960px, 100vw)" :footer="null" destroy-on-close>
    <a-form layout="inline" class="form">
      <a-form-item :label="$t('ecomPublish.batch.changeType')">
        <a-radio-group v-model:value="form.changeType" :options="typeOptions" @change="reset" />
      </a-form-item>
      <a-form-item :label="$t('ecomPublish.batch.mode')">
        <a-select v-model:value="form.mode" :options="modeOptions" class="mode" @change="reset" />
      </a-form-item>
      <a-form-item v-if="form.mode !== PriceAdjustMode.FORMULA" :label="$t('ecomPublish.batch.value')">
        <a-input-number v-model:value="form.value" :precision="form.changeType === PriceChangeType.STOCK ? 0 : 2" class="value"
                        :addon-after="form.mode === PriceAdjustMode.PERCENT ? '%' : undefined" />
      </a-form-item>
      <a-form-item v-if="form.changeType === PriceChangeType.PRICE" :label="$t('ecomPublish.item.priceTemplate')">
        <a-select v-model:value="form.priceTemplateId" allow-clear :options="priceOptions" class="tpl" :placeholder="$t('ecomPublish.batch.templatePh')" />
      </a-form-item>
      <a-form-item>
        <a-checkbox v-model:checked="form.push">{{ $t('ecomPublish.batch.push') }}</a-checkbox>
      </a-form-item>
      <a-form-item v-if="form.changeType === PriceChangeType.STOCK">
        <a-checkbox v-model:checked="form.lockStock">{{ $t('ecomPublish.batch.lockStock') }}</a-checkbox>
      </a-form-item>
    </a-form>
    <div class="sub-text hint">{{ $t(`ecomPublish.batch.modeTip.${form.mode}`) }}</div>
    <a-alert v-if="form.changeType === PriceChangeType.STOCK" type="warning" show-icon class="hint"
             :message="form.lockStock ? $t('ecomPublish.batch.lockTip') : $t('ecomPublish.batch.noLockTip')" />
    <div class="actions">
      <a-button :loading="previewing" @click="preview">{{ $t('ecomPublish.batch.preview') }}</a-button>
      <a-button v-perm="'ecom:priceChange:add'" type="primary" :loading="applying" :disabled="!rows.length || applied" @click="apply">{{ $t('ecomPublish.batch.apply') }}</a-button>
      <span v-if="summary" class="summary">{{ summary }}</span>
    </div>
    <a-table row-key="listingId" size="small" :columns="columns" :data-source="rows" :pagination="{ pageSize: 10 }" :scroll="{ x: 860 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sku'">
          <div>{{ record.sellerSku }}</div>
          <div class="sub-text">{{ record.storeName }}</div>
        </template>
        <template v-else-if="column.key === 'change'">
          {{ fmt(record.oldValue) }}
          <template v-if="record.newValue != null"> → <b :class="Number(record.newValue) > Number(record.oldValue) ? 'up' : 'down'">{{ fmt(record.newValue) }}</b></template>
          <span v-if="form.changeType === PriceChangeType.PRICE" class="sub-text"> {{ record.currency }}</span>
        </template>
        <template v-else-if="column.key === 'margin'">
          <span v-if="record.estMargin != null" :class="record.estMargin < 0 ? 'up' : ''">{{ record.estMargin }}%</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag v-if="record.status" type="ecom_price_change_status" :value="record.status" />
          <span v-else class="sub-text">{{ $t('ecomPublish.batch.pending') }}</span>
          <div v-if="record.message" class="sub-text msg">{{ record.message }}</div>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { applyPriceChange, previewPriceChange, priceTemplateOptions } from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { PriceAdjustMode, PriceChangeType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { price, qty } from '@/utils/number'

/**
 * 批量改价 / 改库存（在线商品多选后打开）：价格按固定值、百分比 / 金额加减或价格模板公式，库存按固定值或加减；先预览新值与预估利润率，
 * 执行后逐条显示推送结果（已推送 / 推送失败 / 仅本地），全部写入改价记录。open(listings)，执行后 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_price_change_type', 'ecom_price_adjust_mode', 'ecom_price_change_status')
const visible = ref(false)
const listings = ref([])
const priceTemplates = ref([])
const form = ref({})
const rows = ref([])
const summary = ref('')
const applied = ref(false)
const previewing = ref(false)
const applying = ref(false)

const typeOptions = computed(() => toOptions(dict.ecom_price_change_type))
const modeOptions = computed(() => toOptions(dict.ecom_price_adjust_mode).filter((o) => form.value.changeType === PriceChangeType.PRICE || o.value !== PriceAdjustMode.FORMULA))
const priceOptions = computed(() => priceTemplates.value.map((x) => ({ value: x.id, label: x.name })))
const columns = [
  { title: t('ecomPublish.common.sellerSku'), key: 'sku', width: 200 },
  { title: t('ecomPublish.batch.change'), key: 'change', width: 200 },
  { title: t('ecomPublish.item.margin'), key: 'margin', width: 110, align: 'right' },
  { title: t('ecomPublish.change.result'), key: 'status', width: 300 }
]
const fmt = (v) => (v == null ? '-' : form.value.changeType === PriceChangeType.STOCK ? qty(v, 0) : price(v))

async function open(list) {
  listings.value = list
  form.value = { changeType: PriceChangeType.PRICE, mode: PriceAdjustMode.PERCENT, value: undefined, priceTemplateId: undefined, push: true, lockStock: true }
  reset()
  visible.value = true
  priceTemplates.value = (await priceTemplateOptions()) || []
}
function reset() {
  rows.value = []
  summary.value = ''
  applied.value = false
}
const payload = () => ({ ...form.value, listingIds: listings.value.map((l) => l.id) })

async function preview() {
  previewing.value = true
  try {
    const r = await previewPriceChange(payload())
    rows.value = r.rows
    applied.value = false
    summary.value = r.failed ? t('ecomPublish.batch.invalidN', { n: r.failed }) : ''
  } finally {
    previewing.value = false
  }
}
async function apply() {
  applying.value = true
  try {
    const r = await applyPriceChange(payload())
    rows.value = r.rows
    applied.value = true
    summary.value = t('ecomPublish.batch.result', { ok: r.succeeded, failed: r.failed, local: r.local, batch: r.batchNo })
    emit('success')
  } finally {
    applying.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.form {
  row-gap: 8px;
}
.mode {
  width: 140px;
}
.value {
  width: 140px;
}
.tpl {
  width: 200px;
}
.hint {
  margin: 8px 0;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.summary {
  color: var(--app-text-2, #595959);
}
.up {
  color: #cf1322;
}
.down {
  color: #389e0d;
}
.msg {
  white-space: normal;
  word-break: break-word;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
