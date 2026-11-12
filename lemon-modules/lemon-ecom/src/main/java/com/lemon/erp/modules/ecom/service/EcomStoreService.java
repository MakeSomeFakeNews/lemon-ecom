package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomStoreMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.fin.service.FinCurrencyService;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 平台店铺：新建时自动建立对应的客户（应收挂在它下面），渠道授权加密保存、只写不读
 */
@Service
@RequiredArgsConstructor
public class EcomStoreService {

    /** 店铺对应客户的编码前缀 */
    static final String CUSTOMER_CODE_PREFIX = "ST-";
    private static final int DEFAULT_LEAD_DAYS = 30;
    private static final int DEFAULT_SAFETY_DAYS = 15;
    private static final BigDecimal DEFAULT_COMMISSION = BigDecimal.valueOf(15);

    private final EcomStoreMapper storeMapper;
    private final EcomListingMapper listingMapper;
    private final EcomOrderMapper orderMapper;
    private final ScmCustomerService customerService;
    private final WmsWarehouseService warehouseService;
    private final FinCurrencyService currencyService;
    private final DictLookup dictLookup;
    private final CredentialCipher cipher;
    private final ConnectorRegistry connectorRegistry;

    public PageResult<EcomStore> page(long current, long size, String keyword, EcomPlatform platform, EnableStatus status) {
        Page<EcomStore> page = Page.of(current, size);
        storeMapper.selectPageList(page, keyword, platform, status);
        return PageResult.of(page);
    }

    public EcomStore detail(Long id) {
        EcomStore s = storeMapper.selectDetail(id);
        if (s == null) {
            throw new BizException("ecom.error.storeNotFound");
        }
        s.setCredentials(null);
        return s;
    }

    public EcomStore get(Long id) {
        EcomStore s = id == null ? null : storeMapper.selectById(id);
        if (s == null) {
            throw new BizException("ecom.error.storeNotFound");
        }
        return s;
    }

    /** 启用的店铺，停用的不能再导入订单、新建货件 */
    public EcomStore requireEnabled(Long id) {
        EcomStore s = get(id);
        if (s.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("ecom.error.storeDisabled", s.getName());
        }
        return s;
    }

    public EcomStore findByCode(String code) {
        return code == null ? null : storeMapper.selectOne(Wrappers.<EcomStore>lambdaQuery().eq(EcomStore::getCode, code.trim()));
    }

    /** 选择店铺的下拉：启用的店铺 */
    public List<EcomStore> options() {
        return storeMapper.selectList(Wrappers.<EcomStore>lambdaQuery()
                .select(EcomStore::getId, EcomStore::getCode, EcomStore::getName, EcomStore::getPlatform, EcomStore::getMarketplace,
                        EcomStore::getCurrency, EcomStore::getWarehouseId, EcomStore::getPlatformWarehouseId, EcomStore::getCustomerId)
                .eq(EcomStore::getStatus, EnableStatus.ENABLED).orderByAsc(EcomStore::getId));
    }

    /** 有渠道授权、启用的店铺（定时同步） */
    public List<EcomStore> syncable() {
        return storeMapper.selectList(Wrappers.<EcomStore>lambdaQuery().eq(EcomStore::getStatus, EnableStatus.ENABLED)
                .isNotNull(EcomStore::getCredentials).ne(EcomStore::getCredentials, "").ne(EcomStore::getDemo, 1).orderByAsc(EcomStore::getId));
    }

    /** 开启了自动同步订单、有授权的启用店铺（定时任务） */
    public List<EcomStore> orderSyncable() {
        return syncable().stream().filter(s -> !Integer.valueOf(0).equals(s.getOrderSync())).toList();
    }

    @Transactional
    public Long create(EcomStore s) {
        s.setId(null);
        normalize(s);
        // 编码唯一性要看全部店铺（不受店铺数据权限限制）
        if (EcomStoreScope.unrestricted(() -> storeMapper.exists(Wrappers.<EcomStore>lambdaQuery().eq(EcomStore::getCode, s.getCode())))) {
            throw new BizException("ecom.error.storeCodeExists", s.getCode());
        }
        s.setCustomerId(ensureCustomer(s));
        s.setCredentials(cipher.encrypt(s.getCredentials()));
        if (s.getStatus() == null) s.setStatus(EnableStatus.ENABLED);
        storeMapper.insert(s);
        return s.getId();
    }

    /** 修改：授权信息留空表示不修改；店铺改名时同步客户名称 */
    @Transactional
    public void update(Long id, EcomStore s) {
        EcomStore old = get(id);
        normalize(s);
        if (EcomStoreScope.unrestricted(() -> storeMapper.exists(Wrappers.<EcomStore>lambdaQuery().eq(EcomStore::getCode, s.getCode()).ne(EcomStore::getId, id)))) {
            throw new BizException("ecom.error.storeCodeExists", s.getCode());
        }
        s.setId(id);
        s.setCustomerId(old.getCustomerId());
        s.setStatus(null);
        s.setDemo(null);
        s.setCredentials(StringUtils.hasText(s.getCredentials()) ? cipher.encrypt(mergeCredentials(old, s.getCredentials())) : null);
        storeMapper.updateById(s);
        if (!StringUtils.hasText(s.getApiBaseUrl()) && StringUtils.hasText(old.getApiBaseUrl())) {
            storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getApiBaseUrl, null).eq(EcomStore::getId, id));
        }
        if (s.getPlatformWarehouseId() == null && old.getPlatformWarehouseId() != null) {
            storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getPlatformWarehouseId, null).eq(EcomStore::getId, id));
        }
        if (!Objects.equals(old.getName(), s.getName()) && old.getCustomerId() != null) {
            ScmCustomer c = customerService.get(old.getCustomerId());
            c.setName(s.getName());
            customerService.update(c.getId(), c);
        }
    }

    /** 清除渠道授权：之后只能手工导入 */
    public void clearCredentials(Long id) {
        get(id);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getCredentials, null).eq(EcomStore::getId, id));
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getStatus, status).eq(EcomStore::getId, id));
    }

    /** 已有在线商品或订单的店铺不能删除，可以停用 */
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomStore s = get(id);
            if (listingMapper.exists(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, id))
                    || orderMapper.exists(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, id))) {
                throw new BizException("ecom.error.storeHasData", s.getName());
            }
        }
        storeMapper.deleteByIds(ids);
    }

    /** 解密后的渠道授权，没有配置时为空 */
    public StoreCredentials credentials(EcomStore s) {
        return StoreCredentials.parse(cipher.decrypt(s.getCredentials()));
    }

    /**
     * 修改店铺时提交的授权 JSON 与已保存的合并：提交的键覆盖原值，值为空串的键删除，没提交的键（如 OAuth 换来的令牌）保留，
     * 连接器缓存的字段（CredentialField.cached）始终保留
     */
    private String mergeCredentials(EcomStore old, String submitted) {
        java.util.Set<String> cached = connectorRegistry.find(old.getPlatform()).map(c -> c.info().cachedKeys()).orElse(java.util.Set.of());
        Map<String, String> merged = StoreCredentials.merge(credentials(old).values(), StoreCredentials.parse(submitted).values(), cached);
        return merged.isEmpty() ? null : HttpJsonHolder.write(merged);
    }

    /** 调用渠道接口的上下文：解密授权信息，刷新令牌后由框架加密写回 */
    public ConnectorContext context(EcomStore s) {
        requireReal(s);
        return new ConnectorContext(s, credentials(s), Integer.valueOf(1).equals(s.getSandbox()), s.getApiBaseUrl(),
                c -> saveCredentials(s.getId(), c.values()));
    }

    /**
     * 调用渠道接口的上下文，刷新令牌保存后另外通知 onSaved（令牌维护任务据此判断是否刷新过）
     */
    public ConnectorContext context(EcomStore s, Runnable onSaved) {
        requireReal(s);
        return new ConnectorContext(s, credentials(s), Integer.valueOf(1).equals(s.getSandbox()), s.getApiBaseUrl(), c -> {
            saveCredentials(s.getId(), c.values());
            onSaved.run();
        });
    }

    /** 演示店铺不调用平台接口 */
    public static void requireReal(EcomStore s) {
        if (s != null && s.demoStore()) {
            throw new BizException("ecom.error.demoStoreNoApi", s.getName());
        }
    }

    /** 全部店铺（含停用的，渠道接入总览），含加密的授权信息，只在服务端使用 */
    public List<EcomStore> all() {
        return storeMapper.selectList(Wrappers.<EcomStore>lambdaQuery().orderByAsc(EcomStore::getId));
    }

    /** 按 ID 取店铺完整记录（含加密的授权信息），只在服务端使用 */
    public List<EcomStore> byIds(java.util.Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : storeMapper.selectByIds(ids);
    }

    /** 保存授权信息（加密），连接器刷新令牌、OAuth 回调时调用 */
    public void saveCredentials(Long id, Map<String, String> values) {
        String json = values == null || values.isEmpty() ? null : HttpJsonHolder.write(values);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getCredentials, cipher.encrypt(json)).eq(EcomStore::getId, id));
    }

    /** OAuth 授权成功：保存令牌、卖家 ID 和授权时间 */
    public void markAuthorized(Long id, Map<String, String> values, String sellerId) {
        saveCredentials(id, values);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getAuthorizedTime, LocalDateTime.now())
                .set(sellerId != null, EcomStore::getSellerId, sellerId).eq(EcomStore::getId, id));
    }

    /** 记录最近一次同步失败的原因（成功时清空） */
    public void markSyncMessage(Long id, String message) {
        String m = message == null || message.length() <= 500 ? message : message.substring(0, 500);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getLastSyncMessage, m).eq(EcomStore::getId, id));
    }

    /** 开启了自动回传库存、有授权的启用店铺 */
    public List<EcomStore> inventorySyncable() {
        return syncable().stream().filter(s -> Integer.valueOf(1).equals(s.getInventorySync())).toList();
    }

    /** JSON 序列化（避免本类直接依赖 Jackson 的版本差异） */
    private static final class HttpJsonHolder {
        static String write(Map<String, String> values) {
            return com.lemon.erp.modules.ecom.connector.HttpJson.MAPPER.writeValueAsString(new java.util.TreeMap<>(values));
        }
    }

    public void markSynced(Long id, LocalDateTime time) {
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getLastSyncTime, time).eq(EcomStore::getId, id));
    }

    private void normalize(EcomStore s) {
        s.setCode(s.getCode().trim().toUpperCase());
        s.setName(s.getName().trim());
        s.setCurrency(s.getCurrency().trim().toUpperCase());
        dictLookup.check(EcomConstants.MARKETPLACE_DICT, s.getMarketplace(), "ecom.label.marketplace");
        if (currencyService.currencies().stream().noneMatch(c -> c.getCode().equals(s.getCurrency()))) {
            throw new BizException("ecom.error.currencyNotFound", s.getCurrency());
        }
        WmsWarehouse w = warehouseService.get(s.getWarehouseId());
        if (s.getPlatformWarehouseId() != null) {
            if (s.getPlatformWarehouseId().equals(w.getId())) {
                throw new BizException("ecom.error.platformWarehouseSameAsDefault");
            }
            warehouseService.get(s.getPlatformWarehouseId());
        }
        if (s.getCommissionRate() == null) s.setCommissionRate(DEFAULT_COMMISSION);
        if (s.getLeadDays() == null) s.setLeadDays(DEFAULT_LEAD_DAYS);
        if (s.getSafetyDays() == null) s.setSafetyDays(DEFAULT_SAFETY_DAYS);
        if (s.getSandbox() == null) s.setSandbox(0);
        if (s.getOrderSync() == null) s.setOrderSync(1);
        if (s.getInventorySync() == null) s.setInventorySync(0);
        if (s.getInventoryBuffer() == null) s.setInventoryBuffer(BigDecimal.ZERO);
        if (StringUtils.hasText(s.getApiBaseUrl()) && !s.getApiBaseUrl().trim().matches("(?i)^https?://.+")) {
            throw new BizException("ecom.error.apiBaseUrlInvalid");
        }
        if (StringUtils.hasText(s.getCredentials())) {
            StoreCredentials.parse(s.getCredentials());
        }
    }

    /** 店铺对应的客户：按编码 ST-店铺编码 查找，没有时新建 */
    private Long ensureCustomer(EcomStore s) {
        String code = CUSTOMER_CODE_PREFIX + s.getCode();
        if (code.length() > 32) {
            code = code.substring(0, 32);
        }
        ScmCustomer existing = customerService.findByCode(code);
        if (existing != null) {
            return existing.getId();
        }
        ScmCustomer c = new ScmCustomer();
        c.setCode(code);
        c.setName(s.getName());
        c.setRemark(com.lemon.erp.common.i18n.Messages.get("ecom.text.storeCustomerRemark", s.getPlatform().name(), s.getMarketplace()));
        c.setStatus(EnableStatus.ENABLED);
        customerService.create(c);
        return c.getId();
    }
}
