<template>
  <a-modal v-model:open="open" :title="$t('ecomOper.auth.editTitle', { name: store?.storeName || '' })" :confirm-loading="saving" width="min(760px, 100vw)" @ok="submit">
    <div v-if="store?.ownerName" class="sub-text owner">{{ $t('ecomOper.auth.ownerHint', { name: store.ownerName }) }}</div>
    <a-table :row-key="(r) => r.key" size="small" :columns="columns" :data-source="rows" :pagination="false">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'userId'">
          <IdentityUserSelect v-model:value="record.userId" :label="record.nickname" />
        </template>
        <template v-else-if="column.key === 'memberRole'">
          <a-select v-model:value="record.memberRole" :options="optionsWithCurrent(dict.ecom_store_member_role, record.memberRole)" class="full" />
        </template>
        <template v-else-if="column.key === 'remark'">
          <a-input v-model:value="record.remark" :maxlength="255" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="text" danger size="small" @click="rows.splice(index, 1)"><template #icon><DeleteOutlined /></template></a-button>
        </template>
      </template>
    </a-table>
    <a-button type="dashed" block class="add" @click="addRow"><template #icon><PlusOutlined /></template>{{ $t('ecomOper.auth.addMember') }}</a-button>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { saveStoreMembers } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { optionsWithCurrent } from '@/utils/dict'

/** 店铺授权成员弹窗：增删成员（用户 + 岗位 + 备注），保存时整体替换。ref 调用 open(storeAuthRow) */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_store_member_role')

const columns = [
  { title: t('ecomOper.auth.user'), key: 'userId', width: 240 },
  { title: t('ecomOper.auth.memberRole'), key: 'memberRole', width: 140 },
  { title: t('ecomOper.auth.remark'), key: 'remark' },
  { title: '', key: 'action', width: 50 }
]
const open = ref(false)
const saving = ref(false)
const store = ref()
const rows = ref([])
let seq = 0

const defaultRole = () => (dict.ecom_store_member_role || [])[0]?.value
function addRow() {
  rows.value.push({ key: ++seq, userId: undefined, nickname: undefined, memberRole: defaultRole(), remark: '' })
}
function show(row) {
  store.value = row
  rows.value = (row.members || []).map((m) => ({ key: ++seq, userId: m.userId, nickname: m.nickname || m.username, memberRole: m.memberRole, remark: m.remark || '' }))
  if (!rows.value.length) addRow()
  open.value = true
}

async function submit() {
  const list = rows.value.filter((r) => r.userId != null)
  const seen = new Set()
  for (const r of list) {
    if (seen.has(r.userId)) return message.warning(t('ecomOper.auth.duplicateUser', { name: r.nickname || r.userId }))
    seen.add(r.userId)
    if (!r.memberRole) return message.warning(t('common.rule.requiredSelect', { name: t('ecomOper.auth.memberRole') }))
  }
  saving.value = true
  try {
    await saveStoreMembers(store.value.storeId, { members: list.map((r) => ({ userId: r.userId, memberRole: r.memberRole, remark: r.remark || null })) })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.owner {
  margin-bottom: 8px;
}
.add {
  margin-top: 8px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
