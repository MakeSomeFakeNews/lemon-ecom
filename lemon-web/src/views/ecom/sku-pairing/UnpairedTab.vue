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
        <a-button type="primary" @click="search">{{ $t('common.action.search') }}</a-button>
      </a-form-item>
      <a-form-item>
        <a-button v-perm="'ecom:pairing:edit'" :disabled="!confirmable.length" :loading="saving" @click="confirmSelected">
          <template #icon><CheckOutlined /></template>{{ $t('ecomCatalog.pairing.confirmSelected', { n: confirmable.length }) }}
        </a-button>
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
      :scroll="{ x: 1200, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'store'">
          <div>{{ record.storeName }}</div>
          <div v-if="record.platform" class="sub-text"><DictTag type="ecom_platform" :value="record.platform" /></div>
        </template>
        <template v-else-if="column.key === 'title'">
          <div class="cell-ellipsis" :title="record.title">{{ record.title || '-' }}</div>
          <div v-if="record.variant" class="sub-text">{{ record.variant }}</div>
        </template>
        <template v-else-if="column.key === 'suggestion'">
          <template v-if="record.suggestion">
            <a-tag :color="MATCH_COLORS[record.suggestion.matchType]" :bordered="false">
              {{ $t(`ecomCatalog.pairing.match.${record.suggestion.matchType}`) }} {{ record.suggestion.score }}
            </a-tag>
            <span class="sub-text">{{ record.suggestion.materialCode }} {{ record.suggestion.materialName }}</span>
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
          <a v-if="userStore.hasPerm('ecom:pairing:edit')" :class="{ disabled: !record.materialId }" @click="confirmRows([record])">
            {{ $t('ecomCatalog.pairing.confirm') }}
          </a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { CheckOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { confirmPairs, pageUnpaired } from '@/api/ecom/pairing'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { PairingMatchType } from '@/constants/enums'

/** 未配对的平台 SKU：每行带推荐物料（可改选），勾选后一键确认；确认后触发 changed。ref 调用 reload() */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const userStore = useUserStore()

const MATCH_COLORS = { [PairingMatchType.EXACT]: 'green', [PairingMatchType.STRIPPED]: 'blue', [PairingMatchType.TITLE]: 'orange', [PairingMatchType.ATTRIBUTE]: 'cyan' }
const columns = [
  { title: t('ecom.common.store'), key: 'store', width: 160 },
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 170 },
  { title: t('ecomCatalog.common.title'), key: 'title', width: 240 },
  { title: t('ecomCatalog.pairing.suggestion'), key: 'suggestion', width: 260 },
  { title: t('ecomCatalog.pairing.material'), key: 'material', width: 260 },
  { title: t('ecom.common.packQty'), key: 'packQty', width: 110 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
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
.disabled {
  pointer-events: none;
  color: var(--app-text-3);
}
</style>
