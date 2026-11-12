package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.ecom.demo.DemoImages.Icon;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.StoreAuthStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 演示数据的静态资料：商品（物料）、店铺、供应商、仓库、物流渠道。存库内容用中文，平台侧（标题、买家）用当地语言 / 英文
 */
public final class DemoCatalog {

    private DemoCatalog() {
    }

    // ---------------------------------------------------------------- 物料分类与品牌

    /** 分类：key → 名称（一级分类，下面再分二级） */
    public static final Map<String, String> CATEGORIES = Map.of(
            "3C", "3C 数码配件", "HOME", "家居生活", "OUTDOOR", "户外运动", "PET", "宠物用品"); // i18n-ignore 演示数据存库

    /** 二级分类：key → (上级 key, 名称) */
    public static final List<String[]> SUB_CATEGORIES = List.of(
            new String[]{"3C-POWER", "3C", "充电与电源"}, new String[]{"3C-ACC", "3C", "手机与电脑配件"}, // i18n-ignore 演示数据存库
            new String[]{"HOME-KITCHEN", "HOME", "厨房用品"}, new String[]{"HOME-STORAGE", "HOME", "收纳与家纺"}, // i18n-ignore 演示数据存库
            new String[]{"OUTDOOR-CAMP", "OUTDOOR", "露营装备"}, new String[]{"OUTDOOR-SPORT", "OUTDOOR", "健身运动"}, // i18n-ignore 演示数据存库
            new String[]{"PET-CAT", "PET", "猫用品"}, new String[]{"PET-DOG", "PET", "狗用品"}); // i18n-ignore 演示数据存库

    /** 品牌（业务字典 wms_material_brand）：值 → 名称 */
    public static final Map<String, String> BRANDS = Map.of(
            "VOLTIX", "Voltix", "HOMELUX", "Homelux", "TRAILPRO", "TrailPro", "PAWJOY", "PawJoy");

    // ---------------------------------------------------------------- 商品

    /**
     * 商品（物料）
     *
     * @param code       物料编码（DM- 开头）
     * @param name       中文名称
     * @param en         英文名（平台标题、图片首字母）
     * @param spec       规格（颜色 / 容量等）
     * @param unit       单位
     * @param category   二级分类 key
     * @param brand      品牌值
     * @param cost       采购成本（人民币，含税）
     * @param weight     单件毛重（kg）
     * @param size       包装尺寸（cm）
     * @param usd        美国站参考售价（美元）
     * @param color      图片主色
     * @param icon       图片图标
     * @param popularity 销量权重
     * @param supplier   供应商序号（{@link #SUPPLIERS}）
     */
    public record Product(String code, String name, String en, String spec, String unit, String category, String brand, double cost,
                          double weight, String size, double usd, String color, Icon icon, int popularity, int supplier) {

        public BigDecimal costValue() {
            return BigDecimal.valueOf(cost);
        }
    }

    public static final List<Product> PRODUCTS = List.of(
            // 3C 数码配件（Voltix）
            new Product("DM-PB10", "便携充电宝 10000mAh", "Voltix Slim Power Bank 10000mAh", "黑色 / 10000mAh", "个", "3C-POWER", "VOLTIX", 60.8, 0.24, "14×7×1.6", 25.99, "#2F54EB", Icon.BATTERY, 10, 0), // i18n-ignore 演示数据存库
            new Product("DM-PB20", "快充充电宝 20000mAh", "Voltix PD Power Bank 20000mAh", "白色 / 20000mAh / PD 22.5W", "个", "3C-POWER", "VOLTIX", 99.2, 0.42, "15×7.5×2.8", 39.99, "#597EF7", Icon.BATTERY, 6, 0), // i18n-ignore 演示数据存库
            new Product("DM-CB01", "USB-C 编织快充线 1m", "Voltix Braided USB-C Cable 1m", "灰色 / 1m / 60W", "条", "3C-POWER", "VOLTIX", 8.8, 0.05, "12×8×2", 9.99, "#8C8C8C", Icon.CABLE, 12, 1), // i18n-ignore 演示数据存库
            new Product("DM-CB02", "USB-C 编织快充线 2m", "Voltix Braided USB-C Cable 2m", "灰色 / 2m / 60W", "条", "3C-POWER", "VOLTIX", 11.2, 0.07, "12×8×3", 12.99, "#595959", Icon.CABLE, 8, 1), // i18n-ignore 演示数据存库
            new Product("DM-GAN65", "65W 氮化镓充电器", "Voltix 65W GaN Charger 3-Port", "白色 / 2C1A", "个", "3C-POWER", "VOLTIX", 72, 0.16, "8×6×4", 32.99, "#13C2C2", Icon.PLUG, 8, 0), // i18n-ignore 演示数据存库
            new Product("DM-EB01", "真无线蓝牙耳机", "Voltix Wireless Earbuds Pro", "黑色 / 蓝牙 5.3 / 降噪", "副", "3C-ACC", "VOLTIX", 108.8, 0.12, "10×10×4", 45.99, "#1D39C4", Icon.HEADPHONES, 7, 0), // i18n-ignore 演示数据存库
            new Product("DM-PH01", "车载磁吸手机支架", "Voltix Magnetic Car Phone Mount", "黑色 / 出风口款", "个", "3C-ACC", "VOLTIX", 25.6, 0.09, "10×7×5", 18.99, "#434343", Icon.PHONE, 6, 1), // i18n-ignore 演示数据存库
            new Product("DM-SC01", "iPhone 15 透明防摔手机壳", "Voltix Clear Case for iPhone 15", "透明 / iPhone 15", "个", "3C-ACC", "VOLTIX", 7.2, 0.05, "17×9×1.5", 12.99, "#69C0FF", Icon.PHONE, 7, 1), // i18n-ignore 演示数据存库
            new Product("DM-SC02", "iPhone 15 磨砂黑手机壳", "Voltix Matte Case for iPhone 15", "磨砂黑 / iPhone 15", "个", "3C-ACC", "VOLTIX", 7.7, 0.05, "17×9×1.5", 12.99, "#262626", Icon.PHONE, 5, 1), // i18n-ignore 演示数据存库
            new Product("DM-KB01", "蓝牙折叠键盘", "Voltix Foldable Bluetooth Keyboard", "深灰 / 三设备切换", "个", "3C-ACC", "VOLTIX", 88, 0.21, "16×10×2", 35.99, "#3F6600", Icon.KEYBOARD, 3, 0), // i18n-ignore 演示数据存库
            new Product("DM-RL01", "LED 环形补光灯 10 寸", "Voltix 10\" LED Ring Light with Stand", "白色 / 10 寸 / 三色温", "套", "3C-ACC", "VOLTIX", 44.8, 0.85, "28×28×8", 24.99, "#FA8C16", Icon.LIGHT, 4, 1), // i18n-ignore 演示数据存库
            new Product("DM-TB01", "铝合金平板支架", "Voltix Aluminum Tablet Stand", "银色 / 可折叠", "个", "3C-ACC", "VOLTIX", 30.4, 0.32, "18×12×3", 19.99, "#A0D911", Icon.PHONE, 4, 1), // i18n-ignore 演示数据存库
            // 家居生活（Homelux）
            new Product("DM-HM01", "硅胶厨房铲勺 5 件套", "Homelux Silicone Utensil Set 5 Pcs", "抹茶绿 / 5 件", "套", "HOME-KITCHEN", "HOMELUX", 35.2, 0.48, "35×12×6", 21.99, "#52C41A", Icon.CUP, 6, 2), // i18n-ignore 演示数据存库
            new Product("DM-HM02", "真空压缩收纳袋 8 只装", "Homelux Vacuum Storage Bags 8 Pack", "透明 / 8 只（含手泵）", "套", "HOME-STORAGE", "HOMELUX", 28.8, 0.62, "30×25×5", 19.99, "#36CFC9", Icon.BOX, 6, 2), // i18n-ignore 演示数据存库
            new Product("DM-HM03", "人体感应 LED 小夜灯 2 只装", "Homelux Motion Sensor Night Light 2 Pack", "暖白 / 2 只", "套", "HOME-STORAGE", "HOMELUX", 24, 0.18, "12×8×6", 15.99, "#FADB14", Icon.LIGHT, 7, 2), // i18n-ignore 演示数据存库
            new Product("DM-HM04", "不锈钢保温杯 500ml 曜石黑", "Homelux Insulated Tumbler 500ml Black", "曜石黑 / 500ml", "个", "HOME-KITCHEN", "HOMELUX", 41.6, 0.36, "8×8×24", 22.99, "#1F1F1F", Icon.CUP, 6, 3), // i18n-ignore 演示数据存库
            new Product("DM-HM05", "不锈钢保温杯 500ml 珍珠白", "Homelux Insulated Tumbler 500ml White", "珍珠白 / 500ml", "个", "HOME-KITCHEN", "HOMELUX", 41.6, 0.36, "8×8×24", 22.99, "#D9D9D9", Icon.CUP, 5, 3), // i18n-ignore 演示数据存库
            new Product("DM-HM06", "超声波香薰加湿器 300ml", "Homelux Aroma Diffuser 300ml", "木纹 / 300ml / 七彩灯", "台", "HOME-STORAGE", "HOMELUX", 76.8, 0.55, "16×16×18", 29.99, "#D48806", Icon.DROP, 4, 3), // i18n-ignore 演示数据存库
            new Product("DM-HM07", "抽屉收纳盒 6 件套", "Homelux Drawer Organizer 6 Pcs", "白色 / 6 件", "套", "HOME-STORAGE", "HOMELUX", 32, 0.70, "32×24×7", 18.99, "#ADC6FF", Icon.BOX, 4, 2), // i18n-ignore 演示数据存库
            new Product("DM-HM08", "慢回弹记忆棉枕", "Homelux Memory Foam Pillow", "白色 / 60×35cm", "个", "HOME-STORAGE", "HOMELUX", 88, 1.20, "60×35×12", 39.99, "#B37FEB", Icon.PILLOW, 3, 3), // i18n-ignore 演示数据存库
            new Product("DM-HM09", "硅藻泥浴室防滑垫", "Homelux Diatomite Bath Mat", "浅灰 / 60×39cm", "块", "HOME-STORAGE", "HOMELUX", 30.4, 1.05, "40×30×2", 24.99, "#87E8DE", Icon.MAT, 3, 2), // i18n-ignore 演示数据存库
            new Product("DM-HM10", "电动咖啡磨豆机", "Homelux Electric Coffee Grinder", "黑色 / 不锈钢刀头", "台", "HOME-KITCHEN", "HOMELUX", 115.2, 0.95, "20×10×10", 34.99, "#873800", Icon.CUP, 3, 3), // i18n-ignore 演示数据存库
            new Product("DM-HM11", "硅胶冰格 2 只装", "Homelux Silicone Ice Cube Trays 2 Pack", "蓝色 / 带盖", "套", "HOME-KITCHEN", "HOMELUX", 14.4, 0.30, "22×12×5", 11.99, "#40A9FF", Icon.BOX, 5, 2), // i18n-ignore 演示数据存库
            // 户外运动（TrailPro）
            new Product("DM-OD01", "充电式露营灯", "TrailPro Rechargeable Camping Lantern", "军绿 / 3 档 / 5200mAh", "个", "OUTDOOR-CAMP", "TRAILPRO", 56, 0.38, "12×12×18", 27.99, "#5B8C00", Icon.LANTERN, 5, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD02", "便携折叠露营椅", "TrailPro Folding Camping Chair", "墨绿 / 承重 120kg", "把", "OUTDOOR-CAMP", "TRAILPRO", 108.8, 2.40, "50×15×15", 45.99, "#237804", Icon.CHAIR, 3, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD03", "Tritan 运动水壶 1L", "TrailPro Sports Water Bottle 1L", "雾蓝 / 1L / 带刻度", "个", "OUTDOOR-SPORT", "TRAILPRO", 22.4, 0.22, "9×9×27", 16.99, "#1890FF", Icon.BOTTLE, 6, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD04", "TPE 瑜伽垫 6mm 紫色", "TrailPro TPE Yoga Mat 6mm Purple", "紫色 / 183×61cm", "张", "OUTDOOR-SPORT", "TRAILPRO", 51.2, 1.10, "62×15×15", 29.99, "#722ED1", Icon.MAT, 4, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD05", "TPE 瑜伽垫 6mm 蓝色", "TrailPro TPE Yoga Mat 6mm Blue", "蓝色 / 183×61cm", "张", "OUTDOOR-SPORT", "TRAILPRO", 51.2, 1.10, "62×15×15", 29.99, "#096DD9", Icon.MAT, 3, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD06", "碳纤维登山杖 2 支装", "TrailPro Carbon Trekking Poles 2 Pack", "黑红 / 可伸缩", "副", "OUTDOOR-CAMP", "TRAILPRO", 92.8, 0.55, "68×10×8", 49.99, "#CF1322", Icon.BOTTLE, 2, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD07", "超细纤维速干毛巾", "TrailPro Quick Dry Towel", "湖蓝 / 160×80cm", "条", "OUTDOOR-SPORT", "TRAILPRO", 19.2, 0.20, "20×12×5", 14.99, "#08979C", Icon.MAT, 5, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD08", "USB 充电自行车尾灯", "TrailPro USB Bike Tail Light", "红光 / IPX5", "个", "OUTDOOR-SPORT", "TRAILPRO", 17.6, 0.06, "8×5×4", 12.99, "#F5222D", Icon.LIGHT, 4, 4), // i18n-ignore 演示数据存库
            new Product("DM-OD09", "LED 感应头灯", "TrailPro LED Headlamp with Sensor", "黑色 / 1200 流明", "个", "OUTDOOR-CAMP", "TRAILPRO", 28.8, 0.10, "10×8×6", 17.99, "#FAAD14", Icon.LIGHT, 5, 4), // i18n-ignore 演示数据存库
            // 宠物用品（PawJoy）
            new Product("DM-PT01", "宠物自动饮水机 2L", "PawJoy Pet Water Fountain 2L", "白色 / 2L / 静音泵", "台", "PET-CAT", "PAWJOY", 83.2, 0.80, "20×20×16", 32.99, "#13C2C2", Icon.DROP, 5, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT02", "瓦楞纸猫抓板", "PawJoy Cardboard Cat Scratcher", "原色 / 45×22cm", "块", "PET-CAT", "PAWJOY", 14.4, 0.65, "46×23×6", 13.99, "#D4B106", Icon.BOX, 6, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT03", "伸缩牵引绳 5m", "PawJoy Retractable Dog Leash 5m", "红色 / 5m / 30kg 以下", "个", "PET-DOG", "PAWJOY", 33.6, 0.30, "15×12×4", 19.99, "#F5222D", Icon.PAW, 4, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT04", "宠物脱毛梳", "PawJoy Pet Deshedding Brush", "粉色 / 一键退毛", "把", "PET-CAT", "PAWJOY", 12.8, 0.12, "18×8×3", 11.99, "#EB2F96", Icon.PAW, 6, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT05", "逗猫棒套装 7 件", "PawJoy Cat Teaser Wand Set 7 Pcs", "混色 / 7 件", "套", "PET-CAT", "PAWJOY", 9.6, 0.10, "40×8×3", 9.99, "#F759AB", Icon.HEART, 7, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT06", "狗狗耐咬玩具 3 件套", "PawJoy Dog Chew Toys 3 Pack", "混色 / 天然橡胶", "套", "PET-DOG", "PAWJOY", 20.8, 0.35, "15×12×6", 15.99, "#FA541C", Icon.BONE, 5, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT07", "智能定时喂食器 4L", "PawJoy Automatic Pet Feeder 4L", "白色 / 4L / App 定时", "台", "PET-DOG", "PAWJOY", 156.8, 1.90, "25×20×35", 59.99, "#2F54EB", Icon.BOX, 3, 5), // i18n-ignore 演示数据存库
            new Product("DM-PT08", "圆形宠物窝 M 号", "PawJoy Calming Pet Bed Medium", "浅灰 / 直径 60cm", "个", "PET-DOG", "PAWJOY", 72, 1.30, "40×40×20", 36.99, "#BFBFBF", Icon.PILLOW, 3, 5)); // i18n-ignore 演示数据存库

    /** 物料编码 → 商品 */
    public static Product product(String code) {
        return PRODUCTS.stream().filter(p -> p.code().equals(code)).findFirst().orElseThrow();
    }

    /**
     * 组合商品（在线商品的组成方式为组合）
     *
     * @param sku        平台 SKU 后缀
     * @param en         英文标题
     * @param components 物料编码与用量
     * @param usd        美国站售价
     */
    public record Bundle(String sku, String en, Map<String, Integer> components, double usd) {
    }

    public static final List<Bundle> BUNDLES = List.of(
            new Bundle("KIT-CHARGE", "Voltix Travel Charging Kit (Power Bank + 65W Charger + Cable)", Map.of("DM-PB10", 1, "DM-GAN65", 1, "DM-CB01", 1), 59.99),
            new Bundle("KIT-CAT", "PawJoy Kitten Starter Kit (Scratcher + Brush + Teaser Set)", Map.of("DM-PT02", 1, "DM-PT04", 1, "DM-PT05", 1), 29.99),
            new Bundle("KIT-CAMP", "TrailPro Camp Light Combo (Lantern + Headlamp)", Map.of("DM-OD01", 1, "DM-OD09", 1), 39.99));

    // ---------------------------------------------------------------- 供应商

    /**
     * @param code      编码
     * @param name      名称
     * @param contact   联系人
     * @param phone     电话
     * @param address   地址
     * @param leadDays  交期（天）
     */
    public record Supplier(String code, String name, String contact, String phone, String address, int leadDays) {
    }

    public static final List<Supplier> SUPPLIERS = List.of(
            new Supplier("DEMO-S01", "深圳市沃特电子科技有限公司", "陈工", "0755-26880001", "广东省深圳市宝安区西乡街道", 12), // i18n-ignore 演示数据存库
            new Supplier("DEMO-S02", "东莞市联创线材有限公司", "刘经理", "0769-22990002", "广东省东莞市长安镇", 7), // i18n-ignore 演示数据存库
            new Supplier("DEMO-S03", "义乌市优家日用品有限公司", "王经理", "0579-85550003", "浙江省义乌市北苑街道", 10), // i18n-ignore 演示数据存库
            new Supplier("DEMO-S04", "永康市杯壶电器有限公司", "胡经理", "0579-87110004", "浙江省永康市经济开发区", 15), // i18n-ignore 演示数据存库
            new Supplier("DEMO-S05", "宁波山野户外用品有限公司", "张经理", "0574-88220005", "浙江省宁波市鄞州区", 18), // i18n-ignore 演示数据存库
            new Supplier("DEMO-S06", "温州萌宠宠物用品有限公司", "林经理", "0577-86330006", "浙江省温州市瓯海区", 14)); // i18n-ignore 演示数据存库

    /** 头程货代（物流渠道对应的供应商） */
    public static final Supplier FORWARDER = new Supplier("DEMO-S07", "深圳市环球通国际货运代理有限公司", "黄经理", "0755-83660007", "广东省深圳市福田区", 3); // i18n-ignore 演示数据存库

    // ---------------------------------------------------------------- 仓库

    /**
     * @param code 仓库编码
     * @param name 名称
     * @param defect 是否不良品仓
     */
    public record Warehouse(String code, String name, String address, boolean defect) {
    }

    public static final Warehouse LOCAL_WAREHOUSE = new Warehouse("DEMO-SZ", "深圳国内发货仓", "广东省深圳市龙岗区平湖物流园 B3 栋", false); // i18n-ignore 演示数据存库
    public static final Warehouse DEFECT_WAREHOUSE = new Warehouse("DEMO-DEFECT", "不良品仓", "广东省深圳市龙岗区平湖物流园 B3 栋 1F", true); // i18n-ignore 演示数据存库
    public static final Warehouse FBA_US = new Warehouse("DEMO-FBA-US", "亚马逊 FBA 美国仓", "Amazon FBA (US) · ONT8 / LGB8", false); // i18n-ignore 演示数据存库
    public static final Warehouse FBA_DE = new Warehouse("DEMO-FBA-DE", "亚马逊 FBA 德国仓", "Amazon FBA (DE) · DTM2 / LEJ1", false); // i18n-ignore 演示数据存库
    public static final Warehouse FBA_JP = new Warehouse("DEMO-FBA-JP", "亚马逊 FBA 日本仓", "Amazon FBA (JP) · NRT5", false); // i18n-ignore 演示数据存库
    public static final Warehouse US_3PL = new Warehouse("DEMO-US-3PL", "美国海外仓（洛杉矶）", "Ontario, CA 91761, USA", false); // i18n-ignore 演示数据存库

    public static final List<Warehouse> WAREHOUSES = List.of(LOCAL_WAREHOUSE, DEFECT_WAREHOUSE, FBA_US, FBA_DE, FBA_JP, US_3PL);

    // ---------------------------------------------------------------- 店铺

    /**
     * @param code         店铺编码
     * @param name         名称
     * @param platform     平台
     * @param marketplace  站点
     * @param currency     币种
     * @param timezone     时区
     * @param platformWh   平台仓编码（没有为空）
     * @param platformShare 平台仓配送的订单占比
     * @param share        订单量占比（%）
     * @param fx           售价相对美元的倍数（当地价格水平）
     * @param commission   预估佣金率（%）
     * @param auth         渠道授权状态
     * @param countries    收件国家（加权随机）
     */
    public record Store(String code, String name, EcomPlatform platform, String marketplace, String currency, String timezone,
                        String platformWh, double platformShare, double share, double fx, int commission, StoreAuthStatus auth,
                        List<String> countries) {

        public FulfillmentType mainFulfillment() {
            return platformShare >= 0.5 ? FulfillmentType.PLATFORM : FulfillmentType.SELLER;
        }
    }

    public static final List<Store> STORES = List.of(
            new Store("DEMO-AMZ-US", "Amazon 美国站", EcomPlatform.AMAZON, "US", "USD", "America/Los_Angeles", "DEMO-FBA-US", 0.78, 28, 1, 15, // i18n-ignore 演示数据存库
                    StoreAuthStatus.AUTHORIZED, List.of("US")),
            new Store("DEMO-AMZ-DE", "Amazon 德国站", EcomPlatform.AMAZON, "DE", "EUR", "Europe/Berlin", "DEMO-FBA-DE", 0.75, 12, 0.98, 15, // i18n-ignore 演示数据存库
                    StoreAuthStatus.AUTHORIZED, List.of("DE", "DE", "DE", "AT", "FR")),
            new Store("DEMO-AMZ-JP", "Amazon 日本站", EcomPlatform.AMAZON, "JP", "JPY", "Asia/Tokyo", "DEMO-FBA-JP", 0.72, 8, 152, 12, // i18n-ignore 演示数据存库
                    StoreAuthStatus.REAUTH, List.of("JP")),
            new Store("DEMO-SHP-MY", "Shopee 马来西亚站", EcomPlatform.SHOPEE, "MY", "MYR", "Asia/Kuala_Lumpur", null, 0, 7, 3.9, 9, // i18n-ignore 演示数据存库
                    StoreAuthStatus.NONE, List.of("MY")),
            new Store("DEMO-SHP-SG", "Shopee 新加坡站", EcomPlatform.SHOPEE, "SG", "SGD", "Asia/Singapore", null, 0, 5, 1.32, 9, // i18n-ignore 演示数据存库
                    StoreAuthStatus.REAUTH, List.of("SG")),
            new Store("DEMO-TT-US", "TikTok Shop 美国", EcomPlatform.TIKTOK, "US", "USD", "America/Los_Angeles", "DEMO-US-3PL", 0.55, 12, 0.95, 8, // i18n-ignore 演示数据存库
                    StoreAuthStatus.NONE, List.of("US")),
            new Store("DEMO-TEMU-US", "Temu 美国（半托管）", EcomPlatform.TEMU, "US", "USD", "America/New_York", null, 0, 9, 0.85, 0, // i18n-ignore 演示数据存库
                    StoreAuthStatus.NONE, List.of("US")),
            new Store("DEMO-AE", "速卖通全球店", EcomPlatform.ALIEXPRESS, "GLOBAL", "USD", "Asia/Shanghai", null, 0, 6, 0.9, 8, // i18n-ignore 演示数据存库
                    StoreAuthStatus.AUTHORIZED, List.of("US", "FR", "ES", "PL", "BR", "KR", "IL")),
            new Store("DEMO-SHOPIFY", "Shopify 独立站", EcomPlatform.SHOPIFY, "US", "USD", "America/New_York", null, 0, 7, 1.08, 3, // i18n-ignore 演示数据存库
                    StoreAuthStatus.AUTHORIZED, List.of("US", "US", "CA", "GB", "AU")),
            new Store("DEMO-EBAY-US", "eBay 美国", EcomPlatform.EBAY, "US", "USD", "America/Los_Angeles", null, 0, 6, 1.02, 13, // i18n-ignore 演示数据存库
                    StoreAuthStatus.NONE, List.of("US", "US", "CA")));

    public static Store store(String code) {
        return STORES.stream().filter(s -> s.code().equals(code)).findFirst().orElseThrow();
    }

    /** 没有小数位的币种 */
    public static final String JPY = "JPY";

    /** 币种的金额小数位 */
    public static int scale(String currency) {
        return JPY.equals(currency) ? 0 : 2;
    }

    /** 1 美元约合多少该币种（估算平台费用用） */
    public static double usdFactor(String currency) {
        return switch (currency) {
            case "EUR" -> 0.93;
            case JPY -> 150;
            case "MYR" -> 4.4;
            case "SGD" -> 1.33;
            default -> 1;
        };
    }

    // ---------------------------------------------------------------- 买家

    public static final String[] FIRST_NAMES = {"James", "Mary", "Robert", "Patricia", "John", "Jennifer", "Michael", "Linda", "David", "Emily",
            "Daniel", "Sarah", "Chris", "Jessica", "Matthew", "Ashley", "Kevin", "Amanda", "Brian", "Olivia", "Ethan", "Sophia", "Noah", "Emma"};
    public static final String[] LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Martinez", "Lopez",
            "Wilson", "Anderson", "Taylor", "Thomas", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris", "Clark", "Lewis", "Walker"};
    public static final String[] DE_NAMES = {"Lukas Müller", "Anna Schmidt", "Felix Schneider", "Lea Fischer", "Jonas Weber", "Laura Meyer",
            "Paul Wagner", "Mia Becker", "Leon Hoffmann", "Sophie Schulz", "Tim Koch", "Hannah Richter"};
    public static final String[] JP_NAMES = {"佐藤 健", "鈴木 美咲", "高橋 翔", "田中 陽菜", "伊藤 大輔", "渡辺 結衣", "山本 蓮", "中村 さくら", "小林 拓海", "加藤 葵"}; // i18n-ignore 演示数据存库
    public static final String[] SEA_NAMES = {"Ahmad Faiz", "Nur Aisyah", "Tan Wei Ming", "Lim Mei Ling", "Siti Rahman", "Wong Kar Wai",
            "Lee Jia Hui", "Muhammad Hafiz", "Chua Li Ting", "Goh Jun Jie"};

    /** 国家 → (州 / 地区, 城市, 邮编) 样例 */
    public static final Map<String, String[][]> CITIES = Map.ofEntries(
            Map.entry("US", new String[][]{{"CA", "Los Angeles", "90012"}, {"NY", "New York", "10001"}, {"TX", "Houston", "77002"}, {"FL", "Miami", "33101"},
                    {"WA", "Seattle", "98101"}, {"IL", "Chicago", "60601"}, {"GA", "Atlanta", "30303"}, {"AZ", "Phoenix", "85004"}, {"CO", "Denver", "80202"},
                    {"MA", "Boston", "02108"}, {"NJ", "Newark", "07102"}, {"OH", "Columbus", "43215"}}),
            Map.entry("DE", new String[][]{{"BE", "Berlin", "10115"}, {"BY", "München", "80331"}, {"HH", "Hamburg", "20095"}, {"NW", "Köln", "50667"},
                    {"HE", "Frankfurt am Main", "60311"}, {"BW", "Stuttgart", "70173"}}),
            Map.entry("AT", new String[][]{{"W", "Wien", "1010"}, {"S", "Salzburg", "5020"}}),
            Map.entry("FR", new String[][]{{"IDF", "Paris", "75001"}, {"ARA", "Lyon", "69001"}, {"PAC", "Marseille", "13001"}}),
            Map.entry("JP", new String[][]{{"東京都", "渋谷区", "150-0002"}, {"大阪府", "大阪市", "530-0001"}, {"神奈川県", "横浜市", "220-0011"}, // i18n-ignore 演示数据存库
                    {"愛知県", "名古屋市", "450-0002"}, {"福岡県", "福岡市", "810-0001"}}), // i18n-ignore 演示数据存库
            Map.entry("MY", new String[][]{{"Selangor", "Petaling Jaya", "46000"}, {"Kuala Lumpur", "Kuala Lumpur", "50450"}, {"Penang", "George Town", "10200"},
                    {"Johor", "Johor Bahru", "80000"}}),
            Map.entry("SG", new String[][]{{"Central", "Singapore", "018956"}, {"East", "Singapore", "460001"}, {"West", "Singapore", "608526"}}),
            Map.entry("CA", new String[][]{{"ON", "Toronto", "M5H 2N2"}, {"BC", "Vancouver", "V6B 1A1"}, {"QC", "Montréal", "H2Y 1C6"}}),
            Map.entry("GB", new String[][]{{"ENG", "London", "EC1A 1BB"}, {"ENG", "Manchester", "M1 1AE"}}),
            Map.entry("AU", new String[][]{{"NSW", "Sydney", "2000"}, {"VIC", "Melbourne", "3000"}}),
            Map.entry("ES", new String[][]{{"MD", "Madrid", "28001"}, {"CT", "Barcelona", "08001"}}),
            Map.entry("PL", new String[][]{{"MZ", "Warszawa", "00-001"}, {"MA", "Kraków", "30-001"}}),
            Map.entry("BR", new String[][]{{"SP", "São Paulo", "01000-000"}, {"RJ", "Rio de Janeiro", "20000-000"}}),
            Map.entry("KR", new String[][]{{"Seoul", "Seoul", "04524"}}),
            Map.entry("IL", new String[][]{{"TA", "Tel Aviv", "6100000"}}));
}
