package com.lemon.erp.modules.ecom.publish.connector;

import java.math.BigDecimal;

/**
 * 平台返回的一个商品的竞品价格
 *
 * @param lowestPrice    竞品最低价（商品价，不含本店），没有竞品为 null
 * @param lowestShipping 最低价对应的运费，没有为 null
 * @param buyboxPrice    购物车价（含运费），没有为 null
 * @param buyboxWinner   本店持有购物车
 * @param competitor     最低价卖家（ID 或名称），没有为 null
 */
public record CompetitivePrice(String sellerSku, String platformItemId, BigDecimal lowestPrice, BigDecimal lowestShipping, BigDecimal buyboxPrice,
                               boolean buyboxWinner, Integer offerCount, String competitor, String currency) {
}
