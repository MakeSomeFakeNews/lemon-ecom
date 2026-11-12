package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsRate;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.ecom.service.EcomTaxService;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.fin.service.FinCurrencyService;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.model.WarehouseType;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 基础设置（order 0）：近几个月的币种汇率、品牌与站点字典、仓库（国内发货仓、各站点 FBA 仓、美国海外仓、不良品仓）、
 * 头程货代与物流渠道（头程空派 / 海派、尾程专线 / 快递，含费率）
 */
@Component
@RequiredArgsConstructor
public class DemoSetupContributor implements DemoContributor {

    /** 汇率基准值（1 外币 = x 人民币），各月在此基础上小幅波动 */
    private static final Map<String, Double> RATES = Map.of("USD", 7.10, "EUR", 7.78, "JPY", 0.0472, "MYR", 1.61, "SGD", 5.38, "GBP", 9.10,
            "CAD", 5.20, "AUD", 4.70);

    private static final List<String> TAX_COUNTRIES = List.of("DE", "JP", "SG", "US");

    private final FinCurrencyService currencyService;
    private final EcomTaxService taxService;
    private final DictLookup dictLookup;
    private final WmsWarehouseService warehouseService;
    private final ScmSupplierService supplierService;
    private final EcomLogisticsService logisticsService;

    @Override
    public int order() {
        return 0;
    }

    @Override
    public String key() {
        return "setup";
    }

    @Override
    public void generate(DemoContext ctx) {
        rates(ctx);
        DemoCatalog.BRANDS.forEach((v, l) -> dictLookup.ensureBusinessValue(WmsMaterialService.BRAND_DICT, v, l));
        for (DemoCatalog.Store s : DemoCatalog.STORES) {
            dictLookup.ensureBusinessValue(EcomConstants.MARKETPLACE_DICT, s.marketplace(), s.marketplace());
        }
        warehouses(ctx);
        channels(ctx);
        taxes();
    }

    /** 启用演示站点所在国家的内置税率（德国 VAT、日本消费税、新加坡 GST、美国销售税），订单写入时按它计算税额 */
    private void taxes() {
        for (String country : TAX_COUNTRIES) {
            for (EcomTaxRate r : taxService.page(1, 20, country, null, null).records()) {
                if (r.getStatus() != EnableStatus.ENABLED) {
                    taxService.changeStatus(r.getId(), EnableStatus.ENABLED);
                }
            }
        }
    }

    /** 历史区间（含之前一个月的头程、采购）每个月的汇率，已有的不改 */
    private void rates(DemoContext ctx) {
        LocalDate from = ctx.today().minusDays(ctx.days() + 45L);
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(ctx.today())); m = m.plusMonths(1)) {
            int period = m.getYear() * 100 + m.getMonthValue();
            for (Map.Entry<String, Double> e : RATES.entrySet()) {
                if (currencyService.currencies().stream().noneMatch(c -> c.getCode().equals(e.getKey()))
                        || !currencyService.rates(period, e.getKey()).isEmpty()) {
                    continue;
                }
                double drift = 1 + (ctx.random().nextDouble() - 0.5) * 0.02;
                currencyService.saveRate(e.getKey(), period, BigDecimal.valueOf(e.getValue() * drift).setScale(6, RoundingMode.HALF_UP));
            }
        }
    }

    private void warehouses(DemoContext ctx) {
        Map<String, Long> existing = new HashMap<>();
        warehouseService.options().forEach(o -> existing.put(o.code(), o.id()));
        Map<String, Long> ids = new LinkedHashMap<>();
        int sort = 20;
        for (DemoCatalog.Warehouse w : DemoCatalog.WAREHOUSES) {
            Long id = existing.get(w.code());
            if (id == null) {
                WmsWarehouse wh = new WmsWarehouse();
                wh.setCode(w.code());
                wh.setName(w.name());
                wh.setAddress(w.address());
                wh.setType(w.defect() ? WarehouseType.DEFECT : WarehouseType.FINISHED);
                wh.setContact(w == DemoCatalog.LOCAL_WAREHOUSE ? "仓库主管 李强" : null); // i18n-ignore 演示数据存库
                wh.setSort(sort++);
                wh.setStatus(EnableStatus.ENABLED);
                warehouseService.create(wh);
                id = wh.getId();
            }
            ids.put(w.code(), id);
        }
        ctx.put(DemoContributors.WAREHOUSE_BY_CODE, ids);
        ctx.put(DemoKeys.LOCAL_WAREHOUSE_ID, ids.get(DemoCatalog.LOCAL_WAREHOUSE.code()));
        ctx.put(DemoKeys.DEFECT_WAREHOUSE_ID, ids.get(DemoCatalog.DEFECT_WAREHOUSE.code()));
        ctx.put(DemoKeys.PLATFORM_WAREHOUSE_IDS, List.of(ids.get(DemoCatalog.FBA_US.code()), ids.get(DemoCatalog.FBA_DE.code()),
                ids.get(DemoCatalog.FBA_JP.code()), ids.get(DemoCatalog.US_3PL.code())));
    }

    private void channels(DemoContext ctx) {
        DemoCatalog.Supplier f = DemoCatalog.FORWARDER;
        Long forwarderId = supplierService.findIdByCodeOrName(f.code());
        if (forwarderId == null) {
            ScmSupplier s = new ScmSupplier();
            s.setCode(f.code());
            s.setName(f.name());
            s.setContact(f.contact());
            s.setPhone(f.phone());
            s.setAddress(f.address());
            s.setPaymentDays(30);
            s.setLogistics(1);
            s.setStatus(EnableStatus.ENABLED);
            s.setRemark("头程货代：美国 / 欧洲 / 日本 FBA 空派、海派"); // i18n-ignore 演示数据存库
            supplierService.create(s);
            forwarderId = s.getId();
        }
        ctx.put(DemoContributors.FORWARDER_ID, forwarderId);
        Map<String, Long> ids = new LinkedHashMap<>();
        // 头程
        ids.put("DEMO-AIR-US", channel("DEMO-AIR-US", "美国空派（FBA）", "环球通", "AIR", forwarderId, 8, 12, "头程：空运 + 美国本土派送到 FBA", // i18n-ignore 演示数据存库
                List.of(rate("US", 0, 100.0, 0, 46), rate("US", 100, null, 0, 40))));
        ids.put("DEMO-SEA-US", channel("DEMO-SEA-US", "美森快船（FBA）", "环球通", "SEA", forwarderId, 18, 25, "头程：美森快船 + 卡派到 FBA / 海外仓", // i18n-ignore 演示数据存库
                List.of(rate("US", 0, null, 0, 13.5))));
        ids.put("DEMO-AIR-EU", channel("DEMO-AIR-EU", "欧洲空派（FBA）", "环球通", "AIR", forwarderId, 10, 15, "头程：空运 + 欧洲本土派送到 FBA", // i18n-ignore 演示数据存库
                List.of(rate("DE", 0, null, 0, 52), rate("*", 0, null, 0, 58))));
        ids.put("DEMO-SEA-JP", channel("DEMO-SEA-JP", "日本海运（FBA）", "环球通", "SEA", forwarderId, 7, 12, "头程：海运 + 日本本土派送到 FBA", // i18n-ignore 演示数据存库
                List.of(rate("JP", 0, null, 0, 9.5))));
        // 尾程（自发货）
        ids.put("DEMO-YUN", channel("DEMO-YUN", "云途全球专线挂号", "云途 YunExpress", "LINE", null, 7, 12, "自发货尾程：普货专线", // i18n-ignore 演示数据存库
                List.of(rate("US", 0, 0.5, 18, 72), rate("US", 0.5, null, 22, 65), rate("*", 0, 0.5, 20, 80), rate("*", 0.5, null, 25, 75))));
        ids.put("DEMO-4PX", channel("DEMO-4PX", "4PX 联邮通优先", "递四方 4PX", "LINE", null, 8, 14, "自发货尾程：带电专线", // i18n-ignore 演示数据存库
                List.of(rate("US", 0, null, 20, 70), rate("*", 0, null, 22, 78))));
        ids.put("DEMO-DHL", channel("DEMO-DHL", "DHL 国际快递", "DHL", "EXPRESS", null, 3, 6, "自发货尾程：高货值、加急", // i18n-ignore 演示数据存库
                List.of(rate("*", 0, 0.5, 120, 0), rate("*", 0.5, null, 120, 95))));
        ids.put("DEMO-SLS", channel("DEMO-SLS", "Shopee SLS 标准快递", "Shopee SLS", "LINE", null, 5, 9, "东南亚：虾皮官方物流（平台面单）", // i18n-ignore 演示数据存库
                List.of(rate("MY", 0, null, 8, 26), rate("SG", 0, null, 9, 30))));
        ctx.put(DemoKeys.CHANNEL_BY_CODE, ids);
    }

    private Long channel(String code, String name, String provider, String mode, Long supplierId, int min, int max, String remark,
                         List<EcomLogisticsRate> rates) {
        EcomLogisticsChannel existing = logisticsService.options().stream().filter(c -> code.equals(c.getCode())).findFirst().orElse(null);
        if (existing != null) {
            return existing.getId();
        }
        EcomLogisticsChannel c = new EcomLogisticsChannel();
        c.setCode(code);
        c.setName(name);
        c.setProvider(provider);
        c.setTransportMode(mode);
        c.setSupplierId(supplierId);
        c.setDaysMin(min);
        c.setDaysMax(max);
        c.setRemark(remark);
        c.setStatus(EnableStatus.ENABLED);
        c.setRates(new ArrayList<>(rates));
        return logisticsService.create(c);
    }

    private static EcomLogisticsRate rate(String country, double min, Double max, double base, double perKg) {
        EcomLogisticsRate r = new EcomLogisticsRate();
        r.setCountry(country);
        r.setMinWeight(BigDecimal.valueOf(min));
        r.setMaxWeight(max == null ? null : BigDecimal.valueOf(max));
        r.setBaseFee(BigDecimal.valueOf(base));
        r.setPerKg(BigDecimal.valueOf(perKg));
        return r;
    }
}
