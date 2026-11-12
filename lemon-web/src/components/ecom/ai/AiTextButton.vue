<template>
  <span v-if="!status.available" class="ai-off">
    <a-tooltip :title="status.reason || $t('ecomFix.ai.unavailable')">
      <span>
        <a-button :size="size" disabled>
          <template #icon><RobotOutlined /></template>{{ label }}
        </a-button>
      </span>
    </a-tooltip>
    <a v-if="canConfig && status.loaded" class="config-link" @click="goConfig">{{ $t('ecomFix.ai.goConfig') }}</a>
  </span>
  <a-dropdown-button v-else-if="withOptions" :size="size" :loading="loading" @click="run({})">
    <RobotOutlined /> {{ label }}
    <template #overlay>
      <a-menu @click="onMenu">
        <a-menu-item-group :title="$t('ecomFix.ai.replyIn')">
          <a-menu-item v-for="l in languages" :key="`lang:${l.value}`">{{ l.label }}</a-menu-item>
        </a-menu-item-group>
        <a-menu-divider />
        <a-menu-item key="instruction"><EditOutlined /> {{ $t('ecomFix.ai.withInstruction') }}</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown-button>
  <a-button v-else :size="size" :loading="loading" @click="run({})">
    <template #icon><RobotOutlined /></template>{{ label }}
  </a-button>

  <a-modal v-model:open="askOpen" :title="$t('ecomFix.ai.withInstruction')" :confirm-loading="loading" @ok="confirmInstruction">
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomFix.ai.language')">
        <a-select v-model:value="ask.language" allow-clear :placeholder="$t('ecomFix.ai.autoLanguage')" :options="languages" />
      </a-form-item>
      <a-form-item :label="$t('ecomFix.ai.instruction')">
        <a-textarea v-model:value="ask.instruction" :maxlength="500" show-count :auto-size="{ minRows: 3, maxRows: 6 }" :placeholder="$t('ecomFix.ai.instructionPh')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { EditOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { useEcomAi } from './useEcomAi'

/**
 * AI 写作按钮：AI 未开启时禁用并提示原因（有「AI 设置」权限时显示「去开启」）；
 * withOptions 时带下拉：指定回复语言（业务字典 ecom_cs_language）、填写补充要求。
 * - label 按钮文字；action({ language, instruction }) 返回 Promise<string>；@done(text) 生成成功
 */
const props = defineProps({
  label: { type: String, required: true },
  action: { type: Function, required: true },
  withOptions: { type: Boolean, default: false },
  size: { type: String, default: 'small' }
})
const emit = defineEmits(['done'])
const router = useRouter()
const user = useUserStore()
const { status } = useEcomAi()
const dict = useDict('ecom_cs_language')
const languages = computed(() => toOptions(dict.ecom_cs_language))
const canConfig = computed(() => user.hasPerm('ai:config:list'))
const loading = ref(false)
const askOpen = ref(false)
const ask = reactive({ language: undefined, instruction: '' })

async function run(params) {
  if (loading.value) return
  loading.value = true
  try {
    const text = await props.action(params)
    if (text) emit('done', text)
    return true
  } finally {
    loading.value = false
  }
}

function onMenu({ key }) {
  if (key === 'instruction') {
    askOpen.value = true
  } else if (String(key).startsWith('lang:')) {
    run({ language: String(key).slice(5) })
  }
}

async function confirmInstruction() {
  const ok = await run({ language: ask.language, instruction: ask.instruction })
  if (ok) askOpen.value = false
}

const goConfig = () => router.push('/ai/settings')
</script>

<style scoped>
.config-link {
  margin-left: 6px;
  font-size: 12px;
}
</style>
