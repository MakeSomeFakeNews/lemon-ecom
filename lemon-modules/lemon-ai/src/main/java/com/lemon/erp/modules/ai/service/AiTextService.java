package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiTextGenerator;
import com.lemon.erp.framework.ai.AiTextRequest;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.client.LlmResponse;
import com.lemon.erp.modules.ai.dto.AiStatus;
import com.lemon.erp.modules.ai.model.AiFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link AiTextGenerator} 的实现：业务模块的文本生成（客服回复草拟、翻译、刊登文案等）统一经 {@link AiModelService}
 * 以功能「AI 写作」（{@link AiFeature#WRITE}）调用对话模型——检查已配置、已启用、功能开关、月度上限，并记录用量。
 * 指定了输出语言时在系统提示末尾追加语言要求
 */
@Service
@RequiredArgsConstructor
public class AiTextService implements AiTextGenerator {

    /** 用户内容的最大字符数（超出截断，避免一次请求过大） */
    static final int MAX_CONTENT = 20_000;

    private final AiModelService modelService;
    private final AiConfigService configService;

    @Override
    public boolean isAvailable() {
        return Boolean.TRUE.equals(configService.status().features().get(AiFeature.WRITE));
    }

    @Override
    public String unavailableReason() {
        AiStatus s = configService.status();
        if (!s.configured()) {
            return Messages.get("ai.error.notConfigured");
        }
        if (!s.enabled()) {
            return Messages.get("ai.error.disabled");
        }
        if (!Boolean.TRUE.equals(s.features().get(AiFeature.WRITE))) {
            return Messages.get("ai.error.featureDisabled", Messages.get(AiFeature.WRITE.labelKey()));
        }
        return null;
    }

    @Override
    public String generate(AiTextRequest request) {
        if (request == null || !StringUtils.hasText(request.userContent())) {
            throw new BizException("ai.error.writeEmpty");
        }
        List<LlmMessage> messages = new ArrayList<>();
        String system = request.systemPrompt() == null ? "" : request.systemPrompt().trim();
        if (StringUtils.hasText(request.language())) {
            system = (system.isEmpty() ? "" : system + "\n") + Messages.get("ai.prompt.writeLanguage", request.language().trim());
        }
        if (!system.isEmpty()) {
            messages.add(LlmMessage.system(system));
        }
        String content = request.userContent();
        messages.add(LlmMessage.user(content.length() > MAX_CONTENT ? content.substring(0, MAX_CONTENT) : content));
        LlmRequest req = LlmRequest.of(messages);
        if (request.maxTokens() != null) {
            req = req.withMaxTokens(request.maxTokens());
        }
        if (request.temperature() != null) {
            req = req.withTemperature(request.temperature());
        }
        LlmResponse res = modelService.chat(AiFeature.WRITE, req);
        String text = res.content() == null ? "" : res.content().trim();
        if (text.isEmpty()) {
            throw new BizException("ai.error.writeNoResult");
        }
        return text;
    }
}
