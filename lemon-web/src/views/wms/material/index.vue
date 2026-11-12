<template>
  <div class="workspace">
    <!-- 物料分类树：点击分类列出该分类及其下级分类的物料，有权限时可维护分类 -->
    <a-card v-show="showCategory" :bordered="false" class="page-card workspace-side">
      <MaterialCategoryTree ref="categoryTreeRef" v-model="query.category" @change="search" />
    </a-card>

    <div class="table-page workspace-main">
      <a-card v-show="showSearch" :bordered="false" class="page-card">
        <SearchForm :model="query" @search="search" @reset="resetQuery">
          <a-form-item :label="$t('wms.common.material')" name="keyword">
            <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
          </a-form-item>
          <a-form-item :label="$t('pricing.goods.brand')" name="brand">
            <a-select v-model:value="query.brand" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_brand)" />
          </a-form-item>
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </SearchForm>
      </a-card>

      <a-card :bordered="false" class="page-card table-card">
        <TableToolbar
          :exporter="exportAll"
          v-model:size="size"
          v-model:checked="checked"
          v-model:show-search="showSearch"
          :columns="toolbarColumns"
          @refresh="load"
        >
          <a-button v-perm="'wms:material:add'" type="primary" @click="formRef.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('wms.material.createTitle') }}
          </a-button>
          <AiExtractButton target="wms.material" perm="wms:material:add" :title="$t('wms.material.createTitle')" @result="(d) => formRef.applyAi(d)" />
          <a-button v-perm="'wms:material:add'" @click="importOpen = true">
            <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
          </a-button>
          <a-button v-perm="'wms:material:priceLevel'" @click="priceLevelRef.open()">
            <template #icon><DollarOutlined /></template>{{ $t('pricing.level.title') }}
          </a-button>
          <template v-if="selectedKeys.length">
            <a-divider type="vertical" />
            <a-button @click="openLabels">
              <template #icon><BarcodeOutlined /></template>{{ $t('wms.material.printLabel') }} ({{ selectedKeys.length }})
            </a-button>
            <span v-perm="'wms:material:remove'">
              <a-popconfirm :title="$t('wms.material.confirmDeleteN', { n: selectedKeys.length })" @confirm="remove(selectedKeys)">
                <a-button danger>
                  <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }} ({{ selectedKeys.length }})
                </a-button>
              </a-popconfirm>
            </span>
            <a-button type="link" @click="selectedKeys = []">{{ $t('common.action.cancel') }}</a-button>
          </template>
          <template #extra>
            <a-tooltip :title="showCategory ? $t('wms.material.hideCategory') : $t('wms.material.showCategory')">
              <a-button type="text" @click="showCategory = !showCategory">
                <template #icon><MenuFoldOutlined v-if="showCategory" /><MenuUnfoldOutlined v-else /></template>
              </a-button>
            </a-tooltip>
          </template>
        </TableToolbar>

        <a-table
          row-key="id"
          :size="size"
          :columns="tableColumns"
          :data-source="list"
          :loading="loading"
          :pagination="pagination"
          :row-selection="rowSelection"
          :scroll="{ x: sumWidth(tableColumns) + 32, y: '100%' }"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="name-cell">
                <a-image v-if="record.imageUrl" :src="record.imageUrl" :width="36" :height="36" class="thumb" />
                <div v-else class="thumb-empty"><PictureOutlined /></div>
                <div class="name-text">
                  <div class="name-row">
                    <a class="name-link" :title="record.name" @click="detailRef.open(record.id)">{{ record.name }}</a>
                    <a-tag v-if="record.isTemplate === 1" color="magenta" :bordered="false" class="ml">{{ $t('wms.material.style') }}</a-tag>
                  </div>
                  <div class="sub-text name-link">
                    {{ record.code }}
                    <template v-if="record.variantText"> · {{ record.variantText }}</template>
                  </div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'category'">
              <a-tooltip :title="categoryTitle(record.category)">
                <span class="tag-ellipsis"><DictTag type="wms_material_category" :value="record.category" /></span>
              </a-tooltip>
            </template>
            <template v-else-if="column.key === 'brand'">
              <DictTag type="wms_material_brand" :value="record.brand" />
            </template>
            <template v-else-if="column.priceLevel != null">{{ record[column.key] == null ? '-' : price(record[column.key]) }}</template>
            <template v-else-if="column.key === 'tracking'">
              <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">{{ $t('wms.common.batch') }}</a-tag>
              <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">{{ $t('wms.common.serialNo') }}</a-tag>
              <a-tag v-if="record.iqcRequired === 1" color="orange" :bordered="false">{{ $t('wms.material.iqc') }}</a-tag>
              <span v-if="record.batchEnabled !== 1 && record.serialEnabled !== 1 && record.iqcRequired !== 1" class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'safetyStock'">{{ formatQty(record.safetyStock) }}</template>
            <template v-else-if="column.key === 'barcode'">{{ record.barcode || '-' }}</template>
            <template v-else-if="column.key === 'status'">
              <StatusSwitch
                :value="record.status"
                :name="record.name"
                :api="(s) => changeMaterialStatus(record.id, s)"
                :disabled="!userStore.hasPerm('wms:material:edit')"
                @changed="(s) => (record.status = s)"
              />
            </template>
            <template v-else-if="column.custom">{{ cfCell(record, column) }}</template>
            <template v-else-if="column.key === 'action'">
              <TableActions :actions="rowActions(record)" />
            </template>
          </template>
        </a-table>
      </a-card>

      <MaterialForm ref="formRef" @saved="onSaved" />
      <VariantDrawer v-model:open="variantOpen" :material="variantMaterial" @generated="load" />

      <MaterialDetail ref="detailRef" @edit="(id) => formRef.openEdit({ id })" @images-changed="load" />
      <ImportModal
        v-model:open="importOpen"
        :title="$t('wms.common.material')"
        :columns="importColumns"
        :importer="importMaterial"
        :sample="{ code: 'M0001', name: $t('wms.material.sampleName'), spec: 'M3*8', unit: $t('wms.material.sampleUnit'), batchEnabled: $t('wms.material.sampleNo'), serialEnabled: $t('wms.material.sampleNo'), safetyStock: 100 }"
        @success="onImported"
      />
      <MaterialUnitsModal v-model:open="unitsOpen" :material="unitsMaterial" :editable="canEdit" />
      <RecordExtrasModal
        v-model:open="extrasOpen"
        :title="extrasTitle"
        biz-type="wms_material"
        :biz-id="extrasId"
        :editable="userStore.hasPerm('wms:material:edit')"
      />
      <LabelPrintModal v-model:open="labelOpen" type-key="label.material" :items="labelItems" />
      <PriceLevelModal ref="priceLevelRef" @saved="load" />
      <StockHistory ref="historyRef" />
      <MaterialImagesModal ref="imagesRef" :editable="canEdit" @changed="load" />
      <MaterialTrackingModal ref="trackingRef" @saved="load" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { AppstoreAddOutlined, BarcodeOutlined, DeleteOutlined, DollarOutlined, ImportOutlined, MenuFoldOutlined, MenuUnfoldOutlined, PaperClipOutlined, PictureOutlined, PlusOutlined, SafetyCertificateOutlined, SwapOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LabelPrintModal from '@/components/label/LabelPrintModal.vue'
import MaterialCategoryTree from '@/components/wms/MaterialCategoryTree.vue'
import MaterialUnitsModal from '@/components/wms/MaterialUnitsModal.vue'
import StockHistory from '@/components/wms/StockHistory.vue'
import VariantDrawer from '@/components/wms/VariantDrawer.vue'
import { changeMaterialStatus, deleteMaterial, pageMaterial, importMaterial } from '@/api/wms/material'
import { formatQty } from '@/components/wms/wms'
import { sumWidth, useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'
import { useCustomColumns } from '@/composables/useCustomFields'
import { PRICE_FIELDS, usePriceLevels } from '@/components/wms/priceLevels'
import { price } from '@/utils/number'
import PriceLevelModal from './PriceLevelModal.vue'
import MaterialImagesModal from './MaterialImagesModal.vue'
import MaterialTrackingModal from './MaterialTrackingModal.vue'
import MaterialForm from './MaterialForm.vue'
import MaterialDetail from './MaterialDetail.vue'
import AiExtractButton from '@/components/ai/AiExtractButton.vue'
import { load as loadPref, save as savePref } from '@/utils/storage'
const { t } = useI18n()

/** 物料管理：物料主数据只有这一份，仓库、采购、销售共用 */
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:material:edit'))
// 左侧分类树：显示 / 隐藏记在本机
const showCategory = ref(loadPref('material-page:show-category', true))
watch(showCategory, (v) => savePref('material-page:show-category', v))
const categoryTreeRef = ref()
/** 物料有增删改、导入后刷新分类树上的物料数 */
const refreshCounts = () => categoryTreeRef.value?.refresh()
const dict = useDict('sys_common_status', 'wms_material_category', 'wms_material_brand')
const categoryTitle = (v) => findDict(dict.wms_material_category, v)?.label || v
const importOpen = ref(false)
// 价格列：停用的列在表单、列表、导入导出中隐藏
const { enabledLevels } = usePriceLevels()
const priceLevelRef = ref()
// 辅助单位
const unitsOpen = ref(false)
const unitsMaterial = ref()
// 多规格：生成 SKU、查看库存矩阵
const variantOpen = ref(false)
const variantMaterial = ref()
function openVariants(record) {
  variantMaterial.value = { ...record }
  variantOpen.value = true
}

// 经营历史（库存流水的进出、单价与结余）
const historyRef = ref()
function openHistory(record) {
  historyRef.value.open({ materialId: record.id, materialCode: record.code, materialName: record.name })
}

function openUnits(record) {
  unitsMaterial.value = record
  unitsOpen.value = true
}
// 附件、变更记录
const extrasOpen = ref(false)
const extrasId = ref()
const extrasTitle = ref('')
function openExtras(record) {
  extrasId.value = record.id
  extrasTitle.value = `${record.code} ${record.name}`
  extrasOpen.value = true
}
const importColumns = computed(() => [
  { title: t('wms.material.code'), key: 'code', required: true },
  { title: t('wms.material.name'), key: 'name', required: true },
  { title: t('wms.inout.spec'), key: 'spec' },
  { title: t('wms.material.unit'), key: 'unit' },
  // 分类可填编码、完整路径（如「电子元器件/电阻」）或不重名的分类名称，由后端匹配
  { title: t('wms.inout.category'), key: 'category' },
  { title: t('pricing.goods.brand'), key: 'brand', dict: 'wms_material_brand' },
  { title: t('wms.material.batchManaged'), key: 'batchEnabled', type: 'bool' },
  { title: t('wms.material.serialManaged'), key: 'serialEnabled', type: 'bool' },
  { title: t('wms.material.iqc'), key: 'iqcRequired', type: 'bool' },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', type: 'number' },
  ...enabledLevels.value.map((l) => ({ title: l.name, key: l.field, type: 'number' })),
  { title: t('common.field.remark'), key: 'remark' }
])

const columns = [
  { title: t('wms.common.material'), key: 'name', width: 240, fixed: 'left' },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 150, ellipsis: true },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70, align: 'center' },
  { title: t('wms.units.barcode'), key: 'barcode', width: 140, ellipsis: true },
  { title: t('wms.inout.category'), key: 'category', width: 170, dict: 'wms_material_category' },
  { title: t('pricing.goods.brand'), key: 'brand', width: 100, dict: 'wms_material_brand' },
  ...PRICE_FIELDS.map((key, level) => ({ title: key, key, dataIndex: key, priceLevel: level, width: 100, align: 'right' })),
  { title: t('wms.material.tracking'), key: 'tracking', width: 120 },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 编辑、删除直接显示；资料、单位、变更记录、款式变体在「更多」中
  actionColumn(3)
]
const { checked } = useColumns(columns, 'wms-material')
/** 列设置、导出用的列：只有启用的价格列，标题为价格列名称 */
const toolbarColumns = computed(() => {
  const enabled = new Map(enabledLevels.value.map((l) => [l.level, l.name]))
  return columns.filter((c) => c.priceLevel == null || enabled.has(c.priceLevel))
    .map((c) => (c.priceLevel == null ? c : { ...c, title: enabled.get(c.priceLevel) }))
})
const visibleColumns = computed(() => toolbarColumns.value.filter((c) => checked.value.includes(c.key || c.dataIndex)))
const showSearch = ref(true)

const query = reactive({ keyword: undefined, category: undefined, brand: undefined, status: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageMaterial, query)
// 自定义字段：勾选「列表显示」的作为列
const { columns: cfColumns, cell: cfCell } = useCustomColumns('wms_material', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, category: undefined, brand: undefined, status: undefined })
  search()
}

onMounted(load)


const formRef = ref()
const detailRef = ref()
const imagesRef = ref()
const trackingRef = ref()
/** 新增 / 编辑保存后：刷新列表、分类树物料数，详情打开着时重新加载 */
function onSaved() {
  load()
  refreshCounts()
  detailRef.value?.reload()
}

const rowActions = (record) => [
  { type: 'edit', show: canEdit.value, onClick: () => formRef.value.openEdit(record) },
  { type: 'delete', perm: 'wms:material:remove', confirm: t('wms.material.confirmDelete', { name: record.name }), onClick: () => remove([record.id]) },
  { key: 'copy', type: 'copy', perm: 'wms:material:add', more: true, onClick: () => formRef.value.copy(record) },
  { key: 'images', label: t('wms.materialImage.action'), icon: PictureOutlined, more: true, onClick: () => imagesRef.value.open(record) },
  { key: 'tracking', label: t('wms.tracking.action'), icon: SafetyCertificateOutlined, perm: 'wms:material:edit', show: record.isTemplate !== 1, more: true, onClick: () => trackingRef.value.open(record) },
  { key: 'extras', label: t('wms.material.extras'), icon: PaperClipOutlined, more: true, onClick: () => openExtras(record) },
  { key: 'units', label: t('common.field.unit'), icon: SwapOutlined, more: true, onClick: () => openUnits(record) },
  { key: 'history', type: 'history', label: t('wms.history.action'), show: record.isTemplate !== 1, more: true, onClick: () => openHistory(record) },
  { key: 'variants', label: t('wms.variant.title'), icon: AppstoreAddOutlined, show: !record.templateId, more: true, onClick: () => openVariants(record) }
]


async function remove(ids) {
  await deleteMaterial(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
  refreshCounts()
}

function onImported() {
  search()
  refreshCounts()
}

// 条码标签：选中的行（当前页）
const labelOpen = ref(false)
const labelItems = ref([])
function openLabels() {
  labelItems.value = list.value.filter((r) => selectedKeys.value.includes(r.id)).map((r) => ({ code: r.code, name: r.name, spec: r.spec, unit: r.unit, barcode: r.barcode }))
  labelOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ml {
  margin-left: 6px;
}
.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.name-cell :deep(.thumb) {
  object-fit: cover;
  border-radius: 4px;
}
.thumb-empty {
  flex: none;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: var(--app-hover);
  color: var(--app-text-3);
}
.name-text {
  min-width: 0;
  flex: 1;
}
.name-row {
  display: flex;
  align-items: center;
  min-width: 0;
}
.name-link {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tag-ellipsis {
  display: inline-block;
  max-width: 100%;
  vertical-align: middle;
}
.tag-ellipsis :deep(.ant-tag) {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
