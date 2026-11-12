<template>
  <a-drawer :open="open" :title="$t('ecomAds.auth.title', { name: store?.storeName || '' })" width="min(560px, 100vw)" destroy-on-close @close="open = false">
    <a-form v-if="connector" layout="vertical">
      <a-form-item v-for="f in connector.fields" :key="f.key" :label="f.label" :required="f.required && !saved(f.key)">
        <a-input-password v-if="f.secret" v-model:value="values[f.key]" autocomplete="new-password" :placeholder="saved(f.key) ? $t('ecomAds.auth.secretSet') : ''" />
        <a-input v-else v-model:value="values[f.key]" :placeholder="saved(f.key) ? $t('ecomAds.auth.secretSet') : ''" />
      </a-form-item>
      <a-form-item v-if="connector.hasSandbox" :label="$t('ecomAds.auth.sandbox')">
        <a-switch v-model:checked="sandbox" />
        <span class="sub-text url">{{ sandbox ? connector.sandboxBaseUrl : connector.productionBaseUrl }}</span>
      </a-form-item>
      <a-form-item v-if="connector.needsProfile" :label="$t('ecomAds.auth.profile')">
        <a-space-compact class="full">
          <a-select v-model:value="profileId" :options="profileOptions" allow-clear show-search :placeholder="$t('ecomAds.auth.profilePh')" class="full" />
          <a-button :loading="loadingProfiles" :disabled="!store?.hasCredentials" @click="loadProfiles">{{ $t('ecomAds.auth.loadProfiles') }}</a-button>
        </a-space-compact>
      </a-form-item>
      <a-form-item :label="$t('ecomAds.auth.baseUrl')">
        <a-input v-model:value="baseUrl" :maxlength="255" :placeholder="$t('ecomAds.auth.baseUrlPh')" />
      </a-form-item>
      <a-form-item :label="$t('ecomAds.auth.sync')">
        <a-switch v-model:checked="sync" />
      </a-form-item>
      <a v-if="connector.docUrl" :href="connector.docUrl" target="_blank" rel="noopener">{{ $t('ecomAds.auth.doc') }}</a>
    </a-form>
    <template #footer>
      <div class="footer">
        <a-button @click="open = false">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { adsProfiles, saveAdsAuth } from '@/api/ecom/ads'
import { t } from '@/locales'

/**
 * 广告授权表单（抽屉）：按广告接口声明的字段生成输入框（敏感字段为密码框，已设置的留空不修改），环境、配置文件（保存授权后获取）、
 * 自定义地址、每天自动同步。open(店铺授权行) 打开
 */
const props = defineProps({ connectors: { type: Array, default: () => [] } })
const emit = defineEmits(['success'])
const open = ref(false)
const saving = ref(false)
const loadingProfiles = ref(false)
const store = ref(null)
const values = ref({})
const sandbox = ref(false)
const baseUrl = ref('')
const profileId = ref()
const sync = ref(true)
const profiles = ref([])

const connector = computed(() => props.connectors.find((c) => c.platform === store.value?.platform))
const saved = (key) => store.value?.credentialKeys?.includes(key)
const profileOptions = computed(() => {
  const list = profiles.value.map((p) => ({ value: p.profileId, label: `${p.profileId} · ${p.countryCode || ''} ${p.accountName || ''}`.trim() }))
  if (profileId.value && !list.some((o) => o.value === profileId.value)) list.unshift({ value: profileId.value, label: profileId.value })
  return list
})

function show(row) {
  store.value = row
  values.value = {}
  sandbox.value = row.adsSandbox === 1
  baseUrl.value = row.adsBaseUrl || ''
  profileId.value = row.adsProfileId || undefined
  sync.value = row.adsSync !== 0
  profiles.value = []
  open.value = true
}

async function loadProfiles() {
  loadingProfiles.value = true
  try {
    profiles.value = (await adsProfiles(store.value.storeId)) || []
    if (!profiles.value.length) message.warning(t('ecomAds.auth.noProfiles'))
  } finally {
    loadingProfiles.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const credentials = Object.fromEntries(Object.entries(values.value).filter(([, v]) => v !== undefined && v !== ''))
    await saveAdsAuth(store.value.storeId, {
      credentials,
      sandbox: sandbox.value ? 1 : 0,
      baseUrl: baseUrl.value || null,
      profileId: profileId.value || null,
      sync: sync.value ? 1 : 0
    })
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.full {
  width: 100%;
}
.url {
  margin-left: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
