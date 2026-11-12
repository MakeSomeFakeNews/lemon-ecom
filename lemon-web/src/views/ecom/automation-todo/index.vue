<template>
  <div class="table-page ecom-automation-todo">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-segmented v-model:value="scope" :options="scopeOptions" @change="search" />
        <a-select v-model:value="query.status" allow-clear :options="toOptions(dict.ecom_auto_todo_status)" :placeholder="$t('ecomAuto.todo.status')"
          class="status" @change="search" />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAuto.todo.keywordPh')" class="kw" @press-enter="search">
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-button v-if="selectedKeys.length" type="primary" @click="markDone(selectedKeys)">
          <template #icon><CheckOutlined /></template>{{ $t('ecomAuto.todo.doneSelected', { n: selectedKeys.length }) }}
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
          <template v-if="column.key === 'title'">
            <a v-if="record.link" class="title" :class="{ done: record.status === AutoTodoStatus.DONE }" @click="router.push(record.link)">{{ record.title }}</a>
            <span v-else class="title" :class="{ done: record.status === AutoTodoStatus.DONE }">{{ record.title }}</span>
            <div class="sub-text ellipsis" :title="record.content">{{ record.content }}</div>
          </template>
          <template v-else-if="column.key === 'ruleName'">
            <div class="ellipsis" :title="record.ruleName">{{ record.ruleName || '-' }}</div>
            <div class="sub-text">{{ record.storeName || '' }}</div>
          </template>
          <template v-else-if="column.key === 'dueDate'">
            <span v-if="record.dueDate" :class="{ overdue: overdue(record) }">{{ record.dueDate }}</span>
            <span v-else class="sub-text">-</span>
            <div v-if="overdue(record)" class="overdue small">{{ $t('ecomAuto.todo.overdue') }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_auto_todo_status" :value="record.status" />
            <div v-if="record.doneTime" class="sub-text">{{ record.doneTime }}</div>
          </template>
          <template v-else-if="column.key === 'ops'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { CheckOutlined, SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { changeAutoTodoStatus, pageAutoTodos } from '@/api/ecom/automation'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { AutoTodoStatus } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'

/**
 * 自动化待办提醒：自动化规则「创建待办」动作生成，默认看自己的未完成待办（看板、首页待办跳转带 ?status=OPEN），
 * 点击标题打开相关页面，标为完成 / 重新打开，可批量完成
 */
const route = useRoute()
const router = useRouter()
const dict = useDict('ecom_auto_todo_status')
const scope = ref('mine')
const scopeOptions = [
  { value: 'mine', label: t('ecomAuto.todo.mine') },
  { value: 'all', label: t('ecomAuto.todo.all') }
]

const columns = [
  { title: t('ecomAuto.todo.title'), key: 'title', dataIndex: 'title', width: 320, fixed: 'left', exportValue: (r) => `${r.title} ${r.content || ''}` },
  { title: t('ecomAuto.todo.rule'), key: 'ruleName', dataIndex: 'ruleName', width: 200 },
  { title: t('ecomAuto.todo.assignee'), dataIndex: 'assigneeName', key: 'assigneeName', width: 110 },
  { title: t('ecomAuto.todo.due'), key: 'dueDate', dataIndex: 'dueDate', width: 120 },
  { title: t('ecomAuto.todo.status'), key: 'status', dataIndex: 'status', width: 120, dict: 'ecom_auto_todo_status' },
  { title: t('ecomAuto.todo.created'), dataIndex: 'createTime', key: 'createTime', width: 160 },
  { ...actionColumn(2), key: 'ops' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-automation-todo')

const query = reactive({ status: route.query.status || AutoTodoStatus.OPEN, keyword: undefined })
const params = (q) => ({ mine: scope.value === 'mine', status: q.status, keyword: q.keyword || undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll, selectedKeys, rowSelection } = useTable(pageAutoTodos, query, { params })

const overdue = (r) => r.status === AutoTodoStatus.OPEN && r.dueDate && dayjs(r.dueDate).isBefore(dayjs(), 'day')

async function markDone(ids, status = AutoTodoStatus.DONE) {
  const n = await changeAutoTodoStatus(ids, status)
  message.success(t('ecomAuto.todo.doneOk', { n }))
  selectedKeys.value = []
  load()
}

const rowActions = (r) => [
  ...(r.link ? [{ type: 'view', label: t('ecomAuto.todo.open'), onClick: () => router.push(r.link) }] : []),
  r.status === AutoTodoStatus.OPEN
    ? { type: 'confirm', label: t('ecomAuto.todo.done'), onClick: () => markDone([r.id]) }
    : { type: 'unaudit', label: t('ecomAuto.todo.reopen'), onClick: () => markDone([r.id], AutoTodoStatus.OPEN) }
]

onMounted(() => {
  load()
  if (route.query.status) router.replace({ query: {} })
})
</script>

<style scoped>
.status {
  width: 130px;
}
.kw {
  width: 220px;
}
.title {
  font-weight: 500;
}
.title.done {
  color: var(--app-text-3);
  text-decoration: line-through;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.overdue {
  color: #cf1322;
}
.small {
  font-size: 12px;
}
</style>

<style>
html[data-theme='dark'] .ecom-automation-todo .overdue {
  color: #ff7875;
}
</style>
