/** 电商 → 订单与客服页面的界面文字（平台订单列表 / 详情、包裹、物流跟踪、退货、客服等页面打磨时新增） */
export default {
  list: {
    order: '订单',
    items: '商品',
    buyer: '买家 / 收件',
    profit: '利润',
    status: '状态',
    ship: '发货',
    allItems: '全部商品（{n} 行）',
    moreItems: '还有 {n} 行商品',
    itemCount: '{n} 行商品',
    remote: '偏远地区',
    actual: '实际利润',
    shipBy: '截止',
    packages: '{n} 个包裹',
    emptyTab: '当前状态下没有订单',
    emptyFiltered: '没有符合条件的订单，试试调整筛选条件'
  },
  detail: {
    product: '商品',
    lineExtra: '运费 {shipping} · 税 {tax} · 优惠 {discount}',
    vat: '需申报 VAT',
    items: '商品',
    itemsValue: '{lines} 行 / {qty} 件',
    logistics: '包裹与物流',
    timeline: '处理记录与备注',
    buyer: '买家与收货地址',
    profit: '利润构成',
    rate: '利润率'
  },
  timeline: {
    system: '系统',
    empty: '还没有处理记录'
  },
  pkg: {
    selected: '已选 {n} 个包裹',
    hint: '勾选包裹后可批量申请运单号、打印面单 / 拣货单、发货',
    empty: '当前状态下没有包裹',
    route: '仓库 / 物流渠道'
  },
  scan: {
    ship: '发货',
    waiting: '待发货包裹',
    okCount: '本次已发货',
    failCount: '本次失败',
    ready: '等待扫描：扫描面单上的运单号或输入包裹号后回车',
    noHistory: '还没有扫描记录'
  },
  track: {
    hint: '勾选包裹后可批量拉取轨迹；系统每 2 小时自动拉取',
    empty: '没有符合条件的包裹'
  },
  ret: {
    stepCreate: '登记',
    stepReceive: '退货入库',
    stepRefund: '退款',
    all: '全部',
    order: '平台订单 / 买家',
    warehouse: '退回仓库 / 退货单',
    empty: '没有退货退款单'
  },
  cs: {
    filter: '筛选'
  },
  edit: {
    shipping: '配送与备注'
  },
  logAction: {
    CREATE: '订单写入',
    PLATFORM_STATUS: '平台状态',
    RULE: '命中规则',
    TAG: '打标签',
    UNTAG: '去标签',
    AUDIT: '审核通过',
    EXCEPTION: '异常',
    OUT_OF_STOCK: '缺货',
    RECHECK: '重新检查库存',
    REMATCH: '重新匹配 SKU',
    HOLD: '挂起',
    UNHOLD: '取消挂起',
    EDIT: '修改信息',
    ASSIGN: '指定仓库 / 物流',
    PACKAGE: '包裹变化',
    LABEL: '申请运单号',
    PRINT: '打印面单',
    OUTBOUND: '出库',
    SHIP: '发货',
    SHIP_CONFIRM: '回传平台',
    CANCEL: '取消订单',
    RETURN: '退货退款',
    REFUND: '退款',
    RESEND: '补发',
    NOTE: '内部备注'
  },
  batch: {
    selected: '已选 {n} 单',
    selectedItems: '已选 {n} 项',
    more: '更多',
    clear: '取消选择',
    hint: '勾选订单后可批量处理'
  }
}
