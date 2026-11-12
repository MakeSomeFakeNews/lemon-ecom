package com.lemon.erp.modules.ecom.publish.dto;

import com.lemon.erp.modules.ecom.publish.connector.PublishCapability;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import com.lemon.erp.modules.ecom.publish.model.RepriceLogStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 刊登与定价接口返回的视图
 */
public final class PublishViews {

    private PublishViews() {
    }

    /** 刊登记录各状态的数量 */
    @Data
    public static class StatusCount {
        private PublishStatus status;
        private Long cnt;
    }

    /** 在线商品的近期销量 */
    @Data
    public static class ListingSales {
        private Long listingId;
        private BigDecimal qty;
    }

    /**
     * 定价试算结果（金额为店铺币种，成本为本位币）
     *
     * @param cost           单件成本（本位币），没有成本时为 null
     * @param rate           汇率（1 店铺币种 = rate 本位币）
     * @param suggestedPrice 按模板计算的建议价
     * @param price          用于计算利润的售价（传入的售价，没有传时为建议价）
     * @param profit         单件预估利润（店铺币种）
     * @param margin         预估利润率（%）
     * @param fees           平台佣金 + 其他费率 + 固定费（店铺币种）
     */
    public record PriceQuote(BigDecimal cost, BigDecimal rate, String currency, BigDecimal suggestedPrice, BigDecimal price, BigDecimal profit,
                             BigDecimal margin, BigDecimal fees) {
    }

    /** 店铺的刊登能力（页面据此决定类目 / 属性是接口选择还是手工填写，是否能发布、下架、改价） */
    public record StoreCapabilities(Long storeId, String platform, boolean connector, boolean hasCredentials, Set<PublishCapability> capabilities,
                                    boolean categoryRequired, String note) {
    }

    /** 提交 / 下架的结果：成功的数量与逐条的错误 */
    public record ActionResult(int total, int succeeded, List<ActionError> errors) {
    }

    public record ActionError(Long id, String name, String message) {
    }

    /** 批量改价 / 改库存的一行（预览与执行结果） */
    public record ChangeRow(Long listingId, Long storeId, String storeName, String sellerSku, String title, String currency, BigDecimal oldValue,
                            BigDecimal newValue, BigDecimal estMargin, PriceChangeStatus status, String message) {
    }

    /** 批量改价 / 改库存的结果 */
    public record ChangeResult(String batchNo, int total, int succeeded, int failed, int local, List<ChangeRow> rows) {
    }

    /** 自动调价的一条计算结果（预览、执行） */
    public record RepriceRow(Long listingId, Long storeId, String storeName, String sellerSku, String title, String currency, BigDecimal oldPrice,
                             BigDecimal newPrice, BigDecimal lowestPrice, BigDecimal buyboxPrice, BigDecimal floorPrice, RepriceLogStatus status,
                             String reason, String message) {
    }

    /** 自动调价执行结果 */
    public record RepriceResult(Long ruleId, String ruleName, int checked, int changed, int suggested, int pushed, int failed, int skipped,
                                String message, List<RepriceRow> rows) {
    }

    /**
     * 刊登设置
     *
     * @param publicUrlConfigured 已配置后端外网地址 lemon.ecom.public-url（系统内图片可转为公网地址）
     */
    public record Settings(boolean publicUrlConfigured, String publicUrl) {
    }

    /** 拉取竞品价格的结果 */
    public record FetchResult(int requested, int saved, String message) {
    }
}
