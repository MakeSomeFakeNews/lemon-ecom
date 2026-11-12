<template>
  <a-modal v-model:open="open" :title="$t('ecomOrder.ops.splitTitle', { no: order?.platformOrderNo || '' })" width="min(960px, 100vw)" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon class="tip" :message="$t('ecomOrder.ops.splitHint')" />
    <div class="toolbar">
      <a-button size="small" @click="addPackage"><template #icon><PlusOutlined /></template>{{ $t('ecomOrder.ops.splitAddPackage') }}</a-button>
      <a-button size="small" :disabled="count <= 1" @click="removePackage"><template #icon><MinusOutlined /></template>{{ $t('ecomOrder.ops.splitRemovePackage') }}</a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="lines" :pagination="false" :scroll="{ x: 420 + count * 130 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sku'">
          <div>{{ record.sellerSku }}</div>
          <div class="sub-text">{{ record.materialCode }} {{ record.materialName }}</div>
        </template>
        <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
        <template v-else-if="column.key === 'available'">{{ qty(record.available) }}</template>
        <template v-else-if="column.key.startsWith('p')">
          <a-input-number v-model:value="record.alloc[Number(column.key.slice(1))]" :min="0" :max="record.available" :precision="0" :disabled="!record.available" class="full" />
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { MinusOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { getEcomOrder, splitEcomOrder } from '@/api/ecom/order'
import { qty } from '@/utils/number'

/**
 * 拆分订单为多个包裹：每列一个包裹，填写每行放进该包裹的件数；可分配 = 件数 − 已打单 / 已发货的件数（待打单的包裹会作废重新分配）。
 * ref 调用 open(order)，完成后触发 done
 */
const emit = defineEmits(['done'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const order = ref()
const lines = ref([])
const count = ref(2)

const columns = computed(() => [
  { title: t('ecomOrder.order.itemSku'), key: 'sku', width: 220, fixed: 'left' },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 80, align: 'right' },
  { title: t('ecomOrder.ops.splitAvailable'), key: 'available', width: 90, align: 'right' },
  ...Array.from({ length: count.value }, (_, i) => ({ title: t('ecomOrder.ops.splitPackage', { n: i + 1 }), key: `p${i}`, width: 130 }))
])

async function show(record) {
  const d = await getEcomOrder(record.id)
  order.value = d
  count.value = 2
  lines.value = (d.items || []).map((it) => {
    const available = Math.max(0, Number(it.qty) - Number(it.lockedQty || 0))
    return { ...it, available, alloc: [available, 0] }
  })
  open.value = true
}

function addPackage() {
  count.value += 1
  lines.value.forEach((l) => l.alloc.push(0))
}

function removePackage() {
  count.value -= 1
  lines.value.forEach((l) => l.alloc.pop())
}

async function submit() {
  for (const l of lines.value) {
    const sum = l.alloc.reduce((a, b) => a + (Number(b) || 0), 0)
    if (sum > l.available) {
      message.warning(t('ecomOrder.ops.splitOver', { sku: l.sellerSku, n: l.available }))
      return
    }
  }
  const packages = Array.from({ length: count.value }, (_, i) =>
    lines.value.filter((l) => Number(l.alloc[i]) > 0).map((l) => ({ orderItemId: l.id, qty: Number(l.alloc[i]) }))
  ).filter((p) => p.length)
  if (!packages.length) {
    message.warning(t('ecomOrder.ops.splitEmpty'))
    return
  }
  saving.value = true
  try {
    const created = await splitEcomOrder(order.value.id, packages)
    message.success(t('ecomOrder.ops.splitDone', { n: created?.length || packages.length }))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
