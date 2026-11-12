<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomAsst.word.word')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAsst.word.keywordPh')" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomAsst.word.wordType')" name="wordType">
          <a-select v-model:value="query.wordType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_banned_word_type)" />
        </a-form-item>
        <a-form-item :label="$t('ecomAsst.word.level')" name="level">
          <a-select v-model:value="query.level" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_banned_word_level)" />
        </a-form-item>
        <a-form-item :label="$t('ecomAsst.common.platform')" name="platform">
          <a-select v-model:value="query.platform" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('ecomAsst.common.marketplace')" name="marketplace">
          <a-select v-model:value="query.marketplace" allow-clear show-search :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_marketplace)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:bannedWord:add'" type="primary" @click="formRef.open()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomAsst.word.add') }}
        </a-button>
        <a-button v-perm="'ecom:bannedWord:add'" @click="batchRef.open()">
          <template #icon><UnorderedListOutlined /></template>{{ $t('ecomAsst.word.batch') }}
        </a-button>
        <a-button @click="checkRef.open()">
          <template #icon><SafetyCertificateOutlined /></template>{{ $t('ecomAsst.word.check') }}
        </a-button>
        <template v-if="selectedKeys.length">
          <span v-perm="'ecom:bannedWord:remove'">
            <a-popconfirm :title="$t('ecomAsst.word.batchDeleteConfirm', { n: selectedKeys.length })" @confirm="removeSelected">
              <a-button danger><template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}</a-button>
            </a-popconfirm>
          </span>
          <span class="selected-tip">{{ $t('common.table.selected', { n: selectedKeys.length }) }} <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a></span>
        </template>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: scrollX + 32, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'word'">
            <b class="word">{{ record.word }}</b>
            <a-tag v-if="record.builtin" class="builtin">{{ $t('ecomAsst.word.builtin') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'wordType'"><DictTag type="ecom_banned_word_type" :value="record.wordType" /></template>
          <template v-else-if="column.key === 'level'"><DictTag type="ecom_banned_word_level" :value="record.level" /></template>
          <template v-else-if="column.key === 'matchMode'"><DictTag type="ecom_banned_word_match" :value="record.matchMode" /></template>
          <template v-else-if="column.key === 'platform'">
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
            <span v-else class="sub-text">{{ $t('ecomAsst.word.allPlatforms') }}</span>
          </template>
          <template v-else-if="column.key === 'marketplace'">
            <span v-if="record.marketplace">{{ record.marketplace }}</span>
            <span v-else class="sub-text">{{ $t('ecomAsst.word.allMarketplaces') }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch :value="record.status" :name="record.word" :api="(s) => changeBannedWordStatus(record.id, s)" :disabled="!canEdit" @changed="(s) => (record.status = s)" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <WordForm ref="formRef" @success="load" />
    <BatchModal ref="batchRef" @success="load" />
    <CheckModal ref="checkRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined, SafetyCertificateOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { changeBannedWordStatus, deleteBannedWord, pageBannedWord } from '@/api/ecom/assistant'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import WordForm from './WordForm.vue'
import BatchModal from './BatchModal.vue'
import CheckModal from './CheckModal.vue'

/**
 * 违禁词库：按平台 / 站点维护禁用词、侵权品牌词、夸大宣传词、医疗功效词（类型为业务字典），处理方式为阻止发布或只提醒，
 * 整词或包含匹配。刊登编辑时实时检查并高亮命中词，提交发布时命中阻止类的词不能发布。内置一批常见词，可停用、修改或补充
 */
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('ecom:bannedWord:edit'))
const dict = useDict('ecom_banned_word_type', 'ecom_banned_word_level', 'ecom_banned_word_match', 'ecom_platform', 'ecom_marketplace', 'sys_common_status')

const columns = [
  { title: t('ecomAsst.word.word'), key: 'word', dataIndex: 'word', width: 200, fixed: 'left' },
  { title: t('ecomAsst.word.wordType'), key: 'wordType', dataIndex: 'wordType', width: 120, dict: 'ecom_banned_word_type' },
  { title: t('ecomAsst.word.level'), key: 'level', dataIndex: 'level', width: 100, dict: 'ecom_banned_word_level' },
  { title: t('ecomAsst.word.matchMode'), key: 'matchMode', dataIndex: 'matchMode', width: 90, dict: 'ecom_banned_word_match' },
  { title: t('ecomAsst.common.platform'), key: 'platform', dataIndex: 'platform', width: 120, dict: 'ecom_platform' },
  { title: t('ecomAsst.common.marketplace'), key: 'marketplace', dataIndex: 'marketplace', width: 90 },
  { title: t('ecomAsst.word.suggestion'), dataIndex: 'suggestion', width: 320, ellipsis: true },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-banned-word')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, wordType: undefined, level: undefined, platform: undefined, marketplace: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll, selectedKeys, rowSelection } = useTable(pageBannedWord, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
// 下拉筛选变化即查询
watch(() => [query.wordType, query.level, query.platform, query.marketplace, query.status], () => search())

const formRef = ref()
const batchRef = ref()
const checkRef = ref()
const remove = useAction(deleteBannedWord, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
async function removeSelected() {
  await remove(selectedKeys.value)
  selectedKeys.value = []
}
const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:bannedWord:edit', onClick: () => formRef.value.open(r) },
  { type: 'delete', perm: 'ecom:bannedWord:remove', confirm: { title: t('ecomAsst.word.deleteConfirm', { word: r.word }), okType: 'danger' }, onClick: () => remove(r.id) }
]

onMounted(load)
</script>

<style scoped>
.word {
  font-weight: 600;
}
.builtin {
  margin-left: 6px;
  font-size: 11px;
  line-height: 16px;
}
.sub-text {
  color: var(--app-text-3);
}
.selected-tip {
  color: var(--app-text-2);
}
</style>
