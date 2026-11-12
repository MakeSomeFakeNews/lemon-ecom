<template>
  <div v-if="visible" class="page-card demo-banner">
    <div class="demo-icon"><ExperimentOutlined /></div>
    <div class="demo-body">
      <template v-if="status.running">
        <div class="demo-title">{{ $t('ecomDemo.running') }}</div>
        <a-progress :percent="percent" size="small" status="active" />
        <div class="sub-text">{{ $t('ecomDemo.current', { part: partLabel(status.current), done: status.done, total: status.total }) }}</div>
      </template>
      <template v-else>
        <div class="demo-title">{{ $t('ecomDemo.title') }}</div>
        <div class="sub-text">{{ $t('ecomDemo.desc') }}</div>
        <div v-if="failures.length" class="demo-failures">
          {{ $t('ecomDemo.failed') }}
          <span v-for="f in failures" :key="f.key" class="demo-failure">{{ $t('ecomDemo.failureItem', { part: partLabel(f.key), message: f.message }) }}</span>
        </div>
      </template>
    </div>
    <a-button v-if="!status.running && !status.generated" v-perm="'ecom:demo:generate'" type="primary" :loading="starting" @click="confirmGenerate">
      <template #icon><ThunderboltOutlined /></template>{{ $t('ecomDemo.generate') }}
    </a-button>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ExperimentOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { demoGenerate, demoStatus } from '@/api/ecom/demo'
import { t, tt } from '@/locales'

/**
 * 电商看板的「生成演示数据」入口：看板没有数据（empty）且还没有生成过时显示按钮，确认后后台生成并显示进度，
 * 完成后提示并触发 done（看板刷新）。生成中（包括其他人或启动时自动生成）始终显示进度
 */
const props = defineProps({
  /** 看板是否没有数据（本月与今天都没有订单） */
  empty: { type: Boolean, default: false }
})
const emit = defineEmits(['done'])

const status = ref({})
const starting = ref(false)
let timer = null

const visible = computed(() => status.value.running || (props.empty && (!status.value.generated || failures.value.length > 0)))
const percent = computed(() => (status.value.total ? Math.round((status.value.done * 100) / status.value.total) : 0))
const failures = computed(() => status.value.failures || [])

/** 组成部分的名称：语言包中有的显示名称，其他（各功能自己的部分）显示标识 */
function partLabel(key) {
  return key ? tt(`ecomDemo.parts.${key}`, key) : ''
}

async function load() {
  status.value = (await demoStatus()) || {}
  if (status.value.running) schedule()
}

function schedule() {
  clearTimeout(timer)
  timer = setTimeout(poll, 2000)
}

async function poll() {
  const wasRunning = status.value.running
  status.value = (await demoStatus()) || {}
  if (status.value.running) {
    schedule()
  } else if (wasRunning) {
    if (failures.value.length) {
      message.warning(t('ecomDemo.doneWithFailures', { count: failures.value.length }))
    } else {
      message.success(t('ecomDemo.done'))
    }
    emit('done')
  }
}

function confirmGenerate() {
  Modal.confirm({
    title: t('ecomDemo.confirmTitle'),
    content: t('ecomDemo.confirmContent'),
    okText: t('ecomDemo.generate'),
    onOk: async () => {
      starting.value = true
      try {
        status.value = (await demoGenerate()) || {}
        schedule()
      } finally {
        starting.value = false
      }
    }
  })
}

watch(() => props.empty, (v) => v && !status.value.running && load())
onMounted(load)
onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.demo-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 16px 20px;
  border: 1px dashed #91caff;
  background: linear-gradient(90deg, rgba(22, 119, 255, 0.06), rgba(22, 119, 255, 0));
}
.demo-icon {
  font-size: 28px;
  color: #1677ff;
}
.demo-body {
  flex: 1;
  min-width: 0;
}
.demo-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.demo-failures {
  margin-top: 6px;
  color: #d4380d;
  font-size: 12px;
}
.demo-failure {
  margin-left: 8px;
}
</style>

<style>
html[data-theme='dark'] .demo-banner {
  border-color: #15325b;
  background: linear-gradient(90deg, rgba(22, 119, 255, 0.12), rgba(22, 119, 255, 0));
}
</style>
