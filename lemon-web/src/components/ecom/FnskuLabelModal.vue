<template>
  <a-modal :open="open" :title="$t('ecomCatalog.fnsku.printTitle')" :ok-text="$t('common.action.print')" :confirm-loading="printing" width="min(760px, 100vw)"
           @ok="print" @cancel="emit('update:open', false)">
    <div class="sub-text block">{{ $t('ecomCatalog.fnsku.hint') }}</div>
    <a-table row-key="key" size="small" :columns="columns" :data-source="rows" :pagination="false" :scroll="{ x: 640, y: 320 }" class="block">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'fnsku'">
          <span v-if="record.fnsku">{{ record.fnsku }}</span>
          <a-tag v-else color="warning" :bordered="false">{{ $t('ecomCatalog.fnsku.missing') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'copies'">
          <a-input-number v-model:value="record.copies" :min="0" :max="9999" :precision="0" size="small" :disabled="!record.fnsku" />
        </template>
      </template>
    </a-table>
    <div class="sub-text block">{{ $t('ecomCatalog.fnsku.summary', { n: printable.length, total }) }}</div>
    <iframe v-if="previewHtml" class="preview" :srcdoc="previewHtml" :title="$t('ecomCatalog.fnsku.printTitle')" />
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { labelConfig, printLabels, renderLabelsHtml } from '@/utils/labels'
import { loadTemplates } from '@/utils/printTemplates'

/**
 * FNSKU 标签打印（前端 HTML，Code128 条码 + 标题 + 状态 New，模板 label.fnsku 可在「打印模板」中调整尺寸与字段）：
 * items 为 [{ fnsku, sellerSku, title, qty }]，每行份数默认为 qty（每件一张），可修改；没有 FNSKU 的不打印
 */
const props = defineProps({
  open: Boolean,
  items: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:open'])
const { t } = useI18n()
const TYPE = 'label.fnsku'

const columns = [
  { title: 'FNSKU', key: 'fnsku', width: 140 },
  { title: t('ecomCatalog.common.sellerSku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecomCatalog.common.title'), dataIndex: 'title', ellipsis: true },
  { title: t('ecomCatalog.fnsku.copies'), key: 'copies', width: 110 }
]
const rows = ref([])
const printing = ref(false)
const previewHtml = ref('')
const printable = computed(() => rows.value.filter((r) => r.fnsku && r.copies > 0))
const total = computed(() => printable.value.reduce((s, r) => s + r.copies, 0))

const toLabel = (r) => ({ fnsku: r.fnsku, title: r.title || r.sellerSku, condition: t('ecomCatalog.fnsku.conditionNew'), sku: r.sellerSku })

watch(() => props.open, async (v) => {
  if (!v) return
  rows.value = props.items.map((r, i) => ({ ...r, key: `${i}-${r.sellerSku}`, copies: r.fnsku ? Math.max(1, Math.ceil(Number(r.qty) || 1)) : 0 }))
  previewHtml.value = ''
  const first = rows.value.find((r) => r.fnsku)
  if (!first) return
  const saved = (await loadTemplates().catch(() => ({})))[TYPE]
  previewHtml.value = await renderLabelsHtml(TYPE, [toLabel(first)], labelConfig(TYPE, saved))
})

async function print() {
  if (!total.value) {
    message.warning(t('ecomCatalog.fnsku.nothing'))
    return
  }
  printing.value = true
  try {
    await printLabels(TYPE, printable.value.flatMap((r) => Array.from({ length: r.copies }, () => toLabel(r))))
    emit('update:open', false)
  } finally {
    printing.value = false
  }
}
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.preview {
  width: 100%;
  height: 200px;
  border: 1px solid var(--app-border, #eee);
  border-radius: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
