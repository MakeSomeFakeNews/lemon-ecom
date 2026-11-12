package com.lemon.erp.modules.ecom.ads;

/**
 * 轮询异步报告时的等待（测试中替换为不真正休眠的实现）
 */
@FunctionalInterface
public interface AdsSleeper {

    AdsSleeper DEFAULT = Thread::sleep;

    void sleep(long millis) throws InterruptedException;
}
