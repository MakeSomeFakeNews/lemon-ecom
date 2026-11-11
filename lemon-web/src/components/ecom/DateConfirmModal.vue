<template>
  <a-modal v-model:open="open" :title="title" :confirm-loading="saving" @ok="submit">
    <a-alert v-if="content" type="info" show-icon class="tip" :message="content" />
    <a-form layout="vertical">
      <a-form-item :label="label || $t('ecomOrder.common.date')" required>
        <a-date-picker v-model:value="date" value-format="YYYY-MM-DD" class="full" :allow-clear="false" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { today } from '@/components/wms/wms'

/**
 * 选择日期后确认的操作（退货入库日期、货件签收日期等）：
 *   <DateConfirmModal ref="dateModal" :label="$t('…')" @done="reload" />
 *   dateModal.open({ title, content, action: (date) => receive(id, date) })
 * action 返回 Promise，完成后关闭弹窗并触发 done；失败时（提示由请求拦截器给出）弹窗保持打开
 */
defineProps({
  label: String
})
const emit = defineEmits(['done'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const title = ref('')
const content = ref('')
const date = ref()
let action

function show({ title: tl = '', content: ct = '', action: fn }) {
  action = fn
  title.value = tl
  content.value = ct
  date.value = today()
  open.value = true
}

async function submit() {
  if (!date.value) {
    message.warning(t('ecomOrder.common.selectDate'))
    return
  }
  saving.value = true
  try {
    const res = await action?.(date.value)
    open.value = false
    emit('done', res, date.value)
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
