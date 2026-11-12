package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.MappingRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.ProductChannelRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.ProviderView;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhProduct;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhAccountService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhSkuService;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhSku;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.SkuRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 海外仓账号 /ecom/owh/account：账号（授权只写不读）、测试授权、服务商仓库列表、仓库映射、物流产品
 */
@RestController
@RequestMapping("/ecom/owh/account")
@RequiredArgsConstructor
public class EcomOwhAccountController {

    private final EcomOwhAccountService accountService;
    private final EcomOwhSkuService skuService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:list')")
    public R<PageResult<EcomOwhAccount>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              String keyword, OwhProvider provider, EnableStatus status) {
        return R.ok(accountService.page(current, size, keyword, provider, status));
    }

    /** 启用的账号（选择用，登录即可） */
    @GetMapping("/options")
    public R<List<EcomOwhAccount>> options() {
        return R.ok(accountService.options());
    }

    /** 可对接的服务商：授权字段、地址、能力 */
    @GetMapping("/providers")
    public R<List<ProviderView>> providers() {
        return R.ok(accountService.providers());
    }

    /** 全部仓库映射（选择仓库、筛选用，登录即可） */
    @GetMapping("/mappings")
    public R<List<EcomOwhWarehouse>> allMappings() {
        return R.ok(accountService.mappings(null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:list')")
    public R<EcomOwhAccount> get(@PathVariable Long id) {
        return R.ok(accountService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:add')")
    @OperLog(title = "海外仓账号", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody EcomOwhAccount a) {
        return R.ok(accountService.create(a));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:edit')")
    @OperLog(title = "海外仓账号", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody EcomOwhAccount a) {
        accountService.update(id, a);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:edit')")
    @OperLog(title = "海外仓账号", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        accountService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}/credentials")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:edit')")
    @OperLog(title = "海外仓账号", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> clearCredentials(@PathVariable Long id) {
        accountService.clearCredentials(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:remove')")
    @OperLog(title = "海外仓账号", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return R.ok();
    }

    /** 测试授权，返回服务商的仓库数 */
    @PostMapping("/{id}/test")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:edit')")
    public R<Integer> test(@PathVariable Long id) {
        return R.ok(accountService.test(id));
    }

    /** 服务商的仓库列表（实时） */
    @GetMapping("/{id}/remote-warehouses")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    public R<List<OwhData.Warehouse>> remoteWarehouses(@PathVariable Long id) {
        return R.ok(accountService.remoteWarehouses(id));
    }

    @GetMapping("/{id}/mappings")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:list')")
    public R<List<EcomOwhWarehouse>> mappings(@PathVariable Long id) {
        return R.ok(accountService.mappings(id));
    }

    @PostMapping("/mapping")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓仓库映射", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> createMapping(@Validated @RequestBody MappingRequest req) {
        return R.ok(accountService.saveMapping(null, req));
    }

    @PutMapping("/mapping/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓仓库映射", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> updateMapping(@PathVariable Long id, @Validated @RequestBody MappingRequest req) {
        return R.ok(accountService.saveMapping(id, req));
    }

    @DeleteMapping("/mapping/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓仓库映射", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteMapping(@PathVariable Long id) {
        accountService.deleteMapping(id);
        return R.ok();
    }

    /** 账号的物流产品（登录即可：推送时选择） */
    @GetMapping("/{id}/products")
    public R<List<EcomOwhProduct>> products(@PathVariable Long id) {
        return R.ok(accountService.products(id));
    }

    /** 从服务商拉取物流产品，返回产品数 */
    @PostMapping("/{id}/products/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓物流产品", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Integer> syncProducts(@PathVariable Long id) {
        return R.ok(accountService.syncProducts(id));
    }

    @PutMapping("/product/{productId}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓物流产品", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> updateProduct(@PathVariable Long productId, @RequestBody ProductChannelRequest req) {
        accountService.updateProduct(productId, req);
        return R.ok();
    }

    /** 账号的 SKU 映射：keyword 按服务商 SKU / 物料编码 / 名称 */
    @GetMapping("/{id}/skus")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:list')")
    public R<List<EcomOwhSku>> skus(@PathVariable Long id, String keyword) {
        accountService.get(id);
        return R.ok(skuService.list(id, keyword));
    }

    /** 新增 / 修改 SKU 映射（同一物料只有一条） */
    @PostMapping("/{id}/skus")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓 SKU 映射", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> saveSku(@PathVariable Long id, @Validated @RequestBody SkuRequest req) {
        accountService.get(id);
        return R.ok(skuService.save(id, req));
    }

    @DeleteMapping("/sku/{skuId}")
    @PreAuthorize("@ss.hasPerm('ecom:owhAccount:mapping')")
    @OperLog(title = "海外仓 SKU 映射", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteSku(@PathVariable Long skuId) {
        skuService.delete(skuId);
        return R.ok();
    }
}
