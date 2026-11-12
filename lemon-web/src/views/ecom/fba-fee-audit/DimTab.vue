<template>
  <div class="tab-body">
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomFba.common.allStores')" class="w220" @change="search" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomFba.health.keywordPh')" allow-clear class="w240" @search="search" />
      <a-checkbox v-model:checked="query.missingOnly" @change="search">{{ $t('ecomFba.dim.missingOnly') }}</a-checkbox>
    </div>
    <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
      <a-tooltip :title="query.storeId ? '' : $t('ecomFba.common.pickStoreFirst')">
        <a-button v-perm="'ecom:fbaFee:edit'" :disabled="!query.storeId" @click="sync(query.storeId)">
          <template #icon><CloudSyncOutlined /></template>{{ $t('ecomFba.dim.sync') }}
        </a-button>
      </a-tooltip>
      <a-button v-perm="'ecom:fbaFee:edit'" @click="uploadRef.open(query.storeId, AmzReportType.ESTIMATED_FEES)">
        <template #icon><UploadOutlined /></template>{{ $t('ecomFba.common.uploadReport') }}
      </a-button>
      <a-tooltip :title="$t('ecomFba.dim.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
    </TableToolbar>
    <a-table
      :row-key="(r) => `${r.storeId}|${r.sellerSku}`"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sku'">
          <div>{{ record.sellerSku }}</div>
          <div v-if="record.title" class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
        </template>
        <template v-else-if="column.key === 'material'">
          <div>{{ record.materialCode || '-' }}</div>
          <div class="sub-text">{{ record.materialName }}</div>
        </template>
        <template v-else-if="column.key === 'size'">
          <span v-if="record.lengthCm">{{ n0(record.lengthCm) }} × {{ n0(record.widthCm) }} × {{ n0(record.heightCm) }}</span>
          <a-tag v-else color="warning" :bordered="false">{{ $t('ecomFba.dim.notSet') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'weightKg'">{{ record.weightKg != null ? n0(record.weightKg) : '-' }}</template>
        <template v-else-if="column.key === 'source'">{{ record.source ? $t(`ecomFba.dataSource.${record.source}`) : '-' }}</template>
        <template v-else-if="column.money">
          <span v-if="record[column.key] != null">{{ money(record[column.key]) }} <span class="sub-text">{{ record.currency }}</span></span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'edit', label: $t('ecomFba.dim.edit'), perm: 'ecom:fbaFee:edit', onClick: () => dimRef.open(record) },
              { type: 'delete', perm: 'ecom:fbaFee:edit', show: !!record.id, confirm: $t('ecomFba.dim.deleteConfirm', { sku: record.sellerSku }), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>
    <DimModal ref="dimRef" @success="load" />
    <AmzReportUploadModal ref="uploadRef" :types="[AmzReportType.ESTIMATED_FEES]" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { CloudSyncOutlined, InfoCircleOutlined, UploadOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import AmzReportUploadModal from '@/components/ecom/AmzReportUploadModal.vue'
import DimModal from './DimModal.vue'
import { deleteDim, pageDims, syncDims } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'
import { AmzReportType, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'
import { n0 } from '../fba-health/fba'

/** 商品尺寸：平台仓配送的在线商品 + 已维护的尺寸重量、亚马逊分段 / 预估费、系统试算 */
const m = (key, title) => ({ title, key, dataIndex: key, width: 120, align: 'right', money: true })
const columns = [
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomFba.common.sku'), key: 'sku', width: 200, exportColumns: [{ title: t('ecomFba.common.sku'), dataIndex: 'sellerSku' }, { title: t('ecomFba.common.title'), dataIndex: 'title' }] },
  { title: t('ecomFba.common.material'), key: 'material', width: 160, exportColumns: [{ title: t('ecomFba.common.material'), dataIndex: 'materialCode' }] },
  { title: t('ecomFba.dim.size'), key: 'size', width: 160, exportValue: (r) => (r.lengthCm ? `${r.lengthCm}x${r.widthCm}x${r.heightCm}` : '') },
  { title: t('ecomFba.dim.weight'), key: 'weightKg', dataIndex: 'weightKg', width: 90, align: 'right' },
  { title: t('ecomFba.dim.source'), key: 'source', dataIndex: 'source', width: 70 },
  { title: t('ecomFba.dim.platformTier'), dataIndex: 'platformTier', width: 140, ellipsis: true },
  m('platformFee', t('ecomFba.dim.platformFee')),
  { title: t('ecomFba.dim.tierName'), dataIndex: 'tierName', width: 140, ellipsis: true },
  m('expectedFee', t('ecomFba.dim.expectedFee')),
  actionColumn(2, [t('ecomFba.dim.edit')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-dims')
const query = reactive({ storeId: undefined, keyword: undefined, missingOnly: false })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageDims, query, {
  params: (p) => ({ ...p, missingOnly: p.missingOnly || undefined })
})
onMounted(load)

const dimRef = ref()
const uploadRef = ref()
const remove = useAction(deleteDim, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const sync = useAction(syncDims, { successKey: (ids) => t('ecomFba.common.syncCreated', { n: (ids || []).length }) })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.w220 {
  width: 220px;
}
.w240 {
  width: 240px;
  max-width: 100%;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
