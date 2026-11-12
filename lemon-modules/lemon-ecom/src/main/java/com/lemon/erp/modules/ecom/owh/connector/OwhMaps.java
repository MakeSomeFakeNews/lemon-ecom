package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;

import java.util.Locale;

/**
 * 各服务商返回值归一的公共规则
 */
public final class OwhMaps {

    /** 分页拉取的最大页数（防止服务商返回异常时无限循环） */
    public static final int MAX_PAGES = 200;
    public static final int PAGE_SIZE = 100;

    private OwhMaps() {
    }

    /** 按费用名称 / 代码识别费用类型：仓租、运费、操作、入库，其余为其他 */
    public static OwhFeeType feeType(String raw) {
        if (raw == null) {
            return OwhFeeType.OTHER;
        }
        String s = raw.toLowerCase(Locale.ROOT);
        if (s.contains("仓租") || s.contains("仓储") || s.contains("storage") || s.contains("rent")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            return OwhFeeType.STORAGE;
        }
        if (s.contains("运费") || s.contains("尾程") || s.contains("freight") || s.contains("shipping") || s.contains("delivery") || s.contains("postage")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            return OwhFeeType.FREIGHT;
        }
        if (s.contains("入库") || s.contains("上架") || s.contains("卸") || s.contains("inbound") || s.contains("receiving") || s.contains("putaway")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            return OwhFeeType.INBOUND;
        }
        if (s.contains("操作") || s.contains("出库") || s.contains("拣") || s.contains("打包") || s.contains("handling") || s.contains("operat") // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
                || s.contains("pick") || s.contains("outbound") || s.contains("fulfil")) {
            return OwhFeeType.OPERATION;
        }
        return OwhFeeType.OTHER;
    }

    /** 名：姓名按最后一个空格拆分，没有空格时整体作为名 */
    public static String firstName(String name) {
        String n = name == null ? "" : name.trim();
        int i = n.lastIndexOf(' ');
        return i > 0 ? n.substring(0, i) : n;
    }

    /** 姓 */
    public static String lastName(String name) {
        String n = name == null ? "" : name.trim();
        int i = n.lastIndexOf(' ');
        return i > 0 ? n.substring(i + 1) : "";
    }

    /** 大写后是否等于其中之一 */
    public static boolean in(String raw, String... values) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        for (String v : values) {
            if (s.equals(v)) {
                return true;
            }
        }
        return false;
    }
}
