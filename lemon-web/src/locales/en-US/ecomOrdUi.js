/** E-commerce → order & customer service page texts */
export default {
  list: {
    order: 'Order',
    items: 'Items',
    buyer: 'Buyer / Ship to',
    profit: 'Profit',
    status: 'Status',
    ship: 'Shipping',
    allItems: 'All items ({n} lines)',
    moreItems: '{n} more lines',
    itemCount: '{n} lines',
    remote: 'Remote area',
    actual: 'Actual',
    shipBy: 'Due',
    packages: '{n} packages',
    emptyTab: 'No orders in this status',
    emptyFiltered: 'No matching orders. Try adjusting the filters'
  },
  detail: {
    product: 'Product',
    lineExtra: 'Shipping {shipping} · Tax {tax} · Discount {discount}',
    vat: 'VAT to declare',
    items: 'Items',
    itemsValue: '{lines} lines / {qty} pcs',
    logistics: 'Packages & logistics',
    timeline: 'History & notes',
    buyer: 'Buyer & ship-to address',
    profit: 'Profit breakdown',
    rate: 'Margin'
  },
  timeline: {
    system: 'System',
    empty: 'No history yet'
  },
  pkg: {
    selected: '{n} packages selected',
    hint: 'Select packages to request labels, print labels / pick lists or ship in batch',
    empty: 'No packages in this status',
    route: 'Warehouse / Channel'
  },
  scan: {
    ship: 'Ship',
    waiting: 'Packages to ship',
    okCount: 'Shipped this session',
    failCount: 'Failed this session',
    ready: 'Ready: scan the tracking number on the label or enter a package number and press Enter',
    noHistory: 'No scans yet'
  },
  track: {
    hint: 'Select packages to pull tracking in batch; tracking is pulled automatically every 2 hours',
    empty: 'No matching packages'
  },
  ret: {
    stepCreate: 'Created',
    stepReceive: 'Received',
    stepRefund: 'Refunded',
    all: 'All',
    order: 'Order / Buyer',
    warehouse: 'Return warehouse / Doc',
    empty: 'No returns or refunds'
  },
  cs: {
    filter: 'Filters'
  },
  edit: {
    shipping: 'Shipping & remarks'
  },
  logAction: {
    CREATE: 'Order created',
    PLATFORM_STATUS: 'Platform status',
    RULE: 'Rules matched',
    TAG: 'Tagged',
    UNTAG: 'Untagged',
    AUDIT: 'Approved',
    EXCEPTION: 'Exception',
    OUT_OF_STOCK: 'Out of stock',
    RECHECK: 'Stock rechecked',
    REMATCH: 'SKU rematched',
    HOLD: 'On hold',
    UNHOLD: 'Released',
    EDIT: 'Info edited',
    ASSIGN: 'Warehouse / channel assigned',
    PACKAGE: 'Package changed',
    LABEL: 'Label requested',
    PRINT: 'Label printed',
    OUTBOUND: 'Outbound',
    SHIP: 'Shipped',
    SHIP_CONFIRM: 'Shipment confirmed',
    CANCEL: 'Cancelled',
    RETURN: 'Return',
    REFUND: 'Refunded',
    RESEND: 'Resend',
    NOTE: 'Internal note'
  },
  batch: {
    selected: '{n} selected',
    selectedItems: '{n} selected',
    more: 'More',
    clear: 'Clear selection',
    hint: 'Select orders for batch actions'
  }
}
