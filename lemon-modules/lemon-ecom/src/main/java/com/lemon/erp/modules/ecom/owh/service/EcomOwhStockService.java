package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.owh.connector.OwhConnector;
import com.lemon.erp.modules.ecom.owh.connector.OwhContext;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.AdjustRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.AdjustResult;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhStock;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhAccountMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhStockMapper;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 海外仓库存同步与差异调整：定时 / 手工从服务商拉取各映射仓库的库存（可用、在途、锁定），按 SKU 映射（没有映射时按物料编码）匹配物料；
 * 列表把海外仓在库（可用 + 锁定）与 ERP 账面库存并列显示差异，选中行可一键按海外仓数量调整
 * （盘盈生成其他入库、盘亏生成其他出库，已确认）。服务商接口在事务之外调用，每个仓库一个事务写入
 */
@Service
@RequiredArgsConstructor
public class EcomOwhStockService {

    private static final DateTimeFormatter SOURCE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final EcomOwhStockMapper stockMapper;
    private final EcomOwhAccountMapper accountMapper;
    private final EcomOwhAccountService accountService;
    private final EcomOwhSupport support;
    private final EcomOwhSkuService skuService;
    private final WmsStockDocService stockDocService;
    private final TransactionTemplate transactionTemplate;

    public PageResult<EcomOwhStock> page(long current, long size, Long accountId, Long warehouseId, String keyword, boolean diffOnly) {
        Page<EcomOwhStock> page = Page.of(current, size);
        stockMapper.selectPageList(page, accountId, warehouseId, keyword, diffOnly);
        return PageResult.of(page);
    }

    public Map<String, Object> summary(Long accountId, Long warehouseId) {
        Map<String, Object> m = stockMapper.selectSummary(accountId, warehouseId);
        return m == null ? Map.of() : m;
    }

    /** 同步全部启用且开启自动同步的账号（定时任务），一个账号失败不影响其他 */
    public List<SyncResult> syncAll() {
        List<SyncResult> out = new ArrayList<>();
        for (EcomOwhAccount a : accountMapper.selectList(Wrappers.<EcomOwhAccount>lambdaQuery().eq(EcomOwhAccount::getStatus, EnableStatus.ENABLED)
                .eq(EcomOwhAccount::getAutoSync, 1))) {
            out.add(syncQuietly(a.getId()));
        }
        return out;
    }

    /** 同步一个账号，失败时返回原因（不抛出） */
    public SyncResult syncQuietly(Long accountId) {
        EcomOwhAccount a = accountService.get(accountId);
        try {
            return new SyncResult(a.getName(), sync(accountId), null);
        } catch (BizException e) {
            accountService.recordError(accountId, e.getMessage());
            return new SyncResult(a.getName(), 0, e.getMessage());
        }
    }

    /**
     * 同步一个账号下全部启用映射的库存
     *
     * @return 同步到的 SKU 行数
     */
    public int sync(Long accountId) {
        EcomOwhAccount a = accountService.get(accountId);
        OwhConnector c = support.connector(a.getProvider());
        OwhContext ctx = support.context(a);
        int n = 0;
        for (EcomOwhWarehouse m : accountService.mappings(accountId)) {
            if (m.getStatus() == EnableStatus.DISABLED) {
                continue;
            }
            List<OwhData.Stock> rows = c.inventory(ctx, m.getOwhCode());
            LocalDateTime now = LocalDateTime.now();
            transactionTemplate.executeWithoutResult(s -> write(m, rows, now));
            n += rows.size();
        }
        LocalDateTime now = LocalDateTime.now();
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getStockSyncTime, now).set(EcomOwhAccount::getLastError, null)
                .eq(EcomOwhAccount::getId, accountId));
        return n;
    }

    /** 写入一个仓库的库存快照（服务商推送、补录使用），返回行数 */
    @Transactional
    public int saveSnapshot(EcomOwhWarehouse m, List<OwhData.Stock> rows) {
        write(m, rows, LocalDateTime.now());
        return rows.size();
    }

    /** 系统仓库中各物料的 ERP 账面库存：[{ materialId, code, qty }] */
    public List<Map<String, Object>> erpStock(Long warehouseId) {
        return stockMapper.selectErpStock(warehouseId);
    }

    /** 写入一个仓库的库存快照：新 SKU 插入、已有的更新、服务商已没有的删除；同一 SKU 多行时合并 */
    void write(EcomOwhWarehouse m, List<OwhData.Stock> rows, LocalDateTime now) {
        Map<String, OwhData.Stock> bySku = new LinkedHashMap<>();
        for (OwhData.Stock r : rows) {
            if (!StringUtils.hasText(r.sku())) {
                continue;
            }
            bySku.merge(r.sku().trim(), r, (x, y) -> new OwhData.Stock(x.sku(), Decimals.nz(x.available()).add(Decimals.nz(y.available())),
                    Decimals.nz(x.transit()).add(Decimals.nz(y.transit())), Decimals.nz(x.locked()).add(Decimals.nz(y.locked()))));
        }
        Map<String, Long> materials = skuService.materials(m.getAccountId(), bySku.keySet());
        Map<String, EcomOwhStock> existing = stockMapper.selectList(Wrappers.<EcomOwhStock>lambdaQuery().eq(EcomOwhStock::getMappingId, m.getId()))
                .stream().collect(Collectors.toMap(EcomOwhStock::getSku, x -> x, (x, y) -> x));
        List<EcomOwhStock> inserts = new ArrayList<>();
        for (Map.Entry<String, OwhData.Stock> e : bySku.entrySet()) {
            OwhData.Stock r = e.getValue();
            EcomOwhStock old = existing.remove(e.getKey());
            Long materialId = materials.get(e.getKey());
            if (old == null) {
                EcomOwhStock n = new EcomOwhStock();
                n.setMappingId(m.getId());
                n.setWarehouseId(m.getWarehouseId());
                n.setSku(e.getKey());
                n.setMaterialId(materialId);
                n.setAvailableQty(Decimals.qty(Decimals.nz(r.available())));
                n.setTransitQty(Decimals.qty(Decimals.nz(r.transit())));
                n.setLockedQty(Decimals.qty(Decimals.nz(r.locked())));
                n.setSyncTime(now);
                inserts.add(n);
            } else {
                stockMapper.update(Wrappers.<EcomOwhStock>lambdaUpdate().set(EcomOwhStock::getMaterialId, materialId)
                        .set(EcomOwhStock::getWarehouseId, m.getWarehouseId())
                        .set(EcomOwhStock::getAvailableQty, Decimals.qty(Decimals.nz(r.available())))
                        .set(EcomOwhStock::getTransitQty, Decimals.qty(Decimals.nz(r.transit())))
                        .set(EcomOwhStock::getLockedQty, Decimals.qty(Decimals.nz(r.locked()))).set(EcomOwhStock::getSyncTime, now)
                        .eq(EcomOwhStock::getId, old.getId()));
            }
        }
        if (!inserts.isEmpty()) {
            stockMapper.insertBatch(inserts);
        }
        if (!existing.isEmpty()) {
            stockMapper.deleteByIds(existing.values().stream().map(EcomOwhStock::getId).toList());
        }
    }

    /**
     * 按海外仓数量调整 ERP 账面库存：选中行的差异（海外仓可用 + 锁定 − ERP 账面）按仓库汇总，盘盈生成一张其他入库、盘亏生成一张其他出库
     * （已确认，来源单号 OWH-时间）；没有匹配物料、没有差异的行跳过
     */
    @Transactional
    public AdjustResult adjust(AdjustRequest req) {
        List<EcomOwhStock> rows = stockMapper.selectWithErp(req.ids());
        LocalDate date = req.bizDate() == null ? LocalDate.now() : req.bizDate();
        String sourceNo = "OWH-" + LocalDateTime.now().format(SOURCE_TS);
        Map<Long, List<StockDocItemRequest>> ins = new LinkedHashMap<>();
        Map<Long, List<StockDocItemRequest>> outs = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        int lines = 0;
        for (EcomOwhStock r : rows) {
            if (r.getMaterialId() == null) {
                continue;
            }
            BigDecimal diff = Decimals.nz(r.getDiffQty());
            if (diff.signum() == 0) {
                continue;
            }
            names.put(r.getWarehouseId(), r.getAccountName() + " " + r.getOwhCode());
            String remark = Messages.get("ecom.text.owh.adjustLine", r.getSku(), Decimals.qty(r.getAvailableQty().add(r.getLockedQty())).stripTrailingZeros().toPlainString(),
                    Decimals.qty(Decimals.nz(r.getErpQty())).stripTrailingZeros().toPlainString());
            StockDocItemRequest item = new StockDocItemRequest(r.getMaterialId(), null, null, null, null, null, Decimals.qty(diff.abs()), null,
                    remark.length() > 255 ? remark.substring(0, 255) : remark);
            (diff.signum() > 0 ? ins : outs).computeIfAbsent(r.getWarehouseId(), k -> new ArrayList<>()).add(item);
            lines++;
        }
        if (lines == 0) {
            throw new BizException("ecom.error.owh.noDiff");
        }
        List<String> inNos = new ArrayList<>();
        List<String> outNos = new ArrayList<>();
        for (Map.Entry<Long, List<StockDocItemRequest>> e : ins.entrySet()) {
            String remark = Messages.get("ecom.text.owh.adjustInRemark", names.get(e.getKey()));
            WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.IN, new StockDocRequest(StockInType.OTHER.name(), e.getKey(), null, date,
                    remark, e.getValue(), null, "OTHER"), sourceNo);
            inNos.add(doc.getDocNo());
        }
        for (Map.Entry<Long, List<StockDocItemRequest>> e : outs.entrySet()) {
            String remark = Messages.get("ecom.text.owh.adjustOutRemark", names.get(e.getKey()));
            WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.OTHER.name(), e.getKey(), null, date,
                    remark, e.getValue(), null, "OTHER"), sourceNo);
            outNos.add(doc.getDocNo());
        }
        return new AdjustResult(inNos.isEmpty() ? null : String.join(",", inNos), outNos.isEmpty() ? null : String.join(",", outNos), lines);
    }
}
