/**
 * a-table 没有唯一字段时的 rowKey：按行对象分配递增序号。
 * ant-design-vue 4 中 rowKey 函数的第二个参数（index）已废弃，用它会在控制台警告
 */
export function seqRowKey() {
  const keys = new WeakMap()
  let seq = 0
  return (record) => {
    if (record === null || typeof record !== 'object') return String(record)
    if (!keys.has(record)) keys.set(record, ++seq)
    return keys.get(record)
  }
}
