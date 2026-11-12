<p align="center">
  <img src="lemon-web/public/favicon.svg" width="96" alt="柠檬跨境ERP" />
</p>

<h1 align="center">柠檬跨境ERP · Lemon Ecom ERP</h1>

<p align="center">
  <b>面向跨境电商 / 电商卖家的开源 ERP</b><br />
  多平台店铺与订单、SKU 映射、FBA / 海外仓与头程、平台结算与利润，和采购、仓库、财务、审批流程放在一套系统里
</p>

<p align="center">
  <a href="https://github.com/MakeSomeFakeNews/lemon-ecom/actions/workflows/ci.yml"><img src="https://github.com/MakeSomeFakeNews/lemon-ecom/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <a href="https://github.com/MakeSomeFakeNews/lemon-ecom/releases/latest"><img src="https://img.shields.io/github/v/release/MakeSomeFakeNews/lemon-ecom" alt="release" /></a>
  <a href="https://github.com/MakeSomeFakeNews/lemon-ecom/stargazers"><img src="https://img.shields.io/github/stars/MakeSomeFakeNews/lemon-ecom?style=flat&logo=github" alt="stars" /></a>
  <img src="https://img.shields.io/badge/license-Apache--2.0-blue.svg" alt="license" />
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen.svg" alt="spring boot" />
  <img src="https://img.shields.io/badge/Vue-3.5-42b883.svg" alt="vue" />
  <img src="https://img.shields.io/badge/Ant%20Design%20Vue-4.2-1677ff.svg" alt="antdv" />
  <img src="https://img.shields.io/badge/Flowable-8.0-ff6a00.svg" alt="flowable" />
</p>

<p align="center">
  <a href="#联系方式"><b>联系我们</b></a> ·
  <a href="#english"><b>English</b></a>
</p>

## 项目简介

柠檬跨境ERP（Lemon Ecom ERP）是一套面向**跨境电商和电商卖家**（亚马逊、速卖通、eBay、Shopee、Lazada、Shopify、TikTok Shop、Temu、沃尔玛、Ozon 等）的开源 ERP，前后端分离，开箱即用。它由开源项目 [柠檬ERP](https://github.com/MakeSomeFakeNews/lemon-erp) 衍生：去掉了生产制造、质量、人力、客户关系、项目管理和商务智能，保留了成熟的仓库、采购、财务、流程中心和系统平台能力，并在此之上新增了跨境电商模块。

- **多平台店铺与订单**：一个系统管理多个平台、多个站点的店铺；平台订单通过 Excel 导入或渠道接口同步进来，按店铺 + 平台 SKU 自动匹配本地商品，批量出库、发货回传，退货退款、取消一并处理
- **SKU 映射与多件装**：平台 SKU 映射到本地物料（支持多规格 SKU），一个平台 SKU 可对应多个物料数量（多件装），库存、成本按物料数量折算
- **FBA / 海外仓与头程**：平台仓就是一个仓库，头程货件从本地仓发往平台仓（在途调拨、签收入库），运费、关税分摊为单件头程成本并计入订单物流成本；导入平台库存报告与系统库存比对；按日均销量、补货周期和安全天数给出补货建议
- **结算与利润**：导入平台结算报告，佣金、配送费、退款回写到订单，净额生成收款单自动核销店铺应收，平台费用作为折让；广告数据按店铺 / SKU 扣减；每张订单、每个店铺、每个 SKU、每个国家的利润都算得出来（收入 − 平台费用 − 退款 − 商品成本 − 物流成本），全部折算本位币
- **多币种**：订单、结算、广告按店铺 / 订单币种保存原币，按月汇率折算本位币；外币应收应付、汇兑损益、期末调汇都在财务模块中
- **进销存与财务一体**：平台订单出库即销售出库，库存减少、成本结转、应收生成；采购入库、退货、调拨、盘点自动更新库存并生成应付；凭证、账簿、三大报表、固定资产、费用报销、预算一套打通
- **可做 SaaS**：内置多租户、租户套餐和行业模板（跨境电商卖家、国内电商卖家、商贸批发、全部功能），软件服务商可以用它为多家卖家提供在线服务
- **AI 能力**：接入 DeepSeek、通义千问、智谱 GLM、Kimi、豆包等大模型，AI 问数可直接问「上个月哪个店铺利润最高」「哪些 SKU 需要紧急补货」，还有智能录入、数据整理和智能报告
- **技术栈新、全部开源**：Spring Boot 4 + Vue 3 + Ant Design Vue 4，Gradle 多模块、单向依赖；全部源码 Apache-2.0 开源，没有商业版和授权码

> 如果这个项目对你有帮助，请点一下右上角的 **Star**，这是对我们最大的鼓励！

## 为什么选择柠檬跨境ERP

1. **按卖家的实际流程设计**：店铺 → 在线商品 → 平台订单 → 出库发货 → 退货退款 → 平台结算 → 利润分析，每一步都和库存、应收、成本联动，不是在进销存上贴一层「电商订单」页面
2. **利润算得清**：订单级估算佣金、结算后回写实际费用，商品成本取出库时的实时移动加权成本，平台配送订单的物流成本按头程货件的单件成本分摊，广告费按店铺 / SKU 扣减，看板、按店铺 / SKU / 国家汇总、结算费用构成一目了然
3. **FBA / 海外仓不是黑盒**：平台仓作为仓库纳入库存体系，头程在途、签收、单件头程成本、平台库存报告差异比对、补货预警都在系统里
4. **业财一体，账实相符**：出库、入库、退货自动生成应收应付和凭证；平台结算直接核销店铺应收并记折让；外币按月汇率折算，期末调汇自动冲回
5. **接口可扩展**：渠道接口按平台实现 `ChannelConnector`（已有亚马逊 SP-API、速卖通的实现，按公开文档编写，接入时按真实账号联调），没有接口的平台用 Excel 导入
6. **拿来就能用**：Docker 一条命令启动；内置示例物流渠道、平台仓、字典和内置角色（电商运营、采购员、仓库管理员、会计、出纳、总经理）；按行业模板一键开通功能；中英文界面
7. **容易二次开发**：主流技术栈，Gradle 多模块、依赖单向，单据审批、状态流转、数据权限、明细行、单据编号、打印、导入导出都有公共组件；新增模块、接入审批、扩展 AI 工具、新增渠道接口都有开发文档
8. **多端协同**：电脑浏览器处理全部业务，微信小程序审批、看消息和公告、商品速查、快速开单，手机 / PDA 扫码收货、上架、盘点
9. **安全可靠**：菜单 / 按钮 / 接口三级权限和五种数据权限，店铺授权信息加密保存且只写不读，操作日志与登录日志，会话可强制下线，登录验证码与密码策略，审批流程留痕
10. **真正开源**：全部功能、全部源码开放，个人与企业均可免费商用，数据部署在自己的服务器上

### 适合谁使用

| 用户 | 能得到什么 |
| --- | --- |
| **跨境电商卖家** | 多平台多站点店铺、平台订单、FBA / 海外仓与头程、平台结算与利润、多币种、补货建议，加上采购、仓库、财务一套闭环 |
| **国内电商卖家** | 店铺与订单管理、SKU 映射、仓库发货、退货退款、结算与利润，按「国内电商卖家」模板只开通需要的功能 |
| **商贸、批发企业** | 采购、销售出库 / 退货直接开单、库存、商品速查、进销存报表、应收应付与资金管理 |
| **软件服务商、实施商** | 免授权费的电商 ERP 底座，可二次开发后交付客户，或用多租户做成 SaaS 服务 |
| **开发者、学生** | 一个完整的企业级 Java 项目：多模块架构、权限与多租户、工作流、第三方平台接口、AI 集成、Docker 部署，可以直接学习和参考 |

## 功能一览

<table>
  <tr><th width="120">模块</th><th>主要功能</th></tr>
  <tr><td><b>电商</b></td><td>平台店铺（平台、站点、结算币种、默认发货仓与平台仓、佣金率、补货参数、渠道授权）、在线商品（平台 SKU 映射本地物料与多件装，Excel 导入）；平台订单（Excel 导入 / 渠道接口同步，自动匹配物料，批量出库生成销售出库，发货与回传，取消）、退货退款（仅退款 / 退货入库，冲减成本、重算利润）；头程货件（本地仓 → 平台仓在途调拨，运费关税分摊为单件头程成本，可接入审批）、平台仓库存（导入平台库存报告与系统库存比对）、补货建议（日均销量、可售天数、紧急 / 预警）、物流渠道（目的国 × 重量段费率、运费试算）；电商看板、平台结算（导入结算报告，费用回写订单，生成收款单核销应收）、广告数据（ACOS、点击率、CPC）、利润分析（按店铺 / SKU / 国家、费用构成）</td></tr>
  <tr><td><b>供应链</b></td><td>采购订单（审批、变更、多币种、整单优惠、发票类型）、采购入库 / 退货（引用订单或直接开单、草稿与审核、运费、订单优惠）、补货建议；销售出库 / 退货记录（直接开单，以及平台订单出库生成的单据）、商品速查（库存与价格、图片、库存分布、价格历史）；供应商（供货价格、物流公司标记）与客户（信用额度、账期、价格等级、执行价、关联供应商、资料合并）；进销存报表：销售报表、进货报表（多维汇总，汇总 → 明细 → 单据穿透）、经营状况、资产状况（可选截至日期）、商品分析（畅销、滞销、积压、缺货、周转）、客户 RFM 分群</td></tr>
  <tr><td><b>仓库</b></td><td>入库、出库、调拨（含在途调拨）、盘点、组装拆卸，冲销；实时库存成本（移动加权平均，库存均价与金额、经营历史）；批次与序列号追踪、库存冻结、收发存汇总、库存流水；物料分类、多规格物料（SKU）、辅助单位与条码、商品图片、价格列、库位区域与上架推荐；手机 / PDA 扫码作业</td></tr>
  <tr><td><b>财务</b></td><td>凭证、账簿、资产负债表 / 利润表 / 现金流量表（小企业会计准则）、期末结账、凭证科目设置；应收应付与核销（自动核销、折让抹零、多收转预收、冲账单、挂接应付与应收应付对冲、坏账，支持外币）、账龄、发票与数电票、往来对账单、供应商对账；出纳日记账、费用报销、员工借款、银行对账、账户调拨、其他收支、资金计划；固定资产与折旧、资产盘点、低值易耗品；存货成本核算（月末加权 / 移动加权 / 先进先出）、库龄分析、预算与预算控制、经营分析；币种汇率与期末调汇</td></tr>
  <tr><td><b>智能（AI）</b></td><td>接入 DeepSeek、通义千问、智谱 GLM、Kimi、豆包等大模型：AI 问数（电商销售与利润、补货建议、库存、采购、应收应付、财务报表等查询工具，按提问人的权限执行）、智能录入（识别图片 / 文件生成采购订单、供应商、客户、物料、报销单）、数据整理（物料、客户、供应商查重与规范化建议）、智能报告（周报 / 月报）</td></tr>
  <tr><td><b>流程中心</b></td><td>仿钉钉流程设计器（条件分支、并行分支、会签 / 或签）与表单设计，发起申请、待办 / 已办 / 抄送、流程实例；采购订单、出入库、盘点差异、凭证、付款、报销、借款、头程货件一键接入审批</td></tr>
  <tr><td><b>系统平台</b></td><td>用户、部门、角色、菜单 / 按钮 / 数据权限；SaaS 多租户、租户套餐与行业模板；编码规则、打印模板（PDF、套打、多份格式）、业务参数、业务字典、自定义字段、附件、变更历史、通知公告、站内消息；钉钉 / 企业微信 / 飞书集成；中英文切换；系统监控（在线用户、日志、服务与缓存监控、定时任务）</td></tr>
  <tr><td><b>小程序 / 移动端</b></td><td>微信小程序：审批、消息、通知公告、商品速查、快速开单、快速入库、单据草稿、库存查询、扫码收货 / 上架 / 盘点；Web 端「移动作业」供手机 / PDA 扫码收货、上架、盘点</td></tr>
</table>

每个功能的设计细节见 [docs/development.md](docs/development.md)。

## 快速开始

### 方式一：Docker 一键部署（推荐）

安装了 Docker（含 Compose v2）时，一条命令启动 MySQL、Redis、后端和前端：

```bash
cd docker
cp .env.example .env        # 修改其中的密码和 JWT_SECRET
docker compose up -d --build
```

启动完成后访问 http://localhost（端口见 `.env` 中的 `WEB_PORT`）。镜像名为 `lemon-ecom-backend` 和 `lemon-ecom-web`，数据库 `lemon_ecom` 在第一次启动时由 `sql/init.sql` 初始化；构建、推送镜像使用 `./docker/build.sh`（`-t` 指定标签，`-p <仓库前缀> --push` 推送到镜像仓库）。

### 方式二：源码运行

```bash
# 1. 初始化数据库（会删除并重建全部表）
mysql -uroot -p -e "CREATE DATABASE lemon_ecom DEFAULT CHARSET utf8mb4"
mysql -uroot -p lemon_ecom < sql/init.sql

# 2. 启动后端（项目根目录执行，默认连接本机 MySQL root / root 和本机 Redis，
#    可用环境变量 DB_URL、DB_USERNAME、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD 覆盖）
DB_PASSWORD=你的密码 ./gradlew bootRun

# 3. 启动前端，访问 http://localhost:5173（/api 自动代理到 http://localhost:8080）
cd lemon-web && npm install && npm run dev
```

不想装 MySQL 和 Redis 时，`./gradlew bootTestRun` 用 H2 内存库和内存缓存启动后端，适合体验和开发。启动完成后会自动生成一套**演示数据**（约 1–2 分钟，看板上显示进度）：一个跨境卖家的 10 个平台店铺（亚马逊美 / 德 / 日、虾皮、TikTok Shop、Temu、速卖通、Shopify、eBay）、40 个带图片的商品、供应商与采购、头程货件、近 120 天约 5000 张订单（各作业环节都有）、退货、结算回款、广告、客服、补货与预测，打开就能看到完整效果；设置环境变量 `LEMON_ECOM_DEMO_DATA=false` 可以关闭。

<details>
<summary>打包部署与 Nginx 参考配置</summary>

```bash
./gradlew build                     # 生成 lemon-admin/build/libs/lemon-ecom.jar
cd lemon-web && npm run build       # 生成 lemon-web/dist，交给 Nginx 托管

# 生产环境配置全部取环境变量，JWT_SECRET 至少 32 字节
java -jar lemon-ecom.jar --spring.profiles.active=prod
```

```nginx
location / {
    root /path/to/lemon-web/dist;
    try_files $uri $uri/ /index.html;
}
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

使用 AI 问数时还需要为 `/api/ai/` 关闭代理缓冲（`proxy_buffering off`），流式回答才能逐段显示。

</details>

**默认账号**：`admin` / `admin123`（超级管理员）、`test` / `admin123`（只读用户）。开启 SaaS 多租户（`LEMON_TENANT_ENABLED=true`）后登录需要填写租户「柠檬科技」；设置 `LEMON_DEMO_ENABLED=true` 开启演示模式（只能查看，不能修改数据）。

**演示数据**：Docker 部署时在 `.env` 中设置 `LEMON_ECOM_DEMO_DATA=true`，后端首次启动完成后自动生成（已生成过则跳过）；也可以在「电商 → 电商看板」没有数据时点击「生成演示数据」（权限 `ecom:demo:generate`，超级管理员和电商运营默认拥有）。演示店铺编码以 `DEMO-` 开头，授权信息是占位值、不会访问真实平台；每个租户只能生成一次。

**接入平台**：在「电商 → 平台店铺」中新建店铺，没有渠道授权时用 Excel 导入订单、结算报告、广告报告和平台库存报告；填写渠道授权后可「测试授权」并手工或定时同步订单（亚马逊 SP-API、速卖通开放平台的接口按公开文档实现，接入前请用自己的账号联调）。

## 技术栈

| 分类 | 技术 | 环境要求 |
| --- | --- | --- |
| 后端 | Java 17、Spring Boot 4.1、Spring Security + JWT、MyBatis-Plus 3.5、Flowable 8、JasperReports 7 | JDK 17 |
| 数据与缓存 | MySQL、Redis（可切换为内存缓存） | MySQL 8.0+、Redis 6.2+ |
| 前端 | Vue 3.5、Vite 7、Ant Design Vue 4.2、Pinia、Vue Router、ECharts 5、vue-i18n | Node 20+ |
| 小程序 | uni-app（Vue 3），微信小程序 | 微信开发者工具 |
| 构建部署 | Gradle 多模块、Docker / Docker Compose、Nginx | Docker 20.10+、Nginx 1.20+ |

## 项目结构

```
lemon-ecom
├── lemon-common        通用层：统一返回、异常、基础实体、工具类
├── lemon-framework     框架层：安全、数据权限、多租户、缓存、日志、模块间契约接口
├── lemon-modules       业务模块
│   ├── lemon-system    系统管理        ├── lemon-scm   供应链：采购、出入库记录、客户 / 供应商、商品速查
│   ├── lemon-monitor   系统监控        ├── lemon-fin   财务管理
│   ├── lemon-workflow  流程中心        ├── lemon-ecom  跨境电商（含进销存报表）
│   ├── lemon-dashboard 工作台          └── lemon-ai    智能（AI）
│   └── lemon-wms       仓库管理
├── lemon-admin         启动模块：组装全部业务模块，打包为可执行 jar
├── lemon-web           前端（Vite + Vue 3 + Ant Design Vue）
├── lemon-mp            微信小程序（uni-app + Vue 3）
├── sql/init.sql        建表与初始数据
├── docker              Dockerfile、docker-compose 与镜像构建脚本
└── docs                开发文档
```

模块依赖只允许单向：`wms ← scm ← fin ← ecom`，`system ← monitor ← dashboard`，`workflow`、`ai` 只依赖 framework。

## 联系方式

商务合作、定制开发、部署实施或使用中遇到问题，欢迎联系：

- 电话：17701123202
- 微信：BG7LIF
- 邮箱：zmissu@163.com

也欢迎在 [Issues](https://github.com/MakeSomeFakeNews/lemon-ecom/issues) 中反馈问题和建议，或提交 Pull Request 一起完善。

## 开源说明

柠檬跨境ERP 不是「开源版 + 商业版」的模式，仓库中的就是全部：

- **全部功能开源**：电商、供应链、仓库、财务、流程中心、AI、系统平台，没有任何模块收费或闭源
- **没有功能限制**：没有授权码和激活，系统本身不限制用户数、店铺数、租户数和单据量
- **免费商用**：遵守 [Apache-2.0](LICENSE) 协议，个人与企业都可以免费使用、修改、二次开发和商业部署，使用时保留原作者版权声明与许可证文件即可
- **来源**：基于 [柠檬ERP](https://github.com/MakeSomeFakeNews/lemon-erp)（Apache-2.0）衍生，保留其版权声明

如果觉得柠檬跨境ERP 不错，请点一个 **Star**，也欢迎推荐给身边做跨境电商、做电商、做软件的朋友。

## English

**Lemon Ecom ERP** is an open-source ERP for cross-border and domestic e-commerce sellers (Amazon, AliExpress, eBay, Shopee, Lazada, Shopify, TikTok Shop, Temu, Walmart, Ozon and more), built with **Spring Boot 4 + Vue 3 + Ant Design Vue 4**. It is derived from the open-source Lemon ERP and keeps its warehouse, purchasing, finance, workflow and platform capabilities, adding an e-commerce module on top:

- Multi-platform, multi-marketplace stores; platform orders imported from Excel or synced through channel connectors (Amazon SP-API and AliExpress implemented from public docs), matched to local materials by store + seller SKU, shipped in batches with shipment confirmation, plus returns, refunds and cancellations
- SKU mapping with pack quantities, FBA / overseas warehouses as regular warehouses, inbound shipments with landed cost allocation, platform stock reconciliation and replenishment suggestions
- Settlement report import that writes fees back to orders and settles the store's receivables, advertising data, and profit analysis by store, SKU and country in the base currency with monthly exchange rates
- Full inventory and accounting: stock movements, receivables / payables, vouchers, financial statements, fixed assets, expenses and budgets
- Visual approval workflow (Flowable), SaaS multi-tenancy with industry templates, menu / button / API permissions and department-level data permissions
- AI assistants with DeepSeek, Qwen, GLM, Kimi or Doubao: natural-language data queries, document recognition, data cleansing and business reports
- WeChat mini program and mobile / PDA barcode scanning, DingTalk / WeCom / Feishu integration, Chinese and English UI, Docker one-click deployment
- Every feature and every line of source code is open: no paid edition, no hidden modules, no license keys; Apache-2.0 licensed, free for commercial use

Try it without MySQL / Redis: `./gradlew bootTestRun` starts the backend on H2 and automatically generates a complete demo seller (10 marketplace stores, 40 products with images, 120 days of orders, inbound shipments, settlements, ads, customer service and more, about 1–2 minutes). With Docker, set `LEMON_ECOM_DEMO_DATA=true` in `.env`; or click "Generate Demo Data" on the empty ecom dashboard.

Contact: zmissu@163.com

---

<p align="center">Apache License 2.0 © 2026 柠檬跨境ERP</p>
