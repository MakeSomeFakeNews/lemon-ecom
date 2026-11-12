package com.lemon.erp.modules.ecom.publish.dto;

import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 刊登与定价的请求
 */
public final class PublishRequests {

    private PublishRequests() {
    }

    /**
     * 从物料 / SPU 创建刊登草稿并选择店铺
     *
     * @param title           主标题，为空取物料名称
     * @param storeIds        刊登的店铺
     * @param templateIds     各平台使用的刊登模板：平台（EcomPlatform 名称）→ 模板 ID，可为空
     * @param priceTemplateId 价格模板（为空时取刊登模板上的默认价格模板）
     */
    public record CreateDraft(@NotNull(message = "{ecom.valid.pub.materialRequired}") Long materialId, String title,
                              @NotEmpty(message = "{ecom.valid.pub.storeRequired}") @Size(max = 50, message = "{ecom.valid.pub.tooManyStores}") List<Long> storeIds,
                              Map<String, Long> templateIds, Long priceTemplateId) {
    }

    /** 给草稿增加店铺 */
    public record AddStores(@NotEmpty(message = "{ecom.valid.pub.storeRequired}") @Size(max = 50, message = "{ecom.valid.pub.tooManyStores}") List<Long> storeIds,
                            Map<String, Long> templateIds, Long priceTemplateId) {
    }

    /**
     * 把草稿的主内容应用到店铺的刊登记录（只改草稿 / 失败 / 已下架的）
     *
     * @param fields 要覆盖的内容：title、bullets、description、images、brand、barcode，为空全部
     */
    public record ApplyMaster(List<String> fields) {
    }

    /**
     * 定价试算
     *
     * @param cost  单件成本（本位币），为空时按物料参考成本
     * @param price 售价（店铺币种），为空时按建议价计算利润
     */
    public record Quote(@NotNull(message = "{ecom.valid.pub.priceTemplateRequired}") Long priceTemplateId, Long storeId, String currency,
                        Long materialId, BigDecimal cost, BigDecimal price) {
    }

    /**
     * 批量改价 / 改库存
     *
     * @param mode            FIXED 固定值、PERCENT 按百分比加减、AMOUNT 按金额加减、FORMULA 按价格模板（只用于价格）
     * @param value           值（FORMULA 时不用）
     * @param priceTemplateId 按公式时的价格模板
     * @param push            推送到平台（false 只改本地）
     * @param lockStock       改库存时锁定为手工库存（自动库存回传跳过这些商品），为空时改库存默认锁定
     */
    public record BatchChange(@NotEmpty(message = "{ecom.valid.pub.listingRequired}") @Size(max = 500, message = "{ecom.valid.pub.tooManyListings}") List<Long> listingIds,
                              @NotNull(message = "{ecom.valid.pub.changeTypeRequired}") PriceChangeType changeType,
                              @NotNull(message = "{ecom.valid.pub.adjustModeRequired}") PriceAdjustMode mode,
                              @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}") BigDecimal value,
                              Long priceTemplateId, Boolean push, Boolean lockStock) {
    }

    /** 拉取竞品价格：店铺（平台支持 Product Pricing API），在线商品为空时取店铺全部在售商品 */
    public record FetchCompetitors(@NotNull(message = "{ecom.valid.pub.storeRequired}") Long storeId, List<Long> listingIds) {
    }

    /** 竞品价格导入的一行：店铺编码 + 平台 SKU 定位在线商品 */
    @lombok.Data
    public static class CompetitorImportRow {
        @jakarta.validation.constraints.NotBlank(message = "{ecom.valid.pub.storeCodeRequired}")
        private String storeCode;
        @jakarta.validation.constraints.NotBlank(message = "{ecom.valid.pub.skuRequired}")
        private String sellerSku;
        @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
        private BigDecimal lowestPrice;
        @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
        private BigDecimal lowestShipping;
        @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
        private BigDecimal buyboxPrice;
        private Integer buyboxWinner;
        private Integer offerCount;
        @Size(max = 128, message = "{ecom.valid.pub.competitorTooLong}")
        private String competitor;
        /** 快照时间（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss），为空为导入时间 */
        private String snapshotTime;
        @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
        private String remark;
    }
}
