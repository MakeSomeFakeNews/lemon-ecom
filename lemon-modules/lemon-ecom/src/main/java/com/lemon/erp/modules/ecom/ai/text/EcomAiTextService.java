package com.lemon.erp.modules.ecom.ai.text;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiTextGenerator;
import com.lemon.erp.framework.ai.AiTextRequest;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessage;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import com.lemon.erp.modules.ecom.cs.model.CsDirection;
import com.lemon.erp.modules.ecom.cs.service.EcomCsThreadService;
import com.lemon.erp.modules.ecom.cs.service.EcomReviewService;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 电商的 AI 文本生成（经 framework 的 {@link AiTextGenerator}，由 ai 模块以「AI 写作」功能调用模型）：
 * <ul>
 *   <li>客服：草拟回复（带店铺、订单、商品、最近的会话记录，默认用买家最近一条消息的语言）、翻译（买家消息译成中文等、回复译成买家的语言）</li>
 *   <li>评价：草拟公开回复（差评致歉并引导联系卖家、好评致谢，用评价的语言）</li>
 *   <li>刊登（供刊登功能调用）：{@link #translate} 翻译标题 / 描述，{@link #optimizeListing} 按平台规则优化标题与描述</li>
 * </ul>
 * 没有 ai 模块或「AI 设置」中没有开启「AI 写作」时 {@link #available()} 为 false，页面据此隐藏 / 禁用按钮；调用时报不可用的原因。
 * 模型调用较慢，不要在事务中调用；提示词按当前语言取消息键，业务数据只放回复需要的字段（不含买家电话、地址）
 */
@Service
@RequiredArgsConstructor
public class EcomAiTextService {

    /** 草拟回复时带上的最近消息条数 */
    static final int HISTORY_MESSAGES = 20;
    /** 单条消息最多带的字符数 */
    static final int MESSAGE_CHARS = 1000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObjectProvider<AiTextGenerator> generator;
    private final EcomCsThreadService threadService;
    private final EcomReviewService reviewService;

    /** 可用状态（页面据此显示按钮或「去配置」提示） */
    public record Status(boolean available, String reason) {
    }

    /** 优化后的刊登文案 */
    public record ListingCopy(String title, String description) {
    }

    public Status status() {
        AiTextGenerator g = generator.getIfAvailable();
        if (g == null) {
            return new Status(false, Messages.get("ecom.error.fix.aiMissing"));
        }
        return g.isAvailable() ? new Status(true, null) : new Status(false, g.unavailableReason());
    }

    public boolean available() {
        return status().available();
    }

    // ---------------------------------------------------------------- 客服

    /**
     * 草拟会话的回复
     *
     * @param language    回复语言（语言代码，如 en、de），为空时用买家最近一条消息的语言
     * @param instruction 补充要求（如「同意补发」「语气更正式」），可为空
     */
    public String draftThreadReply(Long threadId, String language, String instruction) {
        CsViews.ThreadDetail detail = threadService.detail(threadId);
        EcomMessageThread t = detail.thread();
        EcomOrder order = threadService.orderPanel(threadId).order();
        StringBuilder sb = new StringBuilder();
        sb.append("Store: ").append(nz(t.getStoreName())).append(" (").append(platform(t.getPlatform())).append(")\n");
        sb.append("Buyer: ").append(nz(t.getBuyerName())).append('\n');
        if (StringUtils.hasText(t.getSubject())) {
            sb.append("Subject: ").append(t.getSubject()).append('\n');
        }
        appendOrder(sb, order, t.getPlatformOrderNo());
        sb.append("\nConversation (oldest first):\n");
        List<EcomMessage> messages = detail.messages();
        int from = Math.max(0, messages.size() - HISTORY_MESSAGES);
        boolean hasBuyer = false;
        for (EcomMessage m : messages.subList(from, messages.size())) {
            if (!StringUtils.hasText(m.getContent())) {
                continue;
            }
            hasBuyer |= m.getDirection() == CsDirection.BUYER;
            sb.append('[').append(m.getDirection() == CsDirection.BUYER ? "Buyer" : "Seller");
            if (m.getSentTime() != null) {
                sb.append(' ').append(TIME.format(m.getSentTime()));
            }
            sb.append("] ").append(clip(m.getContent(), MESSAGE_CHARS)).append('\n');
        }
        if (!hasBuyer) {
            throw new BizException("ecom.error.fix.noBuyerMessage");
        }
        if (StringUtils.hasText(instruction)) {
            sb.append("\nExtra instruction from the seller: ").append(clip(instruction.trim(), 500)).append('\n');
        }
        String system = Messages.get("ecom.prompt.fix.csDraft", nz(t.getStoreName()), platform(t.getPlatform()))
                + (StringUtils.hasText(language) ? "" : "\n" + Messages.get("ecom.prompt.fix.buyerLanguage"));
        return generate(AiTextRequest.of(system, sb.toString()).withLanguage(languageName(language)));
    }

    /**
     * 把回复译成买家的语言（参考会话中买家最近一条消息判断语言）
     */
    public String translateForThread(Long threadId, String text) {
        requireText(text);
        CsViews.ThreadDetail detail = threadService.detail(threadId);
        String buyerText = null;
        for (int i = detail.messages().size() - 1; i >= 0; i--) {
            EcomMessage m = detail.messages().get(i);
            if (m.getDirection() == CsDirection.BUYER && StringUtils.hasText(m.getContent())) {
                buyerText = m.getContent();
                break;
            }
        }
        if (buyerText == null) {
            throw new BizException("ecom.error.fix.noBuyerMessage");
        }
        return generate(AiTextRequest.of(Messages.get("ecom.prompt.fix.translateToBuyer", clip(buyerText, 300)), text));
    }

    /**
     * 翻译（买家消息译成中文、刊登标题 / 描述译成目标站点语言等）
     *
     * @param targetLanguage 目标语言代码（如 zh、en、de）或语言名称
     */
    public String translate(String text, String targetLanguage) {
        requireText(text);
        if (!StringUtils.hasText(targetLanguage)) {
            throw new BizException("ecom.error.fix.languageRequired");
        }
        String lang = languageName(targetLanguage);
        return generate(AiTextRequest.of(Messages.get("ecom.prompt.fix.translate", lang), text));
    }

    // ---------------------------------------------------------------- 评价

    /**
     * 草拟评价的公开回复
     *
     * @param language 回复语言，为空时用评价的语言
     */
    public String draftReviewReply(Long reviewId, String language, String instruction) {
        EcomReview r = reviewService.detail(reviewId);
        StringBuilder sb = new StringBuilder();
        sb.append("Store: ").append(nz(r.getStoreName())).append(" (").append(platform(r.getPlatform())).append(")\n");
        sb.append("Product: ").append(nz(StringUtils.hasText(r.getListingTitle()) ? r.getListingTitle() : r.getSellerSku())).append('\n');
        sb.append("Rating: ").append(r.getRating() == null ? "-" : r.getRating() + "/5").append('\n');
        if (StringUtils.hasText(r.getTitle())) {
            sb.append("Review title: ").append(clip(r.getTitle(), 300)).append('\n');
        }
        sb.append("Review: ").append(clip(nz(r.getContent()), 2000)).append('\n');
        if (StringUtils.hasText(instruction)) {
            sb.append("\nExtra instruction from the seller: ").append(clip(instruction.trim(), 500)).append('\n');
        }
        if (!StringUtils.hasText(r.getContent()) && !StringUtils.hasText(r.getTitle())) {
            throw new BizException("ecom.error.fix.reviewEmpty");
        }
        String system = Messages.get("ecom.prompt.fix.reviewDraft", nz(r.getStoreName()))
                + (StringUtils.hasText(language) ? "" : "\n" + Messages.get("ecom.prompt.fix.reviewLanguage"));
        return generate(AiTextRequest.of(system, sb.toString()).withLanguage(languageName(language)));
    }

    // ---------------------------------------------------------------- 刊登

    /**
     * 按平台规则优化商品标题与描述（刊登时使用）
     *
     * @param platform    平台（标题长度等规则按平台说明给模型）
     * @param language    输出语言（目标站点语言），为空时与原文相同
     * @param maxTitle    标题最多字符数（如亚马逊 200），为空不限制
     * @param instruction 补充要求（关键词、卖点等），可为空
     */
    public ListingCopy optimizeListing(EcomPlatform platform, String title, String description, String language, Integer maxTitle,
                                       String instruction) {
        if (!StringUtils.hasText(title) && !StringUtils.hasText(description)) {
            throw new BizException("ecom.error.fix.textRequired");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(nz(title)).append('\n');
        sb.append("Description:\n").append(clip(nz(description), 8000)).append('\n');
        if (StringUtils.hasText(instruction)) {
            sb.append("\nExtra instruction: ").append(clip(instruction.trim(), 500)).append('\n');
        }
        String system = Messages.get("ecom.prompt.fix.listingOptimize", platform(platform), maxTitle == null ? "-" : maxTitle.toString());
        String out = generate(AiTextRequest.of(system, sb.toString()).withLanguage(languageName(language)));
        return parseListing(out, title);
    }

    /** 解析模型输出的 JSON（{"title", "description"}，允许包在 ``` 代码块中）；不是 JSON 时整段作为描述 */
    static ListingCopy parseListing(String out, String fallbackTitle) {
        String s = out.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                JsonNode n = HttpJson.MAPPER.readTree(s.substring(start, end + 1));
                String title = n.hasNonNull("title") ? n.get("title").asString() : null;
                String desc = n.hasNonNull("description") ? n.get("description").asString() : null;
                if (title != null || desc != null) {
                    return new ListingCopy(title == null ? fallbackTitle : title.trim(), desc == null ? null : desc.trim());
                }
            } catch (RuntimeException e) {
                // 不是合法 JSON，按纯文本处理
            }
        }
        return new ListingCopy(fallbackTitle, s);
    }

    // ---------------------------------------------------------------- 内部

    private String generate(AiTextRequest request) {
        AiTextGenerator g = generator.getIfAvailable();
        if (g == null) {
            throw new BizException("ecom.error.fix.aiMissing");
        }
        return g.generate(request);
    }

    private static void appendOrder(StringBuilder sb, EcomOrder o, String platformOrderNo) {
        if (o == null) {
            if (StringUtils.hasText(platformOrderNo)) {
                sb.append("Order: ").append(platformOrderNo).append(" (details unavailable)\n");
            }
            return;
        }
        sb.append("Order: ").append(o.getPlatformOrderNo()).append(", status ").append(o.getStatus());
        if (o.getOrderTime() != null) {
            sb.append(", ordered ").append(TIME.format(o.getOrderTime()));
        }
        if (o.getShipTime() != null) {
            sb.append(", shipped ").append(TIME.format(o.getShipTime()));
        }
        if (StringUtils.hasText(o.getCarrier()) || StringUtils.hasText(o.getTrackingNo())) {
            sb.append(", carrier ").append(nz(o.getCarrier())).append(", tracking ").append(nz(o.getTrackingNo()));
        }
        if (o.getDeliverBy() != null) {
            sb.append(", expected delivery by ").append(TIME.format(o.getDeliverBy()));
        }
        if (StringUtils.hasText(o.getShipCountry())) {
            sb.append(", ship to ").append(o.getShipCountry());
        }
        sb.append('\n');
        if (o.getItems() != null) {
            for (EcomOrderItem it : o.getItems()) {
                sb.append("- ").append(nz(it.getSellerSku())).append(' ').append(clip(nz(it.getTitle()), 200)).append(" x ")
                        .append(it.getQty() == null ? "1" : it.getQty().stripTrailingZeros().toPlainString()).append('\n');
            }
        }
    }

    /** 语言代码转英文名称（en → English），不是代码时原样返回；为空返回 null */
    static String languageName(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String c = code.trim();
        if (c.length() <= 3 || c.contains("-") || c.contains("_")) {
            Locale l = Locale.forLanguageTag(c.replace('_', '-'));
            String name = l.getDisplayLanguage(Locale.ENGLISH);
            if (StringUtils.hasText(name) && !name.equalsIgnoreCase(c)) {
                return name;
            }
        }
        return c;
    }

    private static String platform(EcomPlatform p) {
        return p == null ? "-" : p.name();
    }

    private static void requireText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BizException("ecom.error.fix.textRequired");
        }
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
