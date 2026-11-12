package com.lemon.erp.modules.ecom.dev.ali1688;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 1688 接口返回值转换后的结构
 */
public final class Ali1688Models {

    private Ali1688Models() {
    }

    /**
     * 令牌（授权码换取 / 刷新）
     *
     * @param expireTime        访问令牌过期时间
     * @param refreshExpireTime 刷新令牌过期时间，接口没有返回时为空
     */
    public record Token(String accessToken, String refreshToken, LocalDateTime expireTime, LocalDateTime refreshExpireTime,
                        String memberId, String loginId) {
    }

    /** 阶梯价：起批量及以上的单价 */
    public record PriceRange(Integer startQuantity, BigDecimal price) {
    }

    /** 商品规格 */
    public record Sku(String specId, String skuId, String specText, BigDecimal price, Integer stock) {
    }

    /** 商品（offer） */
    public record Product(String offerId, String title, String imageUrl, String unit, Integer minOrderQty, Integer stock,
                          List<PriceRange> priceRanges, List<Sku> skus, String sellerLoginId, String sellerName, String url) {

        /** 起批量的价格（阶梯价第一档），没有阶梯价时取第一个规格的价格 */
        public BigDecimal basePrice() {
            if (priceRanges != null && !priceRanges.isEmpty() && priceRanges.get(0).price() != null) {
                return priceRanges.get(0).price();
            }
            return skus == null || skus.isEmpty() ? null : skus.get(0).price();
        }

        public Sku sku(String specId) {
            if (specId == null || skus == null) {
                return null;
            }
            return skus.stream().filter(s -> specId.equals(s.specId())).findFirst().orElse(null);
        }
    }

    /** 下单的一行：商品、规格、数量 */
    public record Cargo(String offerId, String specId, long quantity) {
    }

    /** 收货地址 */
    public record Address(String fullName, String mobile, String postCode, String province, String city, String area, String town,
                          String address) {
    }

    /**
     * 创建订单的结果
     *
     * @param totalAmount 订单总额（元，含运费）
     * @param postFee     运费（元）
     */
    public record CreatedOrder(String orderId, BigDecimal totalAmount, BigDecimal postFee) {
    }

    /** 物流单 */
    public record Logistics(String companyName, String billNo) {
    }

    /** 订单详情（买家视角） */
    public record OrderView(String orderId, String status, BigDecimal totalAmount, BigDecimal shippingFee, BigDecimal productAmount,
                            LocalDateTime createTime, LocalDateTime payTime, LocalDateTime shipTime, LocalDateTime receiveTime,
                            String sellerLoginId, String sellerName, List<Logistics> logistics) {
    }

    /** 物流轨迹的一步 */
    public record TraceStep(String time, String remark) {
    }
}
