package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews.AddressIssue;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * 收件地址校验（纯计算，不访问数据库）：
 * <ul>
 *   <li>错误（不通过，审核时进入异常）：国家代码不是 2 位字母；收件人、地址、城市、邮编（不使用邮编的国家除外）缺失；邮编不符合该国格式；
 *       PO Box / 邮政信箱 / Packstation / 军邮（APO、FPO、DPO）；电话含非法字符或位数不对（美国 / 加拿大 10 位或以 1 开头的 11 位，其他 6–15 位）</li>
 *   <li>提示（不影响审核）：偏远地区；美国 / 加拿大 / 澳大利亚 / 巴西 / 墨西哥没有州 / 省；没有电话；地址、城市、邮编都为空
 *       （平台没有返回收件地址，如亚马逊未使用受限数据令牌时，需要在平台后台核对）</li>
 * </ul>
 * 平台配送（FBA 等）的订单不校验
 */
public final class EcomAddressValidator {

    public static final String MISSING_FIELD = "MISSING_FIELD";
    public static final String COUNTRY_INVALID = "COUNTRY_INVALID";
    public static final String POSTCODE_FORMAT = "POSTCODE_FORMAT";
    public static final String PO_BOX = "PO_BOX";
    public static final String PHONE_FORMAT = "PHONE_FORMAT";
    public static final String REMOTE_AREA = "REMOTE_AREA";
    public static final String STATE_MISSING = "STATE_MISSING";
    public static final String PHONE_MISSING = "PHONE_MISSING";
    public static final String ADDRESS_HIDDEN = "ADDRESS_HIDDEN";

    /** 各国邮编格式（去掉首尾空白、转大写后匹配），未列出的国家只检查长度 2–10 位、由字母数字空格横线组成 */
    private static final Map<String, Pattern> POSTCODES = Map.ofEntries(
            Map.entry("US", Pattern.compile("^\\d{5}(-?\\d{4})?$")),
            Map.entry("CA", Pattern.compile("^[A-Z]\\d[A-Z] ?\\d[A-Z]\\d$")),
            Map.entry("GB", Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}$")),
            Map.entry("DE", Pattern.compile("^\\d{5}$")),
            Map.entry("FR", Pattern.compile("^\\d{5}$")),
            Map.entry("IT", Pattern.compile("^\\d{5}$")),
            Map.entry("ES", Pattern.compile("^\\d{5}$")),
            Map.entry("MX", Pattern.compile("^\\d{5}$")),
            Map.entry("FI", Pattern.compile("^\\d{5}$")),
            Map.entry("KR", Pattern.compile("^\\d{5}$")),
            Map.entry("TR", Pattern.compile("^\\d{5}$")),
            Map.entry("SA", Pattern.compile("^\\d{5}(-\\d{4})?$")),
            Map.entry("MY", Pattern.compile("^\\d{5}$")),
            Map.entry("TH", Pattern.compile("^\\d{5}$")),
            Map.entry("ID", Pattern.compile("^\\d{5}$")),
            Map.entry("JP", Pattern.compile("^\\d{3}-?\\d{4}$")),
            Map.entry("AU", Pattern.compile("^\\d{4}$")),
            Map.entry("NZ", Pattern.compile("^\\d{4}$")),
            Map.entry("AT", Pattern.compile("^\\d{4}$")),
            Map.entry("BE", Pattern.compile("^\\d{4}$")),
            Map.entry("CH", Pattern.compile("^\\d{4}$")),
            Map.entry("DK", Pattern.compile("^\\d{4}$")),
            Map.entry("NO", Pattern.compile("^\\d{4}$")),
            Map.entry("HU", Pattern.compile("^\\d{4}$")),
            Map.entry("PH", Pattern.compile("^\\d{4}$")),
            Map.entry("ZA", Pattern.compile("^\\d{4}$")),
            Map.entry("NL", Pattern.compile("^\\d{4} ?[A-Z]{2}$")),
            Map.entry("PL", Pattern.compile("^\\d{2}-?\\d{3}$")),
            Map.entry("PT", Pattern.compile("^\\d{4}-?\\d{3}$")),
            Map.entry("BR", Pattern.compile("^\\d{5}-?\\d{3}$")),
            Map.entry("SE", Pattern.compile("^\\d{3} ?\\d{2}$")),
            Map.entry("CZ", Pattern.compile("^\\d{3} ?\\d{2}$")),
            Map.entry("SK", Pattern.compile("^\\d{3} ?\\d{2}$")),
            Map.entry("IE", Pattern.compile("^[A-Z]\\d[\\dW] ?[A-Z\\d]{4}$")),
            Map.entry("SG", Pattern.compile("^\\d{6}$")),
            Map.entry("CN", Pattern.compile("^\\d{6}$")),
            Map.entry("IN", Pattern.compile("^\\d{6}$")),
            Map.entry("RU", Pattern.compile("^\\d{6}$")),
            Map.entry("IL", Pattern.compile("^\\d{7}$")));
    private static final Pattern POSTCODE_GENERIC = Pattern.compile("^[A-Z\\d][A-Z\\d -]{1,9}$");
    /** 不使用邮编的国家 / 地区（邮编可以为空） */
    private static final Set<String> NO_POSTCODE = Set.of("HK", "MO", "AE", "QA", "IE", "PA", "JM", "BS", "FJ", "GH", "BO", "AO", "ZW", "YE");
    /** 城市可以为空的国家 / 地区（城市国家） */
    private static final Set<String> NO_CITY = Set.of("SG", "HK", "MO", "MC", "VA");
    /** 地址需要州 / 省的国家（缺少时提示） */
    private static final Set<String> STATE_COUNTRIES = Set.of("US", "CA", "AU", "BR", "MX");
    /** 北美电话（10 位，或以 1 开头的 11 位） */
    private static final Set<String> NANP = Set.of("US", "CA");
    private static final Pattern PO_BOX_PATTERN = Pattern.compile(
            "(?i)(\\bp\\.?\\s*o\\.?\\s*box\\b|\\bpost\\s+office\\s+box\\b|\\bpostfach\\b|\\bpackstation\\b|\\bapartado\\s+(postal|de\\s+correos)\\b"
                    + "|\\bbo[iî]te\\s+postale\\b|\\bcasilla\\b|\\b(apo|fpo|dpo)\\b|邮政信箱|私書箱)"); // i18n-ignore 地址关键词
    private static final Pattern PHONE_CHARS = Pattern.compile("^\\+?[\\d\\s().\\-/]+(\\s*(ext|x)\\.?\\s*\\d{1,6})?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");

    private EcomAddressValidator() {
    }

    /**
     * 校验订单的收件地址
     *
     * @param remote 偏远地区判断：(国家, 规范化邮编) → 是否偏远，可为 null
     */
    public static List<AddressIssue> validate(EcomOrder o, BiPredicate<String, String> remote) {
        List<AddressIssue> out = new ArrayList<>();
        String country = upper(o.getShipCountry());
        String postcode = upper(o.getShipPostcode());
        if (!StringUtils.hasText(o.getShipAddress()) && !StringUtils.hasText(o.getShipCity()) && !StringUtils.hasText(postcode)) {
            // 平台没有返回收件地址（只给了国家）：提示到平台后台核对，不拦截
            out.add(warn(ADDRESS_HIDDEN, "ecom.text.ord.addr.hidden"));
            return out;
        }
        if (country == null) {
            out.add(error(MISSING_FIELD, "ecom.text.ord.addr.missing", Messages.get("ecom.label.ord.addr.country")));
        } else if (!COUNTRY.matcher(country).matches()) {
            out.add(error(COUNTRY_INVALID, "ecom.text.ord.addr.countryInvalid", country));
        }
        if (!StringUtils.hasText(o.getShipName())) {
            out.add(error(MISSING_FIELD, "ecom.text.ord.addr.missing", Messages.get("ecom.label.ord.addr.name")));
        }
        if (!StringUtils.hasText(o.getShipAddress())) {
            out.add(error(MISSING_FIELD, "ecom.text.ord.addr.missing", Messages.get("ecom.label.ord.addr.address")));
        }
        if (!StringUtils.hasText(o.getShipCity()) && (country == null || !NO_CITY.contains(country))) {
            out.add(error(MISSING_FIELD, "ecom.text.ord.addr.missing", Messages.get("ecom.label.ord.addr.city")));
        }
        if (postcode == null) {
            if (country == null || !NO_POSTCODE.contains(country)) {
                out.add(error(MISSING_FIELD, "ecom.text.ord.addr.missing", Messages.get("ecom.label.ord.addr.postcode")));
            }
        } else if (!postcodeValid(country, postcode)) {
            out.add(error(POSTCODE_FORMAT, "ecom.text.ord.addr.postcodeFormat", postcode, country == null ? "-" : country));
        }
        String street = (nz(o.getShipAddress()) + " " + nz(o.getShipCity())).trim();
        if (PO_BOX_PATTERN.matcher(street).find()) {
            out.add(error(PO_BOX, "ecom.text.ord.addr.poBox"));
        }
        if (StringUtils.hasText(o.getShipPhone())) {
            if (!phoneValid(country, o.getShipPhone())) {
                out.add(error(PHONE_FORMAT, "ecom.text.ord.addr.phoneFormat", o.getShipPhone().trim()));
            }
        } else {
            out.add(warn(PHONE_MISSING, "ecom.text.ord.addr.phoneMissing"));
        }
        if (country != null && STATE_COUNTRIES.contains(country) && !StringUtils.hasText(o.getShipState())) {
            out.add(warn(STATE_MISSING, "ecom.text.ord.addr.stateMissing", country));
        }
        if (remote != null && country != null && postcode != null && remote.test(country, normPostcode(postcode))) {
            out.add(warn(REMOTE_AREA, "ecom.text.ord.addr.remote", country, postcode));
        }
        return out;
    }

    /** 邮编是否符合国家的格式 */
    public static boolean postcodeValid(String country, String postcode) {
        String pc = upper(postcode);
        if (pc == null) {
            return false;
        }
        Pattern p = country == null ? null : POSTCODES.get(country.toUpperCase(Locale.ROOT));
        return (p != null ? p : POSTCODE_GENERIC).matcher(pc).matches();
    }

    /** 电话是否有效：只含数字、空格、括号、点、横线、斜杠（可带 + 和分机号），位数按国家判断 */
    public static boolean phoneValid(String country, String phone) {
        String raw = phone.trim();
        if (!PHONE_CHARS.matcher(raw).matches()) {
            return false;
        }
        String main = raw.replaceAll("(?i)\\s*(ext|x)\\.?\\s*\\d{1,6}$", "");
        String digits = main.replaceAll("\\D", "");
        if (country != null && NANP.contains(country.toUpperCase(Locale.ROOT)) && !main.startsWith("+")) {
            return digits.length() == 10 || digits.length() == 11 && digits.startsWith("1");
        }
        return digits.length() >= 6 && digits.length() <= 15;
    }

    /** 偏远地区比较用的邮编：去空白、大写 */
    public static String normPostcode(String postcode) {
        return postcode == null ? "" : postcode.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** 是否有错误类问题 */
    public static boolean hasError(List<AddressIssue> issues) {
        return issues.stream().anyMatch(AddressIssue::error);
    }

    private static AddressIssue error(String code, String key, Object... args) {
        return new AddressIssue(code, true, Messages.get(key, args));
    }

    private static AddressIssue warn(String code, String key, Object... args) {
        return new AddressIssue(code, false, Messages.get(key, args));
    }

    private static String upper(String s) {
        return StringUtils.hasText(s) ? s.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
