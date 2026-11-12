<template>
  <a-modal :open="open" :title="$t('ecomCatalog.generate.title', { no: plan?.planNo || '' })" width="min(1000px, 100vw)" :confirm-loading="saving"
           :ok-text="$t('ecomCatalog.generate.ok', { n: shipments.length })" @ok="submit" @cancel="open = false">
    <a-alert type="info" show-icon class="block" :message="$t('ecomCatalog.generate.hint')" />
    <div class="toolbar block">
      <a-space wrap>
        <a-button size="small" :disabled="shipments.length >= 20" @click="addShipment"><template #icon><PlusOutlined /></template>{{ $t('ecomCatalog.generate.addShipment') }}</a-button>
        <a-button size="small" @click="fillRemaining">{{ $t('ecomCatalog.generate.fillFirst') }}</a-button>
      </a-space>
    </div>
    <div class="shipments block">
      <div v-for="(sh, si) in shipments" :key="sh.key" class="shipment">
        <span class="sub-text">{{ $t('ecomCatalog.generate.shipmentN', { n: si + 1 }) }}</span>
        <a-input v-model:value="sh.shipmentId" :maxlength="64" :placeholder="$t('ecomCatalog.generate.shipmentIdPh')" size="small" class="w160" />
        <ChannelSelect v-model:value="sh.channelId" size="small" class="w200" />
        <a-button v-if="shipments.length > 1" type="link" danger size="small" @click="removeShipment(si)"><template #icon><DeleteOutlined /></template></a-button>
      </div>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="items" :pagination="false" :scroll="{ x: 420 + shipments.length * 130, y: 360 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sku'">
          <div>{{ record.sellerSku }}</div>
          <div class="sub-text">{{ record.title }}</div>
        </template>
        <template v-else-if="column.key === 'remaining'">
          <span :class="{ over: leftOf(record) < 0 }">{{ qty(leftOf(record)) }}</span> / {{ qty(remainingOf(record)) }}
        </template>
        <template v-else-if="column.key.startsWith('s')">
          <a-input-number v-model:value="alloc[record.id][Number(column.key.slice(1))]" :min="0" :precision="4" size="small" class="full" />
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import { generateShipmentPlan } from '@/api/ecom/shipmentPlan'
import { qty } from '@/utils/number'
import { remainingOf } from './plan'

/**
 * 生成头程货件：按货件分列填写各平台 SKU 的件数（可拆成多个货件，最多 20 个），每个货件可填平台货件号、物流渠道；
 * 每行各货件合计不能超过未生成的件数。ref 调用 open(plan)，成功后触发 success(货件 ID 列表)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const plan = ref()
const items = ref([])
const shipments = ref([])
const alloc = reactive({})
let seq = 0

const columns = computed(() => [
  { title: t('ecom.common.sellerSku'), key: 'sku', width: 240, fixed: 'left' },
  { title: t('ecomCatalog.generate.remaining'), key: 'remaining', width: 150, align: 'right' },
  ...shipments.value.map((_, i) => ({ title: t('ecomCatalog.generate.shipmentN', { n: i + 1 }), key: `s${i}`, width: 130 }))
])

const leftOf = (r) => remainingOf(r) - (alloc[r.id] || []).reduce((s, v) => s + (Number(v) || 0), 0)

function addShipment() {
  shipments.value.push({ key: ++seq, shipmentId: '', channelId: undefined })
  items.value.forEach((i) => alloc[i.id].push(0))
}
function removeShipment(idx) {
  shipments.value.splice(idx, 1)
  items.value.forEach((i) => alloc[i.id].splice(idx, 1))
}
/** 把剩余件数都放到第一个货件 */
function fillRemaining() {
  items.value.forEach((i) => {
    const others = alloc[i.id].slice(1).reduce((s, v) => s + (Number(v) || 0), 0)
    alloc[i.id][0] = Math.max(0, remainingOf(i) - others)
  })
}

function show(p) {
  plan.value = p
  items.value = (p.items || []).filter((i) => remainingOf(i) > 0)
  shipments.value = [{ key: ++seq, shipmentId: '', channelId: undefined }]
  Object.keys(alloc).forEach((k) => delete alloc[k])
  items.value.forEach((i) => (alloc[i.id] = [remainingOf(i)]))
  open.value = true
}

async function submit() {
  if (items.value.some((i) => leftOf(i) < -1e-9)) {
    message.error(t('ecomCatalog.generate.exceeds'))
    return
  }
  const body = shipments.value.map((sh, si) => ({
    shipmentId: sh.shipmentId || null,
    channelId: sh.channelId ?? null,
    lines: items.value.filter((i) => Number(alloc[i.id][si]) > 0).map((i) => ({ planItemId: i.id, qty: alloc[i.id][si] }))
  })).filter((s) => s.lines.length)
  if (!body.length) {
    message.error(t('ecomCatalog.generate.empty'))
    return
  }
  saving.value = true
  try {
    const ids = await generateShipmentPlan(plan.value.id, { shipments: body })
    message.success(t('ecomCatalog.generate.done', { n: ids.length }))
    open.value = false
    emit('success', ids)
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.shipments {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.shipment {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.w160 {
  width: 160px;
}
.w200 {
  width: 200px;
}
.full {
  width: 100%;
}
.over {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
