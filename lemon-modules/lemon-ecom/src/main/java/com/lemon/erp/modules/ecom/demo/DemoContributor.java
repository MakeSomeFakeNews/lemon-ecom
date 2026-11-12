package com.lemon.erp.modules.ecom.demo;

/**
 * 演示数据的一个组成部分（Spring Bean）。{@link DemoDataService} 按 {@link #order()} 从小到大依次调用，
 * 每个组成部分在自己的事务中执行，一个失败只记录日志、不影响后续部分。
 * <p>
 * 约定：
 * <ul>
 *   <li>0–99 为基础数据（仓库、物料、供应商、店铺、在线商品、订单、货件、结算等），由 demo 包内的基础组成部分生成；
 *       各业务功能的组成部分使用 100 及以上，需要的基础数据通过所属 Service 查询（如按店铺编码 DEMO- 开头查询店铺），
 *       也可以读取基础部分放入 {@link DemoContext} 的共享值（键见 {@link DemoKeys}）。</li>
 *   <li>业务数据尽量经过所属 Service 写入（保证库存、凭证、状态一致），历史日期通过业务日期字段体现。</li>
 *   <li>随机数一律用 {@link DemoContext#random()}（固定种子），保证每次生成的数据一致。</li>
 * </ul>
 */
public interface DemoContributor {

    /** 执行顺序，越小越先执行 */
    int order();

    /** 标识（日志与进度显示用），如 "stores"、"orders" */
    String key();

    /** 生成本部分的演示数据 */
    void generate(DemoContext ctx);
}
