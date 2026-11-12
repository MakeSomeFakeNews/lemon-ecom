package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 待写入的平台订单（导入合并后、渠道接口同步得到），金额为订单币种
 */
@Data
public class OrderDraft {

    private Long storeId;
    private String platformOrderNo;
    private LocalDateTime orderTime;
    private LocalDateTime paidTime;
    private EcomOrderStatus status;
    private FulfillmentType fulfillment;
    private String buyerName;
    private String buyerEmail;
    private String shipName;
    private String shipPhone;
    private String shipCountry;
    private String shipState;
    private String shipCity;
    private String shipPostcode;
    private String shipAddress;
    private String currency;
    private LocalDateTime shipBy;
    private LocalDateTime deliverBy;
    private String note;
    /** 订单级运费（平台只给订单合计时填写），有值时订单运费取它而不是明细合计，为空按明细合计 */
    private BigDecimal shippingAmount;
    /** 订单级税额，规则同 shippingAmount */
    private BigDecimal taxAmount;
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {
        private String platformLineId;
        private String sellerSku;
        private String platformItemId;
        private String title;
        private BigDecimal qty;
        private BigDecimal price;
        /** 商品金额，为空按数量 × 单价 */
        private BigDecimal amount;
        private BigDecimal shipping;
        private BigDecimal tax;
        private BigDecimal discount;
    }
}
