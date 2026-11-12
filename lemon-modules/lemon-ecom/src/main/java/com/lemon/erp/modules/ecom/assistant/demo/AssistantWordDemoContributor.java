package com.lemon.erp.modules.ecom.assistant.demo;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch;
import com.lemon.erp.modules.ecom.assistant.service.EcomBannedWordService;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 演示数据 · 违禁词库（order 181）：内置词由 init.sql 提供，这里补充按平台 / 站点维护的自定义词（虾皮、TikTok、Temu、德国站），
 * 以及一条已停用的词，展示平台 / 站点范围与启用状态
 */
@Component
@RequiredArgsConstructor
public class AssistantWordDemoContributor implements DemoContributor {

    private final EcomBannedWordService wordService;

    @Override
    public int order() {
        return 181;
    }

    @Override
    public String key() {
        return "asst-words";
    }

    @Override
    public void generate(DemoContext ctx) {
        wordService.batchAdd(new AssistantRequests.WordBatch("original\nauthentic\n100% genuine", "EXAGGERATION", BannedWordLevel.WARN,
                BannedWordMatch.WORD, EcomPlatform.SHOPEE, null, null));
        wordService.batchAdd(new AssistantRequests.WordBatch("link in bio\nDM me\nWhatsApp", "PROHIBITED", BannedWordLevel.BLOCK,
                BannedWordMatch.CONTAINS, EcomPlatform.TIKTOK, null, null));
        wordService.batchAdd(new AssistantRequests.WordBatch("free gift\nclearance", "EXAGGERATION", BannedWordLevel.WARN,
                BannedWordMatch.WORD, EcomPlatform.TEMU, null, null));
        wordService.batchAdd(new AssistantRequests.WordBatch("umweltfreundlich\nbeste Qualität", "EXAGGERATION", BannedWordLevel.WARN,
                BannedWordMatch.WORD, EcomPlatform.AMAZON, "DE", null));
        wordService.create(new AssistantRequests.WordSave("Voltix Pro", "TRADEMARK", BannedWordLevel.BLOCK, BannedWordMatch.WORD, null, null,
                null, EnableStatus.DISABLED, null));
    }
}
