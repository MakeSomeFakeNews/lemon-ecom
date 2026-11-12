package com.lemon.erp.framework.ai;

import java.math.BigDecimal;

/**
 * 文本生成请求（{@link AiTextGenerator}）
 *
 * @param systemPrompt 系统提示：角色、任务、格式要求（给模型看的提示词也要按语言写消息键）
 * @param userContent  用户内容：要处理的业务数据（会话、订单、商品信息等）
 * @param language     输出语言（如 en、de、ja、zh 或语言名称），为空时按提示词的要求（如「与买家使用的语言相同」）
 * @param maxTokens    最大输出 token 数，为空用「AI 设置」中的值
 * @param temperature  温度，为空用「AI 设置」中的值
 */
public record AiTextRequest(String systemPrompt, String userContent, String language, Integer maxTokens, BigDecimal temperature) {

    public static AiTextRequest of(String systemPrompt, String userContent) {
        return new AiTextRequest(systemPrompt, userContent, null, null, null);
    }

    public AiTextRequest withLanguage(String value) {
        return new AiTextRequest(systemPrompt, userContent, value, maxTokens, temperature);
    }

    public AiTextRequest withMaxTokens(Integer value) {
        return new AiTextRequest(systemPrompt, userContent, language, value, temperature);
    }

    public AiTextRequest withTemperature(BigDecimal value) {
        return new AiTextRequest(systemPrompt, userContent, language, maxTokens, value);
    }
}
