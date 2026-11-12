-- 柠檬跨境ERP 初始化脚本（MySQL 8），由柠檬ERP 衍生
-- 默认账号：admin / admin123（超级管理员）、test / admin123（普通用户，只有查看权限，数据权限为本部门及以下）
-- 工作流引擎的 ACT_ 开头的表由 Flowable 启动时自动创建，不在本脚本中

DROP TABLE IF EXISTS ecom_platform_stock;
DROP TABLE IF EXISTS ecom_return_item;
DROP TABLE IF EXISTS ecom_return;
DROP TABLE IF EXISTS ecom_ad_daily;
DROP TABLE IF EXISTS ecom_settlement_item;
DROP TABLE IF EXISTS ecom_settlement;
DROP TABLE IF EXISTS ecom_logistics_rate;
DROP TABLE IF EXISTS ecom_logistics_channel;
DROP TABLE IF EXISTS ecom_inbound_item;
DROP TABLE IF EXISTS ecom_inbound;
DROP TABLE IF EXISTS ecom_order_item;
DROP TABLE IF EXISTS ecom_order;
DROP TABLE IF EXISTS ecom_listing;
DROP TABLE IF EXISTS ecom_sync_log;
DROP TABLE IF EXISTS ecom_store;
DROP TABLE IF EXISTS ai_report;
DROP TABLE IF EXISTS ai_cleanse_suggestion;
DROP TABLE IF EXISTS ai_cleanse_task;
DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS ai_usage;
DROP TABLE IF EXISTS ai_config;
DROP TABLE IF EXISTS fin_account_rule;
DROP TABLE IF EXISTS fin_budget;
DROP TABLE IF EXISTS scm_promotion_usage;
DROP TABLE IF EXISTS scm_promotion;
DROP TABLE IF EXISTS fin_consumable_use_item;
DROP TABLE IF EXISTS fin_consumable_use;
DROP TABLE IF EXISTS fin_asset_check_item;
DROP TABLE IF EXISTS fin_asset_check;
DROP TABLE IF EXISTS fin_asset_move;
DROP TABLE IF EXISTS fin_supplier_statement_line;
DROP TABLE IF EXISTS fin_supplier_statement;
DROP TABLE IF EXISTS scm_purchase_contract_item;
DROP TABLE IF EXISTS scm_purchase_contract;
DROP TABLE IF EXISTS fin_fund_transfer;
DROP TABLE IF EXISTS fin_fund_plan_item;
DROP TABLE IF EXISTS fin_cost_layer;
DROP TABLE IF EXISTS fin_material_cost;
DROP TABLE IF EXISTS fin_cost_run;
DROP TABLE IF EXISTS fin_depreciation;
DROP TABLE IF EXISTS fin_asset;
DROP TABLE IF EXISTS fin_invoice;
DROP TABLE IF EXISTS fin_invoice_line;
DROP TABLE IF EXISTS fin_invoice_item;
DROP TABLE IF EXISTS fin_advance;
DROP TABLE IF EXISTS fin_advance_apply;
DROP TABLE IF EXISTS fin_bank_statement;
DROP TABLE IF EXISTS fin_expense_claim;
DROP TABLE IF EXISTS fin_expense_claim_item;
DROP TABLE IF EXISTS fin_loan;
DROP TABLE IF EXISTS fin_loan_repay;
DROP TABLE IF EXISTS fin_offset;
DROP TABLE IF EXISTS fin_netting_item;
DROP TABLE IF EXISTS fin_netting;
DROP TABLE IF EXISTS fin_io_doc_item;
DROP TABLE IF EXISTS fin_io_doc;
DROP TABLE IF EXISTS fin_io_category;
DROP TABLE IF EXISTS fin_settlement_item;
DROP TABLE IF EXISTS fin_settlement;
DROP TABLE IF EXISTS fin_bill;
DROP TABLE IF EXISTS fin_voucher_entry;
DROP TABLE IF EXISTS fin_voucher;
DROP TABLE IF EXISTS fin_period;
DROP TABLE IF EXISTS fin_account_opening;
DROP TABLE IF EXISTS fin_exchange_rate;
DROP TABLE IF EXISTS fin_currency;
DROP TABLE IF EXISTS fin_account;
DROP TABLE IF EXISTS scm_shipment;
DROP TABLE IF EXISTS scm_trade_draft_item;
DROP TABLE IF EXISTS scm_trade_draft;
DROP TABLE IF EXISTS scm_trade_doc_item;
DROP TABLE IF EXISTS scm_trade_doc;
DROP TABLE IF EXISTS scm_order_item;
DROP TABLE IF EXISTS scm_order_change_item;
DROP TABLE IF EXISTS scm_order_change;
DROP TABLE IF EXISTS scm_order;
DROP TABLE IF EXISTS scm_customer;
DROP TABLE IF EXISTS scm_supplier_eval;
DROP TABLE IF EXISTS scm_supplier_price_tier;
DROP TABLE IF EXISTS scm_price_list;
DROP TABLE IF EXISTS scm_price_control;
DROP TABLE IF EXISTS scm_customer_price;
DROP TABLE IF EXISTS scm_price_memory;
DROP TABLE IF EXISTS scm_supplier_price;
DROP TABLE IF EXISTS scm_supplier;
DROP TABLE IF EXISTS wms_doc_sequence;
DROP TABLE IF EXISTS wms_check_item;
DROP TABLE IF EXISTS wms_check;
DROP TABLE IF EXISTS wms_assembly_item;
DROP TABLE IF EXISTS wms_assembly;
DROP TABLE IF EXISTS wms_stock_doc_item;
DROP TABLE IF EXISTS wms_stock_doc;
DROP TABLE IF EXISTS wms_serial_log;
DROP TABLE IF EXISTS wms_stock_flow;
DROP TABLE IF EXISTS wms_serial;
DROP TABLE IF EXISTS wms_reservation;
DROP TABLE IF EXISTS wms_stock_hold;
DROP TABLE IF EXISTS wms_stock;
DROP TABLE IF EXISTS wms_batch;
DROP TABLE IF EXISTS wms_variant_value;
DROP TABLE IF EXISTS wms_attribute_value;
DROP TABLE IF EXISTS wms_attribute;
DROP TABLE IF EXISTS wms_material_unit;
DROP TABLE IF EXISTS wms_material_image;
DROP TABLE IF EXISTS wms_price_level;
DROP TABLE IF EXISTS wms_material;
DROP TABLE IF EXISTS wms_material_category;
DROP TABLE IF EXISTS wms_location;
DROP TABLE IF EXISTS wms_warehouse;
DROP TABLE IF EXISTS wf_copy;
DROP TABLE IF EXISTS wf_model;
DROP TABLE IF EXISTS sys_change_log;
DROP TABLE IF EXISTS sys_attachment;
DROP TABLE IF EXISTS sys_notice_recipient;
DROP TABLE IF EXISTS sys_notice_target;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_job;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_tenant;
DROP TABLE IF EXISTS sys_code_rule;
DROP TABLE IF EXISTS sys_biz_rule;
DROP TABLE IF EXISTS sys_im_config;
DROP TABLE IF EXISTS sys_im_user;
DROP TABLE IF EXISTS sys_im_dept;
DROP TABLE IF EXISTS sys_miniapp_user;
DROP TABLE IF EXISTS sys_custom_value;
DROP TABLE IF EXISTS sys_custom_field;
DROP TABLE IF EXISTS sys_print_template;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_tenant_package_menu;
DROP TABLE IF EXISTS sys_tenant_package;

-- 租户：SaaS 模式（lemon.tenant.enabled）下业务表按 tenant_id 隔离；租户 1 是平台租户（运营方），
-- 未开启 SaaS 模式时所有数据都属于它。菜单、字典、定时任务等平台共享的表没有 tenant_id
CREATE TABLE sys_tenant_package (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '套餐名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_package_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户套餐';

CREATE TABLE sys_tenant_package_menu (
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    menu_id    BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (package_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '套餐可用菜单';

CREATE TABLE sys_tenant (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(64)  NOT NULL COMMENT '租户名称，登录时填写',
    package_id    BIGINT                DEFAULT NULL COMMENT '套餐，平台租户为空（不受限）',
    contact_name  VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    expire_time   DATETIME              DEFAULT NULL COMMENT '到期时间，为空不限',
    account_limit INT                   DEFAULT NULL COMMENT '账号数量上限，为空不限',
    admin_user_id BIGINT                DEFAULT NULL COMMENT '租户管理员（创建租户时开通）',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_name (name),
    KEY idx_tenant_package (package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户';

-- 编码规则：租户修改过的单据编号规则，未修改的单据使用各模块声明的默认规则（DocNoCatalog）
CREATE TABLE sys_code_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(16) NOT NULL COMMENT '规则标识（默认前缀），如 CG',
    prefix      VARCHAR(8)  NOT NULL COMMENT '前缀',
    date_format VARCHAR(16) NOT NULL COMMENT '日期格式 YYYYMMDD / YYMMDD / YYYYMM / YYMM / YYYY / NONE',
    seq_length  INT         NOT NULL COMMENT '流水位数',
    reset_cycle VARCHAR(8)  NOT NULL COMMENT '流水重置周期 DAY / MONTH / YEAR / NEVER',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '编码规则';

-- 系统参数（参数设置）：平台级，所有租户共用；只保存修改过的参数，参数定义见 framework/setting/Setting
CREATE TABLE sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(64)  NOT NULL COMMENT '参数标识，如 captcha.mode',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数';

-- 打印模板：租户修改过的单据打印配置（JSON，由前端解释），未修改的单据使用默认版式；上传了 JRXML 的单据按 JRXML 打印。
-- 同一单据可有多份打印格式（每行一份，如销售出库单的「发货单（不含价格）」），每个模板标识有且只有一份默认格式，有多份启用的格式时打印前选择
CREATE TABLE sys_print_template (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_key VARCHAR(64) NOT NULL COMMENT '模板标识，如 scm.purchaseOrder；_common 为通用设置',
    name         VARCHAR(32) NOT NULL COMMENT '格式名称，同一模板标识内唯一；只有一份格式时为「标准格式」',
    is_default   TINYINT     NOT NULL DEFAULT 1 COMMENT '是否默认格式 1是 0否：每个模板标识只有一份，只有一份启用的格式时直接按它打印',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序（打印时选择格式的顺序）',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（默认格式不能停用）',
    config       TEXT        NOT NULL COMMENT '模板配置 JSON',
    jrxml        MEDIUMTEXT COMMENT '自定义 JRXML（Jaspersoft Studio 7），为空时按配置生成默认版式',
    background   MEDIUMTEXT COMMENT '套打底图（data URL）：设计套打位置时对照，不打印',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_print_template (template_key, name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '打印模板';

CREATE TABLE sys_biz_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(64)  NOT NULL COMMENT '参数标识，如 scm.purchaseIn.overRate',
    rule_value  VARCHAR(255) NOT NULL COMMENT '参数值',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务参数（按租户）：各模块的业务控制规则，只保存修改过的';

CREATE TABLE sys_im_config (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    platform          VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK钉钉 WECOM企业微信 FEISHU飞书',
    enabled           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用 1是 0否',
    corp_id           VARCHAR(64)           DEFAULT NULL COMMENT '企业ID（企业微信 corpid）',
    app_key           VARCHAR(64)           DEFAULT NULL COMMENT '应用标识（钉钉 Client ID / AppKey，飞书 App ID）',
    app_secret        VARCHAR(128)          DEFAULT NULL COMMENT '应用密钥（钉钉 Client Secret，企业微信应用 Secret，飞书 App Secret），不返回前端',
    agent_id          VARCHAR(32)           DEFAULT NULL COMMENT '应用 AgentId（钉钉、企业微信，发送工作通知用）',
    home_url          VARCHAR(255)          DEFAULT NULL COMMENT '系统访问地址（外网可访问的前端地址），登录回调和消息链接使用',
    login_enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '扫码登录与应用内免登 1开启 0关闭',
    message_enabled   TINYINT      NOT NULL DEFAULT 1 COMMENT '站内消息同步推送给已绑定的成员 1开启 0关闭',
    sync_create       TINYINT      NOT NULL DEFAULT 0 COMMENT '同步通讯录时自动创建部门和账号 1是 0否（否则只按手机号绑定已有账号）',
    robot_webhook     VARCHAR(500)          DEFAULT NULL COMMENT '群机器人 Webhook 地址',
    robot_secret      VARCHAR(128)          DEFAULT NULL COMMENT '群机器人加签密钥（钉钉、飞书），不返回前端',
    robot_categories  VARCHAR(64)           DEFAULT NULL COMMENT '推送到群机器人的消息类别（逗号分隔，字典 sys_message_category）',
    last_sync_time    DATETIME              DEFAULT NULL COMMENT '最近同步通讯录时间',
    last_sync_result  VARCHAR(255)          DEFAULT NULL COMMENT '最近同步结果',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_config (platform, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '第三方集成（钉钉、企业微信、飞书）的应用配置，按租户';

CREATE TABLE sys_im_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '系统用户',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK WECOM FEISHU',
    im_user_id  VARCHAR(64)  NOT NULL COMMENT '平台中的成员 userid',
    im_name     VARCHAR(64)           DEFAULT NULL COMMENT '平台中的成员姓名',
    create_time DATETIME              DEFAULT NULL COMMENT '绑定时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_user_member (platform, im_user_id, tenant_id),
    UNIQUE KEY uk_im_user_user (user_id, platform, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户与钉钉 / 企业微信 / 飞书成员的绑定';

CREATE TABLE sys_im_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    dept_id     BIGINT       NOT NULL COMMENT '系统部门',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK WECOM FEISHU',
    im_dept_id  VARCHAR(64)  NOT NULL COMMENT '平台中的部门 ID',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_dept (platform, im_dept_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '同步通讯录时系统部门与平台部门的对应关系';

CREATE TABLE sys_miniapp_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '系统用户',
    openid      VARCHAR(64)  NOT NULL COMMENT '微信小程序 openid',
    create_time DATETIME              DEFAULT NULL COMMENT '绑定时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_miniapp_user_openid (openid, tenant_id),
    UNIQUE KEY uk_miniapp_user_user (user_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户与微信小程序的绑定（登录时按 openid 跨租户查询）';

CREATE TABLE sys_user_setting (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id       BIGINT      NOT NULL COMMENT '用户ID',
    setting_key   VARCHAR(32) NOT NULL COMMENT '设置项标识：ui 界面设置（主题、主题色、布局、多标签页、紧凑模式等）',
    setting_value TEXT                 DEFAULT NULL COMMENT '设置内容（JSON 对象）',
    create_time   DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_setting (user_id, setting_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户个人设置（只能本人读写）';

CREATE TABLE sys_custom_field (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（与附件相同的业务记录类型，如 scm_customer、wms_material）',
    field_key    VARCHAR(32)  NOT NULL COMMENT '字段标识，字母开头的字母、数字、下划线',
    label        VARCHAR(32)  NOT NULL COMMENT '显示名称',
    field_type   VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '类型(字典 sys_custom_field_type) TEXT文本 TEXTAREA多行文本 NUMBER数字 DATE日期 SELECT下拉 BOOL是否',
    options      VARCHAR(1000)         DEFAULT NULL COMMENT '下拉选项，逗号或换行分隔',
    required     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填',
    show_in_list TINYINT      NOT NULL DEFAULT 0 COMMENT '是否在列表中显示',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '说明（显示为输入提示）',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_field (biz_type, field_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段：为单据、主数据扩展字段，不改表结构';

CREATE TABLE sys_custom_value (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)   NOT NULL COMMENT '业务类型',
    biz_id      BIGINT        NOT NULL COMMENT '业务记录ID',
    field_key   VARCHAR(32)   NOT NULL COMMENT '字段标识',
    field_value VARCHAR(1000) NOT NULL COMMENT '值（文本；数字、日期、是否按约定格式）',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_value (biz_type, biz_id, field_key, tenant_id),
    KEY idx_custom_value_field (biz_type, field_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段的值';

CREATE TABLE sys_dept (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级部门ID，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '部门名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    leader_user_id BIGINT               DEFAULT NULL COMMENT '负责人（用户ID），审批人「部门主管」据此计算',
    phone       VARCHAR(32)          DEFAULT NULL COMMENT '联系电话',
    email       VARCHAR(128)         DEFAULT NULL COMMENT '邮箱',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dept_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门';

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dept_id     BIGINT                DEFAULT NULL COMMENT '部门ID',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    pwd_update_time DATETIME              DEFAULT NULL COMMENT '最近设置密码的时间（密码有效期）',
    pwd_reset   TINYINT      NOT NULL DEFAULT 0 COMMENT '1 密码由管理员设置，登录后提示本人修改',
    nickname    VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    supplier_id BIGINT                DEFAULT NULL COMMENT '供应商门户账号关联的供应商，为空是内部用户',
    customer_id BIGINT                DEFAULT NULL COMMENT '经销商门户账号关联的客户，为空是内部用户',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, tenant_id),
    KEY idx_user_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    data_scope  TINYINT     NOT NULL DEFAULT 3 COMMENT '数据权限(字典 sys_data_scope) 1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

CREATE TABLE sys_menu (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '菜单名称',
    type        CHAR(1)     NOT NULL COMMENT '类型 M目录 C菜单 F按钮',
    path        VARCHAR(200)         DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(200)         DEFAULT NULL COMMENT '组件路径(相对 src/views，不带 .vue)',
    perms       VARCHAR(100)         DEFAULT NULL COMMENT '权限标识',
    icon        VARCHAR(64)          DEFAULT NULL COMMENT '图标',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    visible     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否显示 1显示 0隐藏',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    -- 按角色反查用户（删除角色前检查、角色关联用户）
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    -- 删除菜单时清理关联
    KEY idx_menu_id (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联';

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id),
    -- 删除部门时清理关联
    KEY idx_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色自定义数据权限（部门）';

CREATE TABLE sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '字典名称',
    type        VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    kind        VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM' COMMENT '种类 SYSTEM系统字典（取值由程序内置，全平台共用，只能改名称、颜色、排序） BUSINESS业务字典（每个租户各自维护，可新增取值）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型';

CREATE TABLE sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID：系统字典只有平台（1）一份；业务字典每个租户一份，新租户开通时从平台复制',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典键值',
    tag_color   VARCHAR(32)           DEFAULT NULL COMMENT '标签颜色(ant-design-vue Tag color)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置取值 1是：程序依赖该取值，不能删除、不能改键值（系统字典的取值全部内置）',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    ext_value   VARCHAR(32)           DEFAULT NULL COMMENT '扩展值：只用于业务模块声明了扩展值的字典（如开票类型 scm_invoice_kind 的税率 %），其他字典为空',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据';

CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title          VARCHAR(64)           DEFAULT NULL COMMENT '模块标题',
    oper_type      TINYINT      NOT NULL DEFAULT 0 COMMENT '操作类型(字典 sys_oper_type)',
    method         VARCHAR(200)          DEFAULT NULL COMMENT '类名.方法名',
    request_method VARCHAR(10)           DEFAULT NULL COMMENT '请求方式',
    url            VARCHAR(255)          DEFAULT NULL COMMENT '请求地址',
    ip             VARCHAR(64)           DEFAULT NULL COMMENT 'IP',
    location       VARCHAR(128)          DEFAULT NULL COMMENT 'IP 归属地',
    operator       VARCHAR(64)           DEFAULT NULL COMMENT '操作人',
    params         TEXT COMMENT '请求参数',
    result         TEXT COMMENT '返回结果',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    error_msg      VARCHAR(2000)         DEFAULT NULL COMMENT '错误信息',
    cost_time      BIGINT                DEFAULT NULL COMMENT '耗时(毫秒)',
    oper_time      DATETIME              DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

CREATE TABLE sys_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    category    VARCHAR(16)  NOT NULL COMMENT '类别(字典 sys_message_category) APPROVAL审批 REMIND提醒 ALERT预警 SYSTEM系统 NOTICE公告',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '内容',
    link        VARCHAR(255)          DEFAULT NULL COMMENT '前端路由，点击跳转',
    dedup_key   VARCHAR(128)          DEFAULT NULL COMMENT '去重键：同一用户相同去重键只发送一次（定时提醒）',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 1是 0否',
    read_time   DATETIME              DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_message_user (user_id, read_flag),
    KEY idx_message_dedup (dedup_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息';

CREATE TABLE sys_notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title        VARCHAR(128) NOT NULL COMMENT '标题',
    type         VARCHAR(32)  NOT NULL COMMENT '类型(业务字典 sys_notice_type)',
    content      MEDIUMTEXT COMMENT '正文(Markdown)',
    scope_type   VARCHAR(16)  NOT NULL DEFAULT 'ALL' COMMENT '发布范围(字典 sys_notice_scope) ALL全员 DEPT指定部门(含下级) ROLE指定角色',
    pinned       TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶 1是 0否',
    expire_date  DATE                  DEFAULT NULL COMMENT '有效期至(含当天)，为空长期有效，过期后员工不再看到',
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 sys_notice_status) DRAFT草稿 PUBLISHED已发布 WITHDRAWN已撤回',
    publish_time DATETIME              DEFAULT NULL COMMENT '最近发布时间',
    publish_by   BIGINT                DEFAULT NULL COMMENT '发布人',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_notice_status (status, publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告';

CREATE TABLE sys_notice_target (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    notice_id   BIGINT NOT NULL COMMENT '公告ID',
    target_id   BIGINT NOT NULL COMMENT '部门ID(指定部门)或角色ID(指定角色)',
    PRIMARY KEY (id),
    KEY idx_notice_target (notice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告发布范围';

CREATE TABLE sys_notice_recipient (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT   NOT NULL DEFAULT 1 COMMENT '租户ID',
    notice_id   BIGINT   NOT NULL COMMENT '公告ID',
    user_id     BIGINT   NOT NULL COMMENT '接收人(发布时按范围确定)',
    read_time   DATETIME          DEFAULT NULL COMMENT '阅读时间，未读为空',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notice_recipient (notice_id, user_id, tenant_id),
    KEY idx_notice_recipient_user (user_id, read_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告接收人与阅读记录';

CREATE TABLE sys_attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（AttachmentType.code），如 scm_purchase',
    biz_id       BIGINT       NOT NULL COMMENT '业务记录ID',
    file_name    VARCHAR(255) NOT NULL COMMENT '原文件名',
    file_size    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    content_type VARCHAR(128)          DEFAULT NULL COMMENT '文件类型',
    storage_path VARCHAR(255) NOT NULL COMMENT '相对附件目录的存储路径',
    create_by    BIGINT                DEFAULT NULL COMMENT '上传人',
    create_time  DATETIME              DEFAULT NULL COMMENT '上传时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_attachment_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务附件';

CREATE TABLE sys_change_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)  NOT NULL COMMENT '业务类型（RecordType.code），如 wms_material',
    biz_id      BIGINT       NOT NULL COMMENT '业务记录ID',
    field_name  VARCHAR(64)  NOT NULL COMMENT '字段',
    field_label VARCHAR(64)           DEFAULT NULL COMMENT '字段名称',
    old_value   VARCHAR(500)          DEFAULT NULL COMMENT '旧值',
    new_value   VARCHAR(500)          DEFAULT NULL COMMENT '新值',
    create_by   BIGINT                DEFAULT NULL COMMENT '修改人',
    create_time DATETIME              DEFAULT NULL COMMENT '修改时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_change_log_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字段变更历史';

CREATE TABLE sys_job (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_key        VARCHAR(64)  NOT NULL COMMENT '任务标识（JobHandler.key）',
    name           VARCHAR(64)  NOT NULL COMMENT '任务名称',
    cron           VARCHAR(64)  NOT NULL COMMENT 'Cron 表达式（秒 分 时 日 月 周）',
    params         VARCHAR(255)          DEFAULT NULL COMMENT '参数 key=value，多个用逗号分隔',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_run_time  DATETIME              DEFAULT NULL COMMENT '最近执行时间',
    last_status    VARCHAR(16)           DEFAULT NULL COMMENT '最近执行结果 SUCCESS成功 FAILED失败',
    last_message   VARCHAR(500)          DEFAULT NULL COMMENT '最近执行说明',
    remark         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_key (job_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务';

CREATE TABLE sys_job_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_key      VARCHAR(64)  NOT NULL COMMENT '任务标识',
    trigger_type VARCHAR(16)  NOT NULL COMMENT '触发方式 SCHEDULE定时 MANUAL手动',
    start_time   DATETIME     NOT NULL COMMENT '开始时间',
    cost_ms      BIGINT                DEFAULT NULL COMMENT '耗时（毫秒）',
    status       VARCHAR(16)  NOT NULL COMMENT '结果 SUCCESS成功 FAILED失败',
    message      VARCHAR(500)          DEFAULT NULL COMMENT '执行说明或错误信息',
    create_by    BIGINT                DEFAULT NULL COMMENT '手动执行人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_job_log_key (job_key, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务执行日志';

CREATE TABLE sys_login_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    username   VARCHAR(64)          DEFAULT NULL COMMENT '用户名',
    ip         VARCHAR(64)          DEFAULT NULL COMMENT 'IP',
    location   VARCHAR(128)         DEFAULT NULL COMMENT 'IP 归属地',
    browser    VARCHAR(64)          DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(64)          DEFAULT NULL COMMENT '操作系统',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    msg        VARCHAR(255)         DEFAULT NULL COMMENT '提示信息',
    login_time DATETIME             DEFAULT NULL COMMENT '登录时间',
    PRIMARY KEY (id),
    -- 首页今日登录数、近 7 天趋势只需这两列，覆盖索引不回表
    KEY idx_login_time_status (login_time, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志';

CREATE TABLE wf_model (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    model_key     VARCHAR(64)  NOT NULL COMMENT '流程标识（BPMN 流程 ID），发布后不可修改',
    name          VARCHAR(64)  NOT NULL COMMENT '流程名称',
    category      VARCHAR(64)           DEFAULT NULL COMMENT '分类(字典 wf_category)',
    icon          VARCHAR(64)           DEFAULT NULL COMMENT '图标',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（停用后不能发起）',
    version       INT                   DEFAULT NULL COMMENT '最新发布的版本号，NULL 为未发布',
    definition_id VARCHAR(64)           DEFAULT NULL COMMENT '最新发布的流程定义ID',
    deploy_time   DATETIME              DEFAULT NULL COMMENT '最近发布时间',
    design_json   MEDIUMTEXT COMMENT '流程设计（表单 + 节点树）JSON 草稿',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_key (model_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程模型';

CREATE TABLE wf_copy (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    process_instance_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
    process_name        VARCHAR(255)         DEFAULT NULL COMMENT '流程标题（快照）',
    process_key         VARCHAR(64)          DEFAULT NULL COMMENT '流程标识',
    node_id             VARCHAR(64) NOT NULL COMMENT '抄送节点ID',
    node_name           VARCHAR(64)          DEFAULT NULL COMMENT '抄送节点名称',
    start_user_id       BIGINT               DEFAULT NULL COMMENT '发起人',
    user_id             BIGINT      NOT NULL COMMENT '被抄送人',
    create_time         DATETIME             DEFAULT NULL COMMENT '抄送时间',
    update_time         DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 抄送我的：按用户查询，按 id 倒序
    KEY idx_copy_user_id (user_id, id),
    -- 流程详情中的抄送记录
    KEY idx_copy_instance_id (process_instance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程抄送';

-- ---------------------------------------------------------------- 仓库管理

CREATE TABLE wms_warehouse (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '仓库编码',
    name        VARCHAR(64)  NOT NULL COMMENT '仓库名称',
    type        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '仓库类型(字典 wms_warehouse_type) NORMAL普通 RAW原料 FINISHED成品 DEFECT不良品 CONSIGN_OUT客户寄售仓 CONSIGN_IN供应商寄售仓（不良品、寄售仓不计入可用库存，供应商寄售仓不计入存货成本）',
    partner_id  BIGINT                DEFAULT NULL COMMENT '寄售仓的客户（客户寄售仓）/ 供应商（供应商寄售仓）',
    address     VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    contact     VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '仓库';

CREATE TABLE wms_location (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id BIGINT       NOT NULL COMMENT '所属仓库',
    code         VARCHAR(32)  NOT NULL COMMENT '库位编码，仓库内唯一，如 A-01-02',
    name         VARCHAR(64)           DEFAULT NULL COMMENT '库位名称',
    zone         VARCHAR(32)           DEFAULT NULL COMMENT '区域，如 A 区、冷藏区；拣货路径按 区域 → 排序 → 编码',
    loc_type     VARCHAR(16)  NOT NULL DEFAULT 'STORAGE' COMMENT '类型(字典 wms_location_type) STORAGE存储位 PICK拣货位 RECEIVE收货暂存 SHIP发货暂存 DEFECT不良品位',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序（拣货路径顺序）',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_location_code (warehouse_id, code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库位';

CREATE TABLE wms_material (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code           VARCHAR(64)   NOT NULL COMMENT '物料编码',
    name           VARCHAR(128)  NOT NULL COMMENT '物料名称',
    spec           VARCHAR(128)           DEFAULT NULL COMMENT '规格型号',
    unit           VARCHAR(16)            DEFAULT NULL COMMENT '计量单位',
    barcode        VARCHAR(64)            DEFAULT NULL COMMENT '条码（基本单位），扫码录入物料',
    category       VARCHAR(64)            DEFAULT NULL COMMENT '分类编码(wms_material_category.code，多级分类)',
    brand          VARCHAR(64)            DEFAULT NULL COMMENT '品牌(业务字典 wms_material_brand)',
    batch_enabled  TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用批次管理 1是 0否',
    serial_enabled TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用序列号管理 1是 0否',
    iqc_required   TINYINT        NOT NULL DEFAULT 0 COMMENT '来料检验 1是：采购入库后冻结为待检，检验合格后放行',
    safety_stock   DECIMAL(18, 4)         DEFAULT NULL COMMENT '安全库存，汇总库存低于该值时预警',
    reorder_point  DECIMAL(18, 4)         DEFAULT NULL COMMENT '再订货点：预计库存（可用 + 在途 + 在制 − 待发 − 待领）不高于此值时建议补货，为空不参与',
    reorder_qty    DECIMAL(18, 4)         DEFAULT NULL COMMENT '再订货量（经济批量），补货建议不少于此数量',
    default_warehouse_id BIGINT       DEFAULT NULL COMMENT '默认仓库：录入出入库单、订单时默认带出',
    default_location_id  BIGINT       DEFAULT NULL COMMENT '默认库位（默认仓库中的）',
    ref_cost       DECIMAL(18, 4)         DEFAULT NULL COMMENT '参考成本价：没有核算单价时（首次核算的其他入库等）按此计价',
    retail_price      DECIMAL(18, 4)      DEFAULT NULL COMMENT '零售价（含税，基本单位，本位币）：价格列 0',
    wholesale_price1  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价1（含税）：价格列 1，名称在价格列设置中修改',
    wholesale_price2  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价2（含税）',
    wholesale_price3  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价3（含税）',
    wholesale_price4  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价4（含税）',
    wholesale_price5  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价5（含税）',
    wholesale_price6  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价6（含税）',
    wholesale_price7  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价7（含税）',
    wholesale_price8  DECIMAL(18, 4)      DEFAULT NULL COMMENT '批发价8（含税）',
    cost_method    VARCHAR(16)   NOT NULL DEFAULT 'WEIGHTED' COMMENT '存货计价方法(字典 wms_cost_method) WEIGHTED月末一次加权平均 MOVING移动加权平均 FIFO先进先出',
    is_template    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否多规格款式 1是：按规格属性生成 SKU，款式本身不能出入库',
    template_id    BIGINT                 DEFAULT NULL COMMENT 'SKU 所属的款式（物料）',
    variant_text   VARCHAR(255)           DEFAULT NULL COMMENT 'SKU 的规格属性值，如 红色 / XL',
    image_url      VARCHAR(255)           DEFAULT NULL COMMENT '主图 URL（wms_material_image 排在第一张的图片），由图片维护自动更新',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    search_key   VARCHAR(1000)          DEFAULT NULL COMMENT '检索列：编码、名称、规格、条码去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_code (code, tenant_id),
    KEY idx_material_barcode (barcode),
    KEY idx_material_template (template_id),
    KEY idx_material_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料';

CREATE TABLE wms_material_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级分类ID，0为一级分类',
    ancestors   VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '祖级ID路径（逗号分隔，从 0 开始，如 0,3,7），查询下级分类用',
    code        VARCHAR(16)  NOT NULL COMMENT '分类编码：物料、检验方案、促销、提成规则、凭证科目设置等按编码关联，保存后不能修改',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称（同一上级下不重复）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '同级排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（停用后连同下级不能再被新物料选用）',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置分类 1是：程序依赖（原材料、半成品、成品、低值易耗品），不能删除',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_category_code (code, tenant_id),
    KEY idx_material_category_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料分类（多级，每个租户一份）';

CREATE TABLE wms_price_level (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    level       INT         NOT NULL COMMENT '价格列 0零售价 1-8批发价1-8（对应 wms_material.retail_price / wholesale_price1-8）',
    name        VARCHAR(32) NOT NULL COMMENT '显示名称',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用：停用的列在物料表单、列表、导入导出和价格选择中隐藏',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_level (level, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格列设置：物料的零售价、批发价1-8的名称和是否启用（按租户）';

CREATE TABLE wms_attribute (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(32) NOT NULL COMMENT '规格属性名称，如 颜色、尺码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性：多规格物料（款式）按属性值组合生成 SKU';

CREATE TABLE wms_attribute_value (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    attribute_id BIGINT      NOT NULL COMMENT '规格属性',
    attr_value   VARCHAR(32) NOT NULL COMMENT '属性值，如 红色、XL',
    code         VARCHAR(16)          DEFAULT NULL COMMENT '编码，生成 SKU 编码时使用，为空用属性值',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_value (attribute_id, attr_value, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性值';

CREATE TABLE wms_variant_value (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_id  BIGINT NOT NULL COMMENT '款式',
    material_id  BIGINT NOT NULL COMMENT 'SKU',
    attribute_id BIGINT NOT NULL COMMENT '规格属性',
    value_id     BIGINT NOT NULL COMMENT '属性值',
    create_time  DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_variant_value (material_id, attribute_id, tenant_id),
    KEY idx_variant_template (template_id),
    KEY idx_variant_value (value_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 的规格属性值';

CREATE TABLE wms_material_unit (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    unit        VARCHAR(16)    NOT NULL COMMENT '辅助单位，如 箱',
    ratio       DECIMAL(18, 6) NOT NULL COMMENT '换算率：1 辅助单位 = ratio 基本单位',
    barcode     VARCHAR(64)             DEFAULT NULL COMMENT '包装条码，扫码时按换算率带出数量',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_unit (material_id, unit, tenant_id),
    KEY idx_material_unit_barcode (barcode)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料辅助计量单位：库存、成本都按基本单位，单据可按辅助单位录入后换算';

CREATE TABLE wms_material_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT       NOT NULL COMMENT '物料',
    url         VARCHAR(255) NOT NULL COMMENT '图片访问 URL（公开文件 /api/files/material/...）',
    file_name   VARCHAR(255)          DEFAULT NULL COMMENT '原文件名',
    file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，最小的为主图',
    create_by   BIGINT                DEFAULT NULL COMMENT '上传人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_material_image (material_id, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料（商品）图片：排在第一张的为主图，同步到 wms_material.image_url';

CREATE TABLE wms_batch (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id     BIGINT      NOT NULL COMMENT '物料',
    batch_no        VARCHAR(64) NOT NULL COMMENT '批次号',
    production_date DATE                 DEFAULT NULL COMMENT '生产日期',
    expiry_date     DATE                 DEFAULT NULL COMMENT '有效期至',
    create_time     DATETIME             DEFAULT NULL COMMENT '首次入库时间',
    update_time     DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch (material_id, batch_no, tenant_id),
    -- 批次追踪：按批次号查询、临期预警
    KEY idx_batch_no (batch_no),
    KEY idx_batch_expiry (expiry_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '批次';

CREATE TABLE wms_stock (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定库位',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号，未启用批次管理为空串',
    qty          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量，不会小于 0',
    frozen_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '冻结数量（待检、不合格），不能出库',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock (warehouse_id, location_id, material_id, batch_no, tenant_id),
    KEY idx_stock_material (material_id, batch_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存';

CREATE TABLE wms_stock_hold (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 wms_hold_status) PENDING待检 REJECTED不合格 RELEASED已放行 RETURNED已退货',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    hold_qty     DECIMAL(18, 4) NOT NULL COMMENT '冻结时的数量',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '仍冻结的数量',
    source_no    VARCHAR(32)             DEFAULT NULL COMMENT '采购入库单号',
    order_id     BIGINT                  DEFAULT NULL COMMENT '采购订单',
    partner_id   BIGINT                  DEFAULT NULL COMMENT '供应商',
    insp_no      VARCHAR(32)             DEFAULT NULL COMMENT '检验单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    release_by   BIGINT                  DEFAULT NULL COMMENT '放行人',
    release_time DATETIME                DEFAULT NULL COMMENT '放行时间',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_hold_source (source_no, material_id),
    KEY idx_hold_order (order_id, material_id),
    KEY idx_hold_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存冻结（来料待检、不合格）';

CREATE TABLE wms_reservation (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id   BIGINT         NOT NULL COMMENT '仓库',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '剩余预留数量（出库核销、释放后减少，0 表示已结束）',
    source_type    VARCHAR(16)    NOT NULL COMMENT '来源类型 SALES销售订单',
    source_id      BIGINT         NOT NULL COMMENT '来源单据',
    source_item_id BIGINT                  DEFAULT NULL COMMENT '来源明细',
    source_no      VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '预留人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reservation_slot (warehouse_id, material_id),
    KEY idx_reservation_source (source_type, source_id),
    KEY idx_reservation_item (source_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存预留：仓库中可用库存不能低于全部剩余预留';

CREATE TABLE wms_serial (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT      NOT NULL COMMENT '物料',
    serial_no    VARCHAR(64) NOT NULL COMMENT '序列号',
    warehouse_id BIGINT      NOT NULL COMMENT '当前（或最后所在）仓库',
    location_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '当前（或最后所在）库位，0 为未指定',
    batch_no     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    status       VARCHAR(16) NOT NULL COMMENT '状态(字典 wms_serial_status) IN_STOCK在库 OUT已出库',
    create_time  DATETIME             DEFAULT NULL COMMENT '首次入库时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_serial (material_id, serial_no, tenant_id),
    KEY idx_serial_no (serial_no),
    -- 出库、调拨时选择某个库存位置上的在库序列号
    KEY idx_serial_slot (warehouse_id, location_id, material_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '序列号';

CREATE TABLE wms_stock_flow (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(16)    NOT NULL COMMENT '业务类型(字典 wms_biz_type) IN入库 OUT出库 TRANSFER调拨 CHECK盘点',
    doc_id       BIGINT         NOT NULL COMMENT '单据ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '变动数量，正数增加、负数减少',
    biz_price    DECIMAL(18, 6)          DEFAULT NULL COMMENT '业务单价（不含税，本位币）：采购入库、销售出库等来源单据的单价，没有为空',
    cost_price   DECIMAL(18, 6)          DEFAULT NULL COMMENT '成本单价（实时移动加权平均），供应商寄售仓的库存不计价为空',
    cost_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '成本金额，与数量同号',
    partner_id   BIGINT                  DEFAULT NULL COMMENT '往来单位（供应商 / 客户）',
    partner_name VARCHAR(128)            DEFAULT NULL COMMENT '往来单位名称快照',
    cost_estimated TINYINT               NOT NULL DEFAULT 0 COMMENT '1 升级前的历史流水：成本按当时的成本核算单价 / 参考成本价回填，是近似值',
    create_by    BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME                DEFAULT NULL COMMENT '发生时间',
    PRIMARY KEY (id),
    -- 批次追踪、物料台账
    KEY idx_flow_material (material_id, batch_no),
    KEY idx_flow_doc (biz_type, doc_id),
    KEY idx_flow_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存流水';

CREATE TABLE wms_material_cost (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '计价数量：全部仓库（不含供应商寄售仓）的库存合计',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '库存金额',
    unit_cost   DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '移动加权平均单价；数量为 0 时保留最后的单价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wms_material_cost (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料实时库存成本（移动加权平均，按物料不分仓库），随库存变动在同一事务中更新';

CREATE TABLE wms_serial_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT      NOT NULL COMMENT '物料',
    serial_no    VARCHAR(64) NOT NULL COMMENT '序列号',
    biz_type     VARCHAR(16) NOT NULL COMMENT '业务类型(字典 wms_biz_type)',
    doc_id       BIGINT      NOT NULL COMMENT '单据ID',
    doc_no       VARCHAR(32) NOT NULL COMMENT '单据号',
    direction    TINYINT     NOT NULL COMMENT '方向 1入 -1出',
    warehouse_id BIGINT      NOT NULL COMMENT '仓库',
    location_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    create_by    BIGINT               DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME             DEFAULT NULL COMMENT '发生时间',
    PRIMARY KEY (id),
    -- 序列号追踪：按物料 + 序列号查询完整轨迹
    KEY idx_serial_log (material_id, serial_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '序列号轨迹';

CREATE TABLE wms_stock_doc (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no          VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '单据种类 IN入库 OUT出库 TRANSFER调拨',
    biz_type        VARCHAR(32)             DEFAULT NULL COMMENT '入库类型(字典 wms_in_type) / 出库类型(字典 wms_out_type)，调拨为空',
    io_category     VARCHAR(32)             DEFAULT NULL COMMENT '收发类别：其他入库为字典 wms_in_category，其他出库、报废出库为字典 wms_out_category（业务字典），成本凭证按类别取对方科目；业务单据生成的为空',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库；调拨为调出仓库',
    to_warehouse_id BIGINT                  DEFAULT NULL COMMENT '调入仓库，仅调拨',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中 TRANSIT在途（在途调拨已调出未调入） DONE已完成',
    total_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    source_no       VARCHAR(32)             DEFAULT NULL COMMENT '来源单号：由采购入库、销售出库、工单领料等业务单据自动生成时填写',
    in_transit      TINYINT        NOT NULL DEFAULT 0 COMMENT '在途调拨 1是：确认时只调出，调入仓库收货后才入库',
    receive_by      BIGINT                  DEFAULT NULL COMMENT '调入收货人',
    receive_date    DATE                    DEFAULT NULL COMMENT '调入收货日期：在途调拨收货的业务日期，调入流水按此日期统计',
    receive_time    DATETIME                DEFAULT NULL COMMENT '调入收货时间',
    red_of          BIGINT                  DEFAULT NULL COMMENT '红字冲销单：被冲销的原单据',
    reversed        TINYINT        NOT NULL DEFAULT 0 COMMENT '已被冲销 1是',
    partner_id      BIGINT                  DEFAULT NULL COMMENT '往来单位（业务单据生成时由来源单据传入）',
    partner_name    VARCHAR(128)            DEFAULT NULL COMMENT '往来单位名称快照',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例（发布了出入库审批流程时，手工单据确认前审批）',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_doc_no (doc_no, tenant_id),
    -- 各单据列表：按种类查询，按 id 倒序
    KEY idx_stock_doc_kind (kind, id),
    KEY idx_stock_doc_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库单（入库、出库、调拨）';

CREATE TABLE wms_stock_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（入库、出库），为空取表头仓库；调拨不用',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位；调拨为调出库位，0 为未指定',
    to_location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '调入库位，仅调拨',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅入库新批次',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅入库新批次',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    cost_price      DECIMAL(18, 6)          DEFAULT NULL COMMENT '成本单价：其他入库、期初入库填写的入库成本，业务单据传入的成本；为空按当前移动加权平均价',
    biz_price       DECIMAL(18, 6)          DEFAULT NULL COMMENT '业务单价（不含税，本位币），业务单据生成时传入',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stock_doc_item_doc (doc_id),
    -- 删除物料前检查是否被单据引用
    KEY idx_stock_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库单明细';

CREATE TABLE wms_assembly (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_no     VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '种类(字典 wms_assembly_kind) ASSEMBLE组装 DISASSEMBLE拆卸',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    material_id     BIGINT         NOT NULL COMMENT '成套件（组装得到 / 拆卸的物料）',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '成套件库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '成套件批次号',
    production_date DATE                    DEFAULT NULL COMMENT '成套件生产日期（组装入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '成套件有效期至（组装入库新批次）',
    serial_nos      TEXT COMMENT '成套件序列号，逗号分隔',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '成套件数量',
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 DONE已完成',
    out_doc_id      BIGINT                  DEFAULT NULL COMMENT '生成的出库单（组装为子件出库，拆卸为成套件出库）',
    out_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '生成的出库单号',
    in_doc_id       BIGINT                  DEFAULT NULL COMMENT '生成的入库单（组装为成套件入库，拆卸为子件入库）',
    in_doc_no       VARCHAR(32)             DEFAULT NULL COMMENT '生成的入库单号',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assembly_no (assembly_no, tenant_id),
    KEY idx_assembly_date (biz_date),
    KEY idx_assembly_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单：组装消耗子件得到成套件，拆卸相反；确认后生成出库、入库单（类型 ASSEMBLY），成本核算按子件成本计算成套件成本';

CREATE TABLE wms_assembly_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_id     BIGINT         NOT NULL COMMENT '组装拆卸单',
    material_id     BIGINT         NOT NULL COMMENT '子件',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期（拆卸入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至（拆卸入库新批次）',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_assembly_item_assembly (assembly_id),
    KEY idx_assembly_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单的子件';

CREATE TABLE wms_check (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)  NOT NULL COMMENT '单据号',
    warehouse_id BIGINT       NOT NULL COMMENT '盘点仓库',
    biz_date     DATE         NOT NULL COMMENT '盘点日期',
    status       VARCHAR(16)  NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中（盘点差异审批） DONE已完成',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    freeze       TINYINT      NOT NULL DEFAULT 0 COMMENT '冻结仓库 1是：盘点完成前该仓库不能出入库、调拨',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '差异审批流程实例',
    dept_id      BIGINT                DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                DEFAULT NULL COMMENT '制单人',
    confirm_by   BIGINT                DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME              DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_check_doc_no (doc_no, tenant_id),
    KEY idx_check_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '盘点单';

CREATE TABLE wms_check_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_id    BIGINT         NOT NULL COMMENT '盘点单ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    location_id BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no    VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    book_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '账面数量：草稿为保存时的库存，完成后为确认时的库存',
    actual_qty  DECIMAL(18, 4)          DEFAULT NULL COMMENT '实盘数量，草稿中可为空',
    serial_nos  TEXT COMMENT '实盘序列号，逗号分隔（序列号物料）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    cost_price  DECIMAL(18, 6)          DEFAULT NULL COMMENT '盘盈的成本单价，为空按确认时的当前均价；盘亏按当前均价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_check_item_check (check_id),
    KEY idx_check_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '盘点单明细';

CREATE TABLE wms_doc_sequence (
    seq_key   VARCHAR(32) NOT NULL COMMENT '单据号前缀 + 日期，如 RK20261014',
    seq_value BIGINT      NOT NULL COMMENT '当天已使用的最大流水号',
    tenant_id BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (seq_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '单据号流水';

-- ---------------------------------------------------------------- 供应链管理

CREATE TABLE scm_supplier (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    name         VARCHAR(128) NOT NULL COMMENT '供应商名称',
    level        VARCHAR(16)           DEFAULT NULL COMMENT '等级(字典 scm_supplier_level) A/B/C/D',
    contact      VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    phone        VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    email        VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    address      VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    tax_no       VARCHAR(32)           DEFAULT NULL COMMENT '纳税人识别号',
    bank_name    VARCHAR(128)          DEFAULT NULL COMMENT '开户行',
    bank_account VARCHAR(64)           DEFAULT NULL COMMENT '银行账号',
    payment_days INT          NOT NULL DEFAULT 0 COMMENT '账期（天），应付单到期日 = 单据日期 + 账期',
    logistics    TINYINT      NOT NULL DEFAULT 0 COMMENT '物流公司（承运商） 1是 0否：采购运费、发运单的承运商只列出物流公司',
    routes       VARCHAR(1000)         DEFAULT NULL COMMENT '营运路线（物流公司），可写途经站点，如 青岛北—青岛西—诸城—济南',
    customer_id  BIGINT                DEFAULT NULL COMMENT '关联客户（同一单位既是供应商又是客户，一对一）：查询应收时可挂接该供应商的应付，可做应收应付对冲',
    merged_to    BIGINT                DEFAULT NULL COMMENT '已合并到的供应商（资料合并后本资料停用，业务数据已改为指向该供应商）',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    search_key   VARCHAR(1000)          DEFAULT NULL COMMENT '检索列：编码、名称、联系人、电话去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_code (code, tenant_id),
    UNIQUE KEY uk_supplier_customer (customer_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商';

CREATE TABLE scm_supplier_price (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supplier_id BIGINT         NOT NULL COMMENT '供应商',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate    DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    lead_days   INT            NOT NULL DEFAULT 0 COMMENT '采购提前期（天），MRP 按此倒推下单日期',
    min_qty     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最小起订量',
    is_default  TINYINT        NOT NULL DEFAULT 0 COMMENT '是否该物料的默认供应商 1是 0否，MRP 按默认供应商生成采购订单',
    valid_from  DATE                    DEFAULT NULL COMMENT '有效期起，为空不限',
    valid_to    DATE                    DEFAULT NULL COMMENT '有效期止，过期后 MRP、请购不再取该价格',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_price (supplier_id, material_id, tenant_id),
    KEY idx_supplier_price_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供货价格';

CREATE TABLE scm_supplier_price_tier (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    price_id    BIGINT         NOT NULL COMMENT '供货价格',
    min_qty     DECIMAL(18, 4) NOT NULL COMMENT '起订数量（达到该数量适用本档单价）',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_tier_price (price_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供货价格阶梯价';

CREATE TABLE scm_price_list (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    customer_id    BIGINT                  DEFAULT NULL COMMENT '客户专属价：指定客户时只适用该客户（优先于客户等级）',
    customer_level VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)，为空适用全部客户',
    min_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '起订数量',
    price          DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate       DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    valid_from     DATE                    DEFAULT NULL COMMENT '有效期起',
    valid_to       DATE                    DEFAULT NULL COMMENT '有效期止',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_list_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售价目表：取价时优先匹配客户专属价，其次客户等级，再取满足起订数量的最大一档';

CREATE TABLE scm_price_control (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id        BIGINT         NOT NULL COMMENT '物料',
    min_sale_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低销售价（含税，基本单位，本位币），为空不控制',
    max_purchase_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '最高采购价（含税，基本单位，本位币），为空不控制',
    control_mode       VARCHAR(8)     NOT NULL DEFAULT 'WARN' COMMENT '控制方式(字典 scm_price_control_mode) WARN提示 BLOCK禁止保存',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_control (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格控制：销售订单单价不低于最低销售价，采购订单单价不高于最高采购价（手工录入的订单）';

CREATE TABLE scm_customer_price (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    customer_id BIGINT         NOT NULL COMMENT '客户',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '执行价（含税，基本单位，本位币）',
    tax_rate    DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%），为空时开单不改税率',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_price (customer_id, material_id, tenant_id),
    KEY idx_customer_price_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户执行价：开单默认价最优先';

CREATE TABLE scm_price_memory (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind        VARCHAR(16)    NOT NULL COMMENT '种类 SALES销售（销售出库） PURCHASE采购（采购入库）',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    material_id BIGINT         NOT NULL COMMENT '物料',
    last_date   DATE           NOT NULL COMMENT '最新日期',
    last_price  DECIMAL(18, 4) NOT NULL COMMENT '最新价格（含税，基本单位，本位币）',
    min_date    DATE           NOT NULL COMMENT '最低价日期',
    min_price   DECIMAL(18, 4) NOT NULL COMMENT '最低价格',
    max_date    DATE           NOT NULL COMMENT '最高价日期',
    max_price   DECIMAL(18, 4) NOT NULL COMMENT '最高价格',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_memory (kind, partner_id, material_id, tenant_id),
    KEY idx_price_memory_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格记忆：销售出库、采购入库确认后登记往来单位 × 物料的最新 / 最低 / 最高价';

CREATE TABLE scm_supplier_eval (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supplier_id    BIGINT        NOT NULL COMMENT '供应商',
    period         VARCHAR(16)   NOT NULL COMMENT '考核期间，如 2026-Q3、2026-09',
    quality_score  DECIMAL(5, 1) NOT NULL COMMENT '质量得分（0-100）',
    delivery_score DECIMAL(5, 1) NOT NULL COMMENT '交期得分',
    price_score    DECIMAL(5, 1) NOT NULL COMMENT '价格得分',
    service_score  DECIMAL(5, 1) NOT NULL COMMENT '服务得分',
    total_score    DECIMAL(5, 1) NOT NULL COMMENT '综合得分 = 质量40% + 交期30% + 价格20% + 服务10%',
    grade          VARCHAR(16)   NOT NULL COMMENT '评定等级(字典 scm_supplier_level)，按综合得分',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '评语',
    create_by      BIGINT                 DEFAULT NULL COMMENT '评估人',
    create_time    DATETIME               DEFAULT NULL COMMENT '评估时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_supplier_eval_supplier (supplier_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商评估';

CREATE TABLE scm_customer (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)    NOT NULL COMMENT '客户编码',
    name         VARCHAR(128)   NOT NULL COMMENT '客户名称',
    level        VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)',
    price_level  INT            NOT NULL DEFAULT 0 COMMENT '价格等级：开单默认价取物料的价格列 0零售价 1-8批发价1-8',
    contact      VARCHAR(64)             DEFAULT NULL COMMENT '联系人',
    phone        VARCHAR(32)             DEFAULT NULL COMMENT '联系电话',
    address      VARCHAR(255)            DEFAULT NULL COMMENT '收货地址',
    tax_no       VARCHAR(32)             DEFAULT NULL COMMENT '纳税人识别号',
    credit_limit DECIMAL(18, 2)          DEFAULT NULL COMMENT '信用额度，审核销售订单时校验未完成订单金额',
    payment_days INT            NOT NULL DEFAULT 0 COMMENT '账期（天），应收单到期日 = 单据日期 + 账期',
    channel      VARCHAR(16)    NOT NULL DEFAULT 'DIRECT' COMMENT '渠道(字典 scm_channel) DIRECT直销 DEALER经销商',
    region       VARCHAR(128)            DEFAULT NULL COMMENT '区域(业务字典 scm_region)，经销商为授权区域，进销存报表按区域汇总',
    owner_id     BIGINT                  DEFAULT NULL COMMENT '负责销售：经销商门户订单以其为制单人（按其数据权限可见）',
    merged_to    BIGINT                  DEFAULT NULL COMMENT '已合并到的客户（资料合并后本资料停用，业务数据已改为指向该客户）',
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    search_key   VARCHAR(1000)          DEFAULT NULL COMMENT '检索列：编码、名称、联系人、电话去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户';

CREATE TABLE scm_order (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_no         VARCHAR(32)    NOT NULL COMMENT '订单号',
    kind             VARCHAR(16)    NOT NULL COMMENT '订单种类 PURCHASE采购 SALES销售',
    partner_id       BIGINT         NOT NULL COMMENT '供应商 / 客户',
    order_date       DATE           NOT NULL COMMENT '订单日期',
    delivery_date    DATE                    DEFAULT NULL COMMENT '交货日期',
    warehouse_id     BIGINT                  DEFAULT NULL COMMENT '默认收货 / 发货仓库',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_order_status)',
    total_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    source           VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 scm_order_source) MANUAL手工 MRP物料需求计划 CRM商机 KIT生产缺料 DIRECT直接开单（隐式订单）',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    contract_id      BIGINT                  DEFAULT NULL COMMENT '引用的采购合同（按合同价下单）',
    supplier_confirm VARCHAR(16)             DEFAULT NULL COMMENT '供应商确认(字典 scm_supplier_confirm) ACCEPTED已接受 REJECTED已拒绝，为空未确认',
    promised_date    DATE                    DEFAULT NULL COMMENT '供应商承诺交期',
    confirm_remark   VARCHAR(255)            DEFAULT NULL COMMENT '供应商确认说明',
    confirm_time     DATETIME                DEFAULT NULL COMMENT '供应商确认时间',
    discount_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '促销优惠合计（原价 - 成交价），已从金额中扣除',
    rebate_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '使用返利抵扣的金额（按行金额分摊到单价），已从金额中扣除',
    whole_discount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '整单优惠（含税，含抹零）：促销、返利之后按行金额分摊到各行金额，已从金额中扣除',
    invoice_kind     VARCHAR(16)             DEFAULT NULL COMMENT '发票类型(业务字典 scm_invoice_kind)：明细未填写税率时取它的税率；出入库单显示所属订单的发票类型',
    close_adjust     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '关闭时的尾差调整（订单币种）：部分出入库的行按 行金额 × 已出入库数量 / 订单数量 调整已出入库金额，生成应收 / 应付调整单',
    external_no      VARCHAR(64)             DEFAULT NULL COMMENT '外部单号（电商 / 门店订单导入），同一租户不重复',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    version          INT            NOT NULL DEFAULT 1 COMMENT '版本：每次变更单生效后加 1',
    currency         VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种：订单单价、金额按此币种',
    exchange_rate    DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（1 外币 = 汇率 × 本位币），出入库、应收应付按此折算本位币',
    salesman_id      BIGINT                  DEFAULT NULL COMMENT '业务员（销售订单，计算销售提成）：默认客户的负责销售，没有时为制单人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    approve_by       BIGINT                  DEFAULT NULL COMMENT '审核人',
    approve_time     DATETIME                DEFAULT NULL COMMENT '审核时间',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no, tenant_id),
    KEY idx_order_kind (kind, id),
    KEY idx_order_partner (partner_id),
    KEY idx_order_dept (dept_id),
    KEY idx_order_contract (contract_id),
    UNIQUE KEY uk_order_external (external_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购 / 销售订单';

CREATE TABLE scm_order_change (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    change_no        VARCHAR(32)  NOT NULL COMMENT '变更单号',
    order_id         BIGINT       NOT NULL COMMENT '订单',
    version          INT          NOT NULL COMMENT '生效后的订单版本',
    reason           VARCHAR(255) NOT NULL COMMENT '变更原因',
    status           VARCHAR(16)  NOT NULL DEFAULT 'APPROVING' COMMENT '状态(字典 scm_change_status) APPROVING审批中 APPROVED已生效 REJECTED已驳回',
    old_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '变更前价税合计',
    new_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '变更后价税合计',
    old_whole_discount DECIMAL(18, 2)       DEFAULT NULL COMMENT '变更前的整单优惠',
    new_whole_discount DECIMAL(18, 2)       DEFAULT NULL COMMENT '变更后的整单优惠（按变更后未出入库部分重新分摊）',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例',
    approve_time     DATETIME              DEFAULT NULL COMMENT '生效时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '申请人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_change_no (change_no, tenant_id),
    KEY idx_order_change_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单变更单：已审核订单的数量、单价、交期变更，审批通过后生效并升版本';

CREATE TABLE scm_order_change_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    change_id         BIGINT         NOT NULL COMMENT '变更单',
    action            VARCHAR(8)     NOT NULL COMMENT '操作 ADD新增 MODIFY修改 REMOVE删除',
    order_item_id     BIGINT                  DEFAULT NULL COMMENT '订单明细（新增行为空，生效后回填）',
    material_id       BIGINT         NOT NULL COMMENT '物料',
    old_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '原数量',
    new_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '新数量',
    old_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '原单价',
    new_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '新单价',
    tax_rate          DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（新增行）',
    old_delivery_date DATE                    DEFAULT NULL COMMENT '原交货日期',
    new_delivery_date DATE                    DEFAULT NULL COMMENT '新交货日期',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_change_item_change (change_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单变更明细';

CREATE TABLE scm_order_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id      BIGINT         NOT NULL COMMENT '订单ID',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '订购数量',
    unit_name     VARCHAR(16)             DEFAULT NULL COMMENT '录入单位（辅助单位），为空表示基本单位；数量、单价均按基本单位保存',
    unit_ratio    DECIMAL(18, 6)          DEFAULT NULL COMMENT '录入单位的换算率',
    price         DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate      DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '税率（%）',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount    DECIMAL(18, 2) NOT NULL COMMENT '税额',
    done_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已入库 / 已出库数量',
    return_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已退货数量',
    replenish_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '退货后需要补发（补收）的数量，计入未完成数量',
    done_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已出入库金额（订单币种，补发的退货扣减）：最后一批取 行金额 − 已出入库金额',
    return_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已退款的退货金额（订单币种，不补发的退货）',
    discount_share DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '分摊到本行的整单优惠（行金额已扣除）',
    done_share    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已出入库部分已分摊的整单优惠（订单变更时不变，剩余的重新分摊）',
    close_adjust  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '订单关闭时本行的尾差调整（订单币种，已计入已出入库金额）',
    close_adjust_net DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '尾差调整的不含税金额（本位币）：采购订单计入调整单所在期间的月末成本核算，凭证按物料分类取数',
    orig_price    DECIMAL(18, 4)          DEFAULT NULL COMMENT '原价（促销、返利抵扣前的含税单价），没有优惠时为空',
    promotion_id  BIGINT                  DEFAULT NULL COMMENT '应用的折扣促销 / 赠品来源的买赠促销',
    gift          TINYINT        NOT NULL DEFAULT 0 COMMENT '1 赠品（单价为 0，由买赠促销自动生成）',
    delivery_date DATE                    DEFAULT NULL COMMENT '交货日期',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

-- 促销：按期间、客户等级、物料 / 分类设置折扣、满减、买赠，手工新建 / 修改销售订单时自动应用（先折扣，再满减）
CREATE TABLE scm_promotion (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '促销名称',
    promo_type        VARCHAR(16)    NOT NULL COMMENT '类型(字典 scm_promo_type) DISCOUNT折扣 REDUCE满减 GIFT买赠',
    begin_date        DATE           NOT NULL COMMENT '开始日期（按订单日期匹配）',
    end_date          DATE           NOT NULL COMMENT '结束日期',
    customer_level    VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)，为空适用全部客户',
    material_id       BIGINT                  DEFAULT NULL COMMENT '适用物料，为空按分类',
    material_category VARCHAR(16)             DEFAULT NULL COMMENT '适用物料分类编码(wms_material_category.code，含下级分类)，物料和分类都为空适用全部物料',
    min_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '折扣：明细数量达到时才打折',
    discount_rate     DECIMAL(5, 2)           DEFAULT NULL COMMENT '折扣：成交价 = 原价 × 折扣率%，如 90 为九折',
    threshold_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减：适用物料的金额（折扣后）达到此金额',
    reduce_amount     DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减：减免金额，按行金额分摊到单价',
    buy_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '买赠：适用物料每购买此数量',
    gift_material_id  BIGINT                  DEFAULT NULL COMMENT '买赠：赠品物料',
    gift_qty          DECIMAL(18, 4)          DEFAULT NULL COMMENT '买赠：每次赠送数量',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_by         BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_promotion_date (end_date, begin_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销活动';

CREATE TABLE scm_promotion_usage (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    promotion_id    BIGINT         NOT NULL COMMENT '促销',
    order_id        BIGINT         NOT NULL COMMENT '销售订单',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    gift_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '赠品数量',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_promotion_usage (promotion_id),
    KEY idx_promotion_usage_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销应用记录（订单保存时重算）';

CREATE TABLE scm_trade_doc (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 scm_trade_kind) PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 SALES_OUT销售出库 SALES_RETURN销售退货',
    order_id     BIGINT         NOT NULL COMMENT '订单ID',
    partner_id   BIGINT         NOT NULL COMMENT '供应商 / 客户',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    currency     VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '订单币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（取订单汇率）；单据金额为本位币',
    amount_fc    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计（原币）',
    wms_doc_id   BIGINT                  DEFAULT NULL COMMENT '生成的出入库单ID',
    wms_doc_no   VARCHAR(32)             DEFAULT NULL COMMENT '生成的出入库单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    replenish    TINYINT        NOT NULL DEFAULT 0 COMMENT '退货是否补发（补收） 1补发 0退款',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本单分摊的整单优惠（本位币，含税）：优惠前合计 = 价税合计 + 本项',
    discount_adjust DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '采购入库修改的订单优惠与按订单分摊之差（本位币，含税），0为与订单一致',
    freight      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费，0为没有',
    freight_bearer VARCHAR(16)           DEFAULT NULL COMMENT '运费承担(字典 scm_freight_bearer)：采购入库 SUPPLIER供应商代垫 COMPANY本公司承担；销售出库 COMPANY本公司承担 CUSTOMER代垫运费（客户承担）；销售退货 CUSTOMER客户代垫；采购退货 SUPPLIER代垫运费（供应商承担）',
    freight_cost VARCHAR(16)             DEFAULT NULL COMMENT '采购运费入账方式 INVENTORY计入存货成本 EXPENSE计入当期费用（本公司承担时按业务参数）',
    carrier_supplier_id BIGINT           DEFAULT NULL COMMENT '运费的承运商（供应商）：采购入库本公司承担、月结时必填',
    freight_bill_id BIGINT               DEFAULT NULL COMMENT '运费应付单（采购入库运费应付、月结的承运商应付）',
    freight_settle VARCHAR(16)           DEFAULT NULL COMMENT '付给承运商的方式(字典 scm_freight_settle，销售出库、采购退货) CASH现付 MONTHLY月结',
    freight_account_id BIGINT            DEFAULT NULL COMMENT '现付运费的付款账户（现金 / 银行末级科目）',
    freight_partner_bill_id BIGINT       DEFAULT NULL COMMENT '往来单位的代垫运费单据：销售出库代垫运费应收、销售退货红字应收、采购退货红字应付',
    freight_io_id BIGINT                 DEFAULT NULL COMMENT '现付运费生成的费用支出单',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    post_by      BIGINT                  DEFAULT NULL COMMENT '审核人（保存即审核时为制单人，草稿审核时为审核人）',
    post_time    DATETIME                DEFAULT NULL COMMENT '审核时间（出入库、生成应收应付的时间）',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_doc_no (doc_no, tenant_id),
    KEY idx_trade_doc_kind (kind, id),
    KEY idx_trade_doc_order (order_id),
    KEY idx_trade_doc_date (biz_date),
    KEY idx_trade_doc_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 采购退货 / 销售出库 / 销售退货';

CREATE TABLE scm_trade_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    order_item_id   BIGINT         NOT NULL COMMENT '订单明细ID（直接开单为隐式订单的明细）',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（仅销售出库可选），为空取表头仓库',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅采购入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅采购入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price           DECIMAL(18, 4) NOT NULL COMMENT '含税单价（取订单，已扣除整单优惠的分摊）',
    tax_rate        DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '税率（%）',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount      DECIMAL(18, 2) NOT NULL COMMENT '税额',
    gross_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '优惠前含税单价（整单优惠分摊前，本位币），为空同单价',
    discount_share  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本行分摊的整单优惠（本位币，含税）',
    freight_share   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本行分摊的采购运费（本位币，计入存货成本）',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trade_doc_item_doc (doc_id),
    KEY idx_trade_doc_item_order_item (order_item_id),
    KEY idx_trade_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货明细';

CREATE TABLE scm_trade_draft (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号（审核后沿用为出入库单号）',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 scm_trade_kind) PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 SALES_OUT销售出库 SALES_RETURN销售退货',
    order_id     BIGINT                  DEFAULT NULL COMMENT '引用的订单，直接开单为空',
    source_doc_id BIGINT                 DEFAULT NULL COMMENT '退货引用的原出入库单（仅用于表单回显）',
    partner_id   BIGINT         NOT NULL COMMENT '供应商 / 客户',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    salesman_id  BIGINT                  DEFAULT NULL COMMENT '直接开单的销售出库：业务员',
    replenish    TINYINT        NOT NULL DEFAULT 0 COMMENT '退货是否补发（补收） 1补发 0退款',
    whole_discount DECIMAL(18, 2)        DEFAULT NULL COMMENT '直接开单的整单优惠（含税）',
    invoice_kind   VARCHAR(16)           DEFAULT NULL COMMENT '直接开单的发票类型(业务字典 scm_invoice_kind)，审核时记在隐式订单上；引用订单时为空（取订单的）',
    order_discount DECIMAL(18, 2)        DEFAULT NULL COMMENT '引用订单的采购入库：本单的订单优惠（含税，订单币种），为空按订单分摊',
    freight      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费',
    freight_bearer VARCHAR(16)           DEFAULT NULL COMMENT '运费承担(字典 scm_freight_bearer)，含义同出入库单',
    carrier_supplier_id BIGINT           DEFAULT NULL COMMENT '运费的承运商（供应商）',
    freight_settle VARCHAR(16)           DEFAULT NULL COMMENT '付给承运商的方式(字典 scm_freight_settle) CASH现付 MONTHLY月结',
    freight_account_id BIGINT            DEFAULT NULL COMMENT '现付运费的付款账户',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计（按录入单价估算，审核后以出入库单为准）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_draft_no (doc_no, tenant_id),
    KEY idx_trade_draft_kind (kind, id),
    KEY idx_trade_draft_order (order_id),
    KEY idx_trade_draft_partner (partner_id),
    KEY idx_trade_draft_warehouse (warehouse_id),
    KEY idx_trade_draft_dept (dept_id),
    KEY idx_trade_draft_create_by (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货的草稿（不影响库存、往来和凭证，审核后生成出入库单并删除）';

CREATE TABLE scm_trade_draft_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    draft_id        BIGINT         NOT NULL COMMENT '草稿ID',
    order_item_id   BIGINT                  DEFAULT NULL COMMENT '订单明细ID，直接开单为空',
    material_id     BIGINT                  DEFAULT NULL COMMENT '物料（引用订单时为订单明细的物料）',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（仅销售出库可选），为空取表头仓库',
    location_id     BIGINT                  DEFAULT NULL COMMENT '库位',
    batch_no        VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅采购入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅采购入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price           DECIMAL(18, 4)          DEFAULT NULL COMMENT '直接开单：含税单价',
    tax_rate        DECIMAL(5, 2)           DEFAULT NULL COMMENT '直接开单：税率（%）',
    gift            TINYINT        NOT NULL DEFAULT 0 COMMENT '直接开单的销售出库：1 买赠的赠品行',
    unit_name       VARCHAR(16)             DEFAULT NULL COMMENT '直接开单：录入时使用的单位（仅显示）',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    sort            INT            NOT NULL DEFAULT 0 COMMENT '行号',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trade_draft_item_draft (draft_id),
    KEY idx_trade_draft_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货草稿的明细';

CREATE TABLE scm_shipment (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ship_no      VARCHAR(32)    NOT NULL COMMENT '发运单号',
    trade_doc_id BIGINT         NOT NULL COMMENT '销售出库单ID',
    customer_id  BIGINT         NOT NULL COMMENT '客户',
    carrier      VARCHAR(32)    NOT NULL COMMENT '承运商(字典 scm_carrier)',
    tracking_no  VARCHAR(64)             DEFAULT NULL COMMENT '运单号',
    freight      DECIMAL(18, 2)          DEFAULT NULL COMMENT '运费',
    freight_bearer VARCHAR(16)  NOT NULL DEFAULT 'COMPANY' COMMENT '运费承担(字典 scm_freight_bearer) COMPANY本公司承担（销售费用） CUSTOMER客户承担（代垫，计入应收）',
    carrier_supplier_id BIGINT              DEFAULT NULL COMMENT '承运商对应的供应商（运费应付）',
    freight_bill_id     BIGINT              DEFAULT NULL COMMENT '运费应付单',
    freight_ar_bill_id  BIGINT              DEFAULT NULL COMMENT '代垫运费应收单（客户承担时）',
    receiver     VARCHAR(64)             DEFAULT NULL COMMENT '收货人',
    phone        VARCHAR(32)             DEFAULT NULL COMMENT '收货电话',
    address      VARCHAR(255)            DEFAULT NULL COMMENT '收货地址',
    ship_date    DATE                    DEFAULT NULL COMMENT '发货日期',
    sign_date    DATE                    DEFAULT NULL COMMENT '签收日期',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_shipment_status) PENDING待发运 SHIPPED运输中 SIGNED已签收 EXCEPTION异常',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ship_no (ship_no, tenant_id),
    KEY idx_shipment_trade_doc (trade_doc_id),
    KEY idx_shipment_tracking (tracking_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物流发运单';

-- ---------------------------------------------------------------- 财务管理

CREATE TABLE fin_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '科目编码：一级 4 位，下级在上级编码后加 2 位',
    name        VARCHAR(64)  NOT NULL COMMENT '科目名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级科目，0 为一级科目',
    category    VARCHAR(16)  NOT NULL COMMENT '类别(字典 fin_account_category) ASSET资产 LIABILITY负债 EQUITY权益 COST成本 PROFIT损益',
    direction   VARCHAR(8)   NOT NULL COMMENT '余额方向 DEBIT借 CREDIT贷',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    aux_items   VARCHAR(64)           DEFAULT NULL COMMENT '辅助核算项目（逗号分隔，可多选） DEPT部门 CUSTOMER客户 SUPPLIER供应商 PROJECT项目 MATERIAL存货；手工凭证使用该科目时必填',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_code (code, tenant_id),
    KEY idx_account_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会计科目';

CREATE TABLE fin_currency (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(8)  NOT NULL COMMENT '币种代码，如 USD',
    name        VARCHAR(32) NOT NULL COMMENT '名称',
    symbol      VARCHAR(8)           DEFAULT NULL COMMENT '符号',
    is_base     TINYINT     NOT NULL DEFAULT 0 COMMENT '本位币 1是（只有一个，CNY）',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_currency_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '币种';

CREATE TABLE fin_exchange_rate (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    currency    VARCHAR(8)     NOT NULL COMMENT '币种',
    period      INT            NOT NULL COMMENT '期间 yyyyMM：当月业务的记账汇率，期末调汇也取该月汇率',
    rate        DECIMAL(18, 6) NOT NULL COMMENT '汇率：1 外币 = rate 本位币',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exchange_rate (currency, period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汇率（按月）';

CREATE TABLE fin_account_opening (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id  BIGINT         NOT NULL COMMENT '末级科目',
    debit       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初借方余额',
    credit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初贷方余额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_opening (account_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '科目期初余额';

CREATE TABLE fin_period (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period       INT         NOT NULL COMMENT '会计期间 yyyyMM，如 202610',
    status       VARCHAR(16) NOT NULL COMMENT '状态 OPEN未结账 CLOSED已结账',
    voucher_seq  INT         NOT NULL DEFAULT 0 COMMENT '本期已使用的最大凭证号',
    close_by     BIGINT               DEFAULT NULL COMMENT '结账人',
    close_time   DATETIME             DEFAULT NULL COMMENT '结账时间',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会计期间';

CREATE TABLE fin_voucher (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period           INT            NOT NULL COMMENT '会计期间 yyyyMM',
    voucher_seq      INT            NOT NULL COMMENT '期间内凭证号',
    voucher_no       VARCHAR(16)    NOT NULL COMMENT '凭证字号，如 记-0001',
    voucher_date     DATE           NOT NULL COMMENT '凭证日期',
    summary          VARCHAR(255)            DEFAULT NULL COMMENT '摘要（第一条分录）',
    total_debit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方合计',
    total_credit     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方合计',
    attachment_count INT            NOT NULL DEFAULT 0 COMMENT '附单据数',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_voucher_status) DRAFT未审核 APPROVING审批中 AUDITED已审核 POSTED已记账',
    source_type      VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 fin_voucher_source) MANUAL手工 AR应收 AP应付 RECEIPT收款 PAYMENT付款 DEPRECIATION折旧 ASSET资产处置 COST成本结转 CLOSE结转损益',
    source_no        VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    flow_instance_id VARCHAR(64)            DEFAULT NULL COMMENT '审批流程实例ID',
    reversal_of      BIGINT                  DEFAULT NULL COMMENT '红字冲销凭证：被冲销的原凭证',
    reversed         TINYINT        NOT NULL DEFAULT 0 COMMENT '已被红字冲销 1是',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    audit_by         BIGINT                  DEFAULT NULL COMMENT '审核人',
    audit_time       DATETIME                DEFAULT NULL COMMENT '审核时间',
    post_by          BIGINT                  DEFAULT NULL COMMENT '记账人',
    post_time        DATETIME                DEFAULT NULL COMMENT '记账时间',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_voucher_seq (period, voucher_seq, tenant_id),
    KEY idx_voucher_period_status (period, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '记账凭证';

CREATE TABLE fin_voucher_entry (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    voucher_id  BIGINT         NOT NULL COMMENT '凭证ID',
    seq         INT            NOT NULL COMMENT '分录行号',
    summary     VARCHAR(255)   NOT NULL COMMENT '摘要',
    account_id  BIGINT         NOT NULL COMMENT '科目（末级）',
    dept_id     BIGINT                  DEFAULT NULL COMMENT '部门（辅助核算，预算执行按部门统计）',
    project_id  BIGINT                  DEFAULT NULL COMMENT '项目（辅助核算）',
    customer_id BIGINT                  DEFAULT NULL COMMENT '客户（辅助核算）',
    supplier_id BIGINT                  DEFAULT NULL COMMENT '供应商（辅助核算）',
    material_id BIGINT                  DEFAULT NULL COMMENT '存货（辅助核算）',
    debit       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方金额',
    credit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_entry_voucher (voucher_id),
    KEY idx_entry_account (account_id),
    KEY idx_entry_project (project_id),
    KEY idx_entry_customer (customer_id),
    KEY idx_entry_supplier (supplier_id),
    KEY idx_entry_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证分录';

CREATE TABLE fin_bill (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    bill_no        VARCHAR(32)    NOT NULL COMMENT '单号',
    kind           VARCHAR(8)     NOT NULL COMMENT '种类 AR应收 AP应付',
    partner_id     BIGINT         NOT NULL COMMENT '客户 / 供应商',
    source_type    VARCHAR(32)    NOT NULL COMMENT '来源(字典 fin_bill_source) SALES_OUT销售出库 SALES_RETURN销售退货 PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 MANUAL期初 / 手工 等',
    source_id      BIGINT                  DEFAULT NULL COMMENT '来源单据ID',
    source_no      VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    project_id     BIGINT                  DEFAULT NULL COMMENT '项目（辅助核算），项目开票时填写',
    expense_account_id BIGINT                DEFAULT NULL COMMENT '费用类应付单（运费等）的借方费用科目',
    dept_id        BIGINT                  DEFAULT NULL COMMENT '部门（费用的辅助核算）',
    bill_date      DATE           NOT NULL COMMENT '单据日期',
    due_date       DATE           NOT NULL COMMENT '到期日 = 单据日期 + 账期',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '价税合计，退货为负数（红字）',
    tax_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    settled_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已核销金额（与金额同号）',
    invoiced_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已开票 / 已收票金额（与金额同号）',
    bad_debt_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '坏账核销金额（计入已核销）',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '收付款时的折让 / 抹零金额（计入已核销）',
    currency        VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种；金额、已核销等为本位币（按记账汇率折算）',
    exchange_rate   DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '记账汇率',
    amount_fc       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    settled_fc      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币已核销',
    status         VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_bill_status) UNSETTLED未核销 PARTIAL部分核销 SETTLED已核销',
    voucher_id     BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no, tenant_id),
    KEY idx_bill_partner (kind, partner_id, status),
    KEY idx_bill_source (source_type, source_id),
    KEY idx_bill_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收 / 应付单';

CREATE TABLE fin_settlement (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settle_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind        VARCHAR(16)    NOT NULL COMMENT '种类 RECEIPT收款 PAYMENT付款',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    settle_date DATE           NOT NULL COMMENT '收付款日期',
    account_id  BIGINT         NOT NULL COMMENT '结算科目（库存现金 / 银行存款的末级科目）',
    method      VARCHAR(16)             DEFAULT NULL COMMENT '结算方式(字典 fin_settle_method)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额 = 各核销金额合计',
    currency    VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '收付款汇率；金额 = 原币 × 汇率（实收实付本位币）',
    amount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让 / 抹零合计（不含在金额中，单据的已核销 = 金额 + 折让）',
    discount_fc     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让合计（原币）',
    advance_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '多收 / 多付转预收预付的金额（不含在金额中）',
    advance_id      BIGINT                  DEFAULT NULL COMMENT '多收 / 多付生成的预收预付单',
    advance_offset  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '冲抵预收 / 预付合计（原币，不含在金额中）：实收实付 = 核销合计 - 冲抵',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    status       VARCHAR(16)    NOT NULL DEFAULT 'APPROVED' COMMENT '状态 DRAFT草稿 APPROVED已生效 APPROVING审批中 REJECTED已拒绝（付款审批）',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '审批流程实例ID',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_settle_no (settle_no, tenant_id),
    KEY idx_settlement_partner (kind, partner_id),
    KEY idx_settlement_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收款 / 付款单';

CREATE TABLE fin_settlement_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id BIGINT         NOT NULL COMMENT '收付款单ID',
    bill_id       BIGINT         NOT NULL COMMENT '核销的应收 / 应付单',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '核销金额（红字单据为负数）',
    amount_fc     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '核销原币金额（本位币单据与金额相同）',
    discount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让 / 抹零金额（与单据同号），单据的已核销 = 核销金额 + 折让',
    discount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '折让原币金额（外币按单据记账汇率折算为折让）',
    advance_offset DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其中由预收 / 预付冲抵的金额（原币），核销金额只含实收实付的部分',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_settlement_item_settlement (settlement_id),
    KEY idx_settlement_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收付款核销明细';

CREATE TABLE fin_settlement_advance (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id BIGINT         NOT NULL COMMENT '收付款单',
    advance_id    BIGINT         NOT NULL COMMENT '冲抵的预收 / 预付款',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '冲抵金额（原币）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_settlement_advance_settlement (settlement_id),
    KEY idx_settlement_advance_advance (advance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收付款冲抵的预收预付（草稿、审批中时登记，生效时冲抵）';

CREATE TABLE fin_offset (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    offset_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind        VARCHAR(8)     NOT NULL COMMENT '方向 AR应收冲账（客户） AP应付冲账（供应商）',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    offset_date DATE           NOT NULL COMMENT '冲账日期',
    handler_id  BIGINT                  DEFAULT NULL COMMENT '经办人',
    item        VARCHAR(32)    NOT NULL COMMENT '冲账项目(业务字典 fin_offset_item)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '冲账金额（正数），复核后生成金额为其负数的红字应收 / 应付单',
    begin_date  DATE                    DEFAULT NULL COMMENT '冲账项目期间：开始日期',
    end_date    DATE                    DEFAULT NULL COMMENT '冲账项目期间：结束日期',
    ticket_no   VARCHAR(64)             DEFAULT NULL COMMENT '票据编号',
    account_id  BIGINT                  DEFAULT NULL COMMENT '凭证对方科目，为空取凭证科目设置（应收冲账 / 应付冲账）',
    status      VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_offset_status) DRAFT待复核 REVIEWED已复核',
    bill_id     BIGINT                  DEFAULT NULL COMMENT '复核生成的红字应收 / 应付单',
    review_by   BIGINT                  DEFAULT NULL COMMENT '复核人',
    review_time DATETIME                DEFAULT NULL COMMENT '复核时间',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_offset_no (offset_no, tenant_id),
    KEY idx_offset_partner (kind, partner_id),
    KEY idx_offset_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '冲账单（垫付运费、返利、三包赔偿等抵扣往来款）';

CREATE TABLE fin_netting (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    netting_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    customer_id  BIGINT         NOT NULL COMMENT '客户',
    supplier_id  BIGINT         NOT NULL COMMENT '供应商（关联该客户的供应商）',
    netting_date DATE           NOT NULL COMMENT '对冲日期',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '对冲金额（正数）= 核销的应收合计 = 核销的应付合计（本位币）',
    voucher_id   BIGINT                  DEFAULT NULL COMMENT '凭证：借 应付账款（供应商）/ 贷 应收账款（客户）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_netting_no (netting_no, tenant_id),
    KEY idx_netting_customer (customer_id),
    KEY idx_netting_supplier (supplier_id),
    KEY idx_netting_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收应付对冲：关联的客户与供应商之间用应付抵减应收（不收付现金）';

CREATE TABLE fin_netting_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    netting_id  BIGINT         NOT NULL COMMENT '对冲单',
    bill_id     BIGINT         NOT NULL COMMENT '核销的应收 / 应付单',
    kind        VARCHAR(8)     NOT NULL COMMENT '种类 AR应收 AP应付',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '核销金额（与单据金额同号，红字单据为负数）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_netting_item_netting (netting_id),
    KEY idx_netting_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收应付对冲的核销明细';

CREATE TABLE fin_io_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)  NOT NULL COMMENT '类别名称',
    code        VARCHAR(32)           DEFAULT NULL COMMENT '内置类别标识（程序使用，不能删除、不能改方向）：SALES_FREIGHT销售运费 FREIGHT_ADVANCE代垫运费',
    direction   VARCHAR(8)   NOT NULL COMMENT '方向(字典 fin_fund_direction) IN收入 OUT支出',
    account_id  BIGINT                DEFAULT NULL COMMENT '凭证科目（末级）：收入记贷方、支出记借方',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_io_category (direction, name, tenant_id),
    UNIQUE KEY uk_io_category_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收支类别（其他收入、费用支出的分类）';

CREATE TABLE fin_io_doc (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no        VARCHAR(32)    NOT NULL COMMENT '单号',
    direction     VARCHAR(8)     NOT NULL COMMENT '方向(字典 fin_fund_direction) IN其他收入 OUT费用支出',
    doc_date      DATE           NOT NULL COMMENT '日期',
    ticket_no     VARCHAR(64)             DEFAULT NULL COMMENT '票据编号',
    partner_type  VARCHAR(16)             DEFAULT NULL COMMENT '关联单位类型(字典 fin_io_partner_type) CUSTOMER客户 SUPPLIER供应商 OTHER其他',
    partner_id    BIGINT                  DEFAULT NULL COMMENT '客户 / 供应商',
    partner_name  VARCHAR(128)            DEFAULT NULL COMMENT '其他单位的名称',
    settle_method VARCHAR(32)             DEFAULT NULL COMMENT '结算方式(业务字典 fin_settle_method)',
    account_id    BIGINT         NOT NULL COMMENT '结算账户（现金 / 银行末级科目）',
    dept_id       BIGINT                  DEFAULT NULL COMMENT '费用归属部门',
    amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额合计',
    status        VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_io_status) DRAFT待确认 CONFIRMED已确认',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '确认生成的凭证',
    confirm_by    BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time  DATETIME                DEFAULT NULL COMMENT '确认时间',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    source_type   VARCHAR(32)             DEFAULT NULL COMMENT '来源单据类型(字典 fin_bill_source)：由业务单据生成时填写（如销售出库、采购退货现付的运费），手工录入为空',
    source_id     BIGINT                  DEFAULT NULL COMMENT '来源单据ID',
    source_no     VARCHAR(32)             DEFAULT NULL COMMENT '来源单据号',
    create_by     BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_io_doc_no (doc_no, tenant_id),
    KEY idx_io_doc_date (doc_date),
    KEY idx_io_doc_partner (partner_id),
    KEY idx_io_doc_source (source_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '其他收支单（其他收入单、费用支出单）';

CREATE TABLE fin_io_doc_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id      BIGINT         NOT NULL COMMENT '收支单',
    category_id BIGINT         NOT NULL COMMENT '收支类别',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_io_item_doc (doc_id),
    KEY idx_io_item_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '其他收支单明细';

CREATE TABLE fin_invoice (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind           VARCHAR(8)     NOT NULL COMMENT '种类 OUTPUT销项（开给客户） INPUT进项（供应商开来）',
    invoice_no     VARCHAR(32)    NOT NULL COMMENT '发票号码',
    invoice_type   VARCHAR(16)    NOT NULL DEFAULT 'SPECIAL' COMMENT '发票类型(字典 fin_invoice_type) SPECIAL增值税专用发票 NORMAL普通发票 E_SPECIAL数电票（专票） E_NORMAL数电票（普票）',
    partner_id     BIGINT         NOT NULL COMMENT '客户 / 供应商',
    invoice_date   DATE           NOT NULL COMMENT '开票日期',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '价税合计（红字发票为负数）',
    tax_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    matched_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已勾稽应收应付单的金额，导入的发票可以之后再勾稽',
    seller_tax_no  VARCHAR(32)             DEFAULT NULL COMMENT '销售方纳税人识别号',
    seller_name    VARCHAR(128)            DEFAULT NULL COMMENT '销售方名称',
    buyer_tax_no   VARCHAR(32)             DEFAULT NULL COMMENT '购买方纳税人识别号',
    buyer_name     VARCHAR(128)            DEFAULT NULL COMMENT '购买方名称',
    source         VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工登记 XML数电票文件导入 EXCEL发票清单导入',
    status         VARCHAR(16)    NOT NULL DEFAULT 'VALID' COMMENT '状态 VALID有效 VOID已作废',
    certify_period INT                     DEFAULT NULL COMMENT '进项发票认证（勾选抵扣）期间 yyyyMM',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (kind, invoice_no, tenant_id),
    KEY idx_invoice_partner (kind, partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票登记：与应收应付单勾稽，未开票 / 未收票的部分为暂估';

CREATE TABLE fin_invoice_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    bill_id     BIGINT         NOT NULL COMMENT '应收 / 应付单',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '勾稽金额（价税合计）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_item_invoice (invoice_id),
    KEY idx_invoice_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票勾稽的应收应付单';

CREATE TABLE fin_invoice_line (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    seq         INT            NOT NULL COMMENT '行号',
    item_name   VARCHAR(200)   NOT NULL COMMENT '项目名称（含税收分类简称）',
    spec        VARCHAR(128)            DEFAULT NULL COMMENT '规格型号',
    unit        VARCHAR(32)             DEFAULT NULL COMMENT '单位',
    qty         DECIMAL(18, 6)          DEFAULT NULL COMMENT '数量',
    price       DECIMAL(18, 8)          DEFAULT NULL COMMENT '单价（不含税）',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额（不含税）',
    tax_rate    DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%），如 13',
    tax_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_line_invoice (invoice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票明细行：导入数电票时保存';

CREATE TABLE fin_advance (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    advance_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类 RECEIPT预收（客户） PAYMENT预付（供应商）',
    partner_id   BIGINT         NOT NULL COMMENT '客户 / 供应商',
    advance_date DATE           NOT NULL COMMENT '收付款日期',
    account_id   BIGINT         NOT NULL COMMENT '结算科目（库存现金 / 银行存款）',
    method       VARCHAR(16)             DEFAULT NULL COMMENT '结算方式(字典 fin_settle_method)',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '金额',
    used_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已冲抵应收应付的金额',
    currency     VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率；金额 = 原币金额 × 汇率',
    amount_fc    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    used_fc      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币已冲抵',
    voucher_id   BIGINT                  DEFAULT NULL COMMENT '收付款凭证',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_advance_no (advance_no, tenant_id),
    KEY idx_advance_partner (kind, partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预收 / 预付款：收付时记预收账款 / 预付账款，冲抵应收应付单时转出';

CREATE TABLE fin_advance_apply (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    advance_id  BIGINT         NOT NULL COMMENT '预收 / 预付款',
    bill_id     BIGINT         NOT NULL COMMENT '冲抵的应收 / 应付单',
    settlement_id BIGINT                DEFAULT NULL COMMENT '在收付款中冲抵时的收付款单（凭证并入收付款凭证）',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '冲抵金额',
    amount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '冲抵原币金额',
    bill_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '单据一方的本位币核销金额（计入单据的已核销，外币单据可能与预收预付一方的冲抵金额不同）',
    apply_date  DATE           NOT NULL COMMENT '冲抵日期',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '转账凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_advance_apply_advance (advance_id),
    KEY idx_advance_apply_bill (bill_id),
    KEY idx_advance_apply_settlement (settlement_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预收预付冲抵应收应付';

-- 采购合同 / 框架协议：审批生效后采购订单可引用，按合同价下单并校验数量、金额上限
CREATE TABLE scm_purchase_contract (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_no      VARCHAR(32)    NOT NULL COMMENT '合同编号',
    name             VARCHAR(128)   NOT NULL COMMENT '合同名称',
    supplier_id      BIGINT         NOT NULL COMMENT '供应商',
    contract_type    VARCHAR(16)    NOT NULL COMMENT '类型(字典 scm_contract_type) FRAMEWORK框架协议 FIXED固定数量合同',
    sign_date        DATE           NOT NULL COMMENT '签订日期',
    start_date       DATE           NOT NULL COMMENT '生效日期',
    end_date         DATE           NOT NULL COMMENT '到期日期',
    amount_limit     DECIMAL(18, 2)          DEFAULT NULL COMMENT '金额上限（价税合计），为空不限',
    payment_days     INT                     DEFAULT NULL COMMENT '付款天数',
    payment_terms    VARCHAR(255)            DEFAULT NULL COMMENT '付款条款',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_contract_status) DRAFT草稿 APPROVING审批中 ACTIVE生效 CLOSED已关闭',
    tender_id        BIGINT                  DEFAULT NULL COMMENT '来源招标',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    approve_time     DATETIME                DEFAULT NULL COMMENT '生效时间',
    close_reason     VARCHAR(255)            DEFAULT NULL COMMENT '关闭原因',
    remark           VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '部门',
    create_by        BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_contract_no (contract_no, tenant_id),
    KEY idx_purchase_contract_supplier (supplier_id),
    KEY idx_purchase_contract_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购合同';

CREATE TABLE scm_purchase_contract_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_id BIGINT         NOT NULL COMMENT '采购合同',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '合同含税单价',
    tax_rate    DECIMAL(5, 2)  NOT NULL COMMENT '税率（%）',
    qty_limit   DECIMAL(18, 4)          DEFAULT NULL COMMENT '数量上限，为空不限',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_purchase_contract_item (contract_id),
    KEY idx_purchase_contract_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购合同明细';

-- 供应商对账单：按期间汇总供应商的应付单，发送给供应商确认，登记确认结果或异议、发票信息
CREATE TABLE fin_supplier_statement (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    statement_no    VARCHAR(32)    NOT NULL COMMENT '对账单号',
    supplier_id     BIGINT         NOT NULL COMMENT '供应商',
    begin_date      DATE           NOT NULL COMMENT '对账期间起',
    end_date        DATE           NOT NULL COMMENT '对账期间止',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '对账金额（应付单价税合计）',
    bill_count      INT            NOT NULL DEFAULT 0 COMMENT '应付单数',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_statement_status) DRAFT草稿 SENT待确认 CONFIRMED已确认 DISPUTED有异议',
    supplier_remark VARCHAR(500)            DEFAULT NULL COMMENT '供应商意见',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '供应商确认时间',
    invoice_no      VARCHAR(64)             DEFAULT NULL COMMENT '发票号码',
    invoice_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '发票金额',
    invoice_date    DATE                    DEFAULT NULL COMMENT '开票日期',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_statement_no (statement_no, tenant_id),
    KEY idx_statement_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商对账单';

CREATE TABLE fin_supplier_statement_line (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    statement_id BIGINT         NOT NULL COMMENT '对账单',
    bill_id      BIGINT         NOT NULL COMMENT '应付单',
    bill_no      VARCHAR(32)    NOT NULL COMMENT '应付单号',
    source_type  VARCHAR(32)    NOT NULL COMMENT '来源(字典 fin_bill_source)',
    source_no    VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    bill_date    DATE           NOT NULL COMMENT '单据日期',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount   DECIMAL(18, 2) NOT NULL COMMENT '税额',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_statement_line (statement_id),
    KEY idx_statement_line_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商对账单明细';

-- 资产变动履历：领用、归还、调拨（部门 / 使用人 / 存放地点），资料修改中改了使用信息的也记录
CREATE TABLE fin_asset_move (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    asset_id      BIGINT       NOT NULL COMMENT '资产',
    move_type     VARCHAR(16)  NOT NULL COMMENT '类型(字典 fin_asset_move_type) ASSIGN领用 RETURN归还 TRANSFER调拨',
    move_date     DATE         NOT NULL COMMENT '日期',
    from_dept_id  BIGINT                DEFAULT NULL COMMENT '原部门',
    to_dept_id    BIGINT                DEFAULT NULL COMMENT '新部门',
    from_user_id  BIGINT                DEFAULT NULL COMMENT '原使用人',
    to_user_id    BIGINT                DEFAULT NULL COMMENT '新使用人',
    from_location VARCHAR(128)          DEFAULT NULL COMMENT '原存放地点',
    to_location   VARCHAR(128)          DEFAULT NULL COMMENT '新存放地点',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by     BIGINT                DEFAULT NULL COMMENT '经办人',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_asset_move (asset_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资产变动';

-- 资产盘点：按部门 / 地点生成盘点清单，逐项确认，完成时盘亏的处置、位置变化的更新卡片
CREATE TABLE fin_asset_check (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_no    VARCHAR(32)  NOT NULL COMMENT '盘点单号',
    check_date  DATE         NOT NULL COMMENT '盘点日期',
    dept_id     BIGINT                DEFAULT NULL COMMENT '盘点范围：部门，为空全部',
    location    VARCHAR(128)          DEFAULT NULL COMMENT '盘点范围：存放地点（包含），为空全部',
    status      VARCHAR(16)  NOT NULL COMMENT '状态(字典 fin_asset_check_status) CHECKING盘点中 DONE已完成',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                DEFAULT NULL COMMENT '创建人',
    finish_time DATETIME              DEFAULT NULL COMMENT '完成时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_check_no (check_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资产盘点';

CREATE TABLE fin_asset_check_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_id        BIGINT       NOT NULL COMMENT '盘点单',
    asset_id        BIGINT                DEFAULT NULL COMMENT '资产，盘盈的为空',
    book_dept_id    BIGINT                DEFAULT NULL COMMENT '账面部门',
    book_user_id    BIGINT                DEFAULT NULL COMMENT '账面使用人',
    book_location   VARCHAR(128)          DEFAULT NULL COMMENT '账面存放地点',
    result          VARCHAR(16)  NOT NULL COMMENT '结果(字典 fin_asset_check_result) PENDING待盘 NORMAL相符 MOVED位置变化 LOSS盘亏 GAIN盘盈',
    actual_dept_id  BIGINT                DEFAULT NULL COMMENT '实际部门（位置变化）',
    actual_user_id  BIGINT                DEFAULT NULL COMMENT '实际使用人（位置变化）',
    actual_location VARCHAR(128)          DEFAULT NULL COMMENT '实际存放地点（位置变化）',
    gain_name       VARCHAR(64)           DEFAULT NULL COMMENT '盘盈资产名称',
    remark          VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    check_by        BIGINT                DEFAULT NULL COMMENT '盘点人',
    check_time      DATETIME              DEFAULT NULL COMMENT '盘点时间',
    PRIMARY KEY (id),
    KEY idx_asset_check_item (check_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资产盘点明细';

-- 低值易耗品领用：部门从仓库领用（生成部门领用出库单），月末成本核算按实际单价一次摊销计入部门费用
CREATE TABLE fin_consumable_use (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    use_no             VARCHAR(32)  NOT NULL COMMENT '领用单号',
    dept_id            BIGINT       NOT NULL COMMENT '领用部门（费用归属）',
    user_id            BIGINT                DEFAULT NULL COMMENT '领用人',
    warehouse_id       BIGINT       NOT NULL COMMENT '仓库',
    use_date           DATE         NOT NULL COMMENT '领用日期',
    expense_account_id BIGINT       NOT NULL COMMENT '费用科目（管理费用、制造费用、销售费用等）',
    status             VARCHAR(16)  NOT NULL COMMENT '状态(字典 fin_consumable_status) DRAFT草稿 CONFIRMED已出库',
    wms_doc_id         BIGINT                DEFAULT NULL COMMENT '部门领用出库单',
    remark             VARCHAR(255)          DEFAULT NULL COMMENT '用途',
    create_by          BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_consumable_use_no (use_no, tenant_id),
    KEY idx_consumable_use_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '低值易耗品领用';

CREATE TABLE fin_consumable_use_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    use_id      BIGINT         NOT NULL COMMENT '领用单',
    material_id BIGINT         NOT NULL COMMENT '物料（低值易耗品）',
    location_id BIGINT                  DEFAULT NULL COMMENT '库位',
    batch_no    VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '数量',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_consumable_use_item (use_id),
    KEY idx_consumable_use_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '低值易耗品领用明细';

-- 账户调拨：银行账户之间、银行与现金之间转账，生成凭证（借 转入账户、财务费用，贷 转出账户）
CREATE TABLE fin_fund_transfer (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    transfer_no     VARCHAR(32)    NOT NULL COMMENT '调拨单号',
    transfer_date   DATE           NOT NULL COMMENT '调拨日期',
    from_account_id BIGINT         NOT NULL COMMENT '转出科目',
    to_account_id   BIGINT         NOT NULL COMMENT '转入科目',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '调拨金额',
    fee             DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手续费（计入财务费用）',
    fee_bearer      VARCHAR(8)     NOT NULL DEFAULT 'FROM' COMMENT '手续费支付账户(字典 fin_fee_bearer) FROM转出账户支付 TO转入账户支付',
    handler_id      BIGINT                  DEFAULT NULL COMMENT '经办人',
    ticket_no       VARCHAR(64)             DEFAULT NULL COMMENT '票据号码（转账回单、支票号等）',
    voucher_id      BIGINT                  DEFAULT NULL COMMENT '凭证',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_no (transfer_no, tenant_id),
    KEY idx_transfer_date (transfer_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账户调拨';

-- 资金计划的手工计划项（预计的其他收入、支出），与应收应付、报销一起汇总为资金计划
CREATE TABLE fin_fund_plan_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_date   DATE           NOT NULL COMMENT '预计日期',
    direction   VARCHAR(8)     NOT NULL COMMENT '方向 IN流入 OUT流出',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    description VARCHAR(128)   NOT NULL COMMENT '说明，如 房租、借款到期',
    create_by   BIGINT                  DEFAULT NULL COMMENT '录入人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_fund_plan_date (plan_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资金计划项';

CREATE TABLE fin_bank_statement (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id  BIGINT         NOT NULL COMMENT '银行存款科目（末级）',
    txn_date    DATE           NOT NULL COMMENT '交易日期',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额：收入为正、支出为负',
    summary     VARCHAR(255)            DEFAULT NULL COMMENT '摘要',
    ref_no      VARCHAR(64)             DEFAULT NULL COMMENT '银行流水号',
    entry_id    BIGINT                  DEFAULT NULL COMMENT '勾对的凭证分录',
    batch_no    VARCHAR(32)             DEFAULT NULL COMMENT '导入批次',
    create_by   BIGINT                  DEFAULT NULL COMMENT '导入人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bank_statement_account (account_id, txn_date),
    KEY idx_bank_statement_entry (entry_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '银行对账单';

CREATE TABLE fin_expense_claim (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    claim_no         VARCHAR(32)    NOT NULL COMMENT '报销单号',
    user_id          BIGINT         NOT NULL COMMENT '报销人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '费用部门',
    claim_date       DATE           NOT NULL COMMENT '报销日期',
    amount           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '报销金额',
    loan_id          BIGINT                  DEFAULT NULL COMMENT '冲抵的借款',
    loan_offset      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '冲借款金额，实付 = 报销金额 - 冲借款',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_claim_status) DRAFT草稿 APPROVING审批中 APPROVED待付款 PAID已付款',
    pay_account_id   BIGINT                  DEFAULT NULL COMMENT '付款科目',
    pay_date         DATE                    DEFAULT NULL COMMENT '付款日期',
    voucher_id       BIGINT                  DEFAULT NULL COMMENT '付款凭证',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '事由',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_claim_no (claim_no, tenant_id),
    KEY idx_claim_user (user_id),
    KEY idx_claim_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '费用报销单';

CREATE TABLE fin_expense_claim_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    claim_id     BIGINT         NOT NULL COMMENT '报销单',
    expense_date DATE                    DEFAULT NULL COMMENT '费用日期',
    account_id   BIGINT         NOT NULL COMMENT '费用科目（末级）',
    project_id   BIGINT                  DEFAULT NULL COMMENT '项目（辅助核算）',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '金额',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_claim_item_claim (claim_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '费用报销明细';

CREATE TABLE fin_loan (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    loan_no          VARCHAR(32)    NOT NULL COMMENT '借款单号',
    user_id          BIGINT         NOT NULL COMMENT '借款人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '部门',
    loan_date        DATE           NOT NULL COMMENT '借款日期',
    amount           DECIMAL(18, 2) NOT NULL COMMENT '借款金额',
    repaid_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已还款（含报销冲抵）',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_loan_status) DRAFT草稿 APPROVING审批中 APPROVED待付款 PAID已付款（未还清） CLEARED已还清',
    pay_account_id   BIGINT                  DEFAULT NULL COMMENT '付款科目',
    voucher_id       BIGINT                  DEFAULT NULL COMMENT '付款凭证',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    purpose          VARCHAR(255)            DEFAULT NULL COMMENT '借款事由',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_loan_no (loan_no, tenant_id),
    KEY idx_loan_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工借款（备用金）：付款记其他应收款，还款或报销冲抵';

CREATE TABLE fin_loan_repay (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    loan_id     BIGINT         NOT NULL COMMENT '借款',
    repay_date  DATE           NOT NULL COMMENT '还款日期',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    method      VARCHAR(16)    NOT NULL COMMENT '方式 CASH现金还款 CLAIM报销冲抵',
    claim_id    BIGINT                  DEFAULT NULL COMMENT '冲抵的报销单',
    account_id  BIGINT                  DEFAULT NULL COMMENT '收款科目（现金还款）',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_loan_repay_loan (loan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '借款还款记录';

CREATE TABLE fin_asset (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code               VARCHAR(32)    NOT NULL COMMENT '资产编码',
    name               VARCHAR(64)    NOT NULL COMMENT '资产名称',
    category           VARCHAR(32)    NOT NULL COMMENT '类别(字典 fin_asset_category)',
    model              VARCHAR(64)             DEFAULT NULL COMMENT '规格型号',
    dept_id            BIGINT                  DEFAULT NULL COMMENT '使用部门',
    custodian_id       BIGINT                  DEFAULT NULL COMMENT '使用人（保管人）',
    location           VARCHAR(128)            DEFAULT NULL COMMENT '存放地点',
    use_date           DATE           NOT NULL COMMENT '开始使用日期，次月开始计提折旧',
    original_value     DECIMAL(18, 2) NOT NULL COMMENT '原值',
    residual_rate      DECIMAL(5, 2)  NOT NULL DEFAULT 5 COMMENT '残值率（%）',
    useful_months      INT            NOT NULL COMMENT '使用年限（月）',
    depreciated_months INT            NOT NULL DEFAULT 0 COMMENT '已计提月数',
    accumulated        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '累计折旧',
    expense_account_id BIGINT         NOT NULL COMMENT '折旧费用科目，如管理费用-折旧费、制造费用',
    status             VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_asset_status) IN_USE在用 IDLE闲置 DISPOSED已处置',
    dispose_date       DATE                    DEFAULT NULL COMMENT '处置日期',
    dispose_voucher_id BIGINT              DEFAULT NULL COMMENT '处置凭证，删除后资产恢复处置前的状态',
    prev_status        VARCHAR(16)         DEFAULT NULL COMMENT '处置前的状态',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '固定资产卡片';

CREATE TABLE fin_depreciation (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT            NOT NULL COMMENT '会计期间 yyyyMM',
    asset_id    BIGINT         NOT NULL COMMENT '资产',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '本期折旧额',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '计提人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_depreciation (period, asset_id, tenant_id),
    KEY idx_depreciation_asset (asset_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '折旧计提记录';

CREATE TABLE fin_cost_run (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT          NOT NULL COMMENT '会计期间 yyyyMM',
    voucher_id  BIGINT                DEFAULT NULL COMMENT '生成的成本结转凭证',
    kind        VARCHAR(16)  NOT NULL DEFAULT 'RUN' COMMENT '类型 RUN成本核算 OPENING存货期初（启用期间的上一期）',
    overhead_variance DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '转入主营业务成本的制造费用差异',
    create_by   BIGINT                DEFAULT NULL COMMENT '核算人',
    create_time DATETIME              DEFAULT NULL COMMENT '核算时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_run_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '成本核算记录';

CREATE TABLE fin_material_cost (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period           INT            NOT NULL COMMENT '会计期间',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    open_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '期初数量',
    open_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初金额',
    purchase_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '采购入库（扣除退货）',
    purchase_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '采购金额（不含税）',
    make_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '生产入库',
    make_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '生产成本',
    other_in_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '其他入库（盘盈等）',
    other_in_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他入库金额',
    issue_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '生产领用（扣除退料）',
    issue_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '生产领用金额',
    sales_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '销售出库（扣除退货）',
    sales_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售成本',
    other_out_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '其他出库（盘亏等）',
    other_out_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他出库金额',
    close_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '期末数量',
    close_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期末金额',
    unit_cost        DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '本期出库单价（月末一次加权平均为加权平均单价，其他计价方法为本期出库的平均单价）',
    cost_method      VARCHAR(16)    NOT NULL DEFAULT 'WEIGHTED' COMMENT '本期使用的计价方法(字典 wms_cost_method)',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_cost (period, material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '存货成本（按物料的计价方法核算）';

CREATE TABLE fin_cost_layer (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT            NOT NULL COMMENT '会计期间',
    material_id BIGINT         NOT NULL COMMENT '物料',
    seq         INT            NOT NULL COMMENT '批次顺序，越小入库越早',
    in_date     DATE           NOT NULL COMMENT '入库日期',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '结存数量',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '结存金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_cost_layer (period, material_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '先进先出的期末结存批次：下一期核算的期初批次';

CREATE TABLE fin_budget (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    budget_year INT            NOT NULL COMMENT '预算年度',
    dept_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '部门，0 为全公司',
    account_id  BIGINT         NOT NULL COMMENT '科目（可以是上级科目，执行数按下级汇总）',
    m01         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '1 月预算',
    m02         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '2 月预算',
    m03         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '3 月预算',
    m04         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '4 月预算',
    m05         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '5 月预算',
    m06         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '6 月预算',
    m07         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '7 月预算',
    m08         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '8 月预算',
    m09         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '9 月预算',
    m10         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '10 月预算',
    m11         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '11 月预算',
    m12         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '12 月预算',
    total       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '全年预算 = 各月合计',
    control_mode VARCHAR(8)       NOT NULL DEFAULT 'NONE' COMMENT '预算控制(字典 fin_budget_control) NONE不控制 WARN超预算提示 BLOCK超预算禁止（费用报销）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget (budget_year, dept_id, account_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预算';

CREATE TABLE fin_account_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role        VARCHAR(32) NOT NULL COMMENT '用途（AccountRole），如 RECEIVABLE 应收账款、INVENTORY 存货',
    dim_value   VARCHAR(32) NOT NULL DEFAULT '' COMMENT '维度取值：物料分类 / 职位类别，空串为该用途的默认科目',
    account_id  BIGINT      NOT NULL COMMENT '科目（末级）',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_rule (role, dim_value, tenant_id),
    KEY idx_account_rule_account (account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证科目设置：业务单据自动生成凭证时各用途使用的科目，没有设置的用途取预置科目';

-- ---------------------------------------------------------------- 智能（AI）：模型配置、用量、AI 问数的会话与消息
CREATE TABLE ai_config (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    provider            VARCHAR(16)   NOT NULL COMMENT '服务商 DEEPSEEK QWEN ZHIPU MOONSHOT DOUBAO CUSTOM（AiProvider）',
    base_url            VARCHAR(255)           DEFAULT NULL COMMENT '接口地址（不含 /chat/completions），为空时用服务商的默认地址',
    api_key             VARCHAR(512)           DEFAULT NULL COMMENT 'API Key（AES-GCM 加密，enc: 开头），不返回前端',
    chat_model          VARCHAR(64)            DEFAULT NULL COMMENT '对话 / 工具调用模型',
    vision_model        VARCHAR(64)            DEFAULT NULL COMMENT '识图模型，为空时智能录入只支持 PDF、Excel、文本',
    enabled             TINYINT       NOT NULL DEFAULT 0 COMMENT '总开关 1启用 0停用',
    monthly_token_limit BIGINT                 DEFAULT NULL COMMENT '每月 token 上限（输入 + 输出），为空不限',
    chat_enabled        TINYINT       NOT NULL DEFAULT 0 COMMENT 'AI 问数 1开启 0关闭',
    extract_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '智能录入 1开启 0关闭',
    cleanse_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '数据整理 1开启 0关闭',
    report_enabled      TINYINT       NOT NULL DEFAULT 0 COMMENT '智能报告 1开启 0关闭',
    temperature         DECIMAL(3, 2) NOT NULL DEFAULT 0.30 COMMENT '温度',
    max_tokens          INT                    DEFAULT NULL COMMENT '单次回答的最大 token 数，为空由服务商决定',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_config (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型配置（国产模型，OpenAI 兼容协议），每个租户一条';

CREATE TABLE ai_usage (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id           BIGINT               DEFAULT NULL COMMENT '调用人，没有登录用户时为空',
    feature           VARCHAR(16) NOT NULL COMMENT '功能 CHAT问数 EXTRACT录入 CLEANSE整理 REPORT报告 TEST测试连接（AiFeature）',
    model             VARCHAR(64)          DEFAULT NULL COMMENT '模型',
    prompt_tokens     INT         NOT NULL DEFAULT 0 COMMENT '输入 token',
    completion_tokens INT         NOT NULL DEFAULT 0 COMMENT '输出 token',
    create_time       DATETIME             DEFAULT NULL COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_ai_usage_time (tenant_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型调用的 token 用量';

CREATE TABLE ai_conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '所属用户（只有本人能访问）',
    title       VARCHAR(100)          DEFAULT NULL COMMENT '标题（默认为第一条提问）',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '最近提问时间',
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user (user_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的会话';

CREATE TABLE ai_message (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    conversation_id BIGINT      NOT NULL COMMENT '会话',
    role            VARCHAR(16) NOT NULL COMMENT '角色 USER提问 ASSISTANT回答 TOOL工具结果（AiMessageRole）',
    content         TEXT COMMENT '提问 / 回答的文字；工具消息为调用参数',
    tool_call_id    VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：对应的工具调用 ID',
    tool_name       VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：工具名称',
    tool_calls      TEXT COMMENT '回答：请求调用的工具（JSON），列表不返回',
    tool_result     MEDIUMTEXT COMMENT '工具消息：回传给模型的结果（JSON），列表不返回',
    tokens          INT                  DEFAULT NULL COMMENT '回答消耗的 token',
    create_time     DATETIME             DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation (conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的消息';

CREATE TABLE ai_cleanse_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    source_key      VARCHAR(64)  NOT NULL COMMENT '数据类型（AiCleanseSource.key，如 wms.material）',
    keyword         VARCHAR(64)           DEFAULT NULL COMMENT '编码或名称关键字（整理范围），为空表示全部',
    status          VARCHAR(16)  NOT NULL COMMENT '状态 RUNNING进行中 COMPLETED已完成 FAILED失败 CANCELLED已取消（AiCleanseTaskStatus）',
    phase           VARCHAR(16)  NOT NULL COMMENT '阶段 LOADING读取 DUPLICATE查重 NORMALIZE规范化 DONE结束（AiCleansePhase）',
    running_key     VARCHAR(64)           DEFAULT NULL COMMENT '进行中时为数据类型，结束后清空（唯一约束：同一数据类型同时只有一个进行中的任务）',
    record_count    INT          NOT NULL DEFAULT 0 COMMENT '读取的记录数',
    truncated       TINYINT      NOT NULL DEFAULT 0 COMMENT '记录超过上限，只分析了一部分 1是 0否',
    group_count     INT          NOT NULL DEFAULT 0 COMMENT '疑似重复候选组数',
    total_steps     INT          NOT NULL DEFAULT 0 COMMENT '模型调用总步数',
    done_steps      INT          NOT NULL DEFAULT 0 COMMENT '已完成步数',
    failed_steps    INT          NOT NULL DEFAULT 0 COMMENT '模型输出不合格而跳过的步数',
    duplicate_count INT          NOT NULL DEFAULT 0 COMMENT '疑似重复建议数（待停用的记录）',
    normalize_count INT          NOT NULL DEFAULT 0 COMMENT '规范化建议数',
    missing_count   INT          NOT NULL DEFAULT 0 COMMENT '缺失字段提示数',
    discarded_count INT          NOT NULL DEFAULT 0 COMMENT '校验不通过而丢弃的建议数',
    tokens          INT          NOT NULL DEFAULT 0 COMMENT '消耗的 token',
    error_key       VARCHAR(100)          DEFAULT NULL COMMENT '失败原因的消息键',
    error_args      VARCHAR(1000)         DEFAULT NULL COMMENT '失败原因的参数（JSON 数组）',
    create_by       BIGINT                DEFAULT NULL COMMENT '发起人',
    create_time     DATETIME              DEFAULT NULL COMMENT '发起时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '最近进展时间',
    finish_time     DATETIME              DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_cleanse_task_running (running_key, tenant_id),
    KEY idx_ai_cleanse_task_source (tenant_id, source_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理任务（主数据查重与规范化）';

CREATE TABLE ai_cleanse_suggestion (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    task_id        BIGINT       NOT NULL COMMENT '整理任务',
    kind           VARCHAR(16)  NOT NULL COMMENT '种类 DUPLICATE疑似重复 NORMALIZE规范化 MISSING缺失字段（AiCleanseKind）',
    group_no       INT                   DEFAULT NULL COMMENT '疑似重复的组号',
    record_id      BIGINT       NOT NULL COMMENT '记录ID',
    record_code    VARCHAR(64)           DEFAULT NULL COMMENT '记录编码',
    record_name    VARCHAR(255)          DEFAULT NULL COMMENT '记录名称',
    field          VARCHAR(16)           DEFAULT NULL COMMENT '字段 NAME SPEC CATEGORY UNIT CONTACT ADDRESS（AiCleanseField），疑似重复为空',
    old_value      VARCHAR(255)          DEFAULT NULL COMMENT '生成建议时的原值',
    new_value      VARCHAR(255)          DEFAULT NULL COMMENT '建议值',
    keep_record_id BIGINT                DEFAULT NULL COMMENT '疑似重复：建议保留的记录',
    keep_code      VARCHAR(64)           DEFAULT NULL COMMENT '保留记录的编码',
    keep_name      VARCHAR(255)          DEFAULT NULL COMMENT '保留记录的名称',
    reason         VARCHAR(500)          DEFAULT NULL COMMENT '理由（模型给出）',
    confidence     INT                   DEFAULT NULL COMMENT '置信度 0～100',
    status         VARCHAR(16)  NOT NULL COMMENT '状态 PENDING待处理 APPLIED已采纳 IGNORED已忽略（AiSuggestionStatus）',
    handled_by     BIGINT                DEFAULT NULL COMMENT '采纳 / 忽略人',
    handled_time   DATETIME              DEFAULT NULL COMMENT '采纳 / 忽略时间',
    create_time    DATETIME              DEFAULT NULL COMMENT '生成时间',
    PRIMARY KEY (id),
    KEY idx_ai_cleanse_suggestion_task (task_id, kind, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理建议';

CREATE TABLE ai_report (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    period_type       VARCHAR(8)   NOT NULL COMMENT '期间类型 WEEK周报 MONTH月报（AiReportPeriodType）',
    period_start      DATE         NOT NULL COMMENT '期间开始',
    period_end        DATE         NOT NULL COMMENT '期间结束（含）',
    title             VARCHAR(100) NOT NULL COMMENT '标题',
    content           MEDIUMTEXT COMMENT '报告正文（Markdown），列表不返回',
    user_id           BIGINT                DEFAULT NULL COMMENT '生成人，定时任务自动生成时为空（自动报告只有全部数据权限的用户能看）',
    sources           VARCHAR(500)          DEFAULT NULL COMMENT '包含的数据来源标识，逗号分隔',
    snapshot          MEDIUMTEXT COMMENT '交给模型的数据快照（JSON），列表不返回',
    status            VARCHAR(16)  NOT NULL COMMENT '状态 GENERATING生成中 DONE已完成 FAILED失败（AiReportStatus）',
    error             VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
    prompt_tokens     INT                   DEFAULT NULL COMMENT '输入 token',
    completion_tokens INT                   DEFAULT NULL COMMENT '输出 token',
    create_time       DATETIME              DEFAULT NULL COMMENT '生成时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_report_period (tenant_id, period_type, period_start),
    KEY idx_ai_report_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能报告（周报 / 月报）';

-- ---------------------------------------------------------------- 跨境电商：店铺、在线商品、平台订单、头程货件、物流渠道、结算、广告、退货、平台仓库存

CREATE TABLE ecom_store (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code          VARCHAR(32)    NOT NULL COMMENT '店铺编码',
    name          VARCHAR(64)    NOT NULL COMMENT '店铺名称',
    platform      VARCHAR(16)    NOT NULL COMMENT '平台(字典 ecom_platform) AMAZON ALIEXPRESS EBAY SHOPEE LAZADA SHOPIFY TIKTOK TEMU WALMART OZON OTHER',
    marketplace   VARCHAR(16)    NOT NULL COMMENT '站点 / 市场(业务字典 ecom_marketplace)，如 US DE JP',
    currency      VARCHAR(8)     NOT NULL COMMENT '店铺结算币种(fin_currency)',
    timezone      VARCHAR(64)             DEFAULT NULL COMMENT '时区',
    customer_id   BIGINT                  DEFAULT NULL COMMENT '对应的客户(scm_customer)：销售出库、应收的往来单位，新建店铺时自动建立',
    warehouse_id  BIGINT         NOT NULL COMMENT '默认发货仓（自发货订单出库、头程货件调出）',
    platform_warehouse_id BIGINT          DEFAULT NULL COMMENT '平台仓（FBA / 平台海外仓）：平台配送订单出库、头程货件调入',
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 15 COMMENT '预估平台佣金率（%），结算回写实际费用前按它估算利润',
    lead_days     INT            NOT NULL DEFAULT 30 COMMENT '补货周期（天）',
    safety_days   INT            NOT NULL DEFAULT 15 COMMENT '安全库存天数',
    owner_id      BIGINT                  DEFAULT NULL COMMENT '负责人（用户）',
    seller_id     VARCHAR(64)             DEFAULT NULL COMMENT '平台卖家 ID',
    credentials   TEXT COMMENT '渠道接口授权（JSON，加密保存，enc: 前缀）',
    last_sync_time DATETIME               DEFAULT NULL COMMENT '最近一次订单同步时间',
    sandbox       TINYINT        NOT NULL DEFAULT 0 COMMENT '渠道接口环境 1沙箱 / 测试 0正式',
    api_base_url  VARCHAR(255)            DEFAULT NULL COMMENT '自定义接口地址（私有网关、测试用模拟服务器），为空按环境取默认地址',
    order_sync    TINYINT        NOT NULL DEFAULT 1 COMMENT '自动同步订单 1是 0否',
    inventory_sync TINYINT       NOT NULL DEFAULT 0 COMMENT '自动回传可售库存（自发货） 1是 0否',
    inventory_buffer DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '回传库存时保留的安全数量（件），回传数 = 可用库存 ÷ 多件装 − 保留',
    authorized_time DATETIME              DEFAULT NULL COMMENT '最近一次授权（OAuth 回调）时间',
    last_sync_message VARCHAR(500)        DEFAULT NULL COMMENT '最近一次同步失败的原因，成功为空',
    status        TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_store_code (code, tenant_id),
    KEY idx_ecom_store_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台店铺';

CREATE TABLE ecom_sync_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id    BIGINT       NOT NULL COMMENT '店铺',
    sync_type   VARCHAR(16)  NOT NULL COMMENT '类型(字典 ecom_sync_type) ORDERS拉取订单 SHIP_CONFIRM回传发货 INVENTORY_PUSH回传库存 LISTINGS拉取商品 PLATFORM_STOCK平台仓库存 AUTH授权',
    trigger_by  VARCHAR(16)  NOT NULL DEFAULT 'JOB' COMMENT '触发 JOB定时任务 MANUAL手工',
    success     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功',
    fetched     INT          NOT NULL DEFAULT 0 COMMENT '取到 / 提交的条数',
    created     INT          NOT NULL DEFAULT 0 COMMENT '新增条数',
    updated     INT          NOT NULL DEFAULT 0 COMMENT '更新条数',
    message     VARCHAR(500)          DEFAULT NULL COMMENT '失败原因或说明',
    start_time  DATETIME     NOT NULL COMMENT '开始时间',
    end_time    DATETIME              DEFAULT NULL COMMENT '结束时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_sync_log_store (store_id, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '渠道同步日志';

CREATE TABLE ecom_listing (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id         BIGINT         NOT NULL COMMENT '店铺',
    seller_sku       VARCHAR(64)    NOT NULL COMMENT '平台 SKU（卖家 SKU）',
    platform_item_id VARCHAR(64)             DEFAULT NULL COMMENT '平台商品 ID（ASIN、Item ID）',
    parent_item_id   VARCHAR(64)             DEFAULT NULL COMMENT '父商品 ID（变体的父 ASIN）',
    title            VARCHAR(255)            DEFAULT NULL COMMENT '标题',
    variant          VARCHAR(128)            DEFAULT NULL COMMENT '变体属性',
    price            DECIMAL(18, 4)          DEFAULT NULL COMMENT '售价（店铺币种）',
    currency         VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    fulfillment      VARCHAR(16)    NOT NULL DEFAULT 'SELLER' COMMENT '配送方式(字典 ecom_fulfillment) PLATFORM平台仓配送 SELLER自发货',
    platform_qty     DECIMAL(18, 4)          DEFAULT NULL COMMENT '平台可售数量（同步 / 导入）',
    status           VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(字典 ecom_listing_status) ACTIVE在售 INACTIVE下架 INCOMPLETE不完整 DELETED已删除',
    material_id      BIGINT                  DEFAULT NULL COMMENT '映射的本地物料，为空未映射',
    pack_qty         DECIMAL(18, 4) NOT NULL DEFAULT 1 COMMENT '一个平台 SKU 对应的物料数量（多件装）',
    image_url        VARCHAR(255)            DEFAULT NULL COMMENT '图片',
    url              VARCHAR(255)            DEFAULT NULL COMMENT '商品链接',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_listing_sku (store_id, seller_sku, tenant_id),
    KEY idx_ecom_listing_material (material_id),
    KEY idx_ecom_listing_item (platform_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线商品（平台 SKU 与本地物料的映射）';

CREATE TABLE ecom_order (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    platform_order_no VARCHAR(64)    NOT NULL COMMENT '平台订单号',
    order_time        DATETIME       NOT NULL COMMENT '下单时间',
    paid_time         DATETIME                DEFAULT NULL COMMENT '付款时间',
    status            VARCHAR(16)    NOT NULL DEFAULT 'UNSHIPPED' COMMENT '平台状态(字典 ecom_order_status) PENDING待付款 UNSHIPPED待发货 SHIPPED已发货 CANCELLED已取消 REFUNDED已退款',
    fulfillment       VARCHAR(16)    NOT NULL DEFAULT 'SELLER' COMMENT '配送方式(字典 ecom_fulfillment)',
    local_status      VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '处理状态(字典 ecom_local_status) PENDING待处理 EXCEPTION异常 OUTBOUND已出库 SHIPPED已发货 CLOSED已关闭',
    buyer_name        VARCHAR(64)             DEFAULT NULL COMMENT '买家',
    buyer_email       VARCHAR(128)            DEFAULT NULL COMMENT '买家邮箱',
    ship_name         VARCHAR(64)             DEFAULT NULL COMMENT '收件人',
    ship_phone        VARCHAR(32)             DEFAULT NULL COMMENT '收件电话',
    ship_country      VARCHAR(8)              DEFAULT NULL COMMENT '收件国家 / 地区代码',
    ship_state        VARCHAR(64)             DEFAULT NULL COMMENT '州 / 省',
    ship_city         VARCHAR(64)             DEFAULT NULL COMMENT '城市',
    ship_postcode     VARCHAR(16)             DEFAULT NULL COMMENT '邮编',
    ship_address      VARCHAR(255)            DEFAULT NULL COMMENT '地址',
    currency          VARCHAR(8)     NOT NULL COMMENT '订单币种',
    item_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '商品金额',
    shipping_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '买家支付运费',
    tax_amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税费',
    discount_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '优惠',
    total_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '买家实付 = 商品 + 运费 + 税 − 优惠',
    exchange_rate     DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率：1 订单币种 = 汇率 × 本位币',
    total_base        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '买家实付（本位币）',
    platform_fee      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '平台佣金（订单币种），结算回写前为估算',
    fulfillment_fee   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '平台配送费（订单币种）',
    other_fee         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他费用（订单币种）',
    fee_actual        TINYINT        NOT NULL DEFAULT 0 COMMENT '费用是否为结算回写的实际值 1是 0估算',
    refund_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已退款（订单币种）',
    cost_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '商品成本（本位币，出库成本）',
    shipping_cost     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本公司物流成本（本位币）：自发货尾程运费 / 平台配送头程分摊',
    profit            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '利润（本位币）= 本位币金额 − (费用 + 退款) × 汇率 − 成本 − 物流',
    trade_doc_id      BIGINT                  DEFAULT NULL COMMENT '生成的销售出库单(scm_trade_doc)',
    trade_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '销售出库单号',
    warehouse_id      BIGINT                  DEFAULT NULL COMMENT '出库仓库',
    carrier           VARCHAR(64)             DEFAULT NULL COMMENT '承运商',
    tracking_no       VARCHAR(64)             DEFAULT NULL COMMENT '运单号',
    channel_id        BIGINT                  DEFAULT NULL COMMENT '物流渠道',
    ship_time         DATETIME                DEFAULT NULL COMMENT '发货时间',
    ship_by           DATETIME                DEFAULT NULL COMMENT '承诺发货截止',
    deliver_by        DATETIME                DEFAULT NULL COMMENT '承诺送达截止',
    exception         VARCHAR(255)            DEFAULT NULL COMMENT '异常原因',
    note              VARCHAR(255)            DEFAULT NULL COMMENT '买家留言',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id           BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by         BIGINT                  DEFAULT NULL COMMENT '制单人（同步的订单为空）',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_order_no (store_id, platform_order_no, tenant_id),
    KEY idx_ecom_order_time (order_time),
    KEY idx_ecom_order_local (local_status),
    KEY idx_ecom_order_tracking (tracking_no),
    KEY idx_ecom_order_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台订单';

CREATE TABLE ecom_order_item (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id         BIGINT         NOT NULL COMMENT '订单',
    platform_line_id VARCHAR(64)             DEFAULT NULL COMMENT '平台订单行 ID（回传发货用）',
    seller_sku       VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    platform_item_id VARCHAR(64)             DEFAULT NULL COMMENT '平台商品 ID',
    title            VARCHAR(255)            DEFAULT NULL COMMENT '标题',
    qty              DECIMAL(18, 4) NOT NULL COMMENT '数量（平台 SKU 件数）',
    price            DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '单价（订单币种）',
    amount           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '商品金额（订单币种）',
    shipping         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费',
    tax              DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税费',
    discount         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '优惠',
    material_id      BIGINT                  DEFAULT NULL COMMENT '匹配的物料',
    material_qty     DECIMAL(18, 4)          DEFAULT NULL COMMENT '物料数量 = 件数 × 多件装数量',
    cost             DECIMAL(18, 2)          DEFAULT NULL COMMENT '成本（本位币）',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_order_item_order (order_id),
    KEY idx_ecom_order_item_sku (seller_sku),
    KEY idx_ecom_order_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台订单明细';

CREATE TABLE ecom_inbound (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no            VARCHAR(32)    NOT NULL COMMENT '单据号',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    shipment_id       VARCHAR(64)             DEFAULT NULL COMMENT '平台货件号（FBA Shipment ID）',
    from_warehouse_id BIGINT         NOT NULL COMMENT '调出仓（本地仓）',
    to_warehouse_id   BIGINT         NOT NULL COMMENT '调入仓（平台仓 / 海外仓）',
    channel_id        BIGINT                  DEFAULT NULL COMMENT '物流渠道',
    carrier           VARCHAR(64)             DEFAULT NULL COMMENT '承运商',
    tracking_no       VARCHAR(64)             DEFAULT NULL COMMENT '运单号',
    ship_date         DATE                    DEFAULT NULL COMMENT '发运日期',
    eta_date          DATE                    DEFAULT NULL COMMENT '预计到达',
    receive_date      DATE                    DEFAULT NULL COMMENT '签收日期',
    box_count         INT                     DEFAULT NULL COMMENT '箱数',
    weight            DECIMAL(18, 3)          DEFAULT NULL COMMENT '毛重（kg）',
    volume            DECIMAL(18, 4)          DEFAULT NULL COMMENT '体积（m³）',
    freight           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '头程运费（本位币）',
    duty              DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '关税（本位币）',
    other_cost        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他费用（本位币）',
    total_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量合计',
    status            VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_inbound_status) DRAFT草稿 APPROVING审批中 SHIPPED已发运 RECEIVED已签收 CANCELLED已取消',
    transfer_doc_id   BIGINT                  DEFAULT NULL COMMENT '生成的在途调拨单(wms_stock_doc)',
    transfer_doc_no   VARCHAR(32)             DEFAULT NULL COMMENT '调拨单号',
    flow_instance_id  VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id           BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by         BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_inbound_no (doc_no, tenant_id),
    KEY idx_ecom_inbound_store (store_id, status),
    KEY idx_ecom_inbound_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '头程货件（本地仓发往平台仓 / 海外仓）';

CREATE TABLE ecom_inbound_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_id   BIGINT         NOT NULL COMMENT '货件',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    seller_sku   VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '数量',
    received_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已签收数量',
    unit_landed  DECIMAL(18, 6)          DEFAULT NULL COMMENT '单件头程成本（本位币）= (运费 + 关税 + 其他) ÷ 数量合计',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_inbound_item_inbound (inbound_id),
    KEY idx_ecom_inbound_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '头程货件明细';

CREATE TABLE ecom_logistics_channel (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code           VARCHAR(32)  NOT NULL COMMENT '渠道编码',
    name           VARCHAR(64)  NOT NULL COMMENT '渠道名称',
    provider       VARCHAR(64)           DEFAULT NULL COMMENT '物流商 / 货代',
    transport_mode VARCHAR(16)           DEFAULT NULL COMMENT '运输方式(业务字典 ecom_transport_mode)',
    supplier_id    BIGINT                DEFAULT NULL COMMENT '对应的供应商（结算运费）',
    days_min       INT                   DEFAULT NULL COMMENT '参考时效下限（天）',
    days_max       INT                   DEFAULT NULL COMMENT '参考时效上限（天）',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_channel_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '国际物流渠道';

CREATE TABLE ecom_logistics_rate (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    channel_id  BIGINT         NOT NULL COMMENT '渠道',
    country     VARCHAR(8)     NOT NULL COMMENT '目的国家 / 地区代码，* 为通用',
    min_weight  DECIMAL(18, 3) NOT NULL DEFAULT 0 COMMENT '重量段下限（kg，含）',
    max_weight  DECIMAL(18, 3)          DEFAULT NULL COMMENT '重量段上限（kg，不含），为空不限',
    base_fee    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '固定费用（本位币）',
    per_kg      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '每公斤费用（本位币）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_rate_channel (channel_id, country)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物流渠道费率';

CREATE TABLE ecom_settlement (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    settlement_no     VARCHAR(64)    NOT NULL COMMENT '平台结算编号',
    period_start      DATE           NOT NULL COMMENT '结算周期开始',
    period_end        DATE           NOT NULL COMMENT '结算周期结束',
    currency          VARCHAR(8)     NOT NULL COMMENT '币种',
    sales_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '订单收入',
    refund_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '退款（负数）',
    commission_fee    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '平台佣金（负数）',
    fulfillment_fee   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '配送费（负数）',
    storage_fee       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '仓储费（负数）',
    ad_fee            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '广告费（负数）',
    other_fee         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他费用（负数）',
    adjustment        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '调整',
    net_amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '结算净额',
    exchange_rate     DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（结算周期结束日所在月）',
    net_base          DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '结算净额（本位币）',
    status            VARCHAR(16)    NOT NULL DEFAULT 'IMPORTED' COMMENT '状态(字典 ecom_settlement_status) IMPORTED已导入 CONFIRMED已确认',
    fin_settlement_id BIGINT                  DEFAULT NULL COMMENT '确认时生成的收款单(fin_settlement)',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by         BIGINT                  DEFAULT NULL COMMENT '导入人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_settlement_no (store_id, settlement_no, tenant_id),
    KEY idx_ecom_settlement_period (period_end)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台结算单';

CREATE TABLE ecom_settlement_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id     BIGINT         NOT NULL COMMENT '结算单',
    posted_date       DATE                    DEFAULT NULL COMMENT '入账日期',
    item_type         VARCHAR(16)    NOT NULL COMMENT '类型(字典 ecom_settlement_item_type) ORDER订单收入 REFUND退款 COMMISSION佣金 FULFILLMENT配送费 STORAGE仓储费 AD广告费 OTHER其他 ADJUSTMENT调整',
    platform_order_no VARCHAR(64)             DEFAULT NULL COMMENT '平台订单号',
    seller_sku        VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU',
    amount            DECIMAL(18, 2) NOT NULL COMMENT '金额：收入为正、费用为负',
    description       VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_settlement_item_settlement (settlement_id),
    KEY idx_ecom_settlement_item_order (platform_order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台结算明细';

CREATE TABLE ecom_ad_daily (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id         BIGINT         NOT NULL COMMENT '店铺',
    ad_date          DATE           NOT NULL COMMENT '日期',
    campaign         VARCHAR(128)   NOT NULL DEFAULT '' COMMENT '广告活动',
    seller_sku       VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '平台 SKU',
    platform_item_id VARCHAR(64)             DEFAULT NULL COMMENT '平台商品 ID',
    impressions      BIGINT         NOT NULL DEFAULT 0 COMMENT '曝光',
    clicks           BIGINT         NOT NULL DEFAULT 0 COMMENT '点击',
    spend            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '花费（店铺币种）',
    currency         VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    ad_sales         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '广告销售额',
    ad_orders        INT            NOT NULL DEFAULT 0 COMMENT '广告订单数',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_ad_daily (store_id, ad_date, campaign, seller_sku, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告日报';

CREATE TABLE ecom_return (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no            VARCHAR(32)    NOT NULL COMMENT '单据号',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    order_id          BIGINT         NOT NULL COMMENT '平台订单',
    platform_order_no VARCHAR(64)    NOT NULL COMMENT '平台订单号',
    return_type       VARCHAR(16)    NOT NULL COMMENT '类型(字典 ecom_return_type) REFUND_ONLY仅退款 RETURN_REFUND退货退款',
    reason            VARCHAR(32)             DEFAULT NULL COMMENT '退货原因(业务字典 ecom_return_reason)',
    currency          VARCHAR(8)     NOT NULL COMMENT '币种',
    exchange_rate     DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（取订单）',
    refund_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '退款金额（订单币种）',
    warehouse_id      BIGINT                  DEFAULT NULL COMMENT '退回仓库',
    status            VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 ecom_return_status) PENDING待处理 RECEIVED已入库 REFUNDED已退款 CLOSED已关闭',
    trade_doc_id      BIGINT                  DEFAULT NULL COMMENT '生成的销售退货单(scm_trade_doc)',
    trade_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '销售退货单号',
    refund_time       DATETIME                DEFAULT NULL COMMENT '退款时间',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id           BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by         BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_return_no (doc_no, tenant_id),
    KEY idx_ecom_return_order (order_id),
    KEY idx_ecom_return_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退货退款';

CREATE TABLE ecom_return_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    return_id     BIGINT         NOT NULL COMMENT '退货退款单',
    order_item_id BIGINT         NOT NULL COMMENT '订单明细',
    material_id   BIGINT                  DEFAULT NULL COMMENT '物料',
    seller_sku    VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '退回数量（平台 SKU 件数）',
    refund_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本行退款金额',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_return_item_return (return_id),
    KEY idx_ecom_return_item_order_item (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退货退款明细';

CREATE TABLE ecom_platform_stock (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    seller_sku        VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    platform_item_id  VARCHAR(64)             DEFAULT NULL COMMENT '平台商品 ID',
    fulfillable_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '可售',
    inbound_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '入库中',
    reserved_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '预留',
    unfulfillable_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '不可售',
    snapshot_date     DATE           NOT NULL COMMENT '快照日期',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_platform_stock (store_id, seller_sku, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台仓库存快照（平台库存报告）';

INSERT INTO sys_tenant (id, name, admin_user_id, status, remark, create_time, update_time) VALUES
(1, '柠檬科技', 1, 1, '平台租户（运营方），不受套餐限制', NOW(), NOW());

INSERT INTO sys_dept (id, parent_id, name, sort, leader_user_id, status, create_time, update_time) VALUES
(1,   0,  '柠檬科技', 1, 1,    1, NOW(), NOW()),
(10,  1,  '研发部',   1, NULL, 1, NOW(), NOW()),
(11,  1,  '市场部',   2, NULL, 1, NOW(), NOW()),
(12,  1,  '财务部',   3, NULL, 1, NOW(), NOW()),
(100, 10, '前端组',   1, NULL, 1, NOW(), NOW()),
(101, 10, '后端组',   2, NULL, 1, NOW(), NOW());

INSERT INTO sys_user (id, dept_id, username, password, nickname, status, create_time, update_time) VALUES
(1, 1,  'admin', '$2a$10$1KjM59xYPcOlGbBaXNIRGOKMOubiCF1YaqU7b47kEXtfe159apzGW', '超级管理员', 1, NOW(), NOW()),
(2, 10, 'test',  '$2a$10$1KjM59xYPcOlGbBaXNIRGOKMOubiCF1YaqU7b47kEXtfe159apzGW', '测试用户', 1, NOW(), NOW());

INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(1, '超级管理员', 'admin', 1, 1, 1, '拥有全部权限', NOW(), NOW()),
(2, '普通用户', 'common', 2, 4, 1, '只读权限，只能查看本部门及以下的数据', NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1,   0, '系统管理', 'M', '/system', NULL, NULL, 'SettingOutlined', 10, 1, 1, NOW(), NOW()),
(100, 1, '用户管理', 'C', 'user', 'system/user/index', 'sys:user:list', 'UserOutlined', 1, 1, 1, NOW(), NOW()),
(104, 1, '部门管理', 'C', 'dept', 'system/dept/index', 'sys:dept:list', 'ApartmentOutlined', 2, 1, 1, NOW(), NOW()),
(101, 1, '角色管理', 'C', 'role', 'system/role/index', 'sys:role:list', 'TeamOutlined', 3, 1, 1, NOW(), NOW()),
(102, 1, '菜单管理', 'C', 'menu', 'system/menu/index', 'sys:menu:list', 'MenuOutlined', 4, 1, 1, NOW(), NOW()),
(103, 1, '字典管理', 'C', 'dict', 'system/dict/index', 'sys:dict:list', 'BookOutlined', 5, 1, 1, NOW(), NOW()),
(105, 1, '租户管理', 'C', 'tenant', 'system/tenant/index', 'sys:tenant:list', 'ShopOutlined', 6, 1, 1, NOW(), NOW()),
(106, 1, '租户套餐', 'C', 'tenant-package', 'system/tenant-package/index', 'sys:tenantPackage:list', 'GiftOutlined', 7, 1, 1, NOW(), NOW()),
(107, 1, '编码规则', 'C', 'code-rule', 'system/code-rule/index', 'sys:codeRule:list', 'NumberOutlined', 8, 1, 1, NOW(), NOW()),
(108, 1, '打印模板', 'C', 'print-template', 'system/print-template/index', 'sys:printTemplate:list', 'PrinterOutlined', 9, 1, 1, NOW(), NOW()),
(109, 1, '参数设置', 'C', 'config', 'system/config/index', 'sys:config:list', 'ControlOutlined', 10, 1, 1, NOW(), NOW()),
(1000, 100, '用户新增', 'F', NULL, NULL, 'sys:user:add', NULL, 1, 1, 1, NOW(), NOW()),
(1001, 100, '用户修改', 'F', NULL, NULL, 'sys:user:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1002, 100, '用户删除', 'F', NULL, NULL, 'sys:user:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1003, 100, '重置密码', 'F', NULL, NULL, 'sys:user:resetPwd', NULL, 4, 1, 1, NOW(), NOW()),
(1010, 101, '角色新增', 'F', NULL, NULL, 'sys:role:add', NULL, 1, 1, 1, NOW(), NOW()),
(1011, 101, '角色修改', 'F', NULL, NULL, 'sys:role:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1012, 101, '角色删除', 'F', NULL, NULL, 'sys:role:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1013, 101, '数据权限', 'F', NULL, NULL, 'sys:role:dataScope', NULL, 4, 1, 1, NOW(), NOW()),
(1014, 101, '分配用户', 'F', NULL, NULL, 'sys:role:assignUser', NULL, 5, 1, 1, NOW(), NOW()),
(1020, 102, '菜单新增', 'F', NULL, NULL, 'sys:menu:add', NULL, 1, 1, 1, NOW(), NOW()),
(1021, 102, '菜单修改', 'F', NULL, NULL, 'sys:menu:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1022, 102, '菜单删除', 'F', NULL, NULL, 'sys:menu:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1030, 103, '字典新增', 'F', NULL, NULL, 'sys:dict:add', NULL, 1, 1, 1, NOW(), NOW()),
(1031, 103, '字典修改', 'F', NULL, NULL, 'sys:dict:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1032, 103, '字典删除', 'F', NULL, NULL, 'sys:dict:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1040, 104, '部门新增', 'F', NULL, NULL, 'sys:dept:add', NULL, 1, 1, 1, NOW(), NOW()),
(1041, 104, '部门修改', 'F', NULL, NULL, 'sys:dept:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1042, 104, '部门删除', 'F', NULL, NULL, 'sys:dept:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1050, 105, '租户新增', 'F', NULL, NULL, 'sys:tenant:add', NULL, 1, 1, 1, NOW(), NOW()),
(1051, 105, '租户修改', 'F', NULL, NULL, 'sys:tenant:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1052, 105, '租户删除', 'F', NULL, NULL, 'sys:tenant:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1060, 106, '套餐新增', 'F', NULL, NULL, 'sys:tenantPackage:add', NULL, 1, 1, 1, NOW(), NOW()),
(1061, 106, '套餐修改', 'F', NULL, NULL, 'sys:tenantPackage:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1062, 106, '套餐删除', 'F', NULL, NULL, 'sys:tenantPackage:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1070, 107, '规则修改', 'F', NULL, NULL, 'sys:codeRule:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1080, 108, '模板修改', 'F', NULL, NULL, 'sys:printTemplate:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1090, 109, '参数修改', 'F', NULL, NULL, 'sys:config:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2,   0, '系统监控', 'M', '/monitor', NULL, NULL, 'FundProjectionScreenOutlined', 11, 1, 1, NOW(), NOW()),
(110, 2, '在线用户', 'C', 'online', 'monitor/online/index', 'monitor:online:list', 'UsergroupAddOutlined', 1, 1, 1, NOW(), NOW()),
(111, 2, '操作日志', 'C', 'operlog', 'monitor/operlog/index', 'monitor:operlog:list', 'FileTextOutlined', 2, 1, 1, NOW(), NOW()),
(112, 2, '登录日志', 'C', 'loginlog', 'monitor/loginlog/index', 'monitor:loginlog:list', 'LoginOutlined', 3, 1, 1, NOW(), NOW()),
(113, 2, '服务监控', 'C', 'server', 'monitor/server/index', 'monitor:server:list', 'CloudServerOutlined', 4, 1, 1, NOW(), NOW()),
(114, 2, '缓存监控', 'C', 'cache', 'monitor/cache/index', 'monitor:cache:list', 'DatabaseOutlined', 5, 1, 1, NOW(), NOW()),
(115, 2, '定时任务', 'C', 'job', 'monitor/job/index', 'monitor:job:list', 'FieldTimeOutlined', 6, 1, 1, NOW(), NOW()),
(1100, 110, '强制退出', 'F', NULL, NULL, 'monitor:online:kick', NULL, 1, 1, 1, NOW(), NOW()),
(1110, 111, '日志删除', 'F', NULL, NULL, 'monitor:operlog:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1120, 112, '日志删除', 'F', NULL, NULL, 'monitor:loginlog:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1140, 114, '清除缓存', 'F', NULL, NULL, 'monitor:cache:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1150, 115, '任务修改', 'F', NULL, NULL, 'monitor:job:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1151, 115, '立即执行', 'F', NULL, NULL, 'monitor:job:run', NULL, 2, 1, 1, NOW(), NOW()),
(3,   0, '流程中心', 'M', '/workflow', NULL, NULL, 'NodeIndexOutlined', 9, 1, 1, NOW(), NOW()),
(120, 961, '发起申请', 'C', 'start', 'workflow/start/index', 'wf:start:list', 'SendOutlined', 2, 1, 1, NOW(), NOW()),
(121, 961, '我的待办', 'C', 'todo', 'workflow/todo/index', 'wf:task:todo', 'ScheduleOutlined', 1, 1, 1, NOW(), NOW()),
(122, 961, '我的已办', 'C', 'done', 'workflow/done/index', 'wf:task:done', 'CheckSquareOutlined', 4, 1, 1, NOW(), NOW()),
(123, 961, '我的申请', 'C', 'mine', 'workflow/mine/index', 'wf:instance:mine', 'SolutionOutlined', 3, 1, 1, NOW(), NOW()),
(124, 961, '抄送我的', 'C', 'copy', 'workflow/copy/index', 'wf:copy:list', 'CopyOutlined', 5, 1, 1, NOW(), NOW()),
(125, 962, '流程模型', 'C', 'model', 'workflow/model/index', 'wf:model:list', 'PartitionOutlined', 1, 1, 1, NOW(), NOW()),
(126, 962, '流程实例', 'C', 'instance', 'workflow/instance/index', 'wf:instance:list', 'DeploymentUnitOutlined', 2, 1, 1, NOW(), NOW()),
(1250, 125, '模型新增', 'F', NULL, NULL, 'wf:model:add', NULL, 1, 1, 1, NOW(), NOW()),
(1251, 125, '模型修改', 'F', NULL, NULL, 'wf:model:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1252, 125, '模型发布', 'F', NULL, NULL, 'wf:model:deploy', NULL, 3, 1, 1, NOW(), NOW()),
(1253, 125, '模型删除', 'F', NULL, NULL, 'wf:model:remove', NULL, 4, 1, 1, NOW(), NOW()),
(1260, 126, '终止流程', 'F', NULL, NULL, 'wf:instance:cancel', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(1, '通用状态', 'sys_common_status', 1, '启用/停用', NOW(), NOW()),
(2, '显示状态', 'sys_show_hide', 1, '菜单显示/隐藏', NOW(), NOW()),
(3, '菜单类型', 'sys_menu_type', 1, '目录/菜单/按钮', NOW(), NOW()),
(4, '操作类型', 'sys_oper_type', 1, '操作日志的操作类型', NOW(), NOW()),
(5, '成功失败', 'sys_success_fail', 1, '日志结果', NOW(), NOW()),
(6, '数据权限', 'sys_data_scope', 1, '角色的数据权限范围', NOW(), NOW()),
(7, '流程分类', 'wf_category', 1, '流程模型的分类', NOW(), NOW()),
(8, '流程状态', 'wf_status', 1, '流程实例的状态', NOW(), NOW()),
(9, '审批动作', 'wf_task_action', 1, '审批记录中的处理结果', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_common_status', '启用', '1', 'green', 1, 1, NOW(), NOW()),
('sys_common_status', '停用', '0', 'red', 2, 1, NOW(), NOW()),
('sys_show_hide', '显示', '1', 'blue', 1, 1, NOW(), NOW()),
('sys_show_hide', '隐藏', '0', 'default', 2, 1, NOW(), NOW()),
('sys_menu_type', '目录', 'M', 'blue', 1, 1, NOW(), NOW()),
('sys_menu_type', '菜单', 'C', 'green', 2, 1, NOW(), NOW()),
('sys_menu_type', '按钮', 'F', 'orange', 3, 1, NOW(), NOW()),
('sys_oper_type', '其他', '0', 'default', 0, 1, NOW(), NOW()),
('sys_oper_type', '新增', '1', 'green', 1, 1, NOW(), NOW()),
('sys_oper_type', '修改', '2', 'blue', 2, 1, NOW(), NOW()),
('sys_oper_type', '删除', '3', 'red', 3, 1, NOW(), NOW()),
('sys_oper_type', '授权', '4', 'purple', 4, 1, NOW(), NOW()),
('sys_oper_type', '导出', '5', 'cyan', 5, 1, NOW(), NOW()),
('sys_oper_type', '强退', '6', 'orange', 6, 1, NOW(), NOW()),
('sys_oper_type', '清空', '7', 'red', 7, 1, NOW(), NOW()),
('sys_oper_type', '导入', '8', 'geekblue', 8, 1, NOW(), NOW()),
('sys_success_fail', '成功', '1', 'green', 1, 1, NOW(), NOW()),
('sys_success_fail', '失败', '0', 'red', 2, 1, NOW(), NOW()),
('sys_data_scope', '全部数据', '1', 'red', 1, 1, NOW(), NOW()),
('sys_data_scope', '自定义数据', '2', 'purple', 2, 1, NOW(), NOW()),
('sys_data_scope', '本部门数据', '3', 'blue', 3, 1, NOW(), NOW()),
('sys_data_scope', '本部门及以下数据', '4', 'cyan', 4, 1, NOW(), NOW()),
('sys_data_scope', '仅本人数据', '5', 'default', 5, 1, NOW(), NOW()),
('wf_category', '采购', 'PURCHASE', 'blue', 1, 1, NOW(), NOW()),
('wf_category', '销售', 'SALES', 'green', 2, 1, NOW(), NOW()),
('wf_category', '库存', 'INVENTORY', 'cyan', 3, 1, NOW(), NOW()),
('wf_category', '财务', 'FINANCE', 'gold', 4, 1, NOW(), NOW()),
('wf_category', '其他', 'OTHER', 'default', 5, 1, NOW(), NOW()),
('wf_status', '审批中', 'RUNNING', 'processing', 1, 1, NOW(), NOW()),
('wf_status', '已通过', 'APPROVED', 'green', 2, 1, NOW(), NOW()),
('wf_status', '已拒绝', 'REJECTED', 'red', 3, 1, NOW(), NOW()),
('wf_status', '已撤销', 'CANCELED', 'default', 4, 1, NOW(), NOW()),
('wf_task_action', '同意', 'APPROVE', 'green', 1, 1, NOW(), NOW()),
('wf_task_action', '拒绝', 'REJECT', 'red', 2, 1, NOW(), NOW()),
('wf_task_action', '转交', 'TRANSFER', 'blue', 3, 1, NOW(), NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- 普通用户：只能查看各管理页面，没有任何按钮权限；可以使用流程中心发起、审批自己的流程
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 100), (2, 104), (2, 101), (2, 102), (2, 103), (2, 109),
(2, 2), (2, 110), (2, 111), (2, 112), (2, 113), (2, 114), (2, 115),
(2, 3), (2, 120), (2, 121), (2, 122), (2, 123), (2, 124), (2, 125), (2, 126);

-- 打印格式示例：销售出库单除标准格式（默认）外预置「发货单（不含价格）」，给同行的发货单、代发第三方时只显示数量，
-- 标题为「发货单」，隐藏单价、税率、金额列和金额类表头字段。新租户由 SysPrintTenantInitializer 从平台复制
INSERT INTO sys_print_template (template_key, name, is_default, sort, status, config, create_time, update_time) VALUES
('scm.salesOut', '标准格式', 1, 0, 1, '{}', NOW(), NOW()),
('scm.salesOut', '发货单（不含价格）', 0, 1, 1, '{"title":"发货单","orientation":"portrait","paper":"A4","margin":10,"fontSize":10,"showCompany":true,"showNote":true,"fields":[{"label":"订单号"},{"label":"客户"},{"label":"仓库"},{"label":"业务日期"},{"label":"仓库单据"},{"label":"发票类型","hidden":true},{"label":"优惠前合计","hidden":true},{"label":"整单优惠","hidden":true},{"label":"原单优惠分摊","hidden":true},{"label":"优惠后合计","hidden":true},{"label":"价税合计","hidden":true},{"label":"运费","hidden":true},{"label":"运费承担","hidden":true},{"label":"含代垫运费合计","hidden":true},{"label":"原币","hidden":true}],"columns":[{"title":"序号"},{"title":"物料编码"},{"title":"物料名称"},{"title":"规格型号"},{"title":"单位"},{"title":"仓库"},{"title":"库位"},{"title":"批次"},{"title":"数量"},{"title":"含税单价","hidden":true},{"title":"税率","hidden":true},{"title":"价税合计","hidden":true},{"title":"序列号"}],"signs":[{"label":"制单"},{"label":"仓管"},{"label":"客户签收"}]}', NOW(), NOW());

-- ---------------------------------------------------------------- 仓库管理：菜单、字典、示例数据

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(4,   0, '仓库管理', 'M', '/wms', NULL, NULL, 'ShopOutlined', 3, 1, 1, NOW(), NOW()),
(130, 911, '入库单', 'C', 'in', 'wms/in/index', 'wms:in:list', 'ImportOutlined', 1, 1, 1, NOW(), NOW()),
(131, 911, '出库单', 'C', 'out', 'wms/out/index', 'wms:out:list', 'ExportOutlined', 2, 1, 1, NOW(), NOW()),
(132, 911, '调拨单', 'C', 'transfer', 'wms/transfer/index', 'wms:transfer:list', 'SwapOutlined', 3, 1, 1, NOW(), NOW()),
(133, 911, '盘点单', 'C', 'check', 'wms/check/index', 'wms:check:list', 'AuditOutlined', 4, 1, 1, NOW(), NOW()),
(134, 912, '库存查询', 'C', 'stock', 'wms/stock/index', 'wms:stock:list', 'DatabaseOutlined', 1, 1, 1, NOW(), NOW()),
(135, 912, '库存流水', 'C', 'flow', 'wms/flow/index', 'wms:flow:list', 'HistoryOutlined', 2, 1, 1, NOW(), NOW()),
(136, 912, '批次与序列号', 'C', 'trace', 'wms/trace/index', 'wms:trace:list', 'BarcodeOutlined', 3, 1, 1, NOW(), NOW()),
(137, 914, '物料管理', 'C', 'material', 'wms/material/index', 'wms:material:list', 'AppstoreOutlined', 1, 1, 1, NOW(), NOW()),
(138, 914, '仓库管理', 'C', 'warehouse', 'wms/warehouse/index', 'wms:warehouse:list', 'HomeOutlined', 2, 1, 1, NOW(), NOW()),
(139, 914, '库位管理', 'C', 'location', 'wms/location/index', 'wms:location:list', 'EnvironmentOutlined', 3, 1, 1, NOW(), NOW()),
(266, 913, '库存冻结', 'C', 'hold', 'wms/hold/index', 'wms:hold:list', 'LockOutlined', 2, 1, 1, NOW(), NOW()),
(2660, 266, '冻结放行', 'F', NULL, NULL, 'wms:hold:release', NULL, 1, 1, 1, NOW(), NOW()),
(1300, 130, '入库单新增', 'F', NULL, NULL, 'wms:in:add', NULL, 1, 1, 1, NOW(), NOW()),
(1301, 130, '入库单修改', 'F', NULL, NULL, 'wms:in:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1302, 130, '入库单删除', 'F', NULL, NULL, 'wms:in:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1303, 130, '入库确认', 'F', NULL, NULL, 'wms:in:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1310, 131, '出库单新增', 'F', NULL, NULL, 'wms:out:add', NULL, 1, 1, 1, NOW(), NOW()),
(1311, 131, '出库单修改', 'F', NULL, NULL, 'wms:out:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1312, 131, '出库单删除', 'F', NULL, NULL, 'wms:out:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1313, 131, '出库确认', 'F', NULL, NULL, 'wms:out:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1320, 132, '调拨单新增', 'F', NULL, NULL, 'wms:transfer:add', NULL, 1, 1, 1, NOW(), NOW()),
(1321, 132, '调拨单修改', 'F', NULL, NULL, 'wms:transfer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1322, 132, '调拨单删除', 'F', NULL, NULL, 'wms:transfer:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1323, 132, '调拨确认', 'F', NULL, NULL, 'wms:transfer:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1330, 133, '盘点单新增', 'F', NULL, NULL, 'wms:check:add', NULL, 1, 1, 1, NOW(), NOW()),
(1331, 133, '盘点单修改', 'F', NULL, NULL, 'wms:check:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1332, 133, '盘点单删除', 'F', NULL, NULL, 'wms:check:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1333, 133, '盘点确认', 'F', NULL, NULL, 'wms:check:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1370, 137, '物料新增', 'F', NULL, NULL, 'wms:material:add', NULL, 1, 1, 1, NOW(), NOW()),
(1371, 137, '物料修改', 'F', NULL, NULL, 'wms:material:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1372, 137, '物料删除', 'F', NULL, NULL, 'wms:material:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1380, 138, '仓库新增', 'F', NULL, NULL, 'wms:warehouse:add', NULL, 1, 1, 1, NOW(), NOW()),
(1381, 138, '仓库修改', 'F', NULL, NULL, 'wms:warehouse:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1382, 138, '仓库删除', 'F', NULL, NULL, 'wms:warehouse:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1390, 139, '库位新增', 'F', NULL, NULL, 'wms:location:add', NULL, 1, 1, 1, NOW(), NOW()),
(1391, 139, '库位修改', 'F', NULL, NULL, 'wms:location:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1392, 139, '库位删除', 'F', NULL, NULL, 'wms:location:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(11, '入库类型', 'wms_in_type', 1, '入库单的业务类型', NOW(), NOW()),
(12, '出库类型', 'wms_out_type', 1, '出库单的业务类型', NOW(), NOW()),
(13, '单据状态', 'wms_doc_status', 1, '仓库单据的状态', NOW(), NOW()),
(14, '库存业务类型', 'wms_biz_type', 1, '库存流水的业务类型', NOW(), NOW()),
(15, '序列号状态', 'wms_serial_status', 1, '序列号是否在库', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_type', '采购入库', 'PURCHASE', 'blue', 1, 1, NOW(), NOW()),
('wms_in_type', '销售退货', 'SALES_RETURN', 'orange', 3, 1, NOW(), NOW()),
('wms_in_type', '其他入库', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('wms_out_type', '销售出库', 'SALES', 'green', 1, 1, NOW(), NOW()),
('wms_out_type', '采购退货', 'PURCHASE_RETURN', 'orange', 3, 1, NOW(), NOW()),
('wms_out_type', '其他出库', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('wms_doc_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('wms_doc_status', '已完成', 'DONE', 'green', 2, 1, NOW(), NOW()),
('wms_biz_type', '入库', 'IN', 'green', 1, 1, NOW(), NOW()),
('wms_biz_type', '出库', 'OUT', 'orange', 2, 1, NOW(), NOW()),
('wms_biz_type', '调拨', 'TRANSFER', 'blue', 3, 1, NOW(), NOW()),
('wms_biz_type', '盘点', 'CHECK', 'purple', 4, 1, NOW(), NOW()),
('wms_serial_status', '在库', 'IN_STOCK', 'green', 1, 1, NOW(), NOW()),
('wms_serial_status', '已出库', 'OUT', 'default', 2, 1, NOW(), NOW());

-- 普通用户：仓库管理各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 4), (2, 130), (2, 131), (2, 132), (2, 133), (2, 134), (2, 135), (2, 136), (2, 137), (2, 138), (2, 139), (2, 266);

-- 示例基础资料（没有库存，库存只能通过单据产生）
INSERT INTO wms_warehouse (id, code, name, address, contact, sort, status, create_time, update_time) VALUES
(1, 'WH01', '总部成品仓', '上海市浦东新区 1 号库', '张三', 1, 1, NOW(), NOW()),
(2, 'WH02', '原材料仓', '上海市浦东新区 2 号库', '李四', 2, 1, NOW(), NOW());

INSERT INTO wms_location (warehouse_id, code, name, sort, status, create_time, update_time) VALUES
(1, 'A-01', 'A 区 1 号货架', 1, 1, NOW(), NOW()),
(1, 'A-02', 'A 区 2 号货架', 2, 1, NOW(), NOW()),
(2, 'R-01', '原料区 1 排', 1, 1, NOW(), NOW());

INSERT INTO wms_material (code, name, spec, unit, category, batch_enabled, serial_enabled, safety_stock, status, create_time, update_time) VALUES
('M0001', '柠檬原浆', '5L/桶', '桶', 'RAW', 1, 0, 20, 1, NOW(), NOW()),
('M0002', '包装纸箱', '40×30×20cm', '个', 'PACKING', 0, 0, 100, 1, NOW(), NOW()),
('P0001', '智能榨汁机', 'LJ-200', '台', 'FINISHED', 0, 1, 5, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 供应链管理：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(5,   0, '供应链', 'M', '/scm', NULL, NULL, 'DeploymentUnitOutlined', 2, 1, 1, NOW(), NOW()),
(140, 901, '采购订单', 'C', 'purchase', 'scm/purchase/index', 'scm:purchase:list', 'ShoppingCartOutlined', 3, 1, 1, NOW(), NOW()),
(141, 901, '采购入库', 'C', 'purchase-in', 'scm/purchaseIn/index', 'scm:purchaseIn:list', 'ImportOutlined', 4, 1, 1, NOW(), NOW()),
(142, 901, '采购退货', 'C', 'purchase-return', 'scm/purchaseReturn/index', 'scm:purchaseReturn:list', 'RollbackOutlined', 5, 1, 1, NOW(), NOW()),
(143, 902, '销售订单', 'C', 'sales', 'scm/sales/index', 'scm:sales:list', 'ShoppingOutlined', 3, 1, 1, NOW(), NOW()),
(144, 902, '销售出库', 'C', 'sales-out', 'scm/salesOut/index', 'scm:salesOut:list', 'ExportOutlined', 5, 1, 1, NOW(), NOW()),
(145, 902, '销售退货', 'C', 'sales-return', 'scm/salesReturn/index', 'scm:salesReturn:list', 'UndoOutlined', 6, 1, 1, NOW(), NOW()),
(147, 903, '供应商', 'C', 'supplier', 'scm/supplier/index', 'scm:supplier:list', 'BankOutlined', 1, 1, 1, NOW(), NOW()),
(149, 903, '客户', 'C', 'customer', 'scm/customer/index', 'scm:customer:list', 'ContactsOutlined', 3, 1, 1, NOW(), NOW()),
(1400, 140, '采购订单新增', 'F', NULL, NULL, 'scm:purchase:add', NULL, 1, 1, 1, NOW(), NOW()),
(1401, 140, '采购订单修改', 'F', NULL, NULL, 'scm:purchase:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1402, 140, '采购订单删除', 'F', NULL, NULL, 'scm:purchase:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1403, 140, '采购订单审核', 'F', NULL, NULL, 'scm:purchase:approve', NULL, 4, 1, 1, NOW(), NOW()),
(1404, 140, '采购订单关闭', 'F', NULL, NULL, 'scm:purchase:close', NULL, 5, 1, 1, NOW(), NOW()),
(1410, 141, '采购入库', 'F', NULL, NULL, 'scm:purchaseIn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1430, 143, '销售订单新增', 'F', NULL, NULL, 'scm:sales:add', NULL, 1, 1, 1, NOW(), NOW()),
(1431, 143, '销售订单修改', 'F', NULL, NULL, 'scm:sales:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1432, 143, '销售订单删除', 'F', NULL, NULL, 'scm:sales:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1433, 143, '销售订单审核', 'F', NULL, NULL, 'scm:sales:approve', NULL, 4, 1, 1, NOW(), NOW()),
(1434, 143, '销售订单关闭', 'F', NULL, NULL, 'scm:sales:close', NULL, 5, 1, 1, NOW(), NOW()),
(1436, 143, '销售订单变更', 'F', NULL, NULL, 'scm:sales:change', NULL, 7, 1, 1, NOW(), NOW()),
(1420, 142, '采购退货', 'F', NULL, NULL, 'scm:purchaseReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1440, 144, '销售出库', 'F', NULL, NULL, 'scm:salesOut:add', NULL, 1, 1, 1, NOW(), NOW()),
(1450, 145, '销售退货', 'F', NULL, NULL, 'scm:salesReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1470, 147, '供应商新增', 'F', NULL, NULL, 'scm:supplier:add', NULL, 1, 1, 1, NOW(), NOW()),
(1471, 147, '供应商修改', 'F', NULL, NULL, 'scm:supplier:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1472, 147, '供应商删除', 'F', NULL, NULL, 'scm:supplier:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1490, 149, '客户新增', 'F', NULL, NULL, 'scm:customer:add', NULL, 1, 1, 1, NOW(), NOW()),
(1491, 149, '客户修改', 'F', NULL, NULL, 'scm:customer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1492, 149, '客户删除', 'F', NULL, NULL, 'scm:customer:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(16, '供应商等级', 'scm_supplier_level', 1, '供应商评估的评定等级', NOW(), NOW()),
(17, '客户等级', 'scm_customer_level', 1, '客户分级', NOW(), NOW()),
(18, '订单状态', 'scm_order_status', 1, '采购、销售订单的状态', NOW(), NOW()),
(19, '出入库种类', 'scm_trade_kind', 1, '采购入库 / 退货、销售出库 / 退货', NOW(), NOW()),
(20, '发运状态', 'scm_shipment_status', 1, '物流发运单的状态', NOW(), NOW()),
(21, '承运商', 'scm_carrier', 1, '物流公司', NOW(), NOW()),
(22, '订单来源', 'scm_order_source', 1, '手工录入或 MRP 生成', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_supplier_level', 'A 优秀', 'A', 'green', 1, 1, NOW(), NOW()),
('scm_supplier_level', 'B 良好', 'B', 'blue', 2, 1, NOW(), NOW()),
('scm_supplier_level', 'C 合格', 'C', 'orange', 3, 1, NOW(), NOW()),
('scm_supplier_level', 'D 待改进', 'D', 'red', 4, 1, NOW(), NOW()),
('scm_customer_level', '重点客户', 'VIP', 'gold', 1, 1, NOW(), NOW()),
('scm_customer_level', '普通客户', 'NORMAL', 'blue', 2, 1, NOW(), NOW()),
('scm_customer_level', '潜在客户', 'POTENTIAL', 'default', 3, 1, NOW(), NOW()),
('scm_order_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('scm_order_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_order_status', '已审核', 'APPROVED', 'blue', 3, 1, NOW(), NOW()),
('scm_order_status', '部分完成', 'PARTIAL', 'cyan', 4, 1, NOW(), NOW()),
('scm_order_status', '已完成', 'COMPLETED', 'green', 5, 1, NOW(), NOW()),
('scm_order_status', '已关闭', 'CLOSED', 'default', 6, 1, NOW(), NOW()),
('scm_trade_kind', '采购入库', 'PURCHASE_IN', 'blue', 1, 1, NOW(), NOW()),
('scm_trade_kind', '采购退货', 'PURCHASE_RETURN', 'orange', 2, 1, NOW(), NOW()),
('scm_trade_kind', '销售出库', 'SALES_OUT', 'green', 3, 1, NOW(), NOW()),
('scm_trade_kind', '销售退货', 'SALES_RETURN', 'red', 4, 1, NOW(), NOW()),
('scm_shipment_status', '待发运', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('scm_shipment_status', '运输中', 'SHIPPED', 'processing', 2, 1, NOW(), NOW()),
('scm_shipment_status', '已签收', 'SIGNED', 'green', 3, 1, NOW(), NOW()),
('scm_shipment_status', '异常', 'EXCEPTION', 'red', 4, 1, NOW(), NOW()),
('scm_carrier', '顺丰速运', 'SF', 'default', 1, 1, NOW(), NOW()),
('scm_carrier', '京东物流', 'JD', 'default', 2, 1, NOW(), NOW()),
('scm_carrier', '中通快递', 'ZTO', 'default', 3, 1, NOW(), NOW()),
('scm_carrier', '德邦物流', 'DEPPON', 'default', 4, 1, NOW(), NOW()),
('scm_carrier', '自提 / 自送', 'SELF', 'default', 5, 1, NOW(), NOW()),
('scm_order_source', '手工', 'MANUAL', 'default', 1, 1, NOW(), NOW());

-- 普通用户：供应链各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 5), (2, 140), (2, 141), (2, 142), (2, 144), (2, 145), (2, 147), (2, 149);

-- 示例往来单位
INSERT INTO scm_supplier (id, code, name, level, contact, phone, address, payment_days, status, create_time, update_time) VALUES
(1, 'S001', '上海鲜果供应链有限公司', 'A', '王经理', '021-55550001', '上海市闵行区', 30, 1, NOW(), NOW()),
(2, 'S002', '华东包装材料厂', 'B', '赵经理', '0571-55550002', '浙江省杭州市', 60, 1, NOW(), NOW());

INSERT INTO scm_customer (id, code, name, level, contact, phone, address, credit_limit, payment_days, status, create_time, update_time) VALUES
(1, 'C001', '鲜饮连锁（上海）有限公司', 'VIP', '陈店长', '021-66660001', '上海市黄浦区南京东路 100 号', 500000, 30, 1, NOW(), NOW()),
(2, 'C002', '优品商贸有限公司', 'NORMAL', '孙经理', '010-66660002', '北京市朝阳区建国路 88 号', 100000, 15, 1, NOW(), NOW());

INSERT INTO scm_supplier_price (supplier_id, material_id, price, tax_rate, lead_days, min_qty, is_default, create_time, update_time) VALUES
(1, 1, 120, 13, 3, 10, 1, NOW(), NOW()),
(2, 2, 2.5, 13, 5, 500, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 财务管理：菜单、字典、会计科目

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(7,   0, '财务管理', 'M', '/fin', NULL, NULL, 'AccountBookOutlined', 4, 1, 1, NOW(), NOW()),
(180, 931, '凭证管理', 'C', 'voucher', 'fin/voucher/index', 'fin:voucher:list', 'FileTextOutlined', 1, 1, 1, NOW(), NOW()),
(181, 931, '账簿查询', 'C', 'book', 'fin/book/index', 'fin:book:list', 'ReadOutlined', 2, 1, 1, NOW(), NOW()),
(182, 931, '财务报表', 'C', 'report', 'fin/report/index', 'fin:report:list', 'PieChartOutlined', 3, 1, 1, NOW(), NOW()),
(183, 932, '应收管理', 'C', 'ar', 'fin/ar/index', 'fin:ar:list', 'RiseOutlined', 1, 1, 1, NOW(), NOW()),
(184, 932, '应付管理', 'C', 'ap', 'fin/ap/index', 'fin:ap:list', 'FallOutlined', 2, 1, 1, NOW(), NOW()),
(185, 932, '收款单', 'C', 'receipt', 'fin/receipt/index', 'fin:receipt:list', 'TransactionOutlined', 3, 1, 1, NOW(), NOW()),
(186, 932, '付款单', 'C', 'payment', 'fin/payment/index', 'fin:payment:list', 'PayCircleOutlined', 4, 1, 1, NOW(), NOW()),
(187, 934, '固定资产', 'C', 'asset', 'fin/asset/index', 'fin:asset:list', 'BuildOutlined', 1, 1, 1, NOW(), NOW()),
(188, 934, '成本核算', 'C', 'cost', 'fin/cost/index', 'fin:cost:list', 'FundOutlined', 2, 1, 1, NOW(), NOW()),
(189, 935, '预算管理', 'C', 'budget', 'fin/budget/index', 'fin:budget:list', 'AimOutlined', 1, 1, 1, NOW(), NOW()),
(190, 936, '会计科目', 'C', 'account', 'fin/account/index', 'fin:account:list', 'OrderedListOutlined', 2, 1, 1, NOW(), NOW()),
(191, 936, '期末结账', 'C', 'period', 'fin/period/index', 'fin:period:list', 'LockOutlined', 1, 1, 1, NOW(), NOW()),
(1800, 180, '凭证新增', 'F', NULL, NULL, 'fin:voucher:add', NULL, 1, 1, 1, NOW(), NOW()),
(1801, 180, '凭证修改', 'F', NULL, NULL, 'fin:voucher:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1802, 180, '凭证删除', 'F', NULL, NULL, 'fin:voucher:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1803, 180, '凭证审核', 'F', NULL, NULL, 'fin:voucher:audit', NULL, 4, 1, 1, NOW(), NOW()),
(1804, 180, '凭证记账', 'F', NULL, NULL, 'fin:voucher:post', NULL, 5, 1, 1, NOW(), NOW()),
(1830, 183, '应收单新增', 'F', NULL, NULL, 'fin:ar:add', NULL, 1, 1, 1, NOW(), NOW()),
(1831, 183, '应收单删除', 'F', NULL, NULL, 'fin:ar:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1832, 183, '应收生成凭证', 'F', NULL, NULL, 'fin:ar:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1840, 184, '应付单新增', 'F', NULL, NULL, 'fin:ap:add', NULL, 1, 1, 1, NOW(), NOW()),
(1841, 184, '应付单删除', 'F', NULL, NULL, 'fin:ap:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1842, 184, '应付生成凭证', 'F', NULL, NULL, 'fin:ap:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1850, 185, '收款单新增', 'F', NULL, NULL, 'fin:receipt:add', NULL, 1, 1, 1, NOW(), NOW()),
(1851, 185, '收款单删除', 'F', NULL, NULL, 'fin:receipt:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1852, 185, '收款生成凭证', 'F', NULL, NULL, 'fin:receipt:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1860, 186, '付款单新增', 'F', NULL, NULL, 'fin:payment:add', NULL, 1, 1, 1, NOW(), NOW()),
(1861, 186, '付款单删除', 'F', NULL, NULL, 'fin:payment:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1862, 186, '付款生成凭证', 'F', NULL, NULL, 'fin:payment:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1870, 187, '资产新增', 'F', NULL, NULL, 'fin:asset:add', NULL, 1, 1, 1, NOW(), NOW()),
(1871, 187, '资产修改', 'F', NULL, NULL, 'fin:asset:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1872, 187, '资产删除', 'F', NULL, NULL, 'fin:asset:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1873, 187, '计提折旧', 'F', NULL, NULL, 'fin:asset:depreciate', NULL, 4, 1, 1, NOW(), NOW()),
(1880, 188, '成本核算', 'F', NULL, NULL, 'fin:cost:run', NULL, 1, 1, 1, NOW(), NOW()),
(1890, 189, '预算编制', 'F', NULL, NULL, 'fin:budget:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1900, 190, '科目新增', 'F', NULL, NULL, 'fin:account:add', NULL, 1, 1, 1, NOW(), NOW()),
(1901, 190, '科目修改', 'F', NULL, NULL, 'fin:account:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1902, 190, '科目删除', 'F', NULL, NULL, 'fin:account:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1910, 191, '结账', 'F', NULL, NULL, 'fin:period:close', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(40, '科目类别', 'fin_account_category', 1, '会计科目的类别', NOW(), NOW()),
(41, '凭证状态', 'fin_voucher_status', 1, '记账凭证的状态', NOW(), NOW()),
(42, '凭证来源', 'fin_voucher_source', 1, '手工或业务生成', NOW(), NOW()),
(43, '核销状态', 'fin_bill_status', 1, '应收应付单的核销状态', NOW(), NOW()),
(44, '应收应付来源', 'fin_bill_source', 1, '应收应付单的来源', NOW(), NOW()),
(45, '结算方式', 'fin_settle_method', 1, '收付款的结算方式', NOW(), NOW()),
(46, '资产类别', 'fin_asset_category', 1, '固定资产的类别', NOW(), NOW()),
(47, '资产状态', 'fin_asset_status', 1, '固定资产的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_account_category', '资产', 'ASSET', 'blue', 1, 1, NOW(), NOW()),
('fin_account_category', '负债', 'LIABILITY', 'orange', 2, 1, NOW(), NOW()),
('fin_account_category', '所有者权益', 'EQUITY', 'purple', 3, 1, NOW(), NOW()),
('fin_account_category', '成本', 'COST', 'cyan', 4, 1, NOW(), NOW()),
('fin_account_category', '损益', 'PROFIT', 'green', 5, 1, NOW(), NOW()),
('fin_voucher_status', '未审核', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('fin_voucher_status', '已审核', 'AUDITED', 'blue', 2, 1, NOW(), NOW()),
('fin_voucher_status', '已记账', 'POSTED', 'green', 3, 1, NOW(), NOW()),
('fin_voucher_source', '手工', 'MANUAL', 'default', 1, 1, NOW(), NOW()),
('fin_voucher_source', '应收', 'AR', 'green', 2, 1, NOW(), NOW()),
('fin_voucher_source', '应付', 'AP', 'orange', 3, 1, NOW(), NOW()),
('fin_voucher_source', '收款', 'RECEIPT', 'cyan', 4, 1, NOW(), NOW()),
('fin_voucher_source', '付款', 'PAYMENT', 'gold', 5, 1, NOW(), NOW()),
('fin_voucher_source', '折旧', 'DEPRECIATION', 'purple', 6, 1, NOW(), NOW()),
('fin_voucher_source', '资产处置', 'ASSET', 'purple', 7, 1, NOW(), NOW()),
('fin_voucher_source', '成本结转', 'COST', 'blue', 8, 1, NOW(), NOW()),
('fin_voucher_source', '结转损益', 'CLOSE', 'red', 9, 1, NOW(), NOW()),
('fin_bill_status', '未核销', 'UNSETTLED', 'orange', 1, 1, NOW(), NOW()),
('fin_bill_status', '部分核销', 'PARTIAL', 'blue', 2, 1, NOW(), NOW()),
('fin_bill_status', '已核销', 'SETTLED', 'green', 3, 1, NOW(), NOW()),
('fin_bill_source', '销售出库', 'SALES_OUT', 'green', 1, 1, NOW(), NOW()),
('fin_bill_source', '销售退货', 'SALES_RETURN', 'red', 2, 1, NOW(), NOW()),
('fin_bill_source', '采购入库', 'PURCHASE_IN', 'blue', 3, 1, NOW(), NOW()),
('fin_bill_source', '采购退货', 'PURCHASE_RETURN', 'orange', 4, 1, NOW(), NOW()),
('fin_bill_source', '期初 / 手工', 'MANUAL', 'default', 5, 1, NOW(), NOW()),
('fin_settle_method', '银行转账', 'TRANSFER', 'default', 1, 1, NOW(), NOW()),
('fin_settle_method', '现金', 'CASH', 'default', 2, 1, NOW(), NOW()),
('fin_settle_method', '承兑汇票', 'DRAFT', 'default', 3, 1, NOW(), NOW()),
('fin_settle_method', '支付宝 / 微信', 'ONLINE', 'default', 4, 1, NOW(), NOW()),
('fin_asset_category', '房屋建筑物', 'BUILDING', 'default', 1, 1, NOW(), NOW()),
('fin_asset_category', '机器设备', 'MACHINE', 'default', 2, 1, NOW(), NOW()),
('fin_asset_category', '运输工具', 'VEHICLE', 'default', 3, 1, NOW(), NOW()),
('fin_asset_category', '电子设备', 'ELECTRONIC', 'default', 4, 1, NOW(), NOW()),
('fin_asset_category', '办公家具', 'FURNITURE', 'default', 5, 1, NOW(), NOW()),
('fin_asset_status', '在用', 'IN_USE', 'green', 1, 1, NOW(), NOW()),
('fin_asset_status', '闲置', 'IDLE', 'orange', 2, 1, NOW(), NOW()),
('fin_asset_status', '已处置', 'DISPOSED', 'default', 3, 1, NOW(), NOW());

-- 普通用户：财务管理各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 7), (2, 180), (2, 181), (2, 182), (2, 183), (2, 184), (2, 185), (2, 186), (2, 187), (2, 188), (2, 189), (2, 190), (2, 191);

-- 会计科目（企业会计准则常用科目）：自动生成凭证使用的科目见 AccountRole（可在「凭证科目设置」中修改），未设置时按编码取预置科目，需保持为末级科目
INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(1,  '1001',   '库存现金',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(2,  '1002',   '银行存款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(3,  '100201', '工商银行',               2,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(4,  '100202', '招商银行',               2,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(5,  '1122',   '应收账款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(6,  '1123',   '预付账款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(7,  '1221',   '其他应收款',             0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(8,  '1403',   '原材料',                 0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(9,  '1405',   '库存商品',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(10, '1601',   '固定资产',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(11, '1602',   '累计折旧',               0,  'ASSET',     'CREDIT', 1, NOW(), NOW()),
(12, '1606',   '固定资产清理',           0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(13, '1901',   '待处理财产损溢',         0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(20, '2001',   '短期借款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(21, '2202',   '应付账款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(22, '2203',   '预收账款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(23, '2211',   '应付职工薪酬',           0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(24, '2221',   '应交税费',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(25, '222101', '应交增值税（进项税额）', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(26, '222102', '应交增值税（销项税额）', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(27, '2241',   '其他应付款',             0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(30, '4001',   '实收资本',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(31, '4103',   '本年利润',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(32, '4104',   '利润分配',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(40, '5001',   '生产成本',               0,  'COST',      'DEBIT',  1, NOW(), NOW()),
(41, '5101',   '制造费用',               0,  'COST',      'DEBIT',  1, NOW(), NOW()),
(50, '6001',   '主营业务收入',           0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(51, '6051',   '其他业务收入',           0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(52, '6301',   '营业外收入',             0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(53, '6401',   '主营业务成本',           0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(54, '6403',   '税金及附加',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(55, '6601',   '销售费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(56, '6602',   '管理费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(57, '660201', '工资',                   56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(58, '660202', '办公费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(59, '660203', '折旧费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(60, '660204', '差旅费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(61, '6603',   '财务费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(62, '6711',   '营业外支出',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(63, '6801',   '所得税费用',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW());
-- 往来科目按客户 / 供应商辅助核算
UPDATE fin_account SET aux_items = 'CUSTOMER' WHERE code IN ('1122', '2203');
UPDATE fin_account SET aux_items = 'SUPPLIER' WHERE code IN ('1123', '2202');

-- ---------------------------------------------------------------- 会计科目补充：应交个人所得税

INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(64, '222103', '应交个人所得税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

-- 会计科目补充；期初余额菜单
INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(65, '1408', '委托加工物资', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()),
(66, '5401', '合同履约成本', 0, 'COST', 'DEBIT', 1, NOW(), NOW()),
(67, '2205', '合同负债', 0, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(68, '660205', '培训费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(192, 936, '期初余额', 'C', 'opening', 'fin/opening/index', 'fin:opening:list', 'FlagOutlined', 3, 1, 1, NOW(), NOW()),
(1920, 192, '期初修改', 'F', NULL, NULL, 'fin:opening:edit', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 192);

-- 站内消息类别
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(101, '消息类别', 'sys_message_category', 1, '站内消息的类别', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_message_category', '审批', 'APPROVAL', 'blue', 1, 1, NOW(), NOW()),
('sys_message_category', '提醒', 'REMIND', 'orange', 2, 1, NOW(), NOW()),
('sys_message_category', '预警', 'ALERT', 'red', 3, 1, NOW(), NOW()),
('sys_message_category', '系统', 'SYSTEM', 'default', 4, 1, NOW(), NOW()),
('sys_message_category', '公告', 'NOTICE', 'cyan', 5, 1, NOW(), NOW());

-- 内置流程示例：未发布，业务单据仍直接审核；在「流程管理」中按需调整并发布后启用
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('scm_purchase_order', '采购订单审批', 'PURCHASE', 1, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按订单金额", "next": {"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 5 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "50000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 金额 ≥ 5 万元时总经理 → 抄送会计。发布后采购订单「审核」改为提交审批，审批通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('fin_voucher', '凭证审批', 'FINANCE', 8, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "财务主管审核", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}', '内置流程（未发布）：财务主管审核。发布后凭证不能直接审核，由制单人提交审批，通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('fin_payment', '付款审批', 'FINANCE', 9, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "财务主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按付款金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 10 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "100000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：财务主管 → 金额 ≥ 10 万元时总经理 → 抄送出纳。发布后付款单需审批，通过后核销应付单；可在流程设计中调整审批人后发布', NOW(), NOW()),
('wms_check', '盘点差异审批', 'INVENTORY', 14, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "仓库主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_gm", "type": "CC", "name": "抄送总经理", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "next": null}}}}}', '内置流程（未发布）：仓库主管 → 会计。发布后盘点单确认需审批，通过后才调整库存；流程变量 gainQty 盘盈数量、lossQty 盘亏数量、diffLines 差异行数', NOW(), NOW()),
('fin_expense', '费用报销审批', 'FINANCE', 15, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按报销金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 5000 元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "5000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：部门主管 → 会计 → 金额 ≥ 5000 元时总经理 → 抄送出纳。发布后费用报销提交需审批，批准后待付款；流程变量 amount 报销金额', NOW(), NOW()),
('fin_loan', '员工借款审批', 'FINANCE', 16, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按借款金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 1 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "10000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：部门主管 → 会计 → 金额 ≥ 1 万元时总经理 → 抄送出纳。发布后员工借款提交需审批，批准后待付款；流程变量 amount 借款金额', NOW(), NOW()),
('wms_stock_doc', '出入库审批', 'INVENTORY', 19, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "仓库主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "biz_check", "type": "CONDITION", "name": "按业务类型", "next": null, "branches": [{"id": "biz_check_yes", "name": "报废、其他出入库", "defaultBranch": false, "conditions": [[{"field": "bizType", "operator": "IN", "value": "SCRAP,SCRAP_IN,OTHER"}]], "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "biz_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：仓库主管 → 报废、其他出入库时会计。发布后手工出入库单、调拨单确认前需审批，通过后更新库存；流程变量 kind（IN / OUT / TRANSFER）、bizType、totalQty', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('sys_approve_status', '已生效', 'APPROVED', 'green', 1, 1, NOW(), NOW()),
('sys_approve_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('sys_approve_status', '已拒绝', 'REJECTED', 'red', 3, 1, NOW(), NOW()),
('sys_approve_status', '已撤回', 'CANCELED', 'default', 4, 1, NOW(), NOW()),
('sys_approve_status', '已生效', 'DONE', 'green', 5, 1, NOW(), NOW()),
('sys_approve_status', '草稿', 'DRAFT', 'default', 6, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(102, '审批状态', 'sys_approve_status', 1, '需审批生效的业务记录（人事异动、付款、项目费用）的状态', NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(103, '冻结状态', 'wms_hold_status', 1, '库存冻结（来料检验）的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_hold_status', '待检', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('wms_hold_status', '不合格', 'REJECTED', 'red', 2, 1, NOW(), NOW()),
('wms_hold_status', '已放行', 'RELEASED', 'green', 3, 1, NOW(), NOW()),
('wms_hold_status', '已退货', 'RETURNED', 'default', 4, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '费用', 'EXPENSE', 'orange', 12, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1304, 130, '入库冲销', 'F', NULL, NULL, 'wms:in:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1314, 131, '出库冲销', 'F', NULL, NULL, 'wms:out:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1324, 132, '调拨冲销', 'F', NULL, NULL, 'wms:transfer:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1325, 132, '调拨收货', 'F', NULL, NULL, 'wms:transfer:receive', NULL, 6, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(108, '仓库类型', 'wms_warehouse_type', 1, '不良品仓的库存不计入 MRP、齐套、可承诺量', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_warehouse_type', '普通仓', 'NORMAL', 'default', 1, 1, NOW(), NOW()),
('wms_warehouse_type', '原料仓', 'RAW', 'cyan', 2, 1, NOW(), NOW()),
('wms_warehouse_type', '成品仓', 'FINISHED', 'green', 3, 1, NOW(), NOW()),
('wms_warehouse_type', '不良品仓', 'DEFECT', 'red', 4, 1, NOW(), NOW()),
('wms_doc_status', '在途', 'TRANSIT', 'blue', 3, 1, NOW(), NOW()),
('wms_doc_status', '审批中', 'APPROVING', 'processing', 4, 1, NOW(), NOW()),
('wms_out_type', '报废出库', 'SCRAP', 'red', 8, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(274, 901, '补货建议', 'C', 'reorder', 'scm/reorder/index', 'scm:reorder:list', 'AlertOutlined', 6, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 274);

INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(69, '222104', '未交增值税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(70, '222105', '应交城市维护建设税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(71, '222106', '应交教育费附加', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(72, '222107', '应交地方教育附加', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(276, 932, '发票管理', 'C', 'invoice', 'fin/invoice/index', 'fin:invoice:list', 'ProfileOutlined', 6, 1, 1, NOW(), NOW()),
(277, 932, '预收预付', 'C', 'advance', 'fin/advance/index', 'fin:advance:list', 'WalletOutlined', 5, 1, 1, NOW(), NOW()),
(278, 933, '银行对账', 'C', 'bank', 'fin/bank/index', 'fin:bank:list', 'BankOutlined', 3, 1, 1, NOW(), NOW()),
(279, 933, '费用报销', 'C', 'claim', 'fin/claim/index', 'fin:claim:list', 'AccountBookOutlined', 1, 1, 1, NOW(), NOW()),
(280, 933, '员工借款', 'C', 'loan', 'fin/loan/index', 'fin:loan:list', 'MoneyCollectOutlined', 2, 1, 1, NOW(), NOW()),
(281, 935, '经营分析', 'C', 'analysis', 'fin/analysis/index', 'fin:analysis:list', 'BarChartOutlined', 2, 1, 1, NOW(), NOW()),
(2760, 276, '发票登记', 'F', NULL, NULL, 'fin:invoice:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2770, 277, '预收预付登记', 'F', NULL, NULL, 'fin:advance:add', NULL, 1, 1, 1, NOW(), NOW()),
(2780, 278, '导入与勾对', 'F', NULL, NULL, 'fin:bank:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2790, 279, '报销新增', 'F', NULL, NULL, 'fin:claim:add', NULL, 1, 1, 1, NOW(), NOW()),
(2791, 279, '报销付款', 'F', NULL, NULL, 'fin:claim:pay', NULL, 2, 1, 1, NOW(), NOW()),
(2800, 280, '借款新增', 'F', NULL, NULL, 'fin:loan:add', NULL, 1, 1, 1, NOW(), NOW()),
(2801, 280, '借款付款与还款', 'F', NULL, NULL, 'fin:loan:pay', NULL, 2, 1, 1, NOW(), NOW()),
(1834, 183, '坏账核销', 'F', NULL, NULL, 'fin:ar:baddebt', NULL, 9, 1, 1, NOW(), NOW()),
(1805, 180, '红字冲销', 'F', NULL, NULL, 'fin:voucher:reverse', NULL, 9, 1, 1, NOW(), NOW()),
(1912, 191, '增值税结转', 'F', NULL, NULL, 'fin:period:tax', NULL, 9, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 276), (2, 277), (2, 278), (2, 279), (2, 280), (2, 281);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(110, '发票类型', 'fin_invoice_type', 1, '增值税专用发票、普通发票', NOW(), NOW()),
(111, '报销状态', 'fin_claim_status', 1, '费用报销单的状态', NOW(), NOW()),
(112, '借款状态', 'fin_loan_status', 1, '员工借款的状态', NOW(), NOW()),
(113, '预算控制', 'fin_budget_control', 1, '费用报销超预算时的处理', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_invoice_type', '增值税专用发票', 'SPECIAL', 'red', 1, 1, NOW(), NOW()),
('fin_invoice_type', '普通发票', 'NORMAL', 'blue', 2, 1, NOW(), NOW()),
('fin_invoice_type', '数电票（专票）', 'E_SPECIAL', 'volcano', 3, 1, NOW(), NOW()),
('fin_invoice_type', '数电票（普票）', 'E_NORMAL', 'geekblue', 4, 1, NOW(), NOW()),
('fin_claim_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_claim_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('fin_claim_status', '待付款', 'APPROVED', 'orange', 3, 1, NOW(), NOW()),
('fin_claim_status', '已付款', 'PAID', 'green', 4, 1, NOW(), NOW()),
('fin_loan_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_loan_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('fin_loan_status', '待付款', 'APPROVED', 'orange', 3, 1, NOW(), NOW()),
('fin_loan_status', '未还清', 'PAID', 'blue', 4, 1, NOW(), NOW()),
('fin_loan_status', '已还清', 'CLEARED', 'green', 5, 1, NOW(), NOW()),
('fin_budget_control', '不控制', 'NONE', 'default', 1, 1, NOW(), NOW()),
('fin_budget_control', '超预算提示', 'WARN', 'orange', 2, 1, NOW(), NOW()),
('fin_budget_control', '超预算禁止', 'BLOCK', 'red', 3, 1, NOW(), NOW()),
('fin_voucher_source', '预收预付', 'ADVANCE', 'cyan', 13, 1, NOW(), NOW()),
('fin_voucher_source', '坏账', 'BAD_DEBT', 'red', 14, 1, NOW(), NOW()),
('fin_voucher_source', '红字冲销', 'REVERSAL', 'red', 15, 1, NOW(), NOW()),
('fin_voucher_source', '税费', 'TAX', 'purple', 16, 1, NOW(), NOW()),
('fin_voucher_source', '报销', 'CLAIM', 'orange', 17, 1, NOW(), NOW()),
('fin_voucher_source', '借款', 'LOAN', 'gold', 18, 1, NOW(), NOW()),
('fin_bill_source', '固定资产', 'ASSET', 'purple', 9, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1405, 140, '采购订单变更', 'F', NULL, NULL, 'scm:purchase:change', NULL, 6, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(118, '变更单状态', 'scm_change_status', 1, '订单变更单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_change_status', '审批中', 'APPROVING', 'processing', 1, 1, NOW(), NOW()),
('scm_change_status', '已生效', 'APPROVED', 'green', 2, 1, NOW(), NOW()),
('scm_change_status', '已驳回', 'REJECTED', 'red', 3, 1, NOW(), NOW());

INSERT INTO fin_currency (code, name, symbol, is_base, status, sort, create_time, update_time) VALUES
('CNY', '人民币', '¥', 1, 1, 1, NOW(), NOW()),
('USD', '美元', '$', 0, 1, 2, NOW(), NOW()),
('EUR', '欧元', '€', 0, 1, 3, NOW(), NOW()),
('HKD', '港币', 'HK$', 0, 1, 4, NOW(), NOW()),
('JPY', '日元', '¥', 0, 1, 5, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(286, 936, '币种汇率', 'C', 'currency', 'fin/currency/index', 'fin:currency:list', 'DollarOutlined', 4, 1, 1, NOW(), NOW()),
(2860, 286, '汇率维护与期末调汇', 'F', NULL, NULL, 'fin:currency:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 286);
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '汇兑', 'FX', 'geekblue', 19, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_warehouse_type', '客户寄售仓', 'CONSIGN_OUT', 'purple', 5, 1, NOW(), NOW()),
('wms_warehouse_type', '供应商寄售仓', 'CONSIGN_IN', 'geekblue', 6, 1, NOW(), NOW()),
('wms_in_type', '寄售入库', 'CONSIGN_IN', 'geekblue', 9, 1, NOW(), NOW()),
('wms_out_type', '寄售消耗', 'CONSIGN', 'geekblue', 9, 1, NOW(), NOW()),
('scm_order_source', '寄售结算', 'CONSIGN', 'purple', 8, 1, NOW(), NOW());

-- 内置角色：按岗位预置菜单和按钮权限，可在「角色管理」中调整
INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(3, '总经理', 'gm', 3, 1, 1, '查看全部业务、报表与电商利润分析，审核关闭订单、头程货件', NOW(), NOW()),
(4, '采购员', 'purchaser', 4, 3, 1, '采购订单与收退货、供应商与供货价格、补货建议，可查看库存和物料', NOW(), NOW()),
(5, '电商运营', 'ecom', 5, 4, 1, '店铺与在线商品、平台订单出库发货、退货退款、头程货件、平台结算与广告、补货建议，可查看库存与物料，数据范围为本部门及以下', NOW(), NOW()),
(6, '仓库管理员', 'warehouse', 6, 1, 1, '出入库、调拨、盘点、批次序列号、库存冻结，采购收货、平台订单出库、头程货件发运与签收，手机 / PDA 扫码作业', NOW(), NOW()),
(9, '会计', 'accountant', 9, 1, 1, '财务全部功能：凭证、往来、资产、成本、预算、结账，可查看采购单据与平台结算', NOW(), NOW()),
(10, '出纳', 'cashier', 10, 1, 1, '收付款、预收预付、银行对账、报销和借款付款，可查看账簿和往来', NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 3), (3, 4), (3, 5), (3, 7), (3, 120), (3, 121), (3, 122),
(3, 123), (3, 124), (3, 130), (3, 131), (3, 132), (3, 133), (3, 134), (3, 135), (3, 136), (3, 137), (3, 138), (3, 139),
(3, 140), (3, 141), (3, 142), (3, 144), (3, 145), (3, 147), (3, 149),
(3, 180), (3, 181), (3, 182), (3, 183),
(3, 184), (3, 185), (3, 186), (3, 187), (3, 188), (3, 189), (3, 190), (3, 191), (3, 192),
(3, 266), (3, 274),
(3, 276), (3, 277), (3, 278), (3, 279), (3, 280), (3, 281), (3, 286),
(3, 1403), (3, 1404),
(3, 2790), (3, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(4, 3), (4, 4), (4, 5), (4, 7), (4, 120), (4, 121), (4, 122), (4, 123), (4, 124), (4, 134),
(4, 135), (4, 137), (4, 138), (4, 140), (4, 141), (4, 142), (4, 147),
(4, 274), (4, 279), (4, 280), (4, 1400), (4, 1401), (4, 1402), (4, 1403), (4, 1404), (4, 1405), (4, 1410), (4, 1420),
(4, 1470), (4, 1471), (4, 1472),
(4, 2790), (4, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(5, 3), (5, 4), (5, 5), (5, 7), (5, 120), (5, 121), (5, 122), (5, 123), (5, 124), (5, 134),
(5, 137), (5, 144), (5, 145), (5, 149),
(5, 279), (5, 280),
(5, 1490), (5, 1491), (5, 1492),
(5, 2790), (5, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 3), (6, 4), (6, 5), (6, 7), (6, 120), (6, 121), (6, 122), (6, 123), (6, 124), (6, 130),
(6, 131), (6, 132), (6, 133), (6, 134), (6, 135), (6, 136), (6, 137), (6, 138), (6, 139), (6, 140), (6, 141), (6, 142),
(6, 144), (6, 145), (6, 147), (6, 149), (6, 266),
(6, 279), (6, 280), (6, 1300), (6, 1301), (6, 1302), (6, 1303), (6, 1304), (6, 1310), (6, 1311), (6, 1312), (6, 1313),
(6, 1314), (6, 1320), (6, 1321), (6, 1322), (6, 1323), (6, 1324), (6, 1325), (6, 1330), (6, 1331), (6, 1332), (6, 1333), (6, 1370),
(6, 1371), (6, 1372), (6, 1380), (6, 1381), (6, 1382), (6, 1390), (6, 1391), (6, 1392), (6, 1410), (6, 1420), (6, 1440), (6, 1450),
(6, 2660), (6, 2790), (6, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(9, 3), (9, 4), (9, 5), (9, 7), (9, 120), (9, 121), (9, 122), (9, 123), (9, 124), (9, 134),
(9, 137), (9, 140), (9, 141), (9, 142), (9, 144), (9, 145), (9, 147), (9, 149), (9, 180), (9, 181), (9, 182),
(9, 183), (9, 184), (9, 185), (9, 186), (9, 187), (9, 188), (9, 189), (9, 190), (9, 191), (9, 192),
(9, 276), (9, 277), (9, 278), (9, 279), (9, 280), (9, 281), (9, 286), (9, 1800), (9, 1801), (9, 1802), (9, 1803),
(9, 1804), (9, 1805), (9, 1830), (9, 1831), (9, 1832), (9, 1834), (9, 1840), (9, 1841), (9, 1842), (9, 1850), (9, 1851), (9, 1852),
(9, 1860), (9, 1861), (9, 1862), (9, 1870), (9, 1871), (9, 1872), (9, 1873), (9, 1880), (9, 1890), (9, 1900), (9, 1901), (9, 1902),
(9, 1910), (9, 1912), (9, 1920), (9, 2760), (9, 2770), (9, 2780), (9, 2790), (9, 2791), (9, 2800), (9, 2801), (9, 2860);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(10, 3), (10, 7), (10, 120), (10, 121), (10, 122), (10, 123), (10, 124), (10, 181), (10, 183), (10, 184), (10, 185),
(10, 186), (10, 276), (10, 277), (10, 278), (10, 279), (10, 280), (10, 286), (10, 1850), (10, 1851), (10, 1860), (10, 1861),
(10, 2770), (10, 2780), (10, 2790), (10, 2791), (10, 2800), (10, 2801);

-- 菜单分组：模块下的二级目录，不填路由地址，页面地址保持不变
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(901, 5, '采购', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(902, 5, '销售出库', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(903, 5, '往来单位', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(911, 4, '库存作业', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(912, 4, '库存查询', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(913, 4, '库存控制', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(914, 4, '基础资料', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(931, 7, '总账', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(932, 7, '应收应付', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(933, 7, '资金与费用', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(934, 7, '资产与成本', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(935, 7, '预算与分析', 'M', NULL, NULL, NULL, NULL, 5, 1, 1, NOW(), NOW()),
(936, 7, '期末与设置', 'M', NULL, NULL, NULL, NULL, 6, 1, 1, NOW(), NOW()),
(961, 3, '我的事务', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(962, 3, '流程管理', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, c.parent_id FROM sys_role_menu rm JOIN sys_menu c ON c.id = rm.menu_id
WHERE c.parent_id IN (901, 902, 903, 911, 912, 913, 914, 931, 932, 933, 934, 935, 936, 961, 962)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = c.parent_id);

-- 通用流程（可在「发起申请」中直接发起，未发布）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wf_category', '行政', 'ADMIN', 'cyan', 9, 1, NOW(), NOW());
INSERT INTO wf_model (model_key, name, category, icon, sort, status, design_json, remark, create_time, update_time) VALUES
('oa_travel', '出差申请', 'ADMIN', 'CarOutlined', 21, 1, '{"form": [{"field": "destination", "label": "出差地点", "type": "INPUT", "required": true, "placeholder": "如：上海"}, {"field": "startDate", "label": "开始日期", "type": "DATE", "required": true, "placeholder": ""}, {"field": "endDate", "label": "结束日期", "type": "DATE", "required": true, "placeholder": ""}, {"field": "days", "label": "出差天数", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "budget", "label": "预计费用（元）", "type": "NUMBER", "required": false, "placeholder": ""}, {"field": "reason", "label": "出差事由", "type": "TEXTAREA", "required": true, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "days_check", "type": "CONDITION", "name": "按天数和费用", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "days_check_yes", "name": "≥ 5 天或费用 ≥ 1 万元", "defaultBranch": false, "conditions": [[{"field": "days", "operator": "GE", "value": "5"}], [{"field": "budget", "operator": "GE", "value": "10000"}]], "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "days_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程：部门主管 → 出差 ≥ 5 天或费用 ≥ 1 万元时上级主管 → 抄送人事。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_seal', '用印申请', 'ADMIN', 'SafetyCertificateOutlined', 22, 1, '{"form": [{"field": "sealType", "label": "印章类型", "type": "SELECT", "required": true, "placeholder": "", "options": ["公章", "合同专用章", "财务专用章", "部门章"]}, {"field": "fileName", "label": "文件名称", "type": "INPUT", "required": true, "placeholder": ""}, {"field": "copies", "label": "用印份数", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "purpose", "label": "用途说明", "type": "TEXTAREA", "required": true, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "seal_check", "type": "CONDITION", "name": "按印章类型", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "seal_check_yes", "name": "公章、合同专用章", "defaultBranch": false, "conditions": [[{"field": "sealType", "operator": "IN", "value": "公章,合同专用章"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "seal_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程：部门主管 → 公章、合同章时总经理 → 抄送人事行政。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_goods', '物品领用', 'ADMIN', 'ShoppingOutlined', 23, 1, '{"form": [{"field": "goods", "label": "物品名称", "type": "INPUT", "required": true, "placeholder": "如：笔记本电脑、显示器"}, {"field": "qty", "label": "数量", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "useDate", "label": "领用日期", "type": "DATE", "required": false, "placeholder": ""}, {"field": "purpose", "label": "用途", "type": "TEXTAREA", "required": false, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_wh", "type": "CC", "name": "抄送仓库管理员", "assignee": {"type": "ROLE", "ids": [6], "names": ["仓库管理员"]}, "next": null}}}}', '内置流程：部门主管 → 抄送仓库管理员。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_general', '通用审批', 'OTHER', 'FileTextOutlined', 24, 1, '{"form": [{"field": "title", "label": "审批事项", "type": "INPUT", "required": true, "placeholder": ""}, {"field": "content", "label": "详细说明", "type": "TEXTAREA", "required": true, "placeholder": ""}, {"field": "amount", "label": "涉及金额（元）", "type": "NUMBER", "required": false, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}}', '内置流程：部门主管 → 上级主管（依次审批）。可在「发起申请」中直接发起', NOW(), NOW());

-- ---------------------------------------------------------------- 资金管理：账户调拨、资金计划（应收票据、应付票据科目用于资产负债表）
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('1121', '应收票据', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()),
('2201', '应付票据', 0, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '账户调拨', 'TRANSFER', 'cyan', 21, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(194, 933, '账户调拨', 'C', 'transfer', 'fin/transfer/index', 'fin:transfer:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(195, 933, '资金计划', 'C', 'fund-plan', 'fin/fund-plan/index', 'fin:fundPlan:list', 'FundOutlined', 6, 1, 1, NOW(), NOW()),
(1940, 194, '调拨登记', 'F', NULL, NULL, 'fin:transfer:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1950, 195, '计划项维护', 'F', NULL, NULL, 'fin:fundPlan:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 普通用户只读；会计、出纳可操作；总经理查看资金计划
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 194), (2, 195),
(9, 194), (9, 195), (9, 1940), (9, 1950),
(10, 194), (10, 195), (10, 1940), (10, 1950),
(3, 195);

-- ===================== 采购合同、供应商对账 =====================
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(130, '采购合同类型', 'scm_contract_type', 1, '框架协议或固定数量合同', NOW(), NOW()),
(131, '采购合同状态', 'scm_contract_status', 1, '采购合同的状态', NOW(), NOW()),
(134, '供应商确认', 'scm_supplier_confirm', 1, '供应商对采购订单的确认结果', NOW(), NOW()),
(135, '对账单状态', 'fin_statement_status', 1, '供应商对账单的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_contract_type', '框架协议', 'FRAMEWORK', 'blue', 1, 1, NOW(), NOW()),
('scm_contract_type', '固定数量合同', 'FIXED', 'green', 2, 1, NOW(), NOW()),
('scm_contract_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('scm_contract_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_contract_status', '生效', 'ACTIVE', 'green', 3, 1, NOW(), NOW()),
('scm_contract_status', '已关闭', 'CLOSED', 'default', 4, 1, NOW(), NOW()),
('scm_supplier_confirm', '已接受', 'ACCEPTED', 'green', 1, 1, NOW(), NOW()),
('scm_supplier_confirm', '已拒绝', 'REJECTED', 'red', 2, 1, NOW(), NOW()),
('fin_statement_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_statement_status', '待确认', 'SENT', 'blue', 2, 1, NOW(), NOW()),
('fin_statement_status', '已确认', 'CONFIRMED', 'green', 3, 1, NOW(), NOW()),
('fin_statement_status', '有异议', 'DISPUTED', 'red', 4, 1, NOW(), NOW());

-- 应收应付下新增供应商对账
UPDATE sys_menu SET sort = 3 WHERE id = 902;
UPDATE sys_menu SET sort = 4 WHERE id = 903;
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 901 AND sort >= 4;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(299, 932, '供应商对账', 'C', 'statement', 'fin/statement/index', 'fin:statement:list', 'ReconciliationOutlined', 7, 1, 1, NOW(), NOW()),
(2990, 299, '对账单生成', 'F', NULL, NULL, 'fin:statement:add', NULL, 1, 1, 1, NOW(), NOW()),
(2991, 299, '对账单发送', 'F', NULL, NULL, 'fin:statement:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2992, 299, '对账单删除', 'F', NULL, NULL, 'fin:statement:remove', NULL, 3, 1, 1, NOW(), NOW());

-- 普通用户只读；会计：供应商对账；总经理查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 299),
(9, 299), (9, 2990), (9, 2991), (9, 2992),
(3, 299);

-- ===================== 资产：使用人与存放地点、领用调拨、盘点、低值易耗品 =====================
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('1411', '周转材料', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW());
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(140, '资产变动类型', 'fin_asset_move_type', 1, '资产领用、归还、调拨', NOW(), NOW()),
(141, '资产盘点状态', 'fin_asset_check_status', 1, '资产盘点单的状态', NOW(), NOW()),
(142, '资产盘点结果', 'fin_asset_check_result', 1, '资产盘点明细的结果', NOW(), NOW()),
(143, '易耗品领用状态', 'fin_consumable_status', 1, '低值易耗品领用单的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_asset_move_type', '领用', 'ASSIGN', 'blue', 1, 1, NOW(), NOW()),
('fin_asset_move_type', '归还', 'RETURN', 'orange', 2, 1, NOW(), NOW()),
('fin_asset_move_type', '调拨', 'TRANSFER', 'purple', 3, 1, NOW(), NOW()),
('fin_asset_check_status', '盘点中', 'CHECKING', 'processing', 1, 1, NOW(), NOW()),
('fin_asset_check_status', '已完成', 'DONE', 'green', 2, 1, NOW(), NOW()),
('fin_asset_check_result', '待盘', 'PENDING', 'default', 1, 1, NOW(), NOW()),
('fin_asset_check_result', '相符', 'NORMAL', 'green', 2, 1, NOW(), NOW()),
('fin_asset_check_result', '位置变化', 'MOVED', 'orange', 3, 1, NOW(), NOW()),
('fin_asset_check_result', '盘亏', 'LOSS', 'red', 4, 1, NOW(), NOW()),
('fin_asset_check_result', '盘盈', 'GAIN', 'purple', 5, 1, NOW(), NOW()),
('fin_consumable_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_consumable_status', '已出库', 'CONFIRMED', 'green', 2, 1, NOW(), NOW()),
('wms_out_type', '部门领用', 'CONSUME', 'lime', 10, 1, NOW(), NOW());

-- 资产与成本：固定资产之后为资产盘点、低值易耗品
UPDATE sys_menu SET sort = sort + 2 WHERE parent_id = 934 AND sort >= 2;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(307, 934, '资产盘点', 'C', 'asset-check', 'fin/asset-check/index', 'fin:assetCheck:list', 'AuditOutlined', 2, 1, 1, NOW(), NOW()),
(308, 934, '低值易耗品', 'C', 'consumable', 'fin/consumable/index', 'fin:consumable:list', 'ToolOutlined', 3, 1, 1, NOW(), NOW()),
(1874, 187, '领用调拨', 'F', NULL, NULL, 'fin:asset:move', NULL, 5, 1, 1, NOW(), NOW()),
(3070, 307, '盘点新增', 'F', NULL, NULL, 'fin:assetCheck:add', NULL, 1, 1, 1, NOW(), NOW()),
(3071, 307, '盘点录入', 'F', NULL, NULL, 'fin:assetCheck:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3072, 307, '盘点删除', 'F', NULL, NULL, 'fin:assetCheck:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3080, 308, '领用新增', 'F', NULL, NULL, 'fin:consumable:add', NULL, 1, 1, 1, NOW(), NOW()),
(3081, 308, '领用出库', 'F', NULL, NULL, 'fin:consumable:confirm', NULL, 2, 1, 1, NOW(), NOW()),
(3082, 308, '领用删除', 'F', NULL, NULL, 'fin:consumable:remove', NULL, 3, 1, 1, NOW(), NOW());

-- 普通用户只读；会计：全部；总经理查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 307), (2, 308),
(9, 307), (9, 308), (9, 1874), (9, 3070), (9, 3071), (9, 3072), (9, 3080), (9, 3081), (9, 3082),
(3, 307), (3, 308);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(149, '促销类型', 'scm_promo_type', 1, '促销活动的类型', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_promo_type', '折扣', 'DISCOUNT', 'blue', 1, 1, NOW(), NOW()),
('scm_promo_type', '满减', 'REDUCE', 'orange', 2, 1, NOW(), NOW()),
('scm_promo_type', '买赠', 'GIFT', 'magenta', 3, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(152, '销售渠道', 'scm_channel', 1, '客户的销售渠道：直销、经销商', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_channel', '直销', 'DIRECT', 'default', 1, 1, NOW(), NOW()),
('scm_channel', '经销商', 'DEALER', 'purple', 2, 1, NOW(), NOW()),
('scm_order_source', '外部导入', 'IMPORT', 'lime', 9, 1, NOW(), NOW()),
('scm_order_source', '直接开单', 'DIRECT', 'blue', 10, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 仓储作业：库位类型与区域、上架策略
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(153, '库位类型', 'wms_location_type', 1, '存储位、拣货位、收发货暂存位、不良品位', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_location_type', '存储位', 'STORAGE', 'blue', 1, 1, NOW(), NOW()),
('wms_location_type', '拣货位', 'PICK', 'green', 2, 1, NOW(), NOW()),
('wms_location_type', '收货暂存', 'RECEIVE', 'cyan', 3, 1, NOW(), NOW()),
('wms_location_type', '发货暂存', 'SHIP', 'purple', 4, 1, NOW(), NOW()),
('wms_location_type', '不良品位', 'DEFECT', 'red', 5, 1, NOW(), NOW());

-- 移动作业（手机 / PDA）：独立的移动端布局，页面权限沿用对应的业务操作权限
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(15, 0, '移动作业', 'M', '/m', NULL, NULL, 'MobileOutlined', 15, 1, 1, NOW(), NOW()),
(330, 15, '扫码收货', 'C', 'receive', 'mobile/receive/index', 'scm:purchaseIn:add', 'ScanOutlined', 1, 1, 1, NOW(), NOW()),
(331, 15, '上架', 'C', 'putaway', 'mobile/putaway/index', 'wms:transfer:add', 'VerticalAlignTopOutlined', 2, 1, 1, NOW(), NOW()),
(333, 15, '盘点', 'C', 'check', 'mobile/check/index', 'wms:check:edit', 'AuditOutlined', 4, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 15), (6, 330), (6, 331), (6, 333);

-- 存货计价方法
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(155, '存货计价方法', 'wms_cost_method', 1, '物料的存货计价方法，月末成本核算时按此计算出库成本', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_cost_method', '月末一次加权平均', 'WEIGHTED', 'blue', 1, 1, NOW(), NOW()),
('wms_cost_method', '移动加权平均', 'MOVING', 'cyan', 2, 1, NOW(), NOW()),
('wms_cost_method', '先进先出', 'FIFO', 'purple', 3, 1, NOW(), NOW());

-- 期初入库：系统启用时导入的期初库存
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_type', '期初入库', 'OPENING', 'gold', 10, 1, NOW(), NOW());

-- 规格属性（多规格物料）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(342, 914, '规格属性', 'C', 'attribute', 'wms/attribute/index', 'wms:attribute:list', 'TagsOutlined', 4, 1, 1, NOW(), NOW()),
(3420, 342, '规格属性维护', 'F', NULL, NULL, 'wms:attribute:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 342), (3, 342), (4, 342), (6, 342), (6, 3420);
INSERT INTO wms_attribute (id, name, sort, status, create_time, update_time) VALUES
(1, '颜色', 1, 1, NOW(), NOW()),
(2, '尺码', 2, 1, NOW(), NOW());
INSERT INTO wms_attribute_value (attribute_id, attr_value, code, sort, create_time, update_time) VALUES
(1, '黑色', 'BK', 1, NOW(), NOW()),
(1, '白色', 'WH', 2, NOW(), NOW()),
(1, '红色', 'RD', 3, NOW(), NOW()),
(1, '蓝色', 'BL', 4, NOW(), NOW()),
(2, 'S', 'S', 1, NOW(), NOW()),
(2, 'M', 'M', 2, NOW(), NOW()),
(2, 'L', 'L', 3, NOW(), NOW()),
(2, 'XL', 'XL', 4, NOW(), NOW()),
(2, 'XXL', 'XXL', 5, NOW(), NOW());

-- 组装拆卸单
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(343, 911, '组装拆卸', 'C', 'assembly', 'wms/assembly/index', 'wms:assembly:list', 'BuildOutlined', 5, 1, 1, NOW(), NOW()),
(3430, 343, '组装拆卸维护', 'F', NULL, NULL, 'wms:assembly:add', NULL, 1, 1, 1, NOW(), NOW()),
(3431, 343, '组装拆卸确认', 'F', NULL, NULL, 'wms:assembly:confirm', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 343), (3, 343), (6, 343), (6, 3430), (6, 3431), (9, 343);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(156, '组装拆卸种类', 'wms_assembly_kind', 1, '组装、拆卸', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_assembly_kind', '组装', 'ASSEMBLE', 'blue', 1, 1, NOW(), NOW()),
('wms_assembly_kind', '拆卸', 'DISASSEMBLE', 'orange', 2, 1, NOW(), NOW()),
('wms_in_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW()),
('wms_out_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW());

-- 价格控制方式
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(158, '价格控制方式', 'scm_price_control_mode', 1, '超出最低销售价 / 最高采购价时的处理', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_price_control_mode', '提示', 'WARN', 'orange', 1, 1, NOW(), NOW()),
('scm_price_control_mode', '禁止保存', 'BLOCK', 'red', 2, 1, NOW(), NOW());

-- 自定义字段
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(345, 1, '自定义字段', 'C', 'custom-field', 'system/custom-field/index', 'sys:customField:list', 'FormOutlined', 11, 1, 1, NOW(), NOW()),
(3450, 345, '自定义字段维护', 'F', NULL, NULL, 'sys:customField:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 345);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(159, '自定义字段类型', 'sys_custom_field_type', 1, '自定义字段的输入类型', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_custom_field_type', '文本', 'TEXT', 'default', 1, 1, NOW(), NOW()),
('sys_custom_field_type', '多行文本', 'TEXTAREA', 'default', 2, 1, NOW(), NOW()),
('sys_custom_field_type', '数字', 'NUMBER', 'blue', 3, 1, NOW(), NOW()),
('sys_custom_field_type', '日期', 'DATE', 'cyan', 4, 1, NOW(), NOW()),
('sys_custom_field_type', '下拉选择', 'SELECT', 'purple', 5, 1, NOW(), NOW()),
('sys_custom_field_type', '是否', 'BOOL', 'green', 6, 1, NOW(), NOW());

-- 业务参数
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(346, 1, '业务参数', 'C', 'biz-rule', 'system/biz-rule/index', 'sys:bizRule:list', 'SlidersOutlined', 12, 1, 1, NOW(), NOW()),
(3460, 346, '业务参数修改', 'F', NULL, NULL, 'sys:bizRule:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 346);

-- 第三方集成：钉钉、企业微信、飞书（扫码登录、消息推送、通讯录同步）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(350, 1, '第三方集成', 'C', 'im', 'system/im/index', 'sys:im:list', 'ApiOutlined', 13, 1, 1, NOW(), NOW()),
(3500, 350, '集成配置修改', 'F', NULL, NULL, 'sys:im:edit', NULL, 1, 1, 1, NOW(), NOW()),
(3501, 350, '通讯录同步与绑定', 'F', NULL, NULL, 'sys:im:sync', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(160, '集成平台', 'sys_im_platform', 1, '第三方集成的平台', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_im_platform', '钉钉', 'DINGTALK', 'blue', 1, 1, NOW(), NOW()),
('sys_im_platform', '企业微信', 'WECOM', 'green', 2, 1, NOW(), NOW()),
('sys_im_platform', '飞书', 'FEISHU', 'cyan', 3, 1, NOW(), NOW());

-- 业务字典：各租户维护自己的业务字典取值（系统管理员可见）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(352, 1, '业务字典', 'C', 'biz-dict', 'system/bizDict/index', 'sys:bizdict:list', 'TagsOutlined', 5, 1, 1, NOW(), NOW()),
(3520, 352, '业务字典维护', 'F', NULL, NULL, 'sys:bizdict:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 收发类别：手工其他入库 / 其他出库的用途分类（业务字典，月末成本凭证可按类别设置对方科目）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(161, '入库类别', 'wms_in_category', 'BUSINESS', 1, '其他入库的用途分类，可在凭证科目设置中按类别指定对方科目', NOW(), NOW()),
(162, '出库类别', 'wms_out_category', 'BUSINESS', 1, '其他出库、报废出库的用途分类，可在凭证科目设置中按类别指定对方科目', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_category', '样品退回', 'SAMPLE_RETURN', 'blue', 1, 1, NOW(), NOW()),
('wms_in_category', '受赠入库', 'GIFT', 'green', 2, 1, NOW(), NOW()),
('wms_in_category', '领用退回', 'USE_RETURN', 'cyan', 3, 1, NOW(), NOW()),
('wms_in_category', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW()),
('wms_out_category', '样品出库', 'SAMPLE', 'blue', 1, 1, NOW(), NOW()),
('wms_out_category', '赠品出库', 'GIFT', 'green', 2, 1, NOW(), NOW()),
('wms_out_category', '福利领用', 'WELFARE', 'orange', 3, 1, NOW(), NOW()),
('wms_out_category', '研发领用', 'RD', 'purple', 4, 1, NOW(), NOW()),
('wms_out_category', '办公领用', 'OFFICE', 'cyan', 5, 1, NOW(), NOW()),
('wms_out_category', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW());

-- 凭证科目设置：自动凭证各用途使用的科目
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(340, 936, '凭证科目设置', 'C', 'account-rule', 'fin/account-rule/index', 'fin:accountRule:list', 'NodeIndexOutlined', 5, 1, 1, NOW(), NOW()),
(3400, 340, '科目设置修改', 'F', NULL, NULL, 'fin:accountRule:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 340), (9, 340), (9, 3400);

-- 出纳管理：现金 / 银行日记账、资金日报
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(341, 933, '出纳管理', 'C', 'cashier', 'fin/cashier/index', 'fin:cashier:list', 'WalletOutlined', 0, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 341), (3, 341), (9, 341), (10, 341);

-- 业务报表：收发存汇总（库存查询）、往来对账单（应收应付）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(347, 912, '收发存汇总', 'C', 'in-out', 'wms/inout/index', 'wms:inout:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(348, 932, '往来对账单', 'C', 'partner-statement', 'fin/partner-statement/index', 'fin:partnerStatement:list', 'AuditOutlined', 8, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 347), (3, 347), (4, 347), (6, 347), (9, 347),
(2, 348), (3, 348), (9, 348), (10, 348);

-- 智能（AI）：AI 问数、AI 设置（国产大模型，按租户配置；第二期的智能录入、数据整理、智能报告页面放在本模块下）
-- 顶部导航排在商务智能之后
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 0 AND sort >= 10;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(16, 0, '智能', 'M', '/ai', NULL, NULL, 'RobotOutlined', 10, 1, 1, NOW(), NOW()),
(360, 16, 'AI 问数', 'C', 'chat', 'ai/chat/index', 'ai:chat:use', 'MessageOutlined', 1, 1, 1, NOW(), NOW()),
(361, 16, 'AI 设置', 'C', 'settings', 'ai/settings/index', 'ai:config:list', 'SettingOutlined', 99, 1, 1, NOW(), NOW()),
(3610, 361, 'AI 设置修改', 'F', NULL, NULL, 'ai:config:edit', NULL, 1, 1, 1, NOW(), NOW());
-- AI 问数给总经理（工具按用户自己的权限和数据权限查询）；AI 设置只给超级管理员，租户管理员按套餐获得
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 16), (3, 360);
-- 智能录入：上传单据图片 / PDF / 表格识别成单据草稿，在业务页面核对后保存（还需要目标单据的新增权限）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(362, 16, '智能录入', 'C', 'extract', 'ai/extract/index', 'ai:extract:use', 'ScanOutlined', 2, 1, 1, NOW(), NOW());
-- 给总经理、采购员、销售员、会计（各自只能生成有新增权限的单据）；其他 AI 页面也会给这些角色加「智能」模块，用 NOT EXISTS 避免重复
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 362) m ON 1 = 1
WHERE r.id IN (3, 4, 5, 9) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);
-- 数据整理：物料、客户、供应商的查重与规范化建议，采纳后调用业务模块的修改 / 停用（还需要对应资料的查看、修改权限）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(363, 16, '数据整理', 'C', 'cleanse', 'ai/cleanse/index', 'ai:cleanse:list', 'ClearOutlined', 3, 1, 1, NOW(), NOW()),
(3630, 363, '采纳整理建议', 'F', NULL, NULL, 'ai:cleanse:apply', NULL, 1, 1, 1, NOW(), NOW());
-- 给采购员（供应商）、仓库管理员（物料）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 363 UNION ALL SELECT 3630) m ON 1 = 1
WHERE r.id IN (4, 6) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);
-- 智能报告：按周 / 月汇总有权限的业务数据，由 AI 撰写经营报告（手动生成或定时任务自动生成）；BI 预警的 AI 分析
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(364, 16, '智能报告', 'C', 'report', 'ai/report/index', 'ai:report:list', 'FileTextOutlined', 4, 1, 1, NOW(), NOW()),
(3640, 364, '生成报告', 'F', NULL, NULL, 'ai:report:generate', NULL, 1, 1, 1, NOW(), NOW());
-- 给总经理（数据权限为全部数据，能看自动生成的全公司报告）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 364 UNION ALL SELECT 3640) m ON 1 = 1
WHERE r.id IN (3) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 价格体系：品牌、价格列（零售价、批发价1-8）、客户价格等级与执行价、价格记忆与价格参考
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(175, '商品品牌', 'wms_material_brand', 'BUSINESS', 1, '物料的品牌，进销存报表可按品牌汇总', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_material_brand', '自有品牌', 'OWN', 'blue', 1, 1, NOW(), NOW()),
('wms_material_brand', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW());
INSERT INTO wms_price_level (level, name, status, create_time, update_time) VALUES
(0, '零售价', 1, NOW(), NOW()), (1, '批发价1', 1, NOW(), NOW()), (2, '批发价2', 1, NOW(), NOW()), (3, '批发价3', 1, NOW(), NOW()),
(4, '批发价4', 0, NOW(), NOW()), (5, '批发价5', 0, NOW(), NOW()), (6, '批发价6', 0, NOW(), NOW()), (7, '批发价7', 0, NOW(), NOW()),
(8, '批发价8', 0, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(3651, 137, '价格列设置', 'F', NULL, NULL, 'wms:material:priceLevel', NULL, 5, 1, 1, NOW(), NOW());
-- 普通用户、总经理、销售员只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 5 AS menu_id UNION ALL SELECT 902) m ON 1 = 1
WHERE r.id IN (2, 3, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 进销存报表（供应链下的分组，代码在 bi 模块）：实时汇总出入库单，汇总 → 明细 → 单据穿透；资产状况汇总全公司数据（需要「全部数据」权限）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(966, 5, '进销存报表', 'M', NULL, NULL, NULL, 'BarChartOutlined', 5, 1, 1, NOW(), NOW()),
(370, 966, '销售报表', 'C', 'jxc-sales', 'jxc/sales/index', 'bi:jxc:sales', 'RiseOutlined', 1, 1, 1, NOW(), NOW()),
(371, 966, '进货报表', 'C', 'jxc-purchase', 'jxc/purchase/index', 'bi:jxc:purchase', 'ShoppingCartOutlined', 2, 1, 1, NOW(), NOW()),
(372, 966, '经营状况', 'C', 'jxc-business', 'jxc/business/index', 'bi:jxc:business', 'LineChartOutlined', 3, 1, 1, NOW(), NOW()),
(373, 966, '资产状况', 'C', 'jxc-assets', 'jxc/assets/index', 'bi:jxc:asset', 'AccountBookOutlined', 4, 1, 1, NOW(), NOW()),
(374, 966, '商品分析', 'C', 'jxc-goods', 'jxc/goods/index', 'bi:jxc:goods', 'AppstoreOutlined', 5, 1, 1, NOW(), NOW()),
(375, 966, '客户分析', 'C', 'jxc-rfm', 'jxc/rfm/index', 'bi:jxc:rfm', 'TeamOutlined', 6, 1, 1, NOW(), NOW());
-- 普通用户（除资产状况）、总经理、会计全部；采购员进货报表和商品分析；销售员销售报表和客户分析；仓库管理员商品分析；出纳资产状况（连同分组和供应链模块）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 370 AS menu_id UNION ALL SELECT 2, 371 UNION ALL SELECT 2, 372 UNION ALL SELECT 2, 374 UNION ALL SELECT 2, 375 UNION ALL SELECT 3, 370 UNION ALL SELECT 3, 371
    UNION ALL SELECT 3, 372 UNION ALL SELECT 3, 373 UNION ALL SELECT 3, 374 UNION ALL SELECT 3, 375 UNION ALL SELECT 9, 370 UNION ALL SELECT 9, 371
    UNION ALL SELECT 9, 372 UNION ALL SELECT 9, 373 UNION ALL SELECT 9, 374 UNION ALL SELECT 9, 375 UNION ALL SELECT 4, 371 UNION ALL SELECT 4, 374
    UNION ALL SELECT 5, 370 UNION ALL SELECT 5, 375 UNION ALL SELECT 6, 374 UNION ALL SELECT 10, 373
    UNION ALL SELECT r.id, 966 FROM sys_role r WHERE r.id IN (2, 3, 4, 5, 6, 9, 10)
    UNION ALL SELECT r.id, 5 FROM sys_role r WHERE r.id IN (2, 3, 4, 5, 6, 9, 10)
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 实时库存成本：库存查询的「查看成本金额」按钮（均价、库存金额、流水的单价金额），授权总经理、仓库管理员、会计
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(3652, 134, '查看成本金额', 'F', NULL, NULL, 'wms:stock:cost', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 3652 FROM sys_role r
WHERE r.id IN (3, 6, 9) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 3652);

-- 应收应付：冲账单（垫付运费、返利、三包赔偿等抵扣往来款，复核后生成红字应收应付单）、应付坏账（无法支付的应付款转销）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(185, '冲账项目', 'fin_offset_item', 'BUSINESS', 1, '冲账单的项目，如垫付运费、销售返利、三包赔偿', NOW(), NOW()),
(186, '冲账单状态', 'fin_offset_status', 'SYSTEM', 1, '冲账单的复核状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_offset_item', '垫付运费', 'FREIGHT', 'cyan', 1, 1, NOW(), NOW()),
('fin_offset_item', '销售返利', 'REBATE', 'volcano', 2, 1, NOW(), NOW()),
('fin_offset_item', '三包赔偿', 'WARRANTY', 'orange', 3, 1, NOW(), NOW()),
('fin_offset_item', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW()),
('fin_offset_status', '待复核', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_offset_status', '已复核', 'REVIEWED', 'green', 2, 1, NOW(), NOW()),
('fin_bill_source', '冲账单', 'OFFSET', 'magenta', 13, 1, NOW(), NOW()),
('fin_bill_source', '订单尾差调整', 'ORDER_ADJUST', 'geekblue', 14, 1, NOW(), NOW());
UPDATE sys_dict_data SET builtin = 1 WHERE dict_type = 'fin_offset_item' AND dict_value = 'OTHER';

-- 运费承担方（发运单：本公司承担计销售费用、客户承担为代垫运费计入应收；采购入库：供应商代垫计入应付、本公司承担付承运商，都计入存货成本）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(196, '运费承担', 'scm_freight_bearer', 'SYSTEM', 1, '发运单、出入库单的运费由谁承担 / 付给谁', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_freight_bearer', '本公司承担', 'COMPANY', 'default', 1, 1, NOW(), NOW()),
('scm_freight_bearer', '客户承担（代垫）', 'CUSTOMER', 'blue', 2, 1, NOW(), NOW()),
('scm_freight_bearer', '供应商代垫', 'SUPPLIER', 'orange', 3, 1, NOW(), NOW()),
('fin_bill_source', '采购运费', 'PURCHASE_FREIGHT', 'cyan', 15, 1, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(377, 932, '冲账单', 'C', 'offset', 'fin/offset/index', 'fin:offset:list', 'InteractionOutlined', 9, 1, 1, NOW(), NOW()),
(3770, 377, '冲账单新增', 'F', NULL, NULL, 'fin:offset:add', NULL, 1, 1, 1, NOW(), NOW()),
(3771, 377, '冲账单修改', 'F', NULL, NULL, 'fin:offset:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3772, 377, '冲账单删除', 'F', NULL, NULL, 'fin:offset:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3773, 377, '冲账单复核', 'F', NULL, NULL, 'fin:offset:review', NULL, 4, 1, 1, NOW(), NOW()),
(1843, 184, '应付坏账', 'F', NULL, NULL, 'fin:ap:baddebt', NULL, 9, 1, 1, NOW(), NOW());
-- 普通用户、总经理、出纳只读；会计全部
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 377 AS menu_id UNION ALL SELECT 3, 377 UNION ALL SELECT 10, 377 UNION ALL SELECT 9, 377 UNION ALL SELECT 9, 3770 UNION ALL SELECT 9, 3771 UNION ALL SELECT 9, 3772
    UNION ALL SELECT 9, 3773 UNION ALL SELECT 9, 1843
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 客户区域（业务字典，每个租户一份）：客户的地区下拉选择，进销存报表按区域汇总
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(187, '客户区域', 'scm_region', 'BUSINESS', 1, '客户的销售区域（经销商为授权区域），CRM 客户、线索的地区也用此字典，进销存报表按区域汇总', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_region', '华东', 'EAST', 'blue', 1, 1, NOW(), NOW()),
('scm_region', '华南', 'SOUTH', 'green', 2, 1, NOW(), NOW()),
('scm_region', '华北', 'NORTH', 'orange', 3, 1, NOW(), NOW()),
('scm_region', '华中', 'CENTRAL', 'cyan', 4, 1, NOW(), NOW()),
('scm_region', '西南', 'SOUTHWEST', 'purple', 5, 1, NOW(), NOW()),
('scm_region', '西北', 'NORTHWEST', 'gold', 6, 1, NOW(), NOW()),
('scm_region', '东北', 'NORTHEAST', 'geekblue', 7, 1, NOW(), NOW()),
('scm_region', '海外', 'OVERSEAS', 'magenta', 8, 1, NOW(), NOW());

-- 商品速查（issue #97，Web 与小程序共用）：按名称、规格、简拼搜索商品，看可用库存和个人设置中选择的一种价格，联查库存分布、售价 / 进价历史。
-- 价格按已有权限过滤（进货类看采购订单 / 采购入库查看权限，库存均价看「查看成本金额」，售价类看报价 / 销售订单 / 销售出库查看权限），不另设按钮。
-- 授权普通用户、总经理、采购员、销售员、仓库管理员、销售经理（连同销售分组和供应链模块）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(368, 902, '商品速查', 'C', 'quick-lookup', 'scm/quickLookup/index', 'scm:quickLookup:list', 'SearchOutlined', 10, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 5 AS menu_id UNION ALL SELECT 902 UNION ALL SELECT 368) m ON 1 = 1
WHERE r.id IN (2, 3, 4, 5, 6) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 通知公告：系统管理下维护、发布（超级管理员、租户管理员；普通用户只读），员工在工作台和「全部公告」中查看发给自己的公告
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(353, 1, '通知公告', 'C', 'notice', 'system/notice/index', 'sys:notice:list', 'NotificationOutlined', 14, 1, 1, NOW(), NOW()),
(3530, 353, '公告新增', 'F', NULL, NULL, 'sys:notice:add', NULL, 1, 1, 1, NOW(), NOW()),
(3531, 353, '公告修改', 'F', NULL, NULL, 'sys:notice:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3532, 353, '公告删除', 'F', NULL, NULL, 'sys:notice:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3533, 353, '公告发布与撤回', 'F', NULL, NULL, 'sys:notice:publish', NULL, 4, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 1 AS menu_id UNION ALL SELECT 2, 353
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(188, '公告状态', 'sys_notice_status', 'SYSTEM', 1, '通知公告的状态：草稿、已发布、已撤回', NOW(), NOW()),
(189, '公告发布范围', 'sys_notice_scope', 'SYSTEM', 1, '通知公告发给全员、指定部门（含下级部门）或指定角色', NOW(), NOW()),
(190, '公告类型', 'sys_notice_type', 'BUSINESS', 1, '通知公告的分类', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_notice_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('sys_notice_status', '已发布', 'PUBLISHED', 'green', 2, 1, NOW(), NOW()),
('sys_notice_status', '已撤回', 'WITHDRAWN', 'orange', 3, 1, NOW(), NOW()),
('sys_notice_scope', '全员', 'ALL', 'blue', 1, 1, NOW(), NOW()),
('sys_notice_scope', '指定部门', 'DEPT', 'cyan', 2, 1, NOW(), NOW()),
('sys_notice_scope', '指定角色', 'ROLE', 'purple', 3, 1, NOW(), NOW()),
('sys_notice_type', '通知', 'NOTICE', 'blue', 1, 1, NOW(), NOW()),
('sys_notice_type', '公告', 'ANNOUNCE', 'green', 2, 1, NOW(), NOW()),
('sys_notice_type', '制度', 'POLICY', 'purple', 3, 1, NOW(), NOW()),
('sys_notice_type', '活动', 'EVENT', 'orange', 4, 1, NOW(), NOW());

-- 现金银行：其他收入单、费用支出单（明细为收支类别，确认后生成凭证，进入出纳日记账和资金日报），收支类别（每个租户一份，标明收 / 支并关联科目）；
-- 账户调拨的手续费承担方（转出 / 转入账户）
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('660206', '业务招待费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW()),
('660207', '水电费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW()),
('660208', '租赁费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW()),
('660209', '快递费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW());
INSERT INTO fin_io_category (name, direction, account_id, sort, status, create_time, update_time) VALUES
('废料销售', 'IN', (SELECT a.id FROM fin_account a WHERE a.code = '6051' AND a.tenant_id = 1), 1, 1, NOW(), NOW()),
('佣金收入', 'IN', (SELECT a.id FROM fin_account a WHERE a.code = '6051' AND a.tenant_id = 1), 2, 1, NOW(), NOW()),
('房租收入', 'IN', (SELECT a.id FROM fin_account a WHERE a.code = '6051' AND a.tenant_id = 1), 3, 1, NOW(), NOW()),
('其他收入', 'IN', (SELECT a.id FROM fin_account a WHERE a.code = '6301' AND a.tenant_id = 1), 9, 1, NOW(), NOW()),
('差旅费', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660204' AND a.tenant_id = 1), 1, 1, NOW(), NOW()),
('业务招待费', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660206' AND a.tenant_id = 1), 2, 1, NOW(), NOW()),
('办公费', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660202' AND a.tenant_id = 1), 3, 1, NOW(), NOW()),
('房租', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660208' AND a.tenant_id = 1), 4, 1, NOW(), NOW()),
('水电费', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660207' AND a.tenant_id = 1), 5, 1, NOW(), NOW()),
('快递费', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660209' AND a.tenant_id = 1), 6, 1, NOW(), NOW()),
('工资', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '660201' AND a.tenant_id = 1), 7, 1, NOW(), NOW()),
('银行手续费', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '6603' AND a.tenant_id = 1), 8, 1, NOW(), NOW()),
('其他支出', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '6711' AND a.tenant_id = 1), 9, 1, NOW(), NOW());
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(201, '收支方向', 'fin_fund_direction', 'SYSTEM', 1, '收支类别、其他收支单的方向：收入、支出', NOW(), NOW()),
(202, '收支单状态', 'fin_io_status', 'SYSTEM', 1, '其他收入单、费用支出单的状态', NOW(), NOW()),
(203, '关联单位类型', 'fin_io_partner_type', 'SYSTEM', 1, '其他收支单的关联单位：客户、供应商或其他', NOW(), NOW()),
(204, '手续费支付账户', 'fin_fee_bearer', 'SYSTEM', 1, '账户调拨的手续费由转出账户还是转入账户支付', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_fund_direction', '收入', 'IN', 'green', 1, 1, NOW(), NOW()),
('fin_fund_direction', '支出', 'OUT', 'orange', 2, 1, NOW(), NOW()),
('fin_io_status', '待确认', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_io_status', '已确认', 'CONFIRMED', 'green', 2, 1, NOW(), NOW()),
('fin_io_partner_type', '客户', 'CUSTOMER', 'blue', 1, 1, NOW(), NOW()),
('fin_io_partner_type', '供应商', 'SUPPLIER', 'purple', 2, 1, NOW(), NOW()),
('fin_io_partner_type', '其他', 'OTHER', 'default', 3, 1, NOW(), NOW()),
('fin_fee_bearer', '转出账户支付', 'FROM', 'default', 1, 1, NOW(), NOW()),
('fin_fee_bearer', '转入账户支付', 'TO', 'default', 2, 1, NOW(), NOW()),
('fin_voucher_source', '其他收支', 'INCOME_EXPENSE', 'lime', 22, 1, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(391, 933, '其他收支', 'C', 'income-expense', 'fin/income-expense/index', 'fin:io:list', 'DollarCircleOutlined', 7, 1, 1, NOW(), NOW()),
(3910, 391, '收支单新增', 'F', NULL, NULL, 'fin:io:add', NULL, 1, 1, 1, NOW(), NOW()),
(3911, 391, '收支单修改', 'F', NULL, NULL, 'fin:io:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3912, 391, '收支单删除', 'F', NULL, NULL, 'fin:io:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3913, 391, '收支单确认', 'F', NULL, NULL, 'fin:io:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(3914, 391, '收支类别维护', 'F', NULL, NULL, 'fin:io:category', NULL, 5, 1, 1, NOW(), NOW());
-- 普通用户、总经理只读；会计、出纳全部
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 391 AS menu_id UNION ALL SELECT 3, 391 UNION ALL SELECT 9, 391 UNION ALL SELECT 9, 3910 UNION ALL SELECT 9, 3911 UNION ALL SELECT 9, 3912 UNION ALL SELECT 9, 3913
    UNION ALL SELECT 9, 3914 UNION ALL SELECT 10, 391 UNION ALL SELECT 10, 3910 UNION ALL SELECT 10, 3911 UNION ALL SELECT 10, 3912 UNION ALL SELECT 10, 3913
    UNION ALL SELECT 10, 3914
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 会计可查看供应链的采购单据
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 9 AS role_id, 5 AS menu_id UNION ALL SELECT 9, 901
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

ALTER TABLE sys_dept AUTO_INCREMENT = 200;
ALTER TABLE sys_user AUTO_INCREMENT = 100;
ALTER TABLE sys_role AUTO_INCREMENT = 100;
ALTER TABLE sys_menu AUTO_INCREMENT = 10000;
ALTER TABLE sys_dict_type AUTO_INCREMENT = 300;
ALTER TABLE wms_warehouse AUTO_INCREMENT = 100;
ALTER TABLE wms_attribute AUTO_INCREMENT = 100;
ALTER TABLE scm_supplier AUTO_INCREMENT = 100;
ALTER TABLE scm_customer AUTO_INCREMENT = 100;
ALTER TABLE fin_account AUTO_INCREMENT = 1000;

-- 应收应付（issue #50）：收付款单确认按钮（业务参数「收付款先存草稿」开启时，草稿确认后才生效），有新增权限的内置角色（会计、出纳）同时授权
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1853, 185, '收款单确认', 'F', NULL, NULL, 'fin:receipt:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1863, 186, '付款单确认', 'F', NULL, NULL, 'fin:payment:confirm', NULL, 4, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 9 AS role_id, 1853 AS menu_id UNION ALL SELECT 9, 1863 UNION ALL SELECT 10, 1853 UNION ALL SELECT 10, 1863
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 进销存单据草稿（issue #68）：采购入库 / 退货、销售出库 / 退货的审核按钮（业务参数「进销存单据先存草稿」开启时，草稿审核后才出入库、生成应收应付），
-- 有新增权限的内置角色（采购员、仓库管理员）和会计同时授权
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1411, 141, '采购入库审核', 'F', NULL, NULL, 'scm:purchaseIn:post', NULL, 2, 1, 1, NOW(), NOW()),
(1421, 142, '采购退货审核', 'F', NULL, NULL, 'scm:purchaseReturn:post', NULL, 2, 1, 1, NOW(), NOW()),
(1441, 144, '销售出库审核', 'F', NULL, NULL, 'scm:salesOut:post', NULL, 2, 1, 1, NOW(), NOW()),
(1451, 145, '销售退货审核', 'F', NULL, NULL, 'scm:salesReturn:post', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 4 AS role_id, 1411 AS menu_id UNION ALL SELECT 4, 1421 UNION ALL SELECT 6, 1411 UNION ALL SELECT 6, 1421 UNION ALL SELECT 6, 1441 UNION ALL SELECT 6, 1451 UNION ALL SELECT 9, 1411
    UNION ALL SELECT 9, 1421 UNION ALL SELECT 9, 1441 UNION ALL SELECT 9, 1451
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 销售出库、销售退货、采购退货的运费（issue #72、#73）：付给承运商的方式（现付生成费用支出单 / 月结生成承运商应付），
-- 应收应付来源增加三类运费；收支类别增加内置的「销售运费」「代垫运费」（现付运费自动生成费用支出单时使用，科目与凭证科目设置的默认值一致）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(207, '运费结算方式', 'scm_freight_settle', 'SYSTEM', 1, '销售出库、采购退货的运费付给承运商的方式：现付（生成费用支出单）、月结（生成承运商应付单）', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_freight_settle', '现付', 'CASH', 'green', 1, 1, NOW(), NOW()),
('scm_freight_settle', '月结', 'MONTHLY', 'blue', 2, 1, NOW(), NOW()),
('fin_bill_source', '销售运费', 'SALES_FREIGHT', 'cyan', 16, 1, NOW(), NOW()),
('fin_bill_source', '销售退货运费', 'SALES_RETURN_FREIGHT', 'cyan', 17, 1, NOW(), NOW()),
('fin_bill_source', '采购退货运费', 'PURCHASE_RETURN_FREIGHT', 'cyan', 18, 1, NOW(), NOW());
INSERT INTO fin_io_category (name, code, direction, account_id, sort, status, remark, create_time, update_time) VALUES
('销售运费', 'SALES_FREIGHT', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '6601' AND a.tenant_id = 1), 10, 1, '销售出库本公司承担、现付的运费', NOW(), NOW()),
('代垫运费', 'FREIGHT_ADVANCE', 'OUT', (SELECT a.id FROM fin_account a WHERE a.code = '1221' AND a.tenant_id = 1), 11, 1, '销售出库、采购退货替往来单位垫付、现付的运费', NOW(), NOW());

-- 物料分类（多级）：在物料管理页左侧的分类树中维护（按钮「物料分类维护」wms:material:category，授权给有物料修改权限的角色）。
-- 新租户由仓库模块的 WmsTenantInitializer 复制平台的分类；原材料、半成品、成品、低值易耗品为内置分类（凭证科目、低值易耗品领用按编码使用）
INSERT INTO wms_material_category (parent_id, ancestors, code, name, sort, status, builtin, create_time, update_time) VALUES
(0, '0', 'RAW', '原材料', 1, 1, 1, NOW(), NOW()),
(0, '0', 'SEMI', '半成品', 2, 1, 1, NOW(), NOW()),
(0, '0', 'FINISHED', '成品', 3, 1, 1, NOW(), NOW()),
(0, '0', 'PACKING', '包装材料', 4, 1, 0, NOW(), NOW()),
(0, '0', 'OTHER', '其他', 5, 1, 0, NOW(), NOW()),
(0, '0', 'LOW_VALUE', '低值易耗品', 6, 1, 1, NOW(), NOW());
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1374, 137, '物料分类维护', 'F', NULL, NULL, 'wms:material:category', NULL, 6, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 1374 FROM sys_role_menu rm
WHERE rm.menu_id = 1371 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 1374);

-- 发票类型（开票类型，业务字典，每个租户一份，issue #82）：订单、直接开单的出入库表头选择后明细税率按它统一调整，
-- 扩展值为税率（%），租户可自行增加；明细未填写税率时取发票类型的税率，没有时取业务参数「默认税率」
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(208, '发票类型', 'scm_invoice_kind', 'BUSINESS', 1, '进销存单据表头的发票类型（开票类型），扩展值为税率（%）：选择后明细税率统一调整为该税率', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, ext_value, create_time, update_time) VALUES
('scm_invoice_kind', '增值税专票 13%', 'SPECIAL_13', 'red', 1, 1, '13', NOW(), NOW()),
('scm_invoice_kind', '增值税专票 9%', 'SPECIAL_9', 'volcano', 2, 1, '9', NOW(), NOW()),
('scm_invoice_kind', '增值税专票 6%', 'SPECIAL_6', 'orange', 3, 1, '6', NOW(), NOW()),
('scm_invoice_kind', '增值税普票 13%', 'NORMAL_13', 'blue', 4, 1, '13', NOW(), NOW()),
('scm_invoice_kind', '增值税普票 3%', 'NORMAL_3', 'geekblue', 5, 1, '3', NOW(), NOW()),
('scm_invoice_kind', '增值税普票 1%', 'NORMAL_1', 'cyan', 6, 1, '1', NOW(), NOW()),
('scm_invoice_kind', '不开票', 'NONE', 'default', 7, 1, '0', NOW(), NOW());

-- 字典种类：业务字典由各租户自己维护（可新增取值），其余为系统字典；程序依赖的取值标记为内置（不能删除、不能改键值）
UPDATE sys_dict_type SET kind = 'BUSINESS' WHERE type IN ('wf_category', 'scm_customer_level', 'scm_carrier', 'fin_asset_category', 'fin_settle_method');
UPDATE sys_dict_data SET builtin = 1
WHERE dict_type NOT IN (SELECT type FROM sys_dict_type WHERE kind = 'BUSINESS')
   OR (dict_type = 'fin_settle_method' AND dict_value IN ('TRANSFER', 'NOTE'))
   OR dict_type = 'wf_category'
   OR (dict_type = 'scm_customer_level' AND dict_value = 'POTENTIAL')
   OR (dict_type = 'fin_asset_category' AND dict_value = 'ELECTRONIC');

-- 往来单位合并与关联（issue #95）：客户合并、供应商合并按钮（销售经理、采购员），应收应付对冲的新增、删除按钮（会计）；
-- 凭证来源「应收应付对冲」。供应商的关联客户（scm_supplier.customer_id）、已合并标记（merged_to）、对冲单表见建表语句
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1474, 147, '供应商合并', 'F', NULL, NULL, 'scm:supplier:merge', NULL, 5, 1, 1, NOW(), NOW()),
(1494, 149, '客户合并', 'F', NULL, NULL, 'scm:customer:merge', NULL, 5, 1, 1, NOW(), NOW()),
(1835, 183, '应收应付对冲', 'F', NULL, NULL, 'fin:netting:add', NULL, 10, 1, 1, NOW(), NOW()),
(1836, 183, '删除对冲单', 'F', NULL, NULL, 'fin:netting:remove', NULL, 11, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 4 AS role_id, 1474 AS menu_id UNION ALL SELECT 9, 1835 UNION ALL SELECT 9, 1836
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('fin_voucher_source', '应收应付对冲', 'NETTING', 'volcano', 23, 1, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 跨境电商：菜单、字典、内置角色授权、示例数据

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(981, 17,  '店铺与商品', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(982, 17,  '订单与售后', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(983, 17,  '库存与物流', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(421, 981, '平台店铺', 'C', 'store', 'ecom/store/index', 'ecom:store:list', 'ShopOutlined', 1, 1, 1, NOW(), NOW()),
(422, 981, '在线商品', 'C', 'listing', 'ecom/listing/index', 'ecom:listing:list', 'TagsOutlined', 2, 1, 1, NOW(), NOW()),
(423, 982, '平台订单', 'C', 'order', 'ecom/order/index', 'ecom:order:list', 'ShoppingOutlined', 1, 1, 1, NOW(), NOW()),
(424, 982, '退货退款', 'C', 'return', 'ecom/return/index', 'ecom:return:list', 'RollbackOutlined', 2, 1, 1, NOW(), NOW()),
(425, 983, '头程货件', 'C', 'inbound', 'ecom/inbound/index', 'ecom:inbound:list', 'SendOutlined', 1, 1, 1, NOW(), NOW()),
(426, 983, '平台仓库存', 'C', 'platform-stock', 'ecom/platform-stock/index', 'ecom:platformStock:list', 'DatabaseOutlined', 2, 1, 1, NOW(), NOW()),
(427, 983, '补货建议', 'C', 'replenish', 'ecom/replenish/index', 'ecom:replenish:list', 'AlertOutlined', 3, 1, 1, NOW(), NOW()),
(428, 983, '物流渠道', 'C', 'logistics', 'ecom/logistics/index', 'ecom:logistics:list', 'CarOutlined', 4, 1, 1, NOW(), NOW()),
(420, 984, '电商看板', 'C', 'dashboard', 'ecom/dashboard/index', 'ecom:dashboard:list', 'DashboardOutlined', 1, 1, 1, NOW(), NOW()),
(429, 984, '平台结算', 'C', 'settlement', 'ecom/settlement/index', 'ecom:settlement:list', 'TransactionOutlined', 2, 1, 1, NOW(), NOW()),
(430, 984, '广告数据', 'C', 'ad', 'ecom/ad/index', 'ecom:ad:list', 'SoundOutlined', 3, 1, 1, NOW(), NOW()),
(431, 984, '利润分析', 'C', 'profit', 'ecom/profit/index', 'ecom:report:list', 'FundOutlined', 4, 1, 1, NOW(), NOW()),
(4210, 421, '店铺新增', 'F', NULL, NULL, 'ecom:store:add', NULL, 1, 1, 1, NOW(), NOW()),
(4211, 421, '店铺修改', 'F', NULL, NULL, 'ecom:store:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4212, 421, '店铺删除', 'F', NULL, NULL, 'ecom:store:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4213, 421, '同步订单', 'F', NULL, NULL, 'ecom:store:sync', NULL, 4, 1, 1, NOW(), NOW()),
(4220, 422, '在线商品新增', 'F', NULL, NULL, 'ecom:listing:add', NULL, 1, 1, 1, NOW(), NOW()),
(4221, 422, '在线商品修改', 'F', NULL, NULL, 'ecom:listing:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4222, 422, '在线商品删除', 'F', NULL, NULL, 'ecom:listing:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4223, 422, '在线商品导入', 'F', NULL, NULL, 'ecom:listing:import', NULL, 4, 1, 1, NOW(), NOW()),
(4230, 423, '订单导入', 'F', NULL, NULL, 'ecom:order:import', NULL, 1, 1, 1, NOW(), NOW()),
(4231, 423, '订单修改', 'F', NULL, NULL, 'ecom:order:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4232, 423, '订单出库', 'F', NULL, NULL, 'ecom:order:outbound', NULL, 3, 1, 1, NOW(), NOW()),
(4233, 423, '订单发货', 'F', NULL, NULL, 'ecom:order:ship', NULL, 4, 1, 1, NOW(), NOW()),
(4234, 423, '订单取消', 'F', NULL, NULL, 'ecom:order:cancel', NULL, 5, 1, 1, NOW(), NOW()),
(4240, 424, '退货退款新增', 'F', NULL, NULL, 'ecom:return:add', NULL, 1, 1, 1, NOW(), NOW()),
(4241, 424, '退货入库', 'F', NULL, NULL, 'ecom:return:receive', NULL, 2, 1, 1, NOW(), NOW()),
(4242, 424, '登记退款', 'F', NULL, NULL, 'ecom:return:refund', NULL, 3, 1, 1, NOW(), NOW()),
(4243, 424, '退货退款删除', 'F', NULL, NULL, 'ecom:return:remove', NULL, 4, 1, 1, NOW(), NOW()),
(4250, 425, '货件新增', 'F', NULL, NULL, 'ecom:inbound:add', NULL, 1, 1, 1, NOW(), NOW()),
(4251, 425, '货件修改', 'F', NULL, NULL, 'ecom:inbound:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4252, 425, '货件删除', 'F', NULL, NULL, 'ecom:inbound:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4253, 425, '货件发运', 'F', NULL, NULL, 'ecom:inbound:ship', NULL, 4, 1, 1, NOW(), NOW()),
(4254, 425, '货件签收', 'F', NULL, NULL, 'ecom:inbound:receive', NULL, 5, 1, 1, NOW(), NOW()),
(4260, 426, '平台库存导入', 'F', NULL, NULL, 'ecom:platformStock:import', NULL, 1, 1, 1, NOW(), NOW()),
(4280, 428, '渠道新增', 'F', NULL, NULL, 'ecom:logistics:add', NULL, 1, 1, 1, NOW(), NOW()),
(4281, 428, '渠道修改', 'F', NULL, NULL, 'ecom:logistics:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4282, 428, '渠道删除', 'F', NULL, NULL, 'ecom:logistics:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4290, 429, '结算导入', 'F', NULL, NULL, 'ecom:settlement:import', NULL, 1, 1, 1, NOW(), NOW()),
(4291, 429, '结算确认', 'F', NULL, NULL, 'ecom:settlement:confirm', NULL, 2, 1, 1, NOW(), NOW()),
(4292, 429, '结算删除', 'F', NULL, NULL, 'ecom:settlement:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4300, 430, '广告导入', 'F', NULL, NULL, 'ecom:ad:import', NULL, 1, 1, 1, NOW(), NOW()),
(4301, 430, '广告删除', 'F', NULL, NULL, 'ecom:ad:remove', NULL, 2, 1, 1, NOW(), NOW());

-- 系统字典（代码按取值判断）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(211, '电商平台', 'ecom_platform', 'SYSTEM', 1, '店铺所属平台', NOW(), NOW()),
(212, '配送方式', 'ecom_fulfillment', 'SYSTEM', 1, '平台仓配送（FBA）/ 自发货', NOW(), NOW()),
(213, '在线商品状态', 'ecom_listing_status', 'SYSTEM', 1, '在线商品的状态', NOW(), NOW()),
(214, '平台订单状态', 'ecom_order_status', 'SYSTEM', 1, '平台侧的订单状态', NOW(), NOW()),
(215, '订单处理状态', 'ecom_local_status', 'SYSTEM', 1, '平台订单在系统内的处理进度', NOW(), NOW()),
(216, '货件状态', 'ecom_inbound_status', 'SYSTEM', 1, '头程货件的状态', NOW(), NOW()),
(217, '结算单状态', 'ecom_settlement_status', 'SYSTEM', 1, '平台结算单的状态', NOW(), NOW()),
(218, '结算明细类型', 'ecom_settlement_item_type', 'SYSTEM', 1, '结算报告中的收入与费用类型', NOW(), NOW()),
(219, '退货退款类型', 'ecom_return_type', 'SYSTEM', 1, '仅退款 / 退货退款', NOW(), NOW()),
(220, '退货退款状态', 'ecom_return_status', 'SYSTEM', 1, '退货退款单的状态', NOW(), NOW()),
(221, '补货紧急程度', 'ecom_replenish_level', 'SYSTEM', 1, '补货建议的紧急程度', NOW(), NOW()),
(222, '电商站点', 'ecom_marketplace', 'BUSINESS', 1, '店铺所在的站点 / 市场，可自行扩展', NOW(), NOW()),
(223, '运输方式', 'ecom_transport_mode', 'BUSINESS', 1, '国际物流渠道的运输方式', NOW(), NOW()),
(224, '退货原因', 'ecom_return_reason', 'BUSINESS', 1, '退货退款的原因', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_platform', '亚马逊', 'AMAZON', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_platform', '速卖通', 'ALIEXPRESS', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_platform', 'eBay', 'EBAY', 'blue', 3, 1, 1, NOW(), NOW()),
('ecom_platform', 'Shopee', 'SHOPEE', 'volcano', 4, 1, 1, NOW(), NOW()),
('ecom_platform', 'Lazada', 'LAZADA', 'purple', 5, 1, 1, NOW(), NOW()),
('ecom_platform', 'Shopify', 'SHOPIFY', 'green', 6, 1, 1, NOW(), NOW()),
('ecom_platform', 'TikTok Shop', 'TIKTOK', 'default', 7, 1, 1, NOW(), NOW()),
('ecom_platform', 'Temu', 'TEMU', 'orange', 8, 1, 1, NOW(), NOW()),
('ecom_platform', '沃尔玛', 'WALMART', 'blue', 9, 1, 1, NOW(), NOW()),
('ecom_platform', 'Ozon', 'OZON', 'geekblue', 10, 1, 1, NOW(), NOW()),
('ecom_platform', '其他', 'OTHER', 'default', 11, 1, 1, NOW(), NOW()),
('ecom_fulfillment', '平台仓配送', 'PLATFORM', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_fulfillment', '自发货', 'SELLER', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_listing_status', '在售', 'ACTIVE', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_listing_status', '下架', 'INACTIVE', 'default', 2, 1, 1, NOW(), NOW()),
('ecom_listing_status', '信息不完整', 'INCOMPLETE', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_listing_status', '已删除', 'DELETED', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_order_status', '待付款', 'PENDING', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_order_status', '待发货', 'UNSHIPPED', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_order_status', '已发货', 'SHIPPED', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_order_status', '已取消', 'CANCELLED', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_order_status', '已退款', 'REFUNDED', 'purple', 5, 1, 1, NOW(), NOW()),
('ecom_local_status', '待处理', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_local_status', '异常', 'EXCEPTION', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_local_status', '已出库', 'OUTBOUND', 'processing', 3, 1, 1, NOW(), NOW()),
('ecom_local_status', '已发货', 'SHIPPED', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_local_status', '已关闭', 'CLOSED', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_inbound_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_inbound_status', '审批中', 'APPROVING', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_inbound_status', '已发运', 'SHIPPED', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_inbound_status', '已签收', 'RECEIVED', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_inbound_status', '已取消', 'CANCELLED', 'red', 5, 1, 1, NOW(), NOW()),
('ecom_settlement_status', '已导入', 'IMPORTED', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_settlement_status', '已确认', 'CONFIRMED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '订单收入', 'ORDER', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '退款', 'REFUND', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '平台佣金', 'COMMISSION', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '配送费', 'FULFILLMENT', 'volcano', 4, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '仓储费', 'STORAGE', 'gold', 5, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '广告费', 'AD', 'purple', 6, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '其他费用', 'OTHER', 'default', 7, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '调整', 'ADJUSTMENT', 'cyan', 8, 1, 1, NOW(), NOW()),
('ecom_return_type', '仅退款', 'REFUND_ONLY', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_return_type', '退货退款', 'RETURN_REFUND', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_return_status', '待处理', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_return_status', '已入库', 'RECEIVED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_return_status', '已退款', 'REFUNDED', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_return_status', '已关闭', 'CLOSED', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_replenish_level', '充足', 'OK', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_replenish_level', '预警', 'WARN', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_replenish_level', '紧急', 'URGENT', 'red', 3, 1, 1, NOW(), NOW()),
-- 业务字典：每个租户一份，可自行增加,
('ecom_marketplace', '美国', 'US', 'default', 1, 1, 0, NOW(), NOW()),
('ecom_marketplace', '加拿大', 'CA', 'default', 2, 1, 0, NOW(), NOW()),
('ecom_marketplace', '墨西哥', 'MX', 'default', 3, 1, 0, NOW(), NOW()),
('ecom_marketplace', '巴西', 'BR', 'default', 4, 1, 0, NOW(), NOW()),
('ecom_marketplace', '英国', 'UK', 'default', 5, 1, 0, NOW(), NOW()),
('ecom_marketplace', '德国', 'DE', 'default', 6, 1, 0, NOW(), NOW()),
('ecom_marketplace', '法国', 'FR', 'default', 7, 1, 0, NOW(), NOW()),
('ecom_marketplace', '意大利', 'IT', 'default', 8, 1, 0, NOW(), NOW()),
('ecom_marketplace', '西班牙', 'ES', 'default', 9, 1, 0, NOW(), NOW()),
('ecom_marketplace', '荷兰', 'NL', 'default', 10, 1, 0, NOW(), NOW()),
('ecom_marketplace', '波兰', 'PL', 'default', 11, 1, 0, NOW(), NOW()),
('ecom_marketplace', '瑞典', 'SE', 'default', 12, 1, 0, NOW(), NOW()),
('ecom_marketplace', '阿联酋', 'AE', 'default', 13, 1, 0, NOW(), NOW()),
('ecom_marketplace', '沙特', 'SA', 'default', 14, 1, 0, NOW(), NOW()),
('ecom_marketplace', '日本', 'JP', 'default', 15, 1, 0, NOW(), NOW()),
('ecom_marketplace', '澳大利亚', 'AU', 'default', 16, 1, 0, NOW(), NOW()),
('ecom_marketplace', '新加坡', 'SG', 'default', 17, 1, 0, NOW(), NOW()),
('ecom_marketplace', '马来西亚', 'MY', 'default', 18, 1, 0, NOW(), NOW()),
('ecom_marketplace', '泰国', 'TH', 'default', 19, 1, 0, NOW(), NOW()),
('ecom_marketplace', '菲律宾', 'PH', 'default', 20, 1, 0, NOW(), NOW()),
('ecom_marketplace', '越南', 'VN', 'default', 21, 1, 0, NOW(), NOW()),
('ecom_marketplace', '印度尼西亚', 'ID', 'default', 22, 1, 0, NOW(), NOW()),
('ecom_marketplace', '全球站', 'GLOBAL', 'default', 23, 1, 0, NOW(), NOW()),
('ecom_transport_mode', '空运', 'AIR', 'blue', 1, 1, 0, NOW(), NOW()),
('ecom_transport_mode', '海运', 'SEA', 'cyan', 2, 1, 0, NOW(), NOW()),
('ecom_transport_mode', '国际快递', 'EXPRESS', 'orange', 3, 1, 0, NOW(), NOW()),
('ecom_transport_mode', '铁路', 'RAIL', 'default', 4, 1, 0, NOW(), NOW()),
('ecom_transport_mode', '卡航', 'TRUCK', 'default', 5, 1, 0, NOW(), NOW()),
('ecom_transport_mode', '专线小包', 'LINE', 'green', 6, 1, 0, NOW(), NOW()),
('ecom_return_reason', '质量问题', 'QUALITY', 'red', 1, 1, 0, NOW(), NOW()),
('ecom_return_reason', '运输破损', 'DAMAGED', 'orange', 2, 1, 0, NOW(), NOW()),
('ecom_return_reason', '发错货', 'WRONG_ITEM', 'volcano', 3, 1, 0, NOW(), NOW()),
('ecom_return_reason', '与描述不符', 'NOT_AS_DESCRIBED', 'gold', 4, 1, 0, NOW(), NOW()),
('ecom_return_reason', '买家不想要', 'NOT_NEEDED', 'default', 5, 1, 0, NOW(), NOW()),
('ecom_return_reason', '送达太迟', 'LATE', 'purple', 6, 1, 0, NOW(), NOW()),
('ecom_return_reason', '其他', 'OTHER', 'default', 7, 1, 1, NOW(), NOW());

-- 流程分类增加「电商」；头程货件审批的示例流程（停用，发布后货件发运需审批）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('wf_category', '电商', 'ECOM', 'orange', 11, 1, 1, NOW(), NOW());

-- 内置角色：电商运营全部电商功能；总经理、普通用户只读；仓库管理员负责出库发货与货件发运签收；会计负责结算确认、利润分析
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 17 OR m.parent_id = 17 OR m.parent_id IN (981, 982, 983, 984) OR m.parent_id BETWEEN 420 AND 431)
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 17 OR m.parent_id = 17 OR m.parent_id IN (981, 982, 983, 984))
WHERE r.id IN (2, 3) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 6 AS role_id, 17 AS menu_id UNION ALL SELECT 6, 982 UNION ALL SELECT 6, 423 UNION ALL SELECT 6, 4232 UNION ALL SELECT 6, 4233
    UNION ALL SELECT 6, 983 UNION ALL SELECT 6, 425 UNION ALL SELECT 6, 4253 UNION ALL SELECT 6, 4254 UNION ALL SELECT 6, 426 UNION ALL SELECT 6, 4260
    UNION ALL SELECT 6, 427 UNION ALL SELECT 6, 428
    UNION ALL SELECT 9, 17 UNION ALL SELECT 9, 984 UNION ALL SELECT 9, 420 UNION ALL SELECT 9, 429 UNION ALL SELECT 9, 4290 UNION ALL SELECT 9, 4291
    UNION ALL SELECT 9, 4292 UNION ALL SELECT 9, 430 UNION ALL SELECT 9, 431 UNION ALL SELECT 9, 982 UNION ALL SELECT 9, 423
    UNION ALL SELECT 10, 17 UNION ALL SELECT 10, 984 UNION ALL SELECT 10, 429 UNION ALL SELECT 10, 4291
    UNION ALL SELECT 4, 17 UNION ALL SELECT 4, 983 UNION ALL SELECT 4, 427 UNION ALL SELECT 4, 426
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);
-- 电商运营、仓库管理员、会计可以使用 AI 问数查询电商数据（工具按用户自己的权限过滤）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r JOIN (SELECT 16 AS menu_id UNION ALL SELECT 360) m ON 1 = 1
WHERE r.id IN (5, 6, 9) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- 常用币种与示例汇率（当月，1 外币 = 汇率 × 人民币；请按实际在「币种汇率」中维护）
INSERT INTO fin_currency (code, name, symbol, is_base, status, sort, create_time, update_time) VALUES
('GBP', '英镑', '£', 0, 1, 6, NOW(), NOW()),
('CAD', '加元', 'C$', 0, 1, 7, NOW(), NOW()),
('AUD', '澳元', 'A$', 0, 1, 8, NOW(), NOW()),
('MXN', '墨西哥比索', 'MX$', 0, 1, 9, NOW(), NOW()),
('SGD', '新加坡元', 'S$', 0, 1, 10, NOW(), NOW()),
('AED', '阿联酋迪拉姆', 'AED', 0, 1, 11, NOW(), NOW()),
('SAR', '沙特里亚尔', 'SAR', 0, 1, 12, NOW(), NOW()),
('MYR', '马来西亚林吉特', 'RM', 0, 1, 13, NOW(), NOW()),
('THB', '泰铢', '฿', 0, 1, 14, NOW(), NOW()),
('PLN', '波兰兹罗提', 'zł', 0, 1, 15, NOW(), NOW()),
('SEK', '瑞典克朗', 'kr', 0, 1, 16, NOW(), NOW()),
('BRL', '巴西雷亚尔', 'R$', 0, 1, 17, NOW(), NOW());
INSERT INTO fin_exchange_rate (currency, period, rate, create_time, update_time) VALUES
('USD', YEAR(NOW()) * 100 + MONTH(NOW()), 7.100000, NOW(), NOW()),
('EUR', YEAR(NOW()) * 100 + MONTH(NOW()), 7.800000, NOW(), NOW()),
('GBP', YEAR(NOW()) * 100 + MONTH(NOW()), 9.100000, NOW(), NOW()),
('JPY', YEAR(NOW()) * 100 + MONTH(NOW()), 0.047000, NOW(), NOW()),
('HKD', YEAR(NOW()) * 100 + MONTH(NOW()), 0.910000, NOW(), NOW()),
('CAD', YEAR(NOW()) * 100 + MONTH(NOW()), 5.200000, NOW(), NOW()),
('AUD', YEAR(NOW()) * 100 + MONTH(NOW()), 4.700000, NOW(), NOW()),
('MXN', YEAR(NOW()) * 100 + MONTH(NOW()), 0.400000, NOW(), NOW()),
('SGD', YEAR(NOW()) * 100 + MONTH(NOW()), 5.400000, NOW(), NOW()),
('AED', YEAR(NOW()) * 100 + MONTH(NOW()), 1.930000, NOW(), NOW()),
('SAR', YEAR(NOW()) * 100 + MONTH(NOW()), 1.890000, NOW(), NOW()),
('MYR', YEAR(NOW()) * 100 + MONTH(NOW()), 1.600000, NOW(), NOW()),
('THB', YEAR(NOW()) * 100 + MONTH(NOW()), 0.210000, NOW(), NOW()),
('PLN', YEAR(NOW()) * 100 + MONTH(NOW()), 1.800000, NOW(), NOW()),
('SEK', YEAR(NOW()) * 100 + MONTH(NOW()), 0.680000, NOW(), NOW()),
('BRL', YEAR(NOW()) * 100 + MONTH(NOW()), 1.300000, NOW(), NOW());

-- 示例海外仓（平台仓）与物流渠道
INSERT INTO wms_warehouse (id, code, name, address, contact, sort, status, create_time, update_time) VALUES
(3, 'FBA-US', '美国 FBA 仓', 'Amazon FBA (US)', NULL, 3, 1, NOW(), NOW());
INSERT INTO ecom_logistics_channel (id, code, name, provider, transport_mode, days_min, days_max, status, remark, create_time, update_time) VALUES
(1, 'AIR-US', '美国空派', '示例货代', 'AIR', 10, 15, 1, '头程：空运 + 美国本土派送到 FBA', NOW(), NOW()),
(2, 'SEA-US', '美国海派', '示例货代', 'SEA', 35, 45, 1, '头程：海运 + 美国本土派送到 FBA', NOW(), NOW()),
(3, 'EXPRESS', '国际快递', '示例快递', 'EXPRESS', 5, 8, 1, '自发货尾程', NOW(), NOW());
INSERT INTO ecom_logistics_rate (channel_id, country, min_weight, max_weight, base_fee, per_kg, create_time, update_time) VALUES
(1, 'US', 0, 100, 0, 48.00, NOW(), NOW()),
(1, 'US', 100, NULL, 0, 42.00, NOW(), NOW()),
(2, 'US', 0, NULL, 0, 12.00, NOW(), NOW()),
(3, 'US', 0, 0.5, 60.00, 0, NOW(), NOW()),
(3, 'US', 0.5, 2, 60.00, 80.00, NOW(), NOW()),
(3, 'US', 2, NULL, 80.00, 70.00, NOW(), NOW()),
(3, '*', 0, 0.5, 70.00, 0, NOW(), NOW()),
(3, '*', 0.5, NULL, 70.00, 95.00, NOW(), NOW());
ALTER TABLE ecom_logistics_channel AUTO_INCREMENT = 100;

-- 销售订单（批发 / B2B 客户订货；平台订单出库自动生成的隐式订单也在这里查看）：电商运营全部，总经理、仓库、会计、普通用户只读；
-- 总经理负责通知公告
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 2 AS role_id, 143 AS menu_id UNION ALL SELECT 3, 143 UNION ALL SELECT 3, 1433 UNION ALL SELECT 3, 1434 UNION ALL SELECT 6, 143 UNION ALL SELECT 9, 143
    UNION ALL SELECT 5, 143 UNION ALL SELECT 5, 1430 UNION ALL SELECT 5, 1431 UNION ALL SELECT 5, 1432 UNION ALL SELECT 5, 1433 UNION ALL SELECT 5, 1434 UNION ALL SELECT 5, 1436
    UNION ALL SELECT 5, 5 UNION ALL SELECT 5, 902 UNION ALL SELECT 5, 144 UNION ALL SELECT 5, 145 UNION ALL SELECT 5, 903 UNION ALL SELECT 5, 149
    UNION ALL SELECT 3, 1 UNION ALL SELECT 3, 353 UNION ALL SELECT 3, 3530 UNION ALL SELECT 3, 3531 UNION ALL SELECT 3, 3532 UNION ALL SELECT 3, 3533
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 渠道同步日志的类型
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(225, '渠道同步类型', 'ecom_sync_type', 'SYSTEM', 1, '渠道同步日志的类型', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_sync_type', '拉取订单', 'ORDERS', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_sync_type', '回传发货', 'SHIP_CONFIRM', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_sync_type', '回传库存', 'INVENTORY_PUSH', 'cyan', 3, 1, 1, NOW(), NOW()),
('ecom_sync_type', '拉取商品', 'LISTINGS', 'purple', 4, 1, 1, NOW(), NOW()),
('ecom_sync_type', '平台仓库存', 'PLATFORM_STOCK', 'orange', 5, 1, 1, NOW(), NOW()),
('ecom_sync_type', '授权', 'AUTH', 'gold', 6, 1, 1, NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES ('ecom_platform', '美客多', 'MERCADO_LIBRE', 'gold', 12, 1, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 渠道同步中心：库存回传水位、同步日志索引、渠道接入页面与店铺按钮
ALTER TABLE ecom_listing ADD COLUMN last_pushed_qty DECIMAL(18, 4) DEFAULT NULL COMMENT '最近一次回传给平台的可售数量（只回传和上次不同的）';
ALTER TABLE ecom_listing ADD COLUMN last_pushed_time DATETIME DEFAULT NULL COMMENT '最近一次回传库存的时间';
CREATE INDEX idx_ecom_sync_log_type ON ecom_sync_log (store_id, sync_type);
CREATE INDEX idx_ecom_sync_log_time ON ecom_sync_log (start_time);

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(440, 981, '渠道接入', 'C', 'channel', 'ecom/channel/index', 'ecom:channel:list', 'ApiOutlined', 3, 1, 1, NOW(), NOW()),
(4400, 421, '店铺授权', 'F', NULL, NULL, 'ecom:store:auth', NULL, 5, 1, 1, NOW(), NOW()),
(4401, 421, '同步商品', 'F', NULL, NULL, 'ecom:store:syncListing', NULL, 6, 1, 1, NOW(), NOW()),
(4402, 421, '回传库存', 'F', NULL, NULL, 'ecom:store:push', NULL, 7, 1, 1, NOW(), NOW());

-- 电商运营：渠道接入与店铺授权、同步商品、回传库存；普通用户、总经理只读渠道接入
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 440 AS menu_id UNION ALL SELECT 5, 4400 UNION ALL SELECT 5, 4401 UNION ALL SELECT 5, 4402
    UNION ALL SELECT 2, 440 UNION ALL SELECT 3, 440
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- ---------------------------------------------------------------- 跨境电商：订单作业闭环（审单规则、审核与缺货、挂起、合并 / 拆分包裹、物流商面单、批量打单、扫描发货）

-- 平台订单：挂起、截单、重量、命中规则、审核时间
ALTER TABLE ecom_order ADD COLUMN hold_reason VARCHAR(255) DEFAULT NULL COMMENT '挂起原因（审单规则要求人工审核或人工拦截）';
ALTER TABLE ecom_order ADD COLUMN hold_prev_status VARCHAR(16) DEFAULT NULL COMMENT '挂起前的处理状态，取消挂起后恢复';
ALTER TABLE ecom_order ADD COLUMN hold_rule_id BIGINT DEFAULT NULL COMMENT '挂起的审单规则（人工挂起为空）';
ALTER TABLE ecom_order ADD COLUMN plan_ship_time DATETIME DEFAULT NULL COMMENT '计划发货时间（按审单规则的截单时间推算）';
ALTER TABLE ecom_order ADD COLUMN weight DECIMAL(18, 3) DEFAULT NULL COMMENT '预估重量（kg），审单规则按重量区间匹配';
ALTER TABLE ecom_order ADD COLUMN matched_rules VARCHAR(255) DEFAULT NULL COMMENT '命中的审单规则名称';
ALTER TABLE ecom_order ADD COLUMN audit_time DATETIME DEFAULT NULL COMMENT '审核通过时间';
CREATE INDEX idx_ecom_order_ship_by ON ecom_order (ship_by);

-- 物流渠道：对接物流商面单接口
ALTER TABLE ecom_logistics_channel ADD COLUMN carrier VARCHAR(16) DEFAULT NULL COMMENT '对接的物流商(字典 ecom_carrier) FOURPX递四方 YUNEXPRESS云途，为空未对接（手工填写运单号）';
ALTER TABLE ecom_logistics_channel ADD COLUMN carrier_service VARCHAR(64) DEFAULT NULL COMMENT '物流商产品 / 运输方式代码（4PX logistics_product_code、云途 ShippingMethodCode）';
ALTER TABLE ecom_logistics_channel ADD COLUMN carrier_credentials TEXT COMMENT '物流商授权（JSON，加密保存，enc: 前缀）';
ALTER TABLE ecom_logistics_channel ADD COLUMN carrier_sandbox TINYINT NOT NULL DEFAULT 0 COMMENT '物流商接口环境 1测试 0正式';
ALTER TABLE ecom_logistics_channel ADD COLUMN carrier_api_url VARCHAR(255) DEFAULT NULL COMMENT '自定义物流商接口地址，为空按环境取默认地址';

DROP TABLE IF EXISTS ecom_order_rule;
CREATE TABLE ecom_order_rule (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name          VARCHAR(64)    NOT NULL COMMENT '规则名称',
    priority      INT            NOT NULL DEFAULT 100 COMMENT '优先级，数字小的先匹配',
    status        TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    store_ids     VARCHAR(500)            DEFAULT NULL COMMENT '条件：店铺 ID（逗号分隔），为空不限',
    platforms     VARCHAR(255)            DEFAULT NULL COMMENT '条件：平台（逗号分隔）',
    fulfillment   VARCHAR(16)             DEFAULT NULL COMMENT '条件：配送方式(字典 ecom_fulfillment)',
    countries     VARCHAR(500)            DEFAULT NULL COMMENT '条件：收件国家 / 地区代码（逗号分隔）',
    skus          VARCHAR(1000)           DEFAULT NULL COMMENT '条件：平台 SKU（逗号分隔，任一行命中）',
    material_ids  VARCHAR(500)            DEFAULT NULL COMMENT '条件：本地物料 ID（逗号分隔，任一行命中）',
    amount_min    DECIMAL(18, 2)          DEFAULT NULL COMMENT '条件：订单金额下限（本位币，含）',
    amount_max    DECIMAL(18, 2)          DEFAULT NULL COMMENT '条件：订单金额上限（本位币，含）',
    weight_min    DECIMAL(18, 3)          DEFAULT NULL COMMENT '条件：重量下限（kg，含）',
    weight_max    DECIMAL(18, 3)          DEFAULT NULL COMMENT '条件：重量上限（kg，含）',
    warehouse_id  BIGINT                  DEFAULT NULL COMMENT '动作：指定发货仓',
    channel_id    BIGINT                  DEFAULT NULL COMMENT '动作：指定物流渠道',
    hold_order    TINYINT        NOT NULL DEFAULT 0 COMMENT '动作：标记需人工审核（挂起） 1是 0否',
    hold_reason   VARCHAR(255)            DEFAULT NULL COMMENT '挂起原因',
    auto_audit    TINYINT        NOT NULL DEFAULT 0 COMMENT '动作：自动审核通过 1是 0否',
    cutoff_time   TIME                    DEFAULT NULL COMMENT '动作：截单时间（之前付款当天发货，之后次日）',
    stop_on_match TINYINT        NOT NULL DEFAULT 0 COMMENT '命中后不再匹配后续规则 1是 0否',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_order_rule_priority (status, priority)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审单规则';

DROP TABLE IF EXISTS ecom_package_item;
DROP TABLE IF EXISTS ecom_package;
CREATE TABLE ecom_package (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_no       VARCHAR(32)    NOT NULL COMMENT '包裹号（编码规则 PK），向物流商下单的客户单号',
    store_id         BIGINT         NOT NULL COMMENT '店铺',
    warehouse_id     BIGINT         NOT NULL COMMENT '发货仓',
    channel_id       BIGINT                  DEFAULT NULL COMMENT '物流渠道',
    carrier          VARCHAR(16)             DEFAULT NULL COMMENT '对接的物流商(字典 ecom_carrier)',
    carrier_name     VARCHAR(64)             DEFAULT NULL COMMENT '承运商名称（回传平台）',
    carrier_order_no VARCHAR(64)             DEFAULT NULL COMMENT '物流商订单号（取面单、取消用）',
    tracking_no      VARCHAR(64)             DEFAULT NULL COMMENT '运单号（跟踪号）',
    label_file       VARCHAR(255)            DEFAULT NULL COMMENT '面单 PDF（附件目录下的相对路径）',
    label_time       DATETIME                DEFAULT NULL COMMENT '取得运单号时间',
    print_count      INT            NOT NULL DEFAULT 0 COMMENT '面单打印次数',
    weight           DECIMAL(18, 3)          DEFAULT NULL COMMENT '重量（kg）',
    shipping_cost    DECIMAL(18, 2)          DEFAULT NULL COMMENT '运费（本位币），发货时按订单金额分摊到订单的物流成本',
    status           VARCHAR(16)    NOT NULL DEFAULT 'WAIT_LABEL' COMMENT '状态(字典 ecom_package_status) WAIT_LABEL待打单 LABELED待发货 SHIPPED已发货 CANCELLED已取消',
    ship_time        DATETIME                DEFAULT NULL COMMENT '发货时间',
    ship_name        VARCHAR(64)             DEFAULT NULL COMMENT '收件人（快照）',
    ship_country     VARCHAR(8)              DEFAULT NULL COMMENT '收件国家（快照）',
    error_msg        VARCHAR(500)            DEFAULT NULL COMMENT '最近一次申请运单号 / 取面单失败的原因',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by        BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_package_no (package_no, tenant_id),
    KEY idx_ecom_package_tracking (tracking_no),
    KEY idx_ecom_package_status (status, warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '包裹（一个或多个平台订单的全部或部分行，一个运单号）';

CREATE TABLE ecom_package_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_id    BIGINT         NOT NULL COMMENT '包裹',
    order_id      BIGINT         NOT NULL COMMENT '平台订单',
    order_item_id BIGINT         NOT NULL COMMENT '平台订单明细',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '件数（平台 SKU）',
    material_qty  DECIMAL(18, 4) NOT NULL COMMENT '物料数量',
    cost          DECIMAL(18, 2)          DEFAULT NULL COMMENT '出库成本（本位币）',
    trade_doc_id  BIGINT                  DEFAULT NULL COMMENT '发货时生成的销售出库单(scm_trade_doc)',
    trade_doc_no  VARCHAR(32)             DEFAULT NULL COMMENT '销售出库单号',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_package_item_pkg (package_id),
    KEY idx_ecom_package_item_order (order_id),
    KEY idx_ecom_package_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '包裹明细';

-- 菜单：订单与售后下的订单规则、包裹打单、扫描发货；平台订单的审核、合并拆分按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(432, 982, '订单规则', 'C', 'order-rule', 'ecom/order-rule/index', 'ecom:orderRule:list', 'FilterOutlined', 3, 1, 1, NOW(), NOW()),
(433, 982, '包裹打单', 'C', 'package', 'ecom/package/index', 'ecom:package:list', 'InboxOutlined', 4, 1, 1, NOW(), NOW()),
(434, 982, '扫描发货', 'C', 'scan-ship', 'ecom/scan-ship/index', 'ecom:package:ship', 'ScanOutlined', 5, 1, 1, NOW(), NOW()),
(4320, 432, '规则新增', 'F', NULL, NULL, 'ecom:orderRule:add', NULL, 1, 1, 1, NOW(), NOW()),
(4321, 432, '规则修改', 'F', NULL, NULL, 'ecom:orderRule:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4322, 432, '规则删除', 'F', NULL, NULL, 'ecom:orderRule:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4323, 423, '订单审核', 'F', NULL, NULL, 'ecom:order:audit', NULL, 6, 1, 1, NOW(), NOW()),
(4324, 423, '合并拆分', 'F', NULL, NULL, 'ecom:order:merge', NULL, 7, 1, 1, NOW(), NOW()),
(4330, 433, '申请运单', 'F', NULL, NULL, 'ecom:package:label', NULL, 1, 1, 1, NOW(), NOW()),
(4331, 433, '包裹发货', 'F', NULL, NULL, 'ecom:package:ship', NULL, 2, 1, 1, NOW(), NOW());

-- 字典：订单处理状态细化（待处理改为待审核，增加已挂起、缺货、待打单、待发货、部分发货），包裹状态，对接的物流商
UPDATE sys_dict_data SET dict_label = '待审核', sort = 1 WHERE dict_type = 'ecom_local_status' AND dict_value = 'PENDING';
UPDATE sys_dict_data SET sort = 3 WHERE dict_type = 'ecom_local_status' AND dict_value = 'EXCEPTION';
UPDATE sys_dict_data SET sort = 7 WHERE dict_type = 'ecom_local_status' AND dict_value = 'OUTBOUND';
UPDATE sys_dict_data SET sort = 9 WHERE dict_type = 'ecom_local_status' AND dict_value = 'SHIPPED';
UPDATE sys_dict_data SET sort = 10 WHERE dict_type = 'ecom_local_status' AND dict_value = 'CLOSED';
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(230, '包裹状态', 'ecom_package_status', 'SYSTEM', 1, '平台订单包裹的打单、发货状态', NOW(), NOW()),
(231, '对接物流商', 'ecom_carrier', 'SYSTEM', 1, '物流渠道对接的物流商面单接口', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_local_status', '已挂起', 'ON_HOLD', 'volcano', 2, 1, 1, NOW(), NOW()),
('ecom_local_status', '缺货', 'OUT_OF_STOCK', 'magenta', 4, 1, 1, NOW(), NOW()),
('ecom_local_status', '待打单', 'AUDITED', 'cyan', 5, 1, 1, NOW(), NOW()),
('ecom_local_status', '待发货', 'LABELED', 'blue', 6, 1, 1, NOW(), NOW()),
('ecom_local_status', '部分发货', 'PARTIAL', 'gold', 8, 1, 1, NOW(), NOW()),
('ecom_package_status', '待打单', 'WAIT_LABEL', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_package_status', '待发货', 'LABELED', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_package_status', '已发货', 'SHIPPED', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_package_status', '已取消', 'CANCELLED', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_carrier', '递四方 4PX', 'FOURPX', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_carrier', '云途物流', 'YUNEXPRESS', 'green', 2, 1, 1, NOW(), NOW());

-- 内置角色：电商运营全部；仓库管理员打单、发货、扫描；普通用户、总经理只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 432 AS menu_id UNION ALL SELECT 5, 433 UNION ALL SELECT 5, 434 UNION ALL SELECT 5, 4320 UNION ALL SELECT 5, 4321
    UNION ALL SELECT 5, 4322 UNION ALL SELECT 5, 4323 UNION ALL SELECT 5, 4324 UNION ALL SELECT 5, 4330 UNION ALL SELECT 5, 4331
    UNION ALL SELECT 6, 17 UNION ALL SELECT 6, 982 UNION ALL SELECT 6, 433 UNION ALL SELECT 6, 434 UNION ALL SELECT 6, 4330 UNION ALL SELECT 6, 4331
    UNION ALL SELECT 2, 432 UNION ALL SELECT 2, 433 UNION ALL SELECT 3, 432 UNION ALL SELECT 3, 433
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 渠道接入：在线商品的平台附加 ID（Shopify 变体 ID / 库存项 ID、平台仓库 ID 等，JSON 对象），拉取在线商品时写入，回传库存时交给连接器
ALTER TABLE ecom_listing ADD COLUMN platform_extra VARCHAR(1000) DEFAULT NULL COMMENT '平台附加 ID（JSON）';

-- ---------------------------------------------------------------- 电商：补货建议 → 采购计划 → 采购订单

CREATE TABLE ecom_replenish_setting (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    scope         VARCHAR(16)    NOT NULL COMMENT '级别 LISTING在线商品 MATERIAL物料',
    listing_id    BIGINT                  DEFAULT NULL COMMENT '在线商品（在线商品级）',
    material_id   BIGINT                  DEFAULT NULL COMMENT '物料（物料级）',
    lead_days     INT                     DEFAULT NULL COMMENT '平台仓补货周期（天），为空取店铺设置',
    safety_days   INT                     DEFAULT NULL COMMENT '安全天数，为空取店铺设置',
    purchase_days INT                     DEFAULT NULL COMMENT '采购周期（天，物料级），为空取默认供应商的提前期，再取店铺补货周期',
    daily_sales   DECIMAL(18, 4)          DEFAULT NULL COMMENT '手工日均销量（在线商品级为平台 SKU 件数，物料级为物料数量），为空按销量计算',
    factor        DECIMAL(8, 4)           DEFAULT NULL COMMENT '季节 / 活动系数，乘在按销量计算的日均上，为空为 1',
    disabled      TINYINT        NOT NULL DEFAULT 0 COMMENT '不补货 1是 0否',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_replenish_listing (listing_id, tenant_id),
    UNIQUE KEY uk_ecom_replenish_material (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '补货设置（在线商品级 / 物料级）';

CREATE TABLE ecom_purchase_plan (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '计划单号',
    plan_date    DATE           NOT NULL COMMENT '计划日期',
    status       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_purchase_plan_status) DRAFT草稿 APPROVED已审核 ORDERED已下单 CLOSED已关闭',
    warehouse_id BIGINT                  DEFAULT NULL COMMENT '收货仓库（生成的采购订单的默认收货仓）',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '计划采购数量合计',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    approve_by   BIGINT                  DEFAULT NULL COMMENT '审核人',
    approve_time DATETIME                DEFAULT NULL COMMENT '审核时间',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_purchase_plan_no (doc_no, tenant_id),
    KEY idx_ecom_purchase_plan_status (status),
    KEY idx_ecom_purchase_plan_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购计划（由补货建议生成，按供应商拆单生成采购订单）';

CREATE TABLE ecom_purchase_plan_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_id      BIGINT         NOT NULL COMMENT '采购计划',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    suggest_qty  DECIMAL(18, 4)          DEFAULT NULL COMMENT '生成时的建议采购量',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '计划采购量',
    supplier_id  BIGINT                  DEFAULT NULL COMMENT '供应商（生成时取默认供应商）',
    price        DECIMAL(18, 4)          DEFAULT NULL COMMENT '含税单价，为空时生成订单取供应商价格 / 默认采购价',
    tax_rate     DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%），为空取默认',
    arrival_date DATE                    DEFAULT NULL COMMENT '要求到货日期',
    demand       VARCHAR(2000)           DEFAULT NULL COMMENT '各店铺的需求构成（JSON：storeId storeName sellerSku qty）',
    order_id     BIGINT                  DEFAULT NULL COMMENT '生成的采购订单(scm_order)',
    order_no     VARCHAR(32)             DEFAULT NULL COMMENT '采购订单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_purchase_plan_item_plan (plan_id),
    KEY idx_ecom_purchase_plan_item_material (material_id),
    KEY idx_ecom_purchase_plan_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购计划明细';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(460, 983, '采购计划', 'C', 'purchase-plan', 'ecom/purchase-plan/index', 'ecom:purchasePlan:list', 'ScheduleOutlined', 3, 1, 1, NOW(), NOW()),
(4600, 460, '采购计划新增', 'F', NULL, NULL, 'ecom:purchasePlan:add', NULL, 1, 1, 1, NOW(), NOW()),
(4601, 460, '采购计划修改', 'F', NULL, NULL, 'ecom:purchasePlan:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4602, 460, '采购计划删除', 'F', NULL, NULL, 'ecom:purchasePlan:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4603, 460, '采购计划审核', 'F', NULL, NULL, 'ecom:purchasePlan:approve', NULL, 4, 1, 1, NOW(), NOW()),
(4604, 460, '生成采购订单', 'F', NULL, NULL, 'ecom:purchasePlan:order', NULL, 5, 1, 1, NOW(), NOW()),
(4605, 427, '补货设置', 'F', NULL, NULL, 'ecom:replenish:setting', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(260, '采购计划状态', 'ecom_purchase_plan_status', 'SYSTEM', 1, '电商采购计划的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_purchase_plan_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_purchase_plan_status', '已审核', 'APPROVED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_purchase_plan_status', '已下单', 'ORDERED', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_purchase_plan_status', '已关闭', 'CLOSED', 'default', 4, 1, 1, NOW(), NOW()),
('scm_order_source', '采购计划', 'PURCHASE_PLAN', 'gold', 11, 1, 1, NOW(), NOW());

-- 内置角色：电商运营、采购员全部；总经理查看与审核；普通用户只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 460 AS menu_id UNION ALL SELECT 5, 4600 UNION ALL SELECT 5, 4601 UNION ALL SELECT 5, 4602 UNION ALL SELECT 5, 4603
    UNION ALL SELECT 5, 4604 UNION ALL SELECT 5, 4605
    UNION ALL SELECT 4, 17 UNION ALL SELECT 4, 983 UNION ALL SELECT 4, 427 UNION ALL SELECT 4, 460 UNION ALL SELECT 4, 4600 UNION ALL SELECT 4, 4601
    UNION ALL SELECT 4, 4602 UNION ALL SELECT 4, 4603 UNION ALL SELECT 4, 4604 UNION ALL SELECT 4, 4605
    UNION ALL SELECT 3, 460 UNION ALL SELECT 3, 4603
    UNION ALL SELECT 2, 460
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- ---------------------------------------------------------------- 跨境电商：商品体系（组合商品、SPU / 变体、SKU 配对中心、发货计划、货件装箱与 FNSKU 标签）

-- 在线商品：组成方式（单品 / 组合）、FNSKU
ALTER TABLE ecom_listing ADD COLUMN compose_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE' COMMENT '组成方式(字典 ecom_compose_type) SINGLE单品（物料 × 多件装） BUNDLE组合（ecom_bundle_item）';
ALTER TABLE ecom_listing ADD COLUMN fnsku VARCHAR(32) DEFAULT NULL COMMENT 'FNSKU（平台仓商品标签条码）';
CREATE INDEX idx_ecom_listing_parent ON ecom_listing (store_id, parent_item_id);

CREATE TABLE ecom_bundle_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    listing_id  BIGINT         NOT NULL COMMENT '在线商品（组成方式为组合）',
    material_id BIGINT         NOT NULL COMMENT '物料',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '一件平台 SKU 包含的物料数量',
    sort        INT            NOT NULL DEFAULT 0 COMMENT '顺序',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_bundle_item (listing_id, material_id, tenant_id),
    KEY idx_ecom_bundle_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组合商品的组成（一个平台 SKU 由多个物料组成）';

-- 平台订单明细：组合商品展开的物料
ALTER TABLE ecom_order_item ADD COLUMN bundle TINYINT NOT NULL DEFAULT 0 COMMENT '是否组合商品 1是（物料见 ecom_order_item_part，material_id / material_qty 为空）';

CREATE TABLE ecom_order_item_part (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id      BIGINT         NOT NULL COMMENT '平台订单',
    order_item_id BIGINT         NOT NULL COMMENT '平台订单明细',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    unit_qty      DECIMAL(18, 4) NOT NULL COMMENT '每件平台 SKU 含的物料数量',
    material_qty  DECIMAL(18, 4) NOT NULL COMMENT '物料数量 = 件数 × unit_qty',
    share         DECIMAL(10, 6) NOT NULL DEFAULT 1 COMMENT '收入分摊比例（各组件合计为 1，按 用量 × 参考成本）',
    cost          DECIMAL(18, 2)          DEFAULT NULL COMMENT '出库成本（本位币）',
    sort          INT            NOT NULL DEFAULT 0 COMMENT '顺序',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_order_item_part_item (order_item_id),
    KEY idx_ecom_order_item_part_order (order_id),
    KEY idx_ecom_order_item_part_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台订单明细的组合商品展开（匹配时按组成记录）';

-- 包裹明细：组合商品按组件拆成多行
ALTER TABLE ecom_package_item ADD COLUMN part_id BIGINT DEFAULT NULL COMMENT '组合商品的组件(ecom_order_item_part)，单品为空';
ALTER TABLE ecom_package_item ADD COLUMN part_main TINYINT NOT NULL DEFAULT 1 COMMENT '计入订单行已分配件数 1是（组合商品只有第一个组件行）';

-- 发货计划
CREATE TABLE ecom_shipment_plan (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_no           VARCHAR(32)    NOT NULL COMMENT '计划号（编码规则 FP）',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    from_warehouse_id BIGINT         NOT NULL COMMENT '发货仓（本地仓）',
    to_warehouse_id   BIGINT         NOT NULL COMMENT '目标平台仓 / 海外仓',
    plan_date         DATE                    DEFAULT NULL COMMENT '计划发货日期',
    total_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '计划件数合计（平台 SKU）',
    status            VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_shipment_plan_status) DRAFT草稿 CONFIRMED已确认 GENERATED已生成货件 CANCELLED已取消',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id           BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by         BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_shipment_plan_no (plan_no, tenant_id),
    KEY idx_ecom_shipment_plan_store (store_id, status),
    KEY idx_ecom_shipment_plan_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货计划（FBA / 平台仓补货计划）';

CREATE TABLE ecom_shipment_plan_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_id     BIGINT         NOT NULL COMMENT '发货计划',
    listing_id  BIGINT         NOT NULL COMMENT '在线商品',
    seller_sku  VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '计划件数（平台 SKU）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_shipment_plan_item_plan (plan_id),
    KEY idx_ecom_shipment_plan_item_listing (listing_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货计划明细';

-- 头程货件：来源发货计划；明细按平台 SKU 计件数（装箱、FNSKU 标签）
ALTER TABLE ecom_inbound ADD COLUMN plan_id BIGINT DEFAULT NULL COMMENT '来源发货计划';
CREATE INDEX idx_ecom_inbound_plan ON ecom_inbound (plan_id);
ALTER TABLE ecom_inbound_item ADD COLUMN listing_id BIGINT DEFAULT NULL COMMENT '在线商品（发货计划生成或按 店铺 + 平台 SKU 识别）';
ALTER TABLE ecom_inbound_item ADD COLUMN sku_qty DECIMAL(18, 4) DEFAULT NULL COMMENT '本行对应的平台 SKU 件数（单品 = 数量 ÷ 多件装，组合 = 数量 ÷ 组件用量）';
ALTER TABLE ecom_inbound_item ADD COLUMN part_main TINYINT NOT NULL DEFAULT 1 COMMENT '计入平台 SKU 件数 1是（组合商品只有第一个组件行）';
ALTER TABLE ecom_inbound_item ADD COLUMN plan_item_id BIGINT DEFAULT NULL COMMENT '来源发货计划明细';
CREATE INDEX idx_ecom_inbound_item_plan ON ecom_inbound_item (plan_item_id);

CREATE TABLE ecom_inbound_box (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_id  BIGINT         NOT NULL COMMENT '头程货件',
    box_no      INT            NOT NULL COMMENT '箱号（货件内从 1 开始）',
    length      DECIMAL(10, 2)          DEFAULT NULL COMMENT '长（cm）',
    width       DECIMAL(10, 2)          DEFAULT NULL COMMENT '宽（cm）',
    height      DECIMAL(10, 2)          DEFAULT NULL COMMENT '高（cm）',
    weight      DECIMAL(18, 3)          DEFAULT NULL COMMENT '毛重（kg）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_inbound_box (inbound_id, box_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '头程货件的箱子';

CREATE TABLE ecom_inbound_box_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_id  BIGINT         NOT NULL COMMENT '头程货件',
    box_no      INT            NOT NULL COMMENT '箱号',
    seller_sku  VARCHAR(64)    NOT NULL COMMENT '平台 SKU（没有平台 SKU 的明细为物料编码）',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '件数（平台 SKU）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_inbound_box_item (inbound_id, box_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '头程货件箱内的平台 SKU 与件数';

-- 菜单：店铺与商品下的 SKU 配对，库存与物流下的发货计划；头程货件的装箱按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(450, 981, 'SKU 配对', 'C', 'sku-pairing', 'ecom/sku-pairing/index', 'ecom:pairing:list', 'ApartmentOutlined', 4, 1, 1, NOW(), NOW()),
(451, 983, '发货计划', 'C', 'shipment-plan', 'ecom/shipment-plan/index', 'ecom:shipmentPlan:list', 'ScheduleOutlined', 1, 1, 1, NOW(), NOW()),
(4500, 450, '确认 / 解除配对', 'F', NULL, NULL, 'ecom:pairing:edit', NULL, 1, 1, 1, NOW(), NOW()),
(4501, 450, '导入配对', 'F', NULL, NULL, 'ecom:pairing:import', NULL, 2, 1, 1, NOW(), NOW()),
(4510, 451, '计划新增', 'F', NULL, NULL, 'ecom:shipmentPlan:add', NULL, 1, 1, 1, NOW(), NOW()),
(4511, 451, '计划修改', 'F', NULL, NULL, 'ecom:shipmentPlan:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4512, 451, '计划删除', 'F', NULL, NULL, 'ecom:shipmentPlan:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4513, 451, '计划确认 / 取消', 'F', NULL, NULL, 'ecom:shipmentPlan:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(4514, 451, '生成货件', 'F', NULL, NULL, 'ecom:shipmentPlan:generate', NULL, 5, 1, 1, NOW(), NOW()),
(4520, 425, '货件装箱', 'F', NULL, NULL, 'ecom:inbound:box', NULL, 6, 1, 1, NOW(), NOW());

-- 系统字典：组成方式、发货计划状态
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(250, '商品组成方式', 'ecom_compose_type', 'SYSTEM', 1, '在线商品是单品还是组合商品', NOW(), NOW()),
(251, '发货计划状态', 'ecom_shipment_plan_status', 'SYSTEM', 1, '发货计划的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_compose_type', '单品', 'SINGLE', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_compose_type', '组合', 'BUNDLE', 'purple', 2, 1, 1, NOW(), NOW()),
('ecom_shipment_plan_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_shipment_plan_status', '已确认', 'CONFIRMED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_shipment_plan_status', '已生成货件', 'GENERATED', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_shipment_plan_status', '已取消', 'CANCELLED', 'red', 4, 1, 1, NOW(), NOW());

-- 内置角色：电商运营全部；仓库管理员发货计划与装箱；普通用户、总经理只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 450 AS menu_id UNION ALL SELECT 5, 4500 UNION ALL SELECT 5, 4501 UNION ALL SELECT 5, 451 UNION ALL SELECT 5, 4510
    UNION ALL SELECT 5, 4511 UNION ALL SELECT 5, 4512 UNION ALL SELECT 5, 4513 UNION ALL SELECT 5, 4514 UNION ALL SELECT 5, 4520
    UNION ALL SELECT 6, 17 UNION ALL SELECT 6, 983 UNION ALL SELECT 6, 451 UNION ALL SELECT 6, 4510 UNION ALL SELECT 6, 4511 UNION ALL SELECT 6, 4512
    UNION ALL SELECT 6, 4513 UNION ALL SELECT 6, 4514 UNION ALL SELECT 6, 425 UNION ALL SELECT 6, 4520
    UNION ALL SELECT 2, 450 UNION ALL SELECT 2, 451 UNION ALL SELECT 3, 450 UNION ALL SELECT 3, 451
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- ---------------------------------------------------------------- 跨境电商：利润与财务细化（费用类型映射与分摊、VAT / 销售税、回款对账、多维利润报表）

-- 平台订单：税额拆分与退货处理费（没有匹配到启用的税率时税率为空、税额为 0，利润与原口径一致）
ALTER TABLE ecom_order ADD COLUMN tax_type VARCHAR(16) DEFAULT NULL COMMENT '适用的税种(字典 ecom_tax_type) VAT GST SALES_TAX，为空未配置税率';
ALTER TABLE ecom_order ADD COLUMN vat_rate DECIMAL(6, 2) DEFAULT NULL COMMENT '适用税率（%），按收件国家 + 店铺站点匹配 ecom_tax_rate';
ALTER TABLE ecom_order ADD COLUMN tax_facilitator TINYINT NOT NULL DEFAULT 0 COMMENT '平台代扣代缴（Marketplace Facilitator） 1是：税额不计收入、不需卖家申报';
ALTER TABLE ecom_order ADD COLUMN vat_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '需卖家申报的税额（订单币种）：含税价 ÷ (1 + 税率) 拆出，按退款比例冲减';
ALTER TABLE ecom_order ADD COLUMN withheld_tax DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '平台代扣代缴的税额（订单币种），不计收入';
ALTER TABLE ecom_order ADD COLUMN return_fee DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '退货处理费（订单币种，结算回写）';

-- 结算明细：平台报告中的原始类型名
ALTER TABLE ecom_settlement_item ADD COLUMN raw_type VARCHAR(128) DEFAULT NULL COMMENT '结算报告中的原始类型名（按 ecom_fee_mapping 识别为系统类型）';

-- 结算单：不挂订单费用的分摊方式
ALTER TABLE ecom_settlement ADD COLUMN alloc_method VARCHAR(16) DEFAULT NULL COMMENT '不挂订单费用的分摊方式(字典 ecom_fee_alloc_method) QTY按销量 SALES按销售额，为空按类型默认（仓储、移除、入库、配送、退货类按销量，其余按销售额）';

-- 头程货件：进口增值税（可抵扣的进项）
ALTER TABLE ecom_inbound ADD COLUMN import_vat DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '进口增值税（本位币，可抵扣的进项，不计入头程成本）';

CREATE TABLE ecom_fee_mapping (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台(字典 ecom_platform)，OTHER 为通用（店铺平台没有对应映射时使用）',
    raw_type    VARCHAR(128) NOT NULL COMMENT '结算报告中的原始类型名，如 FBAPerUnitFulfillmentFee',
    raw_key     VARCHAR(128) NOT NULL COMMENT '原始类型名去掉首尾空格后的小写，用于匹配',
    item_type   VARCHAR(16)  NOT NULL COMMENT '系统类型(字典 ecom_settlement_item_type)',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置示例 1是',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fee_mapping (platform, raw_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '结算费用类型映射：平台 + 原始类型 → 系统类型';

CREATE TABLE ecom_fee_allocation (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id      BIGINT         NOT NULL COMMENT '结算单',
    settlement_item_id BIGINT         NOT NULL COMMENT '结算明细（不挂订单的费用）',
    store_id           BIGINT         NOT NULL COMMENT '店铺',
    seller_sku         VARCHAR(64)             DEFAULT NULL COMMENT '分摊到的平台 SKU，为空表示留在店铺（结算周期内没有销量）',
    item_type          VARCHAR(16)    NOT NULL COMMENT '费用类型(字典 ecom_settlement_item_type)',
    method             VARCHAR(16)    NOT NULL COMMENT '分摊方式(字典 ecom_fee_alloc_method) SKU直接归属 QTY按销量 SALES按销售额 STORE留在店铺',
    alloc_date         DATE           NOT NULL COMMENT '费用日期（入账日期，没有时为结算周期结束日）',
    basis_value        DECIMAL(18, 4)          DEFAULT NULL COMMENT '分摊依据（该 SKU 在结算周期内的销量或本位币销售额）',
    amount             DECIMAL(18, 2) NOT NULL COMMENT '分摊金额（店铺币种，费用为负）',
    amount_base        DECIMAL(18, 2) NOT NULL COMMENT '分摊金额（本位币，按结算单汇率）',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_fee_alloc_settlement (settlement_id),
    KEY idx_ecom_fee_alloc_date (alloc_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '不挂订单的平台费用分摊到 SKU 的结果';

CREATE TABLE ecom_tax_rate (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    country     VARCHAR(8)    NOT NULL COMMENT '收件国家 / 地区代码，如 DE GB US',
    marketplace VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '店铺站点(业务字典 ecom_marketplace)，空为全部站点；同一国家优先取站点相同的税率',
    tax_type    VARCHAR(16)   NOT NULL COMMENT '税种(字典 ecom_tax_type) VAT增值税 GST商品及服务税 SALES_TAX销售税',
    rate        DECIMAL(6, 2) NOT NULL DEFAULT 0 COMMENT '税率（%）',
    facilitator TINYINT       NOT NULL DEFAULT 0 COMMENT '平台代扣代缴（Marketplace Facilitator） 1是：税额不计收入、不需卖家申报',
    builtin     TINYINT       NOT NULL DEFAULT 0 COMMENT '内置示例 1是',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_tax_rate (country, marketplace, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '电商税率：VAT / GST / 销售税';

CREATE TABLE ecom_payout (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id        BIGINT                  DEFAULT NULL COMMENT '店铺（未匹配的流水可为空）',
    settlement_id   BIGINT                  DEFAULT NULL COMMENT '平台结算单（未匹配的流水为空）',
    source          VARCHAR(16)    NOT NULL COMMENT '来源 SETTLEMENT结算单应到账 IMPORT导入流水 MANUAL手工登记',
    currency        VARCHAR(8)     NOT NULL COMMENT '结算币种',
    expected_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '应到账金额（结算净额，结算币种）',
    expected_base   DECIMAL(18, 2)          DEFAULT NULL COMMENT '应到账金额（本位币，按结算汇率）',
    expected_date   DATE                    DEFAULT NULL COMMENT '应到账日期（结算周期结束日）',
    payout_date     DATE                    DEFAULT NULL COMMENT '到账日期',
    amount          DECIMAL(18, 2)          DEFAULT NULL COMMENT '到账金额（到账币种）',
    pay_currency    VARCHAR(8)              DEFAULT NULL COMMENT '到账币种（收款平台结汇后可能为本位币）',
    account_id      BIGINT                  DEFAULT NULL COMMENT '到账账户(fin_account 现金 / 银行末级科目)',
    channel         VARCHAR(16)             DEFAULT NULL COMMENT '收款平台(业务字典 ecom_payout_channel)',
    bank_ref        VARCHAR(64)             DEFAULT NULL COMMENT '银行 / 收款平台流水号',
    exchange_rate   DECIMAL(18, 6)          DEFAULT NULL COMMENT '到账汇率（1 结算币种 = 本位币）',
    amount_base     DECIMAL(18, 2)          DEFAULT NULL COMMENT '到账本位币金额',
    fx_diff         DECIMAL(18, 2)          DEFAULT NULL COMMENT '汇兑差额（本位币）= 应到账原币 × (到账汇率 − 结算汇率)，正数为收益',
    amount_diff     DECIMAL(18, 2)          DEFAULT NULL COMMENT '金额差额（本位币）= (到账原币 − 应到账原币) × 到账汇率，通常为收款平台手续费',
    status          VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 ecom_payout_status) PENDING未到账 RECEIVED已到账 DIFF有差异',
    voucher_id      BIGINT                  DEFAULT NULL COMMENT '差额凭证(fin_voucher)',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注（导入流水的摘要）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_payout_settlement (settlement_id),
    KEY idx_ecom_payout_status (status, expected_date),
    KEY idx_ecom_payout_ref (bank_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台回款：结算单应到账与银行 / 收款平台到账记录';

-- 菜单：结算与分析下的费用映射、税务、回款对账；平台结算的重新分摊按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(470, 984, '费用映射', 'C', 'fee-mapping', 'ecom/fee-mapping/index', 'ecom:feeMapping:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(471, 984, '税务', 'C', 'tax', 'ecom/tax/index', 'ecom:tax:list', 'AuditOutlined', 6, 1, 1, NOW(), NOW()),
(472, 984, '回款对账', 'C', 'payout', 'ecom/payout/index', 'ecom:payout:list', 'AccountBookOutlined', 7, 1, 1, NOW(), NOW()),
(4700, 470, '费用映射新增', 'F', NULL, NULL, 'ecom:feeMapping:add', NULL, 1, 1, 1, NOW(), NOW()),
(4701, 470, '费用映射修改', 'F', NULL, NULL, 'ecom:feeMapping:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4702, 470, '费用映射删除', 'F', NULL, NULL, 'ecom:feeMapping:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4710, 471, '税率新增', 'F', NULL, NULL, 'ecom:tax:add', NULL, 1, 1, 1, NOW(), NOW()),
(4711, 471, '税率修改', 'F', NULL, NULL, 'ecom:tax:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4712, 471, '税率删除', 'F', NULL, NULL, 'ecom:tax:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4713, 471, '重算订单税额', 'F', NULL, NULL, 'ecom:tax:apply', NULL, 4, 1, 1, NOW(), NOW()),
(4720, 472, '导入到账流水', 'F', NULL, NULL, 'ecom:payout:import', NULL, 1, 1, 1, NOW(), NOW()),
(4721, 472, '登记到账 / 匹配', 'F', NULL, NULL, 'ecom:payout:receive', NULL, 2, 1, 1, NOW(), NOW()),
(4722, 472, '撤销 / 删除到账', 'F', NULL, NULL, 'ecom:payout:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4730, 429, '重新分摊费用', 'F', NULL, NULL, 'ecom:settlement:allocate', NULL, 4, 1, 1, NOW(), NOW());

-- 字典：税种、回款状态、费用分摊方式（系统）；收款平台（业务，租户可扩展）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(270, '税种', 'ecom_tax_type', 'SYSTEM', 1, '电商税率的税种', NOW(), NOW()),
(271, '回款状态', 'ecom_payout_status', 'SYSTEM', 1, '平台回款的到账状态', NOW(), NOW()),
(272, '费用分摊方式', 'ecom_fee_alloc_method', 'SYSTEM', 1, '不挂订单的平台费用分摊到 SKU 的方式', NOW(), NOW()),
(273, '收款平台', 'ecom_payout_channel', 'BUSINESS', 1, '跨境收款平台 / 银行，可自行扩展', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_tax_type', '增值税 VAT', 'VAT', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_tax_type', '商品及服务税 GST', 'GST', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_tax_type', '销售税 Sales Tax', 'SALES_TAX', 'purple', 3, 1, 1, NOW(), NOW()),
('ecom_payout_status', '未到账', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_payout_status', '已到账', 'RECEIVED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_payout_status', '有差异', 'DIFF', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_fee_alloc_method', '按 SKU 直接归属', 'SKU', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_fee_alloc_method', '按销量', 'QTY', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_fee_alloc_method', '按销售额', 'SALES', 'gold', 3, 1, 1, NOW(), NOW()),
('ecom_fee_alloc_method', '留在店铺', 'STORE', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_payout_channel', 'PingPong', 'PINGPONG', 'blue', 1, 1, 0, NOW(), NOW()),
('ecom_payout_channel', '连连支付', 'LIANLIAN', 'cyan', 2, 1, 0, NOW(), NOW()),
('ecom_payout_channel', 'Payoneer', 'PAYONEER', 'orange', 3, 1, 0, NOW(), NOW()),
('ecom_payout_channel', '万里汇 WorldFirst', 'WORLDFIRST', 'red', 4, 1, 0, NOW(), NOW()),
('ecom_payout_channel', '空中云汇 Airwallex', 'AIRWALLEX', 'purple', 5, 1, 0, NOW(), NOW()),
('ecom_payout_channel', '银行', 'BANK', 'green', 6, 1, 0, NOW(), NOW()),
('ecom_payout_channel', '其他', 'OTHER', 'default', 7, 1, 0, NOW(), NOW());

-- 结算明细类型细化（原有取值不变）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_settlement_item_type', '长期仓储费', 'LONG_STORAGE', 'gold', 9, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '移除 / 弃置费', 'REMOVAL', 'volcano', 10, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '退货处理费', 'RETURN_FEE', 'red', 11, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '入库配置费', 'INBOUND_FEE', 'orange', 12, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '订阅费', 'SUBSCRIPTION', 'geekblue', 13, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '促销费', 'PROMOTION', 'magenta', 14, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '优惠券', 'COUPON', 'magenta', 15, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '秒杀费', 'DEAL', 'purple', 16, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '赔偿', 'REIMBURSEMENT', 'green', 17, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '预留 / 释放', 'RESERVE', 'default', 18, 1, 1, NOW(), NOW()),
('ecom_settlement_item_type', '平台代收代缴税', 'MARKETPLACE_TAX', 'cyan', 19, 1, 1, NOW(), NOW());

-- 内置角色：电商运营 费用映射、重新分摊（利润分析已有）；会计、出纳 税务与回款对账；总经理、普通用户只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 470 AS menu_id UNION ALL SELECT 5, 4700 UNION ALL SELECT 5, 4701 UNION ALL SELECT 5, 4702 UNION ALL SELECT 5, 4730
    UNION ALL SELECT 5, 431
    UNION ALL SELECT 9, 17 UNION ALL SELECT 9, 984 UNION ALL SELECT 9, 471 UNION ALL SELECT 9, 4710 UNION ALL SELECT 9, 4711 UNION ALL SELECT 9, 4712
    UNION ALL SELECT 9, 4713 UNION ALL SELECT 9, 472 UNION ALL SELECT 9, 4720 UNION ALL SELECT 9, 4721 UNION ALL SELECT 9, 4722
    UNION ALL SELECT 10, 17 UNION ALL SELECT 10, 984 UNION ALL SELECT 10, 471 UNION ALL SELECT 10, 4710 UNION ALL SELECT 10, 4711 UNION ALL SELECT 10, 4712
    UNION ALL SELECT 10, 4713 UNION ALL SELECT 10, 472 UNION ALL SELECT 10, 4720 UNION ALL SELECT 10, 4721 UNION ALL SELECT 10, 4722
    UNION ALL SELECT 2, 470 UNION ALL SELECT 2, 471 UNION ALL SELECT 2, 472
    UNION ALL SELECT 3, 470 UNION ALL SELECT 3, 471 UNION ALL SELECT 3, 472 UNION ALL SELECT 3, 431
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- 示例税率（平台租户，全部停用：按店铺实际的申报义务启用；没有启用的税率时订单不拆税）
INSERT INTO ecom_tax_rate (country, marketplace, tax_type, rate, facilitator, builtin, status, remark, create_time, update_time) VALUES
('DE', '', 'VAT', 19.00, 0, 1, 0, '德国增值税（卖家自行申报，如 FBA 欧洲仓库存）', NOW(), NOW()),
('FR', '', 'VAT', 20.00, 0, 1, 0, '法国增值税', NOW(), NOW()),
('IT', '', 'VAT', 22.00, 0, 1, 0, '意大利增值税', NOW(), NOW()),
('ES', '', 'VAT', 21.00, 0, 1, 0, '西班牙增值税', NOW(), NOW()),
('GB', '', 'VAT', 20.00, 0, 1, 0, '英国增值税', NOW(), NOW()),
('PL', '', 'VAT', 23.00, 0, 1, 0, '波兰增值税', NOW(), NOW()),
('JP', '', 'VAT', 10.00, 0, 1, 0, '日本消费税（JCT）', NOW(), NOW()),
('AU', '', 'GST', 10.00, 1, 1, 0, '澳大利亚 GST：低价进口商品由平台代扣代缴', NOW(), NOW()),
('SG', '', 'GST', 9.00, 1, 1, 0, '新加坡 GST：平台代扣代缴', NOW(), NOW()),
('US', '', 'SALES_TAX', 0.00, 1, 1, 0, '美国销售税：各州由平台代收代缴，订单税费不计收入', NOW(), NOW()),
('CA', '', 'GST', 5.00, 1, 1, 0, '加拿大 GST / HST：平台代收代缴', NOW(), NOW());

-- 内置费用类型映射（平台租户）：亚马逊结算报告的 amount-description / transaction-type、虾皮与 TikTok Shop 的收入报告列名，OTHER 为通用
INSERT INTO ecom_fee_mapping (platform, raw_type, raw_key, item_type, builtin, status, create_time, update_time) VALUES
('AMAZON', 'Principal', 'principal', 'ORDER', 1, 1, NOW(), NOW()),
('AMAZON', 'Shipping', 'shipping', 'ORDER', 1, 1, NOW(), NOW()),
('AMAZON', 'GiftWrap', 'giftwrap', 'ORDER', 1, 1, NOW(), NOW()),
('AMAZON', 'Tax', 'tax', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'ShippingTax', 'shippingtax', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'GiftWrapTax', 'giftwraptax', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'MarketplaceFacilitatorTax-Principal', 'marketplacefacilitatortax-principal', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'MarketplaceFacilitatorTax-Shipping', 'marketplacefacilitatortax-shipping', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'MarketplaceFacilitatorVAT-Principal', 'marketplacefacilitatorvat-principal', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'MarketplaceFacilitatorVAT-Shipping', 'marketplacefacilitatorvat-shipping', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'LowValueGoodsTax-Principal', 'lowvaluegoodstax-principal', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('AMAZON', 'Commission', 'commission', 'COMMISSION', 1, 1, NOW(), NOW()),
('AMAZON', 'RefundCommission', 'refundcommission', 'COMMISSION', 1, 1, NOW(), NOW()),
('AMAZON', 'VariableClosingFee', 'variableclosingfee', 'COMMISSION', 1, 1, NOW(), NOW()),
('AMAZON', 'FixedClosingFee', 'fixedclosingfee', 'COMMISSION', 1, 1, NOW(), NOW()),
('AMAZON', 'DigitalServicesFee', 'digitalservicesfee', 'COMMISSION', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAPerUnitFulfillmentFee', 'fbaperunitfulfillmentfee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAPerOrderFulfillmentFee', 'fbaperorderfulfillmentfee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAWeightBasedFee', 'fbaweightbasedfee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'ShippingChargeback', 'shippingchargeback', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'Storage Fee', 'storage fee', 'STORAGE', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAStorageFee', 'fbastoragefee', 'STORAGE', 1, 1, NOW(), NOW()),
('AMAZON', 'StorageRenewalBilling', 'storagerenewalbilling', 'LONG_STORAGE', 1, 1, NOW(), NOW()),
('AMAZON', 'FBALongTermStorageFee', 'fbalongtermstoragefee', 'LONG_STORAGE', 1, 1, NOW(), NOW()),
('AMAZON', 'RemovalComplete', 'removalcomplete', 'REMOVAL', 1, 1, NOW(), NOW()),
('AMAZON', 'DisposalComplete', 'disposalcomplete', 'REMOVAL', 1, 1, NOW(), NOW()),
('AMAZON', 'FBARemovalFee', 'fbaremovalfee', 'REMOVAL', 1, 1, NOW(), NOW()),
('AMAZON', 'FBADisposalFee', 'fbadisposalfee', 'REMOVAL', 1, 1, NOW(), NOW()),
('AMAZON', 'FBACustomerReturnPerUnitFee', 'fbacustomerreturnperunitfee', 'RETURN_FEE', 1, 1, NOW(), NOW()),
('AMAZON', 'ReturnPostageBilling', 'returnpostagebilling', 'RETURN_FEE', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAInboundPlacementServiceFee', 'fbainboundplacementservicefee', 'INBOUND_FEE', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAInboundTransportationFee', 'fbainboundtransportationfee', 'INBOUND_FEE', 1, 1, NOW(), NOW()),
('AMAZON', 'FBAInboundConvenienceFee', 'fbainboundconveniencefee', 'INBOUND_FEE', 1, 1, NOW(), NOW()),
('AMAZON', 'Subscription Fee', 'subscription fee', 'SUBSCRIPTION', 1, 1, NOW(), NOW()),
('AMAZON', 'Subscription', 'subscription', 'SUBSCRIPTION', 1, 1, NOW(), NOW()),
('AMAZON', 'Promotion', 'promotion', 'PROMOTION', 1, 1, NOW(), NOW()),
('AMAZON', 'PrincipalPromotion', 'principalpromotion', 'PROMOTION', 1, 1, NOW(), NOW()),
('AMAZON', 'ShippingPromotion', 'shippingpromotion', 'PROMOTION', 1, 1, NOW(), NOW()),
('AMAZON', 'CouponRedemptionFee', 'couponredemptionfee', 'COUPON', 1, 1, NOW(), NOW()),
('AMAZON', 'Coupon Redemption Fee', 'coupon redemption fee', 'COUPON', 1, 1, NOW(), NOW()),
('AMAZON', 'LightningDealFee', 'lightningdealfee', 'DEAL', 1, 1, NOW(), NOW()),
('AMAZON', 'Lightning Deal Fee', 'lightning deal fee', 'DEAL', 1, 1, NOW(), NOW()),
('AMAZON', 'Cost of Advertising', 'cost of advertising', 'AD', 1, 1, NOW(), NOW()),
('AMAZON', 'ProductAdsPayment', 'productadspayment', 'AD', 1, 1, NOW(), NOW()),
('AMAZON', 'REVERSAL_REIMBURSEMENT', 'reversal_reimbursement', 'REIMBURSEMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'WAREHOUSE_DAMAGE', 'warehouse_damage', 'REIMBURSEMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'WAREHOUSE_LOST', 'warehouse_lost', 'REIMBURSEMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'MISSING_FROM_INBOUND', 'missing_from_inbound', 'REIMBURSEMENT', 1, 1, NOW(), NOW()),
('AMAZON', 'Previous Reserve Amount Balance', 'previous reserve amount balance', 'RESERVE', 1, 1, NOW(), NOW()),
('AMAZON', 'Current Reserve Amount', 'current reserve amount', 'RESERVE', 1, 1, NOW(), NOW()),
('SHOPEE', 'Original product price', 'original product price', 'ORDER', 1, 1, NOW(), NOW()),
('SHOPEE', 'Product Price', 'product price', 'ORDER', 1, 1, NOW(), NOW()),
('SHOPEE', 'Shipping Fee Paid by Buyer', 'shipping fee paid by buyer', 'ORDER', 1, 1, NOW(), NOW()),
('SHOPEE', 'Commission Fee', 'commission fee', 'COMMISSION', 1, 1, NOW(), NOW()),
('SHOPEE', 'Service Fee', 'service fee', 'COMMISSION', 1, 1, NOW(), NOW()),
('SHOPEE', 'Transaction Fee', 'transaction fee', 'COMMISSION', 1, 1, NOW(), NOW()),
('SHOPEE', 'Actual Shipping Fee', 'actual shipping fee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('SHOPEE', 'Shopee Shipping Rebate', 'shopee shipping rebate', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('SHOPEE', 'Reverse Shipping Fee', 'reverse shipping fee', 'RETURN_FEE', 1, 1, NOW(), NOW()),
('SHOPEE', 'Refund Amount', 'refund amount', 'REFUND', 1, 1, NOW(), NOW()),
('SHOPEE', 'Seller Voucher', 'seller voucher', 'COUPON', 1, 1, NOW(), NOW()),
('SHOPEE', 'Seller Coin Cash Back', 'seller coin cash back', 'PROMOTION', 1, 1, NOW(), NOW()),
('SHOPEE', 'Shopee Ads', 'shopee ads', 'AD', 1, 1, NOW(), NOW()),
('SHOPEE', 'Withholding Tax', 'withholding tax', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('TIKTOK', 'Subtotal after seller discounts', 'subtotal after seller discounts', 'ORDER', 1, 1, NOW(), NOW()),
('TIKTOK', 'Gross sales', 'gross sales', 'ORDER', 1, 1, NOW(), NOW()),
('TIKTOK', 'Customer-paid shipping fee', 'customer-paid shipping fee', 'ORDER', 1, 1, NOW(), NOW()),
('TIKTOK', 'Gross sales refund', 'gross sales refund', 'REFUND', 1, 1, NOW(), NOW()),
('TIKTOK', 'Referral fee', 'referral fee', 'COMMISSION', 1, 1, NOW(), NOW()),
('TIKTOK', 'Transaction fee', 'transaction fee', 'COMMISSION', 1, 1, NOW(), NOW()),
('TIKTOK', 'Platform commission', 'platform commission', 'COMMISSION', 1, 1, NOW(), NOW()),
('TIKTOK', 'FBT fulfillment fee', 'fbt fulfillment fee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('TIKTOK', 'Actual shipping fee', 'actual shipping fee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('TIKTOK', 'Return shipping fee', 'return shipping fee', 'RETURN_FEE', 1, 1, NOW(), NOW()),
('TIKTOK', 'Affiliate commission', 'affiliate commission', 'PROMOTION', 1, 1, NOW(), NOW()),
('TIKTOK', 'Seller discount', 'seller discount', 'COUPON', 1, 1, NOW(), NOW()),
('TIKTOK', 'Flash deal fee', 'flash deal fee', 'DEAL', 1, 1, NOW(), NOW()),
('TIKTOK', 'Ads payment', 'ads payment', 'AD', 1, 1, NOW(), NOW()),
('TIKTOK', 'Sales tax', 'sales tax', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('OTHER', 'Order Revenue', 'order revenue', 'ORDER', 1, 1, NOW(), NOW()),
('OTHER', 'Sales', 'sales', 'ORDER', 1, 1, NOW(), NOW()),
('OTHER', 'Refund', 'refund', 'REFUND', 1, 1, NOW(), NOW()),
('OTHER', 'Commission', 'commission', 'COMMISSION', 1, 1, NOW(), NOW()),
('OTHER', 'Fulfillment Fee', 'fulfillment fee', 'FULFILLMENT', 1, 1, NOW(), NOW()),
('OTHER', 'Storage Fee', 'storage fee', 'STORAGE', 1, 1, NOW(), NOW()),
('OTHER', 'Long-Term Storage Fee', 'long-term storage fee', 'LONG_STORAGE', 1, 1, NOW(), NOW()),
('OTHER', 'Removal Fee', 'removal fee', 'REMOVAL', 1, 1, NOW(), NOW()),
('OTHER', 'Disposal Fee', 'disposal fee', 'REMOVAL', 1, 1, NOW(), NOW()),
('OTHER', 'Return Fee', 'return fee', 'RETURN_FEE', 1, 1, NOW(), NOW()),
('OTHER', 'Inbound Fee', 'inbound fee', 'INBOUND_FEE', 1, 1, NOW(), NOW()),
('OTHER', 'Subscription Fee', 'subscription fee', 'SUBSCRIPTION', 1, 1, NOW(), NOW()),
('OTHER', 'Promotion', 'promotion', 'PROMOTION', 1, 1, NOW(), NOW()),
('OTHER', 'Coupon', 'coupon', 'COUPON', 1, 1, NOW(), NOW()),
('OTHER', 'Deal Fee', 'deal fee', 'DEAL', 1, 1, NOW(), NOW()),
('OTHER', 'Advertising', 'advertising', 'AD', 1, 1, NOW(), NOW()),
('OTHER', 'Reimbursement', 'reimbursement', 'REIMBURSEMENT', 1, 1, NOW(), NOW()),
('OTHER', 'Reserve', 'reserve', 'RESERVE', 1, 1, NOW(), NOW()),
('OTHER', 'Tax', 'tax', 'MARKETPLACE_TAX', 1, 1, NOW(), NOW()),
('OTHER', 'Adjustment', 'adjustment', 'ADJUSTMENT', 1, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 跨境电商：数据分析（销量统计、销量预测、商品分析）

CREATE TABLE ecom_stock_snapshot (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    snapshot_date DATE           NOT NULL COMMENT '快照日期',
    listing_id    BIGINT         NOT NULL COMMENT '在线商品',
    store_id      BIGINT         NOT NULL COMMENT '店铺',
    fulfillment   VARCHAR(16)             DEFAULT NULL COMMENT '配送方式(字典 ecom_fulfillment)，决定可售数取平台仓还是本地仓',
    platform_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '平台仓可售（空闲库存 ÷ 多件装，组合为套数，整件）',
    local_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '本地仓（店铺默认发货仓）可售',
    sellable_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '按配送方式的可售数，不大于 0 为断货',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_stock_snapshot (listing_id, snapshot_date, tenant_id),
    KEY idx_ecom_stock_snapshot_date (snapshot_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在售 SKU 每日库存快照（定时任务每天记录，断货天数与历史库存曲线）';

CREATE TABLE ecom_forecast (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    listing_id     BIGINT         NOT NULL COMMENT '在线商品',
    store_id       BIGINT         NOT NULL COMMENT '店铺',
    forecast_date  DATE           NOT NULL COMMENT '预测日期',
    qty            DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '预测销量（平台 SKU 件数）',
    method         VARCHAR(16)    NOT NULL COMMENT '预测方法(字典 ecom_forecast_method) MA WMA HOLT SEASONAL',
    generated_time DATETIME       NOT NULL COMMENT '生成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_forecast (listing_id, forecast_date, tenant_id),
    KEY idx_ecom_forecast_date (forecast_date),
    KEY idx_ecom_forecast_generated (generated_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每个 SKU 每天的预测销量（所选方法，定时任务每天从今天起生成 90 天）';

CREATE TABLE ecom_forecast_sku (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    listing_id     BIGINT         NOT NULL COMMENT '在线商品',
    store_id       BIGINT         NOT NULL COMMENT '店铺',
    method         VARCHAR(16)    NOT NULL COMMENT '使用的预测方法(字典 ecom_forecast_method)',
    fixed_method   VARCHAR(16)             DEFAULT NULL COMMENT '手工指定的方法，为空按回测 MAPE 最小自动选择',
    mape_ma        DECIMAL(10, 2)          DEFAULT NULL COMMENT '移动平均的回测 MAPE（%）',
    mape_wma       DECIMAL(10, 2)          DEFAULT NULL COMMENT '加权移动平均的回测 MAPE（%）',
    mape_holt      DECIMAL(10, 2)          DEFAULT NULL COMMENT 'Holt 的回测 MAPE（%）',
    mape_seasonal  DECIMAL(10, 2)          DEFAULT NULL COMMENT '星期季节性的回测 MAPE（%）',
    history_daily  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '最近 28 天实际日均',
    forecast30     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '未来 30 天预测合计',
    forecast60     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '未来 60 天预测合计',
    forecast90     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '未来 90 天预测合计',
    sigma          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '回测日误差的标准差（80% 预测区间 = 预测 ± 1.28σ）',
    generated_time DATETIME       NOT NULL COMMENT '生成时间',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_forecast_sku (listing_id, tenant_id),
    KEY idx_ecom_forecast_sku_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 的预测汇总（方法、各方法回测误差、未来 30 / 60 / 90 天合计）';

CREATE TABLE ecom_forecast_event (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)    NOT NULL COMMENT '名称，如 Prime Day、黑五',
    start_date  DATE           NOT NULL COMMENT '开始日期（含）',
    end_date    DATE           NOT NULL COMMENT '结束日期（含）',
    store_id    BIGINT                  DEFAULT NULL COMMENT '店铺，为空表示全部店铺',
    factor      DECIMAL(8, 4)  NOT NULL DEFAULT 1 COMMENT '系数：区间内的预测量 × 系数，历史销量 ÷ 系数后作为基线；重叠时相乘',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_forecast_event_date (start_date, end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销量预测的促销 / 节假日系数日历';

CREATE TABLE ecom_forecast_setting (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    alpha        DECIMAL(6, 4) NOT NULL DEFAULT 0.3 COMMENT 'Holt 水平平滑系数 α',
    beta         DECIMAL(6, 4) NOT NULL DEFAULT 0.1 COMMENT 'Holt 趋势平滑系数 β',
    window_days  INT           NOT NULL DEFAULT 28 COMMENT '移动平均 / 加权移动平均的天数',
    season_weeks INT           NOT NULL DEFAULT 8 COMMENT '星期系数取最近几周',
    create_time  DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_forecast_setting (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销量预测参数（每个租户一行，没有时取默认值）';

-- 菜单：电商下的分组「数据分析」
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(490, 987, '销量统计', 'C', 'sales-stats', 'ecom/sales-stats/index', 'ecom:salesStats:list', 'BarChartOutlined', 1, 1, 1, NOW(), NOW()),
(491, 987, '销量预测', 'C', 'forecast', 'ecom/forecast/index', 'ecom:forecast:list', 'LineChartOutlined', 2, 1, 1, NOW(), NOW()),
(492, 987, '商品分析', 'C', 'product-analysis', 'ecom/product-analysis/index', 'ecom:productAnalysis:list', 'PieChartOutlined', 3, 1, 1, NOW(), NOW()),
(4900, 490, '销量统计导出', 'F', NULL, NULL, 'ecom:salesStats:export', NULL, 1, 1, 1, NOW(), NOW()),
(4910, 491, '生成预测', 'F', NULL, NULL, 'ecom:forecast:generate', NULL, 1, 1, 1, NOW(), NOW()),
(4911, 491, '预测参数与方法', 'F', NULL, NULL, 'ecom:forecast:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4912, 491, '促销节假日维护', 'F', NULL, NULL, 'ecom:forecast:event', NULL, 3, 1, 1, NOW(), NOW()),
(4920, 492, '商品分析导出', 'F', NULL, NULL, 'ecom:productAnalysis:export', NULL, 1, 1, 1, NOW(), NOW());

-- 系统字典：预测方法、ABC 分类
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(290, '预测方法', 'ecom_forecast_method', 'SYSTEM', 1, '销量预测的方法', NOW(), NOW()),
(291, 'ABC 分类', 'ecom_abc_class', 'SYSTEM', 1, '商品 ABC 分析的类别', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_forecast_method', '移动平均', 'MA', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_forecast_method', '加权移动平均', 'WMA', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_forecast_method', '指数平滑（Holt）', 'HOLT', 'purple', 3, 1, 1, NOW(), NOW()),
('ecom_forecast_method', '星期季节性', 'SEASONAL', 'cyan', 4, 1, 1, NOW(), NOW()),
('ecom_abc_class', 'A 类', 'A', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_abc_class', 'B 类', 'B', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_abc_class', 'C 类', 'C', 'default', 3, 1, 1, NOW(), NOW());

-- 内置角色：电商运营全部；采购员、总经理查看全部页面并可导出；普通用户只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 987 AS menu_id UNION ALL SELECT 5, 490 UNION ALL SELECT 5, 491 UNION ALL SELECT 5, 492 UNION ALL SELECT 5, 4900
    UNION ALL SELECT 5, 4910 UNION ALL SELECT 5, 4911 UNION ALL SELECT 5, 4912 UNION ALL SELECT 5, 4920
    UNION ALL SELECT 4, 17 UNION ALL SELECT 4, 987 UNION ALL SELECT 4, 490 UNION ALL SELECT 4, 491 UNION ALL SELECT 4, 492 UNION ALL SELECT 4, 4900 UNION ALL SELECT 4, 4920
    UNION ALL SELECT 3, 987 UNION ALL SELECT 3, 490 UNION ALL SELECT 3, 491 UNION ALL SELECT 3, 492 UNION ALL SELECT 3, 4900 UNION ALL SELECT 3, 4920
    UNION ALL SELECT 2, 987 UNION ALL SELECT 2, 490 UNION ALL SELECT 2, 491 UNION ALL SELECT 2, 492
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- ============================================================ 广告管理：广告结构、结构日统计、搜索词、广告规则与执行日志、分时策略
-- 店铺上的广告接口授权（与渠道授权分开）
ALTER TABLE ecom_store ADD COLUMN ads_credentials TEXT COMMENT '广告接口授权（JSON，加密保存，enc: 前缀），与渠道授权分开';
ALTER TABLE ecom_store ADD COLUMN ads_sandbox TINYINT NOT NULL DEFAULT 0 COMMENT '广告接口环境 1沙箱 0正式';
ALTER TABLE ecom_store ADD COLUMN ads_base_url VARCHAR(255) DEFAULT NULL COMMENT '广告接口自定义地址，为空按环境取默认地址';
ALTER TABLE ecom_store ADD COLUMN ads_profile_id VARCHAR(32) DEFAULT NULL COMMENT '广告配置文件 ID（亚马逊 profileId）';
ALTER TABLE ecom_store ADD COLUMN ads_sync TINYINT NOT NULL DEFAULT 1 COMMENT '每天自动同步广告结构与报告 1是 0否';
ALTER TABLE ecom_store ADD COLUMN ads_last_sync_time DATETIME DEFAULT NULL COMMENT '最近一次广告同步时间';
ALTER TABLE ecom_store ADD COLUMN ads_last_sync_message VARCHAR(500) DEFAULT NULL COMMENT '最近一次广告同步失败的原因，成功为空';

-- 广告日报关联到活动 / 广告组（已有列的含义不变，利润分析仍按它汇总花费）
ALTER TABLE ecom_ad_daily ADD COLUMN campaign_id BIGINT DEFAULT NULL COMMENT '广告活动(ecom_ad_campaign)';
ALTER TABLE ecom_ad_daily ADD COLUMN ad_group_id BIGINT DEFAULT NULL COMMENT '广告组(ecom_ad_group)，同一 SKU 在多个广告组时为空';
ALTER TABLE ecom_ad_daily ADD COLUMN ad_level VARCHAR(16) NOT NULL DEFAULT 'PRODUCT' COMMENT '粒度 PRODUCT 活动 × SKU / CAMPAIGN 只有活动级数据（导入推广商品数据后删除）';
CREATE INDEX idx_ecom_ad_daily_campaign ON ecom_ad_daily (campaign_id, ad_date);

DROP TABLE IF EXISTS ecom_ad_campaign;
CREATE TABLE ecom_ad_campaign (
    id                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id            BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id             BIGINT         NOT NULL COMMENT '店铺',
    ad_type              VARCHAR(16)    NOT NULL DEFAULT 'SP' COMMENT '广告类型(字典 ecom_ad_type) SP SB SD GENERIC',
    name                 VARCHAR(128)   NOT NULL COMMENT '活动名称',
    platform_campaign_id VARCHAR(64)             DEFAULT NULL COMMENT '平台活动 ID',
    status               VARCHAR(16)    NOT NULL DEFAULT 'ENABLED' COMMENT '状态(字典 ecom_ad_status) ENABLED PAUSED ARCHIVED',
    daily_budget         DECIMAL(18, 2)          DEFAULT NULL COMMENT '日预算（店铺币种）',
    bidding_strategy     VARCHAR(32)             DEFAULT NULL COMMENT '竞价策略（平台取值）',
    targeting_type       VARCHAR(16)             DEFAULT NULL COMMENT '投放方式 AUTO MANUAL',
    start_date           DATE                    DEFAULT NULL COMMENT '开始日期',
    end_date             DATE                    DEFAULT NULL COMMENT '结束日期',
    tags                 VARCHAR(255)            DEFAULT NULL COMMENT '标签（逗号分隔，规则按标签筛选）',
    remark               VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    synced_time          DATETIME                DEFAULT NULL COMMENT '最近一次从广告接口同步的时间',
    create_by            BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time          DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time          DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_ad_campaign_name (store_id, name),
    KEY idx_ecom_ad_campaign_pid (store_id, platform_campaign_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告活动';

DROP TABLE IF EXISTS ecom_ad_group;
CREATE TABLE ecom_ad_group (
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id            BIGINT         NOT NULL COMMENT '店铺',
    campaign_id         BIGINT         NOT NULL COMMENT '广告活动',
    name                VARCHAR(128)   NOT NULL COMMENT '广告组名称',
    platform_ad_group_id VARCHAR(64)            DEFAULT NULL COMMENT '平台广告组 ID',
    status              VARCHAR(16)    NOT NULL DEFAULT 'ENABLED' COMMENT '状态(字典 ecom_ad_status)',
    default_bid         DECIMAL(18, 2)          DEFAULT NULL COMMENT '默认竞价（店铺币种）',
    synced_time         DATETIME                DEFAULT NULL COMMENT '最近一次从广告接口同步的时间',
    create_time         DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_ad_group_campaign (campaign_id, name),
    KEY idx_ecom_ad_group_pid (store_id, platform_ad_group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告组';

DROP TABLE IF EXISTS ecom_ad_target;
CREATE TABLE ecom_ad_target (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id           BIGINT         NOT NULL COMMENT '店铺',
    campaign_id        BIGINT         NOT NULL COMMENT '广告活动',
    ad_group_id        BIGINT         NOT NULL COMMENT '广告组',
    target_type        VARCHAR(16)    NOT NULL COMMENT '投放类型 KEYWORD PRODUCT AUTO NEGATIVE',
    target_text        VARCHAR(255)   NOT NULL COMMENT '关键词或投放表达式',
    match_type         VARCHAR(16)             DEFAULT NULL COMMENT '匹配方式(字典 ecom_ad_match_type)，商品 / 自动投放为空',
    platform_target_id VARCHAR(64)             DEFAULT NULL COMMENT '平台投放 ID（关键词 ID / 投放 ID）',
    status             VARCHAR(16)    NOT NULL DEFAULT 'ENABLED' COMMENT '状态(字典 ecom_ad_status)',
    bid                DECIMAL(18, 2)          DEFAULT NULL COMMENT '当前竞价（店铺币种）',
    base_bid           DECIMAL(18, 2)          DEFAULT NULL COMMENT '基准竞价：分时调价按 基准竞价 × 系数 计算，规则按它调整',
    schedule_factor    DECIMAL(6, 2)           DEFAULT NULL COMMENT '当前生效的分时系数',
    synced_time        DATETIME                DEFAULT NULL COMMENT '最近一次从广告接口同步的时间',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_ad_target_group (ad_group_id),
    KEY idx_ecom_ad_target_campaign (campaign_id),
    KEY idx_ecom_ad_target_pid (store_id, platform_target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告投放（关键词 / 商品投放）';

DROP TABLE IF EXISTS ecom_ad_stat;
CREATE TABLE ecom_ad_stat (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id    BIGINT         NOT NULL COMMENT '店铺',
    stat_date   DATE           NOT NULL COMMENT '日期',
    level       VARCHAR(16)    NOT NULL COMMENT '层级 CAMPAIGN TARGET',
    entity_id   BIGINT         NOT NULL COMMENT '活动 / 投放 ID',
    campaign_id BIGINT         NOT NULL COMMENT '广告活动',
    ad_group_id BIGINT                  DEFAULT NULL COMMENT '广告组（投放级）',
    impressions BIGINT         NOT NULL DEFAULT 0 COMMENT '曝光',
    clicks      BIGINT         NOT NULL DEFAULT 0 COMMENT '点击',
    spend       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '花费（店铺币种）',
    sales       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '广告销售额',
    orders      INT            NOT NULL DEFAULT 0 COMMENT '广告订单',
    units       INT            NOT NULL DEFAULT 0 COMMENT '广告销量（件）',
    currency    VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_ad_stat (store_id, stat_date, level, entity_id, tenant_id),
    KEY idx_ecom_ad_stat_campaign (campaign_id, stat_date),
    KEY idx_ecom_ad_stat_group (ad_group_id, stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告结构日统计（活动报告、投放报告）';

DROP TABLE IF EXISTS ecom_ad_search_term;
CREATE TABLE ecom_ad_search_term (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id     BIGINT         NOT NULL COMMENT '店铺',
    stat_date    DATE           NOT NULL COMMENT '日期',
    campaign_id  BIGINT         NOT NULL COMMENT '广告活动',
    ad_group_id  BIGINT                  DEFAULT NULL COMMENT '广告组',
    target_id    BIGINT                  DEFAULT NULL COMMENT '投放',
    keyword_text VARCHAR(255)            DEFAULT NULL COMMENT '触发的投放（关键词或表达式）',
    match_type   VARCHAR(32)             DEFAULT NULL COMMENT '匹配方式',
    search_term  VARCHAR(255)   NOT NULL COMMENT '客户搜索词',
    impressions  BIGINT         NOT NULL DEFAULT 0 COMMENT '曝光',
    clicks       BIGINT         NOT NULL DEFAULT 0 COMMENT '点击',
    spend        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '花费（店铺币种）',
    sales        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '广告销售额',
    orders       INT            NOT NULL DEFAULT 0 COMMENT '广告订单',
    currency     VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_ad_search_term_date (store_id, stat_date),
    KEY idx_ecom_ad_search_term_campaign (campaign_id, stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告搜索词报告';

DROP TABLE IF EXISTS ecom_ad_rule;
CREATE TABLE ecom_ad_rule (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name             VARCHAR(64)    NOT NULL COMMENT '规则名称',
    status           TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    level            VARCHAR(16)    NOT NULL COMMENT '对象层级 CAMPAIGN AD_GROUP TARGET',
    store_ids        VARCHAR(500)            DEFAULT NULL COMMENT '店铺 ID（逗号分隔，为空不限）',
    campaign_keyword VARCHAR(128)            DEFAULT NULL COMMENT '活动名称包含',
    tags             VARCHAR(255)            DEFAULT NULL COMMENT '活动标签（逗号分隔，任一匹配）',
    ad_type          VARCHAR(16)             DEFAULT NULL COMMENT '广告类型(字典 ecom_ad_type)，为空不限',
    window_days      INT            NOT NULL DEFAULT 7 COMMENT '统计窗口：近 N 天（截至昨天）',
    conditions_json  VARCHAR(2000)  NOT NULL COMMENT '条件（JSON 数组：metric 指标、operator 运算、value 值，全部满足）',
    action           VARCHAR(32)    NOT NULL COMMENT '动作(字典 ecom_ad_rule_action)',
    action_value     DECIMAL(18, 4)          DEFAULT NULL COMMENT '动作的值：百分比调整为 ±%，固定值为竞价 / 预算',
    min_value        DECIMAL(18, 2)          DEFAULT NULL COMMENT '调整后的下限',
    max_value        DECIMAL(18, 2)          DEFAULT NULL COMMENT '调整后的上限',
    run_mode         VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '执行方式 MANUAL 手动 DAILY 每天',
    run_hour         INT            NOT NULL DEFAULT 6 COMMENT '每天执行的整点（0-23）',
    auto_apply       TINYINT        NOT NULL DEFAULT 0 COMMENT '自动执行 1是 0否（否则只生成建议）',
    owner_id         BIGINT                  DEFAULT NULL COMMENT '负责人（接收执行提醒）',
    last_run_time    DATETIME                DEFAULT NULL COMMENT '最近一次执行时间',
    last_run_message VARCHAR(500)            DEFAULT NULL COMMENT '最近一次执行结果',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by        BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告规则';

DROP TABLE IF EXISTS ecom_ad_rule_log;
CREATE TABLE ecom_ad_rule_log (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_id      BIGINT                  DEFAULT NULL COMMENT '广告规则（手工修改为空）',
    source       VARCHAR(16)    NOT NULL DEFAULT 'RULE' COMMENT '来源 RULE 规则 MANUAL 手工修改',
    run_time     DATETIME       NOT NULL COMMENT '执行时间',
    store_id     BIGINT         NOT NULL COMMENT '店铺',
    level        VARCHAR(16)    NOT NULL COMMENT '对象层级',
    entity_id    BIGINT         NOT NULL COMMENT '对象 ID（活动 / 广告组 / 投放）',
    campaign_id  BIGINT                  DEFAULT NULL COMMENT '广告活动',
    entity_name  VARCHAR(255)            DEFAULT NULL COMMENT '对象名称',
    action       VARCHAR(32)    NOT NULL COMMENT '动作(字典 ecom_ad_rule_action)',
    old_value    DECIMAL(18, 2)          DEFAULT NULL COMMENT '调整前的竞价 / 预算',
    new_value    DECIMAL(18, 2)          DEFAULT NULL COMMENT '调整后的竞价 / 预算',
    old_status   VARCHAR(16)             DEFAULT NULL COMMENT '调整前的状态',
    new_status   VARCHAR(16)             DEFAULT NULL COMMENT '调整后的状态',
    metrics      VARCHAR(1000)           DEFAULT NULL COMMENT '命中时的指标快照（JSON）',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 ecom_ad_log_status)',
    message      VARCHAR(500)            DEFAULT NULL COMMENT '说明 / 失败原因',
    applied_time DATETIME                DEFAULT NULL COMMENT '执行时间',
    applied_by   BIGINT                  DEFAULT NULL COMMENT '执行人（自动执行为空）',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_ad_rule_log_rule (rule_id, run_time),
    KEY idx_ecom_ad_rule_log_store (store_id, run_time),
    KEY idx_ecom_ad_rule_log_entity (level, entity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告执行日志 / 建议';

DROP TABLE IF EXISTS ecom_ad_schedule;
CREATE TABLE ecom_ad_schedule (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    campaign_id       BIGINT         NOT NULL COMMENT '广告活动',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    factors_json      VARCHAR(2000)  NOT NULL COMMENT '星期（周一起）× 小时的竞价系数表（JSON 7 × 24）',
    min_bid           DECIMAL(18, 2)          DEFAULT NULL COMMENT '最低竞价',
    max_bid           DECIMAL(18, 2)          DEFAULT NULL COMMENT '最高竞价',
    last_applied_time DATETIME                DEFAULT NULL COMMENT '最近一次应用时间',
    last_factor       DECIMAL(6, 2)           DEFAULT NULL COMMENT '最近一次应用的系数',
    last_message      VARCHAR(500)            DEFAULT NULL COMMENT '最近一次应用的结果',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_ad_schedule (campaign_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广告分时策略（活动级竞价系数）';

-- 菜单：结算与分析下的广告规则页面；广告数据、广告规则的按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(480, 984, '广告规则', 'C', 'ad-rule', 'ecom/ad-rule/index', 'ecom:adRule:list', 'ThunderboltOutlined', 5, 1, 1, NOW(), NOW()),
(4800, 430, '广告结构维护', 'F', NULL, NULL, 'ecom:ad:edit', NULL, 3, 1, 1, NOW(), NOW()),
(4801, 430, '广告授权', 'F', NULL, NULL, 'ecom:ad:auth', NULL, 4, 1, 1, NOW(), NOW()),
(4802, 430, '广告同步', 'F', NULL, NULL, 'ecom:ad:sync', NULL, 5, 1, 1, NOW(), NOW()),
(4803, 430, '分时策略维护', 'F', NULL, NULL, 'ecom:ad:schedule', NULL, 6, 1, 1, NOW(), NOW()),
(4810, 480, '规则新增', 'F', NULL, NULL, 'ecom:adRule:add', NULL, 1, 1, 1, NOW(), NOW()),
(4811, 480, '规则修改', 'F', NULL, NULL, 'ecom:adRule:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4812, 480, '规则删除', 'F', NULL, NULL, 'ecom:adRule:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4813, 480, '规则执行 / 处理建议', 'F', NULL, NULL, 'ecom:adRule:execute', NULL, 4, 1, 1, NOW(), NOW());

-- 系统字典：广告类型、广告状态、匹配方式、规则动作、执行日志状态
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(280, '广告类型', 'ecom_ad_type', 'SYSTEM', 1, '广告活动的类型', NOW(), NOW()),
(281, '广告状态', 'ecom_ad_status', 'SYSTEM', 1, '广告活动、广告组、投放的状态', NOW(), NOW()),
(282, '广告匹配方式', 'ecom_ad_match_type', 'SYSTEM', 1, '关键词投放的匹配方式', NOW(), NOW()),
(283, '广告规则动作', 'ecom_ad_rule_action', 'SYSTEM', 1, '广告规则命中后执行的动作', NOW(), NOW()),
(284, '广告执行状态', 'ecom_ad_log_status', 'SYSTEM', 1, '广告执行日志 / 建议的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_ad_type', '商品推广 SP', 'SP', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_ad_type', '品牌推广 SB', 'SB', 'purple', 2, 1, 1, NOW(), NOW()),
('ecom_ad_type', '展示型推广 SD', 'SD', 'cyan', 3, 1, 1, NOW(), NOW()),
('ecom_ad_type', '平台通用', 'GENERIC', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_ad_status', '启用', 'ENABLED', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_ad_status', '暂停', 'PAUSED', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_ad_status', '已归档', 'ARCHIVED', 'default', 3, 1, 1, NOW(), NOW()),
('ecom_ad_match_type', '精准', 'EXACT', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_ad_match_type', '词组', 'PHRASE', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_ad_match_type', '广泛', 'BROAD', 'geekblue', 3, 1, 1, NOW(), NOW()),
('ecom_ad_match_type', '否定精准', 'NEGATIVE_EXACT', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_ad_match_type', '否定词组', 'NEGATIVE_PHRASE', 'volcano', 5, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '竞价按百分比调整', 'BID_PERCENT', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '竞价调整到固定值', 'BID_SET', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '日预算按百分比调整', 'BUDGET_PERCENT', 'purple', 3, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '日预算调整到固定值', 'BUDGET_SET', 'purple', 4, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '暂停', 'PAUSE', 'orange', 5, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '启用', 'ENABLE', 'green', 6, 1, 1, NOW(), NOW()),
('ecom_ad_rule_action', '仅提醒', 'NOTIFY', 'default', 7, 1, 1, NOW(), NOW()),
('ecom_ad_log_status', '待处理', 'PENDING', 'processing', 1, 1, 1, NOW(), NOW()),
('ecom_ad_log_status', '已执行', 'APPLIED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_ad_log_status', '已手工执行', 'MANUAL', 'cyan', 3, 1, 1, NOW(), NOW()),
('ecom_ad_log_status', '失败', 'FAILED', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_ad_log_status', '仅提醒', 'NOTIFIED', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_ad_log_status', '已忽略', 'IGNORED', 'default', 6, 1, 1, NOW(), NOW());

-- 内置角色：电商运营全部；普通用户、总经理只读（广告数据页面已有授权）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 480 AS menu_id UNION ALL SELECT 5, 4800 UNION ALL SELECT 5, 4801 UNION ALL SELECT 5, 4802 UNION ALL SELECT 5, 4803
    UNION ALL SELECT 5, 4810 UNION ALL SELECT 5, 4811 UNION ALL SELECT 5, 4812 UNION ALL SELECT 5, 4813
    UNION ALL SELECT 2, 480 UNION ALL SELECT 3, 480
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- ---------------------------------------------------------------- 跨境电商：客服与评价（买家消息、回复模板、评价监控、请求评价、售后工单）

DROP TABLE IF EXISTS ecom_message_thread;
CREATE TABLE ecom_message_thread (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT       NOT NULL COMMENT '店铺',
    platform          VARCHAR(16)  NOT NULL COMMENT '平台(字典 ecom_platform)',
    conversation_id   VARCHAR(128) NOT NULL COMMENT '平台会话 ID',
    buyer_id          VARCHAR(128)          DEFAULT NULL COMMENT '买家在平台的 ID（发送消息时使用）',
    buyer_name        VARCHAR(128)          DEFAULT NULL COMMENT '买家名称',
    order_id          BIGINT                DEFAULT NULL COMMENT '关联的平台订单',
    platform_order_no VARCHAR(64)           DEFAULT NULL COMMENT '关联的平台订单号',
    subject           VARCHAR(255)          DEFAULT NULL COMMENT '主题',
    last_message      VARCHAR(500)          DEFAULT NULL COMMENT '最近一条消息（摘要）',
    last_message_time DATETIME              DEFAULT NULL COMMENT '最近消息时间',
    unread            INT          NOT NULL DEFAULT 0 COMMENT '未读的买家消息数',
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 ecom_cs_thread_status) PENDING待回复 REPLIED已回复 CLOSED已关闭',
    reply_deadline    DATETIME              DEFAULT NULL COMMENT '回复截止：最早一条未回复的买家消息 + 平台要求的回复时限，超过为超时',
    owner_id          BIGINT                DEFAULT NULL COMMENT '负责人（默认店铺负责人）',
    tags              VARCHAR(255)          DEFAULT NULL COMMENT '标签（逗号分隔）',
    remark            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_message_thread (store_id, conversation_id, tenant_id),
    KEY idx_ecom_message_thread_time (last_message_time),
    KEY idx_ecom_message_thread_status (status, reply_deadline),
    KEY idx_ecom_message_thread_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '买家消息会话（统一收件箱）';

DROP TABLE IF EXISTS ecom_message;
CREATE TABLE ecom_message (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    thread_id           BIGINT        NOT NULL COMMENT '会话',
    platform_message_id VARCHAR(128)           DEFAULT NULL COMMENT '平台消息 ID（同步去重；未发送成功的回复为空）',
    direction           VARCHAR(8)    NOT NULL COMMENT '方向 BUYER买家 SELLER卖家',
    msg_type            VARCHAR(32)   NOT NULL DEFAULT 'TEXT' COMMENT '消息类型（平台原值，如 TEXT、IMAGE、ORDER）',
    content             VARCHAR(4000)          DEFAULT NULL COMMENT '内容',
    attachments         VARCHAR(2000)          DEFAULT NULL COMMENT '附件地址（JSON 数组）',
    sent_time           DATETIME      NOT NULL COMMENT '发送时间',
    send_status         VARCHAR(16)            DEFAULT NULL COMMENT '发送状态(字典 ecom_cs_send_status，卖家回复) SENDING发送中 SENT已发送 FAILED发送失败',
    error_message       VARCHAR(500)           DEFAULT NULL COMMENT '发送失败的原因',
    template_id         BIGINT                 DEFAULT NULL COMMENT '使用的回复模板',
    sender_id           BIGINT                 DEFAULT NULL COMMENT '回复人（本系统用户）',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_message (thread_id, platform_message_id, tenant_id),
    KEY idx_ecom_message_thread (thread_id, sent_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '买家消息';

DROP TABLE IF EXISTS ecom_reply_template;
CREATE TABLE ecom_reply_template (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)   NOT NULL COMMENT '名称',
    language    VARCHAR(16)   NOT NULL COMMENT '语言(业务字典 ecom_cs_language)',
    platform    VARCHAR(16)            DEFAULT NULL COMMENT '适用平台(字典 ecom_platform)，为空不限',
    scene       VARCHAR(64)            DEFAULT NULL COMMENT '场景（如 物流查询、催评、差评回复）',
    content     VARCHAR(2000) NOT NULL COMMENT '内容，变量 {buyerName} {orderNo} {trackingNo} {carrier} {productName} {storeName}',
    sort        INT           NOT NULL DEFAULT 0 COMMENT '顺序',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_reply_template (name, language, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '快捷回复模板';

DROP TABLE IF EXISTS ecom_review;
CREATE TABLE ecom_review (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT        NOT NULL COMMENT '店铺',
    platform          VARCHAR(16)   NOT NULL COMMENT '平台(字典 ecom_platform)',
    review_id         VARCHAR(128)  NOT NULL COMMENT '平台评价 ID（导入时没有的按内容生成）',
    platform_item_id  VARCHAR(64)            DEFAULT NULL COMMENT '平台商品 ID（ASIN / Item ID）',
    seller_sku        VARCHAR(64)            DEFAULT NULL COMMENT '平台 SKU',
    listing_id        BIGINT                 DEFAULT NULL COMMENT '在线商品',
    rating            TINYINT       NOT NULL COMMENT '评分 1-5',
    title             VARCHAR(255)           DEFAULT NULL COMMENT '标题',
    content           VARCHAR(4000)          DEFAULT NULL COMMENT '内容',
    reviewer          VARCHAR(128)           DEFAULT NULL COMMENT '评价人',
    review_date       DATE          NOT NULL COMMENT '评价日期',
    order_id          BIGINT                 DEFAULT NULL COMMENT '关联的平台订单',
    platform_order_no VARCHAR(64)            DEFAULT NULL COMMENT '关联的平台订单号',
    replied           TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已回复',
    reply_content     VARCHAR(2000)          DEFAULT NULL COMMENT '回复内容',
    reply_time        DATETIME               DEFAULT NULL COMMENT '回复时间',
    handle_status     VARCHAR(16)   NOT NULL DEFAULT 'NONE' COMMENT '处理状态(字典 ecom_cs_review_status) NONE无需处理 PENDING待处理（低分自动生成） PROCESSING处理中 RESOLVED已处理',
    handler_id        BIGINT                 DEFAULT NULL COMMENT '处理人（低分评价默认店铺负责人）',
    handle_note       VARCHAR(500)           DEFAULT NULL COMMENT '处理说明',
    source            VARCHAR(16)   NOT NULL DEFAULT 'IMPORT' COMMENT '来源 IMPORT导入 SYNC平台同步',
    create_time       DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_review (store_id, review_id, tenant_id),
    KEY idx_ecom_review_sku (store_id, seller_sku, review_date),
    KEY idx_ecom_review_date (review_date),
    KEY idx_ecom_review_status (handle_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品评价';

DROP TABLE IF EXISTS ecom_feedback;
CREATE TABLE ecom_feedback (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT        NOT NULL COMMENT '店铺',
    platform          VARCHAR(16)   NOT NULL COMMENT '平台(字典 ecom_platform)',
    feedback_id       VARCHAR(128)  NOT NULL COMMENT '反馈 ID（导入时没有的按订单号 + 日期生成）',
    rating            TINYINT       NOT NULL COMMENT '评分 1-5',
    comments          VARCHAR(2000)          DEFAULT NULL COMMENT '买家留言',
    rater             VARCHAR(128)           DEFAULT NULL COMMENT '评价人（邮箱 / 名称）',
    feedback_date     DATE          NOT NULL COMMENT '日期',
    order_id          BIGINT                 DEFAULT NULL COMMENT '关联的平台订单',
    platform_order_no VARCHAR(64)            DEFAULT NULL COMMENT '平台订单号',
    response          VARCHAR(2000)          DEFAULT NULL COMMENT '卖家回复',
    handle_status     VARCHAR(16)   NOT NULL DEFAULT 'NONE' COMMENT '处理状态(字典 ecom_cs_review_status)',
    handler_id        BIGINT                 DEFAULT NULL COMMENT '处理人',
    handle_note       VARCHAR(500)           DEFAULT NULL COMMENT '处理说明',
    create_time       DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_feedback (store_id, feedback_id, tenant_id),
    KEY idx_ecom_feedback_date (feedback_date),
    KEY idx_ecom_feedback_status (handle_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '店铺反馈（如亚马逊 Seller Feedback）';

DROP TABLE IF EXISTS ecom_review_request;
CREATE TABLE ecom_review_request (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT       NOT NULL COMMENT '店铺',
    order_id          BIGINT       NOT NULL COMMENT '平台订单',
    platform_order_no VARCHAR(64)  NOT NULL COMMENT '平台订单号',
    status            VARCHAR(16)  NOT NULL COMMENT '结果 SENT已发送 NOT_ELIGIBLE不符合条件 FAILED失败',
    message           VARCHAR(500)          DEFAULT NULL COMMENT '说明 / 失败原因',
    request_time      DATETIME     NOT NULL COMMENT '请求时间',
    create_by         BIGINT                DEFAULT NULL COMMENT '操作人（定时任务为空）',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_review_request (order_id, tenant_id),
    KEY idx_ecom_review_request_store (store_id, request_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '请求评价记录（亚马逊 Solicitations，每张订单只能请求一次）';

DROP TABLE IF EXISTS ecom_cs_ticket;
CREATE TABLE ecom_cs_ticket (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT       NOT NULL COMMENT '店铺',
    source_type       VARCHAR(16)  NOT NULL COMMENT '来源 THREAD买家消息 REVIEW评价 FEEDBACK店铺反馈',
    source_id         BIGINT       NOT NULL COMMENT '来源记录',
    ticket_type       VARCHAR(16)  NOT NULL COMMENT '类型(字典 ecom_cs_ticket_type) RETURN_REFUND退货退款 REFUND_ONLY仅退款 RESEND补发',
    order_id          BIGINT       NOT NULL COMMENT '原平台订单',
    platform_order_no VARCHAR(64)  NOT NULL COMMENT '原平台订单号',
    return_id         BIGINT                DEFAULT NULL COMMENT '生成的退货退款单',
    return_no         VARCHAR(32)           DEFAULT NULL COMMENT '退货退款单号',
    resend_order_id   BIGINT                DEFAULT NULL COMMENT '生成的补发订单',
    resend_order_no   VARCHAR(64)           DEFAULT NULL COMMENT '补发订单号',
    remark            VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    dept_id           BIGINT                DEFAULT NULL COMMENT '制单人部门',
    create_by         BIGINT                DEFAULT NULL COMMENT '制单人',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_cs_ticket_source (source_type, source_id),
    KEY idx_ecom_cs_ticket_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '售后工单（会话 / 评价转退货退款或补发）';

-- 菜单：电商下的「客服」分组（买家消息、评价管理、回复模板、售后工单）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(485, 986, '买家消息', 'C', 'cs-message', 'ecom/cs-message/index', 'ecom:csMessage:list', 'MessageOutlined', 1, 1, 1, NOW(), NOW()),
(486, 986, '评价管理', 'C', 'cs-review', 'ecom/cs-review/index', 'ecom:csReview:list', 'StarOutlined', 2, 1, 1, NOW(), NOW()),
(487, 986, '回复模板', 'C', 'cs-template', 'ecom/cs-template/index', 'ecom:csTemplate:list', 'FileTextOutlined', 3, 1, 1, NOW(), NOW()),
(488, 986, '售后工单', 'C', 'cs-ticket', 'ecom/cs-ticket/index', 'ecom:csTicket:list', 'ToolOutlined', 4, 1, 1, NOW(), NOW()),
(4850, 485, '回复消息', 'F', NULL, NULL, 'ecom:csMessage:reply', NULL, 1, 1, 1, NOW(), NOW()),
(4851, 485, '会话处理（负责人 / 标签 / 关闭 / 关联订单）', 'F', NULL, NULL, 'ecom:csMessage:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4852, 485, '同步消息', 'F', NULL, NULL, 'ecom:csMessage:sync', NULL, 3, 1, 1, NOW(), NOW()),
(4860, 486, '导入评价', 'F', NULL, NULL, 'ecom:csReview:import', NULL, 1, 1, 1, NOW(), NOW()),
(4861, 486, '回复评价', 'F', NULL, NULL, 'ecom:csReview:reply', NULL, 2, 1, 1, NOW(), NOW()),
(4862, 486, '处理评价', 'F', NULL, NULL, 'ecom:csReview:edit', NULL, 3, 1, 1, NOW(), NOW()),
(4863, 486, '同步评价', 'F', NULL, NULL, 'ecom:csReview:sync', NULL, 4, 1, 1, NOW(), NOW()),
(4864, 486, '请求评价', 'F', NULL, NULL, 'ecom:csReview:request', NULL, 5, 1, 1, NOW(), NOW()),
(4865, 486, '删除评价', 'F', NULL, NULL, 'ecom:csReview:remove', NULL, 6, 1, 1, NOW(), NOW()),
(4870, 487, '模板新增', 'F', NULL, NULL, 'ecom:csTemplate:add', NULL, 1, 1, 1, NOW(), NOW()),
(4871, 487, '模板修改', 'F', NULL, NULL, 'ecom:csTemplate:edit', NULL, 2, 1, 1, NOW(), NOW()),
(4872, 487, '模板删除', 'F', NULL, NULL, 'ecom:csTemplate:remove', NULL, 3, 1, 1, NOW(), NOW()),
(4880, 488, '转售后', 'F', NULL, NULL, 'ecom:csTicket:add', NULL, 1, 1, 1, NOW(), NOW());

-- 字典：会话状态、评价处理状态、售后类型、发送状态（系统字典），模板语言（业务字典）；渠道同步类型增加客服的三种
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(285, '客服会话状态', 'ecom_cs_thread_status', 'SYSTEM', 1, '买家消息会话的回复状态', NOW(), NOW()),
(286, '评价处理状态', 'ecom_cs_review_status', 'SYSTEM', 1, '评价、店铺反馈的处理状态（低分自动待处理）', NOW(), NOW()),
(287, '模板语言', 'ecom_cs_language', 'BUSINESS', 1, '快捷回复模板的语言', NOW(), NOW()),
(288, '售后类型', 'ecom_cs_ticket_type', 'SYSTEM', 1, '会话 / 评价转售后的类型', NOW(), NOW()),
(289, '消息发送状态', 'ecom_cs_send_status', 'SYSTEM', 1, '卖家回复的发送状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_cs_thread_status', '待回复', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_cs_thread_status', '已回复', 'REPLIED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_cs_thread_status', '已关闭', 'CLOSED', 'default', 3, 1, 1, NOW(), NOW()),
('ecom_cs_review_status', '无需处理', 'NONE', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_cs_review_status', '待处理', 'PENDING', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_cs_review_status', '处理中', 'PROCESSING', 'processing', 3, 1, 1, NOW(), NOW()),
('ecom_cs_review_status', '已处理', 'RESOLVED', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_cs_language', '英语', 'en', 'blue', 1, 1, 0, NOW(), NOW()),
('ecom_cs_language', '德语', 'de', 'gold', 2, 1, 0, NOW(), NOW()),
('ecom_cs_language', '日语', 'ja', 'red', 3, 1, 0, NOW(), NOW()),
('ecom_cs_language', '西班牙语', 'es', 'orange', 4, 1, 0, NOW(), NOW()),
('ecom_cs_language', '法语', 'fr', 'purple', 5, 1, 0, NOW(), NOW()),
('ecom_cs_language', '意大利语', 'it', 'green', 6, 1, 0, NOW(), NOW()),
('ecom_cs_language', '中文', 'zh', 'cyan', 7, 1, 0, NOW(), NOW()),
('ecom_cs_ticket_type', '退货退款', 'RETURN_REFUND', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_cs_ticket_type', '仅退款', 'REFUND_ONLY', 'gold', 2, 1, 1, NOW(), NOW()),
('ecom_cs_ticket_type', '补发', 'RESEND', 'blue', 3, 1, 1, NOW(), NOW()),
('ecom_cs_send_status', '发送中', 'SENDING', 'processing', 1, 1, 1, NOW(), NOW()),
('ecom_cs_send_status', '已发送', 'SENT', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_cs_send_status', '发送失败', 'FAILED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_sync_type', '买家消息', 'MESSAGES', 'geekblue', 7, 1, 1, NOW(), NOW()),
('ecom_sync_type', '评价', 'REVIEWS', 'magenta', 8, 1, 1, NOW(), NOW()),
('ecom_sync_type', '请求评价', 'REVIEW_REQUEST', 'lime', 9, 1, 1, NOW(), NOW());

-- 示例回复模板（英语、德语、日语、西班牙语）
INSERT INTO ecom_reply_template (name, language, platform, scene, content, sort, status, create_time, update_time) VALUES
('物流查询', 'en', NULL, '物流查询', 'Hi {buyerName}, thank you for your message. Your order {orderNo} has been shipped with {carrier}, tracking number {trackingNo}. Please allow a few days for the tracking to update. Best regards, {storeName}', 1, 1, NOW(), NOW()),
('物流查询', 'de', NULL, '物流查询', 'Hallo {buyerName}, vielen Dank für Ihre Nachricht. Ihre Bestellung {orderNo} wurde mit {carrier} versandt, Sendungsnummer {trackingNo}. Mit freundlichen Grüßen, {storeName}', 2, 1, NOW(), NOW()),
('物流查询', 'ja', NULL, '物流查询', '{buyerName} 様、お問い合わせありがとうございます。ご注文 {orderNo} は {carrier} にて発送済みです。追跡番号：{trackingNo}。{storeName}', 3, 1, NOW(), NOW()),
('物流查询', 'es', NULL, '物流查询', 'Hola {buyerName}, gracias por su mensaje. Su pedido {orderNo} ha sido enviado con {carrier}, número de seguimiento {trackingNo}. Saludos, {storeName}', 4, 1, NOW(), NOW()),
('差评回复', 'en', NULL, '差评回复', 'Dear {buyerName}, we are sorry that {productName} did not meet your expectations. Please contact us with your order {orderNo} and we will arrange a replacement or refund right away.', 5, 1, NOW(), NOW());

-- 内置角色：电商运营全部；总经理、普通用户只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 986 OR m.parent_id = 986 OR m.parent_id BETWEEN 485 AND 489)
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 986 OR m.parent_id = 986)
WHERE r.id IN (2, 3) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第四轮：订单增强（手工单与补发、标签与备注、地址校验与黑名单、物流轨迹、利润预估、部分发货回传、多包裹退货、批量操作） =====

-- 订单：类型与来源、地址校验结果、命中的黑名单、利润预估
ALTER TABLE ecom_order ADD COLUMN order_type VARCHAR(16) NOT NULL DEFAULT 'PLATFORM' COMMENT '订单类型(字典 ecom_order_type) PLATFORM平台订单 MANUAL手工订单 RESEND补发单';
ALTER TABLE ecom_order ADD COLUMN source_order_id BIGINT DEFAULT NULL COMMENT '补发单的原订单';
ALTER TABLE ecom_order ADD COLUMN source_ticket_id BIGINT DEFAULT NULL COMMENT '补发单关联的售后工单';
ALTER TABLE ecom_order ADD COLUMN source_return_id BIGINT DEFAULT NULL COMMENT '补发单关联的退货退款单';
ALTER TABLE ecom_order ADD COLUMN address_issue VARCHAR(500) DEFAULT NULL COMMENT '地址校验发现的问题（为空表示通过）';
ALTER TABLE ecom_order ADD COLUMN address_invalid TINYINT NOT NULL DEFAULT 0 COMMENT '地址校验不通过（必填缺失、邮编 / 电话格式、PO Box） 1是 0否';
ALTER TABLE ecom_order ADD COLUMN remote_area TINYINT NOT NULL DEFAULT 0 COMMENT '收件地址在偏远地区 1是 0否';
ALTER TABLE ecom_order ADD COLUMN blacklist_id BIGINT DEFAULT NULL COMMENT '命中的买家黑名单';
ALTER TABLE ecom_order ADD COLUMN est_channel_id BIGINT DEFAULT NULL COMMENT '利润预估使用的物流渠道';
ALTER TABLE ecom_order ADD COLUMN est_weight DECIMAL(12, 3) DEFAULT NULL COMMENT '利润预估使用的重量（kg）';
ALTER TABLE ecom_order ADD COLUMN est_shipping DECIMAL(18, 2) DEFAULT NULL COMMENT '预估运费（本位币）';
ALTER TABLE ecom_order ADD COLUMN est_fee DECIMAL(18, 2) DEFAULT NULL COMMENT '预估平台费用（本位币，佣金 + 配送费 + 其他 + 税）';
ALTER TABLE ecom_order ADD COLUMN est_cost DECIMAL(18, 2) DEFAULT NULL COMMENT '预估商品成本（本位币，按物料参考成本）';
ALTER TABLE ecom_order ADD COLUMN est_profit DECIMAL(18, 2) DEFAULT NULL COMMENT '预估利润（本位币）';
ALTER TABLE ecom_order ADD COLUMN est_profit_rate DECIMAL(10, 2) DEFAULT NULL COMMENT '预估利润率（%）';
CREATE INDEX idx_ecom_order_source ON ecom_order (source_order_id);

-- 包裹：物流轨迹汇总、按包裹回传平台的结果
ALTER TABLE ecom_package ADD COLUMN track_status VARCHAR(16) DEFAULT NULL COMMENT '轨迹状态(字典 ecom_track_status)';
ALTER TABLE ecom_package ADD COLUMN track_alert VARCHAR(16) DEFAULT NULL COMMENT '轨迹预警(字典 ecom_track_alert)';
ALTER TABLE ecom_package ADD COLUMN track_source VARCHAR(16) DEFAULT NULL COMMENT '轨迹来源 CARRIER物流商接口 TRACK17 17TRACK';
ALTER TABLE ecom_package ADD COLUMN track_first_time DATETIME DEFAULT NULL COMMENT '首条轨迹（上网）时间';
ALTER TABLE ecom_package ADD COLUMN track_last_time DATETIME DEFAULT NULL COMMENT '最新轨迹时间';
ALTER TABLE ecom_package ADD COLUMN track_last_event VARCHAR(500) DEFAULT NULL COMMENT '最新轨迹描述';
ALTER TABLE ecom_package ADD COLUMN track_delivered_time DATETIME DEFAULT NULL COMMENT '签收时间';
ALTER TABLE ecom_package ADD COLUMN track_pull_time DATETIME DEFAULT NULL COMMENT '最近一次拉取轨迹的时间';
ALTER TABLE ecom_package ADD COLUMN track_error VARCHAR(500) DEFAULT NULL COMMENT '最近一次拉取轨迹失败的原因';
ALTER TABLE ecom_package ADD COLUMN ship_confirm TINYINT DEFAULT NULL COMMENT '按包裹回传平台（部分发货） 1成功 0失败 为空未回传';
ALTER TABLE ecom_package ADD COLUMN ship_confirm_msg VARCHAR(500) DEFAULT NULL COMMENT '按包裹回传平台的结果说明';
CREATE INDEX idx_ecom_package_track ON ecom_package (status, track_status);

-- 审单规则：新增条件（预估利润率低于、命中黑名单、地址异常、偏远地区）与动作（自动打标签）
ALTER TABLE ecom_order_rule ADD COLUMN profit_rate_max DECIMAL(10, 2) DEFAULT NULL COMMENT '条件：预估利润率低于（%）';
ALTER TABLE ecom_order_rule ADD COLUMN blacklist_hit TINYINT NOT NULL DEFAULT 0 COMMENT '条件：命中买家黑名单 1是 0不限';
ALTER TABLE ecom_order_rule ADD COLUMN address_issue TINYINT NOT NULL DEFAULT 0 COMMENT '条件：地址校验不通过 1是 0不限';
ALTER TABLE ecom_order_rule ADD COLUMN remote_area TINYINT NOT NULL DEFAULT 0 COMMENT '条件：偏远地区 1是 0不限';
ALTER TABLE ecom_order_rule ADD COLUMN tag_ids VARCHAR(255) DEFAULT NULL COMMENT '动作：自动打标签（标签 ID，逗号分隔）';

-- 退货：按包裹 / 发货单选择退回的行
ALTER TABLE ecom_return_item ADD COLUMN package_id BIGINT DEFAULT NULL COMMENT '退回的包裹（多包裹订单），为空按发货顺序分配';
ALTER TABLE ecom_return ADD COLUMN trade_doc_nos VARCHAR(500) DEFAULT NULL COMMENT '生成的全部销售退货单号（多包裹订单按发货单分别生成，逗号分隔）';

DROP TABLE IF EXISTS ecom_order_tag;
CREATE TABLE ecom_order_tag (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(32)  NOT NULL COMMENT '标签名称',
    color       VARCHAR(16)  NOT NULL DEFAULT 'blue' COMMENT '颜色（Ant Design 预设色或 #RRGGBB）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_order_tag_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单标签';

DROP TABLE IF EXISTS ecom_order_tag_link;
CREATE TABLE ecom_order_tag_link (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id    BIGINT   NOT NULL COMMENT '平台订单',
    tag_id      BIGINT   NOT NULL COMMENT '标签',
    auto_tagged TINYINT  NOT NULL DEFAULT 0 COMMENT '由审单规则自动打上 1是 0人工',
    create_time DATETIME          DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_order_tag_link (order_id, tag_id, tenant_id),
    KEY idx_ecom_order_tag_link_tag (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单打标';

DROP TABLE IF EXISTS ecom_order_note;
CREATE TABLE ecom_order_note (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id    BIGINT       NOT NULL COMMENT '平台订单',
    content     VARCHAR(500) NOT NULL COMMENT '备注内容',
    create_by   BIGINT                DEFAULT NULL COMMENT '备注人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_order_note_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单内部备注历史';

DROP TABLE IF EXISTS ecom_buyer_blacklist;
CREATE TABLE ecom_buyer_blacklist (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    match_type    VARCHAR(16)  NOT NULL COMMENT '匹配方式(字典 ecom_blacklist_type) BUYER买家 ID EMAIL邮箱 ADDRESS地址 PHONE电话',
    match_value   VARCHAR(255) NOT NULL COMMENT '匹配值（原文）',
    match_key     VARCHAR(255) NOT NULL COMMENT '规范化后的匹配值（小写、去空白 / 符号）',
    store_id      BIGINT                DEFAULT NULL COMMENT '只对该店铺生效，为空全部店铺',
    reason        VARCHAR(255)          DEFAULT NULL COMMENT '拉黑原因',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    hit_count     INT          NOT NULL DEFAULT 0 COMMENT '命中次数',
    last_hit_time DATETIME              DEFAULT NULL COMMENT '最近命中时间',
    create_by     BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_buyer_blacklist (match_type, match_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '买家黑名单';

DROP TABLE IF EXISTS ecom_remote_area;
CREATE TABLE ecom_remote_area (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    country        VARCHAR(8)   NOT NULL COMMENT '国家 / 地区代码',
    postcode_from  VARCHAR(16)  NOT NULL COMMENT '邮编起（前缀匹配时与止相同）',
    postcode_to    VARCHAR(16)  NOT NULL COMMENT '邮编止（含）',
    name           VARCHAR(64)           DEFAULT NULL COMMENT '地区名称',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark         VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_remote_area (country, postcode_from, postcode_to, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '偏远地区邮编段（地址校验识别偏远地区）';

DROP TABLE IF EXISTS ecom_package_track;
CREATE TABLE ecom_package_track (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_id  BIGINT       NOT NULL COMMENT '包裹',
    event_time  DATETIME              DEFAULT NULL COMMENT '发生时间（解析失败为空）',
    time_text   VARCHAR(32)           DEFAULT NULL COMMENT '发生时间原文',
    location    VARCHAR(128)          DEFAULT NULL COMMENT '地点',
    description VARCHAR(500)          DEFAULT NULL COMMENT '描述',
    status      VARCHAR(16)           DEFAULT NULL COMMENT '归一后的状态(字典 ecom_track_status)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '顺序（0 为最新）',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_package_track_pkg (package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '包裹物流轨迹';

DROP TABLE IF EXISTS ecom_order_setting;
CREATE TABLE ecom_order_setting (
    id                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    address_check     TINYINT  NOT NULL DEFAULT 1 COMMENT '审核前校验地址 1是 0否',
    track_key         TEXT              COMMENT '17TRACK API 密钥（加密保存，enc: 前缀）',
    track_enabled     TINYINT  NOT NULL DEFAULT 1 COMMENT '定时拉取已发货包裹的轨迹 1是 0否',
    no_scan_hours     INT      NOT NULL DEFAULT 72 COMMENT '发货后超过多少小时没有轨迹为超时未上网',
    transit_days      INT      NOT NULL DEFAULT 20 COMMENT '上网后超过多少天未签收为运输超时',
    stale_days        INT      NOT NULL DEFAULT 7 COMMENT '超过多少天轨迹没有更新为停滞（运输超时）',
    create_time       DATETIME          DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_order_setting (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单作业设置（地址校验、物流跟踪）';

-- 菜单：订单与售后下的订单标签、买家黑名单、物流跟踪、偏远地区；平台订单的手工建单、补发、打标签按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(530, 982, '订单标签', 'C', 'order-tag', 'ecom/order-tag/index', 'ecom:orderTag:list', 'TagsOutlined', 6, 1, 1, NOW(), NOW()),
(531, 982, '买家黑名单', 'C', 'buyer-blacklist', 'ecom/buyer-blacklist/index', 'ecom:blacklist:list', 'StopOutlined', 7, 1, 1, NOW(), NOW()),
(532, 982, '物流跟踪', 'C', 'order-track', 'ecom/order-track/index', 'ecom:track:list', 'NodeIndexOutlined', 8, 1, 1, NOW(), NOW()),
(533, 982, '偏远地区', 'C', 'remote-area', 'ecom/remote-area/index', 'ecom:remoteArea:list', 'EnvironmentOutlined', 9, 1, 1, NOW(), NOW()),
(5300, 530, '标签新增', 'F', NULL, NULL, 'ecom:orderTag:add', NULL, 1, 1, 1, NOW(), NOW()),
(5301, 530, '标签修改', 'F', NULL, NULL, 'ecom:orderTag:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5302, 530, '标签删除', 'F', NULL, NULL, 'ecom:orderTag:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5310, 531, '黑名单新增', 'F', NULL, NULL, 'ecom:blacklist:add', NULL, 1, 1, 1, NOW(), NOW()),
(5311, 531, '黑名单修改', 'F', NULL, NULL, 'ecom:blacklist:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5312, 531, '黑名单删除', 'F', NULL, NULL, 'ecom:blacklist:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5320, 532, '拉取轨迹', 'F', NULL, NULL, 'ecom:track:sync', NULL, 1, 1, 1, NOW(), NOW()),
(5321, 532, '跟踪设置', 'F', NULL, NULL, 'ecom:track:setting', NULL, 2, 1, 1, NOW(), NOW()),
(5330, 533, '偏远地区新增', 'F', NULL, NULL, 'ecom:remoteArea:add', NULL, 1, 1, 1, NOW(), NOW()),
(5331, 533, '偏远地区修改', 'F', NULL, NULL, 'ecom:remoteArea:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5332, 533, '偏远地区删除', 'F', NULL, NULL, 'ecom:remoteArea:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5340, 423, '手工建单', 'F', NULL, NULL, 'ecom:order:add', NULL, 8, 1, 1, NOW(), NOW()),
(5341, 423, '创建补发单', 'F', NULL, NULL, 'ecom:order:resend', NULL, 9, 1, 1, NOW(), NOW()),
(5342, 423, '订单打标签 / 备注', 'F', NULL, NULL, 'ecom:order:tag', NULL, 10, 1, 1, NOW(), NOW());

-- 字典：订单类型、轨迹状态、轨迹预警、黑名单匹配方式（系统字典）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(330, '订单类型', 'ecom_order_type', 'SYSTEM', 1, '平台订单 / 手工订单 / 补发单', NOW(), NOW()),
(331, '物流轨迹状态', 'ecom_track_status', 'SYSTEM', 1, '包裹轨迹归一后的状态', NOW(), NOW()),
(332, '物流轨迹预警', 'ecom_track_alert', 'SYSTEM', 1, '超时未上网、运输超时、轨迹异常', NOW(), NOW()),
(333, '黑名单匹配方式', 'ecom_blacklist_type', 'SYSTEM', 1, '买家黑名单按买家 ID / 邮箱 / 地址 / 电话匹配', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_order_type', '平台订单', 'PLATFORM', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_order_type', '手工订单', 'MANUAL', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_order_type', '补发单', 'RESEND', 'purple', 3, 1, 1, NOW(), NOW()),
('ecom_track_status', '暂无轨迹', 'NOT_FOUND', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_track_status', '已揽收', 'PICKED_UP', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_track_status', '运输中', 'IN_TRANSIT', 'blue', 3, 1, 1, NOW(), NOW()),
('ecom_track_status', '到达待取', 'PICKUP', 'gold', 4, 1, 1, NOW(), NOW()),
('ecom_track_status', '已签收', 'DELIVERED', 'green', 5, 1, 1, NOW(), NOW()),
('ecom_track_status', '异常', 'EXCEPTION', 'red', 6, 1, 1, NOW(), NOW()),
('ecom_track_status', '退回', 'RETURNED', 'volcano', 7, 1, 1, NOW(), NOW()),
('ecom_track_alert', '超时未上网', 'NO_SCAN', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_track_alert', '运输超时', 'TRANSIT_TIMEOUT', 'magenta', 2, 1, 1, NOW(), NOW()),
('ecom_track_alert', '轨迹异常', 'EXCEPTION', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_blacklist_type', '买家 ID', 'BUYER', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_blacklist_type', '邮箱', 'EMAIL', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_blacklist_type', '地址', 'ADDRESS', 'purple', 3, 1, 1, NOW(), NOW()),
('ecom_blacklist_type', '电话', 'PHONE', 'gold', 4, 1, 1, NOW(), NOW());

-- 内置角色：超级管理员、电商运营全部；总经理只读（页面）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id BETWEEN 530 AND 539 OR m.id BETWEEN 5300 AND 5399)
WHERE r.id IN (1, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id BETWEEN 530 AND 539
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- 偏远地区示例（美国阿拉斯加、夏威夷、波多黎各，澳大利亚北领地，英国苏格兰高地与离岛），可在「偏远地区」中维护
INSERT INTO ecom_remote_area (country, postcode_from, postcode_to, name, status, create_time, update_time) VALUES
('US', '99500', '99999', 'Alaska', 1, NOW(), NOW()),
('US', '96700', '96899', 'Hawaii', 1, NOW(), NOW()),
('US', '00600', '00999', 'Puerto Rico', 1, NOW(), NOW()),
('AU', '0800', '0899', 'Northern Territory', 1, NOW(), NOW()),
('GB', 'HS', 'HS', 'Outer Hebrides', 1, NOW(), NOW()),
('GB', 'ZE', 'ZE', 'Shetland', 1, NOW(), NOW()),
('GB', 'KW15', 'KW17', 'Orkney', 1, NOW(), NOW()),
('GB', 'IV', 'IV', 'Highlands (Inverness)', 1, NOW(), NOW());

-- ===== 第四轮：刊登与定价 =====
-- 刊登中心（草稿 → 每店一份内容 → 经连接器发布）、刊登模板、价格模板（定价公式）、批量改价 / 改库存与改价历史、自动调价（规则、日志）、竞品价格快照

DROP TABLE IF EXISTS ecom_price_template;
CREATE TABLE ecom_price_template (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name            VARCHAR(64)    NOT NULL COMMENT '名称',
    platform        VARCHAR(16)             DEFAULT NULL COMMENT '适用平台(字典 ecom_platform)，为空通用',
    first_leg_cost  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '头程运费（本位币 / 件）',
    other_cost      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '包材等其他成本（本位币 / 件）',
    commission_rate DECIMAL(6, 2)  NOT NULL DEFAULT 15 COMMENT '平台佣金率（%）',
    other_rate      DECIMAL(6, 2)  NOT NULL DEFAULT 0 COMMENT '其他费率（广告、退货损耗等，%）',
    fixed_fee       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '固定费（店铺币种 / 件，如 FBA 配送费、交易手续费）',
    target_margin   DECIMAL(6, 2)  NOT NULL DEFAULT 20 COMMENT '目标利润率（%，利润 ÷ 售价）',
    price_tail      DECIMAL(6, 2)           DEFAULT NULL COMMENT '尾数（如 0.99：售价向上取到 x.99），为空保留两位小数',
    min_price       DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低售价（店铺币种），为空不限',
    status          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_price_template (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格模板：售价 = ((成本 + 头程 + 其他成本) ÷ 汇率 + 固定费) ÷ (1 − 佣金率 − 其他费率 − 目标利润率)，再取尾数';

DROP TABLE IF EXISTS ecom_publish_template;
CREATE TABLE ecom_publish_template (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)  NOT NULL COMMENT '名称',
    platform          VARCHAR(16)  NOT NULL COMMENT '平台(字典 ecom_platform)',
    category_id       VARCHAR(64)           DEFAULT NULL COMMENT '默认平台类目 ID（亚马逊为 Product Type）',
    category_name     VARCHAR(255)          DEFAULT NULL COMMENT '类目名称（路径）',
    attributes_json   TEXT                  DEFAULT NULL COMMENT '常用属性默认值（JSON 对象：属性键 → 值）',
    brand             VARCHAR(64)           DEFAULT NULL COMMENT '默认品牌',
    shipping_template VARCHAR(128)          DEFAULT NULL COMMENT '运费模板 / 物流（eBay fulfillmentPolicyId、虾皮 logistic_id 等）',
    return_policy     VARCHAR(128)          DEFAULT NULL COMMENT '退货政策（eBay returnPolicyId 等）',
    handling_days     INT                   DEFAULT NULL COMMENT '备货天数',
    fulfillment       VARCHAR(16)           DEFAULT NULL COMMENT '配送方式 PLATFORM平台仓 SELLER自发货',
    price_template_id BIGINT                DEFAULT NULL COMMENT '默认价格模板',
    extra_json        TEXT                  DEFAULT NULL COMMENT '其他平台参数（JSON 对象，如 eBay paymentPolicyId、merchantLocationKey）',
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by         BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_publish_template (name, tenant_id),
    KEY idx_ecom_publish_template_platform (platform)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刊登模板：按平台保存常用属性、运费模板、退货政策等默认值';

DROP TABLE IF EXISTS ecom_publish_draft;
CREATE TABLE ecom_publish_draft (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id    BIGINT       NOT NULL COMMENT '来源物料（多规格款式时为 SPU，变体为其下的 SKU）',
    title          VARCHAR(255) NOT NULL COMMENT '主标题',
    bullets_json   TEXT                  DEFAULT NULL COMMENT '五点描述 / 卖点（JSON 数组）',
    description    TEXT                  DEFAULT NULL COMMENT '商品描述',
    images_json    TEXT                  DEFAULT NULL COMMENT '图片 URL（JSON 数组，第一张为主图）',
    brand          VARCHAR(64)           DEFAULT NULL COMMENT '品牌',
    barcode_type   VARCHAR(16)           DEFAULT NULL COMMENT '条码类型 UPC EAN GTIN JAN ISBN',
    upc_exempt     TINYINT      NOT NULL DEFAULT 0 COMMENT 'UPC 豁免 1是',
    exemption_note VARCHAR(255)          DEFAULT NULL COMMENT '豁免说明（品牌备案 / GTIN 豁免编号等）',
    remark         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    dept_id        BIGINT                DEFAULT NULL COMMENT '制单人部门',
    create_by      BIGINT                DEFAULT NULL COMMENT '制单人',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_publish_draft_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刊登草稿（主内容，按店铺拆成刊登记录）';

DROP TABLE IF EXISTS ecom_publish_item;
CREATE TABLE ecom_publish_item (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    draft_id            BIGINT       NOT NULL COMMENT '刊登草稿',
    store_id            BIGINT       NOT NULL COMMENT '店铺',
    status              VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_publish_status)',
    title               VARCHAR(255) NOT NULL COMMENT '标题',
    bullets_json        TEXT                  DEFAULT NULL COMMENT '五点描述（JSON 数组）',
    description         TEXT                  DEFAULT NULL COMMENT '描述',
    images_json         TEXT                  DEFAULT NULL COMMENT '图片 URL（JSON 数组）',
    brand               VARCHAR(64)           DEFAULT NULL COMMENT '品牌',
    barcode_type        VARCHAR(16)           DEFAULT NULL COMMENT '条码类型',
    upc_exempt          TINYINT      NOT NULL DEFAULT 0 COMMENT 'UPC 豁免',
    exemption_note      VARCHAR(255)          DEFAULT NULL COMMENT '豁免说明',
    category_id         VARCHAR(64)           DEFAULT NULL COMMENT '平台类目 ID（亚马逊 Product Type）',
    category_name       VARCHAR(255)          DEFAULT NULL COMMENT '类目名称',
    attributes_json     TEXT                  DEFAULT NULL COMMENT '平台属性（JSON 对象）',
    publish_template_id BIGINT                DEFAULT NULL COMMENT '刊登模板',
    price_template_id   BIGINT                DEFAULT NULL COMMENT '价格模板',
    currency            VARCHAR(8)            DEFAULT NULL COMMENT '币种（店铺币种）',
    fulfillment         VARCHAR(16)           DEFAULT NULL COMMENT '配送方式',
    shipping_template   VARCHAR(128)          DEFAULT NULL COMMENT '运费模板',
    return_policy       VARCHAR(128)          DEFAULT NULL COMMENT '退货政策',
    handling_days       INT                   DEFAULT NULL COMMENT '备货天数',
    extra_json          TEXT                  DEFAULT NULL COMMENT '其他平台参数（JSON 对象）',
    parent_sku          VARCHAR(64)           DEFAULT NULL COMMENT '父 SKU（多变体时）',
    platform_item_id    VARCHAR(64)           DEFAULT NULL COMMENT '平台商品 ID（发布成功后回写）',
    platform_url        VARCHAR(255)          DEFAULT NULL COMMENT '平台商品链接',
    publish_ref         VARCHAR(128)          DEFAULT NULL COMMENT '平台异步处理编号（沃尔玛 feedId 等，发布中时轮询结果）',
    error_message       VARCHAR(2000)         DEFAULT NULL COMMENT '失败原因（平台返回的错误）',
    retry_count         INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    submit_time         DATETIME              DEFAULT NULL COMMENT '提交发布时间',
    publish_time        DATETIME              DEFAULT NULL COMMENT '发布成功时间',
    offline_time        DATETIME              DEFAULT NULL COMMENT '下架时间',
    create_by           BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time         DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_publish_item (draft_id, store_id, tenant_id),
    KEY idx_ecom_publish_item_status (status),
    KEY idx_ecom_publish_item_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刊登记录：一个草稿在一个店铺的内容与发布状态';

DROP TABLE IF EXISTS ecom_publish_variant;
CREATE TABLE ecom_publish_variant (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    item_id          BIGINT         NOT NULL COMMENT '刊登记录',
    material_id      BIGINT         NOT NULL COMMENT '物料（SKU）',
    seller_sku       VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    variant_text     VARCHAR(128)            DEFAULT NULL COMMENT '变体属性（显示用，如 Red / XL）',
    variant_attrs_json VARCHAR(1000)           DEFAULT NULL COMMENT '变体属性（JSON 对象：属性名 → 值）',
    barcode          VARCHAR(32)             DEFAULT NULL COMMENT '条码（UPC / EAN）',
    price            DECIMAL(18, 4)          DEFAULT NULL COMMENT '售价（店铺币种）',
    qty              INT            NOT NULL DEFAULT 0 COMMENT '库存',
    cost             DECIMAL(18, 4)          DEFAULT NULL COMMENT '单件成本（本位币，物料参考成本）',
    suggested_price  DECIMAL(18, 4)          DEFAULT NULL COMMENT '按价格模板计算的建议价',
    est_profit       DECIMAL(18, 4)          DEFAULT NULL COMMENT '按售价预估的单件利润（店铺币种）',
    est_margin       DECIMAL(10, 2)          DEFAULT NULL COMMENT '预估利润率（%）',
    platform_sku_id  VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU / 变体 ID（发布成功后回写）',
    listing_id       BIGINT                  DEFAULT NULL COMMENT '生成 / 关联的在线商品',
    sort             INT            NOT NULL DEFAULT 0 COMMENT '排序',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_publish_variant (item_id, seller_sku, tenant_id),
    KEY idx_ecom_publish_variant_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刊登记录的变体（每个平台 SKU 一行）';

DROP TABLE IF EXISTS ecom_publish_log;
CREATE TABLE ecom_publish_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    item_id     BIGINT        NOT NULL COMMENT '刊登记录',
    store_id    BIGINT        NOT NULL COMMENT '店铺',
    action      VARCHAR(16)   NOT NULL COMMENT '动作(字典 ecom_publish_action)',
    success     TINYINT       NOT NULL DEFAULT 1 COMMENT '是否成功',
    message     VARCHAR(2000)          DEFAULT NULL COMMENT '说明 / 平台返回的错误',
    create_by   BIGINT                 DEFAULT NULL COMMENT '操作人（定时任务为空）',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_publish_log_item (item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '刊登日志';

DROP TABLE IF EXISTS ecom_price_change;
CREATE TABLE ecom_price_change (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    batch_no    VARCHAR(32)             DEFAULT NULL COMMENT '批次号（一次批量修改相同）',
    listing_id  BIGINT         NOT NULL COMMENT '在线商品',
    store_id    BIGINT         NOT NULL COMMENT '店铺',
    seller_sku  VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    change_type VARCHAR(8)     NOT NULL COMMENT '类型(字典 ecom_price_change_type) PRICE价格 STOCK库存',
    old_value   DECIMAL(18, 4)          DEFAULT NULL COMMENT '原值',
    new_value   DECIMAL(18, 4) NOT NULL COMMENT '新值',
    currency    VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    source      VARCHAR(16)    NOT NULL COMMENT '来源(字典 ecom_price_change_source)',
    status      VARCHAR(16)    NOT NULL COMMENT '结果(字典 ecom_price_change_status)',
    message     VARCHAR(1000)           DEFAULT NULL COMMENT '说明 / 平台返回的错误',
    rule_id     BIGINT                  DEFAULT NULL COMMENT '自动调价规则',
    create_by   BIGINT                  DEFAULT NULL COMMENT '操作人（定时任务为空）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_price_change_listing (listing_id, create_time),
    KEY idx_ecom_price_change_store (store_id, create_time),
    KEY idx_ecom_price_change_batch (batch_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '改价 / 改库存历史';

DROP TABLE IF EXISTS ecom_reprice_rule;
CREATE TABLE ecom_reprice_rule (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '名称',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    store_ids         VARCHAR(500)            DEFAULT NULL COMMENT '适用店铺（逗号分隔，为空全部）',
    skus              VARCHAR(1000)           DEFAULT NULL COMMENT '适用平台 SKU（逗号分隔，为空为店铺的全部在售商品）',
    strategy          VARCHAR(16)    NOT NULL COMMENT '策略(字典 ecom_reprice_strategy)',
    adjust_mode       VARCHAR(16)             DEFAULT NULL COMMENT '调整方式 AMOUNT按金额 PERCENT按百分比（低于最低价、按库存时使用）',
    adjust_value      DECIMAL(18, 4)          DEFAULT NULL COMMENT '调整值（金额或 %）',
    target_margin     DECIMAL(6, 2)           DEFAULT NULL COMMENT '目标利润率（%，按目标利润率时使用）',
    floor_mode        VARCHAR(16)    NOT NULL DEFAULT 'FIXED' COMMENT '底价方式(字典 ecom_reprice_floor_mode)',
    min_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低价（固定底价）',
    max_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '最高价，为空不限',
    min_margin        DECIMAL(6, 2)           DEFAULT NULL COMMENT '最低利润率（%，按成本推算底价）',
    price_template_id BIGINT                  DEFAULT NULL COMMENT '计算成本与费用的价格模板',
    stock_high_days   INT                     DEFAULT NULL COMMENT '可售天数高于它时降价（按库存）',
    stock_low_days    INT                     DEFAULT NULL COMMENT '可售天数低于它时涨价（按库存）',
    interval_minutes  INT            NOT NULL DEFAULT 60 COMMENT '执行间隔（分钟）',
    suggest_only      TINYINT        NOT NULL DEFAULT 1 COMMENT '仅建议不推送 1是',
    last_run_time     DATETIME                DEFAULT NULL COMMENT '最近执行时间',
    last_run_message  VARCHAR(500)            DEFAULT NULL COMMENT '最近执行结果',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by         BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_reprice_rule (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动调价规则';

DROP TABLE IF EXISTS ecom_reprice_log;
CREATE TABLE ecom_reprice_log (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_id      BIGINT         NOT NULL COMMENT '规则',
    listing_id   BIGINT         NOT NULL COMMENT '在线商品',
    store_id     BIGINT         NOT NULL COMMENT '店铺',
    seller_sku   VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    old_price    DECIMAL(18, 4)          DEFAULT NULL COMMENT '原价',
    new_price    DECIMAL(18, 4)          DEFAULT NULL COMMENT '新价',
    lowest_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '竞品最低价（含运费）',
    buybox_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '购物车价',
    floor_price  DECIMAL(18, 4)          DEFAULT NULL COMMENT '底价',
    currency     VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    reason       VARCHAR(500)            DEFAULT NULL COMMENT '调价原因',
    status       VARCHAR(16)    NOT NULL COMMENT '结果(字典 ecom_reprice_log_status)',
    message      VARCHAR(1000)           DEFAULT NULL COMMENT '推送结果 / 平台错误',
    run_time     DATETIME       NOT NULL COMMENT '执行时间',
    create_by    BIGINT                  DEFAULT NULL COMMENT '操作人（定时任务为空）',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_reprice_log_rule (rule_id, run_time),
    KEY idx_ecom_reprice_log_listing (listing_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动调价日志';

DROP TABLE IF EXISTS ecom_competitor_price;
CREATE TABLE ecom_competitor_price (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    listing_id       BIGINT         NOT NULL COMMENT '在线商品',
    store_id         BIGINT         NOT NULL COMMENT '店铺',
    seller_sku       VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    platform_item_id VARCHAR(64)             DEFAULT NULL COMMENT '平台商品 ID（ASIN 等）',
    source           VARCHAR(16)    NOT NULL COMMENT '来源(字典 ecom_competitor_source)',
    lowest_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '竞品最低价（商品价，不含本店）',
    lowest_shipping  DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低价对应的运费',
    buybox_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '购物车价（含运费）',
    buybox_winner    TINYINT        NOT NULL DEFAULT 0 COMMENT '本店持有购物车 1是',
    offer_count      INT                     DEFAULT NULL COMMENT '卖家数',
    competitor       VARCHAR(128)            DEFAULT NULL COMMENT '最低价卖家 / 竞品说明',
    currency         VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    snapshot_time    DATETIME       NOT NULL COMMENT '快照时间',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by        BIGINT                  DEFAULT NULL COMMENT '录入人（接口拉取为空）',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_competitor_listing (listing_id, snapshot_time),
    KEY idx_ecom_competitor_store (store_id, snapshot_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '竞品价格快照（亚马逊经 Product Pricing API 拉取，其他平台手工录入 / 导入）';

-- 菜单：店铺与商品下的刊登中心、刊登模板、价格模板、改价记录、自动调价、竞品价格
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(500, 981, '刊登中心', 'C', 'publish', 'ecom/publish/index', 'ecom:publish:list', 'CloudUploadOutlined', 5, 1, 1, NOW(), NOW()),
(501, 981, '刊登模板', 'C', 'publish-template', 'ecom/publish-template/index', 'ecom:publishTemplate:list', 'ProfileOutlined', 6, 1, 1, NOW(), NOW()),
(502, 981, '价格模板', 'C', 'price-template', 'ecom/price-template/index', 'ecom:priceTemplate:list', 'CalculatorOutlined', 7, 1, 1, NOW(), NOW()),
(503, 981, '改价记录', 'C', 'price-change', 'ecom/price-change/index', 'ecom:priceChange:list', 'HistoryOutlined', 8, 1, 1, NOW(), NOW()),
(504, 981, '自动调价', 'C', 'repricer', 'ecom/repricer/index', 'ecom:repricer:list', 'RiseOutlined', 9, 1, 1, NOW(), NOW()),
(505, 981, '竞品价格', 'C', 'competitor-price', 'ecom/competitor-price/index', 'ecom:competitor:list', 'FundViewOutlined', 10, 1, 1, NOW(), NOW()),
(5000, 500, '刊登新增', 'F', NULL, NULL, 'ecom:publish:add', NULL, 1, 1, 1, NOW(), NOW()),
(5001, 500, '刊登修改', 'F', NULL, NULL, 'ecom:publish:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5002, 500, '刊登删除', 'F', NULL, NULL, 'ecom:publish:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5003, 500, '发布 / 重试 / 下架', 'F', NULL, NULL, 'ecom:publish:push', NULL, 4, 1, 1, NOW(), NOW()),
(5010, 501, '模板新增', 'F', NULL, NULL, 'ecom:publishTemplate:add', NULL, 1, 1, 1, NOW(), NOW()),
(5011, 501, '模板修改', 'F', NULL, NULL, 'ecom:publishTemplate:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5012, 501, '模板删除', 'F', NULL, NULL, 'ecom:publishTemplate:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5020, 502, '模板新增', 'F', NULL, NULL, 'ecom:priceTemplate:add', NULL, 1, 1, 1, NOW(), NOW()),
(5021, 502, '模板修改', 'F', NULL, NULL, 'ecom:priceTemplate:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5022, 502, '模板删除', 'F', NULL, NULL, 'ecom:priceTemplate:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5030, 503, '批量改价 / 改库存', 'F', NULL, NULL, 'ecom:priceChange:add', NULL, 1, 1, 1, NOW(), NOW()),
(5040, 504, '规则新增', 'F', NULL, NULL, 'ecom:repricer:add', NULL, 1, 1, 1, NOW(), NOW()),
(5041, 504, '规则修改', 'F', NULL, NULL, 'ecom:repricer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5042, 504, '规则删除', 'F', NULL, NULL, 'ecom:repricer:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5043, 504, '执行 / 应用建议', 'F', NULL, NULL, 'ecom:repricer:execute', NULL, 4, 1, 1, NOW(), NOW()),
(5050, 505, '录入 / 导入 / 拉取', 'F', NULL, NULL, 'ecom:competitor:add', NULL, 1, 1, 1, NOW(), NOW()),
(5051, 505, '删除', 'F', NULL, NULL, 'ecom:competitor:remove', NULL, 2, 1, 1, NOW(), NOW());

-- 系统字典：刊登状态与动作、改价类型 / 来源 / 结果、调整方式、调价策略 / 底价方式 / 日志结果、竞品价格来源
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(300, '刊登状态', 'ecom_publish_status', 'SYSTEM', 1, '刊登记录的发布状态', NOW(), NOW()),
(301, '刊登动作', 'ecom_publish_action', 'SYSTEM', 1, '刊登日志的动作', NOW(), NOW()),
(302, '改价类型', 'ecom_price_change_type', 'SYSTEM', 1, '改价历史中的修改对象', NOW(), NOW()),
(303, '改价来源', 'ecom_price_change_source', 'SYSTEM', 1, '改价历史的来源', NOW(), NOW()),
(304, '改价结果', 'ecom_price_change_status', 'SYSTEM', 1, '改价是否推送到平台', NOW(), NOW()),
(305, '调整方式', 'ecom_price_adjust_mode', 'SYSTEM', 1, '批量改价 / 改库存、调价规则的调整方式', NOW(), NOW()),
(306, '调价策略', 'ecom_reprice_strategy', 'SYSTEM', 1, '自动调价规则的策略', NOW(), NOW()),
(307, '底价方式', 'ecom_reprice_floor_mode', 'SYSTEM', 1, '自动调价的最低价计算方式', NOW(), NOW()),
(308, '调价结果', 'ecom_reprice_log_status', 'SYSTEM', 1, '自动调价日志的结果', NOW(), NOW()),
(309, '竞品价格来源', 'ecom_competitor_source', 'SYSTEM', 1, '竞品价格快照的来源', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_publish_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_publish_status', '待发布', 'PENDING', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_publish_status', '发布中', 'PUBLISHING', 'processing', 3, 1, 1, NOW(), NOW()),
('ecom_publish_status', '成功', 'SUCCESS', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_publish_status', '失败', 'FAILED', 'red', 5, 1, 1, NOW(), NOW()),
('ecom_publish_status', '已下架', 'OFFLINE', 'default', 6, 1, 1, NOW(), NOW()),
('ecom_publish_action', '提交发布', 'SUBMIT', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_publish_action', '发布', 'PUBLISH', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_publish_action', '查询结果', 'CHECK', 'geekblue', 3, 1, 1, NOW(), NOW()),
('ecom_publish_action', '下架', 'OFFLINE', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_price_change_type', '价格', 'PRICE', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_price_change_type', '库存', 'STOCK', 'purple', 2, 1, 1, NOW(), NOW()),
('ecom_price_change_source', '手工修改', 'MANUAL', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_price_change_source', '批量修改', 'BATCH', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_price_change_source', '自动调价', 'REPRICER', 'purple', 3, 1, 1, NOW(), NOW()),
('ecom_price_change_source', '刊登', 'PUBLISH', 'cyan', 4, 1, 1, NOW(), NOW()),
('ecom_price_change_status', '已推送', 'SUCCESS', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_price_change_status', '推送失败', 'FAILED', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_price_change_status', '仅本地', 'LOCAL', 'default', 3, 1, 1, NOW(), NOW()),
('ecom_price_adjust_mode', '固定值', 'FIXED', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_price_adjust_mode', '按百分比', 'PERCENT', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_price_adjust_mode', '按金额', 'AMOUNT', 'cyan', 3, 1, 1, NOW(), NOW()),
('ecom_price_adjust_mode', '按公式', 'FORMULA', 'purple', 4, 1, 1, NOW(), NOW()),
('ecom_reprice_strategy', '跟随最低价', 'MATCH_LOWEST', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_reprice_strategy', '低于最低价', 'BEAT_LOWEST', 'geekblue', 2, 1, 1, NOW(), NOW()),
('ecom_reprice_strategy', '跟随购物车价', 'MATCH_BUYBOX', 'gold', 3, 1, 1, NOW(), NOW()),
('ecom_reprice_strategy', '按目标利润率', 'TARGET_MARGIN', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_reprice_strategy', '按库存 / 销量', 'BY_STOCK', 'purple', 5, 1, 1, NOW(), NOW()),
('ecom_reprice_floor_mode', '固定底价', 'FIXED', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_reprice_floor_mode', '成本 + 最低利润率', 'COST_MARGIN', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_reprice_log_status', '建议', 'SUGGESTED', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_reprice_log_status', '已推送', 'PUSHED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_reprice_log_status', '推送失败', 'FAILED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_reprice_log_status', '仅本地', 'LOCAL', 'cyan', 4, 1, 1, NOW(), NOW()),
('ecom_reprice_log_status', '跳过', 'SKIPPED', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_reprice_log_status', '已忽略', 'IGNORED', 'default', 6, 1, 1, NOW(), NOW()),
('ecom_competitor_source', '平台接口', 'API', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_competitor_source', '手工录入', 'MANUAL', 'default', 2, 1, 1, NOW(), NOW()),
('ecom_competitor_source', '导入', 'IMPORT', 'cyan', 3, 1, 1, NOW(), NOW());

-- 示例价格模板
INSERT INTO ecom_price_template (name, platform, first_leg_cost, other_cost, commission_rate, other_rate, fixed_fee, target_margin, price_tail, status, remark, create_time, update_time) VALUES
('亚马逊 FBA 标准', 'AMAZON', 12, 1.5, 15, 5, 3.5, 20, 0.99, 1, '海运头程，FBA 配送费按标准尺寸估算', NOW(), NOW()),
('自发货小包', NULL, 0, 1, 12, 3, 0, 25, 0.99, 1, '自发货渠道的通用定价', NOW(), NOW());

-- 内置角色：电商运营全部；总经理只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id BETWEEN 500 AND 509 OR m.parent_id BETWEEN 500 AND 509)
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id BETWEEN 500 AND 509)
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第四轮：FBA 运营 =====
-- 亚马逊报告任务（Reports API：createReport → getReport 轮询 → getReportDocument 下载解析），也可上传报告文件
CREATE TABLE ecom_amz_report (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id         BIGINT        NOT NULL COMMENT '店铺（亚马逊）',
    report_type      VARCHAR(32)   NOT NULL COMMENT '报告类型(字典 ecom_amz_report_type)',
    data_start       DATE                   DEFAULT NULL COMMENT '数据开始日期',
    data_end         DATE                   DEFAULT NULL COMMENT '数据结束日期',
    report_id        VARCHAR(64)            DEFAULT NULL COMMENT '亚马逊报告 ID（上传的为空）',
    document_id      VARCHAR(128)           DEFAULT NULL COMMENT '亚马逊报告文档 ID',
    status           VARCHAR(16)   NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态(字典 ecom_amz_report_status) SUBMITTED已提交 PROCESSING生成中 DONE已完成 FAILED失败 CANCELLED已取消',
    uploaded         TINYINT       NOT NULL DEFAULT 0 COMMENT '手工上传的报告文件 1是',
    row_count        INT           NOT NULL DEFAULT 0 COMMENT '报告行数',
    applied_count    INT           NOT NULL DEFAULT 0 COMMENT '写入 / 更新的条数',
    poll_count       INT           NOT NULL DEFAULT 0 COMMENT '已查询状态的次数',
    next_poll_time   DATETIME               DEFAULT NULL COMMENT '下次查询状态的时间',
    message          VARCHAR(500)           DEFAULT NULL COMMENT '失败原因或处理说明',
    finish_time      DATETIME               DEFAULT NULL COMMENT '完成时间',
    create_by        BIGINT                 DEFAULT NULL COMMENT '创建人（定时任务为空）',
    create_time      DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_amz_report_store (store_id, report_type),
    KEY idx_ecom_amz_report_status (status, next_poll_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '亚马逊报告任务';

-- FBA 库存健康快照：店铺 × SKU 只保留最近一次（库存规划报告 / 库存报告，或按头程签收先进先出推算库龄）
CREATE TABLE ecom_fba_inventory (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    seller_sku        VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    fnsku             VARCHAR(32)             DEFAULT NULL COMMENT 'FNSKU',
    asin              VARCHAR(32)             DEFAULT NULL COMMENT 'ASIN',
    title             VARCHAR(255)            DEFAULT NULL COMMENT '商品名称',
    snapshot_date     DATE           NOT NULL COMMENT '快照日期',
    available_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '可售',
    inbound_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '在途（处理中 + 已发货 + 接收中）',
    reserved_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '预留',
    unfulfillable_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '不可售',
    researching_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '调查中',
    age_0_90          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '库龄 0–90 天',
    age_91_180        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '库龄 91–180 天',
    age_181_270       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '库龄 181–270 天',
    age_271_365       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '库龄 271–365 天',
    age_365_plus      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '库龄 365 天以上',
    age_source        VARCHAR(16)    NOT NULL DEFAULT 'REPORT' COMMENT '库龄来源 REPORT亚马逊报告 FIFO按头程签收推算',
    units_shipped_30  DECIMAL(18, 4)          DEFAULT NULL COMMENT '报告中最近 30 天发货件数',
    report_storage_fee DECIMAL(18, 2)         DEFAULT NULL COMMENT '报告中预估下月仓储费（店铺币种）',
    currency          VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_inventory (store_id, seller_sku, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 库存健康快照';

-- 仓储费费率：月度仓储费按尺寸类别 + 月份（淡旺季），超龄库存附加费（长期仓储费）按尺寸类别 + 库龄段，按单位体积计费
CREATE TABLE ecom_fba_storage_rate (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    marketplace   VARCHAR(16)    NOT NULL COMMENT '站点(业务字典 ecom_marketplace)',
    fee_type      VARCHAR(16)    NOT NULL COMMENT '费用类型(字典 ecom_fba_storage_fee_type) MONTHLY月度仓储费 AGED超龄附加费',
    size_category VARCHAR(16)    NOT NULL COMMENT '尺寸类别(字典 ecom_fba_size_category) STANDARD标准件 OVERSIZE大件',
    month_from    INT                     DEFAULT NULL COMMENT '月份起（月度仓储费，1–12）',
    month_to      INT                     DEFAULT NULL COMMENT '月份止（含）',
    age_from      INT                     DEFAULT NULL COMMENT '库龄起（天，含，超龄附加费）',
    age_to        INT                     DEFAULT NULL COMMENT '库龄止（天，含），为空不限',
    rate          DECIMAL(18, 4) NOT NULL COMMENT '费率（每立方英尺 / 每立方米，店铺币种）',
    volume_unit   VARCHAR(8)     NOT NULL DEFAULT 'CUFT' COMMENT '体积单位 CUFT立方英尺 CBM立方米',
    currency      VARCHAR(8)     NOT NULL COMMENT '币种',
    builtin       TINYINT        NOT NULL DEFAULT 0 COMMENT '内置 1是',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_fba_storage_rate (marketplace, fee_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 仓储费费率';

-- FBA 配送费尺寸分段：按站点从小到大依次匹配，第一个尺寸和重量都不超过上限的分段
CREATE TABLE ecom_fba_size_tier (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    marketplace   VARCHAR(16)    NOT NULL COMMENT '站点(业务字典 ecom_marketplace)',
    code          VARCHAR(32)    NOT NULL COMMENT '分段编码',
    name          VARCHAR(64)    NOT NULL COMMENT '分段名称',
    size_category VARCHAR(16)    NOT NULL DEFAULT 'STANDARD' COMMENT '尺寸类别(字典 ecom_fba_size_category)，仓储费按它取费率',
    unit_system   VARCHAR(16)    NOT NULL DEFAULT 'IMPERIAL' COMMENT '单位制 IMPERIAL英寸 / 磅 METRIC厘米 / 千克',
    max_long      DECIMAL(10, 2)          DEFAULT NULL COMMENT '最长边上限，为空不限',
    max_median    DECIMAL(10, 2)          DEFAULT NULL COMMENT '次长边上限',
    max_short     DECIMAL(10, 2)          DEFAULT NULL COMMENT '最短边上限',
    max_girth     DECIMAL(10, 2)          DEFAULT NULL COMMENT '长度 + 围长上限（最长边 + 2 × (次长边 + 最短边)）',
    max_weight    DECIMAL(10, 3)          DEFAULT NULL COMMENT '发货重量上限',
    dim_divisor   DECIMAL(10, 2)          DEFAULT NULL COMMENT '体积重系数（体积 ÷ 系数 = 体积重，发货重量取实重与体积重的较大者），为空只按实重',
    sort          INT            NOT NULL DEFAULT 0 COMMENT '匹配顺序',
    builtin       TINYINT        NOT NULL DEFAULT 0 COMMENT '内置 1是',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_size_tier (marketplace, code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 配送费尺寸分段';

-- FBA 配送费费率：分段 × 发货重量段（下限不含、上限含），费用 = 固定费 + 超出 extra_from 的部分每 extra_unit 加 extra_fee
CREATE TABLE ecom_fba_fee_rate (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    marketplace  VARCHAR(16)    NOT NULL COMMENT '站点',
    tier_code    VARCHAR(32)    NOT NULL COMMENT '尺寸分段编码',
    weight_from  DECIMAL(10, 3) NOT NULL DEFAULT 0 COMMENT '发货重量下限（不含）',
    weight_to    DECIMAL(10, 3)          DEFAULT NULL COMMENT '发货重量上限（含），为空不限',
    fee          DECIMAL(18, 2) NOT NULL COMMENT '配送费（每件，店铺币种）',
    extra_from   DECIMAL(10, 3)          DEFAULT NULL COMMENT '续重起点（超过它的部分按续重计费），为空不计续重',
    extra_unit   DECIMAL(10, 3)          DEFAULT NULL COMMENT '续重单位重量',
    extra_fee    DECIMAL(18, 2)          DEFAULT NULL COMMENT '每个续重单位的费用',
    currency     VARCHAR(8)     NOT NULL COMMENT '币种',
    builtin      TINYINT        NOT NULL DEFAULT 0 COMMENT '内置 1是',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_fba_fee_rate (marketplace, tier_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 配送费费率';

-- FBA 商品尺寸重量（店铺 × SKU，手工维护或由预估费用报告带入），用于计算配送费、仓储体积
CREATE TABLE ecom_fba_item_dim (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id      BIGINT         NOT NULL COMMENT '店铺',
    seller_sku    VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    length_cm     DECIMAL(10, 2) NOT NULL COMMENT '最长边（cm）',
    width_cm      DECIMAL(10, 2) NOT NULL COMMENT '次长边（cm）',
    height_cm     DECIMAL(10, 2) NOT NULL COMMENT '最短边（cm）',
    weight_kg     DECIMAL(10, 3) NOT NULL COMMENT '单件重量（含包装，kg）',
    source        VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工 REPORT预估费用报告',
    platform_tier VARCHAR(64)             DEFAULT NULL COMMENT '亚马逊给出的尺寸分段',
    platform_fee  DECIMAL(18, 2)          DEFAULT NULL COMMENT '亚马逊预估配送费（每件）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_item_dim (store_id, seller_sku, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 商品尺寸重量';

-- 移除订单：退回本地仓或弃置
CREATE TABLE ecom_fba_removal (
    id                     BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id              BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no                 VARCHAR(32)    NOT NULL COMMENT '单据号',
    store_id               BIGINT         NOT NULL COMMENT '店铺',
    removal_type           VARCHAR(16)    NOT NULL COMMENT '类型(字典 ecom_fba_removal_type) RETURN退回 DISPOSAL弃置',
    platform_order_id      VARCHAR(64)             DEFAULT NULL COMMENT '亚马逊移除订单号',
    status                 VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_fba_removal_status)',
    warehouse_id           BIGINT                  DEFAULT NULL COMMENT '退回的本地仓（良品）',
    defective_warehouse_id BIGINT                  DEFAULT NULL COMMENT '不良品仓',
    ship_address           VARCHAR(255)            DEFAULT NULL COMMENT '退回地址',
    request_date           DATE                    DEFAULT NULL COMMENT '申请日期',
    complete_date          DATE                    DEFAULT NULL COMMENT '亚马逊完成日期',
    carrier                VARCHAR(64)             DEFAULT NULL COMMENT '承运商',
    tracking_no            VARCHAR(255)            DEFAULT NULL COMMENT '运单号（多个逗号分隔）',
    total_qty              DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '申请数量合计',
    removal_fee            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '移除 / 弃置费（店铺币种）',
    currency               VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    fee_base               DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '移除费（本位币）',
    stock_doc_nos          VARCHAR(500)            DEFAULT NULL COMMENT '生成的出入库 / 调拨单号',
    last_sync_time         DATETIME                DEFAULT NULL COMMENT '最近一次按报告同步的时间',
    remark                 VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id                BIGINT                  DEFAULT NULL COMMENT '制单人部门',
    create_by              BIGINT                  DEFAULT NULL COMMENT '制单人（报告同步的为空）',
    create_time            DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time            DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_removal_no (doc_no, tenant_id),
    KEY idx_ecom_fba_removal_order (store_id, platform_order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 移除订单';

CREATE TABLE ecom_fba_removal_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    removal_id      BIGINT         NOT NULL COMMENT '移除订单',
    seller_sku      VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    fnsku           VARCHAR(32)             DEFAULT NULL COMMENT 'FNSKU',
    disposition     VARCHAR(16)    NOT NULL DEFAULT 'SELLABLE' COMMENT '库存状态(字典 ecom_fba_disposition) SELLABLE可售 UNSELLABLE不可售',
    material_id     BIGINT                  DEFAULT NULL COMMENT '映射的物料',
    pack_qty        DECIMAL(18, 4) NOT NULL DEFAULT 1 COMMENT '多件装数量',
    requested_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '申请数量（件）',
    cancelled_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '取消数量',
    shipped_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '亚马逊已发数量',
    disposed_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已弃置数量',
    received_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '本地已收良品',
    defective_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '本地已收不良品',
    scrapped_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已做弃置出库的数量',
    removal_fee     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '移除费（店铺币种）',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_fba_removal_item (removal_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 移除订单明细';

-- FBA 库存流水（库存分类账明细报告 GET_LEDGER_DETAIL_VIEW_DATA），按行内容去重
CREATE TABLE ecom_fba_ledger (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id         BIGINT         NOT NULL COMMENT '店铺',
    row_key          VARCHAR(64)    NOT NULL COMMENT '行内容摘要（去重）',
    event_date       DATE           NOT NULL COMMENT '日期',
    fnsku            VARCHAR(32)             DEFAULT NULL COMMENT 'FNSKU',
    asin             VARCHAR(32)             DEFAULT NULL COMMENT 'ASIN',
    seller_sku       VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU（MSKU）',
    event_type       VARCHAR(32)    NOT NULL COMMENT '事件类型 Receipts Shipments CustomerReturns VendorReturns WhseTransfers Adjustments',
    reference_id     VARCHAR(64)             DEFAULT NULL COMMENT '参考号（货件号、订单号、调整单号）',
    quantity         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量（正为增加）',
    fulfillment_center VARCHAR(16)           DEFAULT NULL COMMENT '运营中心',
    disposition      VARCHAR(32)             DEFAULT NULL COMMENT '库存状态',
    reason           VARCHAR(16)             DEFAULT NULL COMMENT '调整原因代码（M 丢失、F 找到、E 仓库损坏等）',
    unreconciled_qty DECIMAL(18, 4)          DEFAULT NULL COMMENT '未核对数量',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_ledger (store_id, row_key, tenant_id),
    KEY idx_ecom_fba_ledger_sku (store_id, fnsku, event_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 库存流水';

-- FBA 赔偿记录（赔偿报告 GET_FBA_REIMBURSEMENTS_DATA）
CREATE TABLE ecom_fba_reimbursement (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id           BIGINT         NOT NULL COMMENT '店铺',
    reimbursement_id   VARCHAR(64)    NOT NULL COMMENT '赔偿编号',
    approval_date      DATE                    DEFAULT NULL COMMENT '批准日期',
    case_id            VARCHAR(64)             DEFAULT NULL COMMENT 'Case 编号',
    amazon_order_id    VARCHAR(64)             DEFAULT NULL COMMENT '订单号',
    reason             VARCHAR(64)             DEFAULT NULL COMMENT '原因（Lost_Warehouse、Damaged_Warehouse、Lost_Inbound、CustomerReturn 等）',
    seller_sku         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '平台 SKU',
    fnsku              VARCHAR(32)             DEFAULT NULL COMMENT 'FNSKU',
    asin               VARCHAR(32)             DEFAULT NULL COMMENT 'ASIN',
    currency           VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    amount_per_unit    DECIMAL(18, 2)          DEFAULT NULL COMMENT '每件赔偿',
    amount_total       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '现金赔偿合计',
    qty_cash           DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '现金赔偿件数',
    qty_inventory      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '库存赔偿件数',
    amount_base        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '现金赔偿（本位币）',
    claim_id           BIGINT                  DEFAULT NULL COMMENT '匹配到的索赔',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_reimbursement (store_id, reimbursement_id, seller_sku, tenant_id),
    KEY idx_ecom_fba_reimbursement_claim (claim_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 赔偿记录';

-- FBA 索赔：系统识别（丢失、损坏、入库短少、退货未退回、多收配送费）或手工登记，跟踪 Case 与赔付
CREATE TABLE ecom_fba_claim (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    claim_no      VARCHAR(32)    NOT NULL COMMENT '索赔单号',
    store_id      BIGINT         NOT NULL COMMENT '店铺',
    claim_type    VARCHAR(32)    NOT NULL COMMENT '类型(字典 ecom_fba_claim_type)',
    source_key    VARCHAR(128)   NOT NULL COMMENT '识别来源键（同一来源只生成一次）',
    source_ref    VARCHAR(64)             DEFAULT NULL COMMENT '来源单号（货件号、订单号、流水参考号）',
    seller_sku    VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU',
    fnsku         VARCHAR(32)             DEFAULT NULL COMMENT 'FNSKU',
    qty           DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '索赔数量',
    unit_value    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '估算单价（店铺币种）',
    est_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '估算金额（店铺币种）',
    currency      VARCHAR(8)              DEFAULT NULL COMMENT '币种',
    event_date    DATE                    DEFAULT NULL COMMENT '发生日期',
    deadline      DATE                    DEFAULT NULL COMMENT '索赔截止日期',
    status        VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 ecom_fba_claim_status) PENDING待提交 SUBMITTED已提交 PAID已赔付 REJECTED已驳回 IGNORED已忽略',
    case_no       VARCHAR(64)             DEFAULT NULL COMMENT 'Case 编号',
    submit_date   DATE                    DEFAULT NULL COMMENT '提交日期',
    paid_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已赔付金额（店铺币种）',
    paid_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已赔付件数（含库存赔偿）',
    paid_base     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已赔付金额（本位币）',
    paid_date     DATE                    DEFAULT NULL COMMENT '赔付日期',
    detail        VARCHAR(500)            DEFAULT NULL COMMENT '识别说明',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by     BIGINT                  DEFAULT NULL COMMENT '创建人（自动识别为空）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_claim_no (claim_no, tenant_id),
    UNIQUE KEY uk_ecom_fba_claim_source (store_id, source_key, tenant_id),
    KEY idx_ecom_fba_claim_status (status, deadline)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 索赔';

-- 头程货件的亚马逊实收数量（货件对账，识别入库短少）
CREATE TABLE ecom_fba_inbound_receipt (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_item_id BIGINT         NOT NULL COMMENT '头程货件明细',
    inbound_id      BIGINT         NOT NULL COMMENT '头程货件',
    shipped_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '发货件数（平台 SKU 件数）',
    received_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '亚马逊实收件数',
    source          VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工 API亚马逊接口',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_fba_inbound_receipt (inbound_item_id, tenant_id),
    KEY idx_ecom_fba_inbound_receipt (inbound_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'FBA 货件实收数量';

-- 内置美国站费率（2025 年标准，非服装、售价 10–50 美元、非旺季附加费；请按亚马逊最新费率表核对维护）
INSERT INTO ecom_fba_storage_rate (marketplace, fee_type, size_category, month_from, month_to, age_from, age_to, rate, volume_unit, currency, builtin, remark, create_time, update_time) VALUES
('US', 'MONTHLY', 'STANDARD', 1, 9, NULL, NULL, 0.78, 'CUFT', 'USD', 1, '标准件 1–9 月', NOW(), NOW()),
('US', 'MONTHLY', 'STANDARD', 10, 12, NULL, NULL, 2.40, 'CUFT', 'USD', 1, '标准件 10–12 月（旺季）', NOW(), NOW()),
('US', 'MONTHLY', 'OVERSIZE', 1, 9, NULL, NULL, 0.56, 'CUFT', 'USD', 1, '大件 1–9 月', NOW(), NOW()),
('US', 'MONTHLY', 'OVERSIZE', 10, 12, NULL, NULL, 1.40, 'CUFT', 'USD', 1, '大件 10–12 月（旺季）', NOW(), NOW()),
('US', 'AGED', 'STANDARD', NULL, NULL, 181, 270, 1.00, 'CUFT', 'USD', 1, '181–270 天（181–210 / 211–240 / 241–270 天分别为 0.50 / 1.00 / 1.50 的平均）', NOW(), NOW()),
('US', 'AGED', 'STANDARD', NULL, NULL, 271, 365, 5.68, 'CUFT', 'USD', 1, '271–365 天（5.45 / 5.70 / 5.90 的平均）', NOW(), NOW()),
('US', 'AGED', 'STANDARD', NULL, NULL, 366, NULL, 6.90, 'CUFT', 'USD', 1, '365 天以上', NOW(), NOW()),
('US', 'AGED', 'OVERSIZE', NULL, NULL, 181, 270, 1.00, 'CUFT', 'USD', 1, '181–270 天', NOW(), NOW()),
('US', 'AGED', 'OVERSIZE', NULL, NULL, 271, 365, 5.68, 'CUFT', 'USD', 1, '271–365 天', NOW(), NOW()),
('US', 'AGED', 'OVERSIZE', NULL, NULL, 366, NULL, 6.90, 'CUFT', 'USD', 1, '365 天以上', NOW(), NOW());

INSERT INTO ecom_fba_size_tier (marketplace, code, name, size_category, unit_system, max_long, max_median, max_short, max_girth, max_weight, dim_divisor, sort, builtin, create_time, update_time) VALUES
('US', 'SMALL_STD', '小号标准尺寸', 'STANDARD', 'IMPERIAL', 15, 12, 0.75, NULL, 1, NULL, 1, 1, NOW(), NOW()),
('US', 'LARGE_STD', '大号标准尺寸', 'STANDARD', 'IMPERIAL', 18, 14, 8, NULL, 20, 139, 2, 1, NOW(), NOW()),
('US', 'LARGE_BULKY', '大号大件', 'OVERSIZE', 'IMPERIAL', 59, 33, 33, 130, 50, 139, 3, 1, NOW(), NOW()),
('US', 'XL_50', '超大件（0–50 磅）', 'OVERSIZE', 'IMPERIAL', NULL, NULL, NULL, NULL, 50, 139, 4, 1, NOW(), NOW()),
('US', 'XL_70', '超大件（50–70 磅）', 'OVERSIZE', 'IMPERIAL', NULL, NULL, NULL, NULL, 70, 139, 5, 1, NOW(), NOW()),
('US', 'XL_150', '超大件（70–150 磅）', 'OVERSIZE', 'IMPERIAL', NULL, NULL, NULL, NULL, 150, 139, 6, 1, NOW(), NOW()),
('US', 'XL_150_PLUS', '超大件（150 磅以上）', 'OVERSIZE', 'IMPERIAL', NULL, NULL, NULL, NULL, NULL, 139, 7, 1, NOW(), NOW());

INSERT INTO ecom_fba_fee_rate (marketplace, tier_code, weight_from, weight_to, fee, extra_from, extra_unit, extra_fee, currency, builtin, create_time, update_time) VALUES
('US', 'SMALL_STD', 0, 0.125, 3.06, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.125, 0.25, 3.15, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.25, 0.375, 3.24, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.375, 0.5, 3.33, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.5, 0.625, 3.43, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.625, 0.75, 3.53, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.75, 0.875, 3.60, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'SMALL_STD', 0.875, 1, 3.65, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 0, 0.25, 3.68, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 0.25, 0.5, 3.90, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 0.5, 0.75, 4.15, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 0.75, 1, 4.55, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 1, 1.25, 4.99, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 1.25, 1.5, 5.37, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 1.5, 1.75, 5.52, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 1.75, 2, 5.77, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 2, 2.25, 5.94, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 2.25, 2.5, 6.08, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 2.5, 2.75, 6.24, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 2.75, 3, 6.38, NULL, NULL, NULL, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_STD', 3, 20, 6.92, 3, 0.25, 0.08, 'USD', 1, NOW(), NOW()),
('US', 'LARGE_BULKY', 0, 50, 9.61, 1, 1, 0.38, 'USD', 1, NOW(), NOW()),
('US', 'XL_50', 0, 50, 26.33, 1, 1, 0.38, 'USD', 1, NOW(), NOW()),
('US', 'XL_70', 50, 70, 40.12, 51, 1, 0.75, 'USD', 1, NOW(), NOW()),
('US', 'XL_150', 70, 150, 54.81, 71, 1, 0.75, 'USD', 1, NOW(), NOW()),
('US', 'XL_150_PLUS', 150, NULL, 194.95, 151, 1, 0.19, 'USD', 1, NOW(), NOW());

-- 菜单：电商 → 库存与物流 下的 FBA 运营页面
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(510, 983, 'FBA 库存健康', 'C', 'fba-health', 'ecom/fba-health/index', 'ecom:fbaHealth:list', 'HeartOutlined', 11, 1, 1, NOW(), NOW()),
(511, 983, '移除订单', 'C', 'fba-removal', 'ecom/fba-removal/index', 'ecom:fbaRemoval:list', 'ExportOutlined', 12, 1, 1, NOW(), NOW()),
(512, 983, 'FBA 索赔', 'C', 'fba-claim', 'ecom/fba-claim/index', 'ecom:fbaClaim:list', 'SafetyCertificateOutlined', 13, 1, 1, NOW(), NOW()),
(513, 983, 'FBA 费用核查', 'C', 'fba-fee-audit', 'ecom/fba-fee-audit/index', 'ecom:fbaFee:list', 'AuditOutlined', 14, 1, 1, NOW(), NOW()),
(514, 983, '亚马逊报告', 'C', 'amz-report', 'ecom/amz-report/index', 'ecom:amzReport:list', 'FileSearchOutlined', 15, 1, 1, NOW(), NOW()),
(5100, 510, '同步 / 推算库存健康', 'F', NULL, NULL, 'ecom:fbaHealth:sync', NULL, 1, 1, 1, NOW(), NOW()),
(5101, 510, '维护仓储费费率', 'F', NULL, NULL, 'ecom:fbaHealth:rate', NULL, 2, 1, 1, NOW(), NOW()),
(5110, 511, '移除订单新增', 'F', NULL, NULL, 'ecom:fbaRemoval:add', NULL, 1, 1, 1, NOW(), NOW()),
(5111, 511, '移除订单修改', 'F', NULL, NULL, 'ecom:fbaRemoval:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5112, 511, '移除订单删除', 'F', NULL, NULL, 'ecom:fbaRemoval:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5113, 511, '移除收货 / 弃置出库', 'F', NULL, NULL, 'ecom:fbaRemoval:receive', NULL, 4, 1, 1, NOW(), NOW()),
(5114, 511, '同步移除订单', 'F', NULL, NULL, 'ecom:fbaRemoval:sync', NULL, 5, 1, 1, NOW(), NOW()),
(5120, 512, '识别 / 新增索赔', 'F', NULL, NULL, 'ecom:fbaClaim:add', NULL, 1, 1, 1, NOW(), NOW()),
(5121, 512, '索赔处理', 'F', NULL, NULL, 'ecom:fbaClaim:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5122, 512, '索赔删除', 'F', NULL, NULL, 'ecom:fbaClaim:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5123, 512, '同步赔偿 / 流水', 'F', NULL, NULL, 'ecom:fbaClaim:sync', NULL, 4, 1, 1, NOW(), NOW()),
(5130, 513, '维护尺寸与配送费费率', 'F', NULL, NULL, 'ecom:fbaFee:edit', NULL, 1, 1, 1, NOW(), NOW()),
(5140, 514, '创建 / 上传报告', 'F', NULL, NULL, 'ecom:amzReport:add', NULL, 1, 1, 1, NOW(), NOW()),
(5141, 514, '报告删除', 'F', NULL, NULL, 'ecom:amzReport:remove', NULL, 2, 1, 1, NOW(), NOW());

-- 字典（系统字典，代码按取值判断）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(310, '亚马逊报告类型', 'ecom_amz_report_type', 'SYSTEM', 1, 'FBA 运营使用的亚马逊报告', NOW(), NOW()),
(311, '报告任务状态', 'ecom_amz_report_status', 'SYSTEM', 1, '亚马逊报告任务的状态', NOW(), NOW()),
(312, 'FBA 处理建议', 'ecom_fba_advice', 'SYSTEM', 1, '库存健康的处理建议', NOW(), NOW()),
(313, '移除类型', 'ecom_fba_removal_type', 'SYSTEM', 1, 'FBA 移除订单的类型', NOW(), NOW()),
(314, '移除订单状态', 'ecom_fba_removal_status', 'SYSTEM', 1, 'FBA 移除订单的状态', NOW(), NOW()),
(315, 'FBA 库存状态', 'ecom_fba_disposition', 'SYSTEM', 1, '可售 / 不可售', NOW(), NOW()),
(316, '索赔类型', 'ecom_fba_claim_type', 'SYSTEM', 1, 'FBA 索赔的类型', NOW(), NOW()),
(317, '索赔状态', 'ecom_fba_claim_status', 'SYSTEM', 1, 'FBA 索赔的处理状态', NOW(), NOW()),
(318, 'FBA 尺寸类别', 'ecom_fba_size_category', 'SYSTEM', 1, '标准件 / 大件，仓储费按它取费率', NOW(), NOW()),
(319, 'FBA 仓储费类型', 'ecom_fba_storage_fee_type', 'SYSTEM', 1, '月度仓储费 / 超龄附加费', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_amz_report_type', '库存规划（库龄）', 'FBA_INVENTORY_PLANNING', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_amz_report_type', 'FBA 库存', 'FBA_MYI_INVENTORY', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_amz_report_type', '库存分类账明细', 'LEDGER_DETAIL', 'geekblue', 3, 1, 1, NOW(), NOW()),
('ecom_amz_report_type', '赔偿', 'REIMBURSEMENTS', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_amz_report_type', '移除订单详情', 'REMOVAL_ORDERS', 'orange', 5, 1, 1, NOW(), NOW()),
('ecom_amz_report_type', '预估 FBA 费用', 'ESTIMATED_FEES', 'purple', 6, 1, 1, NOW(), NOW()),
('ecom_amz_report_status', '已提交', 'SUBMITTED', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_amz_report_status', '生成中', 'PROCESSING', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_amz_report_status', '已完成', 'DONE', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_amz_report_status', '失败', 'FAILED', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_amz_report_status', '已取消', 'CANCELLED', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_fba_advice', '健康', 'HEALTHY', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_fba_advice', '站外推广', 'PROMOTE', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_fba_advice', '降价清仓', 'PRICE_DOWN', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_fba_advice', '移除 / 弃置', 'REMOVE', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_fba_removal_type', '退回', 'RETURN', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_fba_removal_type', '弃置', 'DISPOSAL', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_fba_removal_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_fba_removal_status', '已提交', 'SUBMITTED', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_fba_removal_status', '处理中', 'PROCESSING', 'processing', 3, 1, 1, NOW(), NOW()),
('ecom_fba_removal_status', '亚马逊已完成', 'COMPLETED', 'orange', 4, 1, 1, NOW(), NOW()),
('ecom_fba_removal_status', '已收货', 'RECEIVED', 'green', 5, 1, 1, NOW(), NOW()),
('ecom_fba_removal_status', '已取消', 'CANCELLED', 'red', 6, 1, 1, NOW(), NOW()),
('ecom_fba_disposition', '可售', 'SELLABLE', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_fba_disposition', '不可售', 'UNSELLABLE', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_fba_claim_type', '仓库丢失', 'LOST', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_fba_claim_type', '仓库损坏', 'DAMAGED', 'volcano', 2, 1, 1, NOW(), NOW()),
('ecom_fba_claim_type', '入库短少', 'INBOUND_SHORTAGE', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_fba_claim_type', '退货未退回', 'RETURN_NOT_RECEIVED', 'gold', 4, 1, 1, NOW(), NOW()),
('ecom_fba_claim_type', '多收配送费', 'FEE_OVERCHARGE', 'purple', 5, 1, 1, NOW(), NOW()),
('ecom_fba_claim_type', '其他', 'OTHER', 'default', 6, 1, 1, NOW(), NOW()),
('ecom_fba_claim_status', '待提交', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_fba_claim_status', '已提交', 'SUBMITTED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_fba_claim_status', '已赔付', 'PAID', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_fba_claim_status', '已驳回', 'REJECTED', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_fba_claim_status', '已忽略', 'IGNORED', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_fba_size_category', '标准件', 'STANDARD', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_fba_size_category', '大件', 'OVERSIZE', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_fba_storage_fee_type', '月度仓储费', 'MONTHLY', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_fba_storage_fee_type', '超龄附加费', 'AGED', 'red', 2, 1, 1, NOW(), NOW());

-- 内置角色：超级管理员、电商运营全部；总经理只读；仓库管理员负责移除订单收货 / 弃置出库
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id BETWEEN 510 AND 519 OR m.parent_id BETWEEN 510 AND 519)
WHERE r.id IN (1, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id BETWEEN 510 AND 519
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (511, 5113)
WHERE r.id = 6 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第四轮：产品开发与 1688 采购 =====
-- 开发项目（选品 → 调研 → 打样 → 审批 → 生成物料）、样品记录、流转记录；利润试算方案与平台佣金费率；
-- 1688 开放平台账号（OAuth 授权，凭证加密保存）、物料关联 1688 商品与价格变动、由采购订单下单生成的 1688 采购单
DROP TABLE IF EXISTS ecom_dev_project;
CREATE TABLE ecom_dev_project (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no             VARCHAR(32)    NOT NULL COMMENT '项目编号（编码规则 KF）',
    name               VARCHAR(128)   NOT NULL COMMENT '候选产品名称',
    category           VARCHAR(64)             DEFAULT NULL COMMENT '品类（物料分类编码 wms_material_category.code）',
    brand              VARCHAR(64)             DEFAULT NULL COMMENT '品牌（业务字典 wms_material_brand）',
    source             VARCHAR(32)             DEFAULT NULL COMMENT '产品来源(业务字典 ecom_dev_source)',
    platform           VARCHAR(32)             DEFAULT NULL COMMENT '目标平台(字典 ecom_platform)',
    marketplace        VARCHAR(16)             DEFAULT NULL COMMENT '目标站点(业务字典 ecom_marketplace)',
    currency           VARCHAR(8)              DEFAULT NULL COMMENT '站点币种（竞品价格、目标售价的币种）',
    competitor_url     VARCHAR(500)            DEFAULT NULL COMMENT '参考竞品链接',
    competitor_price   DECIMAL(18, 2)          DEFAULT NULL COMMENT '竞品售价（站点币种）',
    competitor_reviews INT                     DEFAULT NULL COMMENT '竞品评论数',
    competitor_rating  DECIMAL(3, 1)           DEFAULT NULL COMMENT '竞品评分',
    est_monthly_sales  INT                     DEFAULT NULL COMMENT '预估月销量',
    target_price       DECIMAL(18, 2)          DEFAULT NULL COMMENT '目标售价（站点币种）',
    est_purchase_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '预估采购价（本位币，含税）',
    weight_kg          DECIMAL(10, 3)          DEFAULT NULL COMMENT '预估单件重量（kg）',
    length_cm          DECIMAL(10, 2)          DEFAULT NULL COMMENT '包装长（cm）',
    width_cm           DECIMAL(10, 2)          DEFAULT NULL COMMENT '包装宽（cm）',
    height_cm          DECIMAL(10, 2)          DEFAULT NULL COMMENT '包装高（cm）',
    est_profit         DECIMAL(18, 2)          DEFAULT NULL COMMENT '试算单件利润（本位币，保存试算方案时回写）',
    est_margin         DECIMAL(10, 2)          DEFAULT NULL COMMENT '试算利润率（%）',
    owner_id           BIGINT                  DEFAULT NULL COMMENT '开发负责人',
    images             VARCHAR(2000)           DEFAULT NULL COMMENT '图片地址（JSON 数组）',
    selling_points     VARCHAR(1000)           DEFAULT NULL COMMENT '卖点 / 差异化',
    description        VARCHAR(2000)           DEFAULT NULL COMMENT '调研说明',
    status             VARCHAR(20)    NOT NULL DEFAULT 'PENDING_RESEARCH' COMMENT '状态(字典 ecom_dev_status)',
    prev_status        VARCHAR(20)             DEFAULT NULL COMMENT '提交审批前的状态（撤回、驳回后可恢复）',
    flow_instance_id   VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    material_id        BIGINT                  DEFAULT NULL COMMENT '审批通过后生成的物料',
    approve_by         BIGINT                  DEFAULT NULL COMMENT '审批人（手工审批时）',
    approve_time       DATETIME                DEFAULT NULL COMMENT '审批通过 / 驳回时间',
    reject_reason      VARCHAR(255)            DEFAULT NULL COMMENT '驳回 / 放弃原因',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id            BIGINT                  DEFAULT NULL COMMENT '创建人部门（数据权限）',
    create_by          BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_dev_project_no (doc_no, tenant_id),
    KEY idx_ecom_dev_project_status (status),
    KEY idx_ecom_dev_project_owner (owner_id),
    KEY idx_ecom_dev_project_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '产品开发项目（候选产品）';

DROP TABLE IF EXISTS ecom_dev_sample;
CREATE TABLE ecom_dev_sample (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id    BIGINT         NOT NULL COMMENT '开发项目',
    supplier_id   BIGINT                  DEFAULT NULL COMMENT '供应商(scm_supplier)，未建档时填名称',
    supplier_name VARCHAR(128)            DEFAULT NULL COMMENT '供应商名称（未建档的工厂 / 1688 店铺）',
    sample_url    VARCHAR(500)            DEFAULT NULL COMMENT '样品链接（如 1688 商品链接）',
    sample_qty    INT                     DEFAULT NULL COMMENT '样品数量',
    sample_fee    DECIMAL(18, 2)          DEFAULT NULL COMMENT '样品费（本位币，含运费）',
    quote_price   DECIMAL(18, 4)          DEFAULT NULL COMMENT '大货报价（本位币）',
    request_date  DATE                    DEFAULT NULL COMMENT '申请打样日期',
    arrival_date  DATE                    DEFAULT NULL COMMENT '到样日期',
    result        VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '评估结论(字典 ecom_dev_sample_result) PENDING待评估 PASSED合格 FAILED不合格',
    evaluation    VARCHAR(1000)           DEFAULT NULL COMMENT '评估意见',
    create_by     BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_dev_sample_project (project_id),
    KEY idx_ecom_dev_sample_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开发项目的样品记录';

DROP TABLE IF EXISTS ecom_dev_log;
CREATE TABLE ecom_dev_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id  BIGINT       NOT NULL COMMENT '开发项目',
    action      VARCHAR(16)  NOT NULL COMMENT '操作 CREATE MOVE SUBMIT WITHDRAW APPROVE REJECT ABANDON MATERIAL',
    from_status VARCHAR(20)           DEFAULT NULL COMMENT '原状态',
    to_status   VARCHAR(20)           DEFAULT NULL COMMENT '新状态',
    remark      VARCHAR(500)          DEFAULT NULL COMMENT '说明（驳回原因、审批意见等）',
    create_by   BIGINT                DEFAULT NULL COMMENT '操作人（审批流程回调时为空）',
    create_time DATETIME              DEFAULT NULL COMMENT '操作时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_dev_log_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开发项目流转记录';

DROP TABLE IF EXISTS ecom_commission_rate;
CREATE TABLE ecom_commission_rate (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    platform    VARCHAR(32)    NOT NULL COMMENT '平台(字典 ecom_platform)',
    marketplace VARCHAR(16)    NOT NULL DEFAULT '' COMMENT '站点，空为该平台全部站点',
    category    VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '平台类目名称，空为默认类目',
    rate        DECIMAL(6, 2)  NOT NULL DEFAULT 0 COMMENT '佣金率（%）',
    min_fee     DECIMAL(18, 2)          DEFAULT NULL COMMENT '单件最低佣金（站点币种）',
    fixed_fee   DECIMAL(18, 2)          DEFAULT NULL COMMENT '每单固定费 / 交易手续费（站点币种）',
    currency    VARCHAR(8)              DEFAULT NULL COMMENT '站点币种',
    vat_rate    DECIMAL(6, 2)           DEFAULT NULL COMMENT '默认 VAT / GST 税率（%，价内税），空则取税务中的税率',
    builtin     TINYINT        NOT NULL DEFAULT 0 COMMENT '内置 1是（新租户复制）',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_commission_rate (platform, marketplace, category, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台佣金费率（利润试算用）';

DROP TABLE IF EXISTS ecom_profit_scheme;
CREATE TABLE ecom_profit_scheme (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)    NOT NULL COMMENT '方案名称',
    project_id  BIGINT                  DEFAULT NULL COMMENT '开发项目（在项目详情中保存时）',
    platform    VARCHAR(32)             DEFAULT NULL COMMENT '平台',
    marketplace VARCHAR(16)             DEFAULT NULL COMMENT '站点',
    currency    VARCHAR(8)              DEFAULT NULL COMMENT '站点币种',
    sale_price  DECIMAL(18, 2)          DEFAULT NULL COMMENT '售价（站点币种）',
    profit      DECIMAL(18, 2)          DEFAULT NULL COMMENT '单件利润（本位币）',
    margin      DECIMAL(10, 2)          DEFAULT NULL COMMENT '利润率（%）',
    roi         DECIMAL(10, 2)          DEFAULT NULL COMMENT '投资回报率（%）',
    params      TEXT COMMENT '试算参数（JSON，载入方案时还原）',
    create_by   BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_profit_scheme_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '利润试算方案';

DROP TABLE IF EXISTS ecom_1688_account;
CREATE TABLE ecom_1688_account (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name            VARCHAR(64)  NOT NULL COMMENT '账号名称',
    app_key         VARCHAR(64)  NOT NULL COMMENT '1688 开放平台 AppKey',
    app_secret      VARCHAR(500)          DEFAULT NULL COMMENT 'AppSecret（加密保存，enc: 前缀）',
    access_token    VARCHAR(1000)         DEFAULT NULL COMMENT '访问令牌（加密）',
    refresh_token   VARCHAR(1000)         DEFAULT NULL COMMENT '刷新令牌（加密）',
    token_expire    DATETIME              DEFAULT NULL COMMENT '访问令牌过期时间',
    refresh_expire  DATETIME              DEFAULT NULL COMMENT '刷新令牌过期时间',
    member_id       VARCHAR(64)           DEFAULT NULL COMMENT '授权买家的 memberId',
    login_id        VARCHAR(64)           DEFAULT NULL COMMENT '授权买家的登录名（resource_owner）',
    authorized_time DATETIME              DEFAULT NULL COMMENT '授权时间',
    api_url         VARCHAR(255)          DEFAULT NULL COMMENT '自定义网关地址，为空用 https://gw.open.1688.com/openapi',
    receiver_name   VARCHAR(64)           DEFAULT NULL COMMENT '收货人',
    receiver_mobile VARCHAR(32)           DEFAULT NULL COMMENT '收货手机',
    province        VARCHAR(32)           DEFAULT NULL COMMENT '省',
    city            VARCHAR(32)           DEFAULT NULL COMMENT '市',
    district        VARCHAR(32)           DEFAULT NULL COMMENT '区 / 县',
    town            VARCHAR(32)           DEFAULT NULL COMMENT '镇 / 街道',
    address         VARCHAR(255)          DEFAULT NULL COMMENT '详细地址',
    post_code       VARCHAR(16)           DEFAULT NULL COMMENT '邮编',
    is_default      TINYINT      NOT NULL DEFAULT 0 COMMENT '默认下单账号 1是',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_message    VARCHAR(500)          DEFAULT NULL COMMENT '最近一次调用失败的原因，成功为空',
    remark          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time     DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_1688_account_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '1688 开放平台采购账号';

DROP TABLE IF EXISTS ecom_1688_offer;
CREATE TABLE ecom_1688_offer (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id       BIGINT         NOT NULL COMMENT '物料',
    offer_id          VARCHAR(32)    NOT NULL COMMENT '1688 商品 ID（offerId）',
    spec_id           VARCHAR(64)             DEFAULT NULL COMMENT '规格 ID（specId），单规格商品为空',
    sku_id            VARCHAR(32)             DEFAULT NULL COMMENT 'SKU ID',
    supplier_id       BIGINT                  DEFAULT NULL COMMENT '对应的供应商(scm_supplier)',
    seller_login_id   VARCHAR(64)             DEFAULT NULL COMMENT '卖家登录名（下单时按它拆单）',
    seller_name       VARCHAR(128)            DEFAULT NULL COMMENT '卖家公司 / 店铺名称',
    title             VARCHAR(255)            DEFAULT NULL COMMENT '1688 商品标题',
    spec_text         VARCHAR(255)            DEFAULT NULL COMMENT '规格属性',
    image_url         VARCHAR(500)            DEFAULT NULL COMMENT '主图',
    unit              VARCHAR(16)             DEFAULT NULL COMMENT '1688 计量单位',
    price             DECIMAL(18, 4)          DEFAULT NULL COMMENT '当前价格（元，按起批量的价格）',
    last_price        DECIMAL(18, 4)          DEFAULT NULL COMMENT '上次价格（价格变动前）',
    price_ranges      VARCHAR(500)            DEFAULT NULL COMMENT '阶梯价（JSON [{startQuantity, price}]）',
    min_order_qty     INT                     DEFAULT NULL COMMENT '起批量',
    stock             INT                     DEFAULT NULL COMMENT '可售库存',
    qty_ratio         DECIMAL(10, 4) NOT NULL DEFAULT 1 COMMENT '1 个 1688 单位 = 多少物料单位（下单数量 = 采购数量 ÷ 换算，向上取整）',
    price_alert       TINYINT        NOT NULL DEFAULT 0 COMMENT '价格变动未确认 1是',
    alert_threshold   DECIMAL(6, 2)           DEFAULT NULL COMMENT '提醒阈值（%），变动幅度超过它才提醒，空为任何变动都提醒',
    price_change_time DATETIME                DEFAULT NULL COMMENT '最近一次价格变动时间',
    sync_time         DATETIME                DEFAULT NULL COMMENT '最近同步时间',
    sync_message      VARCHAR(255)            DEFAULT NULL COMMENT '最近一次同步失败的原因（如商品下架），成功为空',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_1688_offer_material (material_id, tenant_id),
    KEY idx_ecom_1688_offer_offer (offer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料关联的 1688 商品（货源）';

DROP TABLE IF EXISTS ecom_1688_price_log;
CREATE TABLE ecom_1688_price_log (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    offer_id    BIGINT         NOT NULL COMMENT '1688 关联(ecom_1688_offer)',
    material_id BIGINT         NOT NULL COMMENT '物料',
    old_price   DECIMAL(18, 4)          DEFAULT NULL COMMENT '原价格',
    new_price   DECIMAL(18, 4)          DEFAULT NULL COMMENT '新价格',
    change_rate DECIMAL(10, 2)          DEFAULT NULL COMMENT '变动幅度（%）',
    create_time DATETIME                DEFAULT NULL COMMENT '发现时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_1688_price_log_offer (offer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '1688 价格变动记录';

DROP TABLE IF EXISTS ecom_1688_order;
CREATE TABLE ecom_1688_order (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id        BIGINT         NOT NULL COMMENT '下单账号(ecom_1688_account)',
    scm_order_id      BIGINT                  DEFAULT NULL COMMENT '采购订单(scm_order)',
    ali_order_id      VARCHAR(32)    NOT NULL COMMENT '1688 订单号',
    seller_login_id   VARCHAR(64)             DEFAULT NULL COMMENT '卖家登录名',
    seller_name       VARCHAR(128)            DEFAULT NULL COMMENT '卖家名称',
    status            VARCHAR(24)    NOT NULL DEFAULT 'WAIT_BUYER_PAY' COMMENT '状态(字典 ecom_1688_order_status)',
    ali_status        VARCHAR(32)             DEFAULT NULL COMMENT '1688 原始状态',
    product_amount    DECIMAL(18, 2)          DEFAULT NULL COMMENT '货款（元）',
    shipping_fee      DECIMAL(18, 2)          DEFAULT NULL COMMENT '运费（元）',
    total_amount      DECIMAL(18, 2)          DEFAULT NULL COMMENT '实付金额（元，含运费）',
    total_qty         DECIMAL(18, 4)          DEFAULT NULL COMMENT '1688 下单数量合计',
    logistics_company VARCHAR(64)             DEFAULT NULL COMMENT '物流公司',
    logistics_no      VARCHAR(64)             DEFAULT NULL COMMENT '物流单号（多个用逗号分隔）',
    trace             TEXT COMMENT '物流轨迹（JSON [{time, remark}]）',
    message           VARCHAR(500)            DEFAULT NULL COMMENT '买家留言',
    ali_create_time   DATETIME                DEFAULT NULL COMMENT '1688 下单时间',
    pay_time          DATETIME                DEFAULT NULL COMMENT '付款时间',
    ship_time         DATETIME                DEFAULT NULL COMMENT '发货时间',
    receive_time      DATETIME                DEFAULT NULL COMMENT '收货时间',
    sync_time         DATETIME                DEFAULT NULL COMMENT '最近同步时间',
    sync_message      VARCHAR(500)            DEFAULT NULL COMMENT '最近一次同步失败的原因，成功为空',
    create_by         BIGINT                  DEFAULT NULL COMMENT '下单人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_1688_order_no (ali_order_id, tenant_id),
    KEY idx_ecom_1688_order_po (scm_order_id),
    KEY idx_ecom_1688_order_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '1688 采购单（由采购订单一键下单生成）';

DROP TABLE IF EXISTS ecom_1688_order_item;
CREATE TABLE ecom_1688_order_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id          BIGINT         NOT NULL COMMENT '1688 采购单(ecom_1688_order)',
    scm_order_item_id BIGINT                  DEFAULT NULL COMMENT '采购订单明细(scm_order_item)',
    material_id       BIGINT         NOT NULL COMMENT '物料',
    offer_id          VARCHAR(32)    NOT NULL COMMENT '1688 商品 ID',
    spec_id           VARCHAR(64)             DEFAULT NULL COMMENT '规格 ID',
    qty               DECIMAL(18, 4) NOT NULL COMMENT '1688 下单数量',
    material_qty      DECIMAL(18, 4)          DEFAULT NULL COMMENT '对应的物料数量',
    price             DECIMAL(18, 4)          DEFAULT NULL COMMENT '下单时的 1688 单价',
    amount            DECIMAL(18, 2)          DEFAULT NULL COMMENT '金额',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_1688_order_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '1688 采购单明细';

-- 内置平台佣金费率（常见平台的默认值，请按平台最新规则维护；空站点 = 该平台全部站点）
INSERT INTO ecom_commission_rate (platform, marketplace, category, rate, min_fee, fixed_fee, currency, vat_rate, builtin, status, remark, create_time, update_time) VALUES
('AMAZON', 'US', '', 15.00, 0.30, NULL, 'USD', 0.00, 1, 1, '亚马逊美国站大多数类目 15%，单件最低 0.30 美元；销售税由平台代收代缴', NOW(), NOW()),
('AMAZON', 'US', '消费电子', 8.00, 0.30, NULL, 'USD', 0.00, 1, 1, '消费类电子产品', NOW(), NOW()),
('AMAZON', 'US', '服装与配饰', 17.00, 0.30, NULL, 'USD', 0.00, 1, 1, '售价超过 20 美元的部分', NOW(), NOW()),
('AMAZON', 'US', '家居与厨房', 15.00, 0.30, NULL, 'USD', 0.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'US', '运动户外', 15.00, 0.30, NULL, 'USD', 0.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'CA', '', 15.00, 0.30, NULL, 'CAD', 0.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'MX', '', 15.00, 10.00, NULL, 'MXN', 16.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'UK', '', 15.30, 0.25, NULL, 'GBP', 20.00, 1, 1, '欧洲站佣金按含税售价计算', NOW(), NOW()),
('AMAZON', 'DE', '', 15.00, 0.30, NULL, 'EUR', 19.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'FR', '', 15.00, 0.30, NULL, 'EUR', 20.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'IT', '', 15.00, 0.30, NULL, 'EUR', 22.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'ES', '', 15.00, 0.30, NULL, 'EUR', 21.00, 1, 1, NULL, NOW(), NOW()),
('AMAZON', 'JP', '', 10.00, 30.00, NULL, 'JPY', 10.00, 1, 1, '消费税 10%', NOW(), NOW()),
('AMAZON', 'AU', '', 15.00, 0.30, NULL, 'AUD', 10.00, 1, 1, 'GST 10%', NOW(), NOW()),
('EBAY', 'US', '', 13.25, NULL, 0.30, 'USD', 0.00, 1, 1, '成交费 13.25% + 每单 0.30 美元（售价超过 10 美元为 0.40）', NOW(), NOW()),
('WALMART', 'US', '', 15.00, NULL, NULL, 'USD', 0.00, 1, 1, NULL, NOW(), NOW()),
('TIKTOK', 'US', '', 8.00, NULL, 0.30, 'USD', 0.00, 1, 1, '推荐费 + 每单固定费', NOW(), NOW()),
('SHOPEE', '', '', 12.00, NULL, NULL, NULL, NULL, 1, 1, '佣金 + 交易手续费，按站点不同', NOW(), NOW()),
('LAZADA', '', '', 10.00, NULL, NULL, NULL, NULL, 1, 1, '佣金 + 支付手续费，按站点不同', NOW(), NOW()),
('ALIEXPRESS', '', '', 8.00, NULL, NULL, 'USD', NULL, 1, 1, NULL, NOW(), NOW()),
('SHOPIFY', '', '', 2.90, NULL, 0.30, 'USD', NULL, 1, 1, '独立站支付手续费 2.9% + 0.30 美元', NOW(), NOW()),
('MERCADO_LIBRE', '', '', 16.00, NULL, NULL, NULL, NULL, 1, 1, NULL, NOW(), NOW()),
('OZON', '', '', 15.00, NULL, NULL, NULL, NULL, 1, 1, NULL, NOW(), NOW()),
('TEMU', '', '', 0.00, NULL, NULL, NULL, NULL, 1, 1, '全托管按供货价结算，平台不收佣金', NOW(), NOW());

-- 菜单：电商下的分组「产品开发」（开发项目、开发看板、利润试算），「库存与物流」下的 1688 采购
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(980, 17, '产品开发', 'M', NULL, NULL, NULL, NULL, 7, 1, 1, NOW(), NOW()),
(550, 980, '开发项目', 'C', 'dev-project', 'ecom/dev-project/index', 'ecom:devProject:list', 'ExperimentOutlined', 1, 1, 1, NOW(), NOW()),
(551, 980, '开发看板', 'C', 'dev-board', 'ecom/dev-board/index', 'ecom:devProject:list', 'ProjectOutlined', 2, 1, 1, NOW(), NOW()),
(552, 980, '利润试算', 'C', 'profit-calc', 'ecom/profit-calc/index', 'ecom:profitCalc:list', 'CalculatorOutlined', 3, 1, 1, NOW(), NOW()),
(553, 983, '1688 账号', 'C', 'ali1688-account', 'ecom/ali1688-account/index', 'ecom:ali1688Account:list', 'KeyOutlined', 6, 1, 1, NOW(), NOW()),
(554, 983, '1688 货源', 'C', 'ali1688-offer', 'ecom/ali1688-offer/index', 'ecom:ali1688Offer:list', 'LinkOutlined', 7, 1, 1, NOW(), NOW()),
(555, 983, '1688 采购单', 'C', 'ali1688-order', 'ecom/ali1688-order/index', 'ecom:ali1688Order:list', 'ShoppingCartOutlined', 8, 1, 1, NOW(), NOW()),
(5500, 550, '开发项目新增', 'F', NULL, NULL, 'ecom:devProject:add', NULL, 1, 1, 1, NOW(), NOW()),
(5501, 550, '开发项目修改（含样品、状态流转、提交审批）', 'F', NULL, NULL, 'ecom:devProject:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5502, 550, '开发项目删除', 'F', NULL, NULL, 'ecom:devProject:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5503, 550, '开发项目审批', 'F', NULL, NULL, 'ecom:devProject:approve', NULL, 4, 1, 1, NOW(), NOW()),
(5504, 550, '生成物料', 'F', NULL, NULL, 'ecom:devProject:material', NULL, 5, 1, 1, NOW(), NOW()),
(5520, 552, '保存试算方案', 'F', NULL, NULL, 'ecom:profitCalc:save', NULL, 1, 1, 1, NOW(), NOW()),
(5521, 552, '佣金费率维护', 'F', NULL, NULL, 'ecom:profitCalc:rate', NULL, 2, 1, 1, NOW(), NOW()),
(5530, 553, '账号维护与授权', 'F', NULL, NULL, 'ecom:ali1688Account:edit', NULL, 1, 1, 1, NOW(), NOW()),
(5540, 554, '货源关联维护', 'F', NULL, NULL, 'ecom:ali1688Offer:edit', NULL, 1, 1, 1, NOW(), NOW()),
(5541, 554, '同步价格库存', 'F', NULL, NULL, 'ecom:ali1688Offer:sync', NULL, 2, 1, 1, NOW(), NOW()),
(5550, 555, '下单到 1688', 'F', NULL, NULL, 'ecom:ali1688Order:create', NULL, 1, 1, 1, NOW(), NOW()),
(5551, 555, '同步订单与物流', 'F', NULL, NULL, 'ecom:ali1688Order:sync', NULL, 2, 1, 1, NOW(), NOW()),
(5552, 555, '取消 1688 订单', 'F', NULL, NULL, 'ecom:ali1688Order:cancel', NULL, 3, 1, 1, NOW(), NOW());

-- 字典：开发状态、样品结论、1688 订单状态（系统字典），产品来源（业务字典）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(350, '开发状态', 'ecom_dev_status', 'SYSTEM', 1, '产品开发项目的状态', NOW(), NOW()),
(351, '样品结论', 'ecom_dev_sample_result', 'SYSTEM', 1, '开发样品的评估结论', NOW(), NOW()),
(352, '1688 订单状态', 'ecom_1688_order_status', 'SYSTEM', 1, '1688 采购单的交易状态', NOW(), NOW()),
(353, '产品来源', 'ecom_dev_source', 'BUSINESS', 1, '开发项目的选品来源，可自行扩展', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_dev_status', '待调研', 'PENDING_RESEARCH', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_dev_status', '调研中', 'RESEARCHING', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_dev_status', '打样中', 'SAMPLING', 'cyan', 3, 1, 1, NOW(), NOW()),
('ecom_dev_status', '待审批', 'PENDING_APPROVAL', 'orange', 4, 1, 1, NOW(), NOW()),
('ecom_dev_status', '已通过', 'APPROVED', 'green', 5, 1, 1, NOW(), NOW()),
('ecom_dev_status', '已驳回', 'REJECTED', 'red', 6, 1, 1, NOW(), NOW()),
('ecom_dev_status', '已放弃', 'ABANDONED', 'default', 7, 1, 1, NOW(), NOW()),
('ecom_dev_sample_result', '待评估', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_dev_sample_result', '合格', 'PASSED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_dev_sample_result', '不合格', 'FAILED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '待付款', 'WAIT_BUYER_PAY', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '待发货', 'WAIT_SELLER_SEND', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '待收货', 'WAIT_BUYER_RECEIVE', 'cyan', 3, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '已收货', 'CONFIRM_GOODS', 'geekblue', 4, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '交易成功', 'SUCCESS', 'green', 5, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '已取消', 'CANCEL', 'default', 6, 1, 1, NOW(), NOW()),
('ecom_1688_order_status', '交易终止', 'TERMINATED', 'red', 7, 1, 1, NOW(), NOW()),
('ecom_dev_source', '1688 选品', 'ALI1688', 'orange', 1, 1, 0, NOW(), NOW()),
('ecom_dev_source', '平台热卖', 'BESTSELLER', 'red', 2, 1, 0, NOW(), NOW()),
('ecom_dev_source', '展会 / 工厂', 'FACTORY', 'blue', 3, 1, 0, NOW(), NOW()),
('ecom_dev_source', '自主设计', 'DESIGN', 'purple', 4, 1, 0, NOW(), NOW()),
('ecom_dev_source', '其他', 'OTHER', 'default', 5, 1, 0, NOW(), NOW());

-- 内置流程（未发布）：开发项目审批
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('ecom_dev_project', '开发项目审批', 'ECOM', 40, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "margin_check", "type": "CONDITION", "name": "按试算利润率", "next": null, "branches": [{"id": "margin_check_low", "name": "利润率 < 20%", "defaultBranch": false, "conditions": [[{"field": "estMargin", "operator": "LT", "value": "20"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "margin_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 试算利润率低于 20% 时总经理审批。未发布时开发项目提交后进入待审批，由有审批权限的人在项目中通过或驳回；发布后提交即发起审批，通过后自动生成物料', NOW(), NOW());

-- 内置角色：电商运营全部；采购员 1688 采购全部、利润试算；总经理查看全部并可审批开发项目；普通用户只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT g.role_id, g.menu_id FROM (
    SELECT 5 AS role_id, 980 AS menu_id UNION ALL SELECT 5, 550 UNION ALL SELECT 5, 551 UNION ALL SELECT 5, 552 UNION ALL SELECT 5, 553
    UNION ALL SELECT 5, 554 UNION ALL SELECT 5, 555 UNION ALL SELECT 5, 5500 UNION ALL SELECT 5, 5501 UNION ALL SELECT 5, 5502
    UNION ALL SELECT 5, 5504 UNION ALL SELECT 5, 5520 UNION ALL SELECT 5, 5521 UNION ALL SELECT 5, 5530 UNION ALL SELECT 5, 5540
    UNION ALL SELECT 5, 5541 UNION ALL SELECT 5, 5550 UNION ALL SELECT 5, 5551 UNION ALL SELECT 5, 5552
    UNION ALL SELECT 4, 17 UNION ALL SELECT 4, 983 UNION ALL SELECT 4, 980 UNION ALL SELECT 4, 552 UNION ALL SELECT 4, 5520
    UNION ALL SELECT 4, 553 UNION ALL SELECT 4, 554 UNION ALL SELECT 4, 555 UNION ALL SELECT 4, 5530 UNION ALL SELECT 4, 5540
    UNION ALL SELECT 4, 5541 UNION ALL SELECT 4, 5550 UNION ALL SELECT 4, 5551 UNION ALL SELECT 4, 5552
    UNION ALL SELECT 3, 980 UNION ALL SELECT 3, 550 UNION ALL SELECT 3, 551 UNION ALL SELECT 3, 552 UNION ALL SELECT 3, 553
    UNION ALL SELECT 3, 554 UNION ALL SELECT 3, 555 UNION ALL SELECT 3, 5503
    UNION ALL SELECT 2, 980 UNION ALL SELECT 2, 550 UNION ALL SELECT 2, 551 UNION ALL SELECT 2, 552 UNION ALL SELECT 2, 554 UNION ALL SELECT 2, 555
) g
WHERE EXISTS (SELECT 1 FROM sys_role r WHERE r.id = g.role_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = g.role_id AND x.menu_id = g.menu_id);

-- ===== 第四轮：订单增强（补充） =====
ALTER TABLE ecom_order_setting ADD COLUMN track_api_url VARCHAR(255) DEFAULT NULL COMMENT '17TRACK 自定义接口地址（代理或测试），为空用默认地址';

-- ===== 第四轮：运营监控与团队 =====
-- 店铺授权（店铺数据权限）、ASIN / 商品监控（快照、预警）、促销管理、运营目标与绩效

CREATE TABLE ecom_store_member (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    store_id    BIGINT       NOT NULL COMMENT '店铺',
    user_id     BIGINT       NOT NULL COMMENT '被授权的用户',
    member_role VARCHAR(16)  NOT NULL DEFAULT 'OPERATOR' COMMENT '岗位(业务字典 ecom_store_member_role) OPERATOR运营 CS客服 ASSISTANT助理',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                DEFAULT NULL COMMENT '授权人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_store_member (store_id, user_id, tenant_id),
    KEY idx_ecom_store_member_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '店铺授权：用户只能看到负责的店铺和被授权店铺的电商数据（全部数据权限的角色不受限制）';

CREATE TABLE ecom_monitor (
    id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    monitor_type       VARCHAR(16)   NOT NULL DEFAULT 'OWN' COMMENT '类型(字典 ecom_monitor_type) OWN自己的商品 COMPETITOR竞品',
    platform           VARCHAR(16)   NOT NULL COMMENT '平台(字典 ecom_platform)',
    marketplace        VARCHAR(16)   NOT NULL COMMENT '站点(业务字典 ecom_marketplace)',
    item_id            VARCHAR(64)   NOT NULL COMMENT 'ASIN / 平台商品 ID',
    store_id           BIGINT                 DEFAULT NULL COMMENT '所属店铺（自己的商品必填；竞品可为空，不受店铺数据权限限制）',
    listing_id         BIGINT                 DEFAULT NULL COMMENT '对应的在线商品（自己的商品）',
    seller_sku         VARCHAR(64)            DEFAULT NULL COMMENT '平台 SKU（自己的商品）',
    benchmark_id       BIGINT                 DEFAULT NULL COMMENT '竞品对标的自己商品（监控 ID）',
    brand              VARCHAR(128)           DEFAULT NULL COMMENT '品牌',
    title              VARCHAR(500)           DEFAULT NULL COMMENT '最新标题',
    main_image         VARCHAR(500)           DEFAULT NULL COMMENT '最新主图',
    category           VARCHAR(128)           DEFAULT NULL COMMENT 'BSR 大类',
    rank_main          INT                    DEFAULT NULL COMMENT '大类排名',
    sub_category       VARCHAR(128)           DEFAULT NULL COMMENT 'BSR 小类',
    rank_sub           INT                    DEFAULT NULL COMMENT '小类排名',
    rating             DECIMAL(3, 2)          DEFAULT NULL COMMENT '评分',
    review_count       INT                    DEFAULT NULL COMMENT '评论数',
    seller_count       INT                    DEFAULT NULL COMMENT '在售卖家数（含自己），大于 1 表示有跟卖',
    price              DECIMAL(18, 2)         DEFAULT NULL COMMENT '售价（Buy Box / 最低价）',
    currency           VARCHAR(8)             DEFAULT NULL COMMENT '币种',
    listing_status     VARCHAR(16)            DEFAULT NULL COMMENT '上下架(字典 ecom_listing_status) ACTIVE在售 INACTIVE下架',
    last_snapshot_date DATE                   DEFAULT NULL COMMENT '最近一次快照日期',
    auto_fetch         TINYINT       NOT NULL DEFAULT 1 COMMENT '每天自动通过接口取数 1是 0否（没有接口的平台只能手工录入 / 导入）',
    last_fetch_time    DATETIME               DEFAULT NULL COMMENT '最近一次接口取数时间',
    last_fetch_message VARCHAR(500)           DEFAULT NULL COMMENT '最近一次接口取数失败的原因，成功为空',
    rank_drop_percent  DECIMAL(6, 2) NOT NULL DEFAULT 30 COMMENT '预警：大类排名比上一次快照变差超过该百分比',
    rating_drop        DECIMAL(3, 2) NOT NULL DEFAULT 0.1 COMMENT '预警：评分比上一次快照下降达到该值',
    notify_user_id     BIGINT                 DEFAULT NULL COMMENT '预警通知人，为空通知店铺负责人（竞品为创建人）',
    status             TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1监控中 0暂停',
    remark             VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_by          BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_monitor_item (platform, marketplace, item_id, tenant_id),
    KEY idx_ecom_monitor_store (store_id),
    KEY idx_ecom_monitor_benchmark (benchmark_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ASIN / 商品监控：自己的商品与竞品的排名、评分、评论、跟卖、标题主图五点、上下架';

CREATE TABLE ecom_monitor_snapshot (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    monitor_id     BIGINT         NOT NULL COMMENT '监控',
    snapshot_date  DATE           NOT NULL COMMENT '日期（每天一条，同一天再次取数覆盖）',
    category       VARCHAR(128)            DEFAULT NULL COMMENT 'BSR 大类',
    rank_main      INT                     DEFAULT NULL COMMENT '大类排名',
    sub_category   VARCHAR(128)            DEFAULT NULL COMMENT 'BSR 小类',
    rank_sub       INT                     DEFAULT NULL COMMENT '小类排名',
    rating         DECIMAL(3, 2)           DEFAULT NULL COMMENT '评分',
    review_count   INT                     DEFAULT NULL COMMENT '评论数',
    seller_count   INT                     DEFAULT NULL COMMENT '在售卖家数',
    price          DECIMAL(18, 2)          DEFAULT NULL COMMENT '售价',
    title          VARCHAR(500)            DEFAULT NULL COMMENT '标题',
    main_image     VARCHAR(500)            DEFAULT NULL COMMENT '主图',
    bullets        VARCHAR(4000)           DEFAULT NULL COMMENT '五点描述（每点一行）',
    listing_status VARCHAR(16)             DEFAULT NULL COMMENT '上下架(字典 ecom_listing_status)',
    source         VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 ecom_monitor_source) API接口 MANUAL手工 IMPORT导入',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_monitor_snapshot (monitor_id, snapshot_date, tenant_id),
    KEY idx_ecom_monitor_snapshot_date (snapshot_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品监控每日快照（趋势图与变化预警的依据）';

CREATE TABLE ecom_monitor_alert (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    monitor_id  BIGINT       NOT NULL COMMENT '监控',
    store_id    BIGINT                DEFAULT NULL COMMENT '所属店铺（取自监控，店铺数据权限用）',
    alert_date  DATE         NOT NULL COMMENT '快照日期',
    alert_type  VARCHAR(16)  NOT NULL COMMENT '预警类型(字典 ecom_monitor_alert_type) RANK_DROP排名大跌 RATING_DROP评分下降 NEW_SELLER新增跟卖 TITLE_CHANGED标题被改 IMAGE_CHANGED主图被改 BULLETS_CHANGED五点被改 DELISTED被下架',
    old_value   VARCHAR(500)          DEFAULT NULL COMMENT '变化前',
    new_value   VARCHAR(500)          DEFAULT NULL COMMENT '变化后',
    status      VARCHAR(16)  NOT NULL DEFAULT 'NEW' COMMENT '状态(字典 ecom_monitor_alert_status) NEW未处理 HANDLED已处理 IGNORED已忽略',
    handler_id  BIGINT                DEFAULT NULL COMMENT '处理人',
    handle_time DATETIME              DEFAULT NULL COMMENT '处理时间',
    handle_note VARCHAR(255)          DEFAULT NULL COMMENT '处理说明',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_monitor_alert (monitor_id, alert_date, alert_type, tenant_id),
    KEY idx_ecom_monitor_alert_status (status, alert_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品监控预警（同一监控同一天同一类型一条）';

CREATE TABLE ecom_promotion (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(128)   NOT NULL COMMENT '活动名称',
    store_id          BIGINT         NOT NULL COMMENT '店铺',
    promo_type        VARCHAR(32)    NOT NULL COMMENT '类型(业务字典 ecom_promo_type)',
    start_time        DATETIME       NOT NULL COMMENT '开始时间',
    end_time          DATETIME       NOT NULL COMMENT '结束时间',
    discount_percent  DECIMAL(6, 2)           DEFAULT NULL COMMENT '默认折扣（% off），明细未填促销价时按它计算',
    threshold_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减门槛（店铺币种）',
    reduce_amount     DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减金额 / 优惠券面额（店铺币种）',
    currency          VARCHAR(8)     NOT NULL COMMENT '币种（店铺币种）',
    fee_amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '活动费用（店铺币种）：秒杀费、优惠券费等，计入活动利润',
    fee_base          DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '活动费用（本位币，按开始日所在月汇率）',
    budget            DECIMAL(18, 2)          DEFAULT NULL COMMENT '预算（店铺币种）',
    status            VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_promo_status) DRAFT草稿 ACTIVE已生效 CANCELLED已取消；进行阶段按时间计算',
    push_status       VARCHAR(16)    NOT NULL DEFAULT 'NONE' COMMENT '平台推送(字典 ecom_promo_push_status) NONE未推送 PUSHED已推送 FAILED推送失败',
    platform_promo_id VARCHAR(64)             DEFAULT NULL COMMENT '平台活动 ID（虾皮 discount_id、TikTok activity_id）',
    push_message      VARCHAR(500)            DEFAULT NULL COMMENT '最近一次推送失败的原因',
    push_time         DATETIME                DEFAULT NULL COMMENT '最近一次推送时间',
    owner_id          BIGINT                  DEFAULT NULL COMMENT '负责人',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by         BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_promotion_store (store_id, start_time),
    KEY idx_ecom_promotion_time (start_time, end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销活动（优惠券、秒杀、Prime 折扣、虾皮折扣、TikTok 闪购、满减等）';

CREATE TABLE ecom_promotion_item (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    promotion_id     BIGINT         NOT NULL COMMENT '促销活动',
    listing_id       BIGINT                  DEFAULT NULL COMMENT '在线商品',
    seller_sku       VARCHAR(64)    NOT NULL COMMENT '平台 SKU',
    platform_item_id VARCHAR(64)             DEFAULT NULL COMMENT '平台商品 ID',
    parent_item_id   VARCHAR(64)             DEFAULT NULL COMMENT '父商品 ID（虾皮 item_id、TikTok product_id）',
    title            VARCHAR(255)            DEFAULT NULL COMMENT '标题',
    original_price   DECIMAL(18, 2)          DEFAULT NULL COMMENT '原价（店铺币种）',
    promo_price      DECIMAL(18, 2)          DEFAULT NULL COMMENT '促销价（店铺币种）',
    discount_percent DECIMAL(6, 2)           DEFAULT NULL COMMENT '折扣（% off）',
    promo_qty        INT                     DEFAULT NULL COMMENT '活动库存',
    limit_per_buyer  INT                     DEFAULT NULL COMMENT '每人限购',
    sort             INT            NOT NULL DEFAULT 0 COMMENT '行号',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_promotion_item_promo (promotion_id),
    KEY idx_ecom_promotion_item_sku (seller_sku)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销活动的参与 SKU';

CREATE TABLE ecom_ops_target (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    target_month  VARCHAR(7)     NOT NULL COMMENT '月份 yyyy-MM',
    target_type   VARCHAR(16)    NOT NULL COMMENT '对象(字典 ecom_ops_target_type) STORE店铺 USER运营人员',
    ref_id        BIGINT         NOT NULL COMMENT '店铺 ID / 用户 ID',
    store_id      BIGINT                  DEFAULT NULL COMMENT '店铺目标的店铺（运营人员目标为空，店铺数据权限用）',
    sales_target  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售额目标（本位币）',
    profit_target DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '利润目标（本位币）',
    order_target  INT            NOT NULL DEFAULT 0 COMMENT '订单数目标',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by     BIGINT                  DEFAULT NULL COMMENT '设置人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_ops_target (target_month, target_type, ref_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运营月度目标（店铺 / 运营人员的销售额、利润、订单数）';

-- 菜单：电商下的「运营」分组（ASIN 监控、促销管理、运营目标、运营绩效、店铺授权）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(989, 17, '运营', 'M', NULL, NULL, NULL, NULL, 7, 1, 1, NOW(), NOW()),
(540, 989, 'ASIN 监控', 'C', 'asin-monitor', 'ecom/asin-monitor/index', 'ecom:monitor:list', 'EyeOutlined', 1, 1, 1, NOW(), NOW()),
(541, 989, '促销管理', 'C', 'promotion', 'ecom/promotion/index', 'ecom:promotion:list', 'GiftOutlined', 2, 1, 1, NOW(), NOW()),
(542, 989, '运营目标', 'C', 'ops-target', 'ecom/ops-target/index', 'ecom:opsTarget:list', 'AimOutlined', 3, 1, 1, NOW(), NOW()),
(543, 989, '运营绩效', 'C', 'ops-performance', 'ecom/ops-performance/index', 'ecom:opsPerf:list', 'TrophyOutlined', 4, 1, 1, NOW(), NOW()),
(544, 989, '店铺授权', 'C', 'store-auth', 'ecom/store-auth/index', 'ecom:storeAuth:list', 'SafetyCertificateOutlined', 5, 1, 1, NOW(), NOW()),
(5400, 540, '新增监控', 'F', NULL, NULL, 'ecom:monitor:add', NULL, 1, 1, 1, NOW(), NOW()),
(5401, 540, '修改监控', 'F', NULL, NULL, 'ecom:monitor:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5402, 540, '删除监控', 'F', NULL, NULL, 'ecom:monitor:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5403, 540, '接口取数', 'F', NULL, NULL, 'ecom:monitor:fetch', NULL, 4, 1, 1, NOW(), NOW()),
(5404, 540, '录入 / 导入快照', 'F', NULL, NULL, 'ecom:monitor:snapshot', NULL, 5, 1, 1, NOW(), NOW()),
(5405, 540, '处理预警', 'F', NULL, NULL, 'ecom:monitor:alert', NULL, 6, 1, 1, NOW(), NOW()),
(5410, 541, '新增促销', 'F', NULL, NULL, 'ecom:promotion:add', NULL, 1, 1, 1, NOW(), NOW()),
(5411, 541, '修改促销', 'F', NULL, NULL, 'ecom:promotion:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5412, 541, '删除促销', 'F', NULL, NULL, 'ecom:promotion:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5413, 541, '生效 / 取消', 'F', NULL, NULL, 'ecom:promotion:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(5414, 541, '推送平台', 'F', NULL, NULL, 'ecom:promotion:push', NULL, 5, 1, 1, NOW(), NOW()),
(5420, 542, '设置目标', 'F', NULL, NULL, 'ecom:opsTarget:edit', NULL, 1, 1, 1, NOW(), NOW()),
(5440, 544, '修改授权', 'F', NULL, NULL, 'ecom:storeAuth:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 字典：店铺岗位、促销类型（业务字典），其余为系统字典
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(340, '店铺岗位', 'ecom_store_member_role', 'BUSINESS', 1, '店铺授权中用户在店铺的岗位', NOW(), NOW()),
(341, '监控类型', 'ecom_monitor_type', 'SYSTEM', 1, 'ASIN / 商品监控的对象：自己的商品或竞品', NOW(), NOW()),
(342, '快照来源', 'ecom_monitor_source', 'SYSTEM', 1, '商品监控快照的数据来源', NOW(), NOW()),
(343, '监控预警类型', 'ecom_monitor_alert_type', 'SYSTEM', 1, '商品监控的变化预警', NOW(), NOW()),
(344, '预警处理状态', 'ecom_monitor_alert_status', 'SYSTEM', 1, '商品监控预警的处理状态', NOW(), NOW()),
(345, '促销类型', 'ecom_promo_type', 'BUSINESS', 1, '促销活动的类型', NOW(), NOW()),
(346, '促销状态', 'ecom_promo_status', 'SYSTEM', 1, '促销活动的状态', NOW(), NOW()),
(347, '促销阶段', 'ecom_promo_phase', 'SYSTEM', 1, '已生效的促销按时间计算的阶段', NOW(), NOW()),
(348, '促销推送状态', 'ecom_promo_push_status', 'SYSTEM', 1, '促销活动推送到平台的状态', NOW(), NOW()),
(349, '目标对象', 'ecom_ops_target_type', 'SYSTEM', 1, '运营目标的对象', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_store_member_role', '运营', 'OPERATOR', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_store_member_role', '客服', 'CS', 'green', 2, 1, 0, NOW(), NOW()),
('ecom_store_member_role', '助理', 'ASSISTANT', 'default', 3, 1, 0, NOW(), NOW()),
('ecom_monitor_type', '自己的商品', 'OWN', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_monitor_type', '竞品', 'COMPETITOR', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_monitor_source', '接口', 'API', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_monitor_source', '手工', 'MANUAL', 'default', 2, 1, 1, NOW(), NOW()),
('ecom_monitor_source', '导入', 'IMPORT', 'blue', 3, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '排名大跌', 'RANK_DROP', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '评分下降', 'RATING_DROP', 'volcano', 2, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '新增跟卖', 'NEW_SELLER', 'magenta', 3, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '标题被改', 'TITLE_CHANGED', 'orange', 4, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '主图被改', 'IMAGE_CHANGED', 'gold', 5, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '五点被改', 'BULLETS_CHANGED', 'purple', 6, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_type', '被下架', 'DELISTED', 'red', 7, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_status', '未处理', 'NEW', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_status', '已处理', 'HANDLED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_monitor_alert_status', '已忽略', 'IGNORED', 'default', 3, 1, 1, NOW(), NOW()),
('ecom_promo_type', '优惠券', 'COUPON', 'blue', 1, 1, 0, NOW(), NOW()),
('ecom_promo_type', '秒杀 / Lightning Deal', 'LIGHTNING_DEAL', 'red', 2, 1, 0, NOW(), NOW()),
('ecom_promo_type', 'Prime 专享折扣', 'PRIME_DISCOUNT', 'cyan', 3, 1, 0, NOW(), NOW()),
('ecom_promo_type', '虾皮折扣活动', 'SHOPEE_DISCOUNT', 'orange', 4, 1, 0, NOW(), NOW()),
('ecom_promo_type', 'TikTok 闪购', 'TIKTOK_FLASH_SALE', 'magenta', 5, 1, 0, NOW(), NOW()),
('ecom_promo_type', '店铺满减', 'FULL_REDUCTION', 'purple', 6, 1, 0, NOW(), NOW()),
('ecom_promo_type', '直降', 'PRICE_DISCOUNT', 'green', 7, 1, 0, NOW(), NOW()),
('ecom_promo_type', '其他', 'OTHER', 'default', 8, 1, 0, NOW(), NOW()),
('ecom_promo_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_promo_status', '已生效', 'ACTIVE', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_promo_status', '已取消', 'CANCELLED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_promo_phase', '未开始', 'NOT_STARTED', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_promo_phase', '进行中', 'ONGOING', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_promo_phase', '已结束', 'ENDED', 'default', 3, 1, 1, NOW(), NOW()),
('ecom_promo_push_status', '未推送', 'NONE', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_promo_push_status', '已推送', 'PUSHED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_promo_push_status', '推送失败', 'FAILED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_ops_target_type', '店铺', 'STORE', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_ops_target_type', '运营人员', 'USER', 'purple', 2, 1, 1, NOW(), NOW());

-- 内置角色：超级管理员、电商运营（店铺授权页面除外：授权由管理员维护），总经理只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 989 OR m.parent_id = 989 OR m.parent_id BETWEEN 540 AND 544)
WHERE r.id = 1 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 989 OR m.id BETWEEN 540 AND 543 OR m.parent_id BETWEEN 540 AND 543)
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 989 OR m.parent_id = 989)
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
UPDATE sys_role SET remark = '店铺与在线商品、平台订单出库发货、退货退款、头程货件、平台结算与广告、补货建议、运营监控与促销，可查看库存与物料；电商数据只能看到负责和被授权的店铺'
WHERE id = 5;

-- ===== 第四轮：海外仓（owh） =====
-- 第三方海外仓（谷仓、万邑通、4PX 海外仓）：服务商账号、仓库映射、物流产品、库存同步、入库单（ASN）、一件代发出库单、费用

CREATE TABLE ecom_owh_account (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code               VARCHAR(32)  NOT NULL COMMENT '账号编码',
    name               VARCHAR(64)  NOT NULL COMMENT '账号名称',
    provider           VARCHAR(16)  NOT NULL COMMENT '海外仓服务商(字典 ecom_owh_provider) GOODCANG谷仓 WINIT万邑通 FOURPX_FB4递四方海外仓',
    credentials        TEXT                  DEFAULT NULL COMMENT '授权（JSON，CredentialCipher 加密，只写不读）',
    sandbox            TINYINT      NOT NULL DEFAULT 0 COMMENT '测试环境 1是',
    api_base_url       VARCHAR(255)          DEFAULT NULL COMMENT '自定义接口地址（为空按环境取服务商的地址）',
    currency           VARCHAR(8)            DEFAULT NULL COMMENT '费用币种（服务商返回的费用没有币种时使用）',
    status             TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    stock_sync_time    DATETIME              DEFAULT NULL COMMENT '最近一次库存同步时间',
    fee_sync_time      DATETIME              DEFAULT NULL COMMENT '最近一次费用同步时间',
    last_error         VARCHAR(500)          DEFAULT NULL COMMENT '最近一次同步失败的原因',
    remark             VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by          BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_account_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓服务商账号';

CREATE TABLE ecom_owh_warehouse (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id            BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id           BIGINT       NOT NULL COMMENT '海外仓账号',
    warehouse_id         BIGINT       NOT NULL COMMENT '系统仓库（wms_warehouse）',
    owh_code             VARCHAR(64)  NOT NULL COMMENT '服务商仓库代码',
    owh_name             VARCHAR(128)          DEFAULT NULL COMMENT '服务商仓库名称',
    country              VARCHAR(8)            DEFAULT NULL COMMENT '仓库所在国家 / 地区',
    default_product_code VARCHAR(64)           DEFAULT NULL COMMENT '默认物流产品代码（出库单没有指定、物流渠道也没有对应产品时使用）',
    auto_push            TINYINT      NOT NULL DEFAULT 0 COMMENT '审核后自动推送出库单 1是（定时任务推送待推送的包裹）',
    status               TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time          DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time          DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_warehouse_wh (warehouse_id, tenant_id),
    KEY idx_ecom_owh_warehouse_account (account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓仓库映射（系统仓库 → 服务商仓库代码）';

CREATE TABLE ecom_owh_product (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id     BIGINT       NOT NULL COMMENT '海外仓账号',
    product_code   VARCHAR(64)  NOT NULL COMMENT '服务商物流产品代码',
    product_name   VARCHAR(128)          DEFAULT NULL COMMENT '物流产品名称',
    owh_code       VARCHAR(64)           DEFAULT NULL COMMENT '适用的服务商仓库代码（为空表示通用）',
    carrier_name   VARCHAR(64)           DEFAULT NULL COMMENT '承运商（回传平台用，为空取产品名称）',
    channel_id     BIGINT                DEFAULT NULL COMMENT '对应的物流渠道（审单规则指定该渠道时使用此产品）',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（服务商列表中已没有的停用）',
    sync_time      DATETIME              DEFAULT NULL COMMENT '最近一次从服务商拉取的时间',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_product (account_id, product_code, tenant_id),
    KEY idx_ecom_owh_product_channel (channel_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓物流产品（从服务商拉取）';

CREATE TABLE ecom_owh_stock (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    mapping_id     BIGINT         NOT NULL COMMENT '仓库映射',
    warehouse_id   BIGINT         NOT NULL COMMENT '系统仓库',
    sku            VARCHAR(64)    NOT NULL COMMENT '海外仓 SKU（与物料编码相同）',
    material_id    BIGINT                  DEFAULT NULL COMMENT '物料（按编码匹配，匹配不到为空）',
    available_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '可用',
    transit_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '在途（已发往海外仓未上架）',
    locked_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '锁定（待出库占用）',
    sync_time      DATETIME                DEFAULT NULL COMMENT '同步时间',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_stock (mapping_id, sku, tenant_id),
    KEY idx_ecom_owh_stock_material (warehouse_id, material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓库存（最近一次同步）';

CREATE TABLE ecom_owh_inbound (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_no      VARCHAR(32)    NOT NULL COMMENT '入库单号（编码规则 OWI），也是向服务商提交的参考号',
    account_id      BIGINT         NOT NULL COMMENT '海外仓账号',
    mapping_id      BIGINT         NOT NULL COMMENT '仓库映射',
    warehouse_id    BIGINT         NOT NULL COMMENT '系统仓库（调入仓）',
    source_type     VARCHAR(16)    NOT NULL COMMENT '来源(字典 ecom_owh_inbound_source) INBOUND头程货件 TRANSFER调拨单',
    source_id       BIGINT         NOT NULL COMMENT '来源单据 ID',
    source_no       VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    owh_order_no    VARCHAR(64)             DEFAULT NULL COMMENT '服务商入库单号',
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_owh_inbound_status) DRAFT草稿 PUSHED已推送 RECEIVING收货中 RECEIVED已收货 CANCELLED已取消',
    owh_status      VARCHAR(64)             DEFAULT NULL COMMENT '服务商返回的原始状态',
    transport_mode  VARCHAR(16)             DEFAULT NULL COMMENT '运输方式(业务字典 ecom_transport_mode)',
    carrier         VARCHAR(64)             DEFAULT NULL COMMENT '头程承运商',
    tracking_no     VARCHAR(64)             DEFAULT NULL COMMENT '头程运单号',
    eta_date        DATE                    DEFAULT NULL COMMENT '预计到仓',
    box_count       INT            NOT NULL DEFAULT 0 COMMENT '箱数',
    total_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量合计',
    received_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已收货数量合计',
    push_time       DATETIME                DEFAULT NULL COMMENT '推送时间',
    receive_time    DATETIME                DEFAULT NULL COMMENT '收货完成时间',
    erp_received    TINYINT        NOT NULL DEFAULT 0 COMMENT 'ERP 已签收（在途转可用）1是',
    sync_time       DATETIME                DEFAULT NULL COMMENT '最近一次同步时间',
    error_msg       VARCHAR(500)            DEFAULT NULL COMMENT '最近一次推送 / 同步 / 签收失败的原因',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_inbound_no (inbound_no, tenant_id),
    KEY idx_ecom_owh_inbound_source (source_type, source_id),
    KEY idx_ecom_owh_inbound_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓入库单（ASN，头程到海外仓）';

CREATE TABLE ecom_owh_inbound_box (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_id   BIGINT         NOT NULL COMMENT '海外仓入库单',
    box_no       INT            NOT NULL COMMENT '箱号（从 1 开始）',
    box_mark     VARCHAR(64)    NOT NULL COMMENT '箱唛号（入库单号-箱号）',
    length       DECIMAL(10, 2)          DEFAULT NULL COMMENT '长（cm）',
    width        DECIMAL(10, 2)          DEFAULT NULL COMMENT '宽（cm）',
    height       DECIMAL(10, 2)          DEFAULT NULL COMMENT '高（cm）',
    weight       DECIMAL(10, 3)          DEFAULT NULL COMMENT '毛重（kg）',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_owh_inbound_box (inbound_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓入库单的箱子（箱唛）';

CREATE TABLE ecom_owh_inbound_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inbound_id    BIGINT         NOT NULL COMMENT '海外仓入库单',
    box_no        INT            NOT NULL DEFAULT 1 COMMENT '箱号',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    sku           VARCHAR(64)    NOT NULL COMMENT '海外仓 SKU（物料编码）',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '数量',
    received_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已收货数量（同步自服务商，同一 SKU 多箱时记在第一行）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_owh_inbound_item (inbound_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓入库单明细（箱 × SKU）';

CREATE TABLE ecom_owh_outbound (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    outbound_no     VARCHAR(32)    NOT NULL COMMENT '出库单号（编码规则 OWO）',
    package_id      BIGINT         NOT NULL COMMENT '包裹',
    store_id        BIGINT         NOT NULL COMMENT '店铺',
    account_id      BIGINT         NOT NULL COMMENT '海外仓账号',
    mapping_id      BIGINT         NOT NULL COMMENT '仓库映射',
    warehouse_id    BIGINT         NOT NULL COMMENT '系统仓库（发货仓）',
    product_code    VARCHAR(64)             DEFAULT NULL COMMENT '物流产品代码',
    owh_order_no    VARCHAR(64)             DEFAULT NULL COMMENT '服务商出库单号',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 ecom_owh_outbound_status) FAILED推送失败 PUSHED已推送 INTERCEPTING截单中 OUTBOUND已出库 SHIPPED已发货 CANCELLED已取消',
    owh_status      VARCHAR(64)             DEFAULT NULL COMMENT '服务商返回的原始状态',
    carrier_name    VARCHAR(64)             DEFAULT NULL COMMENT '承运商',
    tracking_no     VARCHAR(64)             DEFAULT NULL COMMENT '运单号',
    estimate_fee    DECIMAL(18, 2)          DEFAULT NULL COMMENT '服务商预估费用（原币）',
    fee_currency    VARCHAR(8)              DEFAULT NULL COMMENT '预估费用币种',
    push_time       DATETIME                DEFAULT NULL COMMENT '推送时间',
    outbound_time   DATETIME                DEFAULT NULL COMMENT '海外仓出库时间',
    ship_time       DATETIME                DEFAULT NULL COMMENT 'ERP 发货（出库、回传平台）时间',
    sync_time       DATETIME                DEFAULT NULL COMMENT '最近一次同步时间',
    push_count      INT            NOT NULL DEFAULT 0 COMMENT '推送次数',
    error_msg       VARCHAR(500)            DEFAULT NULL COMMENT '最近一次推送 / 同步 / 发货失败的原因',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_outbound_no (outbound_no, tenant_id),
    KEY idx_ecom_owh_outbound_package (package_id),
    KEY idx_ecom_owh_outbound_status (status),
    KEY idx_ecom_owh_outbound_owh_no (owh_order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓出库单（一件代发）';

CREATE TABLE ecom_owh_fee (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id     BIGINT         NOT NULL COMMENT '海外仓账号',
    external_id    VARCHAR(128)   NOT NULL COMMENT '服务商费用流水号（去重）',
    fee_type       VARCHAR(16)    NOT NULL COMMENT '费用类型(字典 ecom_owh_fee_type) STORAGE仓租 OPERATION操作费 FREIGHT尾程运费 INBOUND入库费 OTHER其他',
    fee_date       DATE           NOT NULL COMMENT '费用日期',
    ref_no         VARCHAR(64)             DEFAULT NULL COMMENT '关联单号（服务商出库 / 入库单号或参考号）',
    owh_code       VARCHAR(64)             DEFAULT NULL COMMENT '服务商仓库代码',
    warehouse_id   BIGINT                  DEFAULT NULL COMMENT '系统仓库',
    sku            VARCHAR(64)             DEFAULT NULL COMMENT 'SKU',
    material_id    BIGINT                  DEFAULT NULL COMMENT '物料',
    outbound_id    BIGINT                  DEFAULT NULL COMMENT '关联的海外仓出库单',
    inbound_id     BIGINT                  DEFAULT NULL COMMENT '关联的海外仓入库单',
    currency       VARCHAR(8)     NOT NULL COMMENT '币种',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '金额（原币，正数）',
    exchange_rate  DECIMAL(18, 6)          DEFAULT NULL COMMENT '汇率（费用日期所在月）',
    amount_base    DECIMAL(18, 2)          DEFAULT NULL COMMENT '金额（本位币）',
    status         VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '入账状态(字典 ecom_owh_fee_status) PENDING待入账 POSTED已入账 UNALLOCATED无法归属',
    post_target    VARCHAR(255)            DEFAULT NULL COMMENT '入账去向说明（订单号 / SKU）',
    post_time      DATETIME                DEFAULT NULL COMMENT '入账时间',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注（服务商的费用说明）',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_fee (account_id, external_id, tenant_id),
    KEY idx_ecom_owh_fee_date (fee_date),
    KEY idx_ecom_owh_fee_outbound (outbound_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓费用明细（仓租、操作费、尾程运费等）';

CREATE TABLE ecom_owh_fee_alloc (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    fee_id        BIGINT         NOT NULL COMMENT '海外仓费用',
    store_id      BIGINT         NOT NULL COMMENT '店铺',
    seller_sku    VARCHAR(64)             DEFAULT NULL COMMENT '平台 SKU（为空表示计入店铺）',
    order_id      BIGINT                  DEFAULT NULL COMMENT '订单（出库费用计入订单物流成本时）',
    item_type     VARCHAR(16)    NOT NULL COMMENT '利润报表费用类型(字典 ecom_settlement_item_type)：仓租 STORAGE、入库费 INBOUND_FEE、其他 OTHER',
    alloc_date    DATE           NOT NULL COMMENT '费用日期',
    basis_value   DECIMAL(18, 4)          DEFAULT NULL COMMENT '分摊依据（销量）',
    amount_base   DECIMAL(18, 2) NOT NULL COMMENT '金额（本位币，费用为负；计入订单的为正数的物流成本）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_owh_fee_alloc_fee (fee_id),
    KEY idx_ecom_owh_fee_alloc_date (alloc_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓费用入账明细（计入订单物流成本或分摊到店铺 SKU）';

-- 菜单：电商下的「海外仓」分组（海外仓账号、海外仓库存、海外仓入库、海外仓出库、海外仓费用）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(988, 17, '海外仓', 'M', NULL, NULL, NULL, NULL, 7, 1, 1, NOW(), NOW()),
(520, 988, '海外仓账号', 'C', 'owh-account', 'ecom/owh-account/index', 'ecom:owhAccount:list', 'CloudServerOutlined', 1, 1, 1, NOW(), NOW()),
(521, 988, '海外仓库存', 'C', 'owh-stock', 'ecom/owh-stock/index', 'ecom:owhStock:list', 'DatabaseOutlined', 2, 1, 1, NOW(), NOW()),
(522, 988, '海外仓入库', 'C', 'owh-inbound', 'ecom/owh-inbound/index', 'ecom:owhInbound:list', 'ImportOutlined', 3, 1, 1, NOW(), NOW()),
(523, 988, '海外仓出库', 'C', 'owh-outbound', 'ecom/owh-outbound/index', 'ecom:owhOutbound:list', 'ExportOutlined', 4, 1, 1, NOW(), NOW()),
(524, 988, '海外仓费用', 'C', 'owh-fee', 'ecom/owh-fee/index', 'ecom:owhFee:list', 'AccountBookOutlined', 5, 1, 1, NOW(), NOW()),
(5200, 520, '账号新增', 'F', NULL, NULL, 'ecom:owhAccount:add', NULL, 1, 1, 1, NOW(), NOW()),
(5201, 520, '账号修改', 'F', NULL, NULL, 'ecom:owhAccount:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5202, 520, '账号删除', 'F', NULL, NULL, 'ecom:owhAccount:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5203, 520, '仓库映射与物流产品', 'F', NULL, NULL, 'ecom:owhAccount:mapping', NULL, 4, 1, 1, NOW(), NOW()),
(5210, 521, '同步库存', 'F', NULL, NULL, 'ecom:owhStock:sync', NULL, 1, 1, 1, NOW(), NOW()),
(5211, 521, '按海外仓调整库存', 'F', NULL, NULL, 'ecom:owhStock:adjust', NULL, 2, 1, 1, NOW(), NOW()),
(5220, 522, '新建入库单', 'F', NULL, NULL, 'ecom:owhInbound:add', NULL, 1, 1, 1, NOW(), NOW()),
(5221, 522, '推送入库单', 'F', NULL, NULL, 'ecom:owhInbound:push', NULL, 2, 1, 1, NOW(), NOW()),
(5222, 522, '同步收货', 'F', NULL, NULL, 'ecom:owhInbound:sync', NULL, 3, 1, 1, NOW(), NOW()),
(5223, 522, '取消入库单', 'F', NULL, NULL, 'ecom:owhInbound:cancel', NULL, 4, 1, 1, NOW(), NOW()),
(5230, 523, '推送海外仓', 'F', NULL, NULL, 'ecom:owhOutbound:push', NULL, 1, 1, 1, NOW(), NOW()),
(5231, 523, '同步出库状态', 'F', NULL, NULL, 'ecom:owhOutbound:sync', NULL, 2, 1, 1, NOW(), NOW()),
(5232, 523, '取消 / 截单', 'F', NULL, NULL, 'ecom:owhOutbound:cancel', NULL, 3, 1, 1, NOW(), NOW()),
(5240, 524, '同步费用', 'F', NULL, NULL, 'ecom:owhFee:sync', NULL, 1, 1, 1, NOW(), NOW());

-- 字典：海外仓服务商、入库单状态、出库单状态、费用类型、入库来源、费用入账状态（系统字典）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(320, '海外仓服务商', 'ecom_owh_provider', 'SYSTEM', 1, '已对接接口的第三方海外仓', NOW(), NOW()),
(321, '海外仓入库单状态', 'ecom_owh_inbound_status', 'SYSTEM', 1, '海外仓入库单（ASN）的状态', NOW(), NOW()),
(322, '海外仓出库单状态', 'ecom_owh_outbound_status', 'SYSTEM', 1, '一件代发出库单的状态', NOW(), NOW()),
(323, '海外仓费用类型', 'ecom_owh_fee_type', 'SYSTEM', 1, '海外仓账单的费用类型', NOW(), NOW()),
(324, '海外仓入库来源', 'ecom_owh_inbound_source', 'SYSTEM', 1, '海外仓入库单的来源单据', NOW(), NOW()),
(325, '海外仓费用入账状态', 'ecom_owh_fee_status', 'SYSTEM', 1, '海外仓费用计入订单 / SKU 的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_owh_provider', '谷仓', 'GOODCANG', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_owh_provider', '万邑通', 'WINIT', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_owh_provider', '递四方海外仓', 'FOURPX_FB4', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_status', '已推送', 'PUSHED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_status', '收货中', 'RECEIVING', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_status', '已收货', 'RECEIVED', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_status', '已取消', 'CANCELLED', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_owh_outbound_status', '推送失败', 'FAILED', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_owh_outbound_status', '已推送', 'PUSHED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_owh_outbound_status', '截单中', 'INTERCEPTING', 'gold', 3, 1, 1, NOW(), NOW()),
('ecom_owh_outbound_status', '已出库', 'OUTBOUND', 'cyan', 4, 1, 1, NOW(), NOW()),
('ecom_owh_outbound_status', '已发货', 'SHIPPED', 'green', 5, 1, 1, NOW(), NOW()),
('ecom_owh_outbound_status', '已取消', 'CANCELLED', 'default', 6, 1, 1, NOW(), NOW()),
('ecom_owh_fee_type', '仓租', 'STORAGE', 'purple', 1, 1, 1, NOW(), NOW()),
('ecom_owh_fee_type', '操作费', 'OPERATION', 'blue', 2, 1, 1, NOW(), NOW()),
('ecom_owh_fee_type', '尾程运费', 'FREIGHT', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_owh_fee_type', '入库费', 'INBOUND', 'cyan', 4, 1, 1, NOW(), NOW()),
('ecom_owh_fee_type', '其他', 'OTHER', 'default', 5, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_source', '头程货件', 'INBOUND', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_owh_inbound_source', '调拨单', 'TRANSFER', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_owh_fee_status', '待入账', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_owh_fee_status', '已入账', 'POSTED', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_owh_fee_status', '无法归属', 'UNALLOCATED', 'red', 3, 1, 1, NOW(), NOW());

-- 内置角色：电商运营全部；总经理只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 988 OR m.parent_id = 988 OR m.parent_id BETWEEN 520 AND 529)
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id = 988 OR m.parent_id = 988)
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第四轮：演示数据 =====
-- 电商看板没有数据时的「生成演示数据」按钮（接口 POST /ecom/demo/generate）：超级管理员、电商运营
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(5700, 420, '生成演示数据', 'F', NULL, NULL, 'ecom:demo:generate', NULL, 90, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 5700 FROM sys_role r
WHERE r.id IN (1, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 5700);
-- 退货原因补充（业务字典，平台租户；其他租户生成演示数据时按需补充）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_return_reason', '尺寸 / 规格不合适', 'SIZE', 'cyan', 8, 1, 0, NOW(), NOW()),
('ecom_return_reason', '未收到货', 'NOT_RECEIVED', 'magenta', 9, 1, 0, NOW(), NOW());

-- ===== 第四轮：遗留补齐、AI 与看板 =====
-- 销量预测排除断货日：断货阈值（快照可售数不大于它为断货日）、是否排除；预测汇总记录断货天数与排除后的日均
ALTER TABLE ecom_forecast_setting ADD COLUMN exclude_stockout TINYINT NOT NULL DEFAULT 1 COMMENT '排除断货日 1是 0否（预测补偿断货日销量，补货加权日均不计断货日）';
ALTER TABLE ecom_forecast_setting ADD COLUMN stockout_threshold DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '断货阈值：快照可售数不大于它记为断货日';
ALTER TABLE ecom_forecast_sku ADD COLUMN stockout_days INT NOT NULL DEFAULT 0 COMMENT '最近 120 天中的断货天数';
ALTER TABLE ecom_forecast_sku ADD COLUMN adjusted_daily DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '最近 28 天断货日补偿后的日均';
-- AI 写作（业务模块经 AiTextGenerator 生成文本：客服回复草拟与翻译、评价回复、刊登文案）
ALTER TABLE ai_config ADD COLUMN write_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'AI 写作 1开启 0关闭';
-- 按钮：买家消息「AI 草拟 / 翻译」、评价「AI 草拟回复 / 翻译」（会消耗模型用量，单独授权）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(5600, 485, 'AI 草拟 / 翻译', 'F', NULL, NULL, 'ecom:csMessage:ai', NULL, 10, 1, 1, NOW(), NOW()),
(5601, 486, 'AI 草拟回复 / 翻译', 'F', NULL, NULL, 'ecom:csReview:ai', NULL, 10, 1, 1, NOW(), NOW());
-- 内置角色：超级管理员、电商运营
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (5600, 5601)
WHERE r.id IN (1, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第四轮：刊登与定价（补充） =====
-- 在线商品：手工库存（锁定）标记，批量改库存时可设置，自动库存回传（ecom.inventoryPush）跳过锁定的商品
ALTER TABLE ecom_listing ADD COLUMN stock_locked TINYINT NOT NULL DEFAULT 0 COMMENT '手工库存（锁定）1是：自动库存回传跳过，解除锁定后恢复按可售数回传';

-- ===== 第四轮：FBA 运营（补充：欧洲 / 日本站内置费率） =====
-- 尺寸分段增加「三边合计上限」（日本站按三辺合計分段）；分段的重量上限按单件实重判断
ALTER TABLE ecom_fba_size_tier ADD COLUMN max_sides_sum DECIMAL(10, 2) DEFAULT NULL COMMENT '三边合计上限（cm / 英寸），为空不限';
-- 德国（DE Only 列，参加中欧计划 CEP 时每件少 0.26 欧元）、英国：亚马逊 2025 年欧洲 FBA 费率表（2025-02-01 起，标准 FBA，非服装、非危险品、非低价 FBA）；
-- 日本：2025 年 4 月改定后的配送代行手数料（售价 1,000 日元以上一档）与在庫保管手数料 / 長期在庫追加手数料。特殊大件、服装、危险品、低价商品等附加规则未内置
INSERT INTO ecom_fba_size_tier (marketplace, code, name, size_category, unit_system, max_long, max_median, max_short, max_girth, max_sides_sum, max_weight, dim_divisor, sort, builtin, create_time, update_time) VALUES
('DE', 'LIGHT_ENV', '轻型信封', 'STANDARD', 'METRIC', 33, 23, 2.5, NULL, NULL, 0.1, NULL, 1, 1, NOW(), NOW()),
('DE', 'STD_ENV', '标准信封', 'STANDARD', 'METRIC', 33, 23, 2.5, NULL, NULL, 0.46, NULL, 2, 1, NOW(), NOW()),
('DE', 'LARGE_ENV', '大号信封', 'STANDARD', 'METRIC', 33, 23, 4, NULL, NULL, 0.96, NULL, 3, 1, NOW(), NOW()),
('DE', 'XL_ENV', '超大号信封', 'STANDARD', 'METRIC', 33, 23, 6, NULL, NULL, 0.96, NULL, 4, 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', '小包裹', 'STANDARD', 'METRIC', 35, 25, 12, NULL, NULL, 3.9, 5000, 5, 1, NOW(), NOW()),
('DE', 'STD_PARCEL', '标准包裹', 'STANDARD', 'METRIC', 45, 34, 26, NULL, NULL, 11.9, 5000, 6, 1, NOW(), NOW()),
('DE', 'SMALL_OVER', '小号大件', 'OVERSIZE', 'METRIC', 61, 46, 46, NULL, NULL, 1.76, 5000, 7, 1, NOW(), NOW()),
('DE', 'STD_OVER_LIGHT', '标准大件（轻）', 'OVERSIZE', 'METRIC', 101, 60, 60, NULL, NULL, 15, 5000, 8, 1, NOW(), NOW()),
('DE', 'STD_OVER_HEAVY', '标准大件（重）', 'OVERSIZE', 'METRIC', 101, 60, 60, NULL, NULL, 23, 5000, 9, 1, NOW(), NOW()),
('DE', 'STD_OVER_LARGE', '标准大件（大）', 'OVERSIZE', 'METRIC', 120, 60, 60, NULL, NULL, 23, 5000, 10, 1, NOW(), NOW()),
('DE', 'BULKY_OVER', '超大件', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, NULL, 23, 5000, 11, 1, NOW(), NOW()),
('DE', 'HEAVY_OVER', '重型大件', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, NULL, 31.5, 5000, 12, 1, NOW(), NOW()),
('UK', 'LIGHT_ENV', '轻型信封', 'STANDARD', 'METRIC', 33, 23, 2.5, NULL, NULL, 0.1, NULL, 1, 1, NOW(), NOW()),
('UK', 'STD_ENV', '标准信封', 'STANDARD', 'METRIC', 33, 23, 2.5, NULL, NULL, 0.46, NULL, 2, 1, NOW(), NOW()),
('UK', 'LARGE_ENV', '大号信封', 'STANDARD', 'METRIC', 33, 23, 4, NULL, NULL, 0.96, NULL, 3, 1, NOW(), NOW()),
('UK', 'XL_ENV', '超大号信封', 'STANDARD', 'METRIC', 33, 23, 6, NULL, NULL, 0.96, NULL, 4, 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', '小包裹', 'STANDARD', 'METRIC', 35, 25, 12, NULL, NULL, 3.9, 5000, 5, 1, NOW(), NOW()),
('UK', 'STD_PARCEL', '标准包裹', 'STANDARD', 'METRIC', 45, 34, 26, NULL, NULL, 11.9, 5000, 6, 1, NOW(), NOW()),
('UK', 'SMALL_OVER', '小号大件', 'OVERSIZE', 'METRIC', 61, 46, 46, NULL, NULL, 1.76, 5000, 7, 1, NOW(), NOW()),
('UK', 'STD_OVER_LIGHT', '标准大件（轻）', 'OVERSIZE', 'METRIC', 101, 60, 60, NULL, NULL, 15, 5000, 8, 1, NOW(), NOW()),
('UK', 'STD_OVER_HEAVY', '标准大件（重）', 'OVERSIZE', 'METRIC', 101, 60, 60, NULL, NULL, 23, 5000, 9, 1, NOW(), NOW()),
('UK', 'STD_OVER_LARGE', '标准大件（大）', 'OVERSIZE', 'METRIC', 120, 60, 60, NULL, NULL, 23, 5000, 10, 1, NOW(), NOW()),
('UK', 'BULKY_OVER', '超大件', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, NULL, 23, 5000, 11, 1, NOW(), NOW()),
('UK', 'HEAVY_OVER', '重型大件', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, NULL, 31.5, 5000, 12, 1, NOW(), NOW()),
('JP', 'SMALL', '小型', 'STANDARD', 'METRIC', 25, 18, 2.0, NULL, NULL, 0.25, NULL, 1, 1, NOW(), NOW()),
('JP', 'STD1', '标准 1', 'STANDARD', 'METRIC', 35, 30, 3.3, NULL, NULL, 1, NULL, 2, 1, NOW(), NOW()),
('JP', 'STD2A', '标准 2a', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 20, 2, NULL, 3, 1, NOW(), NOW()),
('JP', 'STD2B', '标准 2b', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 30, 2, NULL, 4, 1, NOW(), NOW()),
('JP', 'STD2C', '标准 2c', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 40, 2, NULL, 5, 1, NOW(), NOW()),
('JP', 'STD2D', '标准 2d', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 50, 2, NULL, 6, 1, NOW(), NOW()),
('JP', 'STD2E', '标准 2e', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 60, 2, NULL, 7, 1, NOW(), NOW()),
('JP', 'STD3', '标准 3', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 80, 5, NULL, 8, 1, NOW(), NOW()),
('JP', 'STD4', '标准 4', 'STANDARD', 'METRIC', 45, 35, 20, NULL, 100, 9, NULL, 9, 1, NOW(), NOW()),
('JP', 'LARGE1', '大型 1', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 60, 2, NULL, 10, 1, NOW(), NOW()),
('JP', 'LARGE2', '大型 2', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 80, 5, NULL, 11, 1, NOW(), NOW()),
('JP', 'LARGE3', '大型 3', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 100, 10, NULL, 12, 1, NOW(), NOW()),
('JP', 'LARGE4', '大型 4', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 120, 15, NULL, 13, 1, NOW(), NOW()),
('JP', 'LARGE5', '大型 5', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 140, 20, NULL, 14, 1, NOW(), NOW()),
('JP', 'LARGE6', '大型 6', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 160, 25, NULL, 15, 1, NOW(), NOW()),
('JP', 'LARGE7', '大型 7', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 180, 30, NULL, 16, 1, NOW(), NOW()),
('JP', 'LARGE8', '大型 8', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 200, 40, NULL, 17, 1, NOW(), NOW()),
('JP', 'XL1', '特大型 1', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 200, 50, NULL, 18, 1, NOW(), NOW()),
('JP', 'XL2', '特大型 2', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 220, 50, NULL, 19, 1, NOW(), NOW()),
('JP', 'XL3', '特大型 3', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 240, 50, NULL, 20, 1, NOW(), NOW()),
('JP', 'XL4A', '特大型 4a', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 260, 50, NULL, 21, 1, NOW(), NOW()),
('JP', 'XL4B', '特大型 4b', 'OVERSIZE', 'METRIC', NULL, NULL, NULL, NULL, 400, 50, NULL, 22, 1, NOW(), NOW());
INSERT INTO ecom_fba_fee_rate (marketplace, tier_code, weight_from, weight_to, fee, extra_from, extra_unit, extra_fee, currency, builtin, create_time, update_time) VALUES
('DE', 'LIGHT_ENV', 0, 0.02, 2.33, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'LIGHT_ENV', 0.02, 0.04, 2.37, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'LIGHT_ENV', 0.04, 0.06, 2.39, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'LIGHT_ENV', 0.06, 0.08, 2.52, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'LIGHT_ENV', 0.08, 0.1, 2.54, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_ENV', 0, 0.21, 2.57, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_ENV', 0.21, 0.46, 2.68, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'LARGE_ENV', 0, 0.96, 3.04, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'XL_ENV', 0, 0.96, 3.42, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', 0, 0.15, 3.38, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', 0.15, 0.4, 3.4, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', 0.4, 0.9, 3.67, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', 0.9, 1.4, 4.29, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', 1.4, 1.9, 4.49, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_PARCEL', 1.9, 3.9, 5.57, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 0, 0.15, 3.39, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 0.15, 0.4, 3.78, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 0.4, 0.9, 3.9, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 0.9, 1.4, 4.54, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 1.4, 1.9, 4.97, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 1.9, 2.9, 5.2, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 2.9, 3.9, 5.67, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 3.9, 5.9, 5.95, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 5.9, 8.9, 6.41, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_PARCEL', 8.9, 11.9, 6.65, NULL, NULL, NULL, 'EUR', 1, NOW(), NOW()),
('DE', 'SMALL_OVER', 0, NULL, 4.79, 0.76, 1, 0.48, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_OVER_LIGHT', 0, NULL, 4.91, 0.76, 1, 0.29, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_OVER_HEAVY', 0, NULL, 9.19, 15.76, 1, 0.14, 'EUR', 1, NOW(), NOW()),
('DE', 'STD_OVER_LARGE', 0, NULL, 6.67, 0.76, 1, 0.18, 'EUR', 1, NOW(), NOW()),
('DE', 'BULKY_OVER', 0, NULL, 9.78, 0.76, 1, 0.36, 'EUR', 1, NOW(), NOW()),
('DE', 'HEAVY_OVER', 0, NULL, 13.0, 31.5, 1, 0.15, 'EUR', 1, NOW(), NOW()),
('UK', 'LIGHT_ENV', 0, 0.02, 1.83, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'LIGHT_ENV', 0.02, 0.04, 1.87, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'LIGHT_ENV', 0.04, 0.06, 1.89, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'LIGHT_ENV', 0.06, 0.08, 2.07, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'LIGHT_ENV', 0.08, 0.1, 2.08, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_ENV', 0, 0.21, 2.1, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_ENV', 0.21, 0.46, 2.16, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'LARGE_ENV', 0, 0.96, 2.72, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'XL_ENV', 0, 0.96, 2.94, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', 0, 0.15, 2.91, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', 0.15, 0.4, 3.0, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', 0.4, 0.9, 3.04, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', 0.9, 1.4, 3.05, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', 1.4, 1.9, 3.25, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_PARCEL', 1.9, 3.9, 5.1, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 0, 0.15, 2.94, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 0.15, 0.4, 3.01, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 0.4, 0.9, 3.06, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 0.9, 1.4, 3.26, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 1.4, 1.9, 3.48, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 1.9, 2.9, 4.73, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 2.9, 3.9, 5.16, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 3.9, 5.9, 5.19, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 5.9, 8.9, 5.57, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_PARCEL', 8.9, 11.9, 5.77, NULL, NULL, NULL, 'GBP', 1, NOW(), NOW()),
('UK', 'SMALL_OVER', 0, NULL, 3.65, 0.76, 1, 0.25, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_OVER_LIGHT', 0, NULL, 4.67, 0.76, 1, 0.24, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_OVER_HEAVY', 0, NULL, 8.28, 15.76, 1, 0.2, 'GBP', 1, NOW(), NOW()),
('UK', 'STD_OVER_LARGE', 0, NULL, 6.2, 0.76, 1, 0.16, 'GBP', 1, NOW(), NOW()),
('UK', 'BULKY_OVER', 0, NULL, 11.53, 0.76, 1, 0.31, 'GBP', 1, NOW(), NOW()),
('UK', 'HEAVY_OVER', 0, NULL, 13.04, 31.5, 1, 0.9, 'GBP', 1, NOW(), NOW()),
('JP', 'SMALL', 0, NULL, 288, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD1', 0, NULL, 318, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD2A', 0, NULL, 410, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD2B', 0, NULL, 415, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD2C', 0, NULL, 420, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD2D', 0, NULL, 425, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD2E', 0, NULL, 430, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD3', 0, NULL, 472, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'STD4', 0, NULL, 532, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE1', 0, NULL, 589, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE2', 0, NULL, 624, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE3', 0, NULL, 675, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE4', 0, NULL, 781, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE5', 0, NULL, 1020, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE6', 0, NULL, 1100, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE7', 0, NULL, 1532, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'LARGE8', 0, NULL, 1756, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'XL1', 0, NULL, 2755, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'XL2', 0, NULL, 3573, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'XL3', 0, NULL, 4496, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'XL4A', 0, NULL, 5625, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW()),
('JP', 'XL4B', 0, NULL, 13950, NULL, NULL, NULL, 'JPY', 1, NOW(), NOW());
INSERT INTO ecom_fba_storage_rate (marketplace, fee_type, size_category, month_from, month_to, age_from, age_to, rate, volume_unit, currency, builtin, remark, create_time, update_time) VALUES
('DE', 'MONTHLY', 'STANDARD', 1, 9, NULL, NULL, 27.54, 'CBM', 'EUR', 1, '标准件 1–9 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'MONTHLY', 'STANDARD', 10, 12, NULL, NULL, 47.45, 'CBM', 'EUR', 1, '标准件 10–12 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'MONTHLY', 'OVERSIZE', 1, 9, NULL, NULL, 19.8, 'CBM', 'EUR', 1, '大件 1–9 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'MONTHLY', 'OVERSIZE', 10, 12, NULL, NULL, 31.35, 'CBM', 'EUR', 1, '大件 10–12 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'STANDARD', NULL, NULL, 241, 270, 47.99, 'CBM', 'EUR', 1, '241–270 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'STANDARD', NULL, NULL, 271, 300, 125.16, 'CBM', 'EUR', 1, '271–300 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'STANDARD', NULL, NULL, 301, 330, 129.92, 'CBM', 'EUR', 1, '301–330 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'STANDARD', NULL, NULL, 331, 365, 135.98, 'CBM', 'EUR', 1, '331–365 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'STANDARD', NULL, NULL, 366, NULL, 227.63, 'CBM', 'EUR', 1, '366 天以上，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'OVERSIZE', NULL, NULL, 241, 270, 47.99, 'CBM', 'EUR', 1, '241–270 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'OVERSIZE', NULL, NULL, 271, 300, 125.16, 'CBM', 'EUR', 1, '271–300 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'OVERSIZE', NULL, NULL, 301, 330, 129.92, 'CBM', 'EUR', 1, '301–330 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'OVERSIZE', NULL, NULL, 331, 365, 135.98, 'CBM', 'EUR', 1, '331–365 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('DE', 'AGED', 'OVERSIZE', NULL, NULL, 366, NULL, 227.63, 'CBM', 'EUR', 1, '366 天以上，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'MONTHLY', 'STANDARD', 1, 9, NULL, NULL, 0.76, 'CUFT', 'GBP', 1, '标准件 1–9 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'MONTHLY', 'STANDARD', 10, 12, NULL, NULL, 1.37, 'CUFT', 'GBP', 1, '标准件 10–12 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'MONTHLY', 'OVERSIZE', 1, 9, NULL, NULL, 0.5, 'CUFT', 'GBP', 1, '大件 1–9 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'MONTHLY', 'OVERSIZE', 10, 12, NULL, NULL, 0.79, 'CUFT', 'GBP', 1, '大件 10–12 月，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'STANDARD', NULL, NULL, 241, 270, 1.18, 'CUFT', 'GBP', 1, '241–270 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'STANDARD', NULL, NULL, 271, 300, 3.14, 'CUFT', 'GBP', 1, '271–300 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'STANDARD', NULL, NULL, 301, 330, 3.26, 'CUFT', 'GBP', 1, '301–330 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'STANDARD', NULL, NULL, 331, 365, 3.41, 'CUFT', 'GBP', 1, '331–365 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'STANDARD', NULL, NULL, 366, NULL, 5.71, 'CUFT', 'GBP', 1, '366 天以上，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'OVERSIZE', NULL, NULL, 241, 270, 1.18, 'CUFT', 'GBP', 1, '241–270 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'OVERSIZE', NULL, NULL, 271, 300, 3.14, 'CUFT', 'GBP', 1, '271–300 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'OVERSIZE', NULL, NULL, 301, 330, 3.26, 'CUFT', 'GBP', 1, '301–330 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'OVERSIZE', NULL, NULL, 331, 365, 3.41, 'CUFT', 'GBP', 1, '331–365 天，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('UK', 'AGED', 'OVERSIZE', NULL, NULL, 366, NULL, 5.71, 'CUFT', 'GBP', 1, '366 天以上，2025 年欧洲费率表（2025-02-01 起）', NOW(), NOW()),
('JP', 'MONTHLY', 'STANDARD', 1, 9, NULL, NULL, 5676, 'CBM', 'JPY', 1, '标准件 1–9 月，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'MONTHLY', 'STANDARD', 10, 12, NULL, NULL, 10087, 'CBM', 'JPY', 1, '标准件 10–12 月，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'MONTHLY', 'OVERSIZE', 1, 9, NULL, NULL, 3278, 'CBM', 'JPY', 1, '大件 1–9 月，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'MONTHLY', 'OVERSIZE', 10, 12, NULL, NULL, 6984, 'CBM', 'JPY', 1, '大件 10–12 月，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'STANDARD', NULL, NULL, 271, 300, 16662, 'CBM', 'JPY', 1, '271–300 天，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'STANDARD', NULL, NULL, 301, 330, 17475, 'CBM', 'JPY', 1, '301–330 天，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'STANDARD', NULL, NULL, 331, 365, 18085, 'CBM', 'JPY', 1, '331–365 天，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'STANDARD', NULL, NULL, 366, NULL, 27431, 'CBM', 'JPY', 1, '366 天以上，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'OVERSIZE', NULL, NULL, 271, 300, 16662, 'CBM', 'JPY', 1, '271–300 天，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'OVERSIZE', NULL, NULL, 301, 330, 17475, 'CBM', 'JPY', 1, '301–330 天，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'OVERSIZE', NULL, NULL, 331, 365, 18085, 'CBM', 'JPY', 1, '331–365 天，2025 年 4 月改定后的日本站标准', NOW(), NOW()),
('JP', 'AGED', 'OVERSIZE', NULL, NULL, 366, NULL, 27431, 'CBM', 'JPY', 1, '366 天以上，2025 年 4 月改定后的日本站标准', NOW(), NOW());

-- ===== 第四轮：海外仓（owh）补充：自动同步开关、SKU 映射、退件、海外仓调拨 =====
ALTER TABLE ecom_owh_account ADD COLUMN auto_sync TINYINT NOT NULL DEFAULT 1 COMMENT '自动同步 1是：定时任务推送 / 同步该账号（演示、测试账号关闭）';
ALTER TABLE ecom_owh_warehouse ADD COLUMN defect_warehouse_id BIGINT DEFAULT NULL COMMENT '不良品仓（退件质检不良的转入；为空时报废出库）';
ALTER TABLE ecom_owh_warehouse ADD COLUMN contact VARCHAR(64) DEFAULT NULL COMMENT '收货联系人（海外仓之间调拨时作为收件人）';
ALTER TABLE ecom_owh_warehouse ADD COLUMN phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话';
ALTER TABLE ecom_owh_warehouse ADD COLUMN state VARCHAR(64) DEFAULT NULL COMMENT '州 / 省';
ALTER TABLE ecom_owh_warehouse ADD COLUMN city VARCHAR(64) DEFAULT NULL COMMENT '城市';
ALTER TABLE ecom_owh_warehouse ADD COLUMN postcode VARCHAR(16) DEFAULT NULL COMMENT '邮编';
ALTER TABLE ecom_owh_warehouse ADD COLUMN address VARCHAR(255) DEFAULT NULL COMMENT '仓库地址';

CREATE TABLE ecom_owh_sku (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id   BIGINT       NOT NULL COMMENT '海外仓账号',
    material_id  BIGINT       NOT NULL COMMENT '物料',
    owh_sku      VARCHAR(64)  NOT NULL COMMENT '服务商 SKU（没有映射的物料按物料编码）',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_sku_material (account_id, material_id, tenant_id),
    UNIQUE KEY uk_ecom_owh_sku_code (account_id, owh_sku, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓 SKU 映射（物料 ↔ 服务商 SKU）';

CREATE TABLE ecom_owh_return (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    return_no       VARCHAR(32)    NOT NULL COMMENT '退件单号（编码规则 OWR）',
    account_id      BIGINT         NOT NULL COMMENT '海外仓账号',
    mapping_id      BIGINT         NOT NULL COMMENT '仓库映射',
    warehouse_id    BIGINT         NOT NULL COMMENT '系统仓库（退回的海外仓）',
    store_id        BIGINT         NOT NULL COMMENT '店铺',
    ecom_return_id  BIGINT         NOT NULL COMMENT 'ERP 退货退款单',
    ecom_return_no  VARCHAR(32)             DEFAULT NULL COMMENT 'ERP 退货退款单号',
    platform_order_no VARCHAR(64)           DEFAULT NULL COMMENT '平台订单号',
    owh_order_no    VARCHAR(64)             DEFAULT NULL COMMENT '服务商退件单号',
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_owh_return_status) DRAFT草稿 PUSHED已推送 RECEIVED已收货 CANCELLED已取消',
    owh_status      VARCHAR(64)             DEFAULT NULL COMMENT '服务商返回的原始状态',
    carrier         VARCHAR(64)             DEFAULT NULL COMMENT '退件承运商',
    tracking_no     VARCHAR(64)             DEFAULT NULL COMMENT '退件运单号（买家寄回）',
    total_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '退件数量',
    good_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '良品数量',
    bad_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '不良品数量',
    push_time       DATETIME                DEFAULT NULL COMMENT '推送时间',
    receive_time    DATETIME                DEFAULT NULL COMMENT '服务商收货时间',
    erp_received    TINYINT        NOT NULL DEFAULT 0 COMMENT 'ERP 已退货入库 1是',
    defect_doc_no   VARCHAR(32)             DEFAULT NULL COMMENT '不良品转仓 / 报废出库单号',
    sync_time       DATETIME                DEFAULT NULL COMMENT '最近一次同步时间',
    error_msg       VARCHAR(500)            DEFAULT NULL COMMENT '最近一次推送 / 同步 / 入库失败的原因',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_return_no (return_no, tenant_id),
    KEY idx_ecom_owh_return_src (ecom_return_id),
    KEY idx_ecom_owh_return_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓退件单';

CREATE TABLE ecom_owh_return_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    owh_return_id BIGINT         NOT NULL COMMENT '海外仓退件单',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    sku           VARCHAR(64)    NOT NULL COMMENT '服务商 SKU',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '退件数量（物料数量）',
    good_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '良品',
    bad_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '不良品',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_owh_return_item (owh_return_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓退件单明细';

CREATE TABLE ecom_owh_transfer (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    transfer_no      VARCHAR(32)    NOT NULL COMMENT '调拨单号（编码规则 OWT），也是向服务商提交的参考号',
    from_warehouse_id BIGINT        NOT NULL COMMENT '调出仓',
    to_warehouse_id  BIGINT         NOT NULL COMMENT '调入仓',
    from_mapping_id  BIGINT                  DEFAULT NULL COMMENT '调出仓的海外仓映射（本地仓为空）',
    to_mapping_id    BIGINT                  DEFAULT NULL COMMENT '调入仓的海外仓映射（本地仓为空）',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 ecom_owh_transfer_status) DRAFT草稿 OUT_PUSHED调出仓处理中 IN_TRANSIT在途 RECEIVED已收货 CANCELLED已取消',
    product_code     VARCHAR(64)             DEFAULT NULL COMMENT '调出海外仓的物流产品',
    owh_out_no       VARCHAR(64)             DEFAULT NULL COMMENT '调出海外仓的出库单号',
    owh_status       VARCHAR(64)             DEFAULT NULL COMMENT '调出海外仓返回的原始状态',
    carrier          VARCHAR(64)             DEFAULT NULL COMMENT '承运商',
    tracking_no      VARCHAR(64)             DEFAULT NULL COMMENT '运单号',
    transfer_doc_id  BIGINT                  DEFAULT NULL COMMENT 'ERP 在途调拨单',
    transfer_doc_no  VARCHAR(32)             DEFAULT NULL COMMENT 'ERP 在途调拨单号',
    inbound_id       BIGINT                  DEFAULT NULL COMMENT '调入海外仓的入库单',
    total_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量合计',
    push_time        DATETIME                DEFAULT NULL COMMENT '提交时间',
    ship_time        DATETIME                DEFAULT NULL COMMENT '调出时间',
    receive_time     DATETIME                DEFAULT NULL COMMENT '收货时间',
    sync_time        DATETIME                DEFAULT NULL COMMENT '最近一次同步时间',
    error_msg        VARCHAR(500)            DEFAULT NULL COMMENT '最近一次失败的原因',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by        BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_owh_transfer_no (transfer_no, tenant_id),
    KEY idx_ecom_owh_transfer_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓调拨单（海外仓之间、海外仓与本地仓之间）';

CREATE TABLE ecom_owh_transfer_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    transfer_id   BIGINT         NOT NULL COMMENT '海外仓调拨单',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '数量',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_owh_transfer_item (transfer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓调拨单明细';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(525, 988, '海外仓退件', 'C', 'owh-return', 'ecom/owh-return/index', 'ecom:owhReturn:list', 'RollbackOutlined', 6, 1, 1, NOW(), NOW()),
(526, 988, '海外仓调拨', 'C', 'owh-transfer', 'ecom/owh-transfer/index', 'ecom:owhTransfer:list', 'SwapOutlined', 7, 1, 1, NOW(), NOW()),
(5250, 525, '新建退件单', 'F', NULL, NULL, 'ecom:owhReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(5251, 525, '推送退件单', 'F', NULL, NULL, 'ecom:owhReturn:push', NULL, 2, 1, 1, NOW(), NOW()),
(5252, 525, '同步退件', 'F', NULL, NULL, 'ecom:owhReturn:sync', NULL, 3, 1, 1, NOW(), NOW()),
(5253, 525, '取消退件单', 'F', NULL, NULL, 'ecom:owhReturn:cancel', NULL, 4, 1, 1, NOW(), NOW()),
(5260, 526, '新建调拨单', 'F', NULL, NULL, 'ecom:owhTransfer:add', NULL, 1, 1, 1, NOW(), NOW()),
(5261, 526, '提交调拨', 'F', NULL, NULL, 'ecom:owhTransfer:push', NULL, 2, 1, 1, NOW(), NOW()),
(5262, 526, '同步调拨', 'F', NULL, NULL, 'ecom:owhTransfer:sync', NULL, 3, 1, 1, NOW(), NOW()),
(5263, 526, '确认收货', 'F', NULL, NULL, 'ecom:owhTransfer:receive', NULL, 4, 1, 1, NOW(), NOW()),
(5264, 526, '取消调拨单', 'F', NULL, NULL, 'ecom:owhTransfer:cancel', NULL, 5, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(326, '海外仓退件单状态', 'ecom_owh_return_status', 'SYSTEM', 1, '海外仓退件单的状态', NOW(), NOW()),
(327, '海外仓调拨单状态', 'ecom_owh_transfer_status', 'SYSTEM', 1, '海外仓之间、海外仓与本地仓之间调拨单的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_owh_return_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_owh_return_status', '已推送', 'PUSHED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_owh_return_status', '已收货', 'RECEIVED', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_owh_return_status', '已取消', 'CANCELLED', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_owh_transfer_status', '草稿', 'DRAFT', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_owh_transfer_status', '调出仓处理中', 'OUT_PUSHED', 'processing', 2, 1, 1, NOW(), NOW()),
('ecom_owh_transfer_status', '在途', 'IN_TRANSIT', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_owh_transfer_status', '已收货', 'RECEIVED', 'green', 4, 1, 1, NOW(), NOW()),
('ecom_owh_transfer_status', '已取消', 'CANCELLED', 'default', 5, 1, 1, NOW(), NOW());

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id IN (525, 526) OR m.parent_id IN (525, 526))
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (525, 526)
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第四轮：演示店铺标记 =====
ALTER TABLE ecom_store ADD COLUMN demo TINYINT NOT NULL DEFAULT 0 COMMENT '演示店铺 1是 0否（由演示数据生成，不调用平台接口，定时任务跳过）';

-- ===== 第四轮：海外仓（owh）补充：推送后取消 / 标记已作废、按实收收货与收货差异 =====
CREATE TABLE ecom_owh_diff (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    source_type      VARCHAR(16)    NOT NULL COMMENT '来源(字典 ecom_owh_diff_source) INBOUND入库单 RETURN退件单 TRANSFER调拨单',
    source_id        BIGINT         NOT NULL COMMENT '来源单据 ID',
    source_no        VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    ref_no           VARCHAR(64)             DEFAULT NULL COMMENT '关联单据（头程货件号 / 调拨单号 / 退货单号）',
    ecom_inbound_id  BIGINT                  DEFAULT NULL COMMENT '关联的头程货件',
    warehouse_id     BIGINT         NOT NULL COMMENT '收货仓',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    sku              VARCHAR(64)             DEFAULT NULL COMMENT '服务商 SKU',
    expected_qty     DECIMAL(18, 4) NOT NULL COMMENT '单据数量',
    received_qty     DECIMAL(18, 4) NOT NULL COMMENT '实收数量',
    diff_qty         DECIMAL(18, 4) NOT NULL COMMENT '差异 = 单据 − 实收（正数短少、负数溢收）',
    status           VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 ecom_owh_diff_status) PENDING待处理 LOSS确认损耗 FOUND找回补收 ACCEPTED确认溢收',
    reason           VARCHAR(255)            DEFAULT NULL COMMENT '差异原因',
    diff_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '收货时生成的差异出库 / 溢收入库单号',
    handle_doc_no    VARCHAR(32)             DEFAULT NULL COMMENT '找回补收的其他入库单号',
    handle_time      DATETIME                DEFAULT NULL COMMENT '处理时间',
    handle_by        BIGINT                  DEFAULT NULL COMMENT '处理人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_owh_diff_source (source_type, source_id),
    KEY idx_ecom_owh_diff_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外仓收货差异（实收与单据数量不一致）';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(527, 988, '收货差异', 'C', 'owh-diff', 'ecom/owh-diff/index', 'ecom:owhDiff:list', 'IssuesCloseOutlined', 8, 1, 1, NOW(), NOW()),
(5270, 527, '处理差异', 'F', NULL, NULL, 'ecom:owhDiff:handle', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(328, '海外仓收货差异状态', 'ecom_owh_diff_status', 'SYSTEM', 1, '实收与单据数量不一致的处理状态', NOW(), NOW()),
(329, '海外仓收货差异来源', 'ecom_owh_diff_source', 'SYSTEM', 1, '收货差异来自哪类单据', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_owh_diff_status', '待处理', 'PENDING', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_owh_diff_status', '确认损耗', 'LOSS', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_owh_diff_status', '找回补收', 'FOUND', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_owh_diff_status', '确认溢收', 'ACCEPTED', 'blue', 4, 1, 1, NOW(), NOW()),
('ecom_owh_diff_source', '入库单', 'INBOUND', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_owh_diff_source', '退件单', 'RETURN', 'purple', 2, 1, 1, NOW(), NOW()),
('ecom_owh_diff_source', '调拨单', 'TRANSFER', 'cyan', 3, 1, 1, NOW(), NOW());

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (527, 5270)
WHERE r.id = 5 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id = 527
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 导航重组：「电商」拆为商品管理、订单管理、运营管理、物流管理四个模块，采购相关并入供应链，平台结算并入财务管理 =====
-- 页面改为绝对地址（/ecom/...、/scm/...、/wms/...），移动模块后页面地址不变，已有的跳转链接、通知链接、收藏的标签页继续可用；
-- 一级模块的地址只作为顶部导航的标识。电商看板同时作为登录后的首页。
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(19, 0, '商品管理', 'M', '/product', NULL, NULL, 'AppstoreOutlined', 1, 1, 1, NOW(), NOW()),
(20, 0, '订单管理', 'M', '/order', NULL, NULL, 'ShoppingOutlined', 2, 1, 1, NOW(), NOW()),
(23, 0, '运营管理', 'M', '/ops', NULL, NULL, 'RiseOutlined', 3, 1, 1, NOW(), NOW()),
(22, 0, '物流管理', 'M', '/logistics', NULL, NULL, 'CarOutlined', 4, 1, 1, NOW(), NOW()),
(990, 7, '平台结算', 'M', NULL, NULL, NULL, NULL, 0, 1, 1, NOW(), NOW()),
(991, 19, '刊登定价', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(992, 23, '店铺管理', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(993, 20, '售后客服', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(994, 20, '订单设置', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(995, 5, '1688 采购', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(996, 23, '数据分析', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(997, 22, 'FBA', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(998, 23, '广告管理', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW());

-- 原「电商」下的页面统一改为绝对地址（按钮没有地址，不受影响）
UPDATE sys_menu SET path = CONCAT('/ecom/', path) WHERE type = 'C' AND parent_id IN (980, 981, 982, 983, 984, 986, 987, 988, 989);

-- 商品管理：商品资料（本地商品 = 原仓库管理的物料管理、在线商品、SKU 配对）、刊登定价、产品开发
UPDATE sys_menu SET parent_id = 19, name = '商品资料', sort = 1 WHERE id = 981;
UPDATE sys_menu SET parent_id = 981, sort = 1, path = '/wms/material', name = '本地商品' WHERE id = 137;
UPDATE sys_menu SET sort = 2 WHERE id = 422;
UPDATE sys_menu SET sort = 3 WHERE id = 450;
UPDATE sys_menu SET parent_id = 991, sort = 1 WHERE id = 500;
UPDATE sys_menu SET parent_id = 991, sort = 2 WHERE id = 501;
UPDATE sys_menu SET parent_id = 991, sort = 3 WHERE id = 502;
UPDATE sys_menu SET parent_id = 991, sort = 4 WHERE id = 504;
UPDATE sys_menu SET parent_id = 991, sort = 5 WHERE id = 505;
UPDATE sys_menu SET parent_id = 991, sort = 6 WHERE id = 503;
UPDATE sys_menu SET parent_id = 19, sort = 3 WHERE id = 980;
-- 订单管理：订单处理、售后客服、订单设置
UPDATE sys_menu SET parent_id = 20, name = '订单处理', sort = 1 WHERE id = 982;
UPDATE sys_menu SET sort = 1 WHERE id = 423;
UPDATE sys_menu SET sort = 2 WHERE id = 433;
UPDATE sys_menu SET sort = 3 WHERE id = 434;
UPDATE sys_menu SET parent_id = 982, sort = 4 WHERE id = 532;
UPDATE sys_menu SET parent_id = 993, sort = 1 WHERE id = 424;
UPDATE sys_menu SET parent_id = 993, sort = 2 WHERE id = 488;
UPDATE sys_menu SET parent_id = 993, sort = 3 WHERE id = 485;
UPDATE sys_menu SET parent_id = 993, sort = 4 WHERE id = 486;
UPDATE sys_menu SET parent_id = 993, sort = 5 WHERE id = 487;
UPDATE sys_menu SET parent_id = 994, sort = 1 WHERE id = 432;
UPDATE sys_menu SET parent_id = 994, sort = 2 WHERE id = 530;
UPDATE sys_menu SET parent_id = 994, sort = 3 WHERE id = 531;
UPDATE sys_menu SET parent_id = 994, sort = 4 WHERE id = 533;
-- 运营管理：店铺管理、广告管理、运营监控（ASIN 监控、促销、目标与绩效）、数据分析
UPDATE sys_menu SET parent_id = 992, sort = 1 WHERE id = 421;
UPDATE sys_menu SET parent_id = 992, sort = 2 WHERE id = 440;
UPDATE sys_menu SET parent_id = 992, sort = 3 WHERE id = 544;
UPDATE sys_menu SET parent_id = 998, sort = 1 WHERE id = 430;
UPDATE sys_menu SET parent_id = 998, sort = 2 WHERE id = 480;
UPDATE sys_menu SET parent_id = 23, name = '运营监控', sort = 3 WHERE id = 989;
UPDATE sys_menu SET sort = 1 WHERE id = 540;
UPDATE sys_menu SET sort = 2 WHERE id = 541;
UPDATE sys_menu SET sort = 3 WHERE id = 542;
UPDATE sys_menu SET sort = 4 WHERE id = 543;
UPDATE sys_menu SET parent_id = 996, sort = 1 WHERE id = 420;
UPDATE sys_menu SET parent_id = 996, sort = 2 WHERE id = 431;
UPDATE sys_menu SET parent_id = 996, sort = 3 WHERE id = 490;
UPDATE sys_menu SET parent_id = 996, sort = 4 WHERE id = 491;
UPDATE sys_menu SET parent_id = 996, sort = 5 WHERE id = 492;
-- 物流管理：头程物流（发货计划、头程货件、物流渠道）、FBA、海外仓
UPDATE sys_menu SET parent_id = 22, name = '头程物流', sort = 1 WHERE id = 983;
UPDATE sys_menu SET sort = 1 WHERE id = 451;
UPDATE sys_menu SET sort = 2 WHERE id = 425;
UPDATE sys_menu SET sort = 3 WHERE id = 428;
UPDATE sys_menu SET parent_id = 997, sort = 1 WHERE id = 426;
UPDATE sys_menu SET parent_id = 997, sort = 2 WHERE id = 510;
UPDATE sys_menu SET parent_id = 997, sort = 3 WHERE id = 511;
UPDATE sys_menu SET parent_id = 997, sort = 4 WHERE id = 512;
UPDATE sys_menu SET parent_id = 997, sort = 5 WHERE id = 513;
UPDATE sys_menu SET parent_id = 997, sort = 6 WHERE id = 514;
UPDATE sys_menu SET parent_id = 22, sort = 3 WHERE id = 988;
-- 供应链：补货采购（补货建议、采购计划、采购订单 / 入库 / 退货、安全库存补货）、1688 采购、批发销售、往来单位、进销存报表
UPDATE sys_menu SET path = CONCAT('/scm/', path) WHERE type = 'C' AND parent_id IN (901, 902, 903, 966);
UPDATE sys_menu SET name = '补货采购', sort = 1 WHERE id = 901;
UPDATE sys_menu SET sort = sort + 2 WHERE parent_id = 901;
UPDATE sys_menu SET parent_id = 901, sort = 1 WHERE id = 427;
UPDATE sys_menu SET parent_id = 901, sort = 2 WHERE id = 460;
UPDATE sys_menu SET name = '安全库存补货' WHERE id = 274;
UPDATE sys_menu SET parent_id = 995, sort = 1 WHERE id = 555;
UPDATE sys_menu SET parent_id = 995, sort = 2 WHERE id = 554;
UPDATE sys_menu SET parent_id = 995, sort = 3 WHERE id = 553;
UPDATE sys_menu SET name = '批发销售', sort = 3 WHERE id = 902;
UPDATE sys_menu SET sort = 4 WHERE id = 903;
UPDATE sys_menu SET sort = 5 WHERE id = 966;
-- 财务管理 - 平台结算：平台结算、回款对账、费用映射、税务
UPDATE sys_menu SET parent_id = 990, sort = 1 WHERE id = 429;
UPDATE sys_menu SET parent_id = 990, sort = 2 WHERE id = 472;
UPDATE sys_menu SET parent_id = 990, sort = 3 WHERE id = 470;
UPDATE sys_menu SET parent_id = 990, sort = 4 WHERE id = 471;

-- 原「电商」模块与「结算与分析」「数据分析」「客服」分组已不再建立，清理早先按它们写入的角色授权
DELETE FROM sys_role_menu WHERE menu_id IN (17, 984, 986, 987);

-- 顶部导航顺序：商品、订单、运营、物流、供应链、仓库、财务，然后是智能、流程中心、系统管理、系统监控
UPDATE sys_menu SET sort = 5 WHERE id = 5;
UPDATE sys_menu SET sort = 6 WHERE id = 4;
UPDATE sys_menu SET sort = 7 WHERE id = 7;
UPDATE sys_menu SET sort = 8 WHERE id = 16;
UPDATE sys_menu SET sort = 9 WHERE id = 3;
UPDATE sys_menu SET sort = 10 WHERE id = 1;
UPDATE sys_menu SET sort = 11 WHERE id = 2;
UPDATE sys_menu SET sort = 12 WHERE id = 15;

-- 角色授权连带新的分组和模块（按钮 → 页面 → 分组 → 模块，逐级补齐）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, c.parent_id FROM sys_role_menu rm JOIN sys_menu c ON c.id = rm.menu_id
WHERE c.parent_id <> 0 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = c.parent_id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, c.parent_id FROM sys_role_menu rm JOIN sys_menu c ON c.id = rm.menu_id
WHERE c.parent_id <> 0 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = c.parent_id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, c.parent_id FROM sys_role_menu rm JOIN sys_menu c ON c.id = rm.menu_id
WHERE c.parent_id <> 0 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = c.parent_id);

-- 订单操作日志：订单的关键动作（写入、规则、审核、挂起、打单、发货、回传、取消、退货、补发、备注等），订单详情「处理记录」读取
DROP TABLE IF EXISTS ecom_order_log;
CREATE TABLE ecom_order_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id    BIGINT       NOT NULL COMMENT '平台订单',
    action      VARCHAR(32)  NOT NULL COMMENT '动作（EcomOrderLogAction）CREATE写入 RULE规则 AUDIT审核 HOLD挂起 LABEL打单 SHIP发货 NOTE备注等',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '内容',
    create_by   BIGINT                DEFAULT NULL COMMENT '操作人，系统执行时为空',
    create_time DATETIME              DEFAULT NULL COMMENT '操作时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_order_log_order (order_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单操作日志';

-- 报表查询的索引：进销存报表按出入库单 + 物料取库存流水的实际成本、按出入库单取应收应付单的折让和坏账；
-- 广告活动报告缺失某天时按活动 + 日期判断是否有活动报告
CREATE INDEX idx_flow_doc_material ON wms_stock_flow (doc_id, material_id);
CREATE INDEX idx_bill_source_id ON fin_bill (source_id);
CREATE INDEX idx_ecom_ad_stat_entity ON ecom_ad_stat (entity_id, stat_date);

-- ===== 第五轮：AI 运营助手 =====
-- 运营日报（程序汇总的结构化数据 + AI 要点与建议）、日报订阅（订阅人、店铺范围、推送渠道）、违禁词库（刊登合规检查）；
-- 菜单在「运营管理」下的「智能运营」分组（985）

CREATE TABLE ecom_ops_report (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    report_date      DATE          NOT NULL COMMENT '日报日期（统计的那一天，一般为昨天）',
    scope_key        VARCHAR(64)   NOT NULL COMMENT '店铺范围标识：排序后的店铺 ID 的摘要，同一天同一范围一份（重新生成时覆盖）',
    scope_name       VARCHAR(255)           DEFAULT NULL COMMENT '范围说明（全部店铺 / 店铺名称）',
    store_count      INT           NOT NULL DEFAULT 0 COMMENT '范围内的店铺数（与 ecom_ops_report_store 的行数一致，列表据此判断是否全部可见）',
    source           VARCHAR(16)   NOT NULL DEFAULT 'SCHEDULE' COMMENT '来源(字典 ecom_ops_report_source) SCHEDULE定时 MANUAL手动',
    orders           INT           NOT NULL DEFAULT 0 COMMENT '订单数',
    sales            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售额（本位币）',
    profit           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '利润（本位币，已扣广告）',
    sales_change     DECIMAL(12, 2)          DEFAULT NULL COMMENT '销售额环比（%，对比前一天）',
    profit_change    DECIMAL(12, 2)          DEFAULT NULL COMMENT '利润环比（%）',
    abnormal_count   INT           NOT NULL DEFAULT 0 COMMENT '异常 SKU 数',
    bad_review_count INT           NOT NULL DEFAULT 0 COMMENT '新差评数',
    stock_risk_count INT           NOT NULL DEFAULT 0 COMMENT '断货风险 SKU 数',
    ad_alert_count   INT           NOT NULL DEFAULT 0 COMMENT '广告异常数',
    todo_count       INT           NOT NULL DEFAULT 0 COMMENT '待处理事项合计',
    summary          VARCHAR(500)           DEFAULT NULL COMMENT '要点摘要（程序生成，推送内容）',
    content          MEDIUMTEXT COMMENT '结构化内容（JSON：核心指标、店铺、异常 SKU、差评、断货、广告、待办、要点）',
    ai_status        VARCHAR(16)   NOT NULL DEFAULT 'NONE' COMMENT 'AI 建议状态(字典 ecom_ops_report_ai_status) NONE未生成 SUCCESS已生成 FAILED失败',
    ai_advice        TEXT COMMENT 'AI 要点与建议',
    ai_error         VARCHAR(500)           DEFAULT NULL COMMENT 'AI 生成失败的原因 / 未生成的说明',
    ai_sample        TINYINT       NOT NULL DEFAULT 0 COMMENT 'AI 建议为示例文本（演示数据） 1是 0否',
    push_count       INT           NOT NULL DEFAULT 0 COMMENT '已推送的订阅人数',
    pushed_time      DATETIME               DEFAULT NULL COMMENT '最近推送时间',
    create_by        BIGINT                 DEFAULT NULL COMMENT '生成人（定时生成为空）',
    create_time      DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_ops_report (report_date, scope_key, tenant_id),
    KEY idx_ecom_ops_report_date (report_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运营日报';

CREATE TABLE ecom_ops_report_store (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT   NOT NULL DEFAULT 1 COMMENT '租户ID',
    report_id   BIGINT   NOT NULL COMMENT '运营日报',
    store_id    BIGINT   NOT NULL COMMENT '店铺（店铺数据权限按此过滤：日报的店铺全部可见时才能查看）',
    create_time DATETIME          DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_ops_report_store_report (report_id),
    KEY idx_ecom_ops_report_store_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运营日报的店铺范围';

CREATE TABLE ecom_ops_report_sub (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id        BIGINT        NOT NULL COMMENT '订阅人',
    store_ids      VARCHAR(1000)          DEFAULT NULL COMMENT '店铺范围（店铺 ID，逗号分隔），为空时为订阅人可见的全部店铺',
    channels       VARCHAR(64)   NOT NULL DEFAULT 'SITE' COMMENT '推送渠道（逗号分隔，字典 ecom_ops_push_channel）SITE站内消息 DINGTALK钉钉 WECOM企业微信 FEISHU飞书',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    last_report_id BIGINT                 DEFAULT NULL COMMENT '最近推送的日报',
    last_push_time DATETIME               DEFAULT NULL COMMENT '最近推送时间',
    create_by      BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecom_ops_report_sub (user_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '运营日报订阅';

CREATE TABLE ecom_banned_word (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    word        VARCHAR(128) NOT NULL COMMENT '词（不区分大小写）',
    word_type   VARCHAR(32)  NOT NULL COMMENT '类型(业务字典 ecom_banned_word_type) PROHIBITED禁用词 TRADEMARK侵权品牌词 EXAGGERATION夸大宣传词 MEDICAL医疗功效词',
    level       VARCHAR(16)  NOT NULL DEFAULT 'BLOCK' COMMENT '处理方式(字典 ecom_banned_word_level) BLOCK阻止发布 WARN只提醒',
    match_mode  VARCHAR(16)  NOT NULL DEFAULT 'WORD' COMMENT '匹配方式(字典 ecom_banned_word_match) WORD整词 CONTAINS包含',
    platform    VARCHAR(16)           DEFAULT NULL COMMENT '适用平台(字典 ecom_platform)，为空全部平台',
    marketplace VARCHAR(16)           DEFAULT NULL COMMENT '适用站点(业务字典 ecom_marketplace)，为空全部站点',
    suggestion  VARCHAR(255)          DEFAULT NULL COMMENT '替换建议 / 说明',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置词 1是 0否（新租户从平台复制）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_banned_word_word (word)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '违禁词库：刊登时检查标题、五点、描述';

-- 菜单：运营管理下的「智能运营」分组（运营日报、违禁词库），刊登中心的「AI 写 Listing」按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 985, 23, '智能运营', 'M', NULL, NULL, NULL, NULL, 5, 1, 1, NOW(), NOW() FROM sys_menu WHERE id = 23 AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 985);
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(580, 985, '运营日报', 'C', '/ecom/ops-report', 'ecom/ops-report/index', 'ecom:opsReport:list', 'FileTextOutlined', 1, 1, 1, NOW(), NOW()),
(581, 985, '违禁词库', 'C', '/ecom/banned-word', 'ecom/banned-word/index', 'ecom:bannedWord:list', 'StopOutlined', 2, 1, 1, NOW(), NOW()),
(5800, 580, '生成日报', 'F', NULL, NULL, 'ecom:opsReport:generate', NULL, 1, 1, 1, NOW(), NOW()),
(5801, 580, '删除日报', 'F', NULL, NULL, 'ecom:opsReport:remove', NULL, 2, 1, 1, NOW(), NOW()),
(5802, 580, '订阅设置', 'F', NULL, NULL, 'ecom:opsReport:subscribe', NULL, 3, 1, 1, NOW(), NOW()),
(5810, 581, '新增违禁词', 'F', NULL, NULL, 'ecom:bannedWord:add', NULL, 1, 1, 1, NOW(), NOW()),
(5811, 581, '修改违禁词', 'F', NULL, NULL, 'ecom:bannedWord:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5812, 581, '删除违禁词', 'F', NULL, NULL, 'ecom:bannedWord:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5820, 500, 'AI 写 Listing', 'F', NULL, NULL, 'ecom:publish:ai', NULL, 20, 1, 1, NOW(), NOW());

-- 字典：违禁词类型为业务字典（可扩展），其余为系统字典
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(380, '日报来源', 'ecom_ops_report_source', 'SYSTEM', 1, '运营日报的生成方式', NOW(), NOW()),
(381, '日报 AI 状态', 'ecom_ops_report_ai_status', 'SYSTEM', 1, '运营日报「要点与建议」的生成状态', NOW(), NOW()),
(382, '推送渠道', 'ecom_ops_push_channel', 'SYSTEM', 1, '运营日报订阅的推送渠道', NOW(), NOW()),
(383, '违禁词类型', 'ecom_banned_word_type', 'BUSINESS', 1, '违禁词库的分类', NOW(), NOW()),
(384, '违禁词处理方式', 'ecom_banned_word_level', 'SYSTEM', 1, '刊登命中违禁词时阻止发布或只提醒', NOW(), NOW()),
(385, '违禁词匹配方式', 'ecom_banned_word_match', 'SYSTEM', 1, '整词匹配或包含即命中', NOW(), NOW()),
(386, '异常 SKU 类型', 'ecom_ops_abnormal_type', 'SYSTEM', 1, '运营日报识别的 SKU 异常', NOW(), NOW()),
(387, '广告异常类型', 'ecom_ops_ad_alert_type', 'SYSTEM', 1, '运营日报识别的广告异常', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_ops_report_source', '定时', 'SCHEDULE', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_ops_report_source', '手动', 'MANUAL', 'purple', 2, 1, 1, NOW(), NOW()),
('ecom_ops_report_ai_status', '未生成', 'NONE', 'default', 1, 1, 1, NOW(), NOW()),
('ecom_ops_report_ai_status', '已生成', 'SUCCESS', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_ops_report_ai_status', '失败', 'FAILED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_ops_push_channel', '站内消息', 'SITE', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_ops_push_channel', '钉钉', 'DINGTALK', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_ops_push_channel', '企业微信', 'WECOM', 'green', 3, 1, 1, NOW(), NOW()),
('ecom_ops_push_channel', '飞书', 'FEISHU', 'geekblue', 4, 1, 1, NOW(), NOW()),
('ecom_banned_word_type', '禁用词', 'PROHIBITED', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_banned_word_type', '侵权品牌词', 'TRADEMARK', 'volcano', 2, 1, 1, NOW(), NOW()),
('ecom_banned_word_type', '夸大宣传词', 'EXAGGERATION', 'orange', 3, 1, 1, NOW(), NOW()),
('ecom_banned_word_type', '医疗功效词', 'MEDICAL', 'magenta', 4, 1, 1, NOW(), NOW()),
('ecom_banned_word_level', '阻止发布', 'BLOCK', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_banned_word_level', '只提醒', 'WARN', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_banned_word_match', '整词', 'WORD', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_banned_word_match', '包含', 'CONTAINS', 'purple', 2, 1, 1, NOW(), NOW()),
('ecom_ops_abnormal_type', '销量骤降', 'SALES_DROP', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_ops_abnormal_type', '利润下滑', 'PROFIT_DROP', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_ops_abnormal_type', '退货率升高', 'RETURN_RATE_UP', 'magenta', 3, 1, 1, NOW(), NOW()),
('ecom_ops_ad_alert_type', '超预算', 'OVER_BUDGET', 'red', 1, 1, 1, NOW(), NOW()),
('ecom_ops_ad_alert_type', 'ACOS 偏高', 'ACOS_HIGH', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_ops_ad_alert_type', '花费无转化', 'NO_ORDER', 'volcano', 3, 1, 1, NOW(), NOW());

-- 内置违禁词（平台租户，新租户由 EcomBannedWordTenantInitializer 复制）
INSERT INTO ecom_banned_word (word, word_type, level, match_mode, platform, marketplace, suggestion, builtin, status, create_time, update_time) VALUES
('FDA approved', 'MEDICAL', 'BLOCK', 'WORD', NULL, NULL, '未经 FDA 批准不得声称；可改为描述材质与检测报告', 1, 1, NOW(), NOW()),
('FDA certified', 'MEDICAL', 'BLOCK', 'WORD', NULL, NULL, 'FDA 不对产品做认证，删除该说法', 1, 1, NOW(), NOW()),
('cure', 'MEDICAL', 'BLOCK', 'WORD', NULL, NULL, '普通商品不能宣称治疗功效', 1, 1, NOW(), NOW()),
('treat', 'MEDICAL', 'WARN', 'WORD', NULL, NULL, '涉及疾病时属于医疗声明，确认上下文', 1, 1, NOW(), NOW()),
('anti-inflammatory', 'MEDICAL', 'BLOCK', 'WORD', NULL, NULL, '医疗功效声明，删除', 1, 1, NOW(), NOW()),
('pain relief', 'MEDICAL', 'WARN', 'WORD', NULL, NULL, '医疗功效声明，非医疗器械慎用', 1, 1, NOW(), NOW()),
('antibacterial', 'PROHIBITED', 'BLOCK', 'WORD', 'AMAZON', NULL, '杀菌类宣称需 EPA 注册（亚马逊农药政策），删除或改为 easy to clean', 1, 1, NOW(), NOW()),
('antimicrobial', 'PROHIBITED', 'BLOCK', 'WORD', 'AMAZON', NULL, '杀菌类宣称需 EPA 注册（亚马逊农药政策）', 1, 1, NOW(), NOW()),
('antiviral', 'PROHIBITED', 'BLOCK', 'WORD', NULL, NULL, '抗病毒宣称属于受限声明', 1, 1, NOW(), NOW()),
('kills 99.9%', 'PROHIBITED', 'BLOCK', 'CONTAINS', NULL, NULL, '杀菌率宣称需注册与检测，删除', 1, 1, NOW(), NOW()),
('pesticide', 'PROHIBITED', 'BLOCK', 'WORD', NULL, NULL, '农药类商品受限', 1, 1, NOW(), NOW()),
('non-toxic', 'PROHIBITED', 'WARN', 'WORD', 'AMAZON', NULL, '亚马逊限制 non-toxic 宣称，可改为 BPA free 等具体说明', 1, 1, NOW(), NOW()),
('eco-friendly', 'EXAGGERATION', 'WARN', 'WORD', 'AMAZON', NULL, '环保宣称需有依据（如认证），否则删除', 1, 1, NOW(), NOW()),
('COVID', 'PROHIBITED', 'BLOCK', 'CONTAINS', NULL, NULL, '与疫情相关的宣称被平台禁止', 1, 1, NOW(), NOW()),
('coronavirus', 'PROHIBITED', 'BLOCK', 'CONTAINS', NULL, NULL, '与疫情相关的宣称被平台禁止', 1, 1, NOW(), NOW()),
('CBD', 'PROHIBITED', 'BLOCK', 'WORD', NULL, NULL, '大麻二酚类商品禁售', 1, 1, NOW(), NOW()),
('replica', 'PROHIBITED', 'BLOCK', 'WORD', NULL, NULL, '仿品描述，禁止使用', 1, 1, NOW(), NOW()),
('counterfeit', 'PROHIBITED', 'BLOCK', 'WORD', NULL, NULL, '仿品描述，禁止使用', 1, 1, NOW(), NOW()),
('knockoff', 'PROHIBITED', 'BLOCK', 'WORD', NULL, NULL, '仿品描述，禁止使用', 1, 1, NOW(), NOW()),
('best seller', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '排名类说法不能写在标题 / 五点中', 1, 1, NOW(), NOW()),
('best-selling', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '排名类说法不能写在标题 / 五点中', 1, 1, NOW(), NOW()),
('#1', 'EXAGGERATION', 'WARN', 'CONTAINS', NULL, NULL, '排名类说法需有依据，建议删除', 1, 1, NOW(), NOW()),
('No.1', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '排名类说法需有依据，建议删除', 1, 1, NOW(), NOW()),
('number one', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '排名类说法需有依据，建议删除', 1, 1, NOW(), NOW()),
('top rated', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '评价类说法不能写在商品文案中', 1, 1, NOW(), NOW()),
('100% guaranteed', 'EXAGGERATION', 'WARN', 'CONTAINS', NULL, NULL, '绝对化承诺，改为具体的保修 / 售后说明', 1, 1, NOW(), NOW()),
('guaranteed', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '绝对化承诺，改为具体的保修 / 售后说明', 1, 1, NOW(), NOW()),
('miracle', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '夸大用语，删除', 1, 1, NOW(), NOW()),
('perfect', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '主观夸大用语，建议换成具体卖点', 1, 1, NOW(), NOW()),
('cheapest', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '比较价格的说法不能写在文案中', 1, 1, NOW(), NOW()),
('lowest price', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '比较价格的说法不能写在文案中', 1, 1, NOW(), NOW()),
('free shipping', 'EXAGGERATION', 'WARN', 'WORD', 'AMAZON', NULL, '亚马逊标题 / 五点不能写促销与运费信息', 1, 1, NOW(), NOW()),
('hot sale', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '促销用语，标题中删除', 1, 1, NOW(), NOW()),
('limited time', 'EXAGGERATION', 'WARN', 'WORD', NULL, NULL, '促销用语，标题中删除', 1, 1, NOW(), NOW()),
('最安値', 'EXAGGERATION', 'WARN', 'CONTAINS', NULL, 'JP', '日本景品表示法：最安值说法需有依据', 1, 1, NOW(), NOW()),
('日本一', 'EXAGGERATION', 'WARN', 'CONTAINS', NULL, 'JP', '日本景品表示法：第一类说法需有依据', 1, 1, NOW(), NOW()),
('Testsieger', 'EXAGGERATION', 'WARN', 'WORD', NULL, 'DE', '德国：未获测评的「测试冠军」说法属于不正当竞争', 1, 1, NOW(), NOW()),
('Apple', 'TRADEMARK', 'WARN', 'WORD', NULL, NULL, '只能以「compatible with」描述兼容性，不能作为品牌或系列名', 1, 1, NOW(), NOW()),
('iPhone', 'TRADEMARK', 'WARN', 'WORD', NULL, NULL, '只能以「compatible with iPhone」描述兼容性', 1, 1, NOW(), NOW()),
('AirPods', 'TRADEMARK', 'WARN', 'WORD', NULL, NULL, '只能以「compatible with」描述兼容性', 1, 1, NOW(), NOW()),
('MagSafe', 'TRADEMARK', 'WARN', 'WORD', NULL, NULL, '未获 MFi / MagSafe 授权时改为 magnetic', 1, 1, NOW(), NOW()),
('Samsung', 'TRADEMARK', 'WARN', 'WORD', NULL, NULL, '只能以「compatible with」描述兼容性', 1, 1, NOW(), NOW()),
('Nike', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('Adidas', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('Disney', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, 'IP 授权商标，未授权禁止使用', 1, 1, NOW(), NOW()),
('Marvel', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, 'IP 授权商标，未授权禁止使用', 1, 1, NOW(), NOW()),
('Pokemon', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, 'IP 授权商标，未授权禁止使用', 1, 1, NOW(), NOW()),
('Hello Kitty', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, 'IP 授权商标，未授权禁止使用', 1, 1, NOW(), NOW()),
('LEGO', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，可改为 building blocks', 1, 1, NOW(), NOW()),
('Dyson', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('YETI', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌（保温杯类目投诉高发）', 1, 1, NOW(), NOW()),
('Stanley', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌（保温杯类目投诉高发）', 1, 1, NOW(), NOW()),
('OtterBox', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('GoPro', 'TRADEMARK', 'WARN', 'WORD', NULL, NULL, '只能以「compatible with」描述兼容性', 1, 1, NOW(), NOW()),
('Rolex', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('Gucci', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('Louis Vuitton', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW()),
('Chanel', 'TRADEMARK', 'BLOCK', 'WORD', NULL, NULL, '知名品牌，未授权禁止使用', 1, 1, NOW(), NOW());

-- 内置角色：超级管理员、电商运营全部，总经理只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON (m.id IN (23, 985, 500) OR m.id BETWEEN 580 AND 589 OR m.id BETWEEN 5800 AND 5899)
WHERE r.id IN (1, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (23, 985, 580, 581)
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- ===== 第五轮：自动化规则中心（auto） =====
-- 「当…就…」自动化：规则（触发器 + 范围 + 条件 + 动作）、执行日志、规则创建的待办提醒。代码在 modules/ecom/automation
DROP TABLE IF EXISTS ecom_auto_rule;
CREATE TABLE ecom_auto_rule (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)  NOT NULL COMMENT '规则名称',
    trigger_code      VARCHAR(32)  NOT NULL COMMENT '触发器(字典 ecom_auto_trigger)',
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    scope_json        VARCHAR(4000)         DEFAULT NULL COMMENT '对象范围 JSON：店铺、平台、站点、SKU、标签',
    condition_json    TEXT                  DEFAULT NULL COMMENT '条件组 JSON：logic AND/OR、items 条件（字段 + 运算符 + 值）、groups 子组',
    actions_json      TEXT                  DEFAULT NULL COMMENT '动作 JSON（按顺序执行，密钥加密）',
    debounce_hours    INT          NOT NULL DEFAULT 24 COMMENT '防抖（小时）：同一对象在这段时间内只执行一次，0 不防抖',
    daily_limit       INT          NOT NULL DEFAULT 0 COMMENT '每日执行上限，0 不限',
    template_code     VARCHAR(32)           DEFAULT NULL COMMENT '来源模板',
    trigger_count     INT          NOT NULL DEFAULT 0 COMMENT '执行次数（执行了动作）',
    last_trigger_time DATETIME              DEFAULT NULL COMMENT '最近执行时间',
    last_result       VARCHAR(16)           DEFAULT NULL COMMENT '最近执行结果(字典 ecom_auto_result)',
    remark            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by         BIGINT                DEFAULT NULL COMMENT '创建人（动作以创建人的身份执行）',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_auto_rule_trigger (trigger_code, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动化规则';

DROP TABLE IF EXISTS ecom_auto_log;
CREATE TABLE ecom_auto_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_id      BIGINT       NOT NULL COMMENT '规则',
    rule_name    VARCHAR(64)  NOT NULL COMMENT '规则名称（规则删除后仍可查看）',
    trigger_code VARCHAR(32)  NOT NULL COMMENT '触发器(字典 ecom_auto_trigger)',
    object_key   VARCHAR(64)  NOT NULL COMMENT '对象标识（防抖按规则 + 对象判断），如 ORDER:123',
    object_label VARCHAR(128)          DEFAULT NULL COMMENT '对象名称（订单号、SKU、活动名称等）',
    store_id     BIGINT                DEFAULT NULL COMMENT '店铺',
    result       VARCHAR(16)  NOT NULL COMMENT '结果(字典 ecom_auto_result) SUCCESS成功 PARTIAL部分成功 FAILED失败 NOT_MATCHED条件不满足 DEBOUNCED防抖跳过 LIMITED达到上限',
    match_detail VARCHAR(1000)         DEFAULT NULL COMMENT '条件判断说明',
    cost_ms      INT          NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    error_msg    VARCHAR(500)          DEFAULT NULL COMMENT '第一个失败动作的原因',
    retry_count  INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    event_json   TEXT                  DEFAULT NULL COMMENT '事件 JSON（字段值、关联对象，重试与试运行使用）',
    actions_json TEXT                  DEFAULT NULL COMMENT '动作结果 JSON（类型、结果、说明、耗时）',
    create_time  DATETIME              DEFAULT NULL COMMENT '执行时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_auto_log_rule (rule_id, object_key, create_time),
    KEY idx_ecom_auto_log_time (create_time),
    KEY idx_ecom_auto_log_trigger (trigger_code, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动化执行日志';

DROP TABLE IF EXISTS ecom_auto_todo;
CREATE TABLE ecom_auto_todo (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_id     BIGINT       NOT NULL COMMENT '创建的规则',
    assignee_id BIGINT       NOT NULL COMMENT '处理人',
    store_id    BIGINT                DEFAULT NULL COMMENT '店铺',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '内容',
    link        VARCHAR(255)          DEFAULT NULL COMMENT '点击跳转的页面',
    object_key  VARCHAR(64)           DEFAULT NULL COMMENT '对象标识（同一规则同一对象未完成的不重复创建）',
    due_date    DATE                  DEFAULT NULL COMMENT '期限',
    status      VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT '状态(字典 ecom_auto_todo_status) OPEN待处理 DONE已完成',
    done_by     BIGINT                DEFAULT NULL COMMENT '完成人',
    done_time   DATETIME              DEFAULT NULL COMMENT '完成时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ecom_auto_todo_assignee (assignee_id, status),
    KEY idx_ecom_auto_todo_rule (rule_id, object_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动化待办提醒';

-- 菜单：运营管理 → 智能运营（分组 985，与 AI 运营助手共用，先建的一方建立）→ 自动化规则、执行日志、待办提醒
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 985, 23, '智能运营', 'M', NULL, NULL, NULL, NULL, 5, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 985);
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(590, 985, '自动化规则', 'C', '/ecom/automation', 'ecom/automation/index', 'ecom:automation:list', 'ThunderboltOutlined', 20, 1, 1, NOW(), NOW()),
(591, 985, '执行日志', 'C', '/ecom/automation-log', 'ecom/automation-log/index', 'ecom:automationLog:list', 'FileSearchOutlined', 21, 1, 1, NOW(), NOW()),
(592, 985, '待办提醒', 'C', '/ecom/automation-todo', 'ecom/automation-todo/index', 'ecom:automationTodo:list', 'BellOutlined', 22, 1, 1, NOW(), NOW()),
(5900, 590, '新增规则', 'F', NULL, NULL, 'ecom:automation:add', NULL, 1, 1, 1, NOW(), NOW()),
(5901, 590, '修改规则', 'F', NULL, NULL, 'ecom:automation:edit', NULL, 2, 1, 1, NOW(), NOW()),
(5902, 590, '删除规则', 'F', NULL, NULL, 'ecom:automation:remove', NULL, 3, 1, 1, NOW(), NOW()),
(5903, 590, '试运行', 'F', NULL, NULL, 'ecom:automation:test', NULL, 4, 1, 1, NOW(), NOW()),
(5910, 591, '重试', 'F', NULL, NULL, 'ecom:automationLog:retry', NULL, 1, 1, 1, NOW(), NOW());

-- 授权：超级管理员、电商运营全部，总经理只读（页面）；连带分组 985 和模块 23
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (985, 590, 591, 592, 5900, 5901, 5902, 5903, 5910)
WHERE r.id IN (1, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (985, 590, 591, 592)
WHERE r.id = 3 AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 23 FROM sys_role r
WHERE r.id IN (1, 3, 5) AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 23);

-- 字典（系统字典）：触发器、动作、执行结果、动作结果、待办状态、触发器分类；改价来源增加「自动化规则」
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time) VALUES
(390, '自动化触发器', 'ecom_auto_trigger', 'SYSTEM', 1, '自动化规则的触发器', NOW(), NOW()),
(391, '自动化动作', 'ecom_auto_action', 'SYSTEM', 1, '自动化规则的动作类型', NOW(), NOW()),
(392, '自动化执行结果', 'ecom_auto_result', 'SYSTEM', 1, '一次规则执行的结果', NOW(), NOW()),
(393, '自动化动作结果', 'ecom_auto_action_status', 'SYSTEM', 1, '单个动作的执行结果', NOW(), NOW()),
(394, '自动化待办状态', 'ecom_auto_todo_status', 'SYSTEM', 1, '自动化规则创建的待办提醒的状态', NOW(), NOW()),
(395, '自动化触发器分类', 'ecom_auto_category', 'SYSTEM', 1, '触发器的业务分类', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time) VALUES
('ecom_auto_trigger', '新订单', 'ORDER_NEW', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '订单异常', 'ORDER_EXCEPTION', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '订单预估亏损', 'ORDER_LOSS', 'volcano', 3, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '断货风险', 'STOCKOUT_RISK', 'orange', 4, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '低于安全库存', 'STOCK_LOW', 'gold', 5, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '新差评', 'REVIEW_NEGATIVE', 'magenta', 6, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '买家消息超时未回复', 'MESSAGE_TIMEOUT', 'purple', 7, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '商品被跟卖', 'LISTING_HIJACKED', 'red', 8, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '排名大跌', 'LISTING_RANK_DROP', 'volcano', 9, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '商品被下架', 'LISTING_DELISTED', 'red', 10, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '店铺授权失效', 'STORE_AUTH_INVALID', 'red', 11, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '渠道同步失败', 'SYNC_FAILED', 'orange', 12, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '头程货件签收', 'INBOUND_RECEIVED', 'green', 13, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '头程货件超期', 'INBOUND_OVERDUE', 'orange', 14, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '广告 ACOS 过高', 'AD_ACOS_HIGH', 'volcano', 15, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '广告预算耗尽', 'AD_BUDGET_EXHAUSTED', 'gold', 16, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', 'FBA 索赔临近截止', 'FBA_CLAIM_DEADLINE', 'purple', 17, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '退货率异常', 'RETURN_RATE_HIGH', 'magenta', 18, 1, 1, NOW(), NOW()),
('ecom_auto_trigger', '价格被竞品低于', 'COMPETITOR_PRICE_LOWER', 'cyan', 19, 1, 1, NOW(), NOW()),
('ecom_auto_action', '发送通知', 'NOTIFY', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_auto_action', '群机器人', 'IM_ROBOT', 'cyan', 2, 1, 1, NOW(), NOW()),
('ecom_auto_action', 'Webhook', 'WEBHOOK', 'geekblue', 3, 1, 1, NOW(), NOW()),
('ecom_auto_action', '创建待办', 'TODO', 'purple', 4, 1, 1, NOW(), NOW()),
('ecom_auto_action', '挂起订单', 'ORDER_HOLD', 'orange', 5, 1, 1, NOW(), NOW()),
('ecom_auto_action', '订单打标签', 'ORDER_TAG', 'gold', 6, 1, 1, NOW(), NOW()),
('ecom_auto_action', '指定仓库 / 物流', 'ORDER_ASSIGN', 'lime', 7, 1, 1, NOW(), NOW()),
('ecom_auto_action', '在线商品调价', 'LISTING_REPRICE', 'volcano', 8, 1, 1, NOW(), NOW()),
('ecom_auto_action', '生成采购计划', 'PURCHASE_PLAN', 'green', 9, 1, 1, NOW(), NOW()),
('ecom_auto_action', '建售后工单', 'CS_TICKET', 'magenta', 10, 1, 1, NOW(), NOW()),
('ecom_auto_action', '暂停广告活动', 'AD_PAUSE', 'red', 11, 1, 1, NOW(), NOW()),
('ecom_auto_action', '调整竞价 / 预算', 'AD_ADJUST', 'orange', 12, 1, 1, NOW(), NOW()),
('ecom_auto_result', '成功', 'SUCCESS', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_auto_result', '部分成功', 'PARTIAL', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_auto_result', '失败', 'FAILED', 'red', 3, 1, 1, NOW(), NOW()),
('ecom_auto_result', '条件不满足', 'NOT_MATCHED', 'default', 4, 1, 1, NOW(), NOW()),
('ecom_auto_result', '防抖跳过', 'DEBOUNCED', 'cyan', 5, 1, 1, NOW(), NOW()),
('ecom_auto_result', '达到上限', 'LIMITED', 'purple', 6, 1, 1, NOW(), NOW()),
('ecom_auto_action_status', '成功', 'SUCCESS', 'green', 1, 1, 1, NOW(), NOW()),
('ecom_auto_action_status', '失败', 'FAILED', 'red', 2, 1, 1, NOW(), NOW()),
('ecom_auto_action_status', '跳过', 'SKIPPED', 'default', 3, 1, 1, NOW(), NOW()),
('ecom_auto_action_status', '仅建议', 'SUGGESTED', 'blue', 4, 1, 1, NOW(), NOW()),
('ecom_auto_todo_status', '待处理', 'OPEN', 'orange', 1, 1, 1, NOW(), NOW()),
('ecom_auto_todo_status', '已完成', 'DONE', 'green', 2, 1, 1, NOW(), NOW()),
('ecom_auto_category', '订单', 'ORDER', 'blue', 1, 1, 1, NOW(), NOW()),
('ecom_auto_category', '库存与补货', 'STOCK', 'orange', 2, 1, 1, NOW(), NOW()),
('ecom_auto_category', '客服与评价', 'CS', 'magenta', 3, 1, 1, NOW(), NOW()),
('ecom_auto_category', '商品监控', 'LISTING', 'red', 4, 1, 1, NOW(), NOW()),
('ecom_auto_category', '店铺与渠道', 'STORE', 'purple', 5, 1, 1, NOW(), NOW()),
('ecom_auto_category', '头程物流', 'LOGISTICS', 'cyan', 6, 1, 1, NOW(), NOW()),
('ecom_auto_category', '广告', 'ADS', 'volcano', 7, 1, 1, NOW(), NOW()),
('ecom_auto_category', 'FBA', 'FBA', 'gold', 8, 1, 1, NOW(), NOW()),
('ecom_auto_category', '价格', 'PRICE', 'green', 9, 1, 1, NOW(), NOW()),
('ecom_price_change_source', '自动化规则', 'AUTOMATION', 'geekblue', 5, 1, 1, NOW(), NOW());
-- 应收应付来源：电商平台费用（平台结算确认时按费用科目生成的红字应收，借 销售费用等 / 贷 应收账款）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_bill_source', '电商平台费用', 'PLATFORM_FEE', 'orange', 30, 1, NOW(), NOW());

-- ===== 第五轮修复：业务正确性与安全（fixa） =====
-- 供应商对账改为内部登记供应商的确认结果（不再依赖供应商门户）：登记人
ALTER TABLE fin_supplier_statement ADD COLUMN confirm_by BIGINT DEFAULT NULL COMMENT '登记供应商对账结果的内部人员';
-- 内置行政流程：抄送节点原指向不存在的角色 11（人事专员），出差改为抄送会计、用印改为抄送总经理；
-- 出差、用印、物品领用、通用审批 4 个流程由应用启动时发布（WfBuiltinPublisher），可直接在「发起申请」中发起
UPDATE wf_model SET design_json = REPLACE(design_json, '{"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}',
        '{"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}'),
    remark = '内置流程（初始化即发布）：部门主管 → 出差 ≥ 5 天或费用 ≥ 1 万元时上级主管 → 抄送会计。可在「发起申请」中直接发起'
WHERE model_key = 'oa_travel';
UPDATE wf_model SET design_json = REPLACE(design_json, '{"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}',
        '{"id": "cc_gm", "type": "CC", "name": "抄送总经理", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}'),
    remark = '内置流程（初始化即发布）：部门主管 → 公章、合同章时总经理审批，其他印章抄送总经理。可在「发起申请」中直接发起'
WHERE model_key = 'oa_seal';
UPDATE wf_model SET remark = '内置流程（初始化即发布）：部门主管 → 抄送仓库管理员。可在「发起申请」中直接发起' WHERE model_key = 'oa_goods';
UPDATE wf_model SET remark = '内置流程（初始化即发布）：部门主管 → 上级主管（依次审批）。可在「发起申请」中直接发起' WHERE model_key = 'oa_general';
-- 销售订单、订单变更的内置流程（与其他业务单据一样未发布；发布后订单「审核」/ 订单变更改为提交审批）
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time)
SELECT 'scm_sales_order', '销售订单审批', 'SALES', 2, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按订单金额", "next": {"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 10 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "100000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}',
       '内置流程（未发布）：部门主管 → 金额 ≥ 10 万元时总经理 → 抄送会计。发布后销售订单「审核」改为提交审批，审批通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()
FROM (SELECT 1 AS x) t WHERE NOT EXISTS (SELECT 1 FROM wf_model WHERE model_key = 'scm_sales_order');
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time)
SELECT 'scm_order_change', '订单变更审批', 'PURCHASE', 3, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "delta_check", "type": "CONDITION", "name": "按变更金额", "next": null, "branches": [{"id": "delta_check_yes", "name": "金额增加 ≥ 1 万元", "defaultBranch": false, "conditions": [[{"field": "amountDelta", "operator": "GE", "value": "10000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "delta_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}',
       '内置流程（未发布）：部门主管 → 变更后金额增加 ≥ 1 万元时总经理。发布后已审核的采购 / 销售订单变更改为提交审批，审批通过后生效；可在流程设计中调整审批人后发布', NOW(), NOW()
FROM (SELECT 1 AS x) t WHERE NOT EXISTS (SELECT 1 FROM wf_model WHERE model_key = 'scm_order_change');
