<template>
  <div class="tab-body">
    <SearchForm v-show="showSearch" :model="query" @search="search" @reset="resetQuery">
      <a-form-item :label="$t('common.field.keyword')" name="keyword">
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomOper.auth.keywordPh')" @press-enter="search" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.common.platform')" name="platform">
        <a-select v-model:value="query.platform" allow-clear :placeholder="$t('ecomOper.common.allPlatforms')" :options="toOptions(dict.ecom_platform)" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.auth.user')" name="userId">
        <IdentityUserSelect v-model:value="query.userId" />
      </a-form-item>
    </SearchForm>
    <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load" />
    <a-table
      row-key="storeId"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'store'">
          <div class="store-cell">
            <PlatformAvatar :platform="record.platform" :size="28" />
            <div class="store-text">
              <div class="ellipsis" :title="record.storeName">{{ record.storeName }}</div>
              <div class="sub-text ellipsis">{{ record.storeCode }}<span v-if="record.status === STATUS_DISABLED"> · {{ findDict(dict.sys_common_status, record.status)?.label }}</span></div>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'platform'">
          <DictTag type="ecom_platform" :value="record.platform" /> {{ record.marketplace }}
        </template>
        <template v-else-if="column.key === 'members'">
          <template v-if="!record.members?.length">
            <span class="sub-text">{{ $t('ecomOper.auth.noMembers') }}</span>
            <a v-if="userStore.hasPerm('ecom:storeAuth:edit')" class="sub-link" @click="modal.open(record)"><PlusOutlined /> {{ $t('common.action.add') }}</a>
          </template>
          <a-tag v-for="m in record.members" :key="m.id" :color="roleColor(m.memberRole)" class="member">
            {{ m.nickname || m.username }} · {{ findDict(dict.ecom_store_member_role, m.memberRole)?.label || m.memberRole }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="[{ type: 'edit', label: $t('ecomOper.auth.edit'), perm: 'ecom:storeAuth:edit', onClick: () => modal.open(record) }]" />
        </template>
      </template>
    </a-table>
    <MembersModal ref="modal" @success="load" />
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageStoreAuth } from '@/api/ecom/oper'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { STATUS_DISABLED } from '@/utils/constants'
import MembersModal from './MembersModal.vue'
import PlatformAvatar from '../store/PlatformAvatar.vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/user'

/** 按店铺：店铺、负责人与授权成员（岗位颜色取字典），修改授权打开成员弹窗。ref 调用 reload() */
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('ecom_platform', 'ecom_store_member_role', 'sys_common_status')
const roleColor = (role) => findDict(dict.ecom_store_member_role, role)?.tagColor || 'default'

const columns = [
  { title: t('ecomOper.common.store'), key: 'store', dataIndex: 'storeName', width: 240, fixed: 'left' },
  { title: t('ecomOper.common.platform'), key: 'platform', dataIndex: 'platform', width: 150, dict: 'ecom_platform' },
  { title: t('ecomOper.common.owner'), dataIndex: 'ownerName', width: 120 },
  { title: t('ecomOper.auth.members'), key: 'members', width: 460, exportValue: (r) => (r.members || []).map((m) => m.nickname || m.username).join(', ') },
  actionColumn(1, [t('ecomOper.auth.edit')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-store-auth')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, platform: undefined, userId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageStoreAuth, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
watch(() => query.userId, search)

const modal = ref()
load()
defineExpose({ reload: () => load() })
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.member {
  margin-bottom: 4px;
}
.store-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.store-text {
  flex: 1;
  min-width: 0;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-link {
  margin-left: 8px;
  font-size: 13px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
