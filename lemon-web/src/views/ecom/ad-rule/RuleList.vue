<template>
  <a-alert type="info" show-icon :message="$t('ecomAds.rule.help')" class="tip" />
  <div class="toolbar">
    <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAds.rule.keywordPh')" class="kw" @press-enter="search" />
    <a-select v-model:value="query.status" allow-clear :placeholder="$t('ecomAds.common.status')" :options="toOptions(dict.sys_common_status, true)" class="status" />
    <a-button @click="search"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
    <span class="spacer" />
    <a-button v-perm="'ecom:adRule:add'" type="primary" @click="formRef.openCreate()">
      <template #icon><PlusOutlined /></template>{{ $t('ecomAds.rule.add') }}
    </a-button>
  </div>
  <a-table
    row-key="id"
    size="middle"
    :columns="columns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: 1500 }"
    @change="onTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        {{ record.name }}
        <div class="sub-text">{{ $t(`ecomAds.enum.level.${record.level}`) }} · {{ $t('ecomAds.rule.windowDays', { n: record.windowDays }) }}</div>
      </template>
      <template v-else-if="column.key === 'conditions'"><span class="multi-line">{{ conditionText(record) }}</span></template>
      <template v-else-if="column.key === 'action'">
        <span class="multi-line">{{ actionText(record, actionLabel) }}</span>
        <div><a-tag v-if="record.autoApply === 1" color="blue">{{ $t('ecomAds.rule.autoApply') }}</a-tag></div>
      </template>
      <template v-else-if="column.key === 'runMode'">
        {{ $t(`ecomAds.enum.runMode.${record.runMode}`) }}
        <div v-if="record.runMode === EcomAdRunMode.DAILY" class="sub-text">{{ $t('ecomAds.schedule.hour', { h: record.runHour }) }}</div>
      </template>
      <template v-else-if="column.key === 'lastRun'">
        {{ record.lastRunTime || '-' }}
        <div class="sub-text multi-line">{{ record.lastRunMessage }}</div>
      </template>
      <template v-else-if="column.key === 'pendingCount'">
        <a v-if="record.pendingCount" @click="emit('logs', record.id)">{{ record.pendingCount }}</a>
        <span v-else>0</span>
      </template>
      <template v-else-if="column.key === 'status'">
        <StatusSwitch
          :value="record.status"
          :name="record.name"
          :api="(s) => changeAdRuleStatus(record.id, s)"
          :disabled="!userStore.hasPerm('ecom:adRule:edit')"
          @changed="(s) => (record.status = s)"
        />
      </template>
      <template v-else-if="column.key === 'ops'">
        <TableActions :actions="rowActions(record)" />
      </template>
    </template>
  </a-table>
  <RuleForm ref="formRef" @success="load" />
  <RulePreview ref="previewRef" />
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { changeAdRuleStatus, deleteAdRule, pageAdRules, runAdRule } from '@/api/ecom/adRule'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { EcomAdRunMode } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { actionText, conditionText } from './ruleText'
import RuleForm from './RuleForm.vue'
import RulePreview from './RulePreview.vue'

/**
 * 广告规则列表：条件 / 动作摘要、执行方式、最近执行结果、待处理建议数（点击查看执行日志）、启停；新建 / 编辑、预览、立即执行、删除。
 * embedded：嵌在广告数据页面的「规则」页签中
 */
defineProps({ embedded: Boolean })
const emit = defineEmits(['logs'])
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'ecom_ad_rule_action')
const actionLabel = (v) => findDict(dict.ecom_ad_rule_action, v)?.label || v

const columns = [
  { title: t('ecomAds.rule.name'), key: 'name', width: 220, fixed: 'left' },
  { title: t('ecomAds.rule.conditions'), key: 'conditions', width: 300 },
  { title: t('ecomAds.rule.action'), key: 'action', width: 260 },
  { title: t('ecomAds.rule.runMode'), key: 'runMode', width: 110 },
  { title: t('ecomAds.rule.owner'), dataIndex: 'ownerName', key: 'ownerName', width: 100 },
  { title: t('ecomAds.rule.lastRun'), key: 'lastRun', width: 260 },
  { title: t('ecomAds.rule.pending'), key: 'pendingCount', width: 80, align: 'right' },
  { title: t('ecomAds.common.status'), key: 'status', width: 80 },
  { ...actionColumn(4), key: 'ops' }
]

const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageAdRules, query)
onMounted(load)

const formRef = ref()
const previewRef = ref()
const run = useAction((r) => runAdRule(r.id), {
  confirm: (r) => t('ecomAds.rule.runConfirm', { name: r.name }),
  successKey: (res) => t('ecomAds.rule.runDone', { msg: res.message }),
  reload: () => load()
})
const remove = useAction((r) => deleteAdRule(r.id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const rowActions = (r) => [
  { type: 'view', label: t('ecomAds.rule.preview'), onClick: () => previewRef.value.open(r) },
  { type: 'run', label: t('ecomAds.rule.run'), perm: 'ecom:adRule:execute', onClick: () => run(r) },
  { type: 'edit', perm: 'ecom:adRule:edit', onClick: () => formRef.value.openEdit(r) },
  { type: 'log', label: t('ecomAds.tabs.logs'), onClick: () => emit('logs', r.id) },
  { type: 'delete', perm: 'ecom:adRule:remove', confirm: t('ecomAds.rule.confirmDelete', { name: r.name }), onClick: () => remove(r) }
]
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.kw {
  width: 200px;
}
.status {
  width: 120px;
}
.spacer {
  flex: 1;
}
.multi-line {
  white-space: normal;
  word-break: break-word;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
