package com.lemon.erp.modules.ecom.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.automation.dto.AutoLogQuery;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoLog;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EcomAutoLogMapper extends BaseMapper<EcomAutoLog> {

    IPage<EcomAutoLog> selectPageList(IPage<EcomAutoLog> page, @Param("q") AutoLogQuery q);

    /** 规则对某对象在某时间之后执行了动作的次数（防抖） */
    long countExecuted(@Param("ruleId") Long ruleId, @Param("objectKey") String objectKey, @Param("since") LocalDateTime since);

    /** 规则在某时间之后执行了动作的次数（每日上限） */
    long countRuleExecuted(@Param("ruleId") Long ruleId, @Param("since") LocalDateTime since);

    /** 规则在某时间之后某种结果的条数 */
    long countRuleResult(@Param("ruleId") Long ruleId, @Param("result") String result, @Param("since") LocalDateTime since);

    /** 按结果计数（日志页统计） */
    List<Map<String, Object>> countByResult(@Param("since") LocalDateTime since, @Param("ruleId") Long ruleId);

    /** 按天统计执行次数与失败次数 */
    List<Map<String, Object>> countByDay(@Param("since") LocalDateTime since, @Param("ruleId") Long ruleId);

    /** 某触发器最近的事件（试运行），按对象去重前的原始日志 */
    List<EcomAutoLog> selectRecentEvents(@Param("trigger") AutoTrigger trigger, @Param("limit") int limit);

    /** 清理某时间之前的日志 */
    int deleteBefore(@Param("before") LocalDateTime before);
}
