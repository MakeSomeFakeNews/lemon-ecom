<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <span>{{ $t('ecomOper.auth.user') }}</span>
        <IdentityUserSelect v-model:value="userId" :placeholder="$t('ecomOper.auth.selectUser')" class="user" />
      </a-space>
      <a-button v-perm="'ecom:storeAuth:edit'" type="primary" :disabled="!info" @click="openAssign">
        <template #icon><EditOutlined /></template>{{ $t('ecomOper.auth.assign') }}
      </a-button>
    </div>
    <a-empty v-if="!userId" :description="$t('ecomOper.auth.selectUserFirst')" />
    <a-spin v-else :spinning="loading">
      <template v-if="info">
        <a-alert v-if="info.unrestricted" type="success" show-icon :message="$t('ecomOper.auth.unrestricted')" class="block" />
        <div class="section-title">{{ $t('ecomOper.auth.owned') }}（{{ info.owned.length }}）</div>
        <div class="block">
          <span v-if="!info.owned.length" class="sub-text">{{ $t('ecomOper.common.none') }}</span>
          <a-tag v-for="s in info.owned" :key="s.id" color="gold" class="tag">{{ s.name }} · {{ platformLabel(s.platform) }} {{ s.marketplace }}</a-tag>
        </div>
        <div class="section-title">{{ $t('ecomOper.auth.granted') }}（{{ info.granted.length }}）</div>
        <div class="block">
          <span v-if="!info.granted.length" class="sub-text">{{ $t('ecomOper.common.none') }}</span>
          <a-tag v-for="m in info.granted" :key="m.id" :color="findDict(dict.ecom_store_member_role, m.memberRole)?.tagColor || 'default'" class="tag">
            {{ m.storeName }} · {{ findDict(dict.ecom_store_member_role, m.memberRole)?.label || m.memberRole }}
          </a-tag>
        </div>
      </template>
    </a-spin>

    <a-modal v-model:open="assignOpen" :title="$t('ecomOper.auth.assignTitle', { name: info?.nickname || '' })" :confirm-loading="saving" width="min(640px, 100vw)" @ok="submitAssign">
      <div class="sub-text block">{{ $t('ecomOper.auth.assignTip') }}</div>
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomOper.auth.role')">
          <a-select v-model:value="assignRole" :options="optionsWithCurrent(dict.ecom_store_member_role, assignRole)" class="role" />
        </a-form-item>
        <a-form-item :label="$t('ecomOper.common.store')">
          <a-checkbox-group v-model:value="assignStores" class="stores">
            <a-checkbox v-for="s in assignable" :key="s.id" :value="s.id" class="store-item">{{ s.name }}（{{ platformLabel(s.platform) }} {{ s.marketplace }}）</a-checkbox>
          </a-checkbox-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { EditOutlined } from '@ant-design/icons-vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { saveUserStores, userStores } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { findDict, optionsWithCurrent } from '@/utils/dict'

/** 按用户：查看用户负责与被授权的店铺，勾选店铺批量授权（取消勾选的收回，只处理当前用户能看到的店铺） */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_store_member_role')
const stores = useStores()
const platformLabel = (p) => findDict(dict.ecom_platform, p)?.label || p

const userId = ref()
const info = ref()
const loading = ref(false)
async function load() {
  if (!userId.value) {
    info.value = undefined
    return
  }
  loading.value = true
  try {
    info.value = await userStores(userId.value)
  } finally {
    loading.value = false
  }
}
watch(userId, load)

const assignOpen = ref(false)
const saving = ref(false)
const assignRole = ref()
const assignStores = ref([])
const ownedIds = computed(() => new Set((info.value?.owned || []).map((s) => s.id)))
const assignable = computed(() => stores.value.filter((s) => !ownedIds.value.has(s.id)))
function openAssign() {
  assignStores.value = (info.value?.granted || []).map((m) => m.storeId)
  assignRole.value = info.value?.granted?.[0]?.memberRole || (dict.ecom_store_member_role || [])[0]?.value
  assignOpen.value = true
}
async function submitAssign() {
  if (!assignRole.value) return message.warning(t('common.rule.requiredSelect', { name: t('ecomOper.auth.role') }))
  saving.value = true
  try {
    await saveUserStores({ userId: userId.value, storeIds: assignStores.value, memberRole: assignRole.value })
    message.success(t('common.msg.saveSuccess'))
    assignOpen.value = false
    await load()
    emit('changed')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.user {
  width: 240px;
}
.role {
  width: 200px;
}
.section-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.block {
  margin-bottom: 12px;
}
.tag {
  margin-bottom: 4px;
}
.stores {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow: auto;
}
.store-item {
  margin-left: 0 !important;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
