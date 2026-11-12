package com.lemon.erp.modules.ecom.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReport;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/** 运营日报 */
public interface EcomOpsReportMapper extends BaseMapper<EcomOpsReport> {

    /**
     * 分页列表（不含结构化内容与 AI 建议）：只列出店铺全部可见的日报（店铺范围子查询受店铺数据权限过滤，行数与 store_count 相等才可见）；
     * storeId 只看包含该店铺的日报
     */
    IPage<EcomOpsReport> selectPageList(IPage<EcomOpsReport> page, @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                        @Param("storeId") Long storeId);

    /** 按 ID 读取（店铺全部可见时），含内容 */
    EcomOpsReport selectVisible(@Param("id") Long id);
}
