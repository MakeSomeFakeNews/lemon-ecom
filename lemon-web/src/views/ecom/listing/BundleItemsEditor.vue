<template>
  <div>
    <div class="items-head">
      <span class="sub-text">{{ $t('ecomCatalog.bundle.hint') }}</span>
      <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
    </div>
    <LineItemsTable :items="items" :columns="columns" :min-rows="1">
      <template #cell="{ column, record }">
        <template v-if="column.key === 'materialId'">
          <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => onMaterial(record, m)" />
        </template>
        <template v-else-if="column.key === 'qty'">
          <a-input-number v-model:value="record.qty" :min="0.0001" :precision="4" class="full" :addon-after="record.unit || undefined" />
        </template>
      </template>
    </LineItemsTable>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { useLineItems } from '@/composables/useLineItems'

/** 组合商品的组成编辑：items 为 [{ key, materialId, materialLabel, unit, qty }]（由父组件持有，直接修改） */
const props = defineProps({ items: { type: Array, required: true } })
const { t } = useI18n()

const columns = [
  { title: t('ecomCatalog.bundle.material'), key: 'materialId', width: 360 },
  { title: t('ecomCatalog.bundle.qty'), key: 'qty', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 60, align: 'center' }
]
const lines = useLineItems(() => props.items, { newRow: () => ({ materialId: undefined, materialLabel: '', unit: '', qty: 1 }) })

function onMaterial(record, m) {
  record.materialLabel = m.label
  record.unit = m.unit
}
</script>

<style scoped>
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
