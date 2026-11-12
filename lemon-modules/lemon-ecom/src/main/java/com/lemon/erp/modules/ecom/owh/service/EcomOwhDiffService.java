package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.DiffHandleRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhDiff;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhDiffMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffStatus;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 收货差异：入库单、退件单、调拨单的实收与单据数量不一致时逐物料记录。
 * <p>
 * ERP 的在途调拨只能整单收货（保证在途存货金额与凭证一致），所以入库单 / 调拨单按「整单收货 + 同时把短少数量差异出库（其他出库，收发类别其他）、
 * 溢收数量其他入库」实现，收货仓的库存始终等于实收；退件单按实收生成销售退货（见 {@link EcomOwhReturnService}），不需要差异出入库。
 * 差异处理：短少的「确认损耗」（保留差异出库；调拨为调出方损耗，入库关联头程货件）或「找回补收」（补收数量其他入库到收货仓），溢收的「确认溢收」
 */
@Service
@RequiredArgsConstructor
public class EcomOwhDiffService {

    private final EcomOwhDiffMapper diffMapper;
    private final WmsStockDocService stockDocService;

    /**
     * 一个物料的单据与实收数量
     *
     * @param sku 服务商 SKU（调入本地仓的调拨为物料编码，可空）
     */
    public record Line(Long materialId, String sku, BigDecimal expected, BigDecimal received) {
    }

    /** 差异记录的来源信息 */
    public record Source(OwhDiffSource type, Long id, String no, String refNo, Long ecomInboundId, Long warehouseId) {
    }

    public PageResult<EcomOwhDiff> page(long current, long size, OwhDiffSource sourceType, OwhDiffStatus status, Long warehouseId, String keyword) {
        Page<EcomOwhDiff> page = Page.of(current, size);
        diffMapper.selectPageList(page, sourceType, status, warehouseId, StringUtils.hasText(keyword) ? keyword.trim() : null);
        return PageResult.of(page);
    }

    public List<EcomOwhDiff> bySource(OwhDiffSource type, Long sourceId) {
        return diffMapper.selectBySource(type, sourceId);
    }

    /**
     * 记录差异（内部，调用方的事务中执行），返回有差异的行数
     *
     * @param adjustStock 是否在收货仓做差异出库（短少）/ 溢收入库（整单收货的入库单、调拨单为 true）
     */
    public int record(Source src, List<Line> lines, LocalDate date, boolean adjustStock) {
        List<EcomOwhDiff> diffs = new ArrayList<>();
        List<StockDocItemRequest> outs = new ArrayList<>();
        List<StockDocItemRequest> ins = new ArrayList<>();
        for (Line l : lines) {
            BigDecimal diff = Decimals.qty(Decimals.nz(l.expected()).subtract(Decimals.nz(l.received())));
            if (diff.signum() == 0) {
                continue;
            }
            EcomOwhDiff d = new EcomOwhDiff();
            d.setSourceType(src.type());
            d.setSourceId(src.id());
            d.setSourceNo(src.no());
            d.setRefNo(src.refNo());
            d.setEcomInboundId(src.ecomInboundId());
            d.setWarehouseId(src.warehouseId());
            d.setMaterialId(l.materialId());
            d.setSku(l.sku());
            d.setExpectedQty(Decimals.qty(Decimals.nz(l.expected())));
            d.setReceivedQty(Decimals.qty(Decimals.nz(l.received())));
            d.setDiffQty(diff);
            d.setStatus(OwhDiffStatus.PENDING);
            diffs.add(d);
            (diff.signum() > 0 ? outs : ins).add(new StockDocItemRequest(l.materialId(), null, null, null, null, null, diff.abs(), null, null));
        }
        if (diffs.isEmpty()) {
            return 0;
        }
        String outNo = null;
        String inNo = null;
        if (adjustStock && !outs.isEmpty()) {
            WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.OTHER.name(), src.warehouseId(), null, date,
                    Messages.get("ecom.text.owh.diffOut", src.no()), outs, null, "OTHER"), src.no());
            outNo = doc.getDocNo();
        }
        if (adjustStock && !ins.isEmpty()) {
            WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.IN, new StockDocRequest(StockInType.OTHER.name(), src.warehouseId(), null, date,
                    Messages.get("ecom.text.owh.diffIn", src.no()), ins, null, "OTHER"), src.no());
            inNo = doc.getDocNo();
        }
        for (EcomOwhDiff d : diffs) {
            d.setDiffDocNo(d.getDiffQty().signum() > 0 ? outNo : inNo);
            diffMapper.insert(d);
        }
        return diffs.size();
    }

    /** 给来源单据待处理的差异写入原因（收货时填写） */
    public void noteReason(OwhDiffSource type, Long sourceId, String reason) {
        diffMapper.update(Wrappers.<EcomOwhDiff>lambdaUpdate().set(EcomOwhDiff::getReason, reason).eq(EcomOwhDiff::getSourceType, type)
                .eq(EcomOwhDiff::getSourceId, sourceId).eq(EcomOwhDiff::getStatus, OwhDiffStatus.PENDING));
    }

    /** 处理差异：确认损耗（短少，需填写原因）、找回补收（短少，补收入库）、确认溢收（溢收） */
    @Transactional
    public void handle(Long id, DiffHandleRequest req) {
        EcomOwhDiff d = diffMapper.selectById(id);
        if (d == null) {
            throw new BizException("ecom.error.owh.diffNotFound");
        }
        if (d.getStatus() != OwhDiffStatus.PENDING) {
            throw new BizException("ecom.error.owh.diffHandled");
        }
        boolean shortage = d.getDiffQty().signum() > 0;
        String reason = StringUtils.hasText(req.reason()) ? req.reason().trim() : d.getReason();
        String docNo = null;
        switch (req.action()) {
            case LOSS -> {
                if (!shortage) {
                    throw new BizException("ecom.error.owh.diffActionNotAllow");
                }
                if (reason == null) {
                    throw new BizException("ecom.error.owh.diffReasonRequired");
                }
            }
            case FOUND -> {
                if (!shortage) {
                    throw new BizException("ecom.error.owh.diffActionNotAllow");
                }
                WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.IN, new StockDocRequest(StockInType.OTHER.name(), d.getWarehouseId(), null,
                        LocalDate.now(), Messages.get("ecom.text.owh.diffFound", d.getSourceNo()),
                        List.of(new StockDocItemRequest(d.getMaterialId(), null, null, null, null, null, d.getDiffQty(), null, null)), null, "OTHER"), d.getSourceNo());
                docNo = doc.getDocNo();
            }
            case ACCEPTED -> {
                if (shortage) {
                    throw new BizException("ecom.error.owh.diffActionNotAllow");
                }
            }
            default -> throw new BizException("ecom.error.owh.diffActionNotAllow");
        }
        diffMapper.update(Wrappers.<EcomOwhDiff>lambdaUpdate().set(EcomOwhDiff::getStatus, req.action()).set(EcomOwhDiff::getReason, reason)
                .set(EcomOwhDiff::getHandleDocNo, docNo).set(EcomOwhDiff::getHandleTime, LocalDateTime.now())
                .set(EcomOwhDiff::getHandleBy, SecurityUtils.getUserIdOrNull()).eq(EcomOwhDiff::getId, id));
    }
}
