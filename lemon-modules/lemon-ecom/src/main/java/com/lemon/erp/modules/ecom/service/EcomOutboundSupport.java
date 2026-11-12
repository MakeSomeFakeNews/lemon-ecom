package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomBundleItem;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderItemPart;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemPartMapper;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 平台订单出库的公共部分（订单直接出库、包裹发货共用）：SKU → 物料匹配，按订单行（或其中一部分数量）直接开单生成销售出库并取出库成本。
 * <p>
 * 组合商品（在线商品的组成方式为组合）：匹配时按组成展开为多个物料，记在 {@code ecom_order_item_part}（订单行 bundle = 1，
 * material_id / material_qty 为空），收入按各组件「用量 × 参考成本」的比例分摊（没有参考成本时按用量）；之后的库存检查、包裹、出库、
 * 成本、退货都按展开结果处理
 */
@Component
@RequiredArgsConstructor
public class EcomOutboundSupport {

    /** 出库明细的单价精度 */
    private static final int PRICE_SCALE = 4;
    /** 收入分摊比例的精度 */
    private static final int SHARE_SCALE = 6;

    private final EcomListingService listingService;
    private final EcomBundleService bundleService;
    private final EcomOrderItemMapper itemMapper;
    private final EcomOrderItemPartMapper partMapper;
    private final ScmTradeDocService tradeDocService;
    private final StockCostService stockCostService;

    /** 订单行是否已匹配物料：单品有物料和数量，组合有展开的组件 */
    public static boolean mapped(EcomOrderItem it) {
        if (isBundle(it)) {
            return it.getParts() != null && !it.getParts().isEmpty();
        }
        return it.getMaterialId() != null && it.getMaterialQty() != null;
    }

    public static boolean isBundle(EcomOrderItem it) {
        return it.getBundle() != null && it.getBundle() == 1;
    }

    /**
     * 按在线商品映射填写各行的物料和数量（组合商品展开到 parts，不写库）；返回未匹配的说明（没有则为 null）
     */
    public String match(Long storeId, List<EcomOrderItem> items) {
        Map<String, EcomListing> listings = listingService.byStoreSkus(storeId,
                items.stream().map(EcomOrderItem::getSellerSku).collect(Collectors.toSet()));
        List<Long> bundleIds = listings.values().stream().filter(l -> l.getComposeType() == ListingComposeType.BUNDLE).map(EcomListing::getId).toList();
        Map<Long, List<EcomBundleItem>> bundles = bundleService.itemsOf(bundleIds);
        List<String> missing = new ArrayList<>();
        for (EcomOrderItem it : items) {
            EcomListing l = listings.get(it.getSellerSku());
            it.setBundle(0);
            it.setParts(new ArrayList<>());
            it.setMaterialId(null);
            it.setMaterialQty(null);
            if (l != null && l.getComposeType() == ListingComposeType.BUNDLE) {
                List<EcomBundleItem> comps = bundles.get(l.getId());
                if (comps == null || comps.isEmpty()) {
                    missing.add(it.getSellerSku());
                } else {
                    it.setBundle(1);
                    it.setParts(expandBundle(it, comps));
                }
            } else if (l == null || l.getMaterialId() == null) {
                missing.add(it.getSellerSku());
            } else {
                it.setMaterialId(l.getMaterialId());
                it.setMaterialQty(Decimals.qty(it.getQty().multiply(EcomBundleService.packOf(l.getPackQty()))));
            }
        }
        return missing.isEmpty() ? null : Messages.get("ecom.text.skuUnmapped", String.join(", ", missing));
    }

    /** 组合商品订单行按组成展开：每件的用量、物料数量、收入分摊比例（按 用量 × 参考成本，都没有参考成本时按用量；最后一个取余数） */
    static List<EcomOrderItemPart> expandBundle(EcomOrderItem it, List<EcomBundleItem> comps) {
        boolean byCost = comps.stream().allMatch(c -> c.getRefCost() != null && c.getRefCost().signum() > 0);
        List<BigDecimal> weights = comps.stream().map(c -> byCost ? c.getQty().multiply(c.getRefCost()) : c.getQty()).toList();
        BigDecimal total = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<EcomOrderItemPart> parts = new ArrayList<>();
        BigDecimal allotted = BigDecimal.ZERO;
        for (int i = 0; i < comps.size(); i++) {
            EcomBundleItem c = comps.get(i);
            EcomOrderItemPart p = new EcomOrderItemPart();
            p.setOrderId(it.getOrderId());
            p.setOrderItemId(it.getId());
            p.setMaterialId(c.getMaterialId());
            p.setUnitQty(c.getQty());
            p.setMaterialQty(Decimals.qty(it.getQty().multiply(c.getQty())));
            BigDecimal share = i == comps.size() - 1 ? BigDecimal.ONE.subtract(allotted)
                    : total.signum() > 0 ? weights.get(i).divide(total, SHARE_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            allotted = allotted.add(share);
            p.setShare(share);
            p.setSort(i + 1);
            p.setMaterialCode(c.getMaterialCode());
            p.setMaterialName(c.getMaterialName());
            p.setMaterialSpec(c.getMaterialSpec());
            p.setUnit(c.getUnit());
            parts.add(p);
        }
        return parts;
    }

    /**
     * 匹配并写回订单行（物料、数量、是否组合，未匹配的清空）与组合商品的展开结果（调用方的事务中执行）
     *
     * @return 未匹配的说明，全部匹配时为 null
     */
    public String matchAndSave(Long storeId, List<EcomOrderItem> items) {
        String exception = match(storeId, items);
        for (EcomOrderItem it : items) {
            itemMapper.update(Wrappers.<EcomOrderItem>lambdaUpdate().set(EcomOrderItem::getMaterialId, it.getMaterialId())
                    .set(EcomOrderItem::getMaterialQty, it.getMaterialQty()).set(EcomOrderItem::getBundle, it.getBundle())
                    .eq(EcomOrderItem::getId, it.getId()));
        }
        saveParts(null, items);
        return exception;
    }

    /**
     * 保存组合商品的展开结果（先删除这些行原来的）；新写入的订单行还没有 ID 时按订单重新读取 ID（与写入顺序一致）
     *
     * @param orderId 订单 ID（订单行没有 ID 时必填）
     */
    public void saveParts(Long orderId, List<EcomOrderItem> items) {
        if (items.stream().anyMatch(i -> i.getId() == null)) {
            if (items.stream().noneMatch(EcomOutboundSupport::isBundle)) {
                return;
            }
            List<EcomOrderItem> saved = itemMapper.selectByOrderId(orderId);
            for (int i = 0; i < items.size() && i < saved.size(); i++) {
                items.get(i).setId(saved.get(i).getId());
                items.get(i).setOrderId(orderId);
            }
        }
        List<Long> ids = items.stream().map(EcomOrderItem::getId).filter(Objects::nonNull).toList();
        if (!ids.isEmpty()) {
            partMapper.delete(Wrappers.<EcomOrderItemPart>lambdaQuery().in(EcomOrderItemPart::getOrderItemId, ids));
        }
        List<EcomOrderItemPart> parts = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (isBundle(it) && it.getParts() != null) {
                it.getParts().forEach(p -> {
                    p.setOrderId(it.getOrderId());
                    p.setOrderItemId(it.getId());
                    parts.add(p);
                });
            }
        }
        if (!parts.isEmpty()) {
            partMapper.insertBatch(parts);
            reloadParts(items);
        }
    }

    /** 带上组合商品订单行的展开结果（含物料编码、名称） */
    public List<EcomOrderItem> attachParts(List<EcomOrderItem> items) {
        List<Long> ids = items.stream().filter(EcomOutboundSupport::isBundle).map(EcomOrderItem::getId).filter(Objects::nonNull).toList();
        Map<Long, List<EcomOrderItemPart>> parts = ids.isEmpty() ? Map.of()
                : partMapper.selectByOrderItemIds(ids).stream().collect(Collectors.groupingBy(EcomOrderItemPart::getOrderItemId));
        for (EcomOrderItem it : items) {
            it.setParts(isBundle(it) ? new ArrayList<>(parts.getOrDefault(it.getId(), List.of())) : new ArrayList<>());
        }
        return items;
    }

    private void reloadParts(List<EcomOrderItem> items) {
        attachParts(items);
    }

    /** 组件的出库成本累加（发货时逐个包裹累加） */
    public void addPartCost(Long partId, BigDecimal cost) {
        EcomOrderItemPart p = partMapper.selectById(partId);
        if (p != null) {
            partMapper.update(Wrappers.<EcomOrderItemPart>lambdaUpdate().set(EcomOrderItemPart::getCost, Decimals.money(Decimals.nz(p.getCost()).add(cost)))
                    .eq(EcomOrderItemPart::getId, partId));
        }
    }

    /**
     * 出库的一行：订单行（组合商品为其中一个组件）及本次出库的件数、物料数量（整行出库时等于订单行的数量）
     */
    public record Line(EcomOrderItem item, BigDecimal qty, BigDecimal materialQty, EcomOrderItemPart part) {

        public Line(EcomOrderItem item, BigDecimal qty, BigDecimal materialQty) {
            this(item, qty, materialQty, null);
        }

        /** 本行出库的物料 */
        public Long materialId() {
            return part != null ? part.getMaterialId() : item.getMaterialId();
        }

        /** 整行出库（单品） */
        public static Line whole(EcomOrderItem item) {
            return new Line(item, item.getQty(), item.getMaterialQty());
        }

        /** 整行出库：单品一行，组合商品每个组件一行（订单行需已带上 parts） */
        public static List<Line> wholeExpanded(EcomOrderItem item) {
            if (!isBundle(item)) {
                return List.of(whole(item));
            }
            return item.getParts().stream().map(p -> new Line(item, item.getQty(), p.getMaterialQty(), p)).toList();
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
     * 直接开单生成销售出库：客户为店铺对应的客户，明细单价 = 本行本位币金额 × 分摊比例 ÷ 本行物料数量（部分出库时单价不变），税率 0；
     * 成本取出库单的计价流水，同一物料多行按数量分摊
     */
    public Result outbound(EcomOrder o, EcomStore store, Long warehouseId, LocalDate date, List<Line> lines, String remark) {
        BigDecimal rate = Decimals.nz(o.getExchangeRate());
        List<TradeDocItemRequest> reqLines = new ArrayList<>();
        for (Line l : lines) {
            EcomOrderItem it = l.item();
            BigDecimal lineFc = it.getAmount().add(Decimals.nz(it.getShipping())).add(Decimals.nz(it.getTax())).subtract(Decimals.nz(it.getDiscount()));
            BigDecimal lineBase = lineFc.multiply(rate);
            BigDecimal fullQty = l.part() != null ? l.part().getMaterialQty() : it.getMaterialQty();
            if (l.part() != null) {
                lineBase = lineBase.multiply(Decimals.nz(l.part().getShare()));
            }
            BigDecimal price = fullQty != null && fullQty.signum() > 0 ? lineBase.divide(fullQty, PRICE_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            if (price.signum() < 0) {
                price = BigDecimal.ZERO;
            }
            reqLines.add(new TradeDocItemRequest(null, null, null, null, null, l.materialQty(), null, it.getSellerSku(), l.materialId(),
                    price, BigDecimal.ZERO, null));
        }
        TradeDocRequest req = new TradeDocRequest(null, warehouseId, date, remark, null, reqLines, store.getCustomerId(), null);
        Long docId = tradeDocService.create(TradeKind.SALES_OUT, req);
        ScmTradeDoc doc = tradeDocService.detail(TradeKind.SALES_OUT, docId);

        Map<Long, StockCostService.CostSum> costs = doc.getWmsDocId() == null ? Map.of()
                : stockCostService.docCosts(StockBizType.OUT, List.of(doc.getWmsDocId()));
        Map<Long, BigDecimal> qtyByMaterial = lines.stream().collect(Collectors.toMap(Line::materialId, Line::materialQty, BigDecimal::add));
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Line l : lines) {
            StockCostService.CostSum sum = costs.get(l.materialId());
            BigDecimal c = BigDecimal.ZERO;
            if (sum != null && sum.amount() != null) {
                BigDecimal all = sum.amount().abs();
                BigDecimal q = qtyByMaterial.get(l.materialId());
                c = q.signum() > 0 ? Decimals.money(all.multiply(l.materialQty()).divide(q, 6, RoundingMode.HALF_UP)) : all;
            }
            out.add(c);
            total = total.add(c);
        }
        return new Result(docId, doc.getDocNo(), out, total);
    }

    /** 出库成本按订单行汇总（组合商品各组件合计）：订单行 ID → 成本 */
    public static Map<Long, BigDecimal> costByItem(List<Line> lines, Result r) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            out.merge(lines.get(i).item().getId(), r.costs().get(i), BigDecimal::add);
        }
        return out;
    }

    /** 出库成本按组件汇总：组件 ID → 成本 */
    public static Map<Long, BigDecimal> costByPart(List<Line> lines, Result r) {
        Map<Long, BigDecimal> out = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            EcomOrderItemPart p = lines.get(i).part();
            if (p != null && p.getId() != null) {
                out.merge(p.getId(), r.costs().get(i), BigDecimal::add);
            }
        }
        return out;
    }
}
