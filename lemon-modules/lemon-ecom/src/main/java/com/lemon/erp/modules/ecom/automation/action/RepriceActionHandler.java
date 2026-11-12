package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.service.EcomPriceChangeService;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * 在线商品调价：按百分比 / 加减金额 / 固定值计算新价（限制在最低价、最高价之间，保留 2 位小数），
 * 经改价服务（{@link EcomPriceChangeService#applyPrices}）推送到平台并写改价历史（来源「自动化规则」）；
 * 演示店铺、平台不支持改价时只改本地。勾选「仅建议」时只在执行日志中记录建议价，不修改
 */
@Component
@RequiredArgsConstructor
public class RepriceActionHandler implements AutoActionHandler {

    private final EcomListingService listingService;
    private final EcomStoreService storeService;
    private final EcomPriceChangeService priceChangeService;

    @Override
    public AutoActionType type() {
        return AutoActionType.LISTING_REPRICE;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        if (a.adjustMode() == null || a.adjustMode() == PriceAdjustMode.FORMULA || a.value() == null) {
            throw new BizException("ecom.error.auto.adjustRequired");
        }
        if (a.adjustMode() == PriceAdjustMode.FIXED && a.value().signum() <= 0) {
            throw new BizException("ecom.error.auto.priceInvalid");
        }
        if (a.minValue() != null && a.maxValue() != null && a.minValue().compareTo(a.maxValue()) > 0) {
            throw new BizException("ecom.error.auto.rangeInvalid");
        }
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        Long id = e.ref(AutoRef.LISTING);
        if (id == null) {
            return Messages.get("ecom.text.auto.desc.repriceNoListing", adjustText(a));
        }
        EcomListing l = listingService.get(id);
        BigDecimal p = newPrice(a, l.getPrice());
        return Messages.get(Boolean.TRUE.equals(a.suggestOnly()) ? "ecom.text.auto.desc.repriceSuggest" : "ecom.text.auto.desc.reprice",
                l.getSellerSku(), fmt(l.getPrice()), fmt(p), l.getCurrency() == null ? "" : l.getCurrency());
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        EcomListing l = listingService.get(AutoActionSupport.require(e, AutoRef.LISTING));
        BigDecimal p = newPrice(a, l.getPrice());
        String currency = l.getCurrency() == null ? "" : l.getCurrency();
        if (l.getPrice() != null && p.compareTo(l.getPrice()) == 0) {
            return AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.priceSame", fmt(p), currency));
        }
        if (Boolean.TRUE.equals(a.suggestOnly())) {
            return AutoOutcome.suggested(AutoText.t("ecom.text.auto.out.priceSuggested", l.getSellerSku(), fmt(l.getPrice()), fmt(p), currency));
        }
        EcomStore store = storeService.get(l.getStoreId());
        List<PublishViews.ChangeRow> rows = priceChangeService.applyPrices(store, List.of(new EcomPriceChangeService.PriceTarget(l, p)),
                PriceChangeSource.AUTOMATION, null, EcomPriceChangeService.newBatchNo(), true);
        PublishViews.ChangeRow r = rows.get(0);
        if (r.status() == PriceChangeStatus.FAILED) {
            throw new BizException("ecom.error.auto.repriceFailed", r.message());
        }
        return AutoOutcome.success(AutoText.t(r.status() == PriceChangeStatus.SUCCESS ? "ecom.text.auto.out.repriced" : "ecom.text.auto.out.repricedLocal",
                l.getSellerSku(), fmt(l.getPrice()), fmt(p), currency));
    }

    /** 新价：按方式计算后限制在上下限之间，保留 2 位 */
    static BigDecimal newPrice(AutoRuleParts.Action a, BigDecimal old) {
        BigDecimal v;
        if (a.adjustMode() == PriceAdjustMode.FIXED) {
            v = a.value();
        } else {
            if (old == null || old.signum() <= 0) {
                throw new BizException("ecom.error.auto.priceMissing");
            }
            v = a.adjustMode() == PriceAdjustMode.PERCENT
                    ? old.multiply(BigDecimal.ONE.add(a.value().divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)), MathContext.DECIMAL64)
                    : old.add(a.value());
        }
        if (a.minValue() != null && v.compareTo(a.minValue()) < 0) {
            v = a.minValue();
        }
        if (a.maxValue() != null && v.compareTo(a.maxValue()) > 0) {
            v = a.maxValue();
        }
        v = v.setScale(2, RoundingMode.HALF_UP);
        if (v.signum() <= 0) {
            throw new BizException("ecom.error.auto.priceInvalid");
        }
        return v;
    }

    private static String adjustText(AutoRuleParts.Action a) {
        return a.adjustMode() == null ? "" : Messages.get(AutoText.key("ecom.enum.auto.adjust", a.adjustMode()), fmt(a.value()));
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "-" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
