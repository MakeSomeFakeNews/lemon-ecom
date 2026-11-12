package com.lemon.erp.modules.ecom.connector;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 回传给平台的一个 SKU 的可售数量（平台 SKU 件数，已按多件装折算并扣除安全库存）。ID 均取自在线商品（拉取在线商品时由连接器写入）
 *
 * @param platformItemId      平台商品 / SKU ID（部分平台按商品 + SKU 更新库存时需要）
 * @param parentItemId        父商品 ID（Temu goodsId、TikTok product_id、Lazada item_id、美客多刊登 ID 等），可为 null
 * @param platformWarehouseId 平台侧的仓库 / 地点 ID（在线商品附加信息 {@link ListingDraft#EXTRA_WAREHOUSE_ID}），为空时用店铺授权信息中的默认值
 * @param extra               在线商品的平台附加 ID（{@link ListingDraft#extra()}），不为 null
 */
public record InventoryUpdate(String sellerSku, String platformItemId, String parentItemId, String platformWarehouseId, BigDecimal qty,
                              Map<String, String> extra) {

    public InventoryUpdate {
        extra = ConnectorJson.compact(extra);
    }

    public InventoryUpdate(String sellerSku, String platformItemId, BigDecimal qty) {
        this(sellerSku, platformItemId, null, null, qty, Map.of());
    }

    public InventoryUpdate(String sellerSku, String platformItemId, String parentItemId, BigDecimal qty) {
        this(sellerSku, platformItemId, parentItemId, null, qty, Map.of());
    }

    public String extra(String key) {
        String v = extra.get(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 非负整数件数（小数向下取整，空为 0） */
    public int intQty() {
        return qty == null ? 0 : Math.max(0, qty.setScale(0, java.math.RoundingMode.DOWN).intValue());
    }

    /**
     * 父商品 ID 与子 ID：parentItemId 有值时为 [parentItemId, platformItemId]；
     * 兼容旧编码——parentItemId 为空且 platformItemId 为「父:子」时按第一个冒号拆开；都不满足时为 null
     */
    public String[] parentAndChild() {
        if (parentItemId != null && !parentItemId.isBlank() && platformItemId != null && !platformItemId.isBlank()) {
            String child = platformItemId.trim();
            String prefix = parentItemId.trim() + ":";
            return new String[]{parentItemId.trim(), child.startsWith(prefix) ? child.substring(prefix.length()) : child};
        }
        if (platformItemId == null) {
            return null;
        }
        int i = platformItemId.indexOf(':');
        if (i <= 0 || i == platformItemId.length() - 1) {
            return null;
        }
        return new String[]{platformItemId.substring(0, i).trim(), platformItemId.substring(i + 1).trim()};
    }
}
