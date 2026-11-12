package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.modules.ecom.model.EcomCarrier;

import java.util.List;

/**
 * 物流商接口的说明
 *
 * @param nameKey       名称的消息键
 * @param displayName   承运商名称（不翻译，回传平台发货时作为承运商）
 * @param fields        授权字段
 * @param productionUrl 正式环境地址
 * @param sandboxUrl    测试环境地址
 * @param serviceKey    「产品代码」说明的消息键（物流渠道表单提示）
 * @param docUrl        官方文档
 */
public record CarrierInfo(EcomCarrier carrier, String nameKey, String displayName, List<CarrierField> fields, String productionUrl, String sandboxUrl,
                          String serviceKey, String docUrl) {
}
