/** Ecom demo data (entry on the ecom dashboard) */
export default {
  title: 'No business data yet. Generate a demo dataset to explore?',
  desc: 'Generates a complete cross-border seller in one click: 10 marketplace stores, 40 products (with images), suppliers and purchasing, inbound shipments, 120 days of orders (across all fulfillment steps), returns, settlements and payouts, ads, customer service, replenishment and forecasts. Takes about 1–2 minutes.',
  generate: 'Generate Demo Data',
  confirmTitle: 'Generate Demo Data',
  confirmContent: 'Demo stores, products, orders, inventory and finance documents will be created in the current tenant (store codes start with DEMO-; their credentials are placeholders and never call real marketplaces). This can only be done once. Continue?',
  running: 'Generating demo data...',
  current: 'Generating: {part} ({done} / {total})',
  done: 'Demo data generated',
  doneWithFailures: 'Demo data generated, {count} part(s) failed (see the dashboard notice; details in the server log)',
  failed: 'These parts failed:',
  failureItem: '{part}: {message}',
  parts: {
    setup: 'Basic Setup',
    users: 'Operator Accounts',
    catalog: 'Products & Suppliers',
    stores: 'Stores',
    listings: 'Listings',
    plan: 'Order Plan',
    purchase: 'Purchasing',
    inbound: 'Inbound Shipments',
    orders: 'Orders',
    'order-ops': 'Order Operations',
    returns: 'Returns & Refunds',
    settlements: 'Settlements & Payouts',
    planning: 'Inventory & Planning',
    ads: 'Advertising',
    cs: 'Customer Service'
  }
}
