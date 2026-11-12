<template>
  <a-modal :open="open" :title="$t('ecomCatalog.box.title', { no: inbound?.docNo || '' })" width="min(1100px, 100vw)" :confirm-loading="saving"
           @ok="save" @cancel="open = false">
    <a-alert type="info" show-icon class="block" :message="$t('ecomCatalog.box.hint')" />
    <div class="toolbar block">
      <a-space wrap>
        <a-button size="small" @click="addBox()"><template #icon><PlusOutlined /></template>{{ $t('ecomCatalog.box.add') }}</a-button>
        <a-button size="small" :disabled="!boxes.length" @click="addBox(boxes[boxes.length - 1])"><template #icon><CopyOutlined /></template>{{ $t('ecomCatalog.box.copyLast') }}</a-button>
        <span class="sub-text">{{ $t('ecomCatalog.box.batch') }}</span>
        <a-input-number v-model:value="batch.count" :min="1" :max="200" :precision="0" size="small" :addon-before="$t('ecomCatalog.box.boxes')" />
        <a-select v-model:value="batch.sku" size="small" class="w180" :options="skus.map((s) => ({ value: s.sellerSku, label: s.sellerSku }))"
                  :placeholder="$t('ecom.common.sellerSku')" />
        <a-input-number v-model:value="batch.perBox" :min="1" :precision="0" size="small" :addon-before="$t('ecomCatalog.box.perBox')" />
        <a-button size="small" :disabled="!batch.sku || !batch.perBox" @click="addBatch">{{ $t('ecomCatalog.box.addBatch') }}</a-button>
      </a-space>
    </div>
    <a-table row-key="key" size="small" :columns="columns" :data-source="boxes" :pagination="false" :scroll="{ x: 560 + skus.length * 120, y: 380 }">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'no'">{{ index + 1 }}</template>
        <template v-else-if="['length', 'width', 'height'].includes(column.key)">
          <a-input-number v-model:value="record[column.key]" :min="0" :precision="2" size="small" class="full" />
        </template>
        <template v-else-if="column.key === 'weight'">
          <a-input-number v-model:value="record.weight" :min="0" :precision="3" size="small" class="full" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="boxes.splice(index, 1)"><template #icon><DeleteOutlined /></template></a-button>
        </template>
        <template v-else-if="column.sku">
          <a-input-number v-model:value="record.qty[column.sku]" :min="0" :precision="0" size="small" class="full" />
        </template>
      </template>
      <template #summary>
        <a-table-summary-row>
          <a-table-summary-cell :index="0" :col-span="6">{{ $t('ecomCatalog.box.packed') }}</a-table-summary-cell>
          <a-table-summary-cell v-for="(s, i) in skus" :key="s.sellerSku" :index="i + 6">
            <span :class="{ over: packed(s.sellerSku) > Number(s.qty), done: packed(s.sellerSku) === Number(s.qty) }">{{ packed(s.sellerSku) }} / {{ qty(s.qty) }}</span>
          </a-table-summary-cell>
          <a-table-summary-cell :index="skus.length + 6" />
        </a-table-summary-row>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { CopyOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { saveEcomInboundBoxes } from '@/api/ecom/inbound'
import { qty } from '@/utils/number'

/**
 * 头程货件装箱：每行一箱（长宽高 cm、毛重 kg）和箱内各平台 SKU 的件数，底部合计对照货件件数；可复制上一箱、按「每箱件数」批量加箱。
 * 保存后后端按装箱更新箱数、毛重、体积。ref 调用 open(inbound)，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const inbound = ref()
const skus = ref([])
const boxes = ref([])
const batch = reactive({ count: 1, sku: undefined, perBox: undefined })
let seq = 0

const columns = computed(() => [
  { title: t('ecomCatalog.box.boxNo'), key: 'no', width: 60, fixed: 'left' },
  { title: t('ecomCatalog.box.length'), key: 'length', width: 100 },
  { title: t('ecomCatalog.box.width'), key: 'width', width: 100 },
  { title: t('ecomCatalog.box.height'), key: 'height', width: 100 },
  { title: t('ecomCatalog.box.weight'), key: 'weight', width: 110 },
  { title: '', key: 'gap', width: 1 },
  ...skus.value.map((s) => ({ title: s.sellerSku, key: `sku-${s.sellerSku}`, sku: s.sellerSku, width: 120 })),
  { title: '', key: 'action', width: 50, fixed: 'right' }
])

const packed = (sku) => boxes.value.reduce((sum, b) => sum + (Number(b.qty[sku]) || 0), 0)

function addBox(from) {
  boxes.value.push({ key: ++seq, length: from?.length, width: from?.width, height: from?.height, weight: from?.weight, remark: '', qty: { ...(from?.qty || {}) } })
}
/** 按每箱件数批量加箱：最多加到该 SKU 未装箱的件数 */
function addBatch() {
  const sku = skus.value.find((s) => s.sellerSku === batch.sku)
  let left = Number(sku.qty) - packed(sku.sellerSku)
  for (let i = 0; i < batch.count && left > 0; i++) {
    const n = Math.min(batch.perBox, left)
    boxes.value.push({ key: ++seq, length: undefined, width: undefined, height: undefined, weight: undefined, remark: '', qty: { [sku.sellerSku]: n } })
    left -= n
  }
}

function show(d) {
  inbound.value = d
  skus.value = d.skuLines || []
  boxes.value = (d.boxes || []).map((b) => ({
    key: ++seq,
    length: b.length != null ? Number(b.length) : undefined,
    width: b.width != null ? Number(b.width) : undefined,
    height: b.height != null ? Number(b.height) : undefined,
    weight: b.weight != null ? Number(b.weight) : undefined,
    remark: b.remark,
    qty: Object.fromEntries((b.items || []).map((i) => [i.sellerSku, Number(i.qty)]))
  }))
  if (!boxes.value.length) addBox()
  open.value = true
}

async function save() {
  const over = skus.value.find((s) => packed(s.sellerSku) > Number(s.qty))
  if (over) {
    message.error(t('ecomCatalog.box.exceeds', { sku: over.sellerSku }))
    return
  }
  const body = boxes.value.map((b) => ({
    length: b.length ?? null,
    width: b.width ?? null,
    height: b.height ?? null,
    weight: b.weight ?? null,
    remark: b.remark || null,
    items: Object.entries(b.qty).filter(([, v]) => Number(v) > 0).map(([sellerSku, v]) => ({ sellerSku, qty: v }))
  })).filter((b) => b.items.length)
  saving.value = true
  try {
    await saveEcomInboundBoxes(inbound.value.id, body)
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('success')
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
.w180 {
  width: 180px;
}
.full {
  width: 100%;
}
.over {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 600;
}
.done {
  color: var(--ant-color-success, #52c41a);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
