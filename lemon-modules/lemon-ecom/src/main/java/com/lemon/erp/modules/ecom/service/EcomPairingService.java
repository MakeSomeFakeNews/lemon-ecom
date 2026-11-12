package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.dto.MaterialPairing;
import com.lemon.erp.modules.ecom.dto.PairingImportRow;
import com.lemon.erp.modules.ecom.dto.PairingSuggestion;
import com.lemon.erp.modules.ecom.dto.SpuGroup;
import com.lemon.erp.modules.ecom.dto.StyleMatchRequest;
import com.lemon.erp.modules.ecom.dto.StyleMatchRow;
import com.lemon.erp.modules.ecom.dto.UnpairedListing;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomCatalogMapper;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.model.PairingMatchType;
import com.lemon.erp.modules.ecom.model.StyleMatchRule;
import com.lemon.erp.modules.wms.dto.VariantMatrix;
import com.lemon.erp.modules.wms.entity.WmsAttributeValue;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SKU 配对中心与按款式批量映射：以本地物料为中心查看它在各店铺、各平台的在线商品；未配对的平台 SKU 按
 * 编码相同 → 去掉前后缀后相同 → 标题相似 推荐物料，一键确认；Excel 批量导入配对；解除配对；配对变化后重新匹配异常订单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomPairingService {

    /** SKU 中的分隔符 */
    private static final Pattern SKU_SEP = Pattern.compile("[-_./\\s]+");
    /** 变体属性、标题的分词 */
    private static final Pattern WORD_SEP = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");
    /** 按编码查物料的单次数量 */
    private static final int CODE_BATCH = 500;
    /** SKU 去掉前后缀时最多看几段 */
    private static final int MAX_SEGMENTS = 6;
    /** 标题相似的最低相似度 */
    private static final double TITLE_MIN = 0.4;
    /** 一次重新匹配的异常订单上限 */
    private static final int REMATCH_LIMIT = 500;

    private final EcomCatalogMapper catalogMapper;
    private final EcomListingMapper listingMapper;
    private final EcomListingService listingService;
    private final EcomStoreService storeService;
    private final EcomOrderService orderService;
    private final WmsMaterialService materialService;
    private final WmsVariantService variantService;

    // ---------------------------------------------------------------- 以物料为中心

    public PageResult<MaterialPairing> materials(long current, long size, String keyword, Boolean pairedOnly) {
        Page<MaterialPairing> page = Page.of(current, size);
        catalogMapper.selectMaterialPage(page, keyword, pairedOnly);
        return PageResult.of(page);
    }

    /** 物料在各店铺、各平台的在线商品：单品映射（packQty 为多件装）与组合商品（packQty 为组合中的用量） */
    public List<EcomListing> listingsOfMaterial(Long materialId) {
        List<EcomListing> out = new ArrayList<>(catalogMapper.selectListingsOfMaterial(materialId));
        out.addAll(catalogMapper.selectBundlesOfMaterial(materialId));
        return out;
    }

    public PageResult<SpuGroup> spuPage(long current, long size, Long storeId, String keyword, Boolean mapped) {
        Page<SpuGroup> page = Page.of(current, size);
        catalogMapper.selectSpuPage(page, storeId, keyword, mapped);
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 未配对与推荐

    /** 未配对的平台 SKU（分页），每条带一个推荐物料（没有可推荐的为空） */
    public PageResult<UnpairedListing> unpaired(long current, long size, Long storeId, String keyword) {
        Page<EcomListing> page = Page.of(current, size);
        catalogMapper.selectUnpaired(page, storeId, keyword);
        Map<Long, PairingSuggestion> suggestions = suggest(page.getRecords());
        List<UnpairedListing> rows = page.getRecords().stream().map(l -> new UnpairedListing(l.getId(), l.getStoreId(), l.getStoreName(), l.getPlatform(),
                l.getSellerSku(), l.getPlatformItemId(), l.getTitle(), l.getVariant(), l.getPrice(), l.getCurrency(), suggestions.get(l.getId()))).toList();
        return new PageResult<>(rows, page.getTotal());
    }

    /**
     * 推荐：① 平台 SKU 与物料编码相同（忽略大小写）；② 去掉前后缀（按 - _ . / 分段后的连续几段）后与物料编码相同，取最长的；
     * ③ 标题与物料名称（含规格）相似（字符二元组 Dice 系数 ≥ 0.4）
     *
     * @return 在线商品 ID → 推荐
     */
    Map<Long, PairingSuggestion> suggest(List<EcomListing> listings) {
        Map<Long, PairingSuggestion> out = new HashMap<>();
        Map<Long, List<String>> candidates = new LinkedHashMap<>();
        Set<String> codes = new LinkedHashSet<>();
        for (EcomListing l : listings) {
            List<String> c = codeCandidates(l.getSellerSku());
            candidates.put(l.getId(), c);
            for (String x : c) {
                codes.add(x);
                codes.add(x.toUpperCase(Locale.ROOT));
                codes.add(x.toLowerCase(Locale.ROOT));
            }
        }
        Map<String, Map<String, Object>> byCode = new HashMap<>();
        List<String> all = new ArrayList<>(codes);
        for (int i = 0; i < all.size(); i += CODE_BATCH) {
            for (Map<String, Object> m : catalogMapper.selectMaterialsByCodes(all.subList(i, Math.min(all.size(), i + CODE_BATCH)))) {
                byCode.putIfAbsent(String.valueOf(m.get("code")).toUpperCase(Locale.ROOT), m);
            }
        }
        for (EcomListing l : listings) {
            List<String> c = candidates.get(l.getId());
            for (int i = 0; i < c.size(); i++) {
                Map<String, Object> m = byCode.get(c.get(i).toUpperCase(Locale.ROOT));
                if (m != null) {
                    boolean exact = i == 0;
                    out.put(l.getId(), new PairingSuggestion(((Number) m.get("id")).longValue(), (String) m.get("code"), (String) m.get("name"),
                            (String) m.get("spec"), exact ? PairingMatchType.EXACT : PairingMatchType.STRIPPED, exact ? 100 : 90));
                    break;
                }
            }
            if (!out.containsKey(l.getId()) && StringUtils.hasText(l.getTitle())) {
                PairingSuggestion t = byTitle(l.getTitle());
                if (t != null) {
                    out.put(l.getId(), t);
                }
            }
        }
        return out;
    }

    /** 平台 SKU 可能对应的物料编码：SKU 本身，然后按段数从多到少的连续几段（用原分隔符、去掉分隔符两种拼法） */
    static List<String> codeCandidates(String sku) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(sku)) {
            return out;
        }
        String s = sku.trim();
        out.add(s);
        String[] parts = SKU_SEP.split(s);
        List<String> segs = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) segs.add(p);
        }
        if (segs.size() < 2 || segs.size() > MAX_SEGMENTS) {
            if (segs.size() > 1) {
                out.add(String.join("", segs));
            }
            return out;
        }
        Set<String> seen = new LinkedHashSet<>(out);
        for (int len = segs.size(); len >= 1; len--) {
            for (int start = 0; start + len <= segs.size(); start++) {
                List<String> sub = segs.subList(start, start + len);
                if (len < segs.size()) {
                    seen.add(String.join("-", sub));
                    seen.add(String.join("_", sub));
                }
                seen.add(String.join("", sub));
            }
        }
        return new ArrayList<>(seen);
    }

    /** 按标题找相似的物料：用标题中最长的两个词搜索物料，取相似度最高的 */
    private PairingSuggestion byTitle(String title) {
        List<String> words = new ArrayList<>();
        for (String w : WORD_SEP.split(title)) {
            if (!w.isEmpty() && !words.contains(w)) words.add(w);
        }
        words.removeIf(w -> w.length() < 3 && !CJK.matcher(w).find());
        words.sort(Comparator.comparingInt(String::length).reversed());
        Map<Long, WmsMaterial> found = new LinkedHashMap<>();
        for (String w : words.subList(0, Math.min(2, words.size()))) {
            materialService.options(w).forEach(m -> found.putIfAbsent(m.getId(), m));
        }
        WmsMaterial best = null;
        double bestScore = 0;
        for (WmsMaterial m : found.values()) {
            double sc = similarity(title, (Objects.toString(m.getName(), "") + " " + Objects.toString(m.getSpec(), "")).trim());
            if (sc > bestScore) {
                bestScore = sc;
                best = m;
            }
        }
        if (best == null || bestScore < TITLE_MIN) {
            return null;
        }
        return new PairingSuggestion(best.getId(), best.getCode(), best.getName(), best.getSpec(), PairingMatchType.TITLE, (int) Math.round(bestScore * 80));
    }

    /** 两段文字的相似度：字符二元组的 Dice 系数（忽略大小写、空白与标点） */
    static double similarity(String a, String b) {
        List<String> x = bigrams(a);
        List<String> y = bigrams(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        Map<String, Integer> count = new HashMap<>();
        y.forEach(g -> count.merge(g, 1, Integer::sum));
        int hit = 0;
        for (String g : x) {
            Integer n = count.get(g);
            if (n != null && n > 0) {
                hit++;
                count.put(g, n - 1);
            }
        }
        return 2.0 * hit / (x.size() + y.size());
    }

    private static List<String> bigrams(String s) {
        String t = WORD_SEP.matcher(Objects.toString(s, "").toLowerCase(Locale.ROOT)).replaceAll("");
        List<String> out = new ArrayList<>();
        for (int i = 0; i + 2 <= t.length(); i++) {
            out.add(t.substring(i, i + 2));
        }
        return out;
    }

    static List<String> words(String s) {
        List<String> out = new ArrayList<>();
        for (String w : WORD_SEP.split(Objects.toString(s, "").toLowerCase(Locale.ROOT))) {
            if (!w.isEmpty()) out.add(w);
        }
        return out;
    }

    // ---------------------------------------------------------------- 导入、解除、重新匹配

    /** 导入一行配对：在线商品按 店铺编码 + 平台 SKU 找（必须已存在），物料按编码找，映射为单品 × 数量 */
    public void importRow(PairingImportRow row) {
        EcomStore store = storeService.findByCode(row.getStoreCode().trim());
        if (store == null) {
            throw new BizException("ecom.error.storeCodeNotFound", row.getStoreCode());
        }
        EcomListing l = listingMapper.selectOne(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, store.getId())
                .eq(EcomListing::getSellerSku, row.getSellerSku().trim()));
        if (l == null) {
            throw new BizException("ecom.error.catalog.listingSkuNotFound", row.getStoreCode(), row.getSellerSku());
        }
        String code = row.getMaterialCode().trim();
        WmsMaterial m = materialService.mapByCodes(List.of(code)).get(code);
        if (m == null) {
            throw new BizException("ecom.error.materialCodeNotFound", code);
        }
        listingService.map(l.getId(), m.getId(), row.getQty());
    }

    /**
     * 重新匹配异常订单（配对变化后）：逐张订单重新匹配 SKU，每张一个事务，匹配齐的回到待审核
     *
     * @return { matched: 匹配齐的订单数, remaining: 仍有未匹配 SKU 的订单数 }
     */
    public Map<String, Integer> rematchExceptions(Long storeId) {
        int matched = 0;
        int remaining = 0;
        for (Long id : catalogMapper.selectExceptionOrderIds(storeId, REMATCH_LIMIT)) {
            try {
                orderService.rematch(id);
                if (orderService.isMatched(id)) {
                    matched++;
                } else {
                    remaining++;
                }
            } catch (BizException e) {
                remaining++;
            }
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("matched", matched);
        out.put("remaining", remaining);
        return out;
    }

    // ---------------------------------------------------------------- 按款式批量映射

    /**
     * 按款式批量映射的预览：款式下的 SKU 物料与范围内的在线商品按规则配对（一个在线商品只配一个 SKU，有多个候选时不配）。
     * 按变体属性：在线商品的变体属性（没有时用 SKU 与标题）包含该 SKU 的全部规格值（值或值编码）；
     * 按 SKU 编码：平台 SKU 去掉前缀 / 后缀后与物料编码相同（忽略大小写和分隔符），没有前后缀时也接受以物料编码开头或结尾
     */
    public List<StyleMatchRow> previewStyle(StyleMatchRequest req) {
        VariantMatrix matrix = variantService.matrix(req.templateId());
        Map<Long, WmsAttributeValue> values = new HashMap<>();
        matrix.attributes().forEach(a -> a.values().forEach(v -> values.put(v.getId(), v)));
        List<VariantMatrix.Variant> variants = matrix.variants().stream().filter(v -> v.status() == null || v.status().isEnabled()).toList();
        List<EcomListing> listings = scope(req);
        List<StyleMatchRow> out = new ArrayList<>();
        for (EcomListing l : listings) {
            boolean paired = l.getMaterialId() != null || l.getComposeType() == ListingComposeType.BUNDLE;
            if (paired && !Boolean.TRUE.equals(req.includeMapped())) {
                continue;
            }
            List<VariantMatrix.Variant> hits = req.rule() == StyleMatchRule.SKU ? bySku(l, variants, req.prefix(), req.suffix())
                    : byAttribute(l, variants, values);
            VariantMatrix.Variant v = hits.size() == 1 ? hits.get(0) : null;
            out.add(new StyleMatchRow(l.getId(), l.getStoreName(), l.getSellerSku(), l.getTitle(), l.getVariant(), l.getMaterialId(), l.getMaterialCode(),
                    v == null ? null : v.id(), v == null ? null : v.code(), v == null ? null : v.name(), v == null ? null : v.variantText(),
                    v == null ? null : req.rule() == StyleMatchRule.SKU ? PairingMatchType.STRIPPED : PairingMatchType.ATTRIBUTE));
        }
        return out;
    }

    private List<EcomListing> scope(StyleMatchRequest req) {
        if (req.listingIds() != null && !req.listingIds().isEmpty()) {
            List<EcomListing> list = new ArrayList<>();
            for (Long id : new LinkedHashSet<>(req.listingIds())) {
                list.add(listingService.detail(id));
            }
            return list;
        }
        if (req.storeId() == null) {
            throw new BizException("ecom.error.catalog.styleScopeRequired");
        }
        return listingService.page(1, 500, req.storeId(), null, null, null, null, null, null,
                StringUtils.hasText(req.parentItemId()) ? req.parentItemId().trim() : null).records();
    }

    static List<VariantMatrix.Variant> byAttribute(EcomListing l, List<VariantMatrix.Variant> variants, Map<Long, WmsAttributeValue> values) {
        String text = StringUtils.hasText(l.getVariant()) ? l.getVariant() : Objects.toString(l.getSellerSku(), "") + " " + Objects.toString(l.getTitle(), "");
        Set<String> tokens = new HashSet<>(words(text));
        String flat = text.toLowerCase(Locale.ROOT);
        List<VariantMatrix.Variant> hits = new ArrayList<>();
        for (VariantMatrix.Variant v : variants) {
            if (v.values() == null || v.values().isEmpty()) {
                continue;
            }
            boolean all = v.values().values().stream().map(values::get).allMatch(val -> val != null && valueIn(val, tokens, flat));
            if (all) {
                hits.add(v);
            }
        }
        return hits;
    }

    private static boolean valueIn(WmsAttributeValue v, Set<String> tokens, String flat) {
        for (String s : new String[]{v.getAttrValue(), v.getCode()}) {
            if (!StringUtils.hasText(s)) {
                continue;
            }
            String x = s.trim().toLowerCase(Locale.ROOT);
            if (tokens.contains(x) || (CJK.matcher(x).find() && flat.contains(x))) {
                return true;
            }
            // 多个词的值（如 Navy Blue）按词都出现
            List<String> ws = words(x);
            if (ws.size() > 1 && tokens.containsAll(ws)) {
                return true;
            }
        }
        return false;
    }

    static List<VariantMatrix.Variant> bySku(EcomListing l, List<VariantMatrix.Variant> variants, String prefix, String suffix) {
        String sku = Objects.toString(l.getSellerSku(), "").trim();
        boolean trimmed = false;
        if (StringUtils.hasText(prefix) && sku.toLowerCase(Locale.ROOT).startsWith(prefix.trim().toLowerCase(Locale.ROOT))) {
            sku = sku.substring(prefix.trim().length());
            trimmed = true;
        }
        if (StringUtils.hasText(suffix) && sku.toLowerCase(Locale.ROOT).endsWith(suffix.trim().toLowerCase(Locale.ROOT))) {
            sku = sku.substring(0, sku.length() - suffix.trim().length());
            trimmed = true;
        }
        String key = compact(sku);
        List<VariantMatrix.Variant> exact = variants.stream().filter(v -> compact(v.code()).equals(key)).toList();
        if (!exact.isEmpty() || trimmed || StringUtils.hasText(prefix) || StringUtils.hasText(suffix)) {
            return exact;
        }
        return variants.stream().filter(v -> {
            String c = compact(v.code());
            return !c.isEmpty() && (key.startsWith(c) || key.endsWith(c));
        }).toList();
    }

    private static String compact(String s) {
        return WORD_SEP.matcher(Objects.toString(s, "").toLowerCase(Locale.ROOT)).replaceAll("");
    }
}
