package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Order;
import com.lemon.erp.modules.scm.model.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 1688 采购的接口返回结构
 */
public final class Ali1688Views {

    private Ali1688Views() {
    }

    /** 链接解析结果 */
    public record ParsedOffer(String offerId, String specId, String url) {
    }

    /** 同步结果 */
    public record SyncSummary(int total, int success, int changed, int failed, List<String> messages) {
    }

    /** 下单预览的一行（采购订单明细 → 1688 规格与数量） */
    public record PreviewLine(Long scmOrderItemId, Long materialId, String materialCode, String materialName, BigDecimal materialQty,
                              String offerId, String specId, String title, String specText, String imageUrl, BigDecimal qtyRatio,
                              long aliQty, Integer minOrderQty, BigDecimal price, BigDecimal amount, boolean belowMinQty) {
    }

    /** 按卖家拆分的一张 1688 订单 */
    public record PreviewGroup(String sellerLoginId, String sellerName, List<PreviewLine> lines, BigDecimal amount) {
    }

    /** 没有关联 1688 商品或已下过单的明细 */
    public record SkippedLine(Long scmOrderItemId, String materialCode, String materialName, BigDecimal qty, String reason) {
    }

    /**
     * 一键下单预览
     *
     * @param canOrder 采购订单已审核且有可下单的明细
     */
    public record OrderPreview(Long scmOrderId, String scmOrderNo, OrderStatus scmOrderStatus, String supplierName, boolean canOrder,
                               List<PreviewGroup> groups, List<SkippedLine> skipped, List<Ecom1688Order> existing) {
    }

    /** 一键下单结果：成功的 1688 订单与失败的卖家 */
    public record CreateResult(List<Ecom1688Order> created, List<String> failures) {
    }

    /** 物流轨迹 */
    public record TraceStep(String time, String remark) {
    }
}
