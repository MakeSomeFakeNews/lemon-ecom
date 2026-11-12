package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dev.dto.DevProjectQuery;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevProject;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomDevProjectMapper extends BaseMapper<EcomDevProject> {

    /** 分页：带分类、负责人、物料名称，样品数与样品费合计 */
    IPage<EcomDevProject> selectPageList(IPage<EcomDevProject> page, @Param("q") DevProjectQuery query);

    /** 看板：按条件的全部项目（不分页，最多 limit 条，按更新时间倒序） */
    List<EcomDevProject> selectBoard(@Param("q") DevProjectQuery query, @Param("limit") int limit);

    /** 各状态的项目数（status, cnt） */
    List<Map<String, Object>> selectStatusCounts(@Param("q") DevProjectQuery query);

    EcomDevProject selectDetail(@Param("id") Long id);
}
