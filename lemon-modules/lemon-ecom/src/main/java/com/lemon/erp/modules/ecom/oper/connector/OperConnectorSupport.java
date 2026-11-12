package com.lemon.erp.modules.ecom.oper.connector;

import com.lemon.erp.modules.ecom.connector.ConnectorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 运营连接器的公共工具
 */
final class OperConnectorSupport {

    private OperConnectorSupport() {
    }

    /** 接口根地址：店铺填写了自定义地址时用它，否则按沙箱开关取默认地址 */
    static String baseUrl(ConnectorContext ctx, String production, String sandbox) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        return ctx.sandbox() && sandbox != null ? sandbox : production;
    }

    /** 本地时间 → Unix 秒 */
    static long seconds(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /** 金额字符串（两位小数） */
    static String money(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
