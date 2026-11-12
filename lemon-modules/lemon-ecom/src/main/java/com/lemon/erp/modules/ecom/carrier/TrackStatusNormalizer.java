package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 物流轨迹状态归一：物流商 / 17TRACK 的状态码和轨迹描述映射到 {@link EcomTrackStatus}，以及轨迹时间的解析。
 * 描述按关键词判断（中英文和德、法、西、意、日的常见说法），顺序：退回 → 异常 → 签收 → 到达待取 / 派送中 → 已揽收 → 仅电子信息（暂无轨迹）→ 其余为运输中
 */
public final class TrackStatusNormalizer {

    private static final Pattern RETURNED = Pattern.compile(
            "return(ed)? to (sender|shipper)|returning to sender|returned|退回|退件|退运|retour|zurück an (den )?absender|devuelto|reso al mittente|返送"); // i18n-ignore 轨迹描述关键词
    private static final Pattern EXCEPTION = Pattern.compile(
            "exception|undeliver|not delivered|delivery (attempt )?failed|failed (delivery|attempt)|unable to deliver|refused|rejected|lost|damaged|"
                    + "customs (hold|held|delay)|held (at|by) customs|address (problem|issue|incorrect|insufficient)|incorrect address|"
                    + "异常|失败|拒收|扣关|丢失|破损|无法投递|地址不详|问题件|zustellung nicht möglich|échec|fallid|non consegn|配達できません"); // i18n-ignore 轨迹描述关键词
    private static final Pattern DELIVERED = Pattern.compile(
            "delivered|签收|妥投|已投递|zugestellt|livré|entregado|consegnato|配達完了|お届け済"); // i18n-ignore 轨迹描述关键词
    private static final Pattern PICKUP = Pattern.compile(
            "out for delivery|available for (pick ?up|collection)|ready for (pick ?up|collection)|awaiting collection|arrived at (the )?(pick ?up|parcel) (point|locker|location)|"
                    + "派送中|派件中|正在派送|待取件|待自提|到达自提|in zustellung|en cours de livraison|en reparto|in consegna|配達中"); // i18n-ignore 轨迹描述关键词
    private static final Pattern PICKED_UP = Pattern.compile(
            "picked up|pickup scan|accepted|received by (the )?carrier|shipment received|origin scan|acceptance|揽收|已收件|收寄|揽件|" // i18n-ignore 轨迹描述关键词
                    + "angenommen|pris en charge|admitido|accettat|引受"); // i18n-ignore 轨迹描述关键词
    private static final Pattern INFO_ONLY = Pattern.compile(
            "label created|shipping label|information received|shipment information|electronic|pre-?advice|awaiting item|电子信息|已下单|预报|等待揽收|未上网"); // i18n-ignore 轨迹描述关键词

    private static final List<DateTimeFormatter> FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"), DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    private static final Pattern EPOCH = Pattern.compile("^\\d{10}(\\d{3})?$");
    private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private TrackStatusNormalizer() {
    }

    /** 按一条轨迹的描述判断状态（没有描述时为运输中） */
    public static EcomTrackStatus fromText(String description) {
        if (!StringUtils.hasText(description)) {
            return EcomTrackStatus.IN_TRANSIT;
        }
        String d = description.toLowerCase(Locale.ROOT);
        if (RETURNED.matcher(d).find()) {
            return EcomTrackStatus.RETURNED;
        }
        if (EXCEPTION.matcher(d).find()) {
            return EcomTrackStatus.EXCEPTION;
        }
        if (DELIVERED.matcher(d).find()) {
            return EcomTrackStatus.DELIVERED;
        }
        if (PICKUP.matcher(d).find()) {
            return EcomTrackStatus.PICKUP;
        }
        if (PICKED_UP.matcher(d).find()) {
            return EcomTrackStatus.PICKED_UP;
        }
        if (INFO_ONLY.matcher(d).find()) {
            return EcomTrackStatus.NOT_FOUND;
        }
        return EcomTrackStatus.IN_TRANSIT;
    }

    /** 整体状态：取时间最新的一条轨迹（时间解析不了时取第一条）的状态；没有轨迹为暂无轨迹 */
    public static EcomTrackStatus overall(List<CarrierTrackEvent> events) {
        if (events == null || events.isEmpty()) {
            return EcomTrackStatus.NOT_FOUND;
        }
        CarrierTrackEvent latest = latest(events);
        return fromText(latest.description());
    }

    /** 时间最新的一条（都解析不了时取第一条） */
    public static CarrierTrackEvent latest(List<CarrierTrackEvent> events) {
        return events.stream().filter(e -> parseTime(e.time()) != null)
                .max(Comparator.comparing(e -> parseTime(e.time()))).orElse(events.get(0));
    }

    /**
     * 17TRACK（v2.2）的主状态 / 子状态：NotFound、InfoReceived → 暂无轨迹；InTransit（子状态 PickedUp 为已揽收）→ 运输中；
     * AvailableForPickup、OutForDelivery → 到达待取；Delivered → 已签收；DeliveryFailure、Exception、Expired → 异常（子状态 Returning / Returned 为退回）
     */
    public static EcomTrackStatus from17Track(String status, String subStatus) {
        String s = status == null ? "" : status;
        String sub = subStatus == null ? "" : subStatus;
        if (sub.startsWith("Exception_Return")) {
            return EcomTrackStatus.RETURNED;
        }
        return switch (s) {
            case "InTransit" -> sub.equals("InTransit_PickedUp") ? EcomTrackStatus.PICKED_UP : EcomTrackStatus.IN_TRANSIT;
            case "AvailableForPickup", "OutForDelivery" -> EcomTrackStatus.PICKUP;
            case "Delivered" -> EcomTrackStatus.DELIVERED;
            case "DeliveryFailure", "Exception", "Expired" -> EcomTrackStatus.EXCEPTION;
            default -> EcomTrackStatus.NOT_FOUND;
        };
    }

    /**
     * 云途 PackageState：1 已提交（暂无轨迹）、2 运输中、3 已签收、4 已收货（揽收）、5 订单取消（异常）、6 投递失败（异常）、7 已退回；其他（0 未知）为 null，按轨迹描述判断
     */
    public static EcomTrackStatus fromYunExpress(String packageState) {
        if (packageState == null) {
            return null;
        }
        return switch (packageState.trim()) {
            case "1" -> EcomTrackStatus.NOT_FOUND;
            case "2" -> EcomTrackStatus.IN_TRANSIT;
            case "3" -> EcomTrackStatus.DELIVERED;
            case "4" -> EcomTrackStatus.PICKED_UP;
            case "5", "6" -> EcomTrackStatus.EXCEPTION;
            case "7" -> EcomTrackStatus.RETURNED;
            default -> null;
        };
    }

    /**
     * 解析轨迹时间：常见的日期时间格式、带时区的 ISO 8601（换算为服务器时区）、10 / 13 位时间戳；解析不了返回 null
     */
    public static LocalDateTime parseTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String t = text.trim();
        if (EPOCH.matcher(t).matches()) {
            long v = Long.parseLong(t);
            return LocalDateTime.ofInstant(t.length() == 10 ? Instant.ofEpochSecond(v) : Instant.ofEpochMilli(v), ZoneId.systemDefault());
        }
        if (DATE_ONLY.matcher(t).matches()) {
            return java.time.LocalDate.parse(t).atStartOfDay();
        }
        try {
            return OffsetDateTime.parse(t).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 不带时区，按本地格式解析
        }
        for (DateTimeFormatter f : FORMATS) {
            try {
                return LocalDateTime.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }
}
