package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.entity.EcomRemoteArea;
import com.lemon.erp.modules.ecom.mapper.EcomRemoteAreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 偏远地区邮编段：维护与判断。邮编去空白、大写后按字符串比较：在 [起, 止] 之间，或以起 / 止开头即为偏远地区
 * （起止相同即前缀匹配，如英国 HS、ZE）
 */
@Service
@RequiredArgsConstructor
public class EcomRemoteAreaService {

    private final EcomRemoteAreaMapper mapper;

    public PageResult<EcomRemoteArea> page(long current, long size, String country, String keyword) {
        Page<EcomRemoteArea> page = mapper.selectPage(Page.of(current, size), Wrappers.<EcomRemoteArea>lambdaQuery()
                .eq(StringUtils.hasText(country), EcomRemoteArea::getCountry, country == null ? null : country.trim().toUpperCase(Locale.ROOT))
                .and(StringUtils.hasText(keyword), w -> w.like(EcomRemoteArea::getName, keyword).or().like(EcomRemoteArea::getPostcodeFrom, keyword))
                .orderByAsc(EcomRemoteArea::getCountry).orderByAsc(EcomRemoteArea::getPostcodeFrom));
        return PageResult.of(page);
    }

    public EcomRemoteArea get(Long id) {
        EcomRemoteArea a = id == null ? null : mapper.selectById(id);
        if (a == null) {
            throw new BizException("ecom.error.ord.remoteAreaNotFound");
        }
        return a;
    }

    public Long create(EcomRemoteArea a) {
        a.setId(null);
        normalize(a, null);
        if (a.getStatus() == null) {
            a.setStatus(EnableStatus.ENABLED);
        }
        mapper.insert(a);
        return a.getId();
    }

    public void update(Long id, EcomRemoteArea a) {
        EcomRemoteArea old = get(id);
        normalize(a, id);
        mapper.update(Wrappers.<EcomRemoteArea>lambdaUpdate().eq(EcomRemoteArea::getId, id)
                .set(EcomRemoteArea::getCountry, a.getCountry()).set(EcomRemoteArea::getPostcodeFrom, a.getPostcodeFrom())
                .set(EcomRemoteArea::getPostcodeTo, a.getPostcodeTo()).set(EcomRemoteArea::getName, a.getName())
                .set(EcomRemoteArea::getRemark, a.getRemark()).set(EcomRemoteArea::getStatus, a.getStatus() == null ? old.getStatus() : a.getStatus()));
    }

    public void delete(List<Long> ids) {
        ids.forEach(this::get);
        mapper.deleteByIds(ids);
    }

    private void normalize(EcomRemoteArea a, Long excludeId) {
        a.setCountry(a.getCountry().trim().toUpperCase(Locale.ROOT));
        a.setPostcodeFrom(EcomAddressValidator.normPostcode(a.getPostcodeFrom()));
        a.setPostcodeTo(StringUtils.hasText(a.getPostcodeTo()) ? EcomAddressValidator.normPostcode(a.getPostcodeTo()) : a.getPostcodeFrom());
        if (a.getPostcodeFrom().compareTo(a.getPostcodeTo()) > 0) {
            throw new BizException("ecom.error.ord.remoteRangeInvalid");
        }
        a.setName(StringUtils.hasText(a.getName()) ? a.getName().trim() : null);
        if (mapper.exists(Wrappers.<EcomRemoteArea>lambdaQuery().eq(EcomRemoteArea::getCountry, a.getCountry())
                .eq(EcomRemoteArea::getPostcodeFrom, a.getPostcodeFrom()).eq(EcomRemoteArea::getPostcodeTo, a.getPostcodeTo())
                .ne(excludeId != null, EcomRemoteArea::getId, excludeId))) {
            throw new BizException("ecom.error.ord.remoteAreaExists", a.getCountry(), a.getPostcodeFrom());
        }
    }

    /** 国家的邮编（已规范化）是否在启用的偏远地区邮编段中 */
    public boolean isRemote(String country, String postcode) {
        if (!StringUtils.hasText(country) || !StringUtils.hasText(postcode)) {
            return false;
        }
        List<EcomRemoteArea> areas = mapper.selectList(Wrappers.<EcomRemoteArea>lambdaQuery().eq(EcomRemoteArea::getCountry, country.toUpperCase(Locale.ROOT))
                .eq(EcomRemoteArea::getStatus, EnableStatus.ENABLED));
        return areas.stream().anyMatch(a -> inRange(postcode, a.getPostcodeFrom(), a.getPostcodeTo()));
    }

    static boolean inRange(String pc, String from, String to) {
        return pc.startsWith(from) || pc.startsWith(to) || pc.compareTo(from) >= 0 && pc.compareTo(to) <= 0;
    }
}
