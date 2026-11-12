package com.lemon.erp.modules.ecom.demo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据生成进度（当前租户）
 *
 * @param generated 是否已经生成过（存在 DEMO- 开头的店铺）
 * @param running   是否正在生成
 * @param current   正在执行的组成部分标识（如 orders）
 * @param done      已执行完的组成部分数（含失败的）
 * @param total     组成部分总数
 * @param failures  失败的组成部分及原因
 * @param startTime 本次开始时间
 * @param endTime   本次结束时间（进行中为空）
 */
public record DemoStatus(boolean generated, boolean running, String current, int done, int total, List<Failure> failures,
                         LocalDateTime startTime, LocalDateTime endTime) {

    /** 失败的组成部分 */
    public record Failure(String key, String message) {
    }
}
