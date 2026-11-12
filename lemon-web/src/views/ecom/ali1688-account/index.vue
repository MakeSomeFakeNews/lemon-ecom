<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon class="tip">
        <template #message>
          {{ $t('ecomDev.account.tip') }}
          <div class="callback">{{ $t('ecomDev.account.callback') }} <code>{{ callbackUrl }}</code></div>
        </template>
      </a-alert>
      <TableToolbar v-model:size="size" @refresh="load">
        <a-button v-perm="'ecom:ali1688Account:edit'" type="primary" @click="formRef.openCreate({ isDefault: list.length ? 0 : 1 })">
          <template #icon><PlusOutlined /></template>{{ $t('ecomDev.account.add') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 1200, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            {{ record.name }} <a-tag v-if="record.isDefault === 1" color="gold">{{ $t('ecomDev.account.default') }}</a-tag>
            <div class="sub-text">AppKey {{ record.appKey }}</div>
          </template>
          <template v-else-if="column.key === 'auth'">
            <template v-if="record.authorized">
              <a-tag color="green">{{ $t('ecomDev.account.authorized') }}</a-tag>
              <div class="sub-text">{{ record.loginId || record.memberId }} · {{ $t('ecomDev.account.expire', { time: record.tokenExpire || '-' }) }}</div>
            </template>
            <a-tag v-else color="orange">{{ $t('ecomDev.account.unauthorized') }}</a-tag>
            <div v-if="record.lastMessage" class="err">{{ record.lastMessage }}</div>
          </template>
          <template v-else-if="column.key === 'address'">
            <span v-if="record.receiverName">{{ record.receiverName }} {{ record.receiverMobile }}</span>
            <div class="sub-text">{{ [record.province, record.city, record.district, record.town, record.address].filter(Boolean).join(' ') }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <AccountForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DisconnectOutlined, KeyOutlined, PlusOutlined, SyncOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { ali1688AuthorizeUrl, deleteAli1688Account, listAli1688Account, refreshAli1688Token, revokeAli1688 } from '@/api/ecom/dev'
import { useAction } from '@/composables/useAction'
import AccountForm from './AccountForm.vue'

/**
 * 1688 账号：开放平台应用的 AppKey / AppSecret（加密保存）、OAuth 授权（新窗口登录 1688 同意授权，回调后自动保存令牌）、
 * 刷新 / 清除授权、默认收货地址（一键下单使用）
 */
const { t } = useI18n()
const size = ref('middle')
const list = ref([])
const loading = ref(false)
const callbackUrl = computed(() => `${window.location.origin}/api/ecom/oauth/callback/alibaba1688`)

const columns = [
  { title: t('ecomDev.account.name'), key: 'name', width: 220 },
  { title: t('ecomDev.account.auth'), key: 'auth', width: 280 },
  { title: t('ecomDev.account.receiver'), key: 'address', width: 300 },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 160, align: 'right', fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    list.value = await listAli1688Account()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const formRef = ref()

async function authorize(r) {
  const win = window.open('about:blank', '_blank')
  try {
    const url = await ali1688AuthorizeUrl(r.id)
    if (win) win.location.href = url
    else window.open(url, '_blank')
    message.info(t('ecomDev.account.authorizeOpened'))
  } catch (e) {
    win?.close()
    throw e
  }
}
const refresh = useAction(refreshAli1688Token, { successKey: 'ecomDev.account.refreshed', reload: () => load() })
const revoke = useAction(revokeAli1688, { confirm: () => t('ecomDev.account.revokeConfirm'), successKey: 'ecomDev.account.revoked', reload: () => load() })
const remove = useAction(deleteAli1688Account, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const rowActions = (r) => [
  { key: 'auth', label: r.authorized ? t('ecomDev.account.reauthorize') : t('ecomDev.account.authorize'), icon: KeyOutlined, perm: 'ecom:ali1688Account:edit', onClick: () => authorize(r) },
  { type: 'edit', perm: 'ecom:ali1688Account:edit', onClick: () => formRef.value.openEdit(r) },
  { key: 'refresh', label: t('ecomDev.account.refresh'), icon: SyncOutlined, perm: 'ecom:ali1688Account:edit', show: !!r.authorized, more: true, onClick: () => refresh(r.id) },
  { key: 'revoke', label: t('ecomDev.account.revoke'), icon: DisconnectOutlined, perm: 'ecom:ali1688Account:edit', show: !!r.authorized, more: true, onClick: () => revoke(r.id) },
  { type: 'delete', perm: 'ecom:ali1688Account:edit', more: true, confirm: { title: t('ecomDev.account.deleteConfirm', { name: r.name }), okType: 'danger' }, onClick: () => remove(r.id) }
]
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.callback {
  margin-top: 4px;
  font-size: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.err {
  color: #cf1322;
  font-size: 12px;
}
</style>
