<template>
  <a-drawer :open="open" :title="$t('ecomAds.auth.title', { name: store?.storeName || '' })" width="min(560px, 100vw)" destroy-on-close @close="open = false">
    <a-form v-if="connector" layout="vertical">
      <div v-if="isOAuth" class="oauth">
        <div class="oauth-title">{{ $t('ecomFix.ads.oauthTitle') }}</div>
        <div class="sub-text">{{ $t('ecomFix.ads.oauthSteps') }}</div>
        <div class="sub-text callback">{{ $t('ecomFix.ads.callbackUrl') }}<a-typography-text copyable code>{{ callbackUrl }}</a-typography-text></div>
        <a-space wrap>
          <a-button v-perm="'ecom:ad:auth'" type="primary" :loading="authorizing" @click="authorize">
            <template #icon><LoginOutlined /></template>{{ hasRefresh ? $t('ecomFix.ads.reauthorize') : $t('ecomFix.ads.authorize') }}
          </a-button>
          <a-tag v-if="hasRefresh" color="green">{{ $t('ecomFix.ads.authorized') }}</a-tag>
          <span v-if="polling" class="sub-text"><LoadingOutlined /> {{ $t('ecomFix.ads.waiting') }}</span>
        </a-space>
        <div class="sub-text manual">{{ $t('ecomFix.ads.manualTip') }}</div>
      </div>
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
import { computed, onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import { LoadingOutlined, LoginOutlined } from '@ant-design/icons-vue'
import { adsAuthorizeUrl, adsOAuthStatus, adsProfiles, saveAdsAuth } from '@/api/ecom/ads'
import { AuthType } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 广告授权表单（抽屉）：按广告接口声明的字段生成输入框（敏感字段为密码框，已设置的留空不修改），环境、配置文件（保存授权后获取）、
 * 自定义地址、每天自动同步。支持 OAuth 的接口（亚马逊 Login with Amazon）可以「使用账号授权」：先保存应用信息，再在新窗口授权，
 * 授权完成后自动获取配置文件（只有一个时后端已自动选中，多个时在下拉中选择）；也可以手工填写刷新令牌。open(店铺授权行) 打开
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
const isOAuth = computed(() => connector.value?.authType === AuthType.OAUTH)
const hasRefresh = computed(() => !!store.value?.credentialKeys?.includes('refreshToken'))
/** 回调地址（需要登记在 LWA 安全配置文件的 Allowed Return URLs 中；反向代理后以 lemon.ecom.public-url 为准） */
const callbackUrl = `${window.location.origin}/api/ecom/oauth/callback/amazon-ads`
const authorizing = ref(false)
const polling = ref(false)
let pollTimer = null
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

function payload() {
  const credentials = Object.fromEntries(Object.entries(values.value).filter(([, v]) => v !== undefined && v !== ''))
  return {
    credentials,
    sandbox: sandbox.value ? 1 : 0,
    baseUrl: baseUrl.value || null,
    profileId: profileId.value || null,
    sync: sync.value ? 1 : 0
  }
}

/** 使用账号授权：先保存表单中的应用信息，再打开授权页并轮询授权结果（最多 5 分钟或授权窗口关闭） */
async function authorize() {
  authorizing.value = true
  try {
    await saveAdsAuth(store.value.storeId, payload())
    values.value = {}
    const url = await adsAuthorizeUrl(store.value.storeId)
    const win = window.open(url, '_blank')
    startPolling(win)
  } finally {
    authorizing.value = false
  }
}

function stopPolling() {
  clearInterval(pollTimer)
  pollTimer = null
  polling.value = false
}

function startPolling(win) {
  stopPolling()
  const hadToken = hasRefresh.value
  const started = Date.now()
  polling.value = true
  pollTimer = setInterval(async () => {
    if (Date.now() - started > 5 * 60 * 1000) return stopPolling()
    const closed = !win || win.closed
    const st = await adsOAuthStatus(store.value.storeId)
    const hasToken = (st.credentialKeys || []).includes('refreshToken')
    // 第一次授权：拿到刷新令牌即完成；重新授权：等授权窗口关闭后结束
    if (!(hasToken && (!hadToken || closed)) && !closed) return
    stopPolling()
    store.value = { ...store.value, ...st }
    if (!hasToken) return
    profileId.value = st.adsProfileId || profileId.value
    message.success(t('ecomFix.ads.oauthOk'))
    emit('success')
    if (!st.adsProfileId) {
      await loadProfiles()
      if (profiles.value.length) message.info(t('ecomFix.ads.chooseProfile'))
    }
  }, 3000)
}

onBeforeUnmount(stopPolling)

async function save() {
  saving.value = true
  try {
    await saveAdsAuth(store.value.storeId, payload())
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
.oauth {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  background: var(--app-bg);
}
.oauth-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.callback {
  margin: 6px 0 10px;
}
.manual {
  margin-top: 8px;
}
.footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
