package com.lemon.erp.modules.ecom.assistant.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiTextGenerator;
import com.lemon.erp.framework.ai.AiTextRequest;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRows;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.entity.EcomBannedWord;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomAssistantQueryMapper;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishDraftService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 写 Listing：按刊登记录的平台规则（标题长度、五点条数）和目标站点语言生成或优化标题、五点、描述，
 * 可融入从广告搜索词报告中勾选的高转化词，提示模型避开违禁词库中适用的词；生成结果再做一次合规检查后返回给页面（由用户确认后填入）。
 * 模型经 {@link AiTextGenerator}（「AI 写作」功能），未配置时报不可用的原因
 */
@Service
@RequiredArgsConstructor
public class EcomListingAiService {

    /** 站点 → 刊登语言（未列出的站点用英语） */
    static final Map<String, String> MARKETPLACE_LANGUAGES = Map.ofEntries(
            Map.entry("DE", "de"), Map.entry("AT", "de"), Map.entry("CH", "de"), Map.entry("FR", "fr"), Map.entry("BE", "fr"),
            Map.entry("IT", "it"), Map.entry("ES", "es"), Map.entry("MX", "es"), Map.entry("CO", "es"), Map.entry("CL", "es"),
            Map.entry("AR", "es"), Map.entry("JP", "ja"), Map.entry("BR", "pt"), Map.entry("PT", "pt"), Map.entry("NL", "nl"),
            Map.entry("SE", "sv"), Map.entry("PL", "pl"), Map.entry("TR", "tr"), Map.entry("TH", "th"), Map.entry("VN", "vi"),
            Map.entry("ID", "id"), Map.entry("TW", "zh-TW"), Map.entry("KR", "ko"), Map.entry("RU", "ru"), Map.entry("CN", "zh"));
    /** 关键词建议默认取最近的天数 */
    static final int KEYWORD_DAYS = 60;
    static final int KEYWORD_LIMIT = 50;
    static final String PART_TITLE = "title";
    static final String PART_BULLETS = "bullets";
    static final String PART_DESCRIPTION = "description";

    private final EcomPublishDraftService draftService;
    private final EcomStoreService storeService;
    private final EcomBannedWordService wordService;
    private final EcomAssistantQueryMapper queryMapper;
    private final ObjectProvider<AiTextGenerator> generator;

    /** 站点的刊登语言代码 */
    public static String languageOf(String marketplace) {
        return marketplace == null ? "en" : MARKETPLACE_LANGUAGES.getOrDefault(marketplace.trim().toUpperCase(Locale.ROOT), "en");
    }

    /** 平台的标题最大长度（字符） */
    static int maxTitle(EcomPlatform p) {
        if (p == null) {
            return 200;
        }
        return switch (p) {
            case EBAY -> 80;
            case MERCADO_LIBRE -> 60;
            case WALMART -> 75;
            case SHOPEE -> 120;
            case ALIEXPRESS -> 128;
            case AMAZON -> 200;
            default -> 255;
        };
    }

    /**
     * 生成 / 优化文案
     */
    public AssistantViews.ListingCopy generate(AssistantRequests.ListingAiRequest req) {
        EcomPublishItem item = draftService.getItem(req.itemId());
        EcomStore store = storeService.detail(item.getStoreId());
        String language = StringUtils.hasText(req.language()) ? req.language().trim() : languageOf(store.getMarketplace());
        Set<String> parts = req.parts() == null || req.parts().isEmpty() ? Set.of(PART_TITLE, PART_BULLETS, PART_DESCRIPTION) : Set.copyOf(req.parts());
        List<EcomBannedWord> words = wordService.applicable(store.getPlatform(), store.getMarketplace());

        StringBuilder sb = new StringBuilder();
        sb.append("Platform: ").append(store.getPlatform()).append(", marketplace: ").append(store.getMarketplace()).append('\n');
        sb.append("Product: ").append(nz(item.getMaterialName())).append(item.getMaterialCode() == null ? "" : " (" + item.getMaterialCode() + ")").append('\n');
        if (StringUtils.hasText(item.getBrand())) {
            sb.append("Brand: ").append(item.getBrand()).append('\n');
        }
        if (StringUtils.hasText(item.getCategoryName())) {
            sb.append("Category: ").append(item.getCategoryName()).append('\n');
        }
        if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
            sb.append("Attributes: ").append(item.getAttributes().entrySet().stream().limit(30).map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; "))).append('\n');
        }
        String title = req.title() != null ? req.title() : item.getTitle();
        List<String> bullets = req.bullets() != null ? req.bullets() : item.getBullets();
        String description = req.description() != null ? req.description() : item.getDescription();
        sb.append("\nCurrent title: ").append(nz(title)).append('\n');
        sb.append("Current bullet points:\n");
        if (bullets != null) {
            bullets.stream().filter(StringUtils::hasText).forEach(b -> sb.append("- ").append(clip(b, 500)).append('\n'));
        }
        sb.append("Current description:\n").append(clip(nz(description), 6000)).append('\n');
        if (req.keywords() != null && !req.keywords().isEmpty()) {
            sb.append("\nKeywords to include naturally (high-converting customer search terms): ")
                    .append(req.keywords().stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.joining(", "))).append('\n');
        }
        String avoid = words.stream().filter(w -> w.getLevel() == BannedWordLevel.BLOCK || w.getLevel() == BannedWordLevel.WARN)
                .map(EcomBannedWord::getWord).distinct().limit(200).collect(Collectors.joining(", "));
        if (!avoid.isEmpty()) {
            sb.append("\nNever use these words or phrases: ").append(avoid).append('\n');
        }
        if (StringUtils.hasText(req.instruction())) {
            sb.append("\nExtra instruction from the seller: ").append(clip(req.instruction().trim(), 500)).append('\n');
        }
        sb.append("\nParts to write: ").append(String.join(", ", parts)).append('\n');
        if (!StringUtils.hasText(title) && (bullets == null || bullets.isEmpty()) && !StringUtils.hasText(description)
                && !StringUtils.hasText(item.getMaterialName())) {
            throw new BizException("ecom.error.fix.textRequired");
        }
        String system = Messages.get("ecom.prompt.asst.listing", String.valueOf(store.getPlatform()), maxTitle(store.getPlatform()));
        String out = require().generate(AiTextRequest.of(system, sb.toString()).withLanguage(languageName(language)));
        AssistantViews.ListingCopy parsed = parse(out, parts, title, bullets, description, language);
        String t = parsed.title();
        if (t != null && t.length() > maxTitle(store.getPlatform())) {
            t = t.substring(0, maxTitle(store.getPlatform())).trim();
        }
        AssistantViews.CheckResult check = EcomBannedWordService.check(words, t, parsed.bullets(), parsed.description());
        return new AssistantViews.ListingCopy(t, parsed.bullets(), parsed.description(), language, check);
    }

    /** 解析模型输出的 JSON（允许包在代码块中）；没有生成的部分保留原文 */
    static AssistantViews.ListingCopy parse(String out, Set<String> parts, String title, List<String> bullets, String description, String language) {
        String s = out == null ? "" : out.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BizException("ecom.error.asst.aiBadOutput");
        }
        JsonNode n;
        try {
            n = HttpJson.MAPPER.readTree(s.substring(start, end + 1));
        } catch (RuntimeException e) {
            throw new BizException("ecom.error.asst.aiBadOutput");
        }
        String t = parts.contains(PART_TITLE) && n.hasNonNull("title") ? n.get("title").asString().trim() : title;
        List<String> b = bullets == null ? List.of() : bullets;
        if (parts.contains(PART_BULLETS) && n.has("bullets") && n.get("bullets").isArray()) {
            List<String> list = new ArrayList<>();
            n.get("bullets").forEach(x -> {
                String v = x.asString().trim();
                if (!v.isEmpty() && list.size() < 10) {
                    list.add(v.length() > 500 ? v.substring(0, 500) : v);
                }
            });
            b = list;
        }
        String d = parts.contains(PART_DESCRIPTION) && n.hasNonNull("description") ? n.get("description").asString().trim() : description;
        return new AssistantViews.ListingCopy(t, b, d, language, null);
    }

    /**
     * 关键词建议：同一物料（或同一款式）在广告搜索词报告中出过单的搜索词（同站点），按订单数排序；sameCategory 时扩大到同品类，
     * 同物料没有时也自动扩大到同品类（结果中 sameCategory 为 true）
     */
    public AssistantViews.KeywordResult keywords(Long itemId, boolean sameCategory, Integer days) {
        EcomPublishItem item = draftService.getItem(itemId);
        EcomStore store = storeService.detail(item.getStoreId());
        int d = days == null ? KEYWORD_DAYS : Math.max(7, Math.min(365, days));
        boolean category = sameCategory;
        List<AssistantRows.KeywordRow> rows = item.getMaterialId() == null ? List.of()
                : queryMapper.selectKeywordSuggestions(item.getMaterialId(), category, store.getMarketplace(), LocalDate.now().minusDays(d), KEYWORD_LIMIT);
        // 同物料没有出单的搜索词（如新品）时自动扩大到同品类
        if (rows.isEmpty() && !category && item.getMaterialId() != null) {
            category = true;
            rows = queryMapper.selectKeywordSuggestions(item.getMaterialId(), true, store.getMarketplace(), LocalDate.now().minusDays(d), KEYWORD_LIMIT);
        }
        for (AssistantRows.KeywordRow r : rows) {
            r.setCvrPercent(r.getClicks() == 0 ? null
                    : BigDecimal.valueOf(r.getOrders()).multiply(Decimals.HUNDRED).divide(BigDecimal.valueOf(r.getClicks()), 2, RoundingMode.HALF_UP));
            r.setMarketplaces(store.getMarketplace());
        }
        return new AssistantViews.KeywordResult(item.getMaterialId(), store.getMarketplace(), category, d, rows);
    }

    private AiTextGenerator require() {
        AiTextGenerator g = generator.getIfAvailable();
        if (g == null) {
            throw new BizException("ecom.error.fix.aiMissing");
        }
        return g;
    }

    /** 语言代码转英文名称（en → English） */
    static String languageName(String code) {
        Locale l = Locale.forLanguageTag(code.replace('_', '-'));
        String name = l.getDisplayName(Locale.ENGLISH);
        return StringUtils.hasText(name) && !name.equalsIgnoreCase(code) ? name : code;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
