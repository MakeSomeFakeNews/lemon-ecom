package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.dto.PurchasePlanFromReplenish;
import com.lemon.erp.modules.ecom.dto.PurchasePlanRequest;
import com.lemon.erp.modules.ecom.dto.PurchaseSuggestRow;
import com.lemon.erp.modules.ecom.dto.ReplenishWeights;
import com.lemon.erp.modules.ecom.service.EcomPurchasePlanService;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 生成采购计划草稿（经采购计划服务）：物料在本地仓采购建议中时按补货建议生成（带出供应商、单价、到货日期、需求构成），
 * 数量为空取建议采购量；不在建议中时按填写的数量手工新建草稿（没有填写数量时失败）。草稿需要人工审核后再生成采购订单
 */
@Component
@RequiredArgsConstructor
public class PurchasePlanActionHandler implements AutoActionHandler {

    private final EcomPurchasePlanService planService;
    private final EcomReplenishService replenishService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;

    @Override
    public AutoActionType type() {
        return AutoActionType.PURCHASE_PLAN;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        if (a.qty() != null && a.qty().signum() <= 0) {
            throw new BizException("ecom.error.auto.qtyInvalid");
        }
        if (a.warehouseId() != null) {
            warehouseService.get(a.warehouseId());
        }
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        Long materialId = e.ref(AutoRef.MATERIAL);
        if (materialId == null) {
            return Messages.get("ecom.text.auto.desc.planNoMaterial");
        }
        BigDecimal qty = a.qty();
        if (qty == null) {
            PurchaseSuggestRow s = replenishService.purchaseRowsOf(List.of(materialId), ReplenishWeights.DEFAULT).get(materialId);
            qty = s == null ? null : s.getSuggestQty();
        }
        return Messages.get("ecom.text.auto.desc.plan", materialService.get(materialId).getCode(), qty == null ? "-" : qty.stripTrailingZeros().toPlainString());
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        Long materialId = AutoActionSupport.require(e, AutoRef.MATERIAL);
        String remark = AutoText.cut(AutoText.t("ecom.text.auto.planRemark", rule.getName(), e.objectLabel() == null ? "" : e.objectLabel()), 255);
        Map<Long, PurchaseSuggestRow> suggestions = replenishService.purchaseRowsOf(List.of(materialId), ReplenishWeights.DEFAULT);
        PurchaseSuggestRow s = suggestions.get(materialId);
        Long planId;
        if (s != null && (a.qty() != null || s.getSuggestQty() != null && s.getSuggestQty().signum() > 0)) {
            planId = planService.createFromReplenish(new PurchasePlanFromReplenish(a.warehouseId(), remark, null, null, null, null,
                    List.of(new PurchasePlanFromReplenish.Item(materialId, a.qty()))));
        } else if (a.qty() != null) {
            planId = planService.create(new PurchasePlanRequest(LocalDate.now(), a.warehouseId(), remark,
                    List.of(new PurchasePlanRequest.Item(materialId, a.qty(), null, null, null, null, null, null, null))));
        } else {
            throw new BizException("ecom.error.auto.noSuggestQty", materialService.get(materialId).getCode());
        }
        return AutoOutcome.success(AutoText.t("ecom.text.auto.out.planCreated", planService.detail(planId).getDocNo()));
    }
}
