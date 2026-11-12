package com.lemon.erp.modules.fin.service.cost;

import com.lemon.erp.modules.fin.entity.FinCostLayer;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.service.cost.CostRows.Assembly;
import com.lemon.erp.modules.fin.service.cost.CostRows.AssemblyRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.ConsumeRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostEvent;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostMaterial;
import com.lemon.erp.modules.fin.service.cost.CostRows.IoCategoryMovement;
import com.lemon.erp.modules.fin.service.cost.CostRows.QtyAmount;
import com.lemon.erp.modules.fin.service.cost.CostRows.StockMovement;
import com.lemon.erp.modules.fin.service.cost.CostRows.StockSums;
import com.lemon.erp.modules.fin.service.cost.CostRows.TradeSum;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 一个期间成本核算的输入数据：上期结果与本期业务数据的汇总。由 {@link CostSourceLoader} 从数据库读取，
 * 单元测试中可以直接构造；按行加入时在这里归类汇总（与数据库无关）
 */
@Getter
public final class CostSource {

    private final int period;
    /** 物料 → 上期成本 */
    private final Map<Long, FinMaterialCost> prev = new HashMap<>();
    /** 上期没有核算时（首次核算），物料 → 本期之前全部流水的结存数量，期初金额按 0 */
    private final Map<Long, BigDecimal> openQtyFallback = new HashMap<>();
    /** 物料 → 参考成本价（大于 0） */
    private final Map<Long, BigDecimal> refCosts = new HashMap<>();
    /** 物料 → 本期库存流水合计 */
    private final Map<Long, StockSums> stock = new HashMap<>();
    /** 物料 → 种类 → 数量、不含税金额 */
    private final Map<Long, Map<TradeKind, QtyAmount>> trade = new HashMap<>();
    /** 采购订单关闭的尾差调整（不含税、本位币），计入采购金额，不影响数量 */
    private final Map<Long, BigDecimal> purchaseAdjust = new HashMap<>();
    /** 物料 → 入库类别 / 出库类别 → 数量：其他出入库按类别取对方科目 */
    private final Map<Long, Map<String, BigDecimal>> categoryIn = new HashMap<>();
    private final Map<Long, Map<String, BigDecimal>> categoryOut = new HashMap<>();
    /** 低值易耗品部门领用 */
    private final List<ConsumeRow> consumes = new ArrayList<>();
    /** 组装拆卸单号 → 单据（按单据顺序） */
    @Getter(lombok.AccessLevel.NONE)
    private final Map<String, Assembly> assemblies = new LinkedHashMap<>();
    /** 物料 → 组装拆卸的入库数量（组装的成套件、拆卸的子件）、出库数量 */
    private final Map<Long, BigDecimal> assemblyIn = new HashMap<>();
    private final Map<Long, BigDecimal> assemblyOut = new HashMap<>();
    /** 参与核算的物料（物料资料中存在的） */
    private final Map<Long, CostMaterial> materials = new LinkedHashMap<>();
    /** 移动加权平均、先进先出的物料 → 本期出入库明细（按发生顺序） */
    private final Map<Long, List<CostEvent>> events = new HashMap<>();
    /** 先进先出的物料 → 上期结存批次（按顺序） */
    private final Map<Long, List<FinCostLayer>> prevLayers = new HashMap<>();

    public CostSource(int period) {
        this.period = period;
    }

    /** 本期已完成的组装拆卸单 */
    public Collection<Assembly> getAssemblies() {
        return assemblies.values();
    }

    // ---------------------------------------------------------------- 按行加入

    public void addPrev(FinMaterialCost c) {
        prev.put(c.getMaterialId(), c);
    }

    /** 首次核算：本期之前的流水推算期初数量 */
    public void addOpeningMovement(StockMovement row) {
        openQtyFallback.merge(row.materialId(), row.inQty().subtract(row.outQty()), BigDecimal::add);
    }

    public void addRefCost(Long materialId, BigDecimal refCost) {
        refCosts.put(materialId, refCost);
    }

    /** 本期库存流水：合计入库、出库，并按业务类型归入销售、组装拆卸 */
    public void addMovement(StockMovement row) {
        Long m = row.materialId();
        StockSums v = stock.getOrDefault(m, StockSums.ZERO);
        BigDecimal salesOut = v.salesOut();
        BigDecimal salesReturn = v.salesReturn();
        // 销售按仓库出库类型统计（含不经过销售订单、直接在仓库做的销售出库）
        if (row.is(StockOutType.SALES)) {
            salesOut = salesOut.add(row.outQty());
        } else if (row.is(StockInType.SALES_RETURN)) {
            salesReturn = salesReturn.add(row.inQty());
        } else if (row.is(StockInType.ASSEMBLY)) {
            assemblyIn.merge(m, row.inQty(), BigDecimal::add);
            assemblyOut.merge(m, row.outQty(), BigDecimal::add);
        }
        stock.put(m, new StockSums(v.inQty().add(row.inQty()), v.outQty().add(row.outQty()), salesOut, salesReturn));
    }

    public void addConsume(ConsumeRow row) {
        consumes.add(row);
    }

    public void addIoCategory(IoCategoryMovement row) {
        Map<Long, Map<String, BigDecimal>> target = row.kind() == StockBizType.IN ? categoryIn : categoryOut;
        target.computeIfAbsent(row.materialId(), k -> new HashMap<>()).merge(row.ioCategory(), row.qty(), BigDecimal::add);
    }

    public void addTrade(TradeSum row) {
        trade.computeIfAbsent(row.materialId(), k -> new HashMap<>()).put(row.kind(), new QtyAmount(row.qty(), row.net()));
    }

    public void addPurchaseAdjust(Long materialId, BigDecimal net) {
        purchaseAdjust.merge(materialId, net, BigDecimal::add);
    }

    public void addAssembly(AssemblyRow row) {
        Assembly a = assemblies.computeIfAbsent(row.assemblyNo(), no -> new Assembly(no, row.kind() == AssemblyKind.ASSEMBLE,
                row.productId(), row.qty(), new LinkedHashMap<>()));
        a.parts().merge(row.partId(), row.partQty(), BigDecimal::add);
    }

    public void addMaterial(CostMaterial m) {
        materials.put(m.id(), m);
    }

    public void addEvent(CostEvent row) {
        events.computeIfAbsent(row.materialId(), k -> new ArrayList<>()).add(row);
    }

    public void addPrevLayer(FinCostLayer l) {
        prevLayers.computeIfAbsent(l.getMaterialId(), k -> new ArrayList<>()).add(l);
    }

    // ---------------------------------------------------------------- 取数中间步骤

    /** 涉及的全部物料（按 ID 排序） */
    public Set<Long> involvedMaterialIds() {
        Set<Long> all = new TreeSet<>(prev.keySet());
        all.addAll(openQtyFallback.keySet());
        all.addAll(stock.keySet());
        all.addAll(trade.keySet());
        all.addAll(purchaseAdjust.keySet());
        assemblies.values().forEach(a -> {
            all.add(a.product());
            all.addAll(a.parts().keySet());
        });
        return all;
    }
}
