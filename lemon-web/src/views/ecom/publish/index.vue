<template>
  <div class="table-page ecom-publish">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomPublish.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomPublish.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.common.platform')" name="platform">
          <a-select v-model:value="query.platform" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.list.keywordPh')" @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" size="small" class="status-tabs" @change="search">
        <a-tab-pane v-for="s in tabs" :key="s.value">
          <template #tab>{{ s.label }} <span class="count">{{ counts[s.value] || 0 }}</span></template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="reload">
        <a-button v-perm="'ecom:publish:add'" type="primary" @click="createRef.open()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomPublish.list.create') }}
        </a-button>
        <template v-if="selectedKeys.length">
          <a-button v-perm="'ecom:publish:push'" @click="submitSelected">
            <template #icon><CloudUploadOutlined /></template>{{ $t('ecomPublish.list.publishN', { n: selectedKeys.length }) }}
          </a-button>
          <a-button v-perm="'ecom:publish:push'" @click="offlineSelected">{{ $t('ecomPublish.list.offlineN', { n: selectedKeys.length }) }}</a-button>
          <a-button v-perm="'ecom:publish:remove'" danger @click="removeSelected">{{ $t('ecomPublish.list.deleteN', { n: selectedKeys.length }) }}</a-button>
        </template>
        <a-tooltip :title="$t('ecomPublish.list.tip')">
          <QuestionCircleOutlined class="tip-icon" />
        </a-tooltip>
        <a-popover v-if="settings && !settings.publicUrlConfigured" :title="$t('ecomPublish.image.publicUrlMissing')" placement="bottomLeft" :overlay-style="{ maxWidth: '420px' }">
          <template #content><div class="url-how">{{ $t('ecomPublish.image.publicUrlHow') }}</div></template>
          <a-tag color="warning" class="url-tag"><WarningOutlined /> {{ $t('ecomPublish.list.publicUrlShort') }}</a-tag>
        </a-popover>
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
          <template v-if="column.key === 'title'">
            <div class="product">
              <img v-if="record.images?.length" :src="record.images[0]" class="thumb" alt="" />
              <div class="thumb empty" v-else><PictureOutlined /></div>
              <div class="product-text">
                <a class="cell-ellipsis" :title="record.title" @click="openItem(record)">{{ record.title }}</a>
                <div class="sub-text">{{ record.materialCode }} · {{ record.materialName }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'store'">
            <div>{{ record.storeName }}</div>
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
          </template>
          <template v-else-if="column.key === 'variantCount'">{{ record.variantCount }}</template>
          <template v-else-if="column.key === 'category'">
            <span class="cell-ellipsis" :title="record.categoryName">{{ record.categoryName || record.categoryId || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_publish_status" :value="record.status" />
            <a-tooltip v-if="record.errorMessage" :title="record.errorMessage">
              <div class="error-text">{{ record.errorMessage }}</div>
            </a-tooltip>
            <div v-if="record.retryCount" class="sub-text">{{ $t('ecomPublish.list.retryCount', { n: record.retryCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'platformItemId'">
            <a v-if="record.platformUrl" :href="record.platformUrl" target="_blank" rel="noopener">{{ record.platformItemId || $t('ecomPublish.list.open') }}</a>
            <span v-else>{{ record.platformItemId || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'time'">
            <div>{{ record.publishTime || record.submitTime || record.createTime }}</div>
            <div class="sub-text">{{ timeLabel(record) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <CreateDraftModal ref="createRef" @success="onCreated" />
    <DraftDrawer ref="draftRef" @changed="onDraftChanged" @edit-item="(id) => itemRef.editItem(id)" />
    <ItemEditor ref="itemRef" @success="reload" @open-draft="(id) => draftRef.open(id)" />
    <PublishLogs ref="logsRef" />
    <a-modal v-model:open="resultOpen" :title="$t('ecomPublish.list.resultTitle')" :footer="null" width="min(640px, 100vw)">
      <a-alert type="info" show-icon :message="$t('ecomPublish.list.resultSummary', { ok: result.succeeded, total: result.total })" />
      <a-list v-if="result.errors?.length" size="small" :data-source="result.errors" class="result-list">
        <template #renderItem="{ item }">
          <a-list-item><span class="error-text multi">{{ item.name ? item.name + ': ' : '' }}{{ item.message }}</span></a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CloudUploadOutlined, PictureOutlined, PlusOutlined, QuestionCircleOutlined, StopOutlined, WarningOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { deletePublishItem, offlinePublish, pagePublish, publishCounts, submitPublish } from '@/api/ecom/publish'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { PublishStatus } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import CreateDraftModal from './CreateDraftModal.vue'
import DraftDrawer from './DraftDrawer.vue'
import ItemEditor from './ItemEditor.vue'
import PublishLogs from './PublishLogs.vue'
import { EDITABLE, usePublishSettings } from './publish'

/**
 * 刊登中心：一行是一个刊登草稿在一个店铺的刊登记录，按状态分标签页。从物料 / SPU 新建刊登（一次选多个店铺），
 * 每个店铺的内容单独编辑；提交发布在后台执行（失败显示平台错误，可修改后重试），成功后生成在线商品，可下架
 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_publish_status')

const tab = ref('ALL')
const tabs = computed(() => [{ value: 'ALL', label: t('ecomPublish.list.all') }, ...(dict.ecom_publish_status || []).map((d) => ({ value: d.value, label: d.label }))])
const counts = ref({})
const settings = usePublishSettings()

const columns = [
  { title: t('ecomPublish.list.product'), key: 'title', dataIndex: 'title', width: 340, fixed: 'left' },
  { title: t('ecomPublish.common.store'), key: 'store', dataIndex: 'storeName', width: 170 },
  { title: t('ecomPublish.list.variants'), key: 'variantCount', dataIndex: 'variantCount', width: 80, align: 'right' },
  { title: t('ecomPublish.item.category'), key: 'category', dataIndex: 'categoryName', width: 180 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 220, dict: 'ecom_publish_status' },
  { title: t('ecomPublish.list.platformItemId'), key: 'platformItemId', dataIndex: 'platformItemId', width: 150 },
  { title: t('ecomPublish.list.time'), key: 'time', dataIndex: 'createTime', width: 170 },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-publish')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, platform: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pagePublish, query, {
  params: (q) => ({ ...q, status: tab.value === 'ALL' ? undefined : tab.value })
})
async function loadCounts() {
  counts.value = (await publishCounts({ storeId: query.storeId, platform: query.platform, keyword: query.keyword })) || {}
}
function search() {
  doSearch()
  loadCounts()
}
function reload() {
  selectedKeys.value = []
  load()
  loadCounts()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(() => {
  load()
  loadCounts()
})

function timeLabel(r) {
  if (r.publishTime) return t('ecomPublish.list.publishedAt')
  if (r.submitTime) return t('ecomPublish.list.submittedAt')
  return t('ecomPublish.list.createdAt')
}

const createRef = ref()
const draftRef = ref()
const itemRef = ref()
const logsRef = ref()
function onCreated(draftId) {
  reload()
  draftRef.value.open(draftId)
}
/** 主内容修改 / 增加店铺后：刷新列表，打开着的刊登工作台也重新加载 */
function onDraftChanged() {
  reload()
  itemRef.value?.refresh()
}
const openItem = (r) => (EDITABLE.includes(r.status) ? itemRef.value.openEdit(r) : itemRef.value.openView(r))

// 提交 / 下架的结果：有失败时列出原因
const resultOpen = ref(false)
const result = ref({})
function showResult(res, okKey) {
  if (res.errors?.length) {
    result.value = res
    resultOpen.value = true
  } else {
    message.success(t(okKey, { n: res.succeeded }))
  }
  reload()
}
async function submit(ids) {
  showResult(await submitPublish(ids), 'ecomPublish.list.submitted')
}
function offline(ids) {
  Modal.confirm({
    title: t('ecomPublish.list.offlineConfirm', { n: ids.length }),
    okType: 'danger',
    onOk: async () => showResult(await offlinePublish(ids), 'ecomPublish.list.offlined')
  })
}
function remove(ids) {
  Modal.confirm({
    title: t('ecomPublish.list.deleteConfirm', { n: ids.length }),
    okType: 'danger',
    onOk: async () => {
      await deletePublishItem(ids)
      message.success(t('common.msg.deleteSuccess'))
      reload()
    }
  })
}
const submitSelected = () => submit(selectedKeys.value)
const offlineSelected = () => offline(selectedKeys.value)
const removeSelected = () => remove(selectedKeys.value)

const rowActions = (r) => {
  const editable = EDITABLE.includes(r.status)
  return [
    { type: 'edit', perm: 'ecom:publish:edit', show: editable, onClick: () => itemRef.value.openEdit(r) },
    {
      type: 'submit',
      label: r.status === PublishStatus.FAILED ? t('ecomPublish.list.retry') : t('ecomPublish.list.publish'),
      perm: 'ecom:publish:push',
      show: editable,
      onClick: () => submit([r.id])
    },
    { type: 'view', show: !editable, onClick: () => itemRef.value.openView(r) },
    { key: 'draft', type: 'list', label: t('ecomPublish.list.master'), onClick: () => draftRef.value.open(r.draftId) },
    { type: 'log', label: t('ecomPublish.list.logs'), onClick: () => logsRef.value.open(r) },
    { key: 'offline', label: t('ecomPublish.list.offline'), icon: StopOutlined, perm: 'ecom:publish:push', show: r.status === PublishStatus.SUCCESS, onClick: () => offline([r.id]) },
    { type: 'delete', perm: 'ecom:publish:remove', show: r.status !== PublishStatus.SUCCESS && r.status !== PublishStatus.PUBLISHING && r.status !== PublishStatus.PENDING, onClick: () => remove([r.id]) }
  ]
}
</script>

<style scoped>
.tip-icon {
  align-self: center;
  color: var(--app-text-3);
  cursor: help;
}
.url-tag {
  align-self: center;
  height: 24px;
  margin-inline-end: 0;
  line-height: 22px;
  cursor: help;
}
.url-how {
  white-space: pre-wrap;
}
.count {
  color: var(--app-text-3);
  font-size: 12px;
}
.product {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.product-text {
  min-width: 0;
  flex: 1;
}
.thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex: none;
  border: 1px solid var(--app-border, #f0f0f0);
}
.thumb.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-3);
}
.cell-ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-text {
  color: #cf1322;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.error-text.multi {
  white-space: normal;
  max-width: none;
}
.result-list {
  margin-top: 8px;
}
</style>

<style>
/* 状态标签页只作切换头，按内容高度显示，剩余高度留给表格 */
.ecom-publish .table-card > .ant-card-body > .status-tabs {
  flex: none;
  margin-bottom: 4px;
}
.ecom-publish .table-card > .ant-card-body > .status-tabs > .ant-tabs-nav {
  margin-bottom: 8px;
}
html[data-theme='dark'] .ecom-publish .error-text {
  color: #ff7875;
}
</style>
