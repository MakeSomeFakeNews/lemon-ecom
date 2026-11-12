<template>
  <div class="tab-body">
    <a-form layout="inline" class="block">
      <a-form-item :label="$t('ecom.common.store')">
        <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecom.common.allStores')" class="w200" />
      </a-form-item>
      <a-form-item :label="$t('common.field.keyword')">
        <a-input v-model:value="query.keyword" :placeholder="$t('ecomCatalog.pairing.keywordPh')" allow-clear class="w200" @press-enter="search" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="search">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.search') }}
        </a-button>
      </a-form-item>
      <a-form-item v-if="selected.length" class="batch">
        <span class="selected-hint">{{ $t('common.table.selected', { n: selected.length }) }}</span>
        <a-button v-perm="'ecom:pairing:edit'" type="primary" ghost :disabled="!confirmable.length" :loading="saving" @click="confirmSelected">
          <template #icon><CheckOutlined /></template>{{ $t('ecomCatalog.pairing.confirmSelected', { n: confirmable.length }) }}
        </a-button>
        <a-button type="link" @click="selected = []">{{ $t('common.action.cancel') }}</a-button>
      </a-form-item>
    </a-form>
    <a-table
      row-key="listingId"
      size="middle"
      class="grow"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :row-selection="{ selectedRowKeys: selected, onChange: (k) => (selected = k) }"
      :scroll="{ x: sumWidth(columns) + 32, y: '100%' }"
      @change="onTableChange"
    >
      <template #emptyText>
        <a-empty v-if="!loading" :description="query.keyword || query.storeId ? $t('common.msg.noData') : $t('ecomCatalog.pairing.allPaired')" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'store'">
          <div class="cell-ellipsis" :title="record.storeName">{{ record.storeName }}</div>
          <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
        </template>
        <template v-else-if="column.key === 'sku'">
          <div class="cell-ellipsis" :title="record.sellerSku">{{ record.sellerSku }}</div>
          <div v-if="record.platformItemId" class="sub-text cell-ellipsis">{{ record.platformItemId }}</div>
        </template>
        <template v-else-if="column.key === 'title'">
          <div class="cell-ellipsis" :title="record.title">{{ record.title || '-' }}</div>
          <div v-if="record.variant" class="sub-text cell-ellipsis">{{ record.variant }}</div>
        </template>
        <template v-else-if="column.key === 'suggestion'">
          <template v-if="record.suggestion">
            <a-tag :color="MATCH_COLORS[record.suggestion.matchType]" :bordered="false">
              {{ $t(`ecomCatalog.pairing.match.${record.suggestion.matchType}`) }} {{ record.suggestion.score }}
            </a-tag>
            <div class="sub-text cell-ellipsis" :title="`${record.suggestion.materialCode} ${record.suggestion.materialName || ''}`">
              {{ record.suggestion.materialCode }} {{ record.suggestion.materialName }}
            </div>
          </template>
          <span v-else class="sub-text">{{ $t('ecomCatalog.pairing.noSuggestion') }}</span>
        </template>
        <template v-else-if="column.key === 'material'">
          <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" :placeholder="$t('ecomCatalog.pairing.pickMaterial')"
                          @select="(m) => (record.materialLabel = m.label)" />
        </template>
        <template v-else-if="column.key === 'packQty'">
          <a-input-number v-model:value="record.packQty" :min="0.0001" :precision="4" class="full" />
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { CheckOutlined, SearchOutlined } from '@ant-design/icons-vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { sumWidth } from '@/composables/useColumns'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { confirmPairs, pageUnpaired } from '@/api/ecom/pairing'
import { useTable } from '@/composables/useTable'
import { PairingMatchType } from '@/constants/enums'

/** 未配对的平台 SKU：每行带推荐物料（可改选），勾选后一键确认；确认后触发 changed。ref 调用 reload() */
const emit = defineEmits(['changed'])
const { t } = useI18n()

const MATCH_COLORS = { [PairingMatchType.EXACT]: 'green', [PairingMatchType.STRIPPED]: 'blue', [PairingMatchType.TITLE]: 'orange', [PairingMatchType.ATTRIBUTE]: 'cyan' }
const columns = [
  { title: t('ecom.common.store'), key: 'store', width: 160 },
  { title: t('ecom.listing.skuAsin'), key: 'sku', dataIndex: 'sellerSku', width: 170 },
  { title: t('ecomCatalog.common.title'), key: 'title', width: 240 },
  { title: t('ecomCatalog.pairing.suggestion'), key: 'suggestion', width: 260 },
  { title: t('ecomCatalog.pairing.material'), key: 'material', width: 260 },
  { title: t('ecom.common.packQty'), key: 'packQty', width: 110 },
  actionColumn(1, [t('ecomCatalog.pairing.confirm')])
]
const rowActions = (r) => [
  {
    type: 'confirm',
    label: t('ecomCatalog.pairing.confirm'),
    perm: 'ecom:pairing:edit',
    disabled: !r.materialId,
    tip: !r.materialId ? t('ecomCatalog.pairing.pickMaterial') : undefined,
    onClick: () => confirmRows([r])
  }
]

const query = reactive({ storeId: undefined, keyword: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageUnpaired, query)
const rows = ref([])
const selected = ref([])
const saving = ref(false)

/** 推荐的物料作为默认选择；编码相同、去掉前后缀相同的默认勾选，标题相似的需要人工确认 */
watch(list, (v) => {
  rows.value = v.map((r) => ({
    ...r,
    materialId: r.suggestion?.materialId,
    materialLabel: r.suggestion ? `${r.suggestion.materialCode} ${r.suggestion.materialName || ''}`.trim() : undefined,
    packQty: 1
  }))
  selected.value = rows.value.filter((r) => r.suggestion && r.suggestion.matchType !== PairingMatchType.TITLE).map((r) => r.listingId)
})
const reload = () => load()
onMounted(load)
watch(() => query.storeId, () => search())

const confirmable = computed(() => rows.value.filter((r) => selected.value.includes(r.listingId) && r.materialId))

async function confirmRows(list) {
  const pairs = list.filter((r) => r.materialId).map((r) => ({ listingId: r.listingId, materialId: r.materialId, packQty: r.packQty || 1 }))
  if (!pairs.length) return
  saving.value = true
  try {
    const n = await confirmPairs(pairs)
    message.success(t('ecomCatalog.pairing.confirmed', { n }))
    await reload()
    emit('changed')
  } finally {
    saving.value = false
  }
}
const confirmSelected = () => confirmRows(confirmable.value)

defineExpose({ reload })
</script>

<style scoped>
.tab-body {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.grow {
  flex: 1;
  min-height: 0;
}
.block {
  margin-bottom: 12px;
  row-gap: 8px;
}
.w200 {
  width: 200px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.batch :deep(.ant-form-item-control-input-content) {
  display: flex;
  align-items: center;
  gap: 8px;
}
.selected-hint {
  color: var(--app-text-2);
}
</style>
