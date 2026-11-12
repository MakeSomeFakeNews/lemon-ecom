package com.lemon.erp.modules.ecom.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.cs.connector.CsCapability;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnector;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnectorRegistry;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.cs.entity.EcomFeedback;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessage;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.entity.EcomReplyTemplate;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import com.lemon.erp.modules.ecom.cs.entity.EcomReviewRequest;
import com.lemon.erp.modules.ecom.cs.model.CsConstants;
import com.lemon.erp.modules.ecom.cs.model.CsDirection;
import com.lemon.erp.modules.ecom.cs.model.CsRecordSource;
import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;
import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.cs.model.CsSendStatus;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsThreadStatus;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.mapper.EcomCsTicketMapper;
import com.lemon.erp.modules.ecom.mapper.EcomFeedbackMapper;
import com.lemon.erp.modules.ecom.mapper.EcomMessageMapper;
import com.lemon.erp.modules.ecom.mapper.EcomMessageThreadMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReplyTemplateMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReturnMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReviewMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReviewRequestMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 演示数据 · 客服与评价（order 90）：与已有的演示订单关联（买家、订单号、SKU 取自订单）。
 * <ul>
 *   <li>回复模板：在 init.sql 示例模板之外补充差评回复、破损补发、退货流程、尺寸咨询、改地址、好评感谢等（名称 + 语言已存在的跳过）</li>
 *   <li>买家消息：只在支持拉取消息的平台店铺（虾皮、TikTok Shop，按 {@link MessageConnectorRegistry} 判断）生成约 80 个会话，
 *       每个 2–6 条英文往来消息（物流查询、尺寸咨询、破损、退货、改地址、催发货、发票、少件等），状态待回复 / 已回复 / 已关闭；
 *       待回复的都在最近两天，其中部分已过平台回复时限（超时），少数带一条发送失败的卖家回复</li>
 *   <li>评价：约 200 条（亚马逊美 / 德 / 日按站点语言，虾皮为平台同步，其余为导入），大部分 4–5 星；低分评价有处理状态、处理人与处理说明，部分已回复</li>
 *   <li>店铺反馈：约 65 条（亚马逊三个站点与 eBay），低分有卖家回复与处理状态</li>
 *   <li>请求评价：亚马逊发货 5–30 天的订单大部分有请求记录（已发送 / 不符合条件 / 失败，少数未请求），30 天以前的订单为历史请求记录</li>
 *   <li>售后工单：已有的退货退款单逐张登记为工单（来源为买家消息会话或低分评价 / 反馈，同时生成对应的会话或评价），
 *       另外经 {@link EcomOrderService#createManual} 为若干破损 / 发错 / 少件的订单生成补发订单（独立事务，在本部分写入任何数据之前执行）并登记补发工单</li>
 * </ul>
 * 不调用任何平台接口，会话、消息、评价、请求记录直接写本模块的表
 */
@Component
@RequiredArgsConstructor
public class DemoCsContributor implements DemoContributor {

    private static final int CHUNK = 500;
    private static final int FREE_THREADS = 66;
    private static final int PENDING_THREADS = 14;
    private static final int OVERDUE_THREADS = 5;
    private static final int REVIEWS = 200;
    private static final int FEEDBACKS = 65;
    private static final int MAX_RETURN_TICKETS = 26;

    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomReturnMapper returnMapper;
    private final EcomMessageThreadMapper threadMapper;
    private final EcomMessageMapper messageMapper;
    private final EcomReviewMapper reviewMapper;
    private final EcomFeedbackMapper feedbackMapper;
    private final EcomReviewRequestMapper requestMapper;
    private final EcomReplyTemplateMapper templateMapper;
    private final EcomCsTicketMapper ticketMapper;
    private final EcomOrderService orderService;
    private final MessageConnectorRegistry registry;
    private final DemoTx tx;

    @Override
    public int order() {
        return 90;
    }

    @Override
    public String key() {
        return "cs";
    }

    // ---------------------------------------------------------------- 静态文本

    /** 会话场景需要的订单状态 */
    private enum Need {
        /** 已发货之后 */
        SHIPPED,
        /** 发货之前 */
        UNSHIPPED,
        /** 下单之后任意时间 */
        ANY
    }

    /**
     * 会话场景：消息从买家开始、买家 / 卖家交替；变量 {name} {order} {p} {carrier} {tracking} {size} {weight}
     */
    private record Scenario(String key, Need need, String tag, String... lines) {
    }

    private static final Scenario LOGISTICS = new Scenario("LOGISTICS", Need.SHIPPED, "物流查询", // i18n-ignore 演示数据存库
            "Hi, I ordered {p} and it still hasn't arrived. Can you check where my parcel is? Order {order}",
            "Hi {name}, thanks for reaching out. Your order {order} was shipped via {carrier}, tracking number {tracking}. It is in transit and should arrive within 3-5 business days.",
            "The tracking hasn't updated for 4 days. Is it lost?",
            "Sorry for the wait. We have opened an inquiry with {carrier}. Parcels sometimes stay at the sorting center for a few days in peak season. If it does not move within 3 days, we will send a replacement for free.",
            "Got it today, thank you!",
            "Great to hear that! Enjoy your purchase and have a nice day.");
    private static final Scenario SIZE = new Scenario("SIZE", Need.ANY, "尺寸咨询", // i18n-ignore 演示数据存库
            "Hello, what are the exact dimensions of {p}? I need to know if it fits in my bag.",
            "Hi {name}, the package size is {size} cm and it weighs about {weight} kg. Hope this helps!",
            "Is the color exactly like the photos?",
            "Yes, the photos are of the actual product. The color may look slightly different depending on your screen.",
            "OK thanks, waiting for my parcel.",
            "You're welcome! Feel free to message us anytime.");
    private static final Scenario DAMAGED = new Scenario("DAMAGED", Need.SHIPPED, "破损", // i18n-ignore 演示数据存库
            "The {p} I received is damaged. The box was crushed and the item is cracked. What can I do?",
            "Hi {name}, we are so sorry about this. Could you please send us a photo of the damaged item and the outer box? We will arrange a replacement right away.",
            "Photos sent. I would prefer a replacement.",
            "Thank you. We have arranged a free replacement for order {order}. You will receive a new tracking number within 2 days. No need to return the damaged one.",
            "Thank you for the quick response!",
            "It's our pleasure. Sorry again for the inconvenience.");
    private static final Scenario RETURN = new Scenario("RETURN", Need.SHIPPED, "退货", // i18n-ignore 演示数据存库
            "Hi, {p} doesn't work for me. Can I return it for a refund?",
            "Hi {name}, sure. Please submit a return request for order {order} and choose the reason that applies. We will approve it within 24 hours.",
            "I submitted the request. Do I need to pay the return shipping?",
            "If the item is defective we cover the return shipping. Your refund will be issued once we receive the item at our warehouse.",
            "OK, I've dropped off the parcel.",
            "Thank you, we will process the refund as soon as it arrives.");
    private static final Scenario REFUND = new Scenario("REFUND", Need.SHIPPED, "退款", // i18n-ignore 演示数据存库
            "The {p} arrived damaged and doesn't work at all. I'd like a refund.",
            "Hi {name}, we're very sorry. Please send us a photo of the item so we can process your refund.",
            "Photo attached.",
            "Thank you. We have issued a refund for order {order}, no need to return the item. The refund will arrive in 3-7 business days.",
            "Received, thank you.",
            "Thanks for your understanding, sorry again for the trouble.");
    private static final Scenario ADDRESS = new Scenario("ADDRESS", Need.UNSHIPPED, "改地址", // i18n-ignore 演示数据存库
            "Hi, I entered the wrong address for order {order}. Can I change the shipping address?",
            "Hi {name}, your order has not shipped yet. Please send us the correct address and we will update it before dispatch.",
            "The correct one is unit 12-3, same building. Sorry for the trouble!",
            "No problem, we have updated the address. Your order will ship within 24 hours.");
    private static final Scenario DELAY = new Scenario("DELAY", Need.UNSHIPPED, "催发货", // i18n-ignore 演示数据存库
            "When will my order {order} be shipped? I need it by the weekend.",
            "Hi {name}, thanks for your patience. Your order is being packed and will be handed to the carrier within 24 hours.",
            "Can you ship it faster? It's a birthday gift.",
            "We have prioritized your order. Once shipped, delivery usually takes 2-4 days to your area.");
    private static final Scenario WRONG = new Scenario("WRONG", Need.SHIPPED, "发错货", // i18n-ignore 演示数据存库
            "I ordered {p} but received a different color. Can you help?",
            "Hi {name}, we apologize for the mix-up. Could you send us a photo of the item and the label on the package?",
            "Here is the photo. I really need the right one for a gift.",
            "We have confirmed the error. We will send the correct item today with express shipping, and you can keep the other one.",
            "Wow, thank you so much!",
            "You're welcome! Thank you for your patience.");
    private static final Scenario MISSING = new Scenario("MISSING", Need.SHIPPED, "少件", // i18n-ignore 演示数据存库
            "My package arrived but one piece of {p} is missing.",
            "Hi {name}, sorry about that. Could you check under the inner tray? If it is not there, we will send the missing part right away.",
            "I checked, it's not there.",
            "We have arranged to resend the missing part for order {order}. Tracking will be shared once it ships.",
            "Thanks.",
            "Thank you for your understanding.");
    private static final Scenario USAGE = new Scenario("USAGE", Need.SHIPPED, "使用咨询", // i18n-ignore 演示数据存库
            "How do I use {p}? The instructions are not very clear.",
            "Hi {name}, please fully charge or assemble it before first use, following steps 1-3 in the manual. We can also send you a video guide.",
            "Yes please send the video.",
            "Please check the video in the 'How to use' section of the product page. Let us know if you still have questions.",
            "It works now, thanks!",
            "Glad we could help!");
    private static final Scenario INVOICE = new Scenario("INVOICE", Need.ANY, "发票", // i18n-ignore 演示数据存库
            "Hi, can you send me an invoice for order {order}? I need it for my company.",
            "Hi {name}, sure. Please tell us the company name and address to show on the invoice.",
            "Company: Bright Studio, 22 Market St.",
            "Thank you, the invoice has been issued and sent to your registered email.");

    /** 普通会话的场景（按权重重复） */
    private static final Scenario[] FREE_SCENARIOS = {LOGISTICS, LOGISTICS, LOGISTICS, LOGISTICS, SIZE, SIZE, DAMAGED, RETURN, ADDRESS, DELAY,
            DELAY, WRONG, MISSING, USAGE, USAGE, INVOICE};
    private static final String NUDGE = "Hello? Any update on this? Please reply.";
    private static final String[] SEND_ERRORS = {"error_auth: Invalid access_token, please refresh the token", "Read timed out (30000 ms)",
            "error_param: message content contains restricted words"};

    /** 评价文本：语言 → 评分档（0：1–2 星，1：3 星，2：4 星，3：5 星）→ {标题, 内容} */
    private static final Map<String, String[][][]> REVIEW_TEXT = Map.of(
            "en", new String[][][]{
                    {{"Arrived damaged", "The box was crushed and the item was cracked when I opened it. Very disappointed."},
                            {"Stopped working", "Worked for a week and then stopped completely. Not worth the money."},
                            {"Wrong item", "I received a different color than I ordered. Had to contact the seller."},
                            {"Poor quality", "Feels flimsy and one part broke on the first day."},
                            {"Not as described", "The listing says it fits my model but it doesn't. Returning it."},
                            {"Missing parts", "One of the pieces was missing from the package."}},
                    {{"It's okay", "Average quality. It works, but feels a bit cheap compared to the photos."},
                            {"Just average", "Not bad, not great. The color is darker than shown in the listing."},
                            {"Mixed feelings", "The product is fine but shipping took almost three weeks."},
                            {"Decent", "Does the job, but the size is smaller than I expected."}},
                    {{"Good, but a bit pricey", "Works well overall. Wish it came with a carrying pouch."},
                            {"Pretty good", "Solid product. The instructions could be clearer, but it does the job."},
                            {"Nice quality", "Good build quality, slightly smaller than I imagined. Still happy with it."},
                            {"Works as described", "No complaints so far. Took a while to arrive though."},
                            {"Good value", "Does what it should. Minus one star because the packaging was a little dented."}},
                    {{"Exactly what I needed", "Works perfectly and arrived quickly. The quality is much better than I expected for the price."},
                            {"Great quality", "Very well made. I've been using it every day for two weeks with no issues at all."},
                            {"Highly recommend", "Bought one for myself and ordered another as a gift. Packaging was neat and the item looks premium."},
                            {"Five stars", "Does exactly what the description says. Setup took less than a minute."},
                            {"Love it!", "Super happy with this purchase. Color is just like the pictures."},
                            {"Excellent value", "Cheaper than the brand name ones and works just as well. Would buy again."},
                            {"Fast shipping, great product", "Arrived two days earlier than expected. Sturdy and easy to use."},
                            {"Better than expected", "I was skeptical at first but it is really good. My whole family uses it now."}}},
            "de", new String[][][]{
                    {{"Beschädigt angekommen", "Die Verpackung war eingedrückt und das Produkt hatte einen Riss. Sehr enttäuschend."},
                            {"Nach einer Woche defekt", "Hat nur eine Woche funktioniert, dann ging gar nichts mehr."},
                            {"Falscher Artikel", "Ich habe eine andere Farbe bekommen als bestellt."},
                            {"Schlechte Qualität", "Billig verarbeitet, ein Teil ist sofort abgebrochen."}},
                    {{"Geht so", "Durchschnittliche Qualität, die Farbe weicht etwas von den Bildern ab."},
                            {"Mittelmäßig", "Funktioniert, wirkt aber nicht besonders robust."}},
                    {{"Gut, aber nicht perfekt", "Funktioniert gut, die Anleitung ist aber nur auf Englisch."},
                            {"Solide", "Gute Qualität für den Preis, etwas kleiner als erwartet."},
                            {"Empfehlenswert", "Macht, was es soll. Die Lieferung hat einen Tag länger gedauert."}},
                    {{"Top Qualität", "Sehr gut verarbeitet und schnell geliefert. Absolute Kaufempfehlung!"},
                            {"Genau wie beschrieben", "Funktioniert einwandfrei, Preis-Leistung stimmt."},
                            {"Super Produkt", "Bin sehr zufrieden und nutze es täglich. Würde ich wieder kaufen."},
                            {"Klasse!", "Schöne Verpackung, hochwertiges Material und schnelle Lieferung."}}},
            "ja", new String[][][]{
                    {{"破損していました", "箱が潰れていて、本体にヒビが入っていました。残念です。"}, // i18n-ignore 演示数据存库
                            {"すぐに壊れた", "一週間で動かなくなりました。"}, // i18n-ignore 演示数据存库
                            {"色違いが届いた", "注文した色と違う商品が届きました。"}, // i18n-ignore 演示数据存库
                            {"品質が悪い", "部品がすぐに外れてしまいました。"}}, // i18n-ignore 演示数据存库
                    {{"普通です", "可もなく不可もなく。写真より少し色が濃いです。"}, // i18n-ignore 演示数据存库
                            {"まあまあ", "使えますが、作りが少し安っぽく感じます。"}}, // i18n-ignore 演示数据存库
                    {{"概ね満足", "使いやすいですが、説明書が少し分かりにくかったです。"}, // i18n-ignore 演示数据存库
                            {"良い商品", "品質は良いです。思ったより少し小さめでした。"}, // i18n-ignore 演示数据存库
                            {"コスパ良し", "値段を考えると十分です。配送が一日遅れました。"}}, // i18n-ignore 演示数据存库
                    {{"とても満足です", "品質が良く、届くのも早かったです。リピートします。"}, // i18n-ignore 演示数据存库
                            {"期待以上", "価格以上の品質で、毎日使っています。おすすめです。"}, // i18n-ignore 演示数据存库
                            {"買ってよかった", "説明通りの商品で、梱包も丁寧でした。"}, // i18n-ignore 演示数据存库
                            {"最高です", "デザインも使いやすさも満足しています。"}}}); // i18n-ignore 演示数据存库

    /** 卖家回复：语言 → {好评回复, 差评回复} */
    private static final Map<String, String[]> REPLY_TEXT = Map.of(
            "en", new String[]{"Thank you so much for your kind review! We're glad you love it.",
                    "We're sorry about your experience. Please contact us through your order page and we will make it right with a replacement or a refund."},
            "de", new String[]{"Vielen Dank für Ihre tolle Bewertung! Wir freuen uns, dass Sie zufrieden sind.",
                    "Es tut uns sehr leid. Bitte kontaktieren Sie uns über Ihre Bestellung, wir finden sofort eine Lösung (Ersatz oder Erstattung)."},
            "ja", new String[]{"高評価をいただきありがとうございます！今後ともよろしくお願いいたします。", // i18n-ignore 演示数据存库
                    "ご迷惑をおかけし申し訳ございません。注文ページからご連絡いただければ、交換または返金にて対応いたします。"}); // i18n-ignore 演示数据存库

    /** 店铺反馈留言：语言 → 评分档 → 内容 */
    private static final Map<String, String[][]> FEEDBACK_TEXT = Map.of(
            "en", new String[][]{
                    {"Item arrived damaged, waiting for seller response.", "Order arrived very late.", "Wrong item sent."},
                    {"Item OK but arrived late."},
                    {"Good item, delivery took a bit longer.", "Item fine, packaging could be better."},
                    {"Fast shipping, item as described. Great seller!", "Excellent communication and quick delivery.",
                            "Arrived early and well packed. Thanks!", "Great seller, would buy again."}},
            "de", new String[][]{
                    {"Artikel beschädigt angekommen.", "Falscher Artikel geliefert."},
                    {"Lieferung hat zu lange gedauert."},
                    {"Ware gut, Versand etwas langsam."},
                    {"Schnelle Lieferung, alles bestens!", "Top Verkäufer, gerne wieder."}},
            "ja", new String[][]{
                    {"商品が破損していました。", "注文と違う商品が届きました。"}, // i18n-ignore 演示数据存库
                    {"配送が遅かったです。"}, // i18n-ignore 演示数据存库
                    {"商品は良いですが、到着が少し遅れました。"}, // i18n-ignore 演示数据存库
                    {"迅速な対応ありがとうございました。", "丁寧な梱包で安心しました。"}}); // i18n-ignore 演示数据存库

    private static final String[] RESOLVED_NOTES = {"已联系买家补发新品，买家已将评分改为 4 星", "已全额退款，买家表示满意", // i18n-ignore 演示数据存库
            "批次外箱偏薄，已通知供应商加固包装", "已公开回复并提供使用指导，买家问题已解决", // i18n-ignore 演示数据存库
            "买家未回复，已在评价下公开说明处理方式", "品质问题已登记，已反馈采购跟进来料质检"}; // i18n-ignore 演示数据存库
    private static final String[] PROCESSING_NOTES = {"已联系买家，等待买家回复照片", "已转售后，等待退货入库后检测", // i18n-ignore 演示数据存库
            "已向平台申请移除（评价内容与商品无关）"}; // i18n-ignore 演示数据存库

    /** 补充的回复模板：{名称, 语言, 平台（为空不限）, 场景, 内容, 状态 1 / 0} */
    private static final String[][] TEMPLATES = {
            {"差评回复", "de", null, "差评回复", "Liebe/r {buyerName}, es tut uns leid, dass {productName} Ihre Erwartungen nicht erfüllt hat. Bitte kontaktieren Sie uns mit Ihrer Bestellnummer {orderNo}, wir senden sofort Ersatz oder erstatten den Betrag.", "1"}, // i18n-ignore 演示数据存库
            {"差评回复", "ja", null, "差评回复", "{buyerName} 様、この度は {productName} がご期待に沿えず申し訳ございません。ご注文番号 {orderNo} にてご連絡いただければ、交換または返金にて対応いたします。{storeName}", "1"}, // i18n-ignore 演示数据存库
            {"破损补发", "en", null, "破损补发", "Hi {buyerName}, we are very sorry that your {productName} arrived damaged. We have arranged a free replacement for order {orderNo}; there is no need to return the damaged item. The new tracking number will be sent within 2 days. {storeName}", "1"}, // i18n-ignore 演示数据存库
            {"破损补发", "de", null, "破损补发", "Hallo {buyerName}, es tut uns leid, dass {productName} beschädigt angekommen ist. Wir senden Ihnen kostenlos Ersatz für Bestellung {orderNo}, eine Rücksendung ist nicht nötig. {storeName}", "1"}, // i18n-ignore 演示数据存库
            {"退货流程", "en", null, "退货退款", "Hi {buyerName}, please submit a return request for order {orderNo} on the platform and choose the reason that applies. Once the item arrives at our warehouse, the refund will be issued within 48 hours. {storeName}", "1"}, // i18n-ignore 演示数据存库
            {"尺寸咨询", "en", null, "售前咨询", "Hi {buyerName}, thanks for your interest in {productName}. The size and weight are listed in the product details; feel free to tell us your use case and we will help you check whether it fits. {storeName}", "1"}, // i18n-ignore 演示数据存库
            {"修改地址", "en", "SHOPEE", "改地址", "Hi {buyerName}, your order {orderNo} has not shipped yet. Please send us the full new address (with postcode and phone number) and we will update it before dispatch.", "1"}, // i18n-ignore 演示数据存库
            {"延迟发货致歉", "en", null, "发货延迟", "Hi {buyerName}, sorry for the delay with order {orderNo}. Your parcel will be handed to {carrier} within 24 hours and we will share the tracking number as soon as it ships. {storeName}", "1"}, // i18n-ignore 演示数据存库
            {"好评感谢", "en", "AMAZON", "好评感谢", "Hi {buyerName}, thank you for choosing {storeName}! We hope you enjoy your {productName}. If anything is not perfect, just reply to this message and we will help right away.", "1"}, // i18n-ignore 演示数据存库
            {"好评感谢", "ja", "AMAZON", "好评感谢", "{buyerName} 様、{storeName} をご利用いただきありがとうございます。{productName} についてご不明な点がございましたら、お気軽にお問い合わせください。", "1"}, // i18n-ignore 演示数据存库
            {"物流查询", "fr", null, "物流查询", "Bonjour {buyerName}, merci pour votre message. Votre commande {orderNo} a été expédiée avec {carrier}, numéro de suivi {trackingNo}. Cordialement, {storeName}", "1"}, // i18n-ignore 演示数据存库
            {"节日促销通知", "en", "SHOPIFY", "营销", "Hi {buyerName}, our holiday sale is live at {storeName}: up to 30% off on bestsellers this week only.", "0"}}; // i18n-ignore 演示数据存库

    private static final String TICKET_RESEND_DAMAGED = "外箱压坏导致商品破损，免费补发一件，无需退回"; // i18n-ignore 演示数据存库
    private static final String TICKET_RESEND_WRONG = "发错颜色，补发正确款式，错发商品赠送买家"; // i18n-ignore 演示数据存库
    private static final String TICKET_RESEND_MISSING = "少发配件，补发缺失部分"; // i18n-ignore 演示数据存库
    private static final Map<String, String> RETURN_REMARK = Map.of(
            "QUALITY", "质量问题（使用一周后无法开机），同意退货退款", // i18n-ignore 演示数据存库
            "DAMAGED", "运输破损，买家已提供照片", // i18n-ignore 演示数据存库
            "WRONG_ITEM", "发错货，买家要求退款", // i18n-ignore 演示数据存库
            "NOT_AS_DESCRIBED", "与描述不符（尺寸偏小）", // i18n-ignore 演示数据存库
            "NOT_NEEDED", "买家不想要了，退货运费由买家承担", // i18n-ignore 演示数据存库
            "LATE", "送达太迟，买家申请退款"); // i18n-ignore 演示数据存库
    private static final String RETURN_REMARK_DEFAULT = "买家申请售后，已同意"; // i18n-ignore 演示数据存库
    private static final String RESEND_ORDER_REMARK = "补发：原订单 "; // i18n-ignore 演示数据存库

    // ---------------------------------------------------------------- 生成

    /** 一次生成的状态 */
    private final class Run {
        final DemoContext ctx;
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final Map<Long, DemoCatalog.Store> storeById = new HashMap<>();
        final Map<Long, String> codeById = new HashMap<>();
        final Map<Long, List<EcomOrder>> ordersByStore = new LinkedHashMap<>();
        final Map<Long, EcomOrder> orderById = new HashMap<>();
        final Map<Long, List<EcomOrderItem>> items = new HashMap<>();
        final Map<Long, Map<String, DemoListings.Item>> listings;
        final Map<String, Long> owners;
        final List<Long> users;
        final Map<String, Long> templateIds = new HashMap<>();
        final Set<Long> threadOrders = new HashSet<>();
        final Set<Long> reviewOrders = new HashSet<>();
        final Set<Long> feedbackOrders = new HashSet<>();
        final Set<String> ids = new HashSet<>();
        long seq;
        int threads, messages, reviews, feedbacks, requests, tickets, templates;

        Run(DemoContext ctx) {
            this.ctx = ctx;
            Map<Long, Map<String, DemoListings.Item>> l = ctx.get(DemoContributors.LISTINGS);
            this.listings = l == null ? Map.of() : l;
            Map<String, Long> o = ctx.get(DemoContributors.STORE_OWNER);
            this.owners = o == null ? Map.of() : o;
            List<Long> u = ctx.get(DemoKeys.OPERATOR_USER_IDS);
            this.users = u == null || u.isEmpty() ? List.of(SecurityUtils.getUserId()) : u;
            this.seq = 1_000_000_000L + ctx.between(0, 99_999_999);
        }

        Long user() {
            return users.get(ctx.random().nextInt(users.size()));
        }

        Long owner(Long storeId) {
            Long id = owners.get(codeById.get(storeId));
            return id != null ? id : users.get(0);
        }

        /** 平台侧唯一 ID */
        String uid(String prefix) {
            String id;
            do {
                id = prefix + (seq += ctx.between(1, 997));
            } while (!ids.add(id));
            return id;
        }

        <T> T pick(List<T> list) {
            return list.get(ctx.random().nextInt(list.size()));
        }

        LocalDateTime minus(int minutes) {
            return now.minusMinutes(minutes).withSecond(ctx.between(0, 59));
        }
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> storeByCode = ctx.get(DemoKeys.STORE_BY_CODE);
        List<Long> orderIds = ctx.get(DemoKeys.ORDER_IDS);
        if (storeByCode == null || storeByCode.isEmpty() || orderIds == null || orderIds.isEmpty()) {
            return;
        }
        Run r = new Run(ctx);
        for (DemoCatalog.Store s : DemoCatalog.STORES) {
            Long id = storeByCode.get(s.code());
            if (id != null) {
                r.storeById.put(id, s);
                r.codeById.put(id, s.code());
            }
        }
        loadOrders(r, new HashSet<>(orderIds));
        if (r.orderById.isEmpty()) {
            return;
        }
        // 补发订单在独立事务中创建，必须在本部分写入任何数据之前执行
        List<TicketPlan> plans = new ArrayList<>(returnTickets(r));
        plans.addAll(resendTickets(r, Math.max(6, Math.min(14, 24 - plans.size()))));
        templates(r);
        for (TicketPlan p : plans) {
            source(r, p);
        }
        freeThreads(r);
        freeReviews(r);
        freeFeedbacks(r);
        requests(r);
        for (TicketPlan p : plans) {
            insertTicket(r, p);
        }
    }

    /** 演示店铺的平台订单（演示订单 ID 范围内）与明细 */
    private void loadOrders(Run r, Set<Long> orderIds) {
        List<EcomOrder> list = orderMapper.selectList(Wrappers.<EcomOrder>lambdaQuery()
                .select(EcomOrder::getId, EcomOrder::getStoreId, EcomOrder::getPlatformOrderNo, EcomOrder::getOrderTime, EcomOrder::getStatus,
                        EcomOrder::getLocalStatus, EcomOrder::getBuyerName, EcomOrder::getBuyerEmail, EcomOrder::getShipName, EcomOrder::getShipPhone,
                        EcomOrder::getShipCountry, EcomOrder::getShipState, EcomOrder::getShipCity, EcomOrder::getShipPostcode,
                        EcomOrder::getShipAddress, EcomOrder::getCurrency, EcomOrder::getCarrier, EcomOrder::getTrackingNo, EcomOrder::getShipTime,
                        EcomOrder::getOrderType)
                .in(EcomOrder::getStoreId, r.storeById.keySet()).orderByAsc(EcomOrder::getOrderTime).orderByAsc(EcomOrder::getId));
        for (EcomOrder o : list) {
            if (orderIds.contains(o.getId()) && o.getOrderType() != EcomOrderType.RESEND && o.getOrderTime() != null) {
                r.orderById.put(o.getId(), o);
                r.ordersByStore.computeIfAbsent(o.getStoreId(), k -> new ArrayList<>()).add(o);
            }
        }
        List<Long> ids = new ArrayList<>(r.orderById.keySet());
        for (int i = 0; i < ids.size(); i += CHUNK) {
            List<Long> part = ids.subList(i, Math.min(ids.size(), i + CHUNK));
            itemMapper.selectList(Wrappers.<EcomOrderItem>lambdaQuery()
                            .select(EcomOrderItem::getId, EcomOrderItem::getOrderId, EcomOrderItem::getPlatformLineId, EcomOrderItem::getSellerSku,
                                    EcomOrderItem::getPlatformItemId, EcomOrderItem::getTitle, EcomOrderItem::getQty, EcomOrderItem::getPrice)
                            .in(EcomOrderItem::getOrderId, part).orderByAsc(EcomOrderItem::getId))
                    .forEach(it -> r.items.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it));
        }
    }

    // ---------------------------------------------------------------- 工具

    private static boolean shipped(EcomOrder o) {
        return o.getStatus() == EcomOrderStatus.SHIPPED;
    }

    /** 发货时间（平台配送订单没有时按下单后半天） */
    private static LocalDateTime shipOf(EcomOrder o) {
        return o.getShipTime() != null ? o.getShipTime() : o.getOrderTime().plusHours(12);
    }

    private static String lang(DemoCatalog.Store s) {
        return switch (s.marketplace()) {
            case "DE" -> "de";
            case "JP" -> "ja";
            default -> "en";
        };
    }

    private boolean messaging(DemoCatalog.Store s) {
        return registry.supports(s.platform(), CsCapability.MESSAGES);
    }

    private int replyHours(EcomPlatform p) {
        return registry.find(p).map(MessageConnector::replyHours).orElse(CsConstants.DEFAULT_REPLY_HOURS);
    }

    private EcomOrderItem firstItem(Run r, EcomOrder o) {
        List<EcomOrderItem> list = r.items.get(o.getId());
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private DemoListings.Item listing(Run r, Long storeId, String sku) {
        Map<String, DemoListings.Item> m = r.listings.get(storeId);
        return m == null || sku == null ? null : m.get(sku);
    }

    private static String firstName(String buyer) {
        if (buyer == null || buyer.isBlank()) {
            return "there";
        }
        return buyer.trim().split("\\s+")[0];
    }

    /** 店铺中满足条件的随机订单（最多尝试 80 次），没有为 null */
    private EcomOrder pickOrder(Run r, List<Long> storeIds, java.util.function.Predicate<EcomOrder> ok) {
        for (int i = 0; i < 80; i++) {
            List<EcomOrder> list = r.ordersByStore.get(r.pick(storeIds));
            if (list == null || list.isEmpty()) {
                continue;
            }
            EcomOrder o = r.pick(list);
            if (ok.test(o)) {
                return o;
            }
        }
        return null;
    }

    private static int bucket(int rating) {
        return rating <= 2 ? 0 : rating - 2;
    }

    private int rating(DemoContext ctx) {
        int x = ctx.between(1, 100);
        return x <= 58 ? 5 : x <= 80 ? 4 : x <= 88 ? 3 : x <= 93 ? 2 : 1;
    }

    // ---------------------------------------------------------------- 回复模板

    private void templates(Run r) {
        int sort = 10;
        for (String[] t : TEMPLATES) {
            sort++;
            if (templateMapper.exists(Wrappers.<EcomReplyTemplate>lambdaQuery().eq(EcomReplyTemplate::getName, t[0])
                    .eq(EcomReplyTemplate::getLanguage, t[1]))) {
                continue;
            }
            EcomReplyTemplate e = new EcomReplyTemplate();
            e.setName(t[0]);
            e.setLanguage(t[1]);
            e.setPlatform(t[2] == null ? null : EcomPlatform.valueOf(t[2]));
            e.setScene(t[3]);
            e.setContent(t[4]);
            e.setSort(sort);
            e.setStatus(EnableStatus.of("1".equals(t[5])));
            e.setCreateBy(r.users.get(0));
            templateMapper.insert(e);
            r.templates++;
        }
        for (EcomReplyTemplate e : templateMapper.selectList(Wrappers.<EcomReplyTemplate>lambdaQuery()
                .select(EcomReplyTemplate::getId, EcomReplyTemplate::getName, EcomReplyTemplate::getLanguage))) {
            r.templateIds.put(e.getName() + "|" + e.getLanguage(), e.getId());
        }
    }

    // ---------------------------------------------------------------- 售后工单

    /** 工单计划：来源记录在 {@link #source} 中生成 */
    private static final class TicketPlan {
        EcomOrder order;
        CsTicketType type;
        LocalDateTime time;
        EcomReturn ret;
        Long resendOrderId;
        String resendOrderNo;
        Scenario scenario;
        String remark;
        CsSourceType sourceType;
        Long sourceId;
    }

    /** 已有的退货退款单逐张登记为工单 */
    private List<TicketPlan> returnTickets(Run r) {
        List<EcomReturn> returns = returnMapper.selectList(Wrappers.<EcomReturn>lambdaQuery()
                .select(EcomReturn::getId, EcomReturn::getDocNo, EcomReturn::getOrderId, EcomReturn::getReturnType, EcomReturn::getReason,
                        EcomReturn::getCreateTime)
                .in(EcomReturn::getStoreId, r.storeById.keySet()).orderByAsc(EcomReturn::getId));
        List<EcomReturn> usable = new ArrayList<>(returns.stream().filter(x -> r.orderById.containsKey(x.getOrderId())).toList());
        Collections.shuffle(usable, r.ctx.random());
        List<TicketPlan> out = new ArrayList<>();
        Set<Long> orders = new HashSet<>();
        for (EcomReturn ret : usable) {
            if (out.size() >= MAX_RETURN_TICKETS || !orders.add(ret.getOrderId())) {
                continue;
            }
            TicketPlan p = new TicketPlan();
            p.order = r.orderById.get(ret.getOrderId());
            p.ret = ret;
            p.type = ret.getReturnType() == ReturnType.REFUND_ONLY ? CsTicketType.REFUND_ONLY : CsTicketType.RETURN_REFUND;
            LocalDateTime t = ret.getCreateTime() == null || ret.getCreateTime().isAfter(r.now) ? r.now.minusMinutes(5) : ret.getCreateTime();
            p.time = t.isAfter(p.order.getOrderTime()) ? t : p.order.getOrderTime().plusHours(2);
            p.scenario = p.type == CsTicketType.REFUND_ONLY ? REFUND : RETURN;
            p.remark = RETURN_REMARK.getOrDefault(ret.getReason(), RETURN_REMARK_DEFAULT);
            out.add(p);
        }
        return out;
    }

    /** 补发：为已发货的订单生成补发订单（独立事务，与「转售后」相同的做法），失败的跳过 */
    private List<TicketPlan> resendTickets(Run r, int count) {
        List<Long> msgStores = r.storeById.entrySet().stream().filter(e -> messaging(e.getValue())).map(Map.Entry::getKey).toList();
        List<Long> amzStores = r.storeById.entrySet().stream().filter(e -> e.getValue().platform() == EcomPlatform.AMAZON).map(Map.Entry::getKey).toList();
        List<TicketPlan> out = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (int i = 0; i < count; i++) {
            List<Long> stores = !msgStores.isEmpty() && (amzStores.isEmpty() || r.ctx.chance(0.7)) ? msgStores : amzStores;
            if (stores.isEmpty()) {
                break;
            }
            LocalDateTime time = r.minus(r.ctx.between(120, 12 * 24 * 60));
            EcomOrder o = pickOrder(r, stores, x -> shipped(x) && !used.contains(x.getId()) && r.items.containsKey(x.getId())
                    && shipOf(x).isBefore(time.minusDays(3)) && shipOf(x).isAfter(time.minusDays(40)));
            if (o == null) {
                continue;
            }
            used.add(o.getId());
            TicketPlan p = new TicketPlan();
            p.order = o;
            p.type = CsTicketType.RESEND;
            p.time = time;
            p.scenario = r.ctx.pick(DAMAGED, DAMAGED, WRONG, MISSING);
            p.remark = p.scenario == WRONG ? TICKET_RESEND_WRONG : p.scenario == MISSING ? TICKET_RESEND_MISSING : TICKET_RESEND_DAMAGED;
            String no = o.getPlatformOrderNo() + "-RS1";
            Long[] created = new Long[1];
            tx.run("cs-resend", () -> created[0] = orderService.createManual(resendDraft(r, o, no, time), RESEND_ORDER_REMARK + o.getPlatformOrderNo()));
            if (created[0] == null) {
                continue;
            }
            p.resendOrderId = created[0];
            p.resendOrderNo = no;
            out.add(p);
        }
        return out;
    }

    /** 补发订单：原订单的收件信息，第一行 1 件，金额 0，自发货 */
    private OrderDraft resendDraft(Run r, EcomOrder o, String no, LocalDateTime time) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(o.getStoreId());
        d.setPlatformOrderNo(no);
        d.setOrderTime(time);
        d.setPaidTime(time);
        d.setStatus(EcomOrderStatus.UNSHIPPED);
        d.setFulfillment(FulfillmentType.SELLER);
        d.setBuyerName(o.getBuyerName());
        d.setBuyerEmail(o.getBuyerEmail());
        d.setShipName(o.getShipName());
        d.setShipPhone(o.getShipPhone());
        d.setShipCountry(o.getShipCountry());
        d.setShipState(o.getShipState());
        d.setShipCity(o.getShipCity());
        d.setShipPostcode(o.getShipPostcode());
        d.setShipAddress(o.getShipAddress());
        d.setCurrency(o.getCurrency());
        d.setShippingAmount(BigDecimal.ZERO);
        d.setTaxAmount(BigDecimal.ZERO);
        d.setOrderType(EcomOrderType.RESEND);
        d.setSourceOrderId(o.getId());
        EcomOrderItem oi = r.items.get(o.getId()).get(0);
        OrderDraft.Line line = new OrderDraft.Line();
        line.setPlatformLineId(oi.getPlatformLineId());
        line.setSellerSku(oi.getSellerSku());
        line.setPlatformItemId(oi.getPlatformItemId());
        line.setTitle(oi.getTitle());
        line.setQty(BigDecimal.ONE);
        line.setPrice(oi.getPrice() == null ? BigDecimal.ZERO : oi.getPrice());
        line.setAmount(BigDecimal.ZERO);
        d.getLines().add(line);
        return d;
    }

    /** 工单的来源：支持消息的店铺生成会话，否则生成低分评价或店铺反馈 */
    private void source(Run r, TicketPlan p) {
        DemoCatalog.Store store = r.storeById.get(p.order.getStoreId());
        boolean recent = p.time.isAfter(r.now.minusDays(7));
        if (messaging(store) && r.threadOrders.add(p.order.getId())) {
            LocalDateTime end = p.time.minusMinutes(r.ctx.between(10, 90));
            LocalDateTime lo = window(r, p.order, p.scenario.need(), end)[0];
            if (lo != null && lo.isBefore(end.minusMinutes(30))) {
                int len = p.scenario.lines().length >= 6 && r.ctx.chance(0.5) ? 6 : 4;
                p.sourceType = CsSourceType.THREAD;
                p.sourceId = insertThread(r, p.order, p.scenario, len, recent ? CsThreadStatus.REPLIED : CsThreadStatus.CLOSED, lo, end, false, false);
                return;
            }
            r.threadOrders.remove(p.order.getId());
        }
        boolean done = p.ret == null ? recent && r.ctx.chance(0.4) : !recent && r.ctx.chance(0.7);
        CsReviewStatus status = done ? CsReviewStatus.RESOLVED : CsReviewStatus.PROCESSING;
        LocalDate date = p.time.toLocalDate().minusDays(r.ctx.between(0, 1));
        LocalDate min = p.order.getOrderTime().toLocalDate();
        if (date.isBefore(min)) {
            date = min;
        }
        int rating = r.ctx.between(1, 2);
        boolean feedbackStore = store.platform() == EcomPlatform.AMAZON || store.platform() == EcomPlatform.EBAY;
        if (feedbackStore && r.ctx.chance(0.4) && r.feedbackOrders.add(p.order.getId())) {
            p.sourceType = CsSourceType.FEEDBACK;
            p.sourceId = insertFeedback(r, p.order, rating, date, status);
        } else {
            r.reviewOrders.add(p.order.getId());
            p.sourceType = CsSourceType.REVIEW;
            p.sourceId = insertReview(r, p.order, rating, date, status);
        }
    }

    private void insertTicket(Run r, TicketPlan p) {
        if (p.sourceId == null) {
            return;
        }
        EcomCsTicket k = new EcomCsTicket();
        k.setStoreId(p.order.getStoreId());
        k.setSourceType(p.sourceType);
        k.setSourceId(p.sourceId);
        k.setTicketType(p.type);
        k.setOrderId(p.order.getId());
        k.setPlatformOrderNo(p.order.getPlatformOrderNo());
        if (p.ret != null) {
            k.setReturnId(p.ret.getId());
            k.setReturnNo(p.ret.getDocNo());
        }
        k.setResendOrderId(p.resendOrderId);
        k.setResendOrderNo(p.resendOrderNo);
        k.setRemark(p.remark);
        k.setDeptId(SecurityUtils.getLoginUser().deptId());
        k.setCreateBy(r.user());
        k.setCreateTime(p.time);
        k.setUpdateTime(p.time);
        ticketMapper.insert(k);
        r.tickets++;
        if (p.resendOrderId != null) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getSourceTicketId, k.getId()).eq(EcomOrder::getId, p.resendOrderId));
        }
    }

    // ---------------------------------------------------------------- 买家消息

    /**
     * 会话的时间范围 {最早, 最晚}：已发货场景在发货后、发货前场景在下单到发货之间、其余在下单之后；最晚不超过 end（为空取现在）。
     * 范围不成立时最早为 null
     */
    private LocalDateTime[] window(Run r, EcomOrder o, Need need, LocalDateTime end) {
        LocalDateTime hi = end != null ? end : r.now.minusMinutes(10);
        LocalDateTime lo;
        switch (need) {
            case SHIPPED -> {
                if (!shipped(o)) {
                    return new LocalDateTime[]{null, hi};
                }
                lo = shipOf(o).plusHours(r.ctx.between(20, 120));
            }
            case UNSHIPPED -> {
                if (o.getStatus() == EcomOrderStatus.CANCELLED) {
                    return new LocalDateTime[]{null, hi};
                }
                lo = o.getOrderTime().plusMinutes(r.ctx.between(15, 180));
                if (o.getShipTime() != null && o.getShipTime().minusMinutes(30).isBefore(hi)) {
                    hi = o.getShipTime().minusMinutes(30);
                }
            }
            default -> lo = o.getOrderTime().plusHours(r.ctx.between(1, 30));
        }
        return new LocalDateTime[]{lo.isBefore(hi) ? lo : null, hi};
    }

    /** 不关联工单的会话：待回复（最近两天，部分超时）、已回复、已关闭 */
    private void freeThreads(Run r) {
        List<Long> stores = r.storeById.entrySet().stream().filter(e -> messaging(e.getValue())).map(Map.Entry::getKey).toList();
        if (stores.isEmpty()) {
            return;
        }
        List<CsThreadStatus> plan = new ArrayList<>();
        for (int i = 0; i < FREE_THREADS; i++) {
            plan.add(i < PENDING_THREADS ? CsThreadStatus.PENDING : i % 2 == 0 ? CsThreadStatus.REPLIED : CsThreadStatus.CLOSED);
        }
        int overdue = 0, failed = 0;
        for (CsThreadStatus status : plan) {
            boolean late = status == CsThreadStatus.PENDING && overdue < OVERDUE_THREADS;
            for (int attempt = 0; attempt < 6; attempt++) {
                Scenario sc = r.ctx.pick(FREE_SCENARIOS);
                EcomOrder o = pickOrder(r, stores, x -> !r.threadOrders.contains(x.getId()) && r.items.containsKey(x.getId()));
                if (o == null) {
                    continue;
                }
                LocalDateTime end;
                if (status == CsThreadStatus.PENDING) {
                    int hours = replyHours(r.storeById.get(o.getStoreId()).platform());
                    end = late ? r.minus(Math.min(44 * 60, hours * 60 + r.ctx.between(30, 18 * 60)))
                            : r.minus(r.ctx.between(5, Math.max(20, hours * 60 / 2)));
                } else {
                    end = null;
                }
                LocalDateTime[] w = window(r, o, sc.need(), end);
                if (w[0] == null) {
                    continue;
                }
                LocalDateTime lo = w[0], hi = w[1];
                if (end == null) {
                    // 已回复：尽量在最近 25 天；已关闭：至少两天前
                    LocalDateTime from = status == CsThreadStatus.REPLIED && lo.isBefore(r.now.minusDays(25)) ? r.now.minusDays(25) : lo.plusHours(3);
                    LocalDateTime to = status == CsThreadStatus.CLOSED && hi.isAfter(r.now.minusDays(2)) ? r.now.minusDays(2) : hi;
                    if (!from.isBefore(to)) {
                        continue;
                    }
                    end = from.plusMinutes(r.ctx.random().nextLong(Duration.between(from, to).toMinutes() + 1));
                } else if (end.isAfter(hi) || !lo.isBefore(end.minusMinutes(60))) {
                    continue;
                }
                int max = sc.lines().length;
                int len;
                if (status == CsThreadStatus.PENDING) {
                    len = late && r.ctx.chance(0.4) ? 1 : max >= 5 && r.ctx.chance(0.5) ? 5 : 3;
                } else if (status == CsThreadStatus.REPLIED) {
                    len = max >= 6 && r.ctx.chance(0.4) ? 6 : r.ctx.chance(0.6) ? 4 : 2;
                } else {
                    len = Math.min(max, r.ctx.pick(2, 4, 5, 6));
                }
                boolean fail = status == CsThreadStatus.PENDING && !late && failed < 3 && r.ctx.chance(0.4);
                r.threadOrders.add(o.getId());
                insertThread(r, o, sc, len, status, lo, end, late && len == 1, fail);
                if (late) {
                    overdue++;
                }
                if (fail) {
                    failed++;
                }
                break;
            }
        }
    }

    /**
     * 写入一个会话与消息。
     *
     * @param len    取场景的前几条消息（买家、卖家交替）
     * @param lo     第一条消息不早于该时间
     * @param end    最后一条（非发送失败的）消息时间
     * @param nudge  末尾追加一条买家催促
     * @param failed 末尾追加一条发送失败的卖家回复
     */
    private Long insertThread(Run r, EcomOrder o, Scenario sc, int len, CsThreadStatus status, LocalDateTime lo, LocalDateTime end,
                              boolean nudge, boolean failed) {
        DemoCatalog.Store store = r.storeById.get(o.getStoreId());
        boolean shopee = store.platform() == EcomPlatform.SHOPEE;
        Map<String, String> vars = vars(r, o);
        List<CsDirection> dirs = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            dirs.add(i % 2 == 0 ? CsDirection.BUYER : CsDirection.SELLER);
            texts.add(fill(sc.lines()[i], vars));
        }
        if (nudge) {
            dirs.add(CsDirection.BUYER);
            texts.add(NUDGE);
        }
        List<LocalDateTime> times = timeline(r, dirs, lo, end);
        Long seller = r.owner(o.getStoreId());
        EcomMessageThread t = new EcomMessageThread();
        t.setStoreId(o.getStoreId());
        t.setPlatform(store.platform());
        t.setConversationId(r.uid(shopee ? "9" : "73"));
        t.setBuyerId(r.uid(shopee ? "" : "70"));
        t.setBuyerName(o.getBuyerName());
        t.setOrderId(o.getId());
        t.setPlatformOrderNo(o.getPlatformOrderNo());
        int last = dirs.size() - 1;
        t.setLastMessage(preview(texts.get(last)));
        t.setLastMessageTime(times.get(last));
        int trailing = 0;
        for (int i = last; i >= 0 && dirs.get(i) == CsDirection.BUYER; i--) {
            trailing++;
        }
        CsThreadStatus st = status == CsThreadStatus.CLOSED ? CsThreadStatus.CLOSED : trailing > 0 ? CsThreadStatus.PENDING : CsThreadStatus.REPLIED;
        t.setStatus(st);
        t.setUnread(st == CsThreadStatus.PENDING && r.ctx.chance(0.75) ? trailing : 0);
        t.setReplyDeadline(st == CsThreadStatus.PENDING ? times.get(last - trailing + 1).plusHours(replyHours(store.platform())) : null);
        t.setOwnerId(seller);
        t.setTags(sc.tag());
        t.setCreateTime(times.get(0));
        t.setUpdateTime(times.get(last));
        threadMapper.insert(t);
        r.threads++;
        Long logisticsTemplate = r.templateIds.get("物流查询|en"); // i18n-ignore 演示数据存库
        for (int i = 0; i <= last; i++) {
            EcomMessage m = message(r, t.getId(), dirs.get(i), texts.get(i), times.get(i), shopee);
            if (dirs.get(i) == CsDirection.SELLER) {
                m.setSenderId(r.ctx.chance(0.6) ? seller : r.user());
                m.setTemplateId(sc == LOGISTICS && i == 1 ? logisticsTemplate : null);
            }
            messageMapper.insert(m);
            r.messages++;
        }
        if (failed) {
            LocalDateTime at = times.get(last).plusMinutes(r.ctx.between(3, 30));
            EcomMessage m = message(r, t.getId(), CsDirection.SELLER, fill(sc.lines()[Math.min(len, sc.lines().length - 1)], vars),
                    at.isAfter(r.now) ? r.now : at, shopee);
            m.setPlatformMessageId(null);
            m.setSendStatus(CsSendStatus.FAILED);
            m.setErrorMessage(r.ctx.pick(SEND_ERRORS));
            m.setSenderId(r.user());
            messageMapper.insert(m);
            r.messages++;
        }
        return t.getId();
    }

    private EcomMessage message(Run r, Long threadId, CsDirection dir, String content, LocalDateTime time, boolean shopee) {
        EcomMessage m = new EcomMessage();
        m.setThreadId(threadId);
        m.setPlatformMessageId(r.uid(shopee ? "2" : "75"));
        m.setDirection(dir);
        m.setMsgType(shopee ? "text" : "TEXT");
        m.setContent(content);
        m.setSentTime(time);
        m.setSendStatus(dir == CsDirection.SELLER ? CsSendStatus.SENT : null);
        m.setCreateTime(time);
        m.setUpdateTime(time);
        return m;
    }

    /** 消息时间：从 end 往前按间隔排列（卖家回复 20 分钟–10 小时，买家再发 1–30 小时），放不下时按比例压缩到 [lo, end] */
    private List<LocalDateTime> timeline(Run r, List<CsDirection> dirs, LocalDateTime lo, LocalDateTime end) {
        int n = dirs.size();
        long[] gaps = new long[n];
        long total = 0;
        for (int i = 1; i < n; i++) {
            gaps[i] = dirs.get(i) == CsDirection.SELLER ? r.ctx.between(20, 600)
                    : dirs.get(i - 1) == CsDirection.BUYER ? r.ctx.between(300, 900) : r.ctx.between(60, 1800);
            total += gaps[i];
        }
        long avail = Math.max(0, Duration.between(lo, end).getSeconds());
        double scale = total * 60 > avail * 0.95 && total > 0 ? avail * 0.95 / (total * 60.0) : 1;
        LocalDateTime[] t = new LocalDateTime[n];
        t[n - 1] = end.withNano(0);
        for (int i = n - 1; i > 0; i--) {
            t[i - 1] = t[i].minusSeconds(Math.max(1, (long) (gaps[i] * 60 * scale)));
        }
        return List.of(t);
    }

    private Map<String, String> vars(Run r, EcomOrder o) {
        DemoCatalog.Store store = r.storeById.get(o.getStoreId());
        EcomOrderItem it = firstItem(r, o);
        String title = it == null || it.getTitle() == null ? "the item" : it.getTitle();
        if (title.length() > 48) {
            int cut = title.lastIndexOf(' ', 48);
            title = title.substring(0, cut > 20 ? cut : 48);
        }
        DemoListings.Item l = it == null ? null : listing(r, o.getStoreId(), it.getSellerSku());
        DemoCatalog.Product p = l == null ? null : DemoCatalog.PRODUCTS.stream().filter(x -> x.code().equals(l.productCode())).findFirst().orElse(null);
        Map<String, String> v = new HashMap<>();
        v.put("{name}", firstName(o.getBuyerName()));
        v.put("{order}", o.getPlatformOrderNo());
        v.put("{p}", title);
        boolean shopee = store.platform() == EcomPlatform.SHOPEE;
        v.put("{carrier}", o.getCarrier() != null ? o.getCarrier() : shopee ? "Shopee Xpress" : "USPS");
        v.put("{tracking}", o.getTrackingNo() != null ? o.getTrackingNo()
                : (shopee ? "SPX" + store.marketplace() : "9400") + String.format("%010d", Math.abs(o.getId() * 7919L) % 10_000_000_000L));
        v.put("{size}", p == null ? "15 x 10 x 5" : p.size().replace("×", " x "));
        v.put("{weight}", p == null ? "0.3" : String.valueOf(p.weight()));
        return v;
    }

    private static String fill(String text, Map<String, String> vars) {
        String s = text;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }
        return s;
    }

    /** 最近消息摘要（与会话服务一致，最多 500 字） */
    private static String preview(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }

    // ---------------------------------------------------------------- 评价与店铺反馈

    private CsReviewStatus lowStatus(Run r, LocalDate date) {
        long age = Duration.between(date.atStartOfDay(), r.now).toDays();
        if (age < 5) {
            return r.ctx.chance(0.8) ? CsReviewStatus.PENDING : CsReviewStatus.PROCESSING;
        }
        if (age < 15) {
            return r.ctx.chance(0.5) ? CsReviewStatus.PROCESSING : CsReviewStatus.RESOLVED;
        }
        return r.ctx.chance(0.85) ? CsReviewStatus.RESOLVED : CsReviewStatus.PROCESSING;
    }

    private String note(Run r, CsReviewStatus status) {
        return switch (status) {
            case RESOLVED -> r.ctx.pick(RESOLVED_NOTES);
            case PROCESSING -> r.ctx.pick(PROCESSING_NOTES);
            default -> null;
        };
    }

    /** 评价日期：发货后 3–25 天，不晚于今天；发货太近的为 null */
    private LocalDate reviewDate(Run r, EcomOrder o) {
        LocalDate ship = shipOf(o).toLocalDate();
        LocalDate today = r.now.toLocalDate();
        if (!ship.plusDays(3).isBefore(today)) {
            return null;
        }
        LocalDate d = ship.plusDays(r.ctx.between(3, 25));
        return d.isAfter(today) ? today.minusDays(r.ctx.between(0, 2)) : d;
    }

    private void freeReviews(Run r) {
        Map<EcomPlatform, Integer> weight = Map.of(EcomPlatform.AMAZON, 55, EcomPlatform.SHOPEE, 15, EcomPlatform.TIKTOK, 10, EcomPlatform.SHOPIFY, 8,
                EcomPlatform.ALIEXPRESS, 6, EcomPlatform.TEMU, 6);
        List<Long> pool = new ArrayList<>();
        for (Map.Entry<Long, DemoCatalog.Store> e : r.storeById.entrySet()) {
            int w = weight.getOrDefault(e.getValue().platform(), 0);
            // 亚马逊按站点订单量分摊
            int n = e.getValue().platform() == EcomPlatform.AMAZON ? (int) Math.round(e.getValue().share() * 1.2) : w / 2;
            for (int i = 0; i < n; i++) {
                pool.add(e.getKey());
            }
        }
        if (pool.isEmpty()) {
            return;
        }
        int target = REVIEWS - r.reviews;
        for (int i = 0, guard = 0; i < target && guard < target * 4; guard++) {
            EcomOrder o = pickOrder(r, pool, x -> shipped(x) && !r.reviewOrders.contains(x.getId()) && r.items.containsKey(x.getId()));
            if (o == null) {
                continue;
            }
            LocalDate date = reviewDate(r, o);
            if (date == null) {
                continue;
            }
            r.reviewOrders.add(o.getId());
            int rating = rating(r.ctx);
            insertReview(r, o, rating, date, rating <= CsConstants.LOW_RATING ? lowStatus(r, date) : CsReviewStatus.NONE);
            i++;
        }
    }

    private Long insertReview(Run r, EcomOrder o, int rating, LocalDate date, CsReviewStatus status) {
        DemoCatalog.Store store = r.storeById.get(o.getStoreId());
        String lang = lang(store);
        EcomOrderItem it = firstItem(r, o);
        String[] text = r.pick(List.of(REVIEW_TEXT.get(lang)[bucket(rating)]));
        boolean sync = registry.supports(store.platform(), CsCapability.REVIEWS);
        EcomReview v = new EcomReview();
        v.setStoreId(o.getStoreId());
        v.setPlatform(store.platform());
        v.setReviewId(switch (store.platform()) {
            case AMAZON -> r.uid("R") + Integer.toString(r.ctx.between(36 * 36, 36 * 36 * 36 - 1), 36).toUpperCase();
            case SHOPEE -> r.uid("1");
            default -> r.uid("RV");
        });
        if (it != null) {
            v.setPlatformItemId(it.getPlatformItemId());
            v.setSellerSku(it.getSellerSku());
            DemoListings.Item l = listing(r, o.getStoreId(), it.getSellerSku());
            v.setListingId(l == null ? null : l.id());
        }
        v.setRating(rating);
        v.setTitle(text[0]);
        v.setContent(text[1]);
        v.setReviewer(reviewer(store, o.getBuyerName()));
        v.setReviewDate(date);
        v.setOrderId(o.getId());
        v.setPlatformOrderNo(o.getPlatformOrderNo());
        boolean low = rating <= CsConstants.LOW_RATING;
        boolean replied = low ? status != CsReviewStatus.PENDING && r.ctx.chance(0.8) : r.ctx.chance(sync ? 0.5 : 0.2);
        v.setReplied(replied ? 1 : 0);
        if (replied) {
            v.setReplyContent(REPLY_TEXT.get(lang)[low ? 1 : 0]);
            LocalDateTime at = date.atTime(r.ctx.between(9, 20), r.ctx.between(0, 59)).plusDays(r.ctx.between(0, 2));
            v.setReplyTime(at.isAfter(r.now) ? r.now.minusMinutes(r.ctx.between(5, 60)) : at);
        }
        v.setHandleStatus(status);
        v.setHandlerId(low ? r.owner(o.getStoreId()) : null);
        v.setHandleNote(low ? note(r, status) : null);
        v.setSource(sync ? CsRecordSource.SYNC : CsRecordSource.IMPORT);
        LocalDateTime created = date.atTime(r.ctx.between(8, 22), r.ctx.between(0, 59));
        v.setCreateTime(created.isAfter(r.now) ? r.now : created);
        v.setUpdateTime(v.getReplyTime() != null ? v.getReplyTime() : v.getCreateTime());
        reviewMapper.insert(v);
        r.reviews++;
        return v.getId();
    }

    private static String reviewer(DemoCatalog.Store store, String buyer) {
        if (buyer == null || buyer.isBlank()) {
            return null;
        }
        String b = buyer.trim();
        if (store.platform() == EcomPlatform.SHOPEE) {
            String s = b.replace(" ", "").toLowerCase();
            return s.charAt(0) + "*****" + s.charAt(s.length() - 1);
        }
        if (DemoCatalog.JPY.equals(store.currency())) {
            return b;
        }
        String[] parts = b.split("\\s+");
        return parts.length > 1 ? parts[0] + " " + parts[parts.length - 1].charAt(0) + "." : b;
    }

    private void freeFeedbacks(Run r) {
        List<Long> pool = new ArrayList<>();
        for (Map.Entry<Long, DemoCatalog.Store> e : r.storeById.entrySet()) {
            DemoCatalog.Store s = e.getValue();
            int n = s.platform() == EcomPlatform.AMAZON ? (int) Math.round(s.share() / 2.0) : s.platform() == EcomPlatform.EBAY ? 5 : 0;
            for (int i = 0; i < n; i++) {
                pool.add(e.getKey());
            }
        }
        if (pool.isEmpty()) {
            return;
        }
        int target = FEEDBACKS - r.feedbacks;
        for (int i = 0, guard = 0; i < target && guard < target * 4; guard++) {
            EcomOrder o = pickOrder(r, pool, x -> shipped(x) && !r.feedbackOrders.contains(x.getId()));
            if (o == null) {
                continue;
            }
            LocalDate date = reviewDate(r, o);
            if (date == null) {
                continue;
            }
            r.feedbackOrders.add(o.getId());
            int x = r.ctx.between(1, 100);
            int rating = x <= 70 ? 5 : x <= 85 ? 4 : x <= 91 ? 3 : x <= 95 ? 2 : 1;
            insertFeedback(r, o, rating, date, rating <= CsConstants.LOW_RATING ? lowStatus(r, date) : CsReviewStatus.NONE);
            i++;
        }
    }

    private Long insertFeedback(Run r, EcomOrder o, int rating, LocalDate date, CsReviewStatus status) {
        DemoCatalog.Store store = r.storeById.get(o.getStoreId());
        String lang = lang(store);
        boolean low = rating <= CsConstants.LOW_RATING;
        EcomFeedback f = new EcomFeedback();
        f.setStoreId(o.getStoreId());
        f.setPlatform(store.platform());
        String fid = store.platform() == EcomPlatform.EBAY ? r.uid("") : o.getPlatformOrderNo() + "-" + date.toString().replace("-", "");
        r.ids.add(fid);
        f.setFeedbackId(fid);
        f.setRating(rating);
        f.setComments(r.ctx.pick(FEEDBACK_TEXT.get(lang)[bucket(rating)]));
        f.setRater(o.getBuyerName());
        f.setFeedbackDate(date);
        f.setOrderId(o.getId());
        f.setPlatformOrderNo(o.getPlatformOrderNo());
        f.setResponse(low && status != CsReviewStatus.PENDING && r.ctx.chance(0.7) ? REPLY_TEXT.get(lang)[1] : null);
        f.setHandleStatus(status);
        f.setHandlerId(low ? r.owner(o.getStoreId()) : null);
        f.setHandleNote(low ? note(r, status) : null);
        LocalDateTime created = date.atTime(r.ctx.between(8, 22), r.ctx.between(0, 59));
        f.setCreateTime(created.isAfter(r.now) ? r.now : created);
        f.setUpdateTime(f.getCreateTime());
        feedbackMapper.insert(f);
        r.feedbacks++;
        return f.getId();
    }

    // ---------------------------------------------------------------- 请求评价

    /**
     * 亚马逊已发货订单：发货 5–30 天的约 78% 有请求记录（已发送 / 不符合条件 / 失败），其余等待请求；更早的订单为历史请求记录（大部分已发送）
     */
    private void requests(Run r) {
        LocalDateTime windowFrom = r.now.minusDays(CsConstants.REQUEST_MAX_DAYS);
        LocalDateTime windowTo = r.now.minusDays(CsConstants.REQUEST_MIN_DAYS);
        LocalDateTime historyFrom = r.now.minusDays(Math.min(r.ctx.days(), 75));
        for (Map.Entry<Long, List<EcomOrder>> e : r.ordersByStore.entrySet()) {
            if (r.storeById.get(e.getKey()).platform() != EcomPlatform.AMAZON) {
                continue;
            }
            for (EcomOrder o : e.getValue()) {
                if (!shipped(o)) {
                    continue;
                }
                // 与候选列表一致：平台配送订单没有发货时间时按下单时间
                LocalDateTime base = o.getShipTime() != null ? o.getShipTime() : o.getOrderTime();
                CsRequestStatus status;
                String message = null;
                if (!base.isBefore(windowFrom) && base.isBefore(windowTo)) {
                    int x = r.ctx.between(1, 100);
                    if (x <= 22) {
                        continue;
                    }
                    status = x <= 80 ? CsRequestStatus.SENT : x <= 93 ? CsRequestStatus.NOT_ELIGIBLE : CsRequestStatus.FAILED;
                } else if (!base.isBefore(historyFrom) && base.isBefore(windowFrom)) {
                    int x = r.ctx.between(1, 100);
                    if (x <= 6) {
                        continue;
                    }
                    status = x <= 90 ? CsRequestStatus.SENT : CsRequestStatus.NOT_ELIGIBLE;
                } else {
                    continue;
                }
                if (status == CsRequestStatus.NOT_ELIGIBLE) {
                    message = r.ctx.pick("No productReviewAndSellerFeedback solicitation action is available for this order",
                            "403 Forbidden: the order is not eligible for solicitation", "404 Not Found: order not found in the marketplace");
                } else if (status == CsRequestStatus.FAILED) {
                    message = r.ctx.pick("429 QuotaExceeded: You exceeded your quota for the requested resource.",
                            "500 InternalFailure: We encountered an internal error. Please try again.", "Read timed out (30000 ms)");
                }
                long room = Duration.between(base.plusDays(CsConstants.REQUEST_MIN_DAYS), r.now).toMinutes();
                LocalDateTime at = base.plusDays(CsConstants.REQUEST_MIN_DAYS).plusMinutes(r.ctx.random().nextLong(Math.max(1, Math.min(room, 4 * 24 * 60))));
                EcomReviewRequest q = new EcomReviewRequest();
                q.setStoreId(o.getStoreId());
                q.setOrderId(o.getId());
                q.setPlatformOrderNo(o.getPlatformOrderNo());
                q.setStatus(status);
                q.setMessage(message);
                q.setRequestTime(at.withNano(0));
                q.setCreateBy(r.user());
                q.setCreateTime(q.getRequestTime());
                q.setUpdateTime(q.getRequestTime());
                requestMapper.insert(q);
                r.requests++;
            }
        }
    }
}
