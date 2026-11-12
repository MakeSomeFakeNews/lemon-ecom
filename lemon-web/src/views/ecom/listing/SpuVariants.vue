<template>
  <a-table row-key="id" size="small" :columns="variantColumns" :data-source="rows" :loading="loading"
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

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageListing } from '@/api/ecom/listing'
import { ListingComposeType } from '@/constants/enums'
import { price, qty } from '@/utils/number'
import { bundleText } from './listing'

/**
 * SPU 展开行：按 店铺 + 父商品 ID 查询变体。独立组件自己加载数据：Ant Design 表格会缓存展开行的渲染结果，
 * 数据放在父组件里时加载完成后展开行不刷新（一直显示加载中）。事件 edit(变体)
 */
const props = defineProps({ storeId: { type: Number, required: true }, parentItemId: { type: String, default: null } })
const emit = defineEmits(['edit'])
const { t } = useI18n()

const variantColumns = [
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecom.listing.variant'), dataIndex: 'variant', width: 140 },
  { title: t('ecom.common.platformItemId'), dataIndex: 'platformItemId', width: 130 },
  { title: t('ecom.common.material'), key: 'material', width: 260 },
  { title: t('ecom.listing.price'), key: 'price', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  actionColumn(1, [], { fixed: undefined })
]
const rows = ref([])
const loading = ref(false)

onMounted(async () => {
  if (!props.parentItemId) return
  loading.value = true
  try {
    rows.value = (await pageListing({ storeId: props.storeId, parentItemId: props.parentItemId, current: 1, size: 200 })).records
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
