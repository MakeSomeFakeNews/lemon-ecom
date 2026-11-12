package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.automation.trigger.AutoRows;
import com.lemon.erp.modules.ecom.dto.HoldRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrderTag;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.ecom.service.EcomOrderBatchService;
import com.lemon.erp.modules.ecom.service.EcomOrderOpsService;
import com.lemon.erp.modules.ecom.service.EcomOrderTagService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单动作：挂起（经审单作业的挂起，打单、发货时拦截）、打标签、指定发货仓 / 物流渠道（经订单批量指定），都只对事件关联的订单执行
 */
@Configuration
@RequiredArgsConstructor
public class OrderActionHandlers {

    private final EcomAutoScanMapper scanMapper;
    private final EcomOrderOpsService opsService;
    private final EcomOrderTagService tagService;
    private final EcomOrderBatchService batchService;
    private final WmsWarehouseService warehouseService;
    private final EcomLogisticsService logisticsService;

    @Bean
    AutoActionHandler orderHoldAction() {
        return new AutoActionHandler() {
            @Override
            public AutoActionType type() {
                return AutoActionType.ORDER_HOLD;
            }

            @Override
            public void validate(AutoRuleParts.Action a) {
                // 原因可为空（默认为「自动化规则：规则名称」）
            }

            @Override
            public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                return Messages.get("ecom.text.auto.desc.hold", e.objectLabel(), reason(a, e, rule));
            }

            @Override
            public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                Long id = AutoActionSupport.require(e, AutoRef.ORDER);
                Map<String, Object> o = scanMapper.selectOrder(id);
                if (o != null && EcomLocalStatus.ON_HOLD.name().equals(AutoRows.str(o, "localStatus"))) {
                    return AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.alreadyHeld"));
                }
                String reason = AutoText.cut(reason(a, e, rule), 255);
                opsService.hold(new HoldRequest(List.of(id), reason));
                return AutoOutcome.success(AutoText.t("ecom.text.auto.out.held", reason));
            }
        };
    }

    @Bean
    AutoActionHandler orderTagAction() {
        return new AutoActionHandler() {
            @Override
            public AutoActionType type() {
                return AutoActionType.ORDER_TAG;
            }

            @Override
            public void validate(AutoRuleParts.Action a) {
                if (a.tagIds() == null || a.tagIds().isEmpty()) {
                    throw new BizException("ecom.error.auto.tagRequired");
                }
                a.tagIds().forEach(tagService::get);
            }

            @Override
            public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                return Messages.get("ecom.text.auto.desc.tag", e.objectLabel(), tagNames(a));
            }

            @Override
            public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                Long id = AutoActionSupport.require(e, AutoRef.ORDER);
                int n = tagService.tag(List.of(id), a.tagIds());
                return n == 0 ? AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.tagExists"))
                        : AutoOutcome.success(AutoText.t("ecom.text.auto.out.tagged", tagNames(a)));
            }
        };
    }

    @Bean
    AutoActionHandler orderAssignAction() {
        return new AutoActionHandler() {
            @Override
            public AutoActionType type() {
                return AutoActionType.ORDER_ASSIGN;
            }

            @Override
            public void validate(AutoRuleParts.Action a) {
                if (a.warehouseId() == null && a.channelId() == null) {
                    throw new BizException("ecom.error.auto.assignRequired");
                }
                if (a.warehouseId() != null) {
                    warehouseService.get(a.warehouseId());
                }
                if (a.channelId() != null) {
                    logisticsService.get(a.channelId());
                }
            }

            @Override
            public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                return Messages.get("ecom.text.auto.desc.assign", e.objectLabel(), assignText(a));
            }

            @Override
            public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                Long id = AutoActionSupport.require(e, AutoRef.ORDER);
                Map<String, String> failed = batchService.assign(List.of(id), a.warehouseId(), a.channelId());
                if (!failed.isEmpty()) {
                    throw new BizException("ecom.error.auto.assignFailed", String.join("; ", failed.values()));
                }
                return AutoOutcome.success(AutoText.t("ecom.text.auto.out.assigned", assignText(a)));
            }
        };
    }

    private static String reason(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return StringUtils.hasText(a.reason()) ? AutoText.render(a.reason(), e, rule) : AutoText.t("ecom.text.auto.holdReasonDefault", rule.getName());
    }

    private String tagNames(AutoRuleParts.Action a) {
        return a.tagIds() == null ? "" : a.tagIds().stream().map(id -> {
            try {
                EcomOrderTag t = tagService.get(id);
                return t.getName();
            } catch (BizException ex) {
                return String.valueOf(id);
            }
        }).collect(Collectors.joining(", "));
    }

    private String assignText(AutoRuleParts.Action a) {
        StringBuilder sb = new StringBuilder();
        if (a.warehouseId() != null) {
            sb.append(Messages.get("ecom.text.auto.warehouseIs", warehouseService.get(a.warehouseId()).getName()));
        }
        if (a.channelId() != null) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append(Messages.get("ecom.text.auto.channelIs", logisticsService.get(a.channelId()).getName()));
        }
        return sb.toString();
    }
}
