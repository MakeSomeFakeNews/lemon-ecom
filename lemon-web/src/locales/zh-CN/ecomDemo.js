/** 电商演示数据（电商看板中的生成入口） */
export default {
  title: '还没有业务数据，先生成一套演示数据看看效果？',
  desc: '一键生成一个跨境卖家的完整业务：10 个平台店铺、40 个商品（带图片）、供应商与采购、头程货件、近 120 天订单（含各作业环节）、退货、结算回款、广告、客服、补货与预测，约 1–2 分钟。',
  generate: '生成演示数据',
  confirmTitle: '生成演示数据',
  confirmContent: '将在当前租户中生成演示用的店铺、商品、订单、库存与财务单据（店铺编码以 DEMO- 开头，演示店铺的授权为占位值、不会访问真实平台）。生成后不能再次生成，确定继续？',
  running: '正在生成演示数据…',
  current: '正在生成：{part}（{done} / {total}）',
  done: '演示数据已生成',
  doneWithFailures: '演示数据已生成，{count} 个部分失败（见看板提示，详情在后端日志中）',
  failed: '以下部分生成失败：',
  failureItem: '{part}：{message}',
  parts: {
    setup: '基础设置',
    users: '运营账号',
    catalog: '商品与供应商',
    stores: '店铺',
    listings: '在线商品',
    plan: '订单计划',
    purchase: '采购入库',
    inbound: '头程货件',
    orders: '平台订单',
    'order-ops': '订单作业',
    returns: '退货退款',
    settlements: '结算回款',
    planning: '库存与计划',
    ads: '广告',
    cs: '客服与评价'
  }
}
