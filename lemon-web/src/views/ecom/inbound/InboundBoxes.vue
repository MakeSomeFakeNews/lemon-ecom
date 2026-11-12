<template>
  <div>
    <div class="section-head">
      <span class="sub-text">{{ $t('ecomCatalog.box.hintShort') }}</span>
      <a-space wrap>
        <a-button v-if="editable && userStore.hasPerm('ecom:inbound:box')" size="small" type="primary" ghost @click="boxRef.open(detail)">
          <template #icon><InboxOutlined /></template>{{ $t('ecomCatalog.box.edit') }}
        </a-button>
        <a-button size="small" :disabled="!boxes.length" @click="printPackingList(detail)">
          <template #icon><PrinterOutlined /></template>{{ $t('ecomCatalog.box.printPackingList') }}
        </a-button>
        <a-button size="small" :disabled="!boxes.length" @click="printCartonLabels(detail)">
          <template #icon><PrinterOutlined /></template>{{ $t('ecomCatalog.box.printCartonLabels') }}
        </a-button>
        <a-button size="small" :disabled="!skuLines.length" @click="fnskuOpen = true">
          <template #icon><BarcodeOutlined /></template>{{ $t('ecomCatalog.fnsku.print') }}
        </a-button>
      </a-space>
    </div>

    <div class="pack-summary">
      <div class="pack-progress">
        <span class="sub-text">{{ $t('ecomCatalog.box.packedProgress') }}</span>
        <a-progress :percent="packPercent" :status="packPercent >= 100 ? 'success' : 'active'" size="small" class="bar" />
        <span class="nums">{{ qty(packedTotal) }} / {{ qty(skuTotal) }}</span>
      </div>
      <div class="pack-stats">
        <span>{{ $t('ecomCatalog.box.boxes') }} <b>{{ boxes.length }}</b></span>
        <span>{{ $t('ecomCatalog.box.totalWeight') }} <b>{{ qty(weightTotal, 3) }}</b> kg</span>
        <span>{{ $t('ecomCatalog.box.totalVolume') }} <b>{{ qty(volumeTotal, 4) }}</b> m³</span>
      </div>
    </div>

    <a-table row-key="sellerSku" size="small" :columns="columns" :data-source="skuLines" :pagination="false" :scroll="{ x: 760 }" class="block">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">{{ record.title || record.materialName || '-' }}</template>
        <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
        <template v-else-if="column.key === 'boxedQty'">
          <span :class="{ done: Number(record.boxedQty) === Number(record.qty), warn: Number(record.boxedQty) < Number(record.qty) }">{{ qty(record.boxedQty) }}</span>
        </template>
      </template>
    </a-table>

    <template v-if="boxes.length">
      <div class="sub-title">{{ $t('ecomCatalog.box.summary', { n: boxes.length }) }}</div>
      <a-table row-key="boxNo" size="small" :columns="boxColumns" :data-source="boxes" :pagination="boxes.length > 10 ? { pageSize: 10, size: 'small' } : false"
               :scroll="{ x: 760 }" class="block">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'boxNo'">#{{ record.boxNo }}</template>
          <template v-else-if="column.key === 'size'">{{ boxSize(record) || '-' }}</template>
          <template v-else-if="column.key === 'weight'">{{ record.weight != null ? `${qty(record.weight, 3)} kg` : '-' }}</template>
          <template v-else-if="column.key === 'items'">
            <a-tag v-for="i in record.items || []" :key="i.sellerSku" class="sku-tag">{{ i.sellerSku }} × {{ qty(i.qty) }}</a-tag>
          </template>
        </template>
      </a-table>
    </template>
    <a-empty v-else :image="simpleImage" :description="$t('ecomCatalog.box.empty')" class="block" />
    <InboundBoxModal ref="boxRef" @success="emit('changed')" />
    <FnskuLabelModal v-model:open="fnskuOpen" :items="fnskuItems" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Empty } from 'ant-design-vue'
import { BarcodeOutlined, InboxOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import FnskuLabelModal from '@/components/ecom/FnskuLabelModal.vue'
import InboundBoxModal from './InboundBoxModal.vue'
import { printCartonLabels, printPackingList } from './inboundPrint'
import { useUserStore } from '@/store/user'
import { InboundStatus } from '@/constants/enums'
import { qty } from '@/utils/number'

/** 头程货件详情中的装箱：按平台 SKU 的件数与已装箱件数，装箱、打印装箱单 / 箱唛 / FNSKU 标签（每件一张）。装箱保存后触发 changed */
const props = defineProps({ detail: { type: Object, required: true } })
const emit = defineEmits(['changed'])
const { t } = useI18n()
const userStore = useUserStore()

const columns = [
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 170 },
  { title: 'FNSKU', dataIndex: 'fnsku', width: 130 },
  { title: t('ecomCatalog.common.title'), key: 'title', ellipsis: true },
  { title: t('ecomCatalog.box.skuQty'), key: 'qty', width: 110, align: 'right' },
  { title: t('ecomCatalog.box.boxedQty'), key: 'boxedQty', width: 110, align: 'right' }
]
const skuLines = computed(() => props.detail.skuLines || [])
const boxes = computed(() => props.detail.boxes || [])
/** 签收、取消后不能再改装箱 */
const editable = computed(() => [InboundStatus.DRAFT, InboundStatus.APPROVING, InboundStatus.SHIPPED].includes(props.detail.status))
const boxSize = (b) => ([b.length, b.width, b.height].every((v) => v != null) ? `${Number(b.length)}×${Number(b.width)}×${Number(b.height)}cm` : '')

const boxColumns = [
  { title: t('ecomCatalog.box.boxNo'), key: 'boxNo', width: 80 },
  { title: t('ecomCatalog.box.size'), key: 'size', width: 160 },
  { title: t('ecomCatalog.box.weight'), key: 'weight', width: 110, align: 'right' },
  { title: t('ecomCatalog.box.content'), key: 'items' }
]
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const skuTotal = computed(() => skuLines.value.reduce((a, s) => a + (Number(s.qty) || 0), 0))
const packedTotal = computed(() => skuLines.value.reduce((a, s) => a + (Number(s.boxedQty) || 0), 0))
const packPercent = computed(() => (skuTotal.value ? Math.min(100, Math.round((packedTotal.value / skuTotal.value) * 100)) : 0))
const weightTotal = computed(() => boxes.value.reduce((a, b) => a + (Number(b.weight) || 0), 0))
/** 体积 m³ = 长 × 宽 × 高（cm）÷ 1,000,000 */
const volumeTotal = computed(() =>
  boxes.value.reduce((a, b) => a + ([b.length, b.width, b.height].every((v) => v != null) ? (b.length * b.width * b.height) / 1e6 : 0), 0)
)

const boxRef = ref()
const fnskuOpen = ref(false)
const fnskuItems = computed(() => skuLines.value.map((s) => ({ fnsku: s.fnsku, sellerSku: s.sellerSku, title: s.title || s.materialName, qty: s.qty })))
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 0 0 10px;
}
.sub-title {
  margin: 4px 0 8px;
  font-weight: 500;
}
.pack-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 24px;
  margin-bottom: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-bg);
}
.pack-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 260px;
}
.pack-progress .bar {
  flex: 1;
  margin: 0;
}
.nums,
.pack-stats b {
  font-variant-numeric: tabular-nums;
}
.pack-stats {
  display: flex;
  gap: 20px;
  color: var(--app-text-2);
}
.block {
  margin-bottom: 12px;
}
.sku-tag {
  margin: 2px 6px 2px 0;
}
.done {
  color: var(--ant-color-success, #52c41a);
}
.warn {
  color: var(--ant-color-warning, #faad14);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
}
</style>
