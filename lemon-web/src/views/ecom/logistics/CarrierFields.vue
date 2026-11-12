<template>
  <div class="section-title">{{ $t('ecomOrder.carrier.section') }}</div>
  <a-row :gutter="16">
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrder.carrier.carrier')" name="carrier">
        <a-select v-model:value="form.carrier" allow-clear :placeholder="$t('ecomOrder.carrier.none')" :options="carriers.map((c) => ({ value: c.carrier, label: c.name }))" />
      </a-form-item>
    </a-col>
    <template v-if="info">
      <a-col :xs="24" :sm="12" :lg="6">
        <a-form-item :label="$t('ecomOrder.carrier.service')" name="carrierService" :extra="info.serviceHint">
          <a-input v-model:value="form.carrierService" :maxlength="64" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="4">
        <a-form-item :label="$t('ecomOrder.carrier.sandbox')" name="carrierSandbox">
          <a-switch :checked="form.carrierSandbox === 1" @change="(v) => (form.carrierSandbox = v ? 1 : 0)" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="8">
        <a-form-item :label="$t('ecomOrder.carrier.apiUrl')" name="carrierApiUrl" :extra="$t('ecomOrder.carrier.endpoints', { prod: info.productionUrl, test: info.sandboxUrl })">
          <a-input v-model:value="form.carrierApiUrl" :maxlength="255" :placeholder="$t('ecomOrder.carrier.apiUrlPh')" />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <div class="cred-head">
          <span>{{ $t('ecomOrder.carrier.credentials') }}</span>
          <a-tag :color="form.hasCarrierCredentials ? 'green' : 'default'" :bordered="false">
            {{ form.hasCarrierCredentials ? $t('ecomOrder.carrier.saved') : $t('ecomOrder.carrier.notSaved') }}
          </a-tag>
          <a-popconfirm v-if="form.id && form.hasCarrierCredentials" :title="$t('ecomOrder.carrier.clearConfirm')" @confirm="clear">
            <a-button size="small" type="link" danger>{{ $t('ecomOrder.carrier.clear') }}</a-button>
          </a-popconfirm>
          <a v-if="info.docUrl" :href="info.docUrl" target="_blank" rel="noopener" class="doc-link">{{ $t('ecomOrder.carrier.doc') }}</a>
        </div>
      </a-col>
      <a-col v-for="f in info.fields" :key="f.key" :xs="24" :sm="12" :lg="6">
        <a-form-item :label="f.label" :required="f.required && !form.hasCarrierCredentials">
          <a-input-password v-if="f.secret" v-model:value="form.carrierAuth[f.key]" autocomplete="new-password" :placeholder="form.hasCarrierCredentials ? $t('ecomOrder.carrier.keep') : ''" />
          <a-input v-else v-model:value="form.carrierAuth[f.key]" :placeholder="form.hasCarrierCredentials ? $t('ecomOrder.carrier.keep') : ''" />
        </a-form-item>
      </a-col>
    </template>
  </a-row>
</template>

<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { clearCarrierCredentials } from '@/api/ecom/logistics'

/**
 * 物流渠道表单中的物流商面单对接：物流商、产品代码、测试环境、自定义接口地址，授权字段按物流商声明生成（只写不读，留空不修改）。
 * form 为渠道表单（carrier、carrierService、carrierSandbox、carrierApiUrl、carrierAuth、hasCarrierCredentials），carriers 为 GET /ecom/logistics/carriers
 */
const props = defineProps({ form: { type: Object, required: true }, carriers: { type: Array, default: () => [] } })
const emit = defineEmits(['cleared'])
const { t } = useI18n()

const info = computed(() => props.carriers.find((c) => c.carrier === props.form.carrier))

async function clear() {
  await clearCarrierCredentials(props.form.id)
  message.success(t('ecomOrder.carrier.cleared'))
  emit('cleared')
}
</script>

<style scoped>
.section-title {
  display: block;
  margin: 8px 0 12px;
  font-weight: 500;
}
.cred-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.doc-link {
  margin-left: auto;
}
</style>
