<template>
  <a-row :gutter="16">
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrder.order.receiver')" name="shipName">
        <a-input v-model:value="form.shipName" :maxlength="64" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrd.addr.phone')" name="shipPhone">
        <a-input v-model:value="form.shipPhone" :maxlength="32" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrder.order.country')" name="shipCountry">
        <a-input v-model:value="form.shipCountry" :maxlength="8" :placeholder="$t('ecomOrder.order.countryPh')" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrd.addr.state')" name="shipState">
        <a-input v-model:value="form.shipState" :maxlength="64" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrd.addr.city')" name="shipCity">
        <a-input v-model:value="form.shipCity" :maxlength="64" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('ecomOrd.addr.postcode')" name="shipPostcode">
        <a-input v-model:value="form.shipPostcode" :maxlength="16" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :lg="12">
      <a-form-item :label="$t('ecomOrder.order.address')" name="shipAddress">
        <a-input v-model:value="form.shipAddress" :maxlength="255" />
      </a-form-item>
    </a-col>
  </a-row>
</template>

<script setup>
/** 收件地址的输入项（手工建单、补发、修改地址共用），form 为响应式对象，字段 shipName / shipPhone / shipCountry / shipState / shipCity / shipPostcode / shipAddress */
defineProps({ form: { type: Object, required: true } })
</script>
