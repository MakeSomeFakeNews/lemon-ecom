package com.lemon.erp.modules.ecom.dev.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.demo.DemoCatalog;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoImages;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688AccountRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevProjectRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevSampleRequest;
import com.lemon.erp.modules.ecom.dev.dto.ProfitCalcRequest;
import com.lemon.erp.modules.ecom.dev.dto.ProfitSchemeRequest;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Offer;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Order;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688OrderItem;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688PriceLog;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevLog;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevProject;
import com.lemon.erp.modules.ecom.dev.entity.EcomProfitScheme;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OfferMapper;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OrderItemMapper;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OrderMapper;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688PriceLogMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevLogMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevProjectMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomProfitSchemeMapper;
import com.lemon.erp.modules.ecom.dev.model.Ali1688OrderStatus;
import com.lemon.erp.modules.ecom.dev.model.DevHeadPricing;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import com.lemon.erp.modules.ecom.dev.model.DevSampleResult;
import com.lemon.erp.modules.ecom.dev.service.Ali1688AccountService;
import com.lemon.erp.modules.ecom.dev.service.EcomDevProjectService;
import com.lemon.erp.modules.ecom.dev.service.EcomProfitCalcService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.scm.dto.OrderQuery;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;
import com.lemon.erp.modules.wms.service.WmsMaterialCategoryService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 产品开发与 1688 采购（order 160）：
 * <ul>
 *   <li>16 个开发项目覆盖全部状态（待调研、调研中、打样中、待审批、已通过并生成物料、已驳回、已放弃），带示意图、样品记录（合格 / 不合格 / 待评估）、
 *       流转与审批记录（时间分布在最近两个月），有售价的项目保存利润试算方案并回写试算利润率；另有几个独立的多平台试算方案；</li>
 *   <li>1688 采购账号（填好收货地址，未授权，授权后才会调用接口）；演示商品关联 1688 货源（价格、起批量、库存、阶梯价），
 *       部分货源有价格变动记录和待确认提醒；</li>
 *   <li>从演示采购订单生成的 1688 采购单：交易成功（含物流轨迹）、待收货、待发货、待付款、已取消，回写采购订单外部单号。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DevDemoContributor implements DemoContributor {

    private final EcomDevProjectService projectService;
    private final EcomProfitCalcService calcService;
    private final Ali1688AccountService accountService;
    private final EcomDevProjectMapper projectMapper;
    private final EcomDevLogMapper logMapper;
    private final EcomProfitSchemeMapper schemeMapper;
    private final Ecom1688OfferMapper offerMapper;
    private final Ecom1688PriceLogMapper priceLogMapper;
    private final Ecom1688OrderMapper orderMapper;
    private final Ecom1688OrderItemMapper orderItemMapper;
    private final WmsMaterialCategoryService categoryService;
    private final WmsMaterialService materialService;
    private final ScmSupplierService supplierService;
    private final ScmOrderService scmOrderService;
    private final ObjectMapper objectMapper;

    @Override
    public int order() {
        return 160;
    }

    @Override
    public String key() {
        return "productDev";
    }

    /**
     * 演示开发项目
     *
     * @param target 最终状态；price / cost 为目标售价（站点币种）/ 预估采购价（元）
     */
    private record Idea(String name, String en, String category, String brand, String source, EcomPlatform platform, String marketplace,
                        double competitorPrice, int reviews, double rating, int monthlySales, double price, double cost, double weight,
                        double l, double w, double h, String color, DemoImages.Icon icon, DevProjectStatus target, String points, String note) {
    }

    private static final List<Idea> IDEAS = List.of(
            new Idea("磁吸无线充电宝 5000mAh", "Magnetic Wireless Power Bank", "3C-POWER", "VOLTIX", "ALI1688", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    32.99, 4120, 4.4, 900, 29.99, 46, 0.16, 11, 7, 1.6, "#1D39C4", DemoImages.Icon.BATTERY, DevProjectStatus.APPROVED,
                    "磁吸对位、7.5W 无线快充、支架一体", "竞品评论多集中在发热和吸力不足，样品二已改进散热"), // i18n-ignore 演示数据存库
            new Idea("桌面伸缩手机支架", "Foldable Desk Phone Stand", "3C-ACC", "VOLTIX", "BESTSELLER", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    15.99, 8800, 4.6, 1500, 14.99, 11.5, 0.22, 16, 9, 3, "#10239E", DemoImages.Icon.PHONE, DevProjectStatus.APPROVED,
                    "铝合金、可调高度、兼容平板", "类目头部价格稳定在 14–17 美元，走量款"), // i18n-ignore 演示数据存库
            new Idea("宠物自动饮水机 2L", "Cat Water Fountain 2L", "PET-CAT", "PAWJOY", "FACTORY", EcomPlatform.AMAZON, "UK", // i18n-ignore 演示数据存库
                    29.99, 2300, 4.3, 400, 27.99, 58, 0.95, 20, 20, 16, "#D4380D", DemoImages.Icon.DROP, DevProjectStatus.APPROVED,
                    "静音水泵、三重过滤、缺水断电", "英国站竞争小于美国站，VAT 20% 已计入试算"), // i18n-ignore 演示数据存库
            new Idea("可折叠硅胶水杯", "Collapsible Silicone Cup", "HOME-KITCHEN", "HOMELUX", "ALI1688", EcomPlatform.AMAZON, "DE", // i18n-ignore 演示数据存库
                    13.99, 1200, 4.2, 600, 12.99, 9.8, 0.12, 9, 9, 4, "#08979C", DemoImages.Icon.CUP, DevProjectStatus.PENDING_APPROVAL,
                    "食品级硅胶、折叠后 4cm、带挂扣", "德国站户外场景需求稳定，已打样两次"), // i18n-ignore 演示数据存库
            new Idea("真空保鲜收纳袋 10 只装", "Vacuum Storage Bags 10 Pack", "HOME-STORAGE", "HOMELUX", "BESTSELLER", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    24.99, 15600, 4.5, 1100, 22.99, 19, 1.1, 30, 22, 6, "#389E0D", DemoImages.Icon.BOX, DevProjectStatus.PENDING_APPROVAL,
                    "加厚 PA+PE、附手泵", "体积大，头程按海运测算"), // i18n-ignore 演示数据存库
            new Idea("超轻户外折叠椅", "Ultralight Camping Chair", "OUTDOOR-CAMP", "TRAILPRO", "FACTORY", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    49.99, 3100, 4.4, 300, 45.99, 72, 1.25, 40, 14, 13, "#5B8C00", DemoImages.Icon.CHAIR, DevProjectStatus.SAMPLING,
                    "7075 铝合金、承重 150kg、收纳 40cm", "等待第二家工厂样品对比"), // i18n-ignore 演示数据存库
            new Idea("露营氛围灯串 10m", "Camping String Lights 10m", "OUTDOOR-CAMP", "TRAILPRO", "ALI1688", EcomPlatform.TIKTOK, "US", // i18n-ignore 演示数据存库
                    19.99, 900, 4.1, 800, 17.99, 13, 0.3, 15, 15, 5, "#FAAD14", DemoImages.Icon.LANTERN, DevProjectStatus.SAMPLING,
                    "USB 供电、8 种模式、防水 IPX4", "TikTok 短视频带货测试款"), // i18n-ignore 演示数据存库
            new Idea("智能逗猫棒", "Interactive Cat Toy Wand", "PET-CAT", "PAWJOY", "DESIGN", EcomPlatform.AMAZON, "JP", // i18n-ignore 演示数据存库
                    2480, 760, 4.0, 500, 2280, 16, 0.18, 30, 6, 4, "#CF1322", DemoImages.Icon.PAW, DevProjectStatus.RESEARCHING,
                    "自动旋转、羽毛可替换", "日本站价格敏感，需控制采购价在 18 元以内"), // i18n-ignore 演示数据存库
            new Idea("宠物慢食碗", "Slow Feeder Dog Bowl", "PET-DOG", "PAWJOY", "ALI1688", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    14.99, 6400, 4.5, 700, 13.99, 8.5, 0.35, 22, 22, 6, "#FA541C", DemoImages.Icon.BONE, DevProjectStatus.RESEARCHING,
                    "防滑底座、迷宫设计", null), // i18n-ignore 演示数据存库
            new Idea("运动腰包", "Running Belt Waist Pack", "OUTDOOR-SPORT", "TRAILPRO", "BESTSELLER", EcomPlatform.AMAZON, "DE", // i18n-ignore 演示数据存库
                    16.99, 2100, 4.3, 450, 15.99, 12, 0.1, 20, 10, 2, "#237804", DemoImages.Icon.BOTTLE, DevProjectStatus.RESEARCHING,
                    "弹力面料、反光条、可放 6.7 寸手机", null), // i18n-ignore 演示数据存库
            new Idea("瑜伽弹力带套装", "Resistance Bands Set", "OUTDOOR-SPORT", "TRAILPRO", "BESTSELLER", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    21.99, 22000, 4.6, 0, 0, 0, 0, 0, 0, 0, "#7CB305", DemoImages.Icon.HEART, DevProjectStatus.PENDING_RESEARCH,
                    null, "类目竞争激烈，先看差异化方向"), // i18n-ignore 演示数据存库
            new Idea("厨房计量电子秤", "Digital Kitchen Scale", "HOME-KITCHEN", "HOMELUX", "OTHER", EcomPlatform.WALMART, "US", // i18n-ignore 演示数据存库
                    18.99, 5100, 4.5, 0, 0, 0, 0, 0, 0, 0, "#13C2C2", DemoImages.Icon.CUP, DevProjectStatus.PENDING_RESEARCH, null, null),
            new Idea("硅胶折叠收纳盒", "Collapsible Storage Bin", "HOME-STORAGE", "HOMELUX", "ALI1688", EcomPlatform.EBAY, "US", // i18n-ignore 演示数据存库
                    19.99, 300, 4.0, 0, 0, 0, 0, 0, 0, 0, "#52C41A", DemoImages.Icon.BOX, DevProjectStatus.PENDING_RESEARCH, null, null),
            new Idea("猫抓板沙发保护贴", "Cat Scratch Furniture Protector", "PET-CAT", "PAWJOY", "ALI1688", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    12.99, 9800, 4.2, 600, 9.99, 9, 0.4, 30, 20, 2, "#FA8C16", DemoImages.Icon.MAT, DevProjectStatus.REJECTED,
                    "透明 PET、免钉安装", null), // i18n-ignore 演示数据存库
            new Idea("蓝牙防丢器", "Bluetooth Item Finder", "3C-ACC", "VOLTIX", "BESTSELLER", EcomPlatform.AMAZON, "US", // i18n-ignore 演示数据存库
                    24.99, 31000, 4.4, 0, 21.99, 22, 0.05, 8, 6, 2, "#2F54EB", DemoImages.Icon.KEYBOARD, DevProjectStatus.ABANDONED,
                    "兼容苹果查找网络", null), // i18n-ignore 演示数据存库
            new Idea("车载手机支架", "Car Phone Mount", "3C-ACC", "VOLTIX", "ALI1688", EcomPlatform.TEMU, "US", // i18n-ignore 演示数据存库
                    9.99, 12000, 4.3, 0, 8.99, 7, 0.15, 12, 8, 6, "#597EF7", DemoImages.Icon.PHONE, DevProjectStatus.ABANDONED, null, null));

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String US = "US";
    private static final String SEA = "SEA";
    private static final String AIR = "AIR";

    /** 演示供应商在 1688 的卖家登录名（按 DemoCatalog.SUPPLIERS 顺序） */
    private static final String[] SELLERS = {"wotedianzi2008", "lianchuangxiancai", "youjiariyong888", "beihudianqi", "shanyehuwai", "mengchongpet"};

    @Override
    public void generate(DemoContext ctx) {
        Map<String, String> categories = categoryCodes();
        List<Long> owners = ctx.get(DemoKeys.OPERATOR_USER_IDS);
        Map<String, Long> suppliers = new LinkedHashMap<>();
        for (DemoCatalog.Supplier s : DemoCatalog.SUPPLIERS) {
            Long id = supplierService.findIdByCodeOrName(s.code());
            if (id != null) {
                suppliers.put(s.code(), id);
            }
        }
        projects(ctx, categories, owners, suppliers);
        standaloneSchemes(ctx);
        accountService.create(new Ali1688AccountRequest("演示 1688 采购账号", "8521473", null, null, "周晓梅", "13800138000", // i18n-ignore 演示数据存库
                "广东省", "深圳市", "龙岗区", "平湖街道", "平湖物流园 B3 栋 1 楼收货处", "518111", 1, EnableStatus.ENABLED, // i18n-ignore 演示数据存库
                "演示账号：填写真实的 AppKey / AppSecret 后点「授权」即可调用 1688 接口")); // i18n-ignore 演示数据存库
        Map<Long, Ecom1688Offer> offers = offers(ctx, suppliers);
        orders(ctx, offers, suppliers);
    }

    // ---------------------------------------------------------------- 开发项目

    private void projects(DemoContext ctx, Map<String, String> categories, List<Long> owners, Map<String, Long> suppliers) {
        List<Long> supplierIds = new ArrayList<>(suppliers.values());
        int i = 0;
        for (Idea idea : IDEAS) {
            boolean priced = idea.price() > 0;
            Long owner = owners == null || owners.isEmpty() ? null : owners.get(i % owners.size());
            String currency = currencyOf(idea.marketplace());
            DevProjectRequest req = new DevProjectRequest(idea.name(), categories.get(idea.category()), idea.brand(), idea.source(), idea.platform(),
                    idea.marketplace(), currency, "https://www.amazon.com/dp/B0" + (8_100_000 + i * 7919L) + "X", bd(idea.competitorPrice()),
                    idea.reviews(), BigDecimal.valueOf(idea.rating()), priced ? idea.monthlySales() : null, priced ? bd(idea.price()) : null,
                    priced ? bd(idea.cost()) : null, priced ? BigDecimal.valueOf(idea.weight()) : null, priced ? bd(idea.l()) : null,
                    priced ? bd(idea.w()) : null, priced ? bd(idea.h()) : null, owner, idea.points(), idea.note(), null);
            Long id = projectService.create(req);
            byte[] png = DemoImages.render(idea.color(), idea.icon(), idea.en(), DemoCatalog.BRANDS.getOrDefault(idea.brand(), idea.brand()));
            projectService.uploadImage(id, DemoImages.file("dev-" + (i + 1) + ".png", png));
            DevProjectStatus target = idea.target();
            if (target != DevProjectStatus.PENDING_RESEARCH) {
                if (target == DevProjectStatus.ABANDONED && !priced) {
                    projectService.move(id, DevProjectStatus.ABANDONED, "类目竞争过于激烈"); // i18n-ignore 演示数据存库
                } else {
                    projectService.move(id, DevProjectStatus.RESEARCHING, null);
                }
            }
            if (priced && target != DevProjectStatus.RESEARCHING && target != DevProjectStatus.PENDING_RESEARCH) {
                samples(ctx, id, idea, target, supplierIds);
            }
            if (priced) {
                saveScheme(idea.name() + " · " + idea.marketplace() + " 站", project(ctx, idea, id)); // i18n-ignore 演示数据存库
            }
            switch (target) {
                case PENDING_APPROVAL -> projectService.submit(id);
                case APPROVED -> {
                    projectService.submit(id);
                    projectService.approve(id, "利润率达标，同意开发，先下 500 件首单"); // i18n-ignore 演示数据存库
                }
                case REJECTED -> {
                    projectService.submit(id);
                    projectService.reject(id, "利润率低于 15%，且头部卖家价格战明显，暂不开发"); // i18n-ignore 演示数据存库
                }
                case ABANDONED -> {
                    if (priced) {
                        projectService.move(id, DevProjectStatus.ABANDONED, "供应商无法提供专利授权，存在侵权风险"); // i18n-ignore 演示数据存库
                    }
                }
                default -> {
                }
            }
            backdate(ctx, id, i);
            i++;
        }
    }

    private void samples(DemoContext ctx, Long projectId, Idea idea, DevProjectStatus target, List<Long> supplierIds) {
        LocalDate base = ctx.today().minusDays(40 + ctx.between(0, 10));
        boolean done = target == DevProjectStatus.APPROVED || target == DevProjectStatus.PENDING_APPROVAL || target == DevProjectStatus.REJECTED
                || target == DevProjectStatus.ABANDONED;
        int idx = idea.category().startsWith("3C") ? ctx.between(0, 1) : idea.category().startsWith("HOME") ? ctx.between(2, 3)
                : idea.category().startsWith("OUTDOOR") ? 4 : 5;
        Long s1 = supplierIds.size() > idx ? supplierIds.get(idx) : null;
        projectService.addSample(projectId, new DevSampleRequest(s1, s1 == null ? "义乌市佳和日用品厂" : null, // i18n-ignore 演示数据存库
                "https://detail.1688.com/offer/" + (680_000_000_000L + ctx.between(1000, 9999) * 13L) + ".html", 2, bd(35 + ctx.between(0, 40)),
                bd(idea.cost() * 1.06), base, base.plusDays(6), done ? DevSampleResult.FAILED : DevSampleResult.PENDING,
                done ? "做工一般，接口处有毛边，包装偏大增加头程成本" : null)); // i18n-ignore 演示数据存库
        projectService.addSample(projectId, new DevSampleRequest(null, "深圳市品航科技有限公司", null, 3, bd(68 + ctx.between(0, 30)), // i18n-ignore 演示数据存库
                bd(idea.cost()), base.plusDays(3), done ? base.plusDays(11) : null, done ? DevSampleResult.PASSED : DevSampleResult.PENDING,
                done ? "外观与功能达标，包装尺寸压缩 15%，报价可接受" : null)); // i18n-ignore 演示数据存库
    }

    private ProfitCalcRequest project(DemoContext ctx, Idea idea, Long projectId) {
        boolean sea = idea.weight() >= 1;
        return new ProfitCalcRequest(idea.platform(), idea.marketplace(), null, null, null, bd(idea.price()), BigDecimal.valueOf(25), bd(idea.cost()),
                bd(1.5), BigDecimal.valueOf(idea.weight()), bd(idea.l()), bd(idea.w()), bd(idea.h()), sea ? SEA : AIR, DevHeadPricing.PER_KG,
                null, bd(sea ? 12 : 38), null, BigDecimal.ZERO, idea.platform() == EcomPlatform.AMAZON && US.equals(idea.marketplace()),
                idea.platform() == EcomPlatform.AMAZON && US.equals(idea.marketplace()) ? null : bd(fulfillment(idea)), null, null, null,
                bd(8 + ctx.between(0, 4)), bd(3), null, null, null, projectId);
    }

    /** 非美国站 / 非亚马逊的配送费（站点币种）参考值 */
    private static double fulfillment(Idea idea) {
        if ("JP".equals(idea.marketplace())) {
            return 434;
        }
        if ("DE".equals(idea.marketplace()) || "UK".equals(idea.marketplace())) {
            return idea.weight() > 0.5 ? 4.6 : 3.1;
        }
        return idea.weight() > 0.5 ? 6.8 : 4.2;
    }

    /** 保存试算方案：先试算（不在事务中出错），失败的跳过 */
    private void saveScheme(String name, ProfitCalcRequest req) {
        try {
            calcService.calc(req);
        } catch (BizException e) {
            return;
        }
        calcService.saveScheme(new ProfitSchemeRequest(name.length() > 64 ? name.substring(0, 64) : name, req));
    }

    /** 把项目、流转记录、方案的时间分布到最近两个月（按创建顺序） */
    private void backdate(DemoContext ctx, Long id, int index) {
        LocalDateTime start = ctx.today().minusDays(58 - index * 3L).atTime(9 + index % 8, 10 + index * 3 % 50);
        projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate().set(EcomDevProject::getCreateTime, start).eq(EcomDevProject::getId, id));
        List<EcomDevLog> logs = logMapper.selectList(Wrappers.<EcomDevLog>lambdaQuery().eq(EcomDevLog::getProjectId, id).orderByAsc(EcomDevLog::getId));
        LocalDateTime t = start;
        for (EcomDevLog l : logs) {
            logMapper.update(Wrappers.<EcomDevLog>lambdaUpdate().set(EcomDevLog::getCreateTime, t).eq(EcomDevLog::getId, l.getId()));
            t = t.plusDays(2 + ctx.between(0, 4)).plusHours(ctx.between(0, 6));
            if (t.isAfter(ctx.today().atTime(18, 0))) {
                t = ctx.today().atTime(17, 30);
            }
        }
        LocalDateTime last = t;
        projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate().set(EcomDevProject::getUpdateTime, last).eq(EcomDevProject::getId, id));
        projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate().set(EcomDevProject::getApproveTime, last.minusHours(3))
                .isNotNull(EcomDevProject::getApproveTime).eq(EcomDevProject::getId, id));
        schemeMapper.update(Wrappers.<EcomProfitScheme>lambdaUpdate().set(EcomProfitScheme::getCreateTime, start.plusDays(4))
                .eq(EcomProfitScheme::getProjectId, id));
    }

    /** 独立的试算方案：同一款充电宝在多个平台 / 站点的对比 */
    private void standaloneSchemes(DemoContext ctx) {
        Object[][] rows = {
                {"充电宝 10000mAh · 美国站 FBA", EcomPlatform.AMAZON, "US", 25.99, null}, // i18n-ignore 演示数据存库
                {"充电宝 10000mAh · 德国站 FBA", EcomPlatform.AMAZON, "DE", 24.99, 3.4}, // i18n-ignore 演示数据存库
                {"充电宝 10000mAh · 日本站 FBA", EcomPlatform.AMAZON, "JP", 3480d, 434d}, // i18n-ignore 演示数据存库
                {"充电宝 10000mAh · 沃尔玛", EcomPlatform.WALMART, "US", 24.99, 5.2}, // i18n-ignore 演示数据存库
                {"充电宝 10000mAh · TikTok Shop", EcomPlatform.TIKTOK, "US", 22.99, 4.6}}; // i18n-ignore 演示数据存库
        for (Object[] r : rows) {
            EcomPlatform p = (EcomPlatform) r[1];
            String mkt = (String) r[2];
            boolean fba = p == EcomPlatform.AMAZON && US.equals(mkt);
            ProfitCalcRequest req = new ProfitCalcRequest(p, mkt, "消费电子", null, null, bd((Double) r[3]), BigDecimal.valueOf(20), bd(38), // i18n-ignore 演示数据存库
                    bd(1.2), BigDecimal.valueOf(0.24), bd(14), bd(7), bd(1.6), AIR, DevHeadPricing.PER_KG, null, bd(38), null, BigDecimal.ZERO,
                    fba, r[4] == null ? null : bd((Double) r[4]), null, null, null, bd(10), bd(2.5), null, null, null, null);
            saveScheme((String) r[0], req);
        }
    }

    // ---------------------------------------------------------------- 1688 货源

    private Map<Long, Ecom1688Offer> offers(DemoContext ctx, Map<String, Long> suppliers) {
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        Map<Long, Ecom1688Offer> out = new HashMap<>();
        if (materials == null) {
            return out;
        }
        Map<Long, String> images = new HashMap<>();
        materialService.mapByIds(materials.values()).forEach((id, m) -> images.put(id, m.getImageUrl()));
        int idx = 0;
        for (DemoCatalog.Product p : DemoCatalog.PRODUCTS) {
            idx++;
            Long materialId = materials.get(p.code());
            if (materialId == null || idx % 7 == 3) {
                continue;
            }
            DemoCatalog.Supplier sup = DemoCatalog.SUPPLIERS.get(p.supplier());
            int moq = new int[]{2, 5, 10, 20, 50}[ctx.between(0, 4)];
            BigDecimal price = BigDecimal.valueOf(p.cost() * (0.9 + ctx.between(0, 6) / 100.0)).setScale(2, RoundingMode.HALF_UP);
            Ecom1688Offer o = new Ecom1688Offer();
            o.setMaterialId(materialId);
            o.setOfferId(String.valueOf(600_000_000_000L + idx * 104_729L + ctx.between(0, 999)));
            o.setSpecId(idx % 3 == 0 ? null : Long.toHexString(0xa1b2c3d4e5L + idx * 7_919L));
            o.setSkuId(o.getSpecId() == null ? null : String.valueOf(4_800_000_000_000L + idx * 31L));
            o.setSupplierId(suppliers.get(sup.code()));
            o.setSellerLoginId(SELLERS[p.supplier()]);
            o.setSellerName(sup.name());
            o.setTitle(p.name() + " 跨境专供 源头工厂 一件代发"); // i18n-ignore 演示数据存库
            o.setImageUrl(images.get(materialId));
            o.setSpecText(o.getSpecId() == null ? null : p.spec());
            o.setUnit(p.unit());
            o.setPrice(price);
            o.setMinOrderQty(moq);
            o.setStock(ctx.between(8, 200) * 100);
            o.setQtyRatio(BigDecimal.ONE);
            o.setPriceRanges(json(List.of(Map.of("startQuantity", moq, "price", price),
                    Map.of("startQuantity", moq * 10, "price", price.multiply(BigDecimal.valueOf(0.96)).setScale(2, RoundingMode.HALF_UP)),
                    Map.of("startQuantity", moq * 50, "price", price.multiply(BigDecimal.valueOf(0.92)).setScale(2, RoundingMode.HALF_UP)))));
            o.setAlertThreshold(idx % 4 == 0 ? BigDecimal.valueOf(3) : null);
            o.setSyncTime(ctx.today().atTime(7, ctx.between(0, 50)));
            o.setPriceAlert(0);
            offerMapper.insert(o);
            // 价格变动：约三分之一的货源有历史调价，其中几条最近一次变动待确认
            if (idx % 3 == 1) {
                BigDecimal prev = price;
                int changes = ctx.between(1, 3);
                for (int c = 0; c < changes; c++) {
                    BigDecimal old = prev.multiply(BigDecimal.valueOf(1 + (ctx.chance(0.5) ? -1 : 1) * ctx.between(2, 9) / 100.0))
                            .setScale(2, RoundingMode.HALF_UP);
                    Ecom1688PriceLog log = new Ecom1688PriceLog();
                    log.setOfferId(o.getId());
                    log.setMaterialId(materialId);
                    log.setOldPrice(old);
                    log.setNewPrice(prev);
                    log.setChangeRate(prev.subtract(old).multiply(BigDecimal.valueOf(100)).divide(old, 2, RoundingMode.HALF_UP));
                    log.setCreateTime(ctx.today().minusDays(3L + c * 25L + ctx.between(0, 10)).atTime(7, 5));
                    priceLogMapper.insert(log);
                    if (c == 0) {
                        boolean alert = idx % 2 == 1;
                        offerMapper.update(Wrappers.<Ecom1688Offer>lambdaUpdate().set(Ecom1688Offer::getLastPrice, old)
                                .set(Ecom1688Offer::getPriceChangeTime, log.getCreateTime()).set(Ecom1688Offer::getPriceAlert, alert ? 1 : 0)
                                .eq(Ecom1688Offer::getId, o.getId()));
                    }
                    prev = old;
                }
            }
            out.put(materialId, o);
        }
        return out;
    }

    // ---------------------------------------------------------------- 1688 采购单

    private void orders(DemoContext ctx, Map<Long, Ecom1688Offer> offers, Map<String, Long> suppliers) {
        if (offers.isEmpty()) {
            return;
        }
        Long accountId = accountService.list().stream().filter(a -> a.getIsDefault() == 1).map(a -> a.getId()).findFirst().orElse(null);
        if (accountId == null) {
            return;
        }
        List<ScmOrder> pos = new ArrayList<>();
        for (Long supplierId : suppliers.values()) {
            pos.addAll(scmOrderService.page(OrderKind.PURCHASE, 1, 20, new OrderQuery(null, supplierId, null, null, null, null, null)).records());
        }
        pos.sort((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()));
        // 未入库的订单：待付款 / 待发货交替；部分入库：待收货；已完成（最多 8 张）：交易成功，其中一张已收货未确认
        int approved = 0;
        int completed = 0;
        int n = 0;
        for (ScmOrder po : pos) {
            Ali1688OrderStatus status;
            if (po.getStatus() == OrderStatus.APPROVED) {
                status = approved++ % 2 == 0 ? Ali1688OrderStatus.WAIT_SELLER_SEND : Ali1688OrderStatus.WAIT_BUYER_PAY;
            } else if (po.getStatus() == OrderStatus.PARTIAL) {
                status = Ali1688OrderStatus.WAIT_BUYER_RECEIVE;
            } else if (po.getStatus() == OrderStatus.COMPLETED && completed < 8) {
                status = completed++ == 1 ? Ali1688OrderStatus.CONFIRM_GOODS : Ali1688OrderStatus.SUCCESS;
            } else {
                continue;
            }
            if (createOrder(ctx, accountId, po, offers, status, n)) {
                n++;
            }
        }
    }

    private boolean createOrder(DemoContext ctx, Long accountId, ScmOrder po, Map<Long, Ecom1688Offer> offers, Ali1688OrderStatus status, int n) {
        ScmOrder detail = scmOrderService.detail(OrderKind.PURCHASE, po.getId());
        List<Ecom1688OrderItem> items = new ArrayList<>();
        Ecom1688Offer first = null;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal qty = BigDecimal.ZERO;
        for (ScmOrderItem it : detail.getItems()) {
            Ecom1688Offer o = offers.get(it.getMaterialId());
            if (o == null) {
                continue;
            }
            if (first == null) {
                first = o;
            } else if (!first.getSellerLoginId().equals(o.getSellerLoginId())) {
                continue;
            }
            Ecom1688OrderItem item = new Ecom1688OrderItem();
            item.setScmOrderItemId(it.getId());
            item.setMaterialId(it.getMaterialId());
            item.setOfferId(o.getOfferId());
            item.setSpecId(o.getSpecId());
            item.setQty(it.getQty().setScale(0, RoundingMode.CEILING));
            item.setMaterialQty(it.getQty());
            item.setPrice(o.getPrice());
            item.setAmount(o.getPrice().multiply(item.getQty()).setScale(2, RoundingMode.HALF_UP));
            amount = amount.add(item.getAmount());
            qty = qty.add(item.getQty());
            items.add(item);
        }
        if (items.isEmpty()) {
            return false;
        }
        LocalDateTime created = po.getOrderDate().atTime(10, 20 + n);
        BigDecimal freight = BigDecimal.valueOf(ctx.between(0, 1) == 0 ? 0 : ctx.between(8, 45));
        Ecom1688Order o = new Ecom1688Order();
        o.setAccountId(accountId);
        o.setScmOrderId(po.getId());
        o.setAliOrderId(String.valueOf(3_712_000_000_000_000_000L + po.getId() * 1_000_003L + n));
        o.setSellerLoginId(first.getSellerLoginId());
        o.setSellerName(first.getSellerName());
        o.setStatus(status);
        o.setAliStatus(switch (status) {
            case WAIT_BUYER_PAY -> "waitbuyerpay";
            case WAIT_SELLER_SEND -> "waitsellersend";
            case WAIT_BUYER_RECEIVE -> "waitbuyerreceive";
            case CONFIRM_GOODS -> "confirm_goods";
            case CANCEL -> "cancel";
            case TERMINATED -> "terminated";
            default -> "success";
        });
        o.setProductAmount(amount);
        o.setShippingFee(freight);
        o.setTotalAmount(amount.add(freight));
        o.setTotalQty(qty);
        o.setMessage(n % 3 == 0 ? "请用纸箱包装，外箱贴采购单号 " + po.getOrderNo() : null); // i18n-ignore 演示数据存库
        o.setAliCreateTime(created);
        o.setCreateTime(created);
        boolean paid = status != Ali1688OrderStatus.WAIT_BUYER_PAY && status != Ali1688OrderStatus.CANCEL;
        boolean shipped = status == Ali1688OrderStatus.WAIT_BUYER_RECEIVE || status == Ali1688OrderStatus.CONFIRM_GOODS || status == Ali1688OrderStatus.SUCCESS;
        if (paid) {
            o.setPayTime(created.plusMinutes(35));
        }
        if (shipped) {
            String company = n % 2 == 0 ? "中通快递" : "德邦物流"; // i18n-ignore 演示数据存库
            o.setLogisticsCompany(company);
            o.setLogisticsNo((n % 2 == 0 ? "ZTO" : "DPK") + (7_800_000_000L + po.getId() * 97L));
            o.setShipTime(created.plusDays(1).withHour(16));
            List<Map<String, String>> trace = new ArrayList<>();
            LocalDateTime t = o.getShipTime();
            trace.add(Map.of("time", fmt(t), "remark", "【" + city(first.getSellerLoginId()) + "】" + company + " 已揽收")); // i18n-ignore 演示数据存库
            trace.add(Map.of("time", fmt(t.plusHours(6)), "remark", "【" + city(first.getSellerLoginId()) + "转运中心】已发出，下一站【深圳转运中心】")); // i18n-ignore 演示数据存库
            trace.add(Map.of("time", fmt(t.plusDays(1).plusHours(3)), "remark", "【深圳转运中心】已到达")); // i18n-ignore 演示数据存库
            if (status != Ali1688OrderStatus.WAIT_BUYER_RECEIVE) {
                trace.add(Map.of("time", fmt(t.plusDays(2).withHour(9)), "remark", "【深圳龙岗平湖】派送中，快递员 李师傅 1380013****")); // i18n-ignore 演示数据存库
                trace.add(Map.of("time", fmt(t.plusDays(2).withHour(14)), "remark", "已签收，签收人：仓库前台")); // i18n-ignore 演示数据存库
                o.setReceiveTime(t.plusDays(2).withHour(14));
            }
            o.setTrace(json(trace));
        }
        o.setSyncTime(LocalDateTime.now().minusMinutes(ctx.between(5, 55)));
        orderMapper.insert(o);
        items.forEach(i -> i.setOrderId(o.getId()));
        orderItemMapper.insertBatch(items);
        if (po.getExternalNo() == null || po.getExternalNo().isEmpty()) {
            scmOrderService.updateExternalNo(po.getId(), "1688:" + o.getAliOrderId());
        }
        // 一张被取消后重新下单的记录（同一采购订单）
        if (n == 1) {
            Ecom1688Order c = new Ecom1688Order();
            c.setAccountId(accountId);
            c.setScmOrderId(po.getId());
            c.setAliOrderId(String.valueOf(Long.parseLong(o.getAliOrderId()) - 17));
            c.setSellerLoginId(o.getSellerLoginId());
            c.setSellerName(o.getSellerName());
            c.setStatus(Ali1688OrderStatus.CANCEL);
            c.setAliStatus("cancel");
            c.setProductAmount(amount);
            c.setShippingFee(BigDecimal.valueOf(20));
            c.setTotalAmount(amount.add(BigDecimal.valueOf(20)));
            c.setTotalQty(qty);
            c.setMessage("运费有误，取消后重新下单"); // i18n-ignore 演示数据存库
            c.setAliCreateTime(created.minusHours(1));
            c.setCreateTime(created.minusHours(1));
            c.setSyncTime(created);
            orderMapper.insert(c);
        }
        return true;
    }

    // ---------------------------------------------------------------- 工具

    /** 演示分类：二级分类 key → 编码（按名称查找演示商品生成的分类） */
    private Map<String, String> categoryCodes() {
        Map<String, String> out = new HashMap<>();
        List<WmsMaterialCategory> all = categoryService.list();
        for (String[] sub : DemoCatalog.SUB_CATEGORIES) {
            all.stream().filter(c -> sub[2].equals(c.getName())).findFirst().ifPresent(c -> out.put(sub[0], c.getCode()));
        }
        return out;
    }

    private static String currencyOf(String marketplace) {
        return switch (marketplace) {
            case "US" -> "USD";
            case "UK" -> "GBP";
            case "JP" -> "JPY";
            default -> "EUR";
        };
    }

    /** 卖家（按 SELLERS 顺序）所在城市 */
    private static String city(String sellerLoginId) {
        String[] cities = {"深圳", "东莞", "义乌", "永康", "宁波", "温州"}; // i18n-ignore 演示数据存库
        for (int i = 0; i < SELLERS.length; i++) {
            if (SELLERS[i].equals(sellerLoginId)) {
                return cities[i];
            }
        }
        return cities[0];
    }

    private static String fmt(LocalDateTime t) {
        return t.format(TIME);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String json(Object v) {
        return objectMapper.writeValueAsString(v);
    }
}
