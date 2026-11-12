import { readExcelHeaders } from '@/utils/excel'

/**
 * 读取广告后台下载的报告文件：xlsx（第一个工作表）或 csv / tsv（自动识别逗号 / 制表符，支持引号与 UTF-8 BOM），
 * 返回 { headers: string[], rows: string[][] }（去掉空行）
 */
export async function readReportFile(file) {
  if (/\.xlsx$/i.test(file.name)) {
    const { headers, samples } = await readExcelHeaders(file, 100000)
    return { headers, rows: samples }
  }
  const text = (await file.text()).replace(/^﻿/, '')
  const firstLine = text.slice(0, text.indexOf('\n') < 0 ? text.length : text.indexOf('\n'))
  const sep = (firstLine.match(/\t/g) || []).length > (firstLine.match(/,/g) || []).length ? '\t' : ','
  const all = parseDelimited(text, sep).filter((r) => r.some((v) => v.trim() !== ''))
  return { headers: (all[0] || []).map((h) => h.trim()), rows: all.slice(1) }
}

/** 解析分隔文本：双引号包围的字段可含分隔符、换行，"" 表示一个引号 */
export function parseDelimited(text, sep = ',') {
  const rows = []
  let row = []
  let field = ''
  let quoted = false
  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    if (quoted) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          field += '"'
          i++
        } else quoted = false
      } else field += c
    } else if (c === '"') quoted = true
    else if (c === sep) {
      row.push(field)
      field = ''
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i++
      row.push(field)
      rows.push(row)
      row = []
      field = ''
    } else field += c
  }
  if (field !== '' || row.length) {
    row.push(field)
    rows.push(row)
  }
  return rows
}
