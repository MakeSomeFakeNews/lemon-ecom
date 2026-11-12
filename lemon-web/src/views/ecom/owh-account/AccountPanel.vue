<template>
  <SubPage :open="open" :title="account ? $t('ecomOwh.panel.title', { name: account.name }) : ''" @close="open = false">
    <template v-if="account" #extra>
      <DictTag type="ecom_owh_provider" :value="account.provider" />
    </template>

    <a-tabs v-model:active-key="tab">
      <a-tab-pane key="mapping" :tab="$t('ecomOwh.panel.mappings')">
        <div class="tab-tip">{{ $t('ecomOwh.panel.mappingTip') }}</div>
        <div class="tab-toolbar">
          <a-button v-perm="'ecom:owhAccount:mapping'" type="primary" size="small" @click="mappingModal.open(account, null, products)">
            <template #icon><PlusOutlined /></template>{{ $t('ecomOwh.panel.addMapping') }}
          </a-button>
        </div>
        <a-table row-key="id" size="small" :columns="mappingColumns" :data-source="mappings" :loading="loading" :pagination="false" :scroll="{ x: 1000 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'warehouse'">
              <div>{{ record.warehouseName }}</div>
              <div class="sub-text">{{ record.warehouseCode }}</div>
            </template>
            <template v-else-if="column.key === 'owh'">
              <div>{{ record.owhCode }}</div>
              <div class="sub-text">{{ [record.owhName, record.country].filter(Boolean).join(' · ') }}</div>
            </template>
            <template v-else-if="column.key === 'autoPush'">
              <a-tag :bordered="false" :color="record.autoPush === 1 ? 'blue' : 'default'">{{ record.autoPush === 1 ? $t('common.status.yes') : $t('common.status.no') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <DictTag type="sys_common_status" :value="record.status" />
            </template>
            <template v-else-if="column.key === 'action'">
              <TableActions
                :actions="[
                  { type: 'edit', perm: 'ecom:owhAccount:mapping', onClick: () => mappingModal.open(account, record, products) },
                  { type: 'delete', perm: 'ecom:owhAccount:mapping', confirm: $t('ecomOwh.panel.confirmDeleteMapping', { name: record.warehouseName }), onClick: () => removeMapping(record.id) }
                ]"
              />
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="sku" :tab="$t('ecomOwh.sku.title')">
        <SkuTab v-if="account && tab === 'sku'" :account-id="account.id" />
      </a-tab-pane>

      <a-tab-pane key="product" :tab="$t('ecomOwh.panel.products')">
        <div class="tab-tip">{{ $t('ecomOwh.panel.productTip') }}</div>
        <div class="tab-toolbar">
          <a-button v-perm="'ecom:owhAccount:mapping'" size="small" :loading="syncing" @click="syncProducts">
            <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.panel.syncProducts') }}
          </a-button>
        </div>
        <a-table row-key="id" size="small" :columns="productColumns" :data-source="products" :loading="loading" :pagination="false" :scroll="{ x: 1000 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'product'">
              <div>{{ record.productName || '-' }}</div>
              <div class="sub-text">{{ record.productCode }}</div>
            </template>
            <template v-else-if="column.key === 'channel'">
              <ChannelSelect
                v-if="userStore.hasPerm('ecom:owhAccount:mapping')"
                :value="record.channelId"
                :label="record.channelName"
                size="small"
                class="full"
                @update:value="(v) => saveProduct(record, { channelId: v })"
              />
              <span v-else>{{ record.channelName || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'carrier'">
              <a-input
                v-if="userStore.hasPerm('ecom:owhAccount:mapping')"
                :value="record.carrierName"
                size="small"
                :maxlength="64"
                :placeholder="$t('ecomOwh.panel.carrierPh')"
                @change="(e) => (record.carrierName = e.target.value)"
                @blur="saveProduct(record, {})"
              />
              <span v-else>{{ record.carrierName || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <DictTag type="sys_common_status" :value="record.status" />
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <MappingModal ref="mappingModal" @done="afterMapping" />
  </SubPage>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import MappingModal from './MappingModal.vue'
import SkuTab from './SkuTab.vue'
import { deleteOwhMapping, owhMappings, owhProducts, syncOwhProducts, updateOwhProduct } from '@/api/ecom/owh'
import { useAction } from '@/composables/useAction'
import { useUserStore } from '@/store/user'

/** 账号的仓库映射（系统仓库 → 服务商仓库代码）与物流产品（从服务商拉取，可对应物流渠道）。open(account) 打开 */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const userStore = useUserStore()

const open = ref(false)
const tab = ref('mapping')
const account = ref(null)
const mappings = ref([])
const products = ref([])
const loading = ref(false)
const mappingModal = ref()

const mappingColumns = [
  { title: t('ecomOwh.panel.warehouse'), key: 'warehouse', width: 180 },
  { title: t('ecomOwh.panel.owhWarehouse'), key: 'owh', width: 200 },
  { title: t('ecomOwh.panel.defaultProduct'), dataIndex: 'defaultProductCode', width: 160 },
  { title: t('ecomOwh.panel.autoPush'), key: 'autoPush', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(2)
]
const productColumns = [
  { title: t('ecomOwh.panel.product'), key: 'product', width: 220 },
  { title: t('ecomOwh.panel.productWarehouse'), dataIndex: 'owhCode', width: 120 },
  { title: t('ecomOwh.panel.channel'), key: 'channel', width: 200 },
  { title: t('ecomOwh.panel.carrier'), key: 'carrier', width: 160 },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('ecomOwh.panel.syncTime'), dataIndex: 'syncTime', width: 160 }
]

async function load() {
  loading.value = true
  try {
    const [m, p] = await Promise.all([owhMappings(account.value.id), owhProducts(account.value.id)])
    mappings.value = m || []
    products.value = p || []
  } finally {
    loading.value = false
  }
}

function show(record) {
  account.value = record
  tab.value = 'mapping'
  open.value = true
  load()
}

function afterMapping() {
  load()
  emit('changed')
}
const removeMapping = useAction(deleteOwhMapping, { successKey: 'common.msg.deleteSuccess', reload: () => afterMapping() })

const syncing = ref(false)
async function syncProducts() {
  syncing.value = true
  try {
    const n = await syncOwhProducts(account.value.id)
    message.success(t('ecomOwh.panel.productsSynced', { n }))
    load()
  } finally {
    syncing.value = false
  }
}

async function saveProduct(record, patch) {
  Object.assign(record, patch)
  await updateOwhProduct(record.id, { channelId: record.channelId ?? null, carrierName: record.carrierName || null })
  message.success(t('common.msg.saveSuccess'))
}

defineExpose({ open: show })
</script>

<style scoped>
.tab-tip {
  margin-bottom: 8px;
  color: var(--app-text-3);
  font-size: 12px;
}
.tab-toolbar {
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
