<template>
  <a-table
    row-key="key"
    :size="size"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: 1100, y: '100%' }"
    :expandable="{ expandedRowKeys: expanded, onExpand, rowExpandable: (r) => !!r.parentItemId }"
    @change="onTableChange"
  >
    <template #expandedRowRender="{ record }">
      <a-table row-key="id" size="small" :columns="variantColumns" :data-source="variants[record.key] || []" :loading="!variants[record.key]"
               :pagination="false" :scroll="{ x: 900 }">
        <template #bodyCell="{ column, record: v }">
          <template v-if="column.key === 'material'">
            <span v-if="v.composeType === ListingComposeType.BUNDLE"><a-tag color="purple" :bordered="false">{{ $t('ecomCatalog.listing.bundle') }}</a-tag>{{ bundleText(v) }}</span>
            <span v-else-if="v.materialId">{{ v.materialCode }} {{ v.materialName }}<span v-if="Number(v.packQty) !== 1" class="sub-text"> × {{ qty(v.packQty) }}</span></span>
            <a-tag v-else color="warning" :bordered="false">{{ $t('ecom.common.unmapped') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'price'">{{ v.price != null ? `${v.currency || ''} ${price(v.price)}` : '-' }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="ecom_listing_status" :value="v.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a v-if="userStore.hasPerm('ecom:listing:edit')" @click="emit('edit', v)">{{ $t('common.action.edit') }}</a>
          </template>
        </template>
      </a-table>
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'store'">
        <div>{{ record.storeName }}</div>
        <div v-if="record.platform" class="sub-text"><DictTag type="ecom_platform" :value="record.platform" /></div>
      </template>
      <template v-else-if="column.key === 'spu'">
        <div class="cell-ellipsis" :title="record.title">{{ record.title || '-' }}</div>
        <div class="sub-text">{{ record.parentItemId || $t('ecomCatalog.spu.noParent') }}</div>
      </template>
      <template v-else-if="column.key === 'variants'">
        {{ record.variantCount }}
        <a-tag v-if="record.mappedCount < record.variantCount" color="warning" :bordered="false" class="ml">
          {{ $t('ecomCatalog.spu.unmappedCount', { n: record.variantCount - record.mappedCount }) }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'price'">
        <span v-if="record.minPrice != null">{{ record.currency || '' }} {{ price(record.minPrice) }}<template v-if="Number(record.maxPrice) !== Number(record.minPrice)"> – {{ price(record.maxPrice) }}</template></span>
        <span v-else>-</span>
      </template>
      <template v-else-if="column.key === 'action'">
        <template v-if="userStore.hasPerm('ecom:listing:edit')">
          <a v-if="record.parentItemId" @click="emit('mapStyle', record)">{{ $t('ecomCatalog.style.title') }}</a>
          <a v-else @click="emit('edit', { id: record.listingId })">{{ $t('common.action.edit') }}</a>
        </template>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { pageListing, spuListing } from '@/api/ecom/listing'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { ListingComposeType } from '@/constants/enums'
import { price, qty } from '@/utils/number'
import { bundleText } from './listing'

/**
 * 在线商品的 SPU 视图：按 店铺 + 父商品 ID 分组（没有父商品 ID 的自成一组），展开显示变体；
 * 事件 edit(变体)、mapStyle(分组：按款式批量映射)。ref 调用 load(query)
 */
defineProps({ size: { type: String, default: 'middle' } })
const emit = defineEmits(['edit', 'mapStyle'])
const { t } = useI18n()
const userStore = useUserStore()

const columns = [
  { title: t('ecom.common.store'), key: 'store', width: 170 },
  { title: t('ecomCatalog.spu.spu'), key: 'spu', width: 320 },
  { title: t('ecomCatalog.spu.variants'), key: 'variants', width: 150 },
  { title: t('ecom.listing.price'), key: 'price', width: 160, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const variantColumns = [
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecom.listing.variant'), dataIndex: 'variant', width: 140 },
  { title: t('ecom.common.platformItemId'), dataIndex: 'platformItemId', width: 130 },
  { title: t('ecom.common.material'), key: 'material', width: 260 },
  { title: t('ecom.listing.price'), key: 'price', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 80 }
]

const query = reactive({ storeId: undefined, keyword: undefined, mapped: undefined })
const keyOf = (g) => `${g.storeId}|${g.parentItemId ?? `#${g.listingId}`}`
const { list, loading, pagination, load: loadPage, onTableChange } = useTable(spuListing, query)
const rows = ref([])
const variants = reactive({})
const expanded = ref([])

async function load(q) {
  if (q) Object.assign(query, q)
  expanded.value = []
  await loadPage()
  rows.value = list.value.map((g) => ({ ...g, key: keyOf(g) }))
}

/** 展开：按父商品 ID 查变体（没有父商品的单个商品不展开） */
async function onExpand(open, record) {
  expanded.value = open ? [...expanded.value, record.key] : expanded.value.filter((k) => k !== record.key)
  if (!open || variants[record.key]) return
  const res = await pageListing({ storeId: record.storeId, parentItemId: record.parentItemId, current: 1, size: 200 })
  variants[record.key] = res.records
}

defineExpose({ load })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ml {
  margin-left: 6px;
}
</style>
