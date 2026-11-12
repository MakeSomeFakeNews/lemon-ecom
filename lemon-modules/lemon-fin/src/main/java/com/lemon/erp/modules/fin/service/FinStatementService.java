package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.StatementRequests;
import com.lemon.erp.modules.fin.entity.FinSupplierStatement;
import com.lemon.erp.modules.fin.entity.FinSupplierStatementLine;
import com.lemon.erp.modules.fin.mapper.FinSupplierStatementMapper;
import com.lemon.erp.modules.fin.model.StatementStatus;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商对账：按期间汇总供应商还没有对过账的应付单（采购入库、退货、质量索赔等）生成对账单；
 * 发送（导出 / 打印给供应商）后由内部人员登记供应商的回复（确认，可同时登记发票；有异议及说明）。
 * 有异议的可以删除后重新生成，或核对后再次发送
 */
@Service
@RequiredArgsConstructor
public class FinStatementService {

    /** 对账单号的编码规则标识 */
    public static final String DOC_NO = "DZ";

    private final FinSupplierStatementMapper statementMapper;
    private final ScmSupplierService supplierService;
    private final DocNoService docNoService;

    /** @param supplierId 筛选条件 */
    public PageResult<FinSupplierStatement> page(long current, long size, Long supplierId, StatementStatus status, String keyword) {
        Page<FinSupplierStatement> page = Page.of(current, size);
        statementMapper.selectPageList(page, supplierId, status, keyword);
        return PageResult.of(page);
    }

    public FinSupplierStatement detail(Long id) {
        FinSupplierStatement st = id == null ? null : statementMapper.selectDetail(id);
        if (st == null) {
            throw new BizException("fin.error.statementNotFound");
        }
        st.setLines(statementMapper.selectLines(id));
        return st;
    }

    @Transactional
    public Long generate(StatementRequests.Generate req) {
        supplierService.get(req.supplierId());
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("fin.error.statementEndDateCannotEarlierStart");
        }
        List<FinSupplierStatementLine> lines = statementMapper.selectUnreconciledBills(req.supplierId(), req.beginDate(), req.endDate());
        if (lines.isEmpty()) {
            throw new BizException("fin.error.noPayablesReconcilePeriodAlreadyReconciled");
        }
        FinSupplierStatement st = new FinSupplierStatement();
        st.setStatementNo(docNoService.next(DOC_NO));
        st.setSupplierId(req.supplierId());
        st.setBeginDate(req.beginDate());
        st.setEndDate(req.endDate());
        st.setAmount(Decimals.sum(lines, FinSupplierStatementLine::getAmount));
        st.setBillCount(lines.size());
        st.setStatus(StatementStatus.DRAFT);
        st.setRemark(req.remark());
        statementMapper.insert(st);
        lines.forEach(l -> l.setStatementId(st.getId()));
        statementMapper.insertLines(lines);
        return st.getId();
    }

    /** 标记为已发送给供应商（草稿或有异议的） */
    @Transactional
    public void send(Long id) {
        FinSupplierStatement st = detail(id);
        if (st.getStatus() != StatementStatus.DRAFT && st.getStatus() != StatementStatus.DISPUTED) {
            throw new BizException("fin.error.statementHasAlreadySentConfirmed");
        }
        transit(id, st.getStatus(), StatementStatus.SENT);
    }

    /**
     * 登记供应商的对账结果（待确认的）：确认时可同时登记供应商开具的发票；有异议时须填写供应商的意见，
     * 之后可核对后再次发送，或删除后重新生成
     */
    @Transactional
    public void registerResult(Long id, StatementRequests.Result req) {
        FinSupplierStatement st = detail(id);
        if (st.getStatus() != StatementStatus.SENT) {
            throw new BizException("fin.error.statementNotPendingConfirmation");
        }
        if (req.status() != StatementStatus.CONFIRMED && req.status() != StatementStatus.DISPUTED) {
            throw new BizException("fin.valid.chooseConfirmDispute");
        }
        boolean confirmed = req.status() == StatementStatus.CONFIRMED;
        String remark = StringUtils.hasText(req.supplierRemark()) ? req.supplierRemark().trim() : null;
        if (!confirmed && remark == null) {
            throw new BizException("fin.error.describeDispute");
        }
        String invoiceNo = confirmed && StringUtils.hasText(req.invoiceNo()) ? req.invoiceNo().trim() : null;
        LocalDateTime time = req.confirmDate() == null || req.confirmDate().equals(LocalDate.now()) ? LocalDateTime.now()
                : req.confirmDate().atStartOfDay();
        StatusTransitions.change(statementMapper, FinSupplierStatement::getId, id, FinSupplierStatement::getStatus,
                StatementStatus.SENT, req.status(), w -> w
                        .set(FinSupplierStatement::getSupplierRemark, remark)
                        .set(FinSupplierStatement::getConfirmTime, time)
                        .set(FinSupplierStatement::getConfirmBy, SecurityUtils.getUserId())
                        .set(FinSupplierStatement::getInvoiceNo, invoiceNo)
                        .set(FinSupplierStatement::getInvoiceAmount, invoiceNo == null ? null : req.invoiceAmount())
                        .set(FinSupplierStatement::getInvoiceDate, invoiceNo == null ? null : req.invoiceDate()),
                "fin.error.statementStatusHasChangedRefreshTry");
    }

    /** 删除草稿或有异议的对账单，其中的应付单可以重新对账 */
    @Transactional
    public void delete(Long id) {
        FinSupplierStatement st = detail(id);
        if (st.getStatus() != StatementStatus.DRAFT && st.getStatus() != StatementStatus.DISPUTED) {
            throw new BizException("fin.error.statementsSentConfirmedCannotDeleted");
        }
        statementMapper.deleteLines(id);
        statementMapper.deleteById(id);
    }

    private void transit(Long id, StatementStatus from, StatementStatus to) {
        StatusTransitions.change(statementMapper, FinSupplierStatement::getId, id, FinSupplierStatement::getStatus, from, to,
                "fin.error.statementStatusHasChangedRefreshTry");
    }
}
