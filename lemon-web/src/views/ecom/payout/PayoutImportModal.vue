<template>
  <a-modal
    :open="visible"
    :title="$t('ecomProfit.payout.import')"
    :ok-text="$t('ecomProfit.payout.chooseFile')"
    width="min(560px, 100vw)"
    @ok="next"
    @cancel="visible = false"
  >
    <a-alert type="info" show-icon :message="$t('ecomProfit.payout.importTip')" class="tip" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.account')" name="accountId" :extra="$t('ecomProfit.payout.accountExtra')">
            <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.channel')" name="channel">
            <a-select v-model:value="form.channel" allow-clear :options="toOptions(dict.ecom_payout_channel)" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomReport.common.store')" name="storeId" :extra="$t('ecomProfit.payout.storeExtra')">
            <StoreSelect v-model:value="form.storeId" :placeholder="$t('ecomReport.common.allStores')" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.windowDays')" name="windowDays">
            <a-input-number v-model:value="form.windowDays" :min="0" :max="90" :precision="0" :addon-after="$t('common.unit.day')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.tolerance')" name="tolerance">
            <a-input-number v-model:value="form.tolerance" :min="0" :max="20" :precision="2" addon-after="%" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
  <ImportModal
    v-model:open="importOpen"
    :title="$t('ecomProfit.payout.importTitle')"
    :columns="importColumns"
    :sample="sample"
    :importer="importer"
    @success="onImported"
  />
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import ImportModal from '@/components/ImportModal.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { importPayouts } from '@/api/ecom/payout'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/**
 * 导入到账流水：先选择到账账户、收款平台、店铺（可空）和自动匹配参数，再选择 Excel（列与后端 PayoutImportRow 一致）；
 * 导入后按 金额 + 日期窗口 + 店铺 匹配未到账的结算单。open() 打开，完成后发出 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_payout_channel')

const visible = ref(false)
const importOpen = ref(false)
const formRef = ref()
const form = reactive({ accountId: undefined, channel: undefined, storeId: undefined, windowDays: 7, tolerance: 2 })
const rules = { accountId: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.payout.account') }) }] }

function open() {
  visible.value = true
}

async function next() {
  await formRef.value.validate()
  visible.value = false
  importOpen.value = true
}

const importer = (data) => importPayouts({ ...data, ...form, storeId: form.storeId || undefined, channel: form.channel || undefined })

function onImported(res) {
  message.info(t('ecomProfit.payout.importResult', { matched: res.matched, diff: res.diff, unmatched: res.unmatched, skipped: res.skipped }), 6)
  emit('success')
}

const importColumns = [
  { title: t('ecomProfit.payout.date'), key: 'payoutDate', required: true, type: 'date', width: 14 },
  { title: t('ecomProfit.payout.amount'), key: 'amount', required: true, type: 'number', width: 14 },
  { title: t('ecomReport.common.currency'), key: 'currency', required: true, width: 8 },
  { title: t('common.field.remark'), key: 'remark', width: 40 },
  { title: t('ecomProfit.payout.bankRef'), key: 'bankRef', width: 24 },
  { title: t('ecomProfit.payout.exchangeRate'), key: 'exchangeRate', type: 'number', width: 12 },
  { title: t('ecomReport.settlement.storeCode'), key: 'storeCode', width: 12 }
]
const sample = {
  payoutDate: '2026-10-18',
  amount: '1234.56',
  currency: 'USD',
  remark: t('ecomProfit.payout.sampleRemark'),
  bankRef: 'PP2026101800001',
  exchangeRate: '7.12',
  storeCode: 'US01'
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
