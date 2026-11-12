package com.lemon.erp.modules.ecom.dev.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.number;
import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.text;

/**
 * 产品开发接入审批的流程及发起时传入的流程变量（与 {@link EcomDevProjectService#submit} 中 flowApi 发起时传入的一致）
 */
@Configuration
public class EcomDevFlowBusinesses {

    @Bean
    public FlowBusiness ecomDevProjectFlow() {
        return FlowBusiness.of(EcomDevProjectService.FLOW.processKey(), "ecom.label.dev.projectApproval",
                number("estMargin", "ecom.label.dev.estMargin"),
                number("estProfit", "ecom.label.dev.estProfit"),
                number("targetPrice", "ecom.label.dev.targetPrice"),
                number("estPurchasePrice", "ecom.label.dev.estPurchasePrice"),
                number("estMonthlySales", "ecom.label.dev.estMonthlySales"),
                number("sampleFee", "ecom.label.dev.sampleFee"),
                text("platform", "ecom.label.dev.platform"),
                text("projectName", "ecom.label.dev.projectName"));
    }
}
