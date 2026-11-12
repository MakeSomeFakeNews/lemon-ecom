<template>
  <a-modal
    :open="visible"
    :title="$t('ecomProfit.payout.receiveTitle', { no: record?.settlementNo || '' })"
    :confirm-loading="saving"
    width="min(600px, 100vw)"
    @ok="submit"
    @cancel="visible = false"
  >
    <a-descriptions v-if="record" :column="2" size="small" class="block">
      <a-descriptions-item :label="$t('ecomReport.common.store')">{{ record.storeName }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomProfit.payout.expectedDate')">{{ record.expectedDate }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomProfit.payout.expectedAmount')">{{ money(record.expectedAmount) }} {{ record.currency }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomProfit.payout.expectedBase')">{{ money(record.expectedBase) }}</a-descriptions-item>
    </a-descriptions>
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.date')" name="payoutDate">
            <a-date-picker v-model:value="form.payoutDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.amount')" name="amount">
            <a-input-group compact class="amount-group">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" class="amount-input" />
              <a-select v-model:value="form.payCurrency" :options="currencyOptions" class="currency-select" />
            </a-input-group>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.account')" name="accountId">
            <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.channel')" name="channel">
            <a-select v-model:value="form.channel" allow-clear :options="toOptions(dict.ecom_payout_channel)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.exchangeRate')" name="exchangeRate" :extra="$t('ecomProfit.payout.rateExtra')">
            <a-input-number v-model:value="form.exchangeRate" :min="0" :precision="6" :disabled="form.payCurrency !== record?.currency" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomProfit.payout.bankRef')" name="bankRef">
            <a-input v-model:value="form.bankRef" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-alert type="info" show-icon :message="$t('ecomProfit.payout.receiveTip')" />
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { BASE_CURRENCY } from '@/components/fin/currency'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { receivePayout } from '@/api/ecom/payout'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'

/** 手工登记到账：open(未到账记录)，到账币种可选结算币种或本位币（收款平台已结汇），保存后发出 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_payout_channel')

const visible = ref(false)
const saving = ref(false)
const record = ref()
const formRef = ref()
const form = ref({})
const rules = {
  payoutDate: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.payout.date') }) }],
  amount: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.payout.amount') }) }],
  accountId: [{ required: true, message: t('common.rule.required', { name: t('ecomProfit.payout.account') }) }]
}
const currencyOptions = computed(() => [...new Set([record.value?.currency, BASE_CURRENCY].filter(Boolean))].map((c) => ({ label: c, value: c })))

function open(r) {
  record.value = r
  form.value = {
    payoutDate: dayjs().format('YYYY-MM-DD'),
    amount: r.expectedAmount,
    payCurrency: r.currency,
    accountId: undefined,
    channel: undefined,
    exchangeRate: undefined,
    bankRef: '',
    remark: ''
  }
  visible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    await receivePayout(record.value.id, { ...f, exchangeRate: f.payCurrency === record.value.currency ? f.exchangeRate ?? null : null })
    message.success(t('common.msg.saveSuccess'))
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.amount-group {
  display: flex;
}
.amount-input {
  flex: 1;
}
.currency-select {
  width: 90px;
}
</style>
