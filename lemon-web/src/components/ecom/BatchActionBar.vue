<template>
  <div class="batch-bar">
    <span class="count">
      <CheckSquareFilled class="count-icon" />{{ text || $t('ecomOrdUi.batch.selectedItems', { n: count }) }}
    </span>
    <a-divider type="vertical" />
    <a-button v-for="a in directActions" :key="a.key" size="small" :type="a.primary ? 'primary' : 'default'" :loading="a.loading" @click="a.run">
      <template #icon><component :is="a.icon" /></template>{{ a.label }}
    </a-button>
    <a-dropdown v-if="more.length" trigger="click">
      <a-button size="small">{{ $t('ecomOrdUi.batch.more') }}<DownOutlined /></a-button>
      <template #overlay>
        <a-menu @click="({ key }) => more.find((a) => a.key === key)?.run()">
          <a-menu-item v-for="a in more" :key="a.key">
            <template #icon><component :is="a.icon" /></template>{{ a.label }}
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
    <a class="clear" @click="emit('clear')">{{ $t('ecomOrdUi.batch.clear') }}</a>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CheckSquareFilled, DownOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/user'

/**
 * 勾选行后显示在表格工具栏的批量操作条（代替一排禁用的批量按钮）：actions 为 [{ key, label, icon, perm, primary, loading, run }]，
 * 按权限过滤后前 direct 个直接显示，其余收进「更多」；text 为已选提示（默认「已选 n 项」），点「取消选择」触发 clear
 */
const props = defineProps({
  count: { type: Number, default: 0 },
  text: String,
  actions: { type: Array, default: () => [] },
  direct: { type: Number, default: 3 }
})
const emit = defineEmits(['clear'])
const userStore = useUserStore()
const allowed = computed(() => props.actions.filter((a) => !a.perm || userStore.hasPerm(a.perm)))
const directActions = computed(() => allowed.value.slice(0, props.direct))
const more = computed(() => allowed.value.slice(props.direct))
</script>

<style scoped>
.batch-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 4px 12px;
  border: 1px solid var(--app-primary-border, #91caff);
  border-radius: 6px;
  background: var(--app-primary-bg, #e6f4ff);
}
.count {
  color: var(--app-text);
  font-weight: 500;
  white-space: nowrap;
}
.count-icon {
  margin-right: 6px;
  color: var(--app-primary, #1677ff);
}
.clear {
  margin-left: 4px;
  white-space: nowrap;
}
</style>
