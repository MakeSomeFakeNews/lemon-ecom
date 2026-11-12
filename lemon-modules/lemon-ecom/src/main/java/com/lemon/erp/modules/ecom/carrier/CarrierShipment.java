package com.lemon.erp.modules.ecom.carrier;

import java.math.BigDecimal;
import java.util.List;

/**
 * 向物流商下单的包裹信息
 *
 * @param refNo    客户单号（包裹号），物流商按它防重复下单
 * @param weightKg 重量（kg）
 * @param currency 申报币种（订单币种）
 */
public record CarrierShipment(String refNo, BigDecimal weightKg, String currency, Receiver receiver, List<Item> items) {

    /**
     * 收件人
     *
     * @param country 国家 / 地区二字码
     */
    public record Receiver(String name, String phone, String email, String country, String state, String city, String postcode,
                           String address) {

        /** 名：姓名按最后一个空格拆分，没有空格时整体作为名 */
        public String firstName() {
            String n = name == null ? "" : name.trim();
            int i = n.lastIndexOf(' ');
            return i > 0 ? n.substring(0, i) : n;
        }

        /** 姓 */
        public String lastName() {
            String n = name == null ? "" : name.trim();
            int i = n.lastIndexOf(' ');
            return i > 0 ? n.substring(i + 1) : "";
        }
    }

    /**
     * 申报明细
     *
     * @param nameEn     英文品名（平台商品标题，截断）
     * @param nameCn     中文品名（本地物料名称）
     * @param unitPrice  单件申报价值（订单币种）
     * @param unitWeight 单件重量（kg，按包裹重量平均）
     */
    public record Item(String sku, String nameEn, String nameCn, int qty, BigDecimal unitPrice, BigDecimal unitWeight) {
    }

    /** 申报总价值 */
    public BigDecimal totalValue() {
        return items.stream().map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.qty()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
