<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomFba.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomFba.common.allStores')" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomFba.report.reportType')" name="reportType">
          <a-select v-model:value="query.reportType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_amz_report_type)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_amz_report_status)" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:amzReport:add'" type="primary" @click="createRef.open(query.storeId)">
          <template #icon><PlusOutlined /></template>{{ $t('ecomFba.report.create') }}
        </a-button>
        <a-button v-perm="'ecom:amzReport:add'" @click="uploadRef.open(query.storeId)">
          <template #icon><UploadOutlined /></template>{{ $t('ecomFba.common.uploadReport') }}
        </a-button>
        <a-tooltip :title="$t('ecomFba.report.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
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
          <template v-if="column.key === 'reportType'"><DictTag type="ecom_amz_report_type" :value="record.reportType" /></template>
          <template v-else-if="column.key === 'status'">
            <a-tooltip :title="record.message || undefined">
              <span class="status-cell">
                <DictTag type="ecom_amz_report_status" :value="record.status" />
                <LoadingOutlined v-if="isRunning(record)" class="spin" />
                <ExclamationCircleOutlined v-else-if="record.status === AmzReportStatus.FAILED && record.message" class="err" />
              </span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'range'">
            <span v-if="record.dataStart">{{ record.dataStart }} ~ {{ record.dataEnd }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'uploaded'">
            <a-tag v-if="record.uploaded" color="blue" :bordered="false">{{ $t('ecomFba.report.uploaded') }}</a-tag>
            <span v-else class="sub-text">API</span>
          </template>
          <template v-else-if="column.key === 'message'">
            <span :class="{ err: record.status === AmzReportStatus.FAILED }" :title="record.message">{{ record.message || '' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <CreateReportModal ref="createRef" @success="search" />
    <AmzReportUploadModal ref="uploadRef" @success="search" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ExclamationCircleOutlined, InfoCircleOutlined, LoadingOutlined, PlusOutlined, SyncOutlined, UploadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import AmzReportUploadModal from '@/components/ecom/AmzReportUploadModal.vue'
import CreateReportModal from './CreateReportModal.vue'
import { deleteAmzReport, pageAmzReport, pollAmzReport } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { AmzReportStatus, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'
import { isRunning } from '../fba-health/fba'

/**
 * 亚马逊报告任务：创建（Reports API createReport，定时任务轮询下载解析）、上传报告文件、刷新状态
 */
const dict = useDict('ecom_amz_report_type', 'ecom_amz_report_status')

const columns = [
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 160, ellipsis: true },
  { title: t('ecomFba.report.reportType'), key: 'reportType', dataIndex: 'reportType', width: 150, dict: 'ecom_amz_report_type' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_amz_report_status' },
  { title: t('ecomFba.report.dataRange'), key: 'range', width: 200, exportValue: (r) => (r.dataStart ? `${r.dataStart} ~ ${r.dataEnd}` : '') },
  { title: t('ecomFba.report.reportId'), dataIndex: 'reportId', width: 140, ellipsis: true },
  { title: t('ecomFba.report.uploaded'), key: 'uploaded', dataIndex: 'uploaded', width: 80 },
  { title: t('ecomFba.report.rowCount'), dataIndex: 'rowCount', width: 80, align: 'right' },
  { title: t('ecomFba.report.appliedCount'), dataIndex: 'appliedCount', width: 80, align: 'right' },
  { title: t('ecomFba.report.pollCount'), dataIndex: 'pollCount', width: 90, align: 'right' },
  { title: t('ecomFba.report.nextPollTime'), dataIndex: 'nextPollTime', width: 160 },
  { title: t('ecomFba.report.message'), key: 'message', dataIndex: 'message', width: 260, ellipsis: true },
  { title: t('ecomFba.report.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 160 },
  { title: t('ecomFba.report.finishTime'), dataIndex: 'finishTime', width: 160 },
  actionColumn(2, [t('ecomFba.report.poll')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-amz-report')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, reportType: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageAmzReport, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const createRef = ref()
const uploadRef = ref()
const poll = useAction(pollAmzReport, { successKey: false, reload: () => load() })
const remove = useAction(deleteAmzReport, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { key: 'poll', label: t('ecomFba.report.poll'), icon: SyncOutlined, show: isRunning(record), onClick: () => poll(record.id) },
  { type: 'delete', perm: 'ecom:amzReport:remove', confirm: t('ecomFba.report.deleteConfirm'), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.spin {
  color: var(--app-primary);
}
.err {
  color: var(--status-critical, #cf1322);
}
</style>
