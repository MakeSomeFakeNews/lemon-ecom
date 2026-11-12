package com.lemon.erp.modules.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.workflow.engine.bpmn.FlowDesignValidator;
import com.lemon.erp.modules.workflow.entity.WfModel;
import com.lemon.erp.modules.workflow.mapper.WfModelMapper;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/**
 * 内置的行政流程（出差、用印、物品领用、通用审批）初始化后即发布，可以直接在「发起申请」中发起：
 * 流程模型由 init.sql 写入（发布需要引擎部署，SQL 中无法完成），应用启动后为平台租户发布从未发布过的这几个模型；
 * 新租户复制流程模型时由 {@link WfTenantInitializer} 调用 {@link #publish()} 一并发布。
 * 业务单据的内置流程不在此列：保持未发布，发布后对应单据才改为提交审批
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfBuiltinPublisher {

    /** 初始化即发布的内置流程 */
    public static final Set<String> PUBLISHED_KEYS = Set.of("oa_travel", "oa_seal", "oa_goods", "oa_general");

    private final WfModelMapper modelMapper;
    private final WfModelService modelService;
    private final FlowDesignValidator designValidator;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            TenantContext.runAs(TenantContext.PLATFORM_TENANT_ID, this::publish);
        } catch (RuntimeException e) {
            log.warn("Failed to publish built-in workflow models", e);
        }
    }

    /**
     * 当前租户中从未发布过的内置行政流程逐个发布；设计不完整的（如审批角色在租户中不存在）跳过，由管理员调整后手动发布。
     * 先校验再发布：发布在事务中，校验失败的异常不能经过事务方法（否则会把调用方的事务标记为回滚）
     */
    public void publish() {
        List<WfModel> models = modelMapper.selectList(Wrappers.<WfModel>lambdaQuery()
                .in(WfModel::getModelKey, PUBLISHED_KEYS).isNull(WfModel::getDefinitionId).isNull(WfModel::getDeployTime));
        for (WfModel m : models) {
            try {
                designValidator.validate(m.getDesignJson() == null ? null : objectMapper.readValue(m.getDesignJson(), FlowDesign.class));
            } catch (RuntimeException e) {
                log.warn("Built-in workflow model {} is not published: {}", m.getModelKey(), e.getMessage());
                continue;
            }
            modelService.deploy(m.getId());
        }
    }
}
