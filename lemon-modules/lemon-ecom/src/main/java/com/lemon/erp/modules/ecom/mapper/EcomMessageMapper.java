package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 买家消息（客服）
 */
public interface EcomMessageMapper extends BaseMapper<EcomMessage> {

    /** 会话的消息（含回复人名称），按发送时间升序 */
    List<EcomMessage> selectByThread(@Param("threadId") Long threadId);

    /** 批量写入同步到的消息 */
    int insertBatch(@Param("items") List<EcomMessage> items);
}
