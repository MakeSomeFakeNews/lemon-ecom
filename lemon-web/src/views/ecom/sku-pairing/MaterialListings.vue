<template>
  <a-table row-key="key" size="small" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 860 }">
    <template #bodyCell="{ column, record: l }">
      <template v-if="column.key === 'store'">
        {{ l.storeName }} <DictTag v-if="l.platform" type="ecom_platform" :value="l.platform" />
      </template>
      <template v-else-if="column.key === 'kind'">
        <DictTag type="ecom_compose_type" :value="l.composeType || ListingComposeType.SINGLE" />
      </template>
      <template v-else-if="column.key === 'qty'">{{ qty(l.packQty) }}</template>
      <template v-else-if="column.key === 'status'"><DictTag type="ecom_listing_status" :value="l.status" /></template>
      <template v-else-if="column.key === 'action'">
        <a-popconfirm v-if="userStore.hasPerm('ecom:pairing:edit')" :title="$t('ecomCatalog.pairing.unpairConfirm', { sku: l.sellerSku })"
                      @confirm="unpair(l.id)">
          <a class="danger">{{ $t('ecomCatalog.pairing.unpair') }}</a>
        </a-popconfirm>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { listingsOfMaterial, unpairListings } from '@/api/ecom/pairing'
import { useUserStore } from '@/store/user'
import { ListingComposeType } from '@/constants/enums'
import { qty } from '@/utils/number'

/**
 * 物料展开行：该物料对应的各店铺 / 平台的平台 SKU，可解除配对。
 * 独立组件自己加载数据：Ant Design 表格会缓存展开行的渲染结果，数据放在父组件里时加载完成后展开行不刷新（一直显示加载中）
 */
const props = defineProps({ materialId: { type: Number, required: true } })
const emit = defineEmits(['changed'])
const { t } = useI18n()
const userStore = useUserStore()

const columns = [
  { title: t('ecom.common.store'), key: 'store', width: 200 },
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 170 },
  { title: t('ecomCatalog.common.title'), dataIndex: 'title', ellipsis: true },
  { title: t('ecomCatalog.listing.composeType'), key: 'kind', width: 90 },
  { title: t('ecomCatalog.pairing.qty'), key: 'qty', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 90 }
]
const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = ((await listingsOfMaterial(props.materialId)) || []).map((l, i) => ({ ...l, key: `${l.id}-${i}` }))
  } finally {
    loading.value = false
  }
}
async function unpair(listingId) {
  await unpairListings(listingId)
  message.success(t('ecomCatalog.pairing.unpaired'))
  await load()
  emit('changed')
}
onMounted(load)
</script>
