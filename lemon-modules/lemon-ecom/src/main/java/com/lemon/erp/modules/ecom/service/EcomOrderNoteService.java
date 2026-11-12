package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderNote;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单内部备注：每次添加一条历史（备注人、时间），订单上的备注（列表、导出显示）为最新一条（超过 255 字截断）
 */
@Service
@RequiredArgsConstructor
public class EcomOrderNoteService {

    private static final int REMARK_MAX = 255;

    private final EcomOrderNoteMapper noteMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderService orderService;

    public List<EcomOrderNote> list(Long orderId) {
        orderService.require(orderId);
        return noteMapper.selectByOrderId(orderId);
    }

    @Transactional
    public Long add(Long orderId, String content) {
        orderService.require(orderId);
        String c = content.trim();
        EcomOrderNote n = new EcomOrderNote();
        n.setOrderId(orderId);
        n.setContent(c);
        noteMapper.insert(n);
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getRemark, c.length() <= REMARK_MAX ? c : c.substring(0, REMARK_MAX))
                .eq(EcomOrder::getId, orderId));
        return n.getId();
    }
}
