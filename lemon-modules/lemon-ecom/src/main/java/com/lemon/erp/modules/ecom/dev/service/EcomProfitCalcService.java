package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.dev.dto.ProfitCalcRequest;
import com.lemon.erp.modules.ecom.dev.dto.ProfitCompareRequest;
import com.lemon.erp.modules.ecom.dev.dto.ProfitDefaults;
import com.lemon.erp.modules.ecom.dev.dto.ProfitResult;
import com.lemon.erp.modules.ecom.dev.dto.ProfitResult.CostLine;
import com.lemon.erp.modules.ecom.dev.dto.ProfitSchemeRequest;
import com.lemon.erp.modules.ecom.dev.entity.EcomCommissionRate;
import com.lemon.erp.modules.ecom.dev.entity.EcomProfitScheme;
import com.lemon.erp.modules.ecom.dev.mapper.EcomCommissionRateMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomProfitSchemeMapper;
import com.lemon.erp.modules.ecom.dev.model.DevFbaSizeTier;
import com.lemon.erp.modules.ecom.dev.model.DevHeadPricing;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.service.FbaFeeCalculator;
import com.lemon.erp.modules.ecom.fba.service.FbaRateService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.TaxType;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.ecom.service.EcomTaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

/**
 * 利润试算：解析默认参数（佣金费率、币种与当月汇率、VAT / GST、物流渠道费率、亚马逊 FBA 配送费——按「FBA 费率」维护的站点分段，
 * 没有维护时美国站用内置估算 {@link FbaFeeEstimator}）后交给 {@link ProfitCalculator}，
 * 正算单件利润、利润率、ROI、保本价，按目标利润率反算建议售价；多平台对比；试算方案；平台佣金费率维护
 */
@Service
@RequiredArgsConstructor
public class EcomProfitCalcService {

    /** 头程按实重计费的运输方式（业务字典 ecom_transport_mode 的海运） */
    static final String SEA_MODE = "SEA";
    /** 空运 / 快递的体积重除数（cm³ / kg） */
    static final double VOLUME_DIVISOR = 6000;
    static final String US = "US";
    static final String LB = "lb";

    private final EcomCommissionRateMapper rateMapper;
    private final EcomProfitSchemeMapper schemeMapper;
    private final EcomLogisticsService logisticsService;
    private final EcomTaxService taxService;
    private final ExchangeRateProvider exchangeRateProvider;
    private final EcomDevProjectService projectService;
    private final FbaRateService fbaRateService;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------- 默认参数

    /** 平台 / 站点 / 类目的默认佣金、币种、汇率、税率 */
    public ProfitDefaults defaults(EcomPlatform platform, String marketplace, String category) {
        EcomCommissionRate rate = matchRate(platform, marketplace, category);
        String currency = rate != null && StringUtils.hasText(rate.getCurrency()) ? rate.getCurrency() : DevMarketplaces.currency(marketplace);
        BigDecimal fx = currency == null ? null : exchangeRateProvider.rate(currency, LocalDate.now());
        TaxSetting tax = taxOf(rate, marketplace);
        return new ProfitDefaults(rate == null ? null : rate.getId(), rate == null ? null : rate.getRate(), rate == null ? null : rate.getMinFee(),
                rate == null ? null : rate.getFixedFee(), currency, fx, tax.type(), tax.rate(), DevMarketplaces.country(marketplace));
    }

    /** 启用的佣金费率中最匹配的：站点 + 类目 → 站点 → 全部站点 + 类目 → 全部站点 */
    EcomCommissionRate matchRate(EcomPlatform platform, String marketplace, String category) {
        if (platform == null) {
            return null;
        }
        String mkt = marketplace == null ? "" : marketplace.trim();
        String cat = category == null ? "" : category.trim();
        List<EcomCommissionRate> rates = rateMapper.selectList(Wrappers.<EcomCommissionRate>lambdaQuery()
                .eq(EcomCommissionRate::getPlatform, platform).eq(EcomCommissionRate::getStatus, EnableStatus.ENABLED));
        return rates.stream()
                .filter(r -> r.getMarketplace().isEmpty() || r.getMarketplace().equalsIgnoreCase(mkt))
                .filter(r -> r.getCategory().isEmpty() || r.getCategory().equalsIgnoreCase(cat))
                .max(Comparator.comparingInt(r -> (r.getMarketplace().isEmpty() ? 0 : 2) + (r.getCategory().isEmpty() ? 0 : 1)))
                .orElse(null);
    }

    private record TaxSetting(TaxType type, BigDecimal rate) {
    }

    /** 佣金费率上的默认 VAT（大于 0）→ 税务中按站点国家匹配的启用税率 → 不计税 */
    private TaxSetting taxOf(EcomCommissionRate rate, String marketplace) {
        if (rate != null && Decimals.positive(rate.getVatRate())) {
            return new TaxSetting(TaxType.VAT, rate.getVatRate());
        }
        EcomTaxRate t = taxService.resolve(marketplace, DevMarketplaces.country(marketplace));
        if (t != null) {
            return new TaxSetting(t.getTaxType(), t.getRate());
        }
        return new TaxSetting(null, BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------- 试算

    public ProfitResult calc(ProfitCalcRequest req) {
        List<String> warnings = new ArrayList<>();
        EcomCommissionRate rate = matchRate(req.platform(), req.marketplace(), req.category());
        String currency = firstText(req.currency(), rate == null ? null : rate.getCurrency(), DevMarketplaces.currency(req.marketplace()));
        if (currency == null) {
            throw new BizException("ecom.error.dev.currencyUnknown", req.marketplace() == null ? "" : req.marketplace());
        }
        currency = currency.toUpperCase(Locale.ROOT);
        BigDecimal fx = req.exchangeRate() != null ? req.exchangeRate() : exchangeRateProvider.rate(currency, LocalDate.now());
        if (fx == null) {
            throw new BizException("ecom.error.dev.exchangeRateMissing", currency);
        }
        BigDecimal commissionRate = req.commissionRate() != null ? req.commissionRate() : rate == null ? BigDecimal.ZERO : rate.getRate();
        if (req.commissionRate() == null && rate == null) {
            warnings.add(Messages.get("ecom.text.dev.noCommissionRate"));
        }
        BigDecimal minFee = req.commissionRate() != null ? req.minCommission() : rate == null ? null : rate.getMinFee();
        BigDecimal fixedFee = req.fixedFee() != null ? req.fixedFee() : rate == null ? null : rate.getFixedFee();
        TaxType taxType;
        BigDecimal taxRate;
        if (req.taxRate() != null) {
            taxType = req.taxType() == null ? TaxType.VAT : req.taxType();
            taxRate = req.taxRate();
        } else {
            TaxSetting t = taxOf(rate, req.marketplace());
            taxType = req.taxType() != null ? req.taxType() : t.type();
            taxRate = t.rate();
        }
        boolean inclusive = taxType != null && taxType.inclusive();

        // 头程：计费重、体积、单件头程
        double weight = d(req.weightKg());
        double volumeCm3 = d(req.lengthCm()) * d(req.widthCm()) * d(req.heightCm());
        double volumeCbm = volumeCm3 / 1_000_000;
        String mode = req.transportMode();
        Long channelId = req.headPricing() == DevHeadPricing.CHANNEL ? req.logisticsChannelId() : null;
        EcomLogisticsChannel channel = null;
        if (req.headPricing() == DevHeadPricing.CHANNEL) {
            if (channelId == null) {
                throw new BizException("ecom.error.dev.channelRequired");
            }
            channel = logisticsService.get(channelId);
            mode = channel.getTransportMode();
        }
        double chargeWeight = SEA_MODE.equals(mode) ? weight : Math.max(weight, volumeCm3 / VOLUME_DIVISOR);
        double head;
        if (req.headPricing() == DevHeadPricing.CHANNEL) {
            String country = DevMarketplaces.country(req.marketplace());
            BigDecimal fee = country == null ? null : logisticsService.estimate(channelId, country, BigDecimal.valueOf(round(chargeWeight, 3)));
            if (fee == null) {
                throw new BizException("ecom.error.dev.noLogisticsRate", channel.getName(), country == null ? "" : country);
            }
            head = fee.doubleValue();
        } else if (req.headPricing() == DevHeadPricing.PER_KG) {
            head = d(req.headUnitPrice()) * chargeWeight;
        } else if (req.headPricing() == DevHeadPricing.PER_CBM) {
            head = d(req.headUnitPrice()) * volumeCbm;
        } else {
            head = d(req.headFee());
        }

        // 配送费：亚马逊按「FBA 费率」中维护的站点分段计算，没有维护时美国站用内置估算
        DoubleUnaryOperator fulfillment = p -> d(req.fulfillmentFee());
        DevFbaSizeTier tier = null;
        String tierName = null;
        BigDecimal fbaWeight = null;
        String fbaUnit = null;
        if (Boolean.TRUE.equals(req.fbaAuto())) {
            if (req.platform() != EcomPlatform.AMAZON || !StringUtils.hasText(req.marketplace())) {
                warnings.add(Messages.get("ecom.text.dev.fbaAmazonOnly"));
            } else if (!dimsComplete(req)) {
                warnings.add(Messages.get("ecom.text.dev.fbaNeedSize"));
            } else {
                FbaViews.FeeQuote quote = fbaRateService.quote(req.marketplace().trim(),
                        new FbaFeeCalculator.Dims(req.lengthCm(), req.widthCm(), req.heightCm(), req.weightKg()));
                if (quote != null && quote.fee() != null) {
                    double fee = quote.fee().doubleValue();
                    fulfillment = p -> fee;
                    tierName = quote.tierName();
                    fbaWeight = quote.shippingWeight();
                    fbaUnit = quote.weightUnit();
                    if (StringUtils.hasText(quote.currency()) && !quote.currency().equalsIgnoreCase(currency)) {
                        warnings.add(Messages.get("ecom.text.dev.fbaCurrency", quote.currency(), currency));
                    }
                } else if (US.equalsIgnoreCase(req.marketplace().trim())) {
                    FbaFeeEstimator.Estimate probe = FbaFeeEstimator.estimate(od(req.lengthCm()), od(req.widthCm()), od(req.heightCm()), od(req.weightKg()), 0);
                    tier = probe.tier();
                    fbaWeight = BigDecimal.valueOf(probe.shippingWeightLb());
                    fbaUnit = LB;
                    fulfillment = p -> FbaFeeEstimator.estimate(od(req.lengthCm()), od(req.widthCm()), od(req.heightCm()), od(req.weightKg()), p).fee();
                } else {
                    warnings.add(Messages.get("ecom.text.dev.fbaNoRate", req.marketplace().trim()));
                }
            }
        }

        ProfitCalculator.Input in = new ProfitCalculator.Input(fx.doubleValue(), pct(commissionRate), d(minFee), d(fixedFee), pct(req.adRate()),
                pct(req.returnRate()), inclusive, pct(taxRate), d(req.otherSiteFee()), d(req.purchasePrice()), head,
                d(req.purchasePrice()) * pct(req.dutyRate()), d(req.otherCost()), fulfillment);
        Double suggest = req.targetMargin() == null ? null : ProfitCalculator.priceForMargin(in, req.targetMargin().doubleValue());
        if (req.targetMargin() != null && suggest == null) {
            warnings.add(Messages.get("ecom.text.dev.marginUnreachable", req.targetMargin().stripTrailingZeros().toPlainString()));
        }
        Double price = req.salePrice() != null ? Double.valueOf(req.salePrice().doubleValue()) : suggest;
        if (price == null) {
            throw new BizException("ecom.error.dev.priceRequired");
        }
        Double breakeven = ProfitCalculator.breakeven(in);
        ProfitCalculator.Breakdown b = ProfitCalculator.at(in, price);
        double rateFx = fx.doubleValue();
        List<CostLine> lines = new ArrayList<>();
        lines.add(new CostLine("purchase", null, money(in.purchase())));
        lines.add(new CostLine("head", null, money(head)));
        lines.add(new CostLine("duty", null, money(in.duty())));
        lines.add(new CostLine("other", null, money(in.other())));
        lines.add(new CostLine("commission", money(b.commission()), money(b.commission() * rateFx)));
        lines.add(new CostLine("fulfillment", money(b.fulfillment()), money(b.fulfillment() * rateFx)));
        lines.add(new CostLine("tax", money(b.tax()), money(b.tax() * rateFx)));
        lines.add(new CostLine("ad", money(b.ad()), money(b.ad() * rateFx)));
        lines.add(new CostLine("returns", money(b.returns()), money(b.returns() * rateFx)));
        lines.add(new CostLine("siteOther", money(b.siteOther()), money(b.siteOther() * rateFx)));
        if (b.profit() < 0) {
            warnings.add(Messages.get("ecom.text.dev.loss"));
        }
        return new ProfitResult(req.platform(), req.marketplace(), req.category(), currency, fx, money(price), money(b.revenue()), commissionRate,
                minFee, fixedFee, taxType, taxRate, tier, tierName, fbaWeight, fbaUnit, BigDecimal.valueOf(round(chargeWeight, 3)), BigDecimal.valueOf(round(volumeCbm, 4)), lines,
                money(b.siteCost() * rateFx + b.landed()), money(b.profit()), money(b.profit() / rateFx), money(b.margin()), money(b.roi()),
                breakeven == null ? null : money(breakeven), req.targetMargin(), suggest == null ? null : money(suggest), warnings);
    }

    /** 多平台对比：逐个平台试算，某个平台失败（如没有汇率）时该项只返回提示 */
    public List<ProfitResult> compare(ProfitCompareRequest req) {
        List<ProfitResult> out = new ArrayList<>();
        for (ProfitCompareRequest.Target t : req.targets()) {
            BigDecimal margin = t.targetMargin() != null ? t.targetMargin() : req.base().targetMargin();
            BigDecimal price = t.salePrice();
            if (price == null && margin == null) {
                margin = req.base().targetMargin();
            }
            ProfitCalcRequest r = req.base().forTarget(t.platform(), t.marketplace(), t.category(), price, margin,
                    t.fulfillmentFee() != null ? t.fulfillmentFee() : req.base().fulfillmentFee());
            try {
                out.add(calc(r));
            } catch (BizException e) {
                out.add(new ProfitResult(t.platform(), t.marketplace(), t.category(), null, null, price, null, null, null, null, null, null, null,
                        null, null, null, null, null, List.of(), null, null, null, null, null, null, margin, null, List.of(e.getMessage())));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 试算方案

    public PageResult<EcomProfitScheme> pageSchemes(long current, long size, Long projectId, String keyword) {
        Page<EcomProfitScheme> page = Page.of(current, size);
        schemeMapper.selectPageList(page, projectId, keyword);
        return PageResult.of(page);
    }

    public EcomProfitScheme scheme(Long id) {
        EcomProfitScheme s = id == null ? null : schemeMapper.selectById(id);
        if (s == null) {
            throw new BizException("ecom.error.dev.schemeNotFound");
        }
        return s;
    }

    /** 保存方案：重新试算后保存结果摘要；关联开发项目时回写项目的试算利润与利润率 */
    @Transactional
    public Long saveScheme(ProfitSchemeRequest req) {
        ProfitResult r = calc(req.params());
        Long projectId = req.params().projectId();
        if (projectId != null) {
            projectService.get(projectId);
        }
        EcomProfitScheme s = new EcomProfitScheme();
        s.setName(req.name().trim());
        s.setProjectId(projectId);
        s.setPlatform(r.platform());
        s.setMarketplace(r.marketplace());
        s.setCurrency(r.currency());
        s.setSalePrice(r.salePrice());
        s.setProfit(r.profit());
        s.setMargin(r.margin());
        s.setRoi(r.roi());
        try {
            s.setParams(objectMapper.writeValueAsString(req.params()));
        } catch (JacksonException e) {
            throw new BizException("ecom.error.dev.paramsInvalid");
        }
        schemeMapper.insert(s);
        if (projectId != null) {
            projectService.updateEstimate(projectId, r.profit(), r.margin());
        }
        return s.getId();
    }

    @Transactional
    public void deleteSchemes(List<Long> ids) {
        schemeMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 佣金费率

    public List<EcomCommissionRate> rates(EcomPlatform platform, String keyword, EnableStatus status) {
        return rateMapper.selectList(Wrappers.<EcomCommissionRate>lambdaQuery()
                .eq(platform != null, EcomCommissionRate::getPlatform, platform)
                .eq(status != null, EcomCommissionRate::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(EcomCommissionRate::getCategory, keyword).or()
                        .like(EcomCommissionRate::getMarketplace, keyword).or().like(EcomCommissionRate::getRemark, keyword))
                .orderByAsc(EcomCommissionRate::getPlatform).orderByAsc(EcomCommissionRate::getMarketplace)
                .orderByAsc(EcomCommissionRate::getCategory));
    }

    public Long createRate(EcomCommissionRate r) {
        normalize(r);
        checkUnique(r, null);
        r.setId(null);
        r.setBuiltin(0);
        if (r.getStatus() == null) {
            r.setStatus(EnableStatus.ENABLED);
        }
        rateMapper.insert(r);
        return r.getId();
    }

    public void updateRate(Long id, EcomCommissionRate r) {
        rate(id);
        normalize(r);
        checkUnique(r, id);
        r.setId(id);
        r.setBuiltin(null);
        rateMapper.updateById(r);
    }

    public void deleteRates(List<Long> ids) {
        rateMapper.deleteByIds(ids);
    }

    private EcomCommissionRate rate(Long id) {
        EcomCommissionRate r = id == null ? null : rateMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.dev.rateNotFound");
        }
        return r;
    }

    private static void normalize(EcomCommissionRate r) {
        r.setMarketplace(r.getMarketplace() == null ? "" : r.getMarketplace().trim().toUpperCase(Locale.ROOT));
        r.setCategory(r.getCategory() == null ? "" : r.getCategory().trim());
        r.setCurrency(StringUtils.hasText(r.getCurrency()) ? r.getCurrency().trim().toUpperCase(Locale.ROOT) : null);
    }

    private void checkUnique(EcomCommissionRate r, Long excludeId) {
        if (rateMapper.exists(Wrappers.<EcomCommissionRate>lambdaQuery().eq(EcomCommissionRate::getPlatform, r.getPlatform())
                .eq(EcomCommissionRate::getMarketplace, r.getMarketplace()).eq(EcomCommissionRate::getCategory, r.getCategory())
                .ne(excludeId != null, EcomCommissionRate::getId, excludeId))) {
            throw new BizException("ecom.error.dev.rateExists");
        }
    }

    // ---------------------------------------------------------------- 工具

    private static String firstText(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static boolean dimsComplete(ProfitCalcRequest r) {
        return Decimals.positive(r.lengthCm()) && Decimals.positive(r.widthCm()) && Decimals.positive(r.heightCm()) && Decimals.positive(r.weightKg());
    }

    private static double d(BigDecimal v) {
        return v == null ? 0 : v.doubleValue();
    }

    private static Double od(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static double pct(BigDecimal v) {
        return v == null ? 0 : v.doubleValue() / 100;
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(Decimals.MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
