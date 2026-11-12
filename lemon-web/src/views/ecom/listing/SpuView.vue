<template>
  <a-table
    row-key="key"
    :size="size"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: sumWidth(columns) + 48, y: '100%' }"
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
            <TableActions :actions="[{ type: 'edit', perm: 'ecom:listing:edit', onClick: () => emit('edit', v) }]" />
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
        <div class="product">
          <a-image v-if="record.imageUrl" :src="record.imageUrl" :width="48" :height="48" class="thumb" />
          <div v-else class="thumb empty"><PictureOutlined /></div>
          <div class="product-text">
            <a-tooltip :title="record.title" placement="topLeft" :mouse-enter-delay="0.4">
              <div class="title-clamp">{{ record.title || '-' }}</div>
            </a-tooltip>
            <div class="sub-text">{{ record.parentItemId || $t('ecomCatalog.spu.noParent') }}</div>
          </div>
        </div>
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
        <TableActions :actions="groupActions(record)" />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ApartmentOutlined, PictureOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { sumWidth } from '@/composables/useColumns'
import { pageListing, spuListing } from '@/api/ecom/listing'
import { useTable } from '@/composables/useTable'
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

const columns = [
  { title: t('ecomCatalog.spu.spu'), key: 'spu', width: 360, fixed: 'left' },
  { title: t('ecom.common.store'), key: 'store', width: 170 },
  { title: t('ecomCatalog.spu.variants'), key: 'variants', width: 150 },
  { title: t('ecom.listing.price'), key: 'price', width: 160, align: 'right' },
  actionColumn(1, [t('ecomCatalog.style.title'), t('common.action.edit')])
]
const variantColumns = [
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecom.listing.variant'), dataIndex: 'variant', width: 140 },
  { title: t('ecom.common.platformItemId'), dataIndex: 'platformItemId', width: 130 },
  { title: t('ecom.common.material'), key: 'material', width: 260 },
  { title: t('ecom.listing.price'), key: 'price', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  actionColumn(1, [], { fixed: undefined })
]

const groupActions = (g) =>
  g.parentItemId
    ? [{ key: 'style', label: t('ecomCatalog.style.title'), icon: ApartmentOutlined, perm: 'ecom:listing:edit', onClick: () => emit('mapStyle', g) }]
    : [{ type: 'edit', perm: 'ecom:listing:edit', onClick: () => emit('edit', { id: g.listingId }) }]

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
.product {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.product-text {
  flex: 1;
  min-width: 0;
}
.product :deep(.ant-image) {
  flex: none;
}
.product :deep(.thumb),
.thumb {
  flex: none;
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--app-border);
}
.thumb.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--app-text-3);
  background: var(--app-bg);
}
.title-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.4;
  word-break: break-word;
}
.ml {
  margin-left: 6px;
}
</style>
