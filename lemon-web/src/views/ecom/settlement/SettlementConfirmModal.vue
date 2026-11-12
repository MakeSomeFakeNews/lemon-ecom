<template>
  <a-modal
    :open="visible"
    :title="$t(receiptMode ? 'ecomFixa.settlement.receiptTitle' : 'ecomReport.settlement.confirmTitle', { no: record?.settlementNo || '' })"
    :confirm-loading="saving"
    :ok-text="$t('common.action.confirm')"
    width="min(600px, 100vw)"
    @ok="submit"
    @cancel="visible = false"
  >
    <a-alert type="info" show-icon :message="$t(receiptMode ? 'ecomFixa.settlement.receiptTip' : 'ecomReport.settlement.confirmTip')" class="tip" />
    <a-descriptions v-if="record" :column="2" size="small" class="block">
      <a-descriptions-item :label="$t('ecomReport.common.store')">{{ record.storeName }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomReport.settlement.period')">{{ record.periodStart }} ~ {{ record.periodEnd }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomReport.settlement.netAmount')">
        <span :class="{ neg: nz(record.netAmount) < 0 }">{{ money(record.netAmount) }} {{ record.currency }}</span>
      </a-descriptions-item>
      <a-descriptions-item :label="$t('ecomReport.settlement.netBase')">
        <span :class="{ neg: nz(record.netBase) < 0 }">{{ money(record.netBase) }}</span>
      </a-descriptions-item>
    </a-descriptions>
    <a-form layout="vertical">
      <a-form-item
        :label="$t('ecomReport.settlement.account')"
        :required="receiptMode"
        :extra="receiptMode ? undefined : $t('ecomReport.settlement.accountExtra')"
      >
        <AccountSelect
          v-model:value="accountId"
          :roles="['CASH', 'BANK']"
          :allow-clear="!receiptMode"
          :placeholder="$t('ecomReport.settlement.accountPlaceholder')"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { confirmSettlement, createSettlementReceipt } from '@/api/ecom/settlement'
import { money, nz } from '@/utils/number'

/**
 * 确认结算单：回写订单费用；可选收款账户（现金 / 银行末级科目），选择后按净额生成收款单核销店铺客户的应收。
 * open(结算单) 打开，成功后发出 success；open(结算单, 'receipt') 为已确认、没有收款单的结算单补生成收款单（必须选收款账户）
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const record = ref()
const accountId = ref()
const receiptMode = ref(false)

function open(settlement, mode) {
  record.value = settlement
  accountId.value = undefined
  receiptMode.value = mode === 'receipt'
  visible.value = true
}

async function submit() {
  if (receiptMode.value && !accountId.value) {
    message.warning(t('ecomFixa.settlement.accountRequired'))
    return
  }
  saving.value = true
  try {
    if (receiptMode.value) {
      await createSettlementReceipt(record.value.id, accountId.value)
      message.success(t('ecomFixa.settlement.receiptCreated'))
    } else {
      await confirmSettlement(record.value.id, accountId.value ?? undefined)
      message.success(t('ecomReport.settlement.confirmed'))
    }
    visible.value = false
    emit('success', record.value)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.block {
  margin-bottom: 8px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
</style>
