package com.lemon.erp.modules.ecom.connector;

/**
 * 回传发货的结果：状态，以及平台分配 / 确认的运单号与承运商（平台物流托管时由平台生成，框架会回写到订单和还没有运单号的包裹）
 *
 * @param trackingNo 平台返回的运单号，没有为 null
 * @param carrier    平台返回的承运商名称，没有为 null
 * @param message    补充说明（跳过的原因等，写入同步日志），可为 null
 */
public record ShipmentResult(ShipmentResultStatus status, String trackingNo, String carrier, String message) {

    public static ShipmentResult confirmed() {
        return new ShipmentResult(ShipmentResultStatus.CONFIRMED, null, null, null);
    }

    public static ShipmentResult confirmed(String trackingNo, String carrier) {
        return new ShipmentResult(ShipmentResultStatus.CONFIRMED, blankToNull(trackingNo), blankToNull(carrier), null);
    }

    public static ShipmentResult platformManaged(String trackingNo, String carrier) {
        return new ShipmentResult(ShipmentResultStatus.PLATFORM_MANAGED, blankToNull(trackingNo), blankToNull(carrier), null);
    }

    public static ShipmentResult skipped(String message) {
        return new ShipmentResult(ShipmentResultStatus.SKIPPED, null, null, message);
    }

    public ShipmentResult withMessage(String msg) {
        return new ShipmentResult(status, trackingNo, carrier, msg);
    }

    public boolean hasTracking() {
        return trackingNo != null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
