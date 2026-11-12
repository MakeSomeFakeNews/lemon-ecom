<template>
  <div class="action-card" :class="{ invalid: !supported }">
    <div class="action-head">
      <span class="action-no">{{ index + 1 }}</span>
      <a-select v-model:value="action.type" :options="typeOptions" class="type" @change="onType" />
      <span class="action-desc">{{ actionOf(catalog, action.type)?.description }}</span>
      <a-tooltip :title="$t('ecomAuto.editor.moveUp')">
        <a-button type="text" size="small" :disabled="index === 0" @click="emit('up')"><template #icon><ArrowUpOutlined /></template></a-button>
      </a-tooltip>
      <a-tooltip :title="$t('ecomAuto.editor.moveDown')">
        <a-button type="text" size="small" :disabled="index === total - 1" @click="emit('down')"><template #icon><ArrowDownOutlined /></template></a-button>
      </a-tooltip>
      <a-tooltip :title="$t('ecomAuto.editor.remove')">
        <a-button type="text" size="small" danger @click="emit('remove')"><template #icon><DeleteOutlined /></template></a-button>
      </a-tooltip>
    </div>
    <a-alert v-if="!supported" type="error" show-icon :message="$t('ecomAuto.editor.unsupported')" class="unsupported" />

    <a-row :gutter="12" class="action-body">
      <!-- 接收人：通知、待办 -->
      <template v-if="hasRecipients">
        <a-col :xs="24" :lg="10">
          <a-form-item :label="$t('ecomAuto.act.users')">
            <UserMultiSelect v-model:value="action.userIds" :names="userNames" :placeholder="$t('ecomAuto.act.usersPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('ecomAuto.act.roles')">
            <a-select :value="action.roleIds || []" mode="multiple" :options="roleOptions" option-filter-prop="label" :max-tag-count="2"
              :placeholder="$t('ecomAuto.act.rolesPh')" @change="(v) => (action.roleIds = v)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="6">
          <a-form-item label=" " :colon="false">
            <a-checkbox v-model:checked="action.storeOwner">{{ $t('ecomAuto.act.storeOwner') }}</a-checkbox>
          </a-form-item>
        </a-col>
      </template>

      <a-col v-if="action.type === AutoActionType.NOTIFY" :span="24">
        <a-form-item :label="$t('ecomAuto.act.channels')" :extra="$t('ecomAuto.act.channelTip')">
          <a-checkbox :checked="action.site !== false" @change="(e) => (action.site = e.target.checked)">{{ $t('ecomAuto.channel.SITE') }}</a-checkbox>
          <a-checkbox-group :value="action.channels || []" :options="imOptions" @change="(v) => (action.channels = v)" />
        </a-form-item>
      </a-col>

      <a-col v-if="action.type === AutoActionType.IM_ROBOT" :xs="24" :lg="8">
        <a-form-item :label="$t('ecomAuto.act.robotPlatform')" required>
          <a-select v-model:value="action.imPlatform" :options="imOptions" />
        </a-form-item>
      </a-col>
      <a-col v-if="action.type === AutoActionType.IM_ROBOT" :xs="24" :lg="16">
        <a-form-item :label="$t('ecomAuto.act.webhook')">
          <a-input v-model:value="action.webhook" :maxlength="500" allow-clear :placeholder="$t('ecomAuto.act.robotWebhookPh')" />
        </a-form-item>
      </a-col>

      <template v-if="action.type === AutoActionType.WEBHOOK">
        <a-col :xs="24" :lg="14">
          <a-form-item :label="$t('ecomAuto.act.webhook')" required :extra="$t('ecomAuto.act.webhookTip')">
            <a-input v-model:value="action.webhook" :maxlength="500" :placeholder="$t('ecomAuto.act.webhookPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="10">
          <a-form-item :label="$t('ecomAuto.act.secret')">
            <a-input-password v-model:value="action.secret" :maxlength="500" :placeholder="$t('ecomAuto.act.secretPh')" autocomplete="new-password" />
          </a-form-item>
        </a-col>
      </template>
      <a-col v-if="action.type === AutoActionType.IM_ROBOT && action.webhook" :xs="24" :lg="12">
        <a-form-item :label="$t('ecomAuto.act.secret')">
          <a-input-password v-model:value="action.secret" :maxlength="500" :placeholder="$t('ecomAuto.act.robotSecretPh')" autocomplete="new-password" />
        </a-form-item>
      </a-col>

      <!-- 标题 / 内容：通知、群机器人、待办 -->
      <template v-if="hasMessage">
        <a-col :span="24">
          <a-form-item :label="$t('ecomAuto.act.title')">
            <a-input v-model:value="action.title" :maxlength="128" allow-clear :placeholder="$t('ecomAuto.act.titlePh')" @focus="focus('title')" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomAuto.act.content')">
            <a-textarea v-model:value="action.content" :maxlength="1000" :auto-size="{ minRows: 2, maxRows: 5 }" :placeholder="$t('ecomAuto.act.contentPh')"
              @focus="focus('content')" />
          </a-form-item>
        </a-col>
      </template>
      <a-col v-if="action.type === AutoActionType.TODO" :xs="24" :lg="8">
        <a-form-item :label="$t('ecomAuto.act.dueDays')">
          <a-input-number v-model:value="action.dueDays" :min="0" :max="365" :precision="0" :placeholder="$t('ecomAuto.act.dueDaysPh')" class="full" />
        </a-form-item>
      </a-col>

      <a-col v-if="action.type === AutoActionType.ORDER_HOLD" :span="24">
        <a-form-item :label="$t('ecomAuto.act.reason')">
          <a-input v-model:value="action.reason" :maxlength="255" allow-clear :placeholder="$t('ecomAuto.act.reasonPh')" @focus="focus('reason')" />
        </a-form-item>
      </a-col>
      <a-col v-if="action.type === AutoActionType.ORDER_TAG" :span="24">
        <a-form-item :label="$t('ecomAuto.act.tagIds')" required>
          <a-select :value="action.tagIds || []" mode="multiple" :options="tagOptions" option-filter-prop="label" @change="(v) => (action.tagIds = v)" />
        </a-form-item>
      </a-col>
      <template v-if="action.type === AutoActionType.ORDER_ASSIGN">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomAuto.act.warehouse')">
            <WarehouseSelect v-model:value="action.warehouseId" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomAuto.act.logisticsChannel')">
            <ChannelSelect v-model:value="action.channelId" allow-clear />
          </a-form-item>
        </a-col>
      </template>

      <!-- 调价、调整广告 -->
      <a-col v-if="action.type === AutoActionType.AD_ADJUST" :xs="24" :lg="8">
        <a-form-item :label="$t('ecomAuto.act.adTarget')" required>
          <a-select v-model:value="action.adTarget" :options="adTargetOptions" />
        </a-form-item>
      </a-col>
      <template v-if="action.type === AutoActionType.LISTING_REPRICE || action.type === AutoActionType.AD_ADJUST">
        <a-col :xs="24" :lg="action.type === AutoActionType.AD_ADJUST ? 6 : 8">
          <a-form-item :label="$t('ecomAuto.act.adjustMode')" required>
            <a-select v-model:value="action.adjustMode" :options="adjustOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="action.type === AutoActionType.AD_ADJUST ? 10 : 16">
          <a-form-item :label="$t('ecomAuto.act.value')" required :extra="action.adjustMode === PriceAdjustMode.PERCENT ? $t('ecomAuto.act.valuePercentTip') : undefined">
            <a-input-number v-model:value="action.value" :precision="2" class="full" :addon-after="action.adjustMode === PriceAdjustMode.PERCENT ? '%' : undefined" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-form-item :label="$t('ecomAuto.act.minValue')">
            <a-input-number v-model:value="action.minValue" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-form-item :label="$t('ecomAuto.act.maxValue')">
            <a-input-number v-model:value="action.maxValue" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="action.type === AutoActionType.LISTING_REPRICE" :xs="24" :lg="12">
          <a-form-item label=" " :colon="false">
            <a-checkbox v-model:checked="action.suggestOnly">{{ $t('ecomAuto.act.suggestOnly') }}</a-checkbox>
          </a-form-item>
        </a-col>
      </template>

      <template v-if="action.type === AutoActionType.PURCHASE_PLAN">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomAuto.act.qty')">
            <a-input-number v-model:value="action.qty" :min="0" :precision="0" :placeholder="$t('ecomAuto.act.qtyPh')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomAuto.act.planWarehouse')">
            <WarehouseSelect v-model:value="action.warehouseId" allow-clear :placeholder="$t('ecomAuto.act.planWarehousePh')" />
          </a-form-item>
        </a-col>
      </template>

      <template v-if="action.type === AutoActionType.CS_TICKET">
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('ecomAuto.act.ticketType')" :extra="$t('ecomAuto.act.ticketTip')">
            <a-select v-model:value="action.ticketType" :options="toOptions(dict.ecom_cs_ticket_type)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('ecomAuto.act.ticketReason')">
            <a-select v-model:value="action.reason" allow-clear :options="toOptions(dict.ecom_return_reason)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-form-item :label="$t('common.field.remark')">
            <a-input v-model:value="action.content" :maxlength="255" allow-clear @focus="focus('content')" />
          </a-form-item>
        </a-col>
      </template>

      <a-col v-if="action.type === AutoActionType.AD_PAUSE" :span="24">
        <div class="hint">{{ $t('ecomAuto.act.pauseTip') }}</div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { useDict } from '@/composables/useDict'
import { AutoActionType, AutoAdTarget, ImChannel, PriceAdjustMode } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import UserMultiSelect from './UserMultiSelect.vue'
import { actionOf, applyActionDefaults } from './autoText'

/**
 * 一个动作的配置（按类型显示字段）：接收人（用户、角色、店铺负责人）、通知方式、群机器人 / Webhook 地址与密钥、
 * 标题与内容（支持变量，获得焦点时通知父组件，「插入变量」插入到这里）、挂起原因、标签、仓库 / 物流、调价、采购数量、售后类型、调整广告
 */
const props = defineProps({
  action: { type: Object, required: true },
  index: { type: Number, required: true },
  total: { type: Number, required: true },
  trigger: { type: Object, default: null },
  catalog: { type: Object, default: null },
  roles: { type: Array, default: () => [] },
  tagOptions: { type: Array, default: () => [] },
  userNames: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['up', 'down', 'remove', 'focus'])
const dict = useDict('ecom_cs_ticket_type', 'ecom_return_reason')

const MESSAGE_TYPES = [AutoActionType.NOTIFY, AutoActionType.IM_ROBOT, AutoActionType.TODO]
const hasRecipients = computed(() => props.action.type === AutoActionType.NOTIFY || props.action.type === AutoActionType.TODO)
const hasMessage = computed(() => MESSAGE_TYPES.includes(props.action.type))
const supported = computed(() => !props.trigger || props.trigger.actions.includes(props.action.type))

/** 动作类型：只列出当前触发器可用的（已选的不可用类型也保留，便于看到提示） */
const typeOptions = computed(() =>
  (props.catalog?.actions || [])
    .filter((a) => !props.trigger || props.trigger.actions.includes(a.type) || a.type === props.action.type)
    .map((a) => ({ value: a.type, label: a.name }))
)
const roleOptions = computed(() => props.roles.map((r) => ({ value: r.id, label: r.name })))
const imOptions = Object.values(ImChannel).map((v) => ({ value: v, label: t(`ecomAuto.channel.${v}`) }))
const adTargetOptions = Object.values(AutoAdTarget).map((v) => ({ value: v, label: t(`ecomAuto.adTarget.${v}`) }))
const adjustOptions = [PriceAdjustMode.PERCENT, PriceAdjustMode.AMOUNT, PriceAdjustMode.FIXED].map((v) => ({ value: v, label: t(`ecomAuto.adjust.${v}`) }))

function focus(key) {
  emit('focus', { action: props.action, key })
}

/** 换类型：补默认值 */
const onType = () => applyActionDefaults(props.action)
</script>

<style scoped>
.action-card {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 10px 12px 2px;
  background: var(--app-card);
}
.action-card.invalid {
  border-color: var(--status-critical, #ff4d4f);
}
.action-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.action-no {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--app-primary);
  color: #fff;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
  flex: none;
}
.type {
  width: 200px;
}
.action-desc {
  flex: 1;
  min-width: 0;
  color: var(--app-text-3);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unsupported {
  margin-bottom: 8px;
}
.action-body :deep(.ant-form-item) {
  margin-bottom: 10px;
}
.full {
  width: 100%;
}
.hint {
  padding: 4px 0 10px;
  color: var(--app-text-3);
  font-size: 13px;
}
</style>
