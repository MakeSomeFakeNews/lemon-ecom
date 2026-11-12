package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.entity.EcomBuyerBlacklist;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.mapper.EcomBuyerBlacklistMapper;
import com.lemon.erp.modules.ecom.model.BlacklistType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 买家黑名单：维护（同一匹配方式 + 规范化值唯一）与匹配。规范化：买家 ID、邮箱小写去空白；电话只保留数字（至少 7 位）；
 * 地址小写并去掉空白和标点。匹配：买家 ID / 邮箱相同；电话一方以另一方结尾（兼容有无国家区号）；订单的 地址 + 邮编 包含黑名单地址
 */
@Service
@RequiredArgsConstructor
public class EcomBuyerBlacklistService {

    /** 电话至少比较的位数 */
    static final int PHONE_MIN_DIGITS = 7;
    /** 地址至少的字符数（规范化后），太短容易误命中 */
    static final int ADDRESS_MIN_CHARS = 8;

    private final EcomBuyerBlacklistMapper mapper;
    private final EcomStoreService storeService;

    public PageResult<EcomBuyerBlacklist> page(long current, long size, BlacklistType type, EnableStatus status, String keyword) {
        Page<EcomBuyerBlacklist> page = Page.of(current, size);
        mapper.selectPageList(page, type, status, keyword);
        return PageResult.of(page);
    }

    public EcomBuyerBlacklist get(Long id) {
        EcomBuyerBlacklist b = id == null ? null : mapper.selectById(id);
        if (b == null) {
            throw new BizException("ecom.error.ord.blacklistNotFound");
        }
        return b;
    }

    public Long create(EcomBuyerBlacklist b) {
        b.setId(null);
        normalize(b, null);
        if (b.getStatus() == null) {
            b.setStatus(EnableStatus.ENABLED);
        }
        b.setHitCount(0);
        mapper.insert(b);
        return b.getId();
    }

    public void update(Long id, EcomBuyerBlacklist b) {
        EcomBuyerBlacklist old = get(id);
        normalize(b, id);
        mapper.update(Wrappers.<EcomBuyerBlacklist>lambdaUpdate().eq(EcomBuyerBlacklist::getId, id)
                .set(EcomBuyerBlacklist::getMatchType, b.getMatchType()).set(EcomBuyerBlacklist::getMatchValue, b.getMatchValue())
                .set(EcomBuyerBlacklist::getMatchKey, b.getMatchKey()).set(EcomBuyerBlacklist::getStoreId, b.getStoreId())
                .set(EcomBuyerBlacklist::getReason, b.getReason())
                .set(EcomBuyerBlacklist::getStatus, b.getStatus() == null ? old.getStatus() : b.getStatus()));
    }

    public void delete(List<Long> ids) {
        ids.forEach(this::get);
        mapper.deleteByIds(ids);
    }

    private void normalize(EcomBuyerBlacklist b, Long excludeId) {
        b.setMatchValue(b.getMatchValue().trim());
        b.setReason(StringUtils.hasText(b.getReason()) ? b.getReason().trim() : null);
        String key = key(b.getMatchType(), b.getMatchValue());
        if (key.isEmpty() || b.getMatchType() == BlacklistType.PHONE && key.length() < PHONE_MIN_DIGITS
                || b.getMatchType() == BlacklistType.ADDRESS && key.length() < ADDRESS_MIN_CHARS) {
            throw new BizException("ecom.error.ord.blacklistValueInvalid", b.getMatchValue());
        }
        b.setMatchKey(key.length() <= 255 ? key : key.substring(0, 255));
        if (b.getStoreId() != null) {
            storeService.get(b.getStoreId());
        }
        if (mapper.exists(Wrappers.<EcomBuyerBlacklist>lambdaQuery().eq(EcomBuyerBlacklist::getMatchType, b.getMatchType())
                .eq(EcomBuyerBlacklist::getMatchKey, b.getMatchKey()).ne(excludeId != null, EcomBuyerBlacklist::getId, excludeId))) {
            throw new BizException("ecom.error.ord.blacklistExists", b.getMatchValue());
        }
    }

    /** 规范化后的匹配值 */
    static String key(BlacklistType type, String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case BUYER, EMAIL -> v.replaceAll("\\s+", "");
            case PHONE -> v.replaceAll("\\D", "");
            case ADDRESS -> v.replaceAll("[^\\p{L}\\p{N}]", "");
        };
    }

    /**
     * 订单命中的第一条启用的黑名单（只对店铺生效的条目只匹配该店铺的订单），没有命中为 null；命中时命中次数加 1
     *
     * @param countHit 是否累计命中次数
     */
    public EcomBuyerBlacklist match(EcomOrder o, boolean countHit) {
        List<EcomBuyerBlacklist> list = mapper.selectEnabledForStore(o.getStoreId());
        if (list.isEmpty()) {
            return null;
        }
        String buyer = key(BlacklistType.BUYER, o.getBuyerName());
        String email = key(BlacklistType.EMAIL, o.getBuyerEmail());
        String phone = key(BlacklistType.PHONE, o.getShipPhone());
        String address = key(BlacklistType.ADDRESS, (o.getShipAddress() == null ? "" : o.getShipAddress()) + " " + (o.getShipPostcode() == null ? "" : o.getShipPostcode()));
        for (EcomBuyerBlacklist b : list) {
            String k = b.getMatchKey();
            boolean hit = switch (b.getMatchType()) {
                case BUYER -> !buyer.isEmpty() && buyer.equals(k);
                case EMAIL -> !email.isEmpty() && email.equals(k);
                case PHONE -> phone.length() >= PHONE_MIN_DIGITS && k.length() >= PHONE_MIN_DIGITS && (phone.endsWith(k) || k.endsWith(phone));
                case ADDRESS -> k.length() >= ADDRESS_MIN_CHARS && address.contains(k);
            };
            if (hit) {
                if (countHit) {
                    mapper.hit(b.getId());
                }
                return b;
            }
        }
        return null;
    }

    /** 黑名单条目的说明（挂起原因、详情显示）：匹配方式 + 值 + 原因 */
    public static String describe(EcomBuyerBlacklist b) {
        String type = Messages.get(switch (b.getMatchType()) {
            case BUYER -> "ecom.label.ord.blacklistType.BUYER";
            case EMAIL -> "ecom.label.ord.blacklistType.EMAIL";
            case ADDRESS -> "ecom.label.ord.blacklistType.ADDRESS";
            case PHONE -> "ecom.label.ord.blacklistType.PHONE";
        });
        return Messages.get("ecom.text.ord.blacklistHit", type, b.getMatchValue(), b.getReason() == null ? "-" : b.getReason());
    }
}
