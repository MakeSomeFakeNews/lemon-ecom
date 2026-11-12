package com.lemon.erp.modules.ecom.ai.text;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 电商 AI 文本 /ecom/ai：可用状态、客服草拟回复 / 翻译、评价回复草拟、刊登文案翻译 / 优化（结果只返回给页面，由人工确认后再发送或保存）
 */
@RestController
@RequestMapping("/ecom/ai")
@RequiredArgsConstructor
public class EcomAiTextController {

    private final EcomAiTextService textService;

    /** 草拟请求：语言（为空按买家 / 评价的语言）、补充要求 */
    @Data
    public static class DraftRequest {
        @Size(max = 16, message = "{ecom.valid.fix.languageTooLong}")
        private String language;
        @Size(max = 500, message = "{ecom.valid.fix.instructionTooLong}")
        private String instruction;
    }

    /** 翻译请求：目标语言为空且有会话时译成买家的语言 */
    @Data
    public static class TranslateRequest {
        @NotBlank(message = "{ecom.valid.fix.textRequired}")
        @Size(max = 8000, message = "{ecom.valid.fix.textTooLong}")
        private String text;
        @Size(max = 16, message = "{ecom.valid.fix.languageTooLong}")
        private String target;
        private Long threadId;
    }

    /** 刊登文案优化请求 */
    @Data
    public static class ListingRequest {
        private EcomPlatform platform;
        @Size(max = 500, message = "{ecom.valid.fix.textTooLong}")
        private String title;
        @Size(max = 8000, message = "{ecom.valid.fix.textTooLong}")
        private String description;
        @Size(max = 16, message = "{ecom.valid.fix.languageTooLong}")
        private String language;
        private Integer maxTitle;
        @Size(max = 500, message = "{ecom.valid.fix.instructionTooLong}")
        private String instruction;
    }

    /** 生成的文本 */
    public record TextResult(String text) {
    }

    /** 登录即可访问：AI 写作是否可用（页面据此显示按钮或「去 AI 设置开启」） */
    @GetMapping("/status")
    public R<EcomAiTextService.Status> status() {
        return R.ok(textService.status());
    }

    @PostMapping("/cs/thread/{id}/draft")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:ai')")
    public R<TextResult> draftThread(@PathVariable Long id, @Valid @RequestBody(required = false) DraftRequest req) {
        DraftRequest r = req == null ? new DraftRequest() : req;
        return R.ok(new TextResult(textService.draftThreadReply(id, r.getLanguage(), r.getInstruction())));
    }

    /** 翻译：买家消息译成指定语言（如中文），或回复译成买家的语言（target 为空、带 threadId） */
    @PostMapping("/cs/translate")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:ai')")
    public R<TextResult> translate(@Valid @RequestBody TranslateRequest req) {
        String text = req.getTarget() == null || req.getTarget().isBlank()
                ? textService.translateForThread(req.getThreadId(), req.getText()) : textService.translate(req.getText(), req.getTarget());
        return R.ok(new TextResult(text));
    }

    @PostMapping("/cs/review/{id}/draft")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:ai')")
    public R<TextResult> draftReview(@PathVariable Long id, @Valid @RequestBody(required = false) DraftRequest req) {
        DraftRequest r = req == null ? new DraftRequest() : req;
        return R.ok(new TextResult(textService.draftReviewReply(id, r.getLanguage(), r.getInstruction())));
    }

    /** 评价译成指定语言（看懂外语评价） */
    @PostMapping("/cs/review/translate")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:ai')")
    public R<TextResult> translateReview(@Valid @RequestBody TranslateRequest req) {
        return R.ok(new TextResult(textService.translate(req.getText(), req.getTarget())));
    }

    /** 刊登：标题 / 描述翻译 */
    @PostMapping("/listing/translate")
    @PreAuthorize("@ss.hasAnyPerm('ecom:listing:add', 'ecom:listing:edit')")
    public R<TextResult> translateListing(@Valid @RequestBody TranslateRequest req) {
        return R.ok(new TextResult(textService.translate(req.getText(), req.getTarget())));
    }

    /** 刊登：按平台规则优化标题与描述 */
    @PostMapping("/listing/optimize")
    @PreAuthorize("@ss.hasAnyPerm('ecom:listing:add', 'ecom:listing:edit')")
    public R<EcomAiTextService.ListingCopy> optimizeListing(@Valid @RequestBody ListingRequest req) {
        return R.ok(textService.optimizeListing(req.getPlatform(), req.getTitle(), req.getDescription(), req.getLanguage(), req.getMaxTitle(),
                req.getInstruction()));
    }
}
