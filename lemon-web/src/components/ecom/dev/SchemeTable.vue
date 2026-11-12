<template>
  <div>
    <div v-if="!projectId" class="tools">
      <a-input-search v-model:value="keyword" :placeholder="$t('ecomDev.scheme.keywordPh')" allow-clear class="search" @search="search" />
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: 900 }"
             @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'platform'">
          <DictTag type="ecom_platform" :value="record.platform" /> {{ record.marketplace || '' }}
        </template>
        <template v-else-if="column.key === 'salePrice'">{{ record.currency }} {{ money(record.salePrice) }}</template>
        <template v-else-if="column.key === 'profit'">{{ symMoney(record.profit) }}</template>
        <template v-else-if="column.key === 'margin'">
          <span :class="['margin', marginClass(record.margin)]">{{ record.margin != null ? `${Number(record.margin)}%` : '-' }}</span>
        </template>
        <template v-else-if="column.key === 'roi'">{{ record.roi != null ? `${Number(record.roi)}%` : '-' }}</template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DownloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { deleteProfitScheme, getProfitScheme, pageProfitScheme } from '@/api/ecom/dev'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { money, symMoney } from '@/utils/number'
import { marginClass } from '@/views/ecom/dev-project/dev'

/**
 * 试算方案列表：载入（触发 load(参数)）、删除。projectId 不为空时只显示该开发项目的方案。ref 调用 reload()
 */
const props = defineProps({ projectId: Number })
const emit = defineEmits(['load'])
const { t } = useI18n()
const userStore = useUserStore()
const canDelete = computed(() => userStore.hasPerm('ecom:profitCalc:save') || (props.projectId && userStore.hasPerm('ecom:devProject:edit')))

const keyword = ref()
const query = reactive({ projectId: props.projectId, keyword: undefined })
const { list, loading, pagination, load, search: doSearch, onTableChange } = useTable(pageProfitScheme, query)
const search = () => {
  query.keyword = keyword.value || undefined
  doSearch()
}

const columns = computed(() => [
  { title: t('ecomDev.scheme.name'), dataIndex: 'name', width: 200, ellipsis: true },
  ...(props.projectId ? [] : [{ title: t('ecomDev.scheme.project'), dataIndex: 'projectName', width: 160, ellipsis: true }]),
  { title: t('ecomDev.calc.platform'), key: 'platform', width: 140 },
  { title: t('ecomDev.calc.salePrice'), key: 'salePrice', width: 110, align: 'right' },
  { title: t('ecomDev.calc.profit'), key: 'profit', width: 100, align: 'right' },
  { title: t('ecomDev.calc.margin'), key: 'margin', width: 90, align: 'right' },
  { title: 'ROI', key: 'roi', width: 90, align: 'right' },
  { title: t('ecomDev.scheme.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 90, align: 'right', fixed: 'right' }
])

async function loadScheme(record) {
  const s = await getProfitScheme(record.id)
  emit('load', JSON.parse(s.params || '{}'), s)
  message.success(t('ecomDev.scheme.loaded', { name: s.name }))
}

const rowActions = (record) => [
  { key: 'load', label: t('ecomDev.scheme.load'), icon: DownloadOutlined, onClick: () => loadScheme(record) },
  {
    type: 'delete',
    show: canDelete.value,
    confirm: { title: t('ecomDev.scheme.deleteConfirm', { name: record.name }), okType: 'danger' },
    onClick: async () => {
      await deleteProfitScheme(record.id)
      message.success(t('common.msg.deleteSuccess'))
      load()
    }
  }
]

onMounted(load)
defineExpose({ reload: load })
</script>

<style scoped>
.tools {
  margin-bottom: 8px;
}
.search {
  max-width: 320px;
}
.margin.good {
  color: #389e0d;
}
.margin.low {
  color: #d48806;
}
.margin.loss {
  color: #cf1322;
}
</style>
