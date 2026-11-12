package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.StockCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台订单出库的公共部分（订单直接出库、包裹发货共用）：SKU → 物料匹配，按订单行（或其中一部分数量）直接开单生成销售出库并取出库成本
 */
@Component
@RequiredArgsConstructor
public class EcomOutboundSupport {

    /** 出库明细的单价精度 */
    private static final int PRICE_SCALE = 4;

    private final EcomListingService listingService;
    private final ScmTradeDocService tradeDocService;
    private final StockCostService stockCostService;

    /**
     * 按在线商品映射填写各行的物料和数量；返回未匹配的说明（没有则为 null）
     */
    public String match(Long storeId, List<EcomOrderItem> items) {
        Map<String, EcomListing> listings = listingService.byStoreSkus(storeId,
                items.stream().map(EcomOrderItem::getSellerSku).collect(Collectors.toSet()));
        List<String> missing = new ArrayList<>();
        for (EcomOrderItem it : items) {
            EcomListing l = listings.get(it.getSellerSku());
            if (l == null || l.getMaterialId() == null) {
                it.setMaterialId(null);
                it.setMaterialQty(null);
                missing.add(it.getSellerSku());
            } else {
                it.setMaterialId(l.getMaterialId());
                it.setMaterialQty(Decimals.qty(it.getQty().multiply(Decimals.nz(l.getPackQty()).signum() > 0 ? l.getPackQty() : BigDecimal.ONE)));
            }
        }
        return missing.isEmpty() ? null : Messages.get("ecom.text.skuUnmapped", String.join(", ", missing));
    }

    /**
     * 出库的一行：订单行及本次出库的件数、物料数量（整行出库时等于订单行的数量）
     */
    public record Line(EcomOrderItem item, BigDecimal qty, BigDecimal materialQty) {

        public static Line whole(EcomOrderItem item) {
            return new Line(item, item.getQty(), item.getMaterialQty());
        }
    }

    /**
     * 出库结果
     *
     * @param costs 各行的出库成本（本位币，与传入的行一一对应）
     */
    public record Result(Long docId, String docNo, List<BigDecimal> costs, BigDecimal totalCost) {
    }

    /**
     * 直接开单生成销售出库：客户为店铺对应的客户，明细单价 = 本行本位币金额 ÷ 本行物料数量（部分出库时单价不变），税率 0；
     * 成本取出库单的计价流水，同一物料多行按数量分摊
     */
    public Result outbound(EcomOrder o, EcomStore store, Long warehouseId, LocalDate date, List<Line> lines, String remark) {
        BigDecimal rate = Decimals.nz(o.getExchangeRate());
        List<TradeDocItemRequest> reqLines = new ArrayList<>();
        for (Line l : lines) {
            EcomOrderItem it = l.item();
            BigDecimal lineFc = it.getAmount().add(Decimals.nz(it.getShipping())).add(Decimals.nz(it.getTax())).subtract(Decimals.nz(it.getDiscount()));
            BigDecimal lineBase = lineFc.multiply(rate);
            BigDecimal price = it.getMaterialQty().signum() > 0 ? lineBase.divide(it.getMaterialQty(), PRICE_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            if (price.signum() < 0) {
                price = BigDecimal.ZERO;
            }
            reqLines.add(new TradeDocItemRequest(null, null, null, null, null, l.materialQty(), null, it.getSellerSku(), it.getMaterialId(),
                    price, BigDecimal.ZERO, null));
        }
        TradeDocRequest req = new TradeDocRequest(null, warehouseId, date, remark, null, reqLines, store.getCustomerId(), null);
        Long docId = tradeDocService.create(TradeKind.SALES_OUT, req);
        ScmTradeDoc doc = tradeDocService.detail(TradeKind.SALES_OUT, docId);

        Map<Long, StockCostService.CostSum> costs = doc.getWmsDocId() == null ? Map.of()
                : stockCostService.docCosts(StockBizType.OUT, List.of(doc.getWmsDocId()));
        Map<Long, BigDecimal> qtyByMaterial = lines.stream().collect(Collectors.toMap(l -> l.item().getMaterialId(), Line::materialQty, BigDecimal::add));
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Line l : lines) {
            StockCostService.CostSum sum = costs.get(l.item().getMaterialId());
            BigDecimal c = BigDecimal.ZERO;
            if (sum != null && sum.amount() != null) {
                BigDecimal all = sum.amount().abs();
                BigDecimal q = qtyByMaterial.get(l.item().getMaterialId());
                c = q.signum() > 0 ? Decimals.money(all.multiply(l.materialQty()).divide(q, 6, RoundingMode.HALF_UP)) : all;
            }
            out.add(c);
            total = total.add(c);
        }
        return new Result(docId, doc.getDocNo(), out, total);
    }
}
