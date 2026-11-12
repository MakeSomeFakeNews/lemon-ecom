<template>
  <a-drawer :open="open" :title="$t('ecomFba.rate.title')" width="min(980px, 100vw)" @close="open = false">
    <a-alert type="info" show-icon :message="$t('ecomFba.rate.tip')" class="block" />
    <div class="toolbar">
      <a-select v-model:value="marketplace" allow-clear :placeholder="$t('ecomFba.common.marketplace')" :options="toOptions(dict.ecom_marketplace)" class="market" @change="load" />
      <a-button v-perm="'ecom:fbaHealth:rate'" type="primary" @click="edit()"><template #icon><PlusOutlined /></template>{{ $t('ecomFba.rate.add') }}</a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 860 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'feeType'"><DictTag type="ecom_fba_storage_fee_type" :value="record.feeType" /></template>
        <template v-else-if="column.key === 'sizeCategory'"><DictTag type="ecom_fba_size_category" :value="record.sizeCategory" /></template>
        <template v-else-if="column.key === 'range'">
          <span v-if="record.feeType === FbaStorageFeeType.MONTHLY">{{ record.monthFrom }} – {{ record.monthTo }}</span>
          <span v-else>{{ record.ageFrom }} – {{ record.ageTo ?? '∞' }}</span>
        </template>
        <template v-else-if="column.key === 'rate'">
          {{ record.rate }} {{ record.currency }} {{ $t('ecomFba.rate.perUnit', { unit: $t(`ecomFba.volumeUnit.${record.volumeUnit}`) }) }}
        </template>
        <template v-else-if="column.key === 'builtin'">
          <a-tag v-if="record.builtin" :bordered="false">{{ $t('ecomFba.rate.builtin') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'edit', perm: 'ecom:fbaHealth:rate', onClick: () => edit(record) },
              { type: 'delete', perm: 'ecom:fbaHealth:rate', confirm: $t('ecomFba.rate.confirmDelete'), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('ecomFba.rate.editTitle') : $t('ecomFba.rate.add')" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.common.marketplace')" name="marketplace" :rules="[{ required: true }]">
              <a-select v-model:value="form.marketplace" :options="toOptions(dict.ecom_marketplace)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.common.currency')" name="currency" :rules="[{ required: true }]">
              <a-input v-model:value="form.currency" :maxlength="8" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.rate.feeType')" name="feeType" :rules="[{ required: true }]">
              <a-select v-model:value="form.feeType" :options="toOptions(dict.ecom_fba_storage_fee_type)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.rate.sizeCategory')" name="sizeCategory" :rules="[{ required: true }]">
              <a-select v-model:value="form.sizeCategory" :options="toOptions(dict.ecom_fba_size_category)" />
            </a-form-item>
          </a-col>
          <template v-if="form.feeType === FbaStorageFeeType.MONTHLY">
            <a-col :span="12"><a-form-item :label="$t('ecomFba.rate.monthFrom')"><a-input-number v-model:value="form.monthFrom" :min="1" :max="12" class="full" /></a-form-item></a-col>
            <a-col :span="12"><a-form-item :label="$t('ecomFba.rate.monthTo')"><a-input-number v-model:value="form.monthTo" :min="1" :max="12" class="full" /></a-form-item></a-col>
          </template>
          <template v-else>
            <a-col :span="12"><a-form-item :label="$t('ecomFba.rate.ageFrom')"><a-input-number v-model:value="form.ageFrom" :min="0" class="full" /></a-form-item></a-col>
            <a-col :span="12"><a-form-item :label="$t('ecomFba.rate.ageTo')"><a-input-number v-model:value="form.ageTo" :min="0" class="full" /></a-form-item></a-col>
          </template>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.rate.rate')" name="rate" :rules="[{ required: true }]">
              <a-input-number v-model:value="form.rate" :min="0" :precision="4" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomFba.rate.volumeUnit')" name="volumeUnit" :rules="[{ required: true }]">
              <a-select v-model:value="form.volumeUnit" :options="volumeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { addStorageRate, deleteStorageRate, listStorageRates, updateStorageRate } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { FbaSizeCategory, FbaStorageFeeType, FbaVolumeUnit } from '@/constants/enums'
import { t } from '@/locales'

/** 仓储费费率维护（按站点）：ref 调用 open() */
const dict = useDict('ecom_marketplace', 'ecom_fba_storage_fee_type', 'ecom_fba_size_category')
const volumeOptions = Object.values(FbaVolumeUnit).map((v) => ({ value: v, label: t(`ecomFba.volumeUnit.${v}`) }))
const columns = [
  { title: t('ecomFba.common.marketplace'), dataIndex: 'marketplace', width: 70 },
  { title: t('ecomFba.rate.feeType'), key: 'feeType', width: 120 },
  { title: t('ecomFba.rate.sizeCategory'), key: 'sizeCategory', width: 90 },
  { title: `${t('ecomFba.rate.months')} / ${t('ecomFba.rate.ages')}`, key: 'range', width: 120 },
  { title: t('ecomFba.rate.rate'), key: 'rate', width: 180 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: '', key: 'builtin', width: 60 },
  actionColumn(2)
]

const open = ref(false)
const loading = ref(false)
const list = ref([])
const marketplace = ref()

async function load() {
  loading.value = true
  try {
    list.value = (await listStorageRates(marketplace.value || undefined)) || []
  } finally {
    loading.value = false
  }
}

function show() {
  open.value = true
  load()
}

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
function edit(record) {
  form.value = record
    ? { ...record }
    : { marketplace: marketplace.value || 'US', currency: 'USD', feeType: FbaStorageFeeType.MONTHLY, sizeCategory: FbaSizeCategory.STANDARD, volumeUnit: FbaVolumeUnit.CUFT }
  formOpen.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, builtin: _b, createTime: _c, updateTime: _u, ...data } = form.value
    await (id ? updateStorageRate(id, data) : addStorageRate(data))
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
const remove = useAction(deleteStorageRate, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.market {
  width: 160px;
}
.full {
  width: 100%;
}
</style>
