package com.lemon.erp.framework.ai;

/**
 * 业务模块调用大模型生成文本的通用契约（与 {@link AiTool} 等「AI 调用业务」的契约方向相反）：客服回复草拟、翻译、评价回复、
 * 刊登标题 / 描述优化等。由 ai 模块实现（经 AiModelService：读取当前租户的「AI 设置」、检查「AI 写作」功能开关与月度上限、记录用量），
 * 业务模块只依赖本接口；没有 ai 模块时注入 {@code ObjectProvider<AiTextGenerator>} 得到空，按不可用处理。
 * <p>
 * 调用方约定：不在数据库事务中调用（模型调用可能要几十秒）；提示词、业务数据按需组装，不要把密钥、身份证号等敏感信息放进去。
 */
public interface AiTextGenerator {

    /** 当前租户能否调用：已配置模型、已启用 AI、「AI 写作」功能已开启（不检查月度上限） */
    boolean isAvailable();

    /** 不可用的原因（按当前语言，给页面提示用），可用时为 null */
    String unavailableReason();

    /**
     * 生成文本
     *
     * @return 模型输出（已去掉首尾空白）
     * @throws com.lemon.erp.common.exception.BizException 未配置、未启用、功能未开启、超过月度上限、模型调用失败
     */
    String generate(AiTextRequest request);
}
