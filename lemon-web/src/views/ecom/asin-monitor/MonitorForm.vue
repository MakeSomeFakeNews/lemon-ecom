<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomOper.monitor.addTitle')" :edit-title="$t('ecomOper.monitor.editTitle')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomOper.monitor.sectionTarget') }}</div>
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('ecomOper.monitor.monitorType')" name="monitorType">
            <a-radio-group v-model:value="form.monitorType" :options="toOptions(dict.ecom_monitor_type)" option-type="button" button-style="solid" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="6">
          <a-form-item :label="$t('ecomOper.common.platform')" name="platform">
            <a-select v-model:value="form.platform" :options="toOptions(dict.ecom_platform)" @change="onPlatform" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="6">
          <a-form-item :label="$t('ecomOper.common.marketplace')" name="marketplace">
            <a-select v-model:value="form.marketplace" show-search :options="optionsWithCurrent(dict.ecom_marketplace, form.marketplace)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="6">
          <a-form-item :label="$t('ecomOper.monitor.itemId')" name="itemId">
            <a-input v-model:value="form.itemId" :maxlength="64" :placeholder="$t('ecomOper.monitor.itemIdPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOper.monitor.brand')" name="brand">
            <a-input v-model:value="form.brand" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('ecomOper.common.store')" name="storeId" :extra="own ? $t('ecomOper.monitor.storeExtraOwn') : $t('ecomOper.monitor.storeExtraCompetitor')">
            <StoreSelect v-model:value="form.storeId" :label="form.storeName" :platform="form.platform" :allow-clear="!own" @select="onStore" />
          </a-form-item>
        </a-col>
        <a-col v-if="own" :xs="24" :lg="16">
          <a-form-item :label="$t('ecomOper.monitor.listing')" name="listingId">
            <ListingSelect v-model:value="form.listingId" :label="form.listingLabel" :store-id="form.storeId" @select="onListing" />
          </a-form-item>
        </a-col>
        <a-col v-else :xs="24" :lg="16">
          <a-form-item :label="$t('ecomOper.monitor.benchmark')" name="benchmarkId">
            <a-select v-model:value="form.benchmarkId" allow-clear show-search option-filter-prop="label" :options="benchmarkOptions" :placeholder="$t('ecomOper.monitor.benchmarkPh')" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomOper.monitor.sectionAlert') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOper.monitor.autoFetch')" name="autoFetch" :extra="$t('ecomOper.monitor.autoFetchExtra')">
            <a-switch v-model:checked="form.autoFetch" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOper.monitor.rankDropPercent')" name="rankDropPercent" :extra="$t('ecomOper.monitor.rankDropExtra')">
            <a-input-number v-model:value="form.rankDropPercent" :min="0" :max="1000" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOper.monitor.ratingDrop')" name="ratingDrop" :extra="$t('ecomOper.monitor.ratingDropExtra')">
            <a-input-number v-model:value="form.ratingDrop" :min="0" :max="5" :step="0.1" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOper.monitor.notifyUser')" name="notifyUserId" :extra="$t('ecomOper.monitor.notifyUserExtra')">
            <IdentityUserSelect v-model:value="form.notifyUserId" :label="form.notifyUserName" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import ListingSelect from '@/components/ecom/ListingSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { addMonitor, getMonitor, pageMonitor, updateMonitor } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { EcomMonitorType, EcomPlatform } from '@/constants/enums'

/** 商品监控新增 / 修改（子页）：自己的商品选店铺与在线商品，竞品可选对标的自己商品。openCreate() / openEdit(record)，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace', 'ecom_monitor_type')

const own = computed(() => form.value.monitorType === EcomMonitorType.OWN)
const benchmarks = ref([])
const benchmarkOptions = computed(() => benchmarks.value.filter((m) => m.id !== form.value.id && m.platform === form.value.platform)
  .map((m) => ({ value: m.id, label: `${m.itemId}${m.title ? `  ${m.title}` : ''}` })))

const rules = {
  monitorType: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecomOper.monitor.monitorType') }) }],
  platform: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecomOper.common.platform') }) }],
  marketplace: [{ required: true, message: t('common.rule.requiredSelect', { name: t('ecomOper.common.marketplace') }) }],
  itemId: [{ required: true, whitespace: true, message: t('ecomOper.monitor.inputItemId') }],
  storeId: [{ validator: (_, v) => (own.value && v == null ? Promise.reject(t('common.rule.requiredSelect', { name: t('ecomOper.common.store') })) : Promise.resolve()) }]
}

const FIELDS = ['monitorType', 'platform', 'marketplace', 'itemId', 'storeId', 'listingId', 'sellerSku', 'benchmarkId', 'brand', 'autoFetch',
  'rankDropPercent', 'ratingDrop', 'notifyUserId', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, monitorType: EcomMonitorType.OWN, platform: EcomPlatform.AMAZON, marketplace: 'US', itemId: '', storeId: undefined, storeName: undefined,
    listingId: undefined, listingLabel: '', sellerSku: undefined, benchmarkId: undefined, brand: '', autoFetch: 1, rankDropPercent: 30, ratingDrop: 0.1,
    notifyUserId: undefined, notifyUserName: undefined, remark: ''
  }),
  beforeOpen: async () => {
    const res = await pageMonitor({ monitorType: EcomMonitorType.OWN, current: 1, size: 500 })
    benchmarks.value = res.records || []
  },
  fetch: async (record) => {
    const d = await getMonitor(record.id)
    return { ...d, listingLabel: d.sellerSku || '', rankDropPercent: Number(d.rankDropPercent), ratingDrop: Number(d.ratingDrop) }
  },
  create: addMonitor,
  update: updateMonitor,
  toPayload: (f) => {
    const p = Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]]))
    if (own.value) p.benchmarkId = null
    else p.listingId = null
    return p
  },
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

function onPlatform() {
  form.value.storeId = undefined
  form.value.listingId = undefined
  form.value.benchmarkId = undefined
}
function onStore(store) {
  form.value.storeName = store.name
  if (store.marketplace) form.value.marketplace = store.marketplace
  form.value.listingId = undefined
}
function onListing(l) {
  form.value.listingLabel = `${l.sellerSku}${l.title ? `  ${l.title}` : ''}`
  form.value.sellerSku = l.sellerSku
  if (l.platformItemId && !form.value.itemId) form.value.itemId = l.platformItemId
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
.full {
  width: 100%;
}
</style>
