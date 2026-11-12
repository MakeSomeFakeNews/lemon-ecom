package com.lemon.erp.modules.ecom.assistant.service;

import com.lemon.erp.modules.ecom.assistant.entity.EcomBannedWord;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * 违禁词匹配（与前端 components/ecom/assistant/bannedWords.js 的规则一致）：不区分大小写；整词匹配要求词的前后不是字母或数字，
 * 词的首 / 尾是中日韩文字或符号时该侧不检查边界（这些文字之间没有空格）；包含匹配只要出现即命中。
 * 同一字段中一个词只报告第一次出现的位置和出现次数
 */
public final class BannedWordMatcher {

    /** 摘录命中位置前后的字符数 */
    static final int EXCERPT = 24;

    private BannedWordMatcher() {
    }

    /**
     * 命中
     *
     * @param field  字段：title / bullets / description
     * @param index  五点的序号（从 0 开始），其他字段为 null
     * @param start  第一次出现的位置
     * @param count  出现次数
     */
    public record Hit(Long wordId, String word, String wordType, BannedWordLevel level, String suggestion, String field, Integer index,
                      int start, int count, String excerpt) {
    }

    public static List<Hit> find(List<EcomBannedWord> words, String field, Integer index, String text) {
        List<Hit> hits = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return hits;
        }
        for (EcomBannedWord w : words) {
            String word = w.getWord() == null ? "" : w.getWord().trim();
            if (word.isEmpty()) {
                continue;
            }
            boolean whole = w.getMatchMode() != BannedWordMatch.CONTAINS;
            int first = -1;
            int count = 0;
            int from = 0;
            while (true) {
                int at = indexOfIgnoreCase(text, word, from);
                if (at < 0) {
                    break;
                }
                if (!whole || bounded(text, word, at)) {
                    if (first < 0) {
                        first = at;
                    }
                    count++;
                }
                from = at + 1;
            }
            if (first >= 0) {
                hits.add(new Hit(w.getId(), word, w.getWordType(), w.getLevel(), w.getSuggestion(), field, index, first, count,
                        excerpt(text, first, word.length())));
            }
        }
        return hits;
    }

    static int indexOfIgnoreCase(String text, String word, int from) {
        int max = text.length() - word.length();
        for (int i = Math.max(0, from); i <= max; i++) {
            if (text.regionMatches(true, i, word, 0, word.length())) {
                return i;
            }
        }
        return -1;
    }

    /** 整词边界：词首 / 尾是字母数字（非中日韩文字）时，相邻字符不能是字母数字 */
    static boolean bounded(String text, String word, int at) {
        if (checksBoundary(word.charAt(0)) && at > 0 && Character.isLetterOrDigit(text.charAt(at - 1))) {
            return false;
        }
        int end = at + word.length();
        return !(checksBoundary(word.charAt(word.length() - 1)) && end < text.length() && Character.isLetterOrDigit(text.charAt(end)));
    }

    private static boolean checksBoundary(char c) {
        if (!Character.isLetterOrDigit(c)) {
            return false;
        }
        Character.UnicodeScript s = Character.UnicodeScript.of(c);
        return s != Character.UnicodeScript.HAN && s != Character.UnicodeScript.HIRAGANA && s != Character.UnicodeScript.KATAKANA
                && s != Character.UnicodeScript.HANGUL && s != Character.UnicodeScript.THAI;
    }

    static String excerpt(String text, int start, int len) {
        int from = Math.max(0, start - EXCERPT);
        int to = Math.min(text.length(), start + len + EXCERPT);
        return (from > 0 ? "…" : "") + text.substring(from, to).replaceAll("\\s+", " ") + (to < text.length() ? "…" : "");
    }
}
