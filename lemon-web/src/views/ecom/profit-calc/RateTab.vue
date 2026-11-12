<template>
  <div>
    <div class="tools">
      <a-space wrap>
        <a-select v-model:value="query.platform" :placeholder="$t('ecomDev.calc.platform')" allow-clear class="sel" :options="toOptions(dict.ecom_platform)" @change="load" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomDev.rate.keywordPh')" allow-clear class="search" @search="load" />
      </a-space>
      <a-button v-perm="'ecom:profitCalc:rate'" type="primary" @click="openForm()"><template #icon><PlusOutlined /></template>{{ $t('ecomDev.rate.add') }}</a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="{ pageSize: 20, showSizeChanger: false }"
             :scroll="{ x: 1100 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'platform'"><DictTag type="ecom_platform" :value="record.platform" /></template>
        <template v-else-if="column.key === 'marketplace'">{{ record.marketplace || $t('ecomDev.rate.allSites') }}</template>
        <template v-else-if="column.key === 'category'">{{ record.category || $t('ecomDev.rate.defaultCategory') }}</template>
        <template v-else-if="column.key === 'rate'">{{ Number(record.rate) }}%</template>
        <template v-else-if="column.key === 'minFee' || column.key === 'fixedFee'">{{ record[column.key] != null ? Number(record[column.key]) : '-' }}</template>
        <template v-else-if="column.key === 'vatRate'">{{ record.vatRate != null ? `${Number(record.vatRate)}%` : '-' }}</template>
        <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'builtin'"><a-tag v-if="record.builtin === 1">{{ $t('ecomDev.rate.builtin') }}</a-tag></template>
        <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('ecomDev.rate.edit') : $t('ecomDev.rate.add')" :confirm-loading="saving" @ok="save">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.platform')" required>
              <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.marketplace')" :extra="$t('ecomDev.rate.siteExtra')">
              <a-select v-model:value="form.marketplace" allow-clear show-search :options="toOptions(dict.ecom_marketplace)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.category')" :extra="$t('ecomDev.rate.categoryExtra')">
              <a-input v-model:value="form.category" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.commissionRate')" required>
              <a-input-number v-model:value="form.rate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.minCommission')">
              <a-input-number v-model:value="form.minFee" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.fixedFee')">
              <a-input-number v-model:value="form.fixedFee" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.calc.currency')">
              <a-input v-model:value="form.currency" :maxlength="8" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomDev.rate.vatRate')" :extra="$t('ecomDev.rate.vatExtra')">
              <a-input-number v-model:value="form.vatRate" :min="0" :max="100" :precision="2" class="full" addon-after="%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.status')">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { addCommissionRate, deleteCommissionRate, listCommissionRate, updateCommissionRate } from '@/api/ecom/dev'
import { EcomPlatform } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'

/**
 * 平台佣金费率：平台 × 站点（空为全部站点）× 类目（空为默认），利润试算按「站点 + 类目 → 站点 → 全部站点 + 类目 → 全部站点」匹配。
 * 内置常见平台的默认值（新租户复制），可修改
 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace', 'sys_common_status')
const query = reactive({ platform: undefined, keyword: undefined })
const list = ref([])
const loading = ref(false)

const columns = [
  { title: t('ecomDev.calc.platform'), key: 'platform', width: 120 },
  { title: t('ecomDev.calc.marketplace'), key: 'marketplace', width: 100 },
  { title: t('ecomDev.calc.category'), key: 'category', width: 140 },
  { title: t('ecomDev.calc.commissionRate'), key: 'rate', width: 90, align: 'right' },
  { title: t('ecomDev.calc.minCommission'), key: 'minFee', width: 100, align: 'right' },
  { title: t('ecomDev.calc.fixedFee'), key: 'fixedFee', width: 100, align: 'right' },
  { title: t('ecomDev.calc.currency'), dataIndex: 'currency', width: 70 },
  { title: t('ecomDev.rate.vatRate'), key: 'vatRate', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: '', key: 'builtin', width: 70 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 90, align: 'right', fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    list.value = await listCommissionRate({ platform: query.platform, keyword: query.keyword || undefined })
  } finally {
    loading.value = false
  }
}
onMounted(load)

const formOpen = ref(false)
const saving = ref(false)
const form = ref({})
const num = (v) => (v == null ? undefined : Number(v))
function openForm(r) {
  form.value = r
    ? { ...r, rate: num(r.rate), minFee: num(r.minFee), fixedFee: num(r.fixedFee), vatRate: num(r.vatRate), marketplace: r.marketplace || undefined }
    : { platform: EcomPlatform.AMAZON, marketplace: undefined, category: '', rate: 15, status: STATUS_ENABLED }
  formOpen.value = true
}
async function save() {
  const f = form.value
  if (!f.platform || f.rate == null) {
    message.warning(t('ecomDev.rate.required'))
    return
  }
  const payload = { platform: f.platform, marketplace: f.marketplace || '', category: f.category || '', rate: f.rate, minFee: f.minFee ?? null,
    fixedFee: f.fixedFee ?? null, currency: f.currency || null, vatRate: f.vatRate ?? null, status: f.status, remark: f.remark || null }
  saving.value = true
  try {
    if (f.id) await updateCommissionRate(f.id, payload)
    else await addCommissionRate(payload)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:profitCalc:rate', onClick: () => openForm(r) },
  {
    type: 'delete',
    perm: 'ecom:profitCalc:rate',
    confirm: { title: t('ecomDev.rate.deleteConfirm'), okType: 'danger' },
    onClick: async () => {
      await deleteCommissionRate(r.id)
      message.success(t('common.msg.deleteSuccess'))
      load()
    }
  }
]
</script>

<style scoped>
.tools {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
  flex-wrap: wrap;
}
.sel {
  width: 160px;
}
.search {
  width: 240px;
}
.full {
  width: 100%;
}
</style>
