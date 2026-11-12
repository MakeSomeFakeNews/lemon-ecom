package com.lemon.erp.modules.ecom.assistant.model;

/** 运营日报的待处理事项：column 为汇总查询的列名，link 为页面地址（带预设筛选） */
public enum OpsTodoKind {
    ORDER_OVERDUE("orderOverdue", "/ecom/order?overdue=1"),
    ORDER_EXCEPTION("orderException", "/ecom/order?localStatus=EXCEPTION"),
    ORDER_PENDING("orderPending", "/ecom/order?localStatus=PENDING"),
    RETURN_PENDING("returnPending", "/ecom/return?status=PENDING"),
    REVIEW_PENDING("reviewPending", "/ecom/cs-review?handleStatus=PENDING"),
    CLAIM_EXPIRING("claimExpiring", "/ecom/fba-claim?status=PENDING");

    private final String column;
    private final String link;

    OpsTodoKind(String column, String link) {
        this.column = column;
        this.link = link;
    }

    public String column() {
        return column;
    }

    public String link() {
        return link;
    }
}
