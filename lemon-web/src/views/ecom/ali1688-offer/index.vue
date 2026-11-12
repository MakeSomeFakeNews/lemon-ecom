<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomDev.offer.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomDev.offer.supplier')" name="supplierId">
          <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('ecomDev.offer.priceAlert')" name="priceAlert">
          <a-checkbox :checked="query.priceAlert === 1" @change="(e) => (query.priceAlert = e.target.checked ? 1 : undefined)">{{ $t('ecomDev.offer.onlyAlert') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:ali1688Offer:edit'" type="primary" @click="linkModal.open()">
          <template #icon><LinkOutlined /></template>{{ $t('ecomDev.offer.link') }}
        </a-button>
        <a-button v-perm="'ecom:ali1688Offer:sync'" :loading="syncing" @click="sync(selectedKeys)">
          <template #icon><SyncOutlined /></template>{{ selectedKeys.length ? $t('ecomDev.offer.syncSelected') : $t('ecomDev.offer.syncAll') }}
        </a-button>
        <a-button v-perm="'ecom:ali1688Offer:edit'" v-if="selectedKeys.length" @click="ack(selectedKeys)">
          <template #icon><CheckOutlined /></template>{{ $t('ecomDev.offer.ack') }} ({{ selectedKeys.length }})
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div class="cell">
              <img v-if="record.materialImage" :src="record.materialImage" class="thumb" alt="" />
              <div>
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.materialSpec || '' }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'offer'">
            <div class="cell">
              <img v-if="record.imageUrl" :src="record.imageUrl" class="thumb" alt="" />
              <div class="offer-info">
                <a :href="record.offerUrl" target="_blank" rel="noopener" :title="record.title">{{ record.title || record.offerId }}</a>
                <div class="sub-text">{{ record.offerId }}<span v-if="record.specText"> · {{ record.specText }}</span></div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'seller'">
            <div>{{ record.sellerName || record.sellerLoginId || '-' }}</div>
            <div v-if="record.supplierName" class="sub-text">{{ $t('ecomDev.offer.supplierText', { name: record.supplierName }) }}</div>
          </template>
          <template v-else-if="column.key === 'price'">
            <b>{{ record.price != null ? `¥${price(record.price)}` : '-' }}</b>
            <div v-if="record.lastPrice != null" :class="['change', Number(record.price) > Number(record.lastPrice) ? 'up' : 'down']">
              <component :is="Number(record.price) > Number(record.lastPrice) ? ArrowUpOutlined : ArrowDownOutlined" />
              {{ $t('ecomDev.offer.from', { v: price(record.lastPrice) }) }}
            </div>
            <a-tag v-if="record.priceAlert === 1" color="red" class="alert-tag">{{ $t('ecomDev.offer.alertTag') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'moq'">{{ record.minOrderQty ?? '-' }} {{ record.unit || '' }}</template>
          <template v-else-if="column.key === 'stock'">{{ record.stock ?? '-' }}</template>
          <template v-else-if="column.key === 'qtyRatio'">{{ Number(record.qtyRatio) }}</template>
          <template v-else-if="column.key === 'sync'">
            <span class="sub-text">{{ record.syncTime || '-' }}</span>
            <div v-if="record.syncMessage" class="err" :title="record.syncMessage">{{ record.syncMessage }}</div>
          </template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <OfferLinkModal ref="linkModal" @success="load" />
    <a-modal v-model:open="logOpen" :title="$t('ecomDev.offer.priceLogs')" :footer="null" width="560px">
      <a-table row-key="id" size="small" :columns="logColumns" :data-source="logs" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'changeRate'">
            <span :class="Number(record.changeRate) > 0 ? 'up' : 'down'">{{ record.changeRate != null ? `${Number(record.changeRate) > 0 ? '+' : ''}${Number(record.changeRate)}%` : '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ArrowDownOutlined, ArrowUpOutlined, CheckOutlined, HistoryOutlined, LinkOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import OfferLinkModal from './OfferLinkModal.vue'
import { ackAli1688Offer, ali1688PriceLogs, deleteAli1688Offer, pageAli1688Offer, syncAli1688Offer } from '@/api/ecom/dev'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { price } from '@/utils/number'

/**
 * 1688 货源：物料关联的 1688 商品（规格、价格、起批量、库存），同步价格库存（定时任务每天同步），价格变动超过阈值时提醒，确认后清除；
 * 采购订单一键下单到 1688 时按这里的货源换算数量、按卖家拆单
 */
const { t } = useI18n()
const route = useRoute()

const columns = [
  { title: t('ecomDev.offer.material'), key: 'material', width: 220, exportValue: (r) => `${r.materialCode} ${r.materialName}` },
  { title: t('ecomDev.offer.offer'), key: 'offer', width: 260, exportValue: (r) => `${r.offerId} ${r.title || ''}` },
  { title: t('ecomDev.offer.seller'), key: 'seller', width: 180, exportValue: (r) => r.sellerName || r.sellerLoginId },
  { title: t('ecomDev.offer.price'), key: 'price', dataIndex: 'price', width: 130, align: 'right' },
  { title: t('ecomDev.offer.moq'), key: 'moq', dataIndex: 'minOrderQty', width: 90, align: 'right' },
  { title: t('ecomDev.offer.stock'), key: 'stock', dataIndex: 'stock', width: 90, align: 'right' },
  { title: t('ecomDev.offer.qtyRatio'), key: 'qtyRatio', dataIndex: 'qtyRatio', width: 80, align: 'right' },
  { title: t('ecomDev.offer.syncTime'), key: 'sync', dataIndex: 'syncTime', width: 160 },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-ali1688-offer')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, supplierId: undefined, priceAlert: undefined })
const query = reactive(emptyQuery())
if (route.query.priceAlert) query.priceAlert = 1
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageAli1688Offer, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const linkModal = ref()
const syncing = ref(false)
async function sync(ids) {
  syncing.value = true
  try {
    const r = await syncAli1688Offer(ids)
    const text = t('ecomDev.offer.syncResult', { total: r.total, success: r.success, changed: r.changed, failed: r.failed })
    r.failed ? message.warning(text) : message.success(text)
    load()
  } finally {
    syncing.value = false
  }
}
const ack = useAction((ids) => ackAli1688Offer([].concat(ids)), { successKey: 'ecomDev.offer.acked', reload: () => load() })
const remove = useAction(deleteAli1688Offer, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const logOpen = ref(false)
const logs = ref([])
const logColumns = [
  { title: t('ecomDev.offer.changeTime'), dataIndex: 'createTime', width: 170 },
  { title: t('ecomDev.offer.oldPrice'), dataIndex: 'oldPrice', align: 'right' },
  { title: t('ecomDev.offer.newPrice'), dataIndex: 'newPrice', align: 'right' },
  { title: t('ecomDev.offer.changeRate'), key: 'changeRate', align: 'right' }
]
async function showLogs(r) {
  logs.value = await ali1688PriceLogs(r.id)
  logOpen.value = true
}

const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:ali1688Offer:edit', onClick: () => linkModal.value.open(r) },
  { key: 'ack', label: t('ecomDev.offer.ack'), icon: CheckOutlined, perm: 'ecom:ali1688Offer:edit', show: r.priceAlert === 1, onClick: () => ack([r.id]) },
  { key: 'logs', label: t('ecomDev.offer.priceLogs'), icon: HistoryOutlined, onClick: () => showLogs(r) },
  { key: 'sync', label: t('ecomDev.offer.sync'), icon: SyncOutlined, perm: 'ecom:ali1688Offer:sync', more: true, onClick: () => sync([r.id]) },
  { type: 'delete', perm: 'ecom:ali1688Offer:edit', more: true, confirm: { title: t('ecomDev.offer.deleteConfirm'), okType: 'danger' }, onClick: () => remove(r.id) }
]
</script>

<style scoped>
.cell {
  display: flex;
  gap: 8px;
  align-items: center;
}
.thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  flex: none;
}
.offer-info {
  min-width: 0;
}
.offer-info a {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.err {
  color: #cf1322;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.change {
  font-size: 12px;
}
.up {
  color: #cf1322;
}
.down {
  color: #389e0d;
}
.alert-tag {
  margin: 2px 0 0;
}
</style>
