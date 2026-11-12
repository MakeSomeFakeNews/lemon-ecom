<template>
  <div>
    <div class="tab-tip">{{ $t('ecomOwh.sku.tip') }}</div>
    <div class="tab-toolbar">
      <a-input-search v-model:value="keyword" :placeholder="$t('ecomOwh.sku.keywordPh')" allow-clear class="search" @search="load" />
      <template v-if="userStore.hasPerm('ecom:owhAccount:mapping')">
        <MaterialSelect v-model:value="form.materialId" class="material" />
        <a-input v-model:value="form.owhSku" :placeholder="$t('ecomOwh.sku.owhSku')" :maxlength="64" class="sku" @press-enter="save" />
        <a-button type="primary" :loading="saving" @click="save"><template #icon><PlusOutlined /></template>{{ $t('ecomOwh.sku.save') }}</a-button>
      </template>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="{ pageSize: 20 }" :scroll="{ x: 700 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }}</div>
          <div class="sub-text">{{ record.materialName }} {{ record.materialSpec || '' }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'edit', perm: 'ecom:owhAccount:mapping', onClick: () => edit(record) },
              { type: 'delete', perm: 'ecom:owhAccount:mapping', confirm: $t('ecomOwh.sku.confirmDelete', { sku: record.owhSku }), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { deleteOwhSku, owhSkus, saveOwhSku } from '@/api/ecom/owh'
import { useAction } from '@/composables/useAction'
import { useUserStore } from '@/store/user'

/** 账号的 SKU 映射（物料 ↔ 服务商 SKU，没有映射的物料按物料编码）。同一物料再次保存即修改 */
const props = defineProps({ accountId: Number })
const { t } = useI18n()
const userStore = useUserStore()

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const form = reactive({ materialId: undefined, owhSku: '' })

const columns = [
  { title: t('ecomOwh.sku.material'), key: 'material', width: 240 },
  { title: t('ecomOwh.sku.owhSku'), dataIndex: 'owhSku', width: 180 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(2)
]

async function load() {
  if (!props.accountId) return
  loading.value = true
  try {
    list.value = (await owhSkus(props.accountId, keyword.value || undefined)) || []
  } finally {
    loading.value = false
  }
}
watch(() => props.accountId, load, { immediate: true })

function edit(record) {
  form.materialId = record.materialId
  form.owhSku = record.owhSku
}

async function save() {
  if (!form.materialId || !form.owhSku?.trim()) {
    message.warning(t('ecomOwh.sku.required'))
    return
  }
  saving.value = true
  try {
    await saveOwhSku(props.accountId, { materialId: form.materialId, owhSku: form.owhSku.trim() })
    message.success(t('common.msg.saveSuccess'))
    form.materialId = undefined
    form.owhSku = ''
    load()
  } finally {
    saving.value = false
  }
}
const remove = useAction(deleteOwhSku, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.tab-tip {
  margin-bottom: 8px;
  color: var(--app-text-3);
  font-size: 12px;
}
.tab-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.search {
  width: 220px;
}
.material {
  width: 260px;
}
.sku {
  width: 180px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
