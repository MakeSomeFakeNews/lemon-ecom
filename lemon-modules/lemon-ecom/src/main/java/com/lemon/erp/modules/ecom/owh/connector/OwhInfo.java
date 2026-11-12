package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.modules.ecom.carrier.CarrierField;
import com.lemon.erp.modules.ecom.owh.model.OwhCapability;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;

import java.util.List;
import java.util.Set;

/**
 * 海外仓接口的说明
 *
 * @param nameKey       名称的消息键
 * @param fields        授权字段（账号表单按它生成输入框，敏感字段只写不读）
 * @param productionUrl 正式环境地址
 * @param sandboxUrl    测试环境地址
 * @param capabilities  支持的能力
 * @param docUrl        官方文档
 */
public record OwhInfo(OwhProvider provider, String nameKey, List<CarrierField> fields, String productionUrl, String sandboxUrl,
                      Set<OwhCapability> capabilities, String docUrl) {
}
