package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.ScmConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 供应链的业务记录类型（附件、变更历史）：采购订单、销售订单（合同扫描件等），客户、供应商（资质、协议）
 */
@Component
@RequiredArgsConstructor
public class ScmRecordTypes implements RecordTypeProvider {

    private final ScmOrderMapper orderMapper;
    private final ScmCustomerMapper customerMapper;
    private final ScmSupplierMapper supplierMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(
                new RecordType("scm_purchase", "scm.label.purchaseOrder", "scm:purchase:list", "scm:purchase:edit",
                        ScmConstants.ORDER_RULE, id -> order(id, OrderKind.PURCHASE)),
                new RecordType("scm_sales", "scm.label.salesOrder", "scm:sales:list", "scm:sales:edit",
                        ScmConstants.ORDER_RULE, id -> order(id, OrderKind.SALES)),
                new RecordType("scm_customer", "scm.label.customer", "scm:customer:list", "scm:customer:edit", null, customerMapper::selectById),
                new RecordType("scm_supplier", "scm.label.supplier", "scm:supplier:list", "scm:supplier:edit", null, supplierMapper::selectById));
    }

    private ScmOrder order(long id, OrderKind kind) {
        ScmOrder o = orderMapper.selectById(id);
        return o != null && o.getKind() == kind ? o : null;
    }
}
