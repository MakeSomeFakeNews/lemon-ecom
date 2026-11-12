package com.lemon.erp.modules.ecom.connector;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 从平台拉取的一个在线商品（平台 SKU）。
 * <p>
 * ID 约定：platformItemId 为平台上最细一级的 ID（SKU / 变体 / 规格 ID，没有规格时为商品 ID），parentItemId 为它所属的商品 ID；
 * 其他平台特有的 ID（Shopify 的变体 ID、库存项 ID，平台仓库 ID 等）放在 extra 中，保存到在线商品的 platform_extra，
 * 回传库存时原样放进 {@link InventoryUpdate#extra()}。订单明细没有父 ID 字段，平台商品 ID 仍可能是「父:子」的组合编码。
 *
 * @param fulfillment PLATFORM 平台仓配送 / SELLER 自发货，未知为 null
 * @param status      ACTIVE / INACTIVE / INCOMPLETE / DELETED（ListingStatus 取值），未知为 null
 * @param extra       平台附加 ID（键由连接器定义），没有为空 Map；同步时与已保存的合并（同名键覆盖）
 */
public record ListingDraft(String sellerSku, String platformItemId, String parentItemId, String title, String variant,
                           BigDecimal price, String currency, BigDecimal platformQty, String fulfillment, String status,
                           String imageUrl, String url, Map<String, String> extra) {

    /** extra 中的平台仓库 / 地点 ID，回传库存时作为 {@link InventoryUpdate#platformWarehouseId()} */
    public static final String EXTRA_WAREHOUSE_ID = "warehouseId";

    public ListingDraft {
        extra = ConnectorJson.compact(extra);
    }

    public ListingDraft(String sellerSku, String platformItemId, String parentItemId, String title, String variant,
                        BigDecimal price, String currency, BigDecimal platformQty, String fulfillment, String status,
                        String imageUrl, String url) {
        this(sellerSku, platformItemId, parentItemId, title, variant, price, currency, platformQty, fulfillment, status, imageUrl, url, Map.of());
    }
}
