package com.lemon.erp.modules.ecom.assistant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.entity.EcomBannedWord;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomBannedWordMapper;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 违禁词库：按平台 / 站点维护禁用词、侵权品牌词、夸大宣传词、医疗功效词（类型为业务字典），处理方式为阻止发布或只提醒。
 * 刊登编辑页取适用的词在前端实时检查并高亮，提交发布时 {@link #blockReason} 再检查一次（命中阻止类的词不能发布）
 */
@Service
@RequiredArgsConstructor
public class EcomBannedWordService {

    public static final String TYPE_DICT = "ecom_banned_word_type";
    static final String MARKETPLACE_DICT = "ecom_marketplace";

    private final EcomBannedWordMapper wordMapper;
    private final DictLookup dictLookup;

    // ---------------------------------------------------------------- 维护

    public PageResult<EcomBannedWord> page(long current, long size, String keyword, String wordType, BannedWordLevel level, EcomPlatform platform,
                                           String marketplace, EnableStatus status) {
        Page<EcomBannedWord> page = wordMapper.selectPage(Page.of(current, size), Wrappers.<EcomBannedWord>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(EcomBannedWord::getWord, keyword.trim()).or().like(EcomBannedWord::getSuggestion, keyword.trim()))
                .eq(StringUtils.hasText(wordType), EcomBannedWord::getWordType, wordType)
                .eq(level != null, EcomBannedWord::getLevel, level)
                .eq(platform != null, EcomBannedWord::getPlatform, platform)
                .eq(StringUtils.hasText(marketplace), EcomBannedWord::getMarketplace, marketplace)
                .eq(status != null, EcomBannedWord::getStatus, status)
                .orderByAsc(EcomBannedWord::getWordType).orderByAsc(EcomBannedWord::getWord));
        return PageResult.of(page);
    }

    public EcomBannedWord get(Long id) {
        EcomBannedWord w = wordMapper.selectById(id);
        if (w == null) {
            throw new BizException("ecom.error.asst.wordNotFound");
        }
        return w;
    }

    public Long create(AssistantRequests.WordSave req) {
        EcomBannedWord w = new EcomBannedWord();
        apply(w, req);
        w.setBuiltin(0);
        w.setCreateBy(SecurityUtils.getUserIdOrNull());
        checkDuplicate(w, null);
        wordMapper.insert(w);
        return w.getId();
    }

    public void update(Long id, AssistantRequests.WordSave req) {
        EcomBannedWord w = get(id);
        apply(w, req);
        checkDuplicate(w, id);
        wordMapper.updateById(w);
    }

    public void changeStatus(Long id, EnableStatus status) {
        EcomBannedWord w = get(id);
        w.setStatus(status);
        wordMapper.updateById(w);
    }

    public void delete(List<Long> ids) {
        ids.forEach(this::get);
        wordMapper.deleteByIds(ids);
    }

    /** 批量添加（一行一个，去掉空行与重复），已存在的跳过；返回新增条数 */
    @Transactional
    public int batchAdd(AssistantRequests.WordBatch req) {
        dictLookup.check(TYPE_DICT, req.wordType(), "ecom.label.asst.wordType");
        checkMarketplace(req.marketplace());
        Set<String> lines = new LinkedHashSet<>();
        for (String line : req.words().split("[\\r\\n]+")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                if (t.length() > 128) {
                    throw new BizException("ecom.error.asst.wordTooLong", t.substring(0, 20));
                }
                lines.add(t);
            }
        }
        if (lines.isEmpty()) {
            throw new BizException("ecom.error.asst.wordRequired");
        }
        String marketplace = StringUtils.hasText(req.marketplace()) ? req.marketplace().trim() : null;
        Set<String> existing = sameScope(req.platform(), marketplace).stream().map(w -> w.getWord().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<EcomBannedWord> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String word : lines) {
            String key = word.toLowerCase(Locale.ROOT);
            if (existing.contains(key) || !seen.add(key)) {
                continue;
            }
            EcomBannedWord w = new EcomBannedWord();
            w.setWord(word);
            w.setWordType(req.wordType());
            w.setLevel(req.level());
            w.setMatchMode(req.matchMode() == null ? BannedWordMatch.WORD : req.matchMode());
            w.setPlatform(req.platform());
            w.setMarketplace(marketplace);
            w.setSuggestion(StringUtils.hasText(req.suggestion()) ? req.suggestion().trim() : null);
            w.setBuiltin(0);
            w.setStatus(EnableStatus.ENABLED);
            w.setCreateBy(SecurityUtils.getUserIdOrNull());
            items.add(w);
        }
        if (!items.isEmpty()) {
            wordMapper.insertBatch(items);
        }
        return items.size();
    }

    private void apply(EcomBannedWord w, AssistantRequests.WordSave req) {
        dictLookup.check(TYPE_DICT, req.wordType(), "ecom.label.asst.wordType");
        checkMarketplace(req.marketplace());
        w.setWord(req.word().trim());
        w.setWordType(req.wordType());
        w.setLevel(req.level());
        w.setMatchMode(req.matchMode() == null ? BannedWordMatch.WORD : req.matchMode());
        w.setPlatform(req.platform());
        w.setMarketplace(StringUtils.hasText(req.marketplace()) ? req.marketplace().trim() : null);
        w.setSuggestion(StringUtils.hasText(req.suggestion()) ? req.suggestion().trim() : null);
        w.setStatus(req.status() == null ? EnableStatus.ENABLED : req.status());
        w.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
    }

    private void checkMarketplace(String marketplace) {
        if (StringUtils.hasText(marketplace)) {
            dictLookup.check(MARKETPLACE_DICT, marketplace.trim(), "ecom.label.asst.marketplace");
        }
    }

    /** 同一词（不区分大小写）在同一平台、站点只能有一条 */
    private void checkDuplicate(EcomBannedWord w, Long selfId) {
        boolean dup = sameScope(w.getPlatform(), w.getMarketplace()).stream()
                .anyMatch(x -> !x.getId().equals(selfId) && x.getWord().equalsIgnoreCase(w.getWord()));
        if (dup) {
            throw new BizException("ecom.error.asst.wordDuplicate", w.getWord());
        }
    }

    /** 平台、站点相同（都为空也算相同）的词 */
    private List<EcomBannedWord> sameScope(EcomPlatform platform, String marketplace) {
        return wordMapper.selectList(Wrappers.<EcomBannedWord>lambdaQuery()
                .eq(platform != null, EcomBannedWord::getPlatform, platform).isNull(platform == null, EcomBannedWord::getPlatform)
                .eq(marketplace != null, EcomBannedWord::getMarketplace, marketplace).isNull(marketplace == null, EcomBannedWord::getMarketplace));
    }

    // ---------------------------------------------------------------- 检查

    /** 启用、适用于该平台 / 站点的词（平台、站点为空的词全部适用） */
    public List<EcomBannedWord> applicable(EcomPlatform platform, String marketplace) {
        return wordMapper.selectList(Wrappers.<EcomBannedWord>lambdaQuery().eq(EcomBannedWord::getStatus, EnableStatus.ENABLED)
                        .orderByAsc(EcomBannedWord::getLevel).orderByAsc(EcomBannedWord::getId)).stream()
                .filter(w -> w.getPlatform() == null || w.getPlatform() == platform)
                .filter(w -> w.getMarketplace() == null || w.getMarketplace().equalsIgnoreCase(marketplace == null ? "" : marketplace))
                .toList();
    }

    public List<AssistantViews.ApplicableWord> applicableWords(EcomPlatform platform, String marketplace) {
        return applicable(platform, marketplace).stream()
                .map(w -> new AssistantViews.ApplicableWord(w.getId(), w.getWord(), w.getWordType(), w.getLevel(), w.getMatchMode(), w.getSuggestion()))
                .toList();
    }

    /** 检查标题、五点、描述 */
    public AssistantViews.CheckResult check(EcomPlatform platform, String marketplace, String title, List<String> bullets, String description) {
        return check(applicable(platform, marketplace), title, bullets, description);
    }

    static AssistantViews.CheckResult check(List<EcomBannedWord> words, String title, List<String> bullets, String description) {
        List<BannedWordMatcher.Hit> hits = new ArrayList<>(BannedWordMatcher.find(words, "title", null, title));
        if (bullets != null) {
            for (int i = 0; i < bullets.size(); i++) {
                hits.addAll(BannedWordMatcher.find(words, "bullets", i, bullets.get(i)));
            }
        }
        hits.addAll(BannedWordMatcher.find(words, "description", null, description));
        int block = (int) hits.stream().filter(h -> h.level() == BannedWordLevel.BLOCK).count();
        return new AssistantViews.CheckResult(hits, block, hits.size() - block);
    }

    /**
     * 提交发布前的检查：命中阻止发布的词时返回原因（列出命中的词），否则返回 null（只提醒的词不阻止）
     */
    public String blockReason(EcomPlatform platform, String marketplace, String title, List<String> bullets, String description) {
        AssistantViews.CheckResult r = check(platform, marketplace, title, bullets, description);
        if (r.blockCount() == 0) {
            return null;
        }
        String words = r.hits().stream().filter(h -> h.level() == BannedWordLevel.BLOCK).map(BannedWordMatcher.Hit::word).distinct()
                .collect(Collectors.joining(Messages.get("ecom.text.asst.sep")));
        return Messages.get("ecom.error.asst.bannedBlocked", words);
    }
}
