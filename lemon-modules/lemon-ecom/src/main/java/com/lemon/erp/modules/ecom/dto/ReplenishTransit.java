package com.lemon.erp.modules.ecom.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 物料的在途构成：采购在途（已审核、部分入库的采购订单）与头程在途（已发运未签收的货件）
 */
public record ReplenishTransit(List<Purchase> purchases, List<Shipment> shipments) {

    /** 采购订单明细：openQty = 订购 − 已入库 + 需补收的退货 */
    @Data
    public static class Purchase {
        private Long orderId;
        private String orderNo;
        private String supplierName;
        private LocalDate orderDate;
        private LocalDate deliveryDate;
        private String status;
        private BigDecimal qty;
        private BigDecimal doneQty;
        private BigDecimal openQty;
    }

    /** 头程货件明细：openQty = 数量 − 已签收 */
    @Data
    public static class Shipment {
        private Long inboundId;
        private String docNo;
        private String storeName;
        private String shipmentId;
        private String sellerSku;
        private LocalDate shipDate;
        private LocalDate etaDate;
        private BigDecimal qty;
        private BigDecimal receivedQty;
        private BigDecimal openQty;
    }
}
