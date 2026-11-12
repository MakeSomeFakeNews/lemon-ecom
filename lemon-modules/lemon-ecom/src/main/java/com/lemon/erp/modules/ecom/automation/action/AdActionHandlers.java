package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoAdTarget;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.service.EcomAdChangeService;
import com.lemon.erp.modules.ecom.service.EcomAdStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * 广告动作（经广告修改服务 {@link EcomAdChangeService}：店铺有广告接口、对象有平台 ID 时先调用接口再改本地，
 * 没有接口（如演示店铺）时只改本地并写广告执行日志）：暂停活动；调整活动下全部启用广告组的默认竞价或活动日预算
 */
@Configuration
@RequiredArgsConstructor
public class AdActionHandlers {

    private final EcomAdChangeService changeService;
    private final EcomAdStructureService structureService;

    @Bean
    AutoActionHandler adPauseAction() {
        return new AutoActionHandler() {
            @Override
            public AutoActionType type() {
                return AutoActionType.AD_PAUSE;
            }

            @Override
            public void validate(AutoRuleParts.Action a) {
                // 没有配置项
            }

            @Override
            public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                return Messages.get("ecom.text.auto.desc.adPause", e.objectLabel());
            }

            @Override
            public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                EcomAdCampaign c = structureService.campaign(AutoActionSupport.require(e, AutoRef.CAMPAIGN));
                if (c.getStatus() != EcomAdStatus.ENABLED) {
                    return AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.adNotEnabled", c.getName()));
                }
                boolean pushed = changeService.changeStatus(EcomAdLevel.CAMPAIGN, c.getId(), EcomAdStatus.PAUSED);
                return AutoOutcome.success(AutoText.t(pushed ? "ecom.text.auto.out.adPaused" : "ecom.text.auto.out.adPausedLocal", c.getName()));
            }
        };
    }

    @Bean
    AutoActionHandler adAdjustAction() {
        return new AutoActionHandler() {
            @Override
            public AutoActionType type() {
                return AutoActionType.AD_ADJUST;
            }

            @Override
            public void validate(AutoRuleParts.Action a) {
                if (a.adTarget() == null || a.value() == null || a.adjustMode() == null || a.adjustMode() == PriceAdjustMode.FORMULA) {
                    throw new BizException("ecom.error.auto.adjustRequired");
                }
                if (a.minValue() != null && a.maxValue() != null && a.minValue().compareTo(a.maxValue()) > 0) {
                    throw new BizException("ecom.error.auto.rangeInvalid");
                }
            }

            @Override
            public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                Long id = e.ref(AutoRef.CAMPAIGN);
                String target = Messages.get(AutoText.key("ecom.enum.auto.adTarget", a.adTarget()));
                String adjust = Messages.get(AutoText.key("ecom.enum.auto.adjust", a.adjustMode()), a.value().stripTrailingZeros().toPlainString());
                if (id == null) {
                    return Messages.get("ecom.text.auto.desc.adAdjust", e.objectLabel(), target, adjust, "-");
                }
                EcomAdCampaign c = structureService.campaign(id);
                String preview = a.adTarget() == AutoAdTarget.BUDGET ? fmt(c.getDailyBudget()) + " → " + fmt(adjust(a, c.getDailyBudget()))
                        : Messages.get("ecom.text.auto.groupCount", activeGroups(id).size());
                return Messages.get("ecom.text.auto.desc.adAdjust", c.getName(), target, adjust, preview);
            }

            @Override
            public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
                EcomAdCampaign c = structureService.campaign(AutoActionSupport.require(e, AutoRef.CAMPAIGN));
                if (a.adTarget() == AutoAdTarget.BUDGET) {
                    if (c.getDailyBudget() == null && a.adjustMode() != PriceAdjustMode.FIXED) {
                        throw new BizException("ecom.error.auto.budgetMissing", c.getName());
                    }
                    BigDecimal v = adjust(a, c.getDailyBudget());
                    boolean pushed = changeService.changeBudget(c.getId(), v);
                    return AutoOutcome.success(AutoText.t(pushed ? "ecom.text.auto.out.budget" : "ecom.text.auto.out.budgetLocal", c.getName(),
                            fmt(c.getDailyBudget()), fmt(v)));
                }
                List<EcomAdGroup> groups = activeGroups(c.getId());
                if (groups.isEmpty()) {
                    return AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.noGroups", c.getName()));
                }
                int changed = 0;
                for (EcomAdGroup g : groups) {
                    if (g.getDefaultBid() == null && a.adjustMode() != PriceAdjustMode.FIXED) {
                        continue;
                    }
                    changeService.changeBid(EcomAdLevel.AD_GROUP, g.getId(), adjust(a, g.getDefaultBid()));
                    changed++;
                }
                return AutoOutcome.success(AutoText.t("ecom.text.auto.out.bid", c.getName(), changed));
            }
        };
    }

    private List<EcomAdGroup> activeGroups(Long campaignId) {
        return structureService.groups(campaignId).stream().filter(g -> g.getStatus() == EcomAdStatus.ENABLED).toList();
    }

    /** 调整后的值：百分比 / 加减金额 / 固定值，限制在上下限之间，保留 2 位且大于 0 */
    static BigDecimal adjust(AutoRuleParts.Action a, BigDecimal old) {
        BigDecimal v = switch (a.adjustMode()) {
            case FIXED -> a.value();
            case PERCENT -> old.multiply(BigDecimal.ONE.add(a.value().divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)), MathContext.DECIMAL64);
            default -> old.add(a.value());
        };
        if (a.minValue() != null && v.compareTo(a.minValue()) < 0) {
            v = a.minValue();
        }
        if (a.maxValue() != null && v.compareTo(a.maxValue()) > 0) {
            v = a.maxValue();
        }
        v = v.setScale(2, RoundingMode.HALF_UP);
        if (v.signum() <= 0) {
            throw new BizException("ecom.error.auto.valueNotPositive");
        }
        return v;
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
