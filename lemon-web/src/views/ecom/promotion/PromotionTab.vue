<template>
  <div class="tab-body">
    <SearchForm v-show="showSearch" :model="query" @search="search" @reset="resetQuery">
      <a-form-item :label="$t('ecomOper.common.store')" name="storeId">
        <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomOper.common.allStores')" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.promo.promoType')" name="promoType">
        <a-select v-model:value="query.promoType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_promo_type)" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.promo.status')" name="status">
        <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_promo_status)" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.promo.phase')" name="phase">
        <a-select v-model:value="query.phase" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_promo_phase)" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.promo.period')" name="range">
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" class="full" />
      </a-form-item>
      <a-form-item :label="$t('common.field.keyword')" name="keyword">
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomOper.promo.keywordPh')" @press-enter="search" />
      </a-form-item>
    </SearchForm>

    <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
      <a-button v-perm="'ecom:promotion:add'" type="primary" @click="emit('create')">
        <template #icon><PlusOutlined /></template>{{ $t('ecomOper.promo.add') }}
      </a-button>
      <span class="sub-text">{{ pushTip }}</span>
    </TableToolbar>
    <a-table
      row-key="id"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <a @click="emit('detail', record.id)">{{ record.name }}</a>
          <div class="sub-text">{{ record.storeName }} · <DictTag type="ecom_platform" :value="record.platform" /></div>
        </template>
        <template v-else-if="column.key === 'promoType'"><DictTag type="ecom_promo_type" :value="record.promoType" /></template>
        <template v-else-if="column.key === 'period'">{{ periodText(record) }}</template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="ecom_promo_status" :value="record.status" />
          <DictTag v-if="record.phase" type="ecom_promo_phase" :value="record.phase" />
        </template>
        <template v-else-if="column.key === 'pushStatus'">
          <a-tooltip v-if="record.pushMessage" :title="record.pushMessage"><DictTag type="ecom_promo_push_status" :value="record.pushStatus" /></a-tooltip>
          <DictTag v-else type="ecom_promo_push_status" :value="record.pushStatus" />
        </template>
        <template v-else-if="column.key === 'feeAmount'">{{ Number(record.feeAmount) ? `${money(record.feeAmount)} ${record.currency}` : '-' }}</template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CloudUploadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { rangeParams } from '@/components/ecom/ecomReport'
import { cancelPromotion, confirmPromotion, deletePromotion, pagePromotion, promotionPushPlatforms, pushPromotion } from '@/api/ecom/oper'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { canDelete, canEdit, canPush, isActive, isDraft, periodText } from './promo'

/** 促销活动列表：生效、取消、推送、删除在行操作中完成，新增 / 编辑 / 详情交给页面（create、edit、detail 事件）。ref 调用 reload() */
const emit = defineEmits(['create', 'edit', 'detail'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_promo_type', 'ecom_promo_status', 'ecom_promo_phase', 'ecom_promo_push_status')

const columns = [
  { title: t('ecomOper.promo.name'), key: 'name', dataIndex: 'name', width: 240 },
  { title: t('ecomOper.promo.promoType'), key: 'promoType', dataIndex: 'promoType', width: 130, dict: 'ecom_promo_type' },
  { title: t('ecomOper.promo.period'), key: 'period', width: 230, exportValue: (r) => periodText(r) },
  { title: t('ecomOper.promo.status'), key: 'status', dataIndex: 'status', width: 150, dict: 'ecom_promo_status' },
  { title: t('ecomOper.promo.pushStatus'), key: 'pushStatus', dataIndex: 'pushStatus', width: 100, dict: 'ecom_promo_push_status' },
  { title: t('ecomOper.promo.itemCount'), dataIndex: 'itemCount', width: 80, align: 'right' },
  { title: t('ecomOper.promo.fee'), key: 'feeAmount', dataIndex: 'feeAmount', width: 120, align: 'right' },
  { title: t('ecomOper.common.owner'), dataIndex: 'ownerName', width: 100 },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-promotion')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, promoType: undefined, status: undefined, phase: undefined, range: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePromotion, query, {
  params: ({ range, ...q }) => ({ ...q, ...rangeParams(range) })
})
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const pushPlatforms = ref([])
const pushTip = computed(() => t('ecomOper.promo.pushTip', {
  platforms: pushPlatforms.value.map((p) => findDict(dict.ecom_platform, p)?.label || p).join(t('ecomOper.common.sep')) || t('ecomOper.common.none')
}))

const confirm = useAction(confirmPromotion, { reload: () => load() })
const cancel = useAction(cancelPromotion, { reload: () => load() })
const remove = useAction(deletePromotion, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const push = useAction(pushPromotion, { successKey: (r) => t('ecomOper.promo.pushed', { id: r?.platformPromoId || '' }), reload: () => load() })
const platformLabel = (p) => findDict(dict.ecom_platform, p)?.label || p

const rowActions = (r) => [
  { type: 'detail', onClick: () => emit('detail', r.id) },
  canEdit(r) && { type: 'edit', perm: 'ecom:promotion:edit', onClick: () => emit('edit', r) },
  isDraft(r) && { type: 'confirm', label: t('ecomOper.promo.confirm'), perm: 'ecom:promotion:confirm', confirm: t('ecomOper.promo.confirmConfirm', { name: r.name }), onClick: () => confirm(r.id) },
  canPush(r) && { type: 'run', label: t('ecomOper.promo.push'), icon: CloudUploadOutlined, perm: 'ecom:promotion:push', confirm: t('ecomOper.promo.pushConfirm', { name: r.name, platform: platformLabel(r.platform) }), onClick: () => push(r.id) },
  (isDraft(r) || isActive(r)) && { type: 'cancel', label: t('ecomOper.promo.cancel'), perm: 'ecom:promotion:confirm', confirm: { title: t('ecomOper.promo.cancelConfirm', { name: r.name }), okType: 'danger' }, onClick: () => cancel(r.id) },
  canDelete(r) && { type: 'delete', perm: 'ecom:promotion:remove', confirm: { title: t('ecomOper.promo.deleteConfirm', { name: r.name }), okType: 'danger' }, onClick: () => remove(r.id) }
].filter(Boolean)

onMounted(async () => {
  load()
  pushPlatforms.value = (await promotionPushPlatforms()) || []
})
defineExpose({ reload: () => load() })
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
