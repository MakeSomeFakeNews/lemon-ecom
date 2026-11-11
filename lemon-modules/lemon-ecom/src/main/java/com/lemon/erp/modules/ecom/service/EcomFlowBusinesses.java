package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.number;
import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.text;

/**
 * 跨境电商模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与 Service 中 approvalSupport.submit 传入的变量一致
 */
@Configuration
public class EcomFlowBusinesses {

    @Bean
    public FlowBusiness ecomInboundFlow() {
        return FlowBusiness.of("ecom_inbound", "ecom.label.inboundApproval",
                number("amount", "ecom.label.inboundCost"),
                number("totalQty", "ecom.label.totalQty"),
                text("storeName", "ecom.label.store"));
    }
}
