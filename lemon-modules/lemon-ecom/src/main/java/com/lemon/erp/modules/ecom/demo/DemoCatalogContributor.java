package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.scm.dto.SupplierPriceRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.model.SupplierLevel;
import com.lemon.erp.modules.scm.service.ScmSupplierPriceService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.dto.MaterialCategoryRequests;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;
import com.lemon.erp.modules.wms.service.WmsMaterialCategoryService;
import com.lemon.erp.modules.wms.service.WmsMaterialImageService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演示数据 · 商品与供应商（order 10）：物料分类（两级）、40 个商品（规格、重量尺寸写在备注、参考成本、零售价、安全库存），
 * 每个商品生成一张示意图走物料图片上传（列表、订单显示缩略图）；6 家供应商与供货价格（默认供应商、交期）
 */
@Component
@RequiredArgsConstructor
public class DemoCatalogContributor implements DemoContributor {

    private final WmsMaterialCategoryService categoryService;
    private final WmsMaterialService materialService;
    private final WmsMaterialImageService imageService;
    private final ScmSupplierService supplierService;
    private final ScmSupplierPriceService supplierPriceService;

    @Override
    public int order() {
        return 10;
    }

    @Override
    public String key() {
        return "catalog";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, String> categoryCodes = categories();
        Map<String, Long> materials = new LinkedHashMap<>();
        Map<String, WmsMaterial> existing = materialService.mapByCodes(DemoCatalog.PRODUCTS.stream().map(DemoCatalog.Product::code).toList());
        for (DemoCatalog.Product p : DemoCatalog.PRODUCTS) {
            WmsMaterial m = existing.get(p.code());
            if (m == null) {
                m = new WmsMaterial();
                m.setCode(p.code());
                m.setName(p.name());
                m.setSpec(p.spec());
                m.setUnit(p.unit());
                m.setCategory(categoryCodes.get(p.category()));
                m.setBrand(p.brand());
                m.setRefCost(p.costValue());
                m.setRetailPrice(BigDecimal.valueOf(p.usd() * 7.1).setScale(0, RoundingMode.HALF_UP));
                m.setSafetyStock(BigDecimal.valueOf(p.popularity() * 8L));
                m.setBarcode("69" + String.format("%011d", 20261100000L + DemoCatalog.PRODUCTS.indexOf(p) * 37L));
                m.setStatus(EnableStatus.ENABLED);
                m.setRemark("毛重 " + p.weight() + " kg，包装 " + p.size() + " cm"); // i18n-ignore 演示数据存库
                materialService.create(m);
                byte[] png = DemoImages.render(p.color(), p.icon(), p.en(), DemoCatalog.BRANDS.get(p.brand()));
                imageService.upload(m.getId(), DemoImages.file(p.code().toLowerCase() + ".png", png));
            }
            materials.put(p.code(), m.getId());
        }
        ctx.put(DemoKeys.MATERIAL_BY_CODE, materials);
        ctx.put(DemoKeys.MATERIAL_IDS, new ArrayList<>(materials.values()));
        suppliers(ctx, materials);
    }

    /** 一级 + 二级分类（按名称复用已有的），返回 key → 分类编码 */
    private Map<String, String> categories() {
        Map<String, String> codes = new LinkedHashMap<>();
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : DemoCatalog.CATEGORIES.entrySet()) {
            WmsMaterialCategory c = find(0L, e.getValue());
            if (c == null) {
                Long id = categoryService.create(new MaterialCategoryRequests.Save(null, null, e.getValue(), null, EnableStatus.ENABLED, null));
                c = categoryService.get(id);
            }
            ids.put(e.getKey(), c.getId());
            codes.put(e.getKey(), c.getCode());
        }
        for (String[] sub : DemoCatalog.SUB_CATEGORIES) {
            Long parentId = ids.get(sub[1]);
            WmsMaterialCategory c = find(parentId, sub[2]);
            if (c == null) {
                Long id = categoryService.create(new MaterialCategoryRequests.Save(parentId, null, sub[2], null, EnableStatus.ENABLED, null));
                c = categoryService.get(id);
            }
            codes.put(sub[0], c.getCode());
        }
        return codes;
    }

    private WmsMaterialCategory find(Long parentId, String name) {
        return categoryService.list().stream()
                .filter(c -> Objects.equals(c.getParentId() == null ? 0L : c.getParentId(), parentId) && name.equals(c.getName()))
                .findFirst().orElse(null);
    }

    private void suppliers(DemoContext ctx, Map<String, Long> materials) {
        Map<String, Long> ids = new LinkedHashMap<>();
        List<Long> list = new ArrayList<>();
        for (DemoCatalog.Supplier d : DemoCatalog.SUPPLIERS) {
            Long id = supplierService.findIdByCodeOrName(d.code());
            boolean created = id == null;
            if (created) {
                ScmSupplier s = new ScmSupplier();
                s.setCode(d.code());
                s.setName(d.name());
                s.setLevel(DemoCatalog.SUPPLIERS.indexOf(d) % 3 == 2 ? SupplierLevel.B : SupplierLevel.A);
                s.setContact(d.contact());
                s.setPhone(d.phone());
                s.setAddress(d.address());
                s.setPaymentDays(DemoCatalog.SUPPLIERS.indexOf(d) % 2 == 0 ? 30 : 45);
                s.setStatus(EnableStatus.ENABLED);
                supplierService.create(s);
                id = s.getId();
            }
            ids.put(d.code(), id);
            list.add(id);
            if (!created) {
                continue;
            }
            int idx = DemoCatalog.SUPPLIERS.indexOf(d);
            for (DemoCatalog.Product p : DemoCatalog.PRODUCTS) {
                if (p.supplier() != idx) {
                    continue;
                }
                // 供货价不含税 = 参考成本 ÷ 1.13，税率 13%，最小起订量 50
                BigDecimal price = p.costValue().divide(BigDecimal.valueOf(1.13), 2, RoundingMode.HALF_UP);
                supplierPriceService.create(new SupplierPriceRequest(id, materials.get(p.code()), price, BigDecimal.valueOf(13), d.leadDays(),
                        BigDecimal.valueOf(50), 1, null, null, null, null));
            }
        }
        ctx.put(DemoContributors.SUPPLIER_BY_CODE, ids);
        ctx.put(DemoKeys.SUPPLIER_IDS, list);
    }
}
