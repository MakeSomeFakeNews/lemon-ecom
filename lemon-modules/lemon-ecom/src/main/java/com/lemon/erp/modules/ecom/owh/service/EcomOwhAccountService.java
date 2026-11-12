package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.carrier.CarrierField;
import com.lemon.erp.modules.ecom.owh.connector.OwhConnector;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.connector.OwhInfo;
import com.lemon.erp.modules.ecom.owh.connector.OwhRegistry;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.MappingRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.ProductChannelRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.FieldView;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.ProviderView;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhProduct;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhAccountMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhOutboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhProductMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhWarehouseMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhCapability;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 海外仓账号、仓库映射与物流产品：账号授权加密保存（只写不读）；映射把系统仓库关联到服务商的仓库代码（一个系统仓库只能映射一次），
 * 映射了的仓库即海外仓；物流产品从服务商拉取保存，可对应一条物流渠道（审单规则指定渠道时推送用对应的产品）。
 * 服务商接口在事务之外调用
 */
@Service
@RequiredArgsConstructor
public class EcomOwhAccountService {

    private final EcomOwhAccountMapper accountMapper;
    private final EcomOwhWarehouseMapper mappingMapper;
    private final EcomOwhProductMapper productMapper;
    private final EcomOwhOutboundMapper outboundMapper;
    private final EcomOwhInboundMapper inboundMapper;
    private final EcomOwhSupport support;
    private final OwhRegistry registry;
    private final WmsWarehouseService warehouseService;
    private final EcomLogisticsService logisticsService;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 账号

    public PageResult<EcomOwhAccount> page(long current, long size, String keyword, OwhProvider provider, EnableStatus status) {
        Page<EcomOwhAccount> page = Page.of(current, size);
        accountMapper.selectPageList(page, keyword, provider, status);
        return PageResult.of(page);
    }

    public EcomOwhAccount get(Long id) {
        EcomOwhAccount a = id == null ? null : accountMapper.selectById(id);
        if (a == null) {
            throw new BizException("ecom.error.owh.accountNotFound");
        }
        return a;
    }

    /** 详情（不含授权，只返回是否已保存） */
    public EcomOwhAccount detail(Long id) {
        EcomOwhAccount a = get(id);
        a.setHasCredentials(StringUtils.hasText(a.getCredentials()));
        a.setWarehouseCount(mappingMapper.selectMappings(id).size());
        return a;
    }

    /** 启用且开启自动同步的账号 ID（定时任务只处理这些账号的单据） */
    public Set<Long> autoSyncAccountIds() {
        Set<Long> out = new HashSet<>();
        accountMapper.selectList(Wrappers.<EcomOwhAccount>lambdaQuery().select(EcomOwhAccount::getId).eq(EcomOwhAccount::getStatus, EnableStatus.ENABLED)
                .eq(EcomOwhAccount::getAutoSync, 1)).forEach(a -> out.add(a.getId()));
        return out;
    }

    /** 启用的账号（选择用） */
    public List<EcomOwhAccount> options() {
        return accountMapper.selectList(Wrappers.<EcomOwhAccount>lambdaQuery()
                .select(EcomOwhAccount::getId, EcomOwhAccount::getCode, EcomOwhAccount::getName, EcomOwhAccount::getProvider)
                .eq(EcomOwhAccount::getStatus, EnableStatus.ENABLED).orderByAsc(EcomOwhAccount::getId));
    }

    public Long create(EcomOwhAccount a) {
        normalize(a);
        if (accountMapper.exists(Wrappers.<EcomOwhAccount>lambdaQuery().eq(EcomOwhAccount::getCode, a.getCode()))) {
            throw new BizException("ecom.error.owh.accountCodeExists", a.getCode());
        }
        a.setId(null);
        a.setCredentials(support.merge(null, a.getProvider(), a.getAuth()));
        if (a.getStatus() == null) {
            a.setStatus(EnableStatus.ENABLED);
        }
        a.setAutoSync(a.getAutoSync() != null && a.getAutoSync() == 0 ? 0 : 1);
        accountMapper.insert(a);
        return a.getId();
    }

    @Transactional
    public void update(Long id, EcomOwhAccount a) {
        EcomOwhAccount old = get(id);
        normalize(a);
        if (accountMapper.exists(Wrappers.<EcomOwhAccount>lambdaQuery().eq(EcomOwhAccount::getCode, a.getCode()).ne(EcomOwhAccount::getId, id))) {
            throw new BizException("ecom.error.owh.accountCodeExists", a.getCode());
        }
        if (old.getProvider() != a.getProvider() && hasDocuments(id)) {
            throw new BizException("ecom.error.owh.providerLocked", old.getName());
        }
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate()
                .set(EcomOwhAccount::getCode, a.getCode()).set(EcomOwhAccount::getName, a.getName()).set(EcomOwhAccount::getProvider, a.getProvider())
                .set(EcomOwhAccount::getCredentials, support.merge(old, a.getProvider(), a.getAuth()))
                .set(EcomOwhAccount::getSandbox, a.getSandbox()).set(EcomOwhAccount::getApiBaseUrl, a.getApiBaseUrl())
                .set(EcomOwhAccount::getCurrency, a.getCurrency()).set(EcomOwhAccount::getRemark, a.getRemark())
                .set(a.getAutoSync() != null, EcomOwhAccount::getAutoSync, a.getAutoSync() != null && a.getAutoSync() == 0 ? 0 : 1)
                .eq(EcomOwhAccount::getId, id));
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getStatus, status).eq(EcomOwhAccount::getId, id));
    }

    /** 清除授权 */
    public void clearCredentials(Long id) {
        get(id);
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getCredentials, null).eq(EcomOwhAccount::getId, id));
    }

    /** 删除：有入库单、出库单的账号不能删除（只能停用），映射与物流产品一并删除 */
    @Transactional
    public void delete(Long id) {
        EcomOwhAccount a = get(id);
        if (hasDocuments(id)) {
            throw new BizException("ecom.error.owh.accountHasData", a.getName());
        }
        mappingMapper.delete(Wrappers.<EcomOwhWarehouse>lambdaQuery().eq(EcomOwhWarehouse::getAccountId, id));
        productMapper.delete(Wrappers.<EcomOwhProduct>lambdaQuery().eq(EcomOwhProduct::getAccountId, id));
        accountMapper.deleteById(id);
    }

    private boolean hasDocuments(Long accountId) {
        return outboundMapper.exists(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getAccountId, accountId))
                || inboundMapper.exists(Wrappers.<EcomOwhInbound>lambdaQuery().eq(EcomOwhInbound::getAccountId, accountId));
    }

    private static void normalize(EcomOwhAccount a) {
        a.setCode(a.getCode().trim());
        a.setName(a.getName().trim());
        a.setSandbox(a.getSandbox() != null && a.getSandbox() == 1 ? 1 : 0);
        a.setApiBaseUrl(StringUtils.hasText(a.getApiBaseUrl()) ? a.getApiBaseUrl().trim() : null);
        if (a.getApiBaseUrl() != null && !(a.getApiBaseUrl().startsWith("http://") || a.getApiBaseUrl().startsWith("https://"))) {
            throw new BizException("ecom.error.owh.urlInvalid", a.getName());
        }
        a.setCurrency(StringUtils.hasText(a.getCurrency()) ? a.getCurrency().trim().toUpperCase(Locale.ROOT) : null);
    }

    /** 可对接的服务商：授权字段、地址、能力 */
    public List<ProviderView> providers() {
        List<ProviderView> out = new ArrayList<>();
        for (OwhConnector c : registry.all()) {
            OwhInfo i = c.info();
            List<FieldView> fields = new ArrayList<>();
            for (CarrierField f : i.fields()) {
                fields.add(new FieldView(f.key(), Messages.get(f.labelKey()), f.secret(), f.required()));
            }
            out.add(new ProviderView(i.provider().name(), Messages.get(i.nameKey()), fields, i.productionUrl(), i.sandboxUrl(),
                    i.capabilities().stream().map(OwhCapability::name).sorted().toList(), i.docUrl()));
        }
        return out;
    }

    /** 测试授权（拉取仓库列表），失败时记录原因 */
    public int test(Long id) {
        EcomOwhAccount a = get(id);
        try {
            List<OwhData.Warehouse> list = support.connector(a.getProvider()).warehouses(support.context(a));
            recordError(id, null);
            return list.size();
        } catch (BizException e) {
            recordError(id, e.getMessage());
            throw e;
        }
    }

    /** 服务商的仓库列表（实时拉取，映射时选择） */
    public List<OwhData.Warehouse> remoteWarehouses(Long id) {
        EcomOwhAccount a = get(id);
        return support.connector(a.getProvider()).warehouses(support.context(a));
    }

    void recordError(Long accountId, String message) {
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getLastError, EcomOwhSupport.abbreviate(message))
                .eq(EcomOwhAccount::getId, accountId));
    }

    // ---------------------------------------------------------------- 物流产品

    public List<EcomOwhProduct> products(Long accountId) {
        get(accountId);
        return productMapper.selectByAccount(accountId);
    }

    /**
     * 从服务商拉取物流产品：按每个映射的仓库拉取（没有映射时拉取全部），新的插入、已有的更新名称并启用，服务商列表中已没有的停用；
     * 已设置的物流渠道对应保留
     *
     * @return 拉取到的产品数
     */
    public int syncProducts(Long accountId) {
        EcomOwhAccount a = get(accountId);
        OwhConnector c = support.connector(a.getProvider());
        var ctx = support.context(a);
        List<EcomOwhWarehouse> mappings = mappingMapper.selectMappings(accountId);
        Map<String, OwhData.Product> fetched = new LinkedHashMap<>();
        if (mappings.isEmpty()) {
            c.products(ctx, null).forEach(p -> fetched.putIfAbsent(p.code(), p));
        } else {
            Set<String> codes = new HashSet<>();
            for (EcomOwhWarehouse m : mappings) {
                if (codes.add(m.getOwhCode())) {
                    c.products(ctx, m.getOwhCode()).forEach(p -> fetched.putIfAbsent(p.code(), p));
                }
            }
        }
        fetched.remove(null);
        LocalDateTime now = LocalDateTime.now();
        transactionTemplate.executeWithoutResult(s -> {
            Map<String, EcomOwhProduct> existing = new LinkedHashMap<>();
            productMapper.selectList(Wrappers.<EcomOwhProduct>lambdaQuery().eq(EcomOwhProduct::getAccountId, accountId))
                    .forEach(p -> existing.put(p.getProductCode(), p));
            for (OwhData.Product p : fetched.values()) {
                EcomOwhProduct old = existing.remove(p.code());
                if (old == null) {
                    EcomOwhProduct n = new EcomOwhProduct();
                    n.setAccountId(accountId);
                    n.setProductCode(p.code());
                    n.setProductName(p.name());
                    n.setOwhCode(p.warehouseCode());
                    n.setCarrierName(p.carrier());
                    n.setStatus(EnableStatus.ENABLED);
                    n.setSyncTime(now);
                    productMapper.insert(n);
                } else {
                    productMapper.update(Wrappers.<EcomOwhProduct>lambdaUpdate().set(EcomOwhProduct::getProductName, p.name())
                            .set(EcomOwhProduct::getOwhCode, p.warehouseCode())
                            .set(StringUtils.hasText(p.carrier()), EcomOwhProduct::getCarrierName, p.carrier())
                            .set(EcomOwhProduct::getStatus, EnableStatus.ENABLED).set(EcomOwhProduct::getSyncTime, now)
                            .eq(EcomOwhProduct::getId, old.getId()));
                }
            }
            for (EcomOwhProduct gone : existing.values()) {
                productMapper.update(Wrappers.<EcomOwhProduct>lambdaUpdate().set(EcomOwhProduct::getStatus, EnableStatus.DISABLED)
                        .eq(EcomOwhProduct::getId, gone.getId()));
            }
        });
        return fetched.size();
    }

    /** 物流产品对应的物流渠道与承运商名称 */
    public void updateProduct(Long productId, ProductChannelRequest req) {
        EcomOwhProduct p = productMapper.selectById(productId);
        if (p == null) {
            throw new BizException("ecom.error.owh.productNotFound");
        }
        if (req.channelId() != null) {
            logisticsService.get(req.channelId());
        }
        productMapper.update(Wrappers.<EcomOwhProduct>lambdaUpdate().set(EcomOwhProduct::getChannelId, req.channelId())
                .set(EcomOwhProduct::getCarrierName, StringUtils.hasText(req.carrierName()) ? req.carrierName().trim() : null)
                .eq(EcomOwhProduct::getId, productId));
    }

    // ---------------------------------------------------------------- 仓库映射

    public List<EcomOwhWarehouse> mappings(Long accountId) {
        return mappingMapper.selectMappings(accountId);
    }

    /** 系统仓库的映射，没有映射（不是海外仓）时为 null */
    public EcomOwhWarehouse mappingOf(Long warehouseId) {
        return warehouseId == null ? null : mappingMapper.selectByWarehouseId(warehouseId);
    }

    public EcomOwhWarehouse mapping(Long id) {
        EcomOwhWarehouse m = id == null ? null : mappingMapper.selectDetail(id);
        if (m == null) {
            throw new BizException("ecom.error.owh.mappingNotFound");
        }
        return m;
    }

    /** 新增 / 修改映射：一个系统仓库只能映射一次 */
    public Long saveMapping(Long id, MappingRequest req) {
        get(req.accountId());
        warehouseService.get(req.warehouseId());
        EcomOwhWarehouse dup = mappingMapper.selectByWarehouseId(req.warehouseId());
        if (dup != null && !Objects.equals(dup.getId(), id)) {
            throw new BizException("ecom.error.owh.warehouseMapped", dup.getWarehouseName(), dup.getAccountName());
        }
        EcomOwhWarehouse m = id == null ? new EcomOwhWarehouse() : mapping(id);
        if (id != null && !Objects.equals(m.getWarehouseId(), req.warehouseId()) && hasMappingDocuments(id)) {
            throw new BizException("ecom.error.owh.mappingLocked", m.getWarehouseName());
        }
        m.setAccountId(req.accountId());
        m.setWarehouseId(req.warehouseId());
        m.setOwhCode(req.owhCode().trim());
        m.setOwhName(StringUtils.hasText(req.owhName()) ? req.owhName().trim() : null);
        m.setCountry(StringUtils.hasText(req.country()) ? req.country().trim().toUpperCase(Locale.ROOT) : null);
        m.setDefaultProductCode(StringUtils.hasText(req.defaultProductCode()) ? req.defaultProductCode().trim() : null);
        m.setAutoPush(req.autoPush() != null && req.autoPush() == 1 ? 1 : 0);
        m.setStatus(req.status() == null ? EnableStatus.ENABLED : req.status());
        m.setRemark(req.remark());
        if (req.defectWarehouseId() != null) {
            if (req.defectWarehouseId().equals(req.warehouseId())) {
                throw new BizException("ecom.error.owh.defectSameWarehouse");
            }
            warehouseService.get(req.defectWarehouseId());
        }
        m.setDefectWarehouseId(req.defectWarehouseId());
        m.setContact(trim(req.contact()));
        m.setPhone(trim(req.phone()));
        m.setState(trim(req.state()));
        m.setCity(trim(req.city()));
        m.setPostcode(trim(req.postcode()));
        m.setAddress(trim(req.address()));
        if (id == null) {
            mappingMapper.insert(m);
        } else {
            mappingMapper.update(Wrappers.<EcomOwhWarehouse>lambdaUpdate().set(EcomOwhWarehouse::getAccountId, m.getAccountId())
                    .set(EcomOwhWarehouse::getWarehouseId, m.getWarehouseId()).set(EcomOwhWarehouse::getOwhCode, m.getOwhCode())
                    .set(EcomOwhWarehouse::getOwhName, m.getOwhName()).set(EcomOwhWarehouse::getCountry, m.getCountry())
                    .set(EcomOwhWarehouse::getDefaultProductCode, m.getDefaultProductCode()).set(EcomOwhWarehouse::getAutoPush, m.getAutoPush())
                    .set(EcomOwhWarehouse::getStatus, m.getStatus()).set(EcomOwhWarehouse::getRemark, m.getRemark())
                    .set(EcomOwhWarehouse::getDefectWarehouseId, m.getDefectWarehouseId()).set(EcomOwhWarehouse::getContact, m.getContact())
                    .set(EcomOwhWarehouse::getPhone, m.getPhone()).set(EcomOwhWarehouse::getState, m.getState()).set(EcomOwhWarehouse::getCity, m.getCity())
                    .set(EcomOwhWarehouse::getPostcode, m.getPostcode()).set(EcomOwhWarehouse::getAddress, m.getAddress())
                    .eq(EcomOwhWarehouse::getId, id));
        }
        return m.getId();
    }

    /** 删除映射：有入库单 / 出库单的不能删除（可停用） */
    public void deleteMapping(Long id) {
        EcomOwhWarehouse m = mapping(id);
        if (hasMappingDocuments(id)) {
            throw new BizException("ecom.error.owh.mappingHasData", m.getWarehouseName());
        }
        mappingMapper.deleteById(id);
    }

    private boolean hasMappingDocuments(Long mappingId) {
        return outboundMapper.exists(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getMappingId, mappingId)
                .in(EcomOwhOutbound::getStatus, OwhOutboundStatus.ACTIVE))
                || inboundMapper.exists(Wrappers.<EcomOwhInbound>lambdaQuery().eq(EcomOwhInbound::getMappingId, mappingId)
                .ne(EcomOwhInbound::getStatus, OwhInboundStatus.CANCELLED));
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
