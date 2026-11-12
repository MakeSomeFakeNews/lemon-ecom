import { BannedWordLevel, BannedWordMatch } from '@/constants/enums'

/**
 * 违禁词匹配（与后端 BannedWordMatcher 规则一致）：不区分大小写；整词匹配要求词的前后不是字母或数字，
 * 词的首 / 尾是中日韩泰文字时该侧不检查边界；包含匹配只要出现即命中。
 */
const ALNUM = /[\p{L}\p{N}]/u
const NO_BOUNDARY = /[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}\p{Script=Hangul}\p{Script=Thai}]/u

const checksBoundary = (ch) => ALNUM.test(ch) && !NO_BOUNDARY.test(ch)

/** 一段文字中某个词的全部命中位置 [{ start, end }] */
export function findRanges(text, word, matchMode) {
  const out = []
  if (!text || !word) return out
  const lower = text.toLowerCase()
  const w = word.toLowerCase()
  const whole = matchMode !== BannedWordMatch.CONTAINS
  let from = 0
  while (from <= lower.length - w.length) {
    const at = lower.indexOf(w, from)
    if (at < 0) break
    const end = at + w.length
    const okStart = !whole || !checksBoundary(word[0]) || at === 0 || !ALNUM.test(text[at - 1])
    const okEnd = !whole || !checksBoundary(word[word.length - 1]) || end >= text.length || !ALNUM.test(text[end])
    if (okStart && okEnd) out.push({ start: at, end })
    from = at + 1
  }
  return out
}

/** 检查一段文字：返回命中 [{ word, wordType, level, suggestion, ranges }] */
export function checkText(words, text) {
  const hits = []
  for (const w of words || []) {
    const ranges = findRanges(text, (w.word || '').trim(), w.matchMode)
    if (ranges.length) hits.push({ ...w, ranges })
  }
  return hits
}

/**
 * 检查刊登文案：title、bullets（数组）、description → { hits: [{ field, index, word, level, ... }], block, warn, byField }
 * byField 的键为 title / bullets.0 / description
 */
export function checkListing(words, { title, bullets, description }) {
  const hits = []
  const add = (field, index, text) => checkText(words, text).forEach((h) => hits.push({ ...h, field, index }))
  add('title', null, title)
  ;(bullets || []).forEach((b, i) => add('bullets', i, b))
  add('description', null, description)
  const byField = {}
  for (const h of hits) {
    const key = h.index == null ? h.field : `${h.field}.${h.index}`
    ;(byField[key] = byField[key] || []).push(h)
  }
  const block = hits.filter((h) => h.level === BannedWordLevel.BLOCK).length
  return { hits, block, warn: hits.length - block, byField }
}

/** 按命中位置把文字拆成片段 [{ text, hit }]，用于高亮显示（重叠的位置合并） */
export function segments(text, hits) {
  if (!text) return []
  const marks = []
  for (const h of hits || []) for (const r of h.ranges || []) marks.push({ ...r, level: h.level })
  marks.sort((a, b) => a.start - b.start)
  const out = []
  let pos = 0
  for (const m of marks) {
    if (m.end <= pos) continue
    const start = Math.max(m.start, pos)
    if (start > pos) out.push({ text: text.slice(pos, start) })
    out.push({ text: text.slice(start, m.end), level: m.level })
    pos = m.end
  }
  if (pos < text.length) out.push({ text: text.slice(pos) })
  return out
}
