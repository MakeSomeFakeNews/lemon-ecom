<template>
  <div>
    <div class="section-head">
      <span class="section-title">{{ $t('ecomCatalog.box.section') }}</span>
      <a-space wrap>
        <a-button v-if="editable && userStore.hasPerm('ecom:inbound:box')" size="small" @click="boxRef.open(detail)">
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
    <a-table row-key="sellerSku" size="small" :columns="columns" :data-source="skuLines" :pagination="false" :scroll="{ x: 760 }" class="block">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">{{ record.title || record.materialName || '-' }}</template>
        <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
        <template v-else-if="column.key === 'boxedQty'">
          <span :class="{ done: Number(record.boxedQty) === Number(record.qty), warn: Number(record.boxedQty) < Number(record.qty) }">{{ qty(record.boxedQty) }}</span>
        </template>
      </template>
    </a-table>
    <div v-if="boxes.length" class="sub-text block">
      {{ $t('ecomCatalog.box.summary', { n: boxes.length }) }}
      <span v-for="b in boxes" :key="b.boxNo" class="box-chip">
        #{{ b.boxNo }} {{ boxSize(b) }} {{ b.weight != null ? `${qty(b.weight, 3)} kg` : '' }} ·
        {{ (b.items || []).map((i) => `${i.sellerSku} × ${qty(i.qty)}`).join(', ') }}
      </span>
    </div>
    <InboundBoxModal ref="boxRef" @success="emit('changed')" />
    <FnskuLabelModal v-model:open="fnskuOpen" :items="fnskuItems" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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
  margin: 12px 0 8px;
}
.section-title {
  font-weight: 600;
}
.block {
  margin-bottom: 12px;
}
.box-chip {
  display: inline-block;
  margin-right: 12px;
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
}
</style>
