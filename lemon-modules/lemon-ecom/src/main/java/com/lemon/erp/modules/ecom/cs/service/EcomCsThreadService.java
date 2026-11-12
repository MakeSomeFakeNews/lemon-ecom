package com.lemon.erp.modules.ecom.cs.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.cs.connector.CsCapability;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnector;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnectorRegistry;
import com.lemon.erp.modules.ecom.cs.connector.MessageDraft;
import com.lemon.erp.modules.ecom.cs.connector.SentMessage;
import com.lemon.erp.modules.ecom.cs.connector.ThreadDraft;
import com.lemon.erp.modules.ecom.cs.connector.ThreadRef;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.dto.ThreadQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessage;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.model.CsConstants;
import com.lemon.erp.modules.ecom.cs.model.CsDirection;
import com.lemon.erp.modules.ecom.cs.model.CsSendStatus;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsThreadStatus;
import com.lemon.erp.modules.ecom.dto.OrderQuery;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomCsTicketMapper;
import com.lemon.erp.modules.ecom.mapper.EcomMessageMapper;
import com.lemon.erp.modules.ecom.mapper.EcomMessageThreadMapper;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 买家消息（统一收件箱）：会话列表、消息、回复（调用平台接口发送，失败可重发）、负责人 / 标签 / 关闭、关联订单；
 * 同步写入会话与消息（按 会话 + 平台消息 ID 去重）并重算状态与回复截止。
 * <p>
 * 状态规则：最近一条已发送的卖家消息之后还有买家消息为待回复，回复截止 = 其中最早一条的时间 + 平台回复时限（{@link MessageConnector#replyHours()}），
 * 过了截止仍未回复为超时；否则为已回复。已关闭的会话收到新的买家消息时重新变为待回复。
 * 平台接口在事务之外调用：回复先保存为发送中，调用成功改为已发送、失败记录原因
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomCsThreadService {

    private static final int PREVIEW_MAX = 500;

    private final EcomMessageThreadMapper threadMapper;
    private final EcomMessageMapper messageMapper;
    private final EcomCsTicketMapper ticketMapper;
    private final EcomStoreService storeService;
    private final EcomOrderService orderService;
    private final MessageConnectorRegistry registry;
    private final EcomCsSupport support;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomMessageThread> page(long current, long size, ThreadQuery q) {
        q.setNow(LocalDateTime.now());
        Page<EcomMessageThread> page = Page.of(current, size);
        threadMapper.selectPageList(page, q);
        return PageResult.of(page);
    }

    /** 各状态的会话数与超时数（筛选标签页）：PENDING、REPLIED、CLOSED、OVERDUE、ALL */
    public Map<String, Long> counts(ThreadQuery q) {
        q.setNow(LocalDateTime.now());
        Map<String, Long> out = new LinkedHashMap<>();
        long all = 0, overdue = 0;
        for (CsThreadStatus s : CsThreadStatus.values()) {
            out.put(s.name(), 0L);
        }
        for (Map<String, Object> row : threadMapper.countByStatus(q)) {
            long cnt = ((Number) row.get("cnt")).longValue();
            out.put(String.valueOf(row.get("status")), cnt);
            all += cnt;
            overdue += row.get("overdue") == null ? 0 : ((Number) row.get("overdue")).longValue();
        }
        out.put("OVERDUE", overdue);
        out.put("ALL", all);
        return out;
    }

    public EcomMessageThread require(Long id) {
        EcomMessageThread t = threadMapper.selectDetail(id, LocalDateTime.now());
        if (t == null) {
            throw new BizException("ecom.error.cs.threadNotFound");
        }
        return t;
    }

    /** 会话详情：消息按时间升序，平台是否支持发送，回复时限，售后工单 */
    public CsViews.ThreadDetail detail(Long id) {
        EcomMessageThread t = require(id);
        MessageConnector c = registry.find(t.getPlatform()).orElse(null);
        boolean canSend = c != null && c.supports(CsCapability.SEND_MESSAGE);
        return new CsViews.ThreadDetail(t, messageMapper.selectByThread(id), canSend, replyHours(t),
                ticketMapper.selectBySource(CsSourceType.THREAD, id));
    }

    /** 订单侧栏：关联订单（按订单的数据权限，没有权限或已删除时为空）与该买家在店铺中的最近 5 张订单 */
    public CsViews.OrderPanel orderPanel(Long id) {
        EcomMessageThread t = require(id);
        EcomOrder order = null;
        if (t.getOrderId() != null) {
            try {
                order = orderService.detail(t.getOrderId());
            } catch (BizException e) {
                order = null;
            }
        }
        List<EcomOrder> recent = List.of();
        if (StringUtils.hasText(t.getBuyerName())) {
            OrderQuery q = new OrderQuery();
            q.setStoreId(t.getStoreId());
            q.setKeyword(t.getBuyerName());
            recent = orderService.page(1, 5, q).records();
        }
        return new CsViews.OrderPanel(order, recent);
    }

    // ---------------------------------------------------------------- 处理

    /** 打开会话后标记已读：本地未读清零，平台支持时同步标记（失败只记日志） */
    public void markRead(Long id) {
        EcomMessageThread t = require(id);
        if (t.getUnread() != null && t.getUnread() > 0) {
            threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getUnread, 0).eq(EcomMessageThread::getId, id));
        }
        MessageConnector c = registry.find(t.getPlatform()).orElse(null);
        if (c == null || t.getUnread() == null || t.getUnread() == 0) {
            return;
        }
        try {
            EcomStore store = storeService.get(t.getStoreId());
            c.markRead(support.prepare(store), ref(t));
        } catch (RuntimeException e) {
            log.warn("Marking conversation {} read failed: {}", t.getConversationId(), e.getMessage());
        }
    }

    public void update(Long id, CsRequests.ThreadUpdate req) {
        require(id);
        threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getOwnerId, req.ownerId())
                .set(EcomMessageThread::getTags, normalizeTags(req.tags())).set(EcomMessageThread::getRemark, blankToNull(req.remark()))
                .eq(EcomMessageThread::getId, id));
    }

    /** 关闭会话（不需要回复的消息，如系统通知、买家致谢），不再计入待回复和超时 */
    public void close(Long id) {
        require(id);
        threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getStatus, CsThreadStatus.CLOSED)
                .set(EcomMessageThread::getReplyDeadline, null).eq(EcomMessageThread::getId, id));
    }

    /** 重新打开：按消息重算状态 */
    @Transactional
    public void reopen(Long id) {
        EcomMessageThread t = require(id);
        threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getStatus, CsThreadStatus.REPLIED)
                .eq(EcomMessageThread::getId, id));
        t.setStatus(CsThreadStatus.REPLIED);
        refreshState(t, replyHours(t), false, null);
    }

    /** 关联订单（店铺内的平台订单号），为空取消关联 */
    public void linkOrder(Long id, String platformOrderNo) {
        EcomMessageThread t = require(id);
        Long orderId = null;
        String no = null;
        if (StringUtils.hasText(platformOrderNo)) {
            EcomOrder o = support.findOrder(t.getStoreId(), platformOrderNo);
            if (o == null) {
                throw new BizException("ecom.error.cs.orderNotInStore", platformOrderNo.trim());
            }
            orderId = o.getId();
            no = o.getPlatformOrderNo();
        }
        threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getOrderId, orderId)
                .set(EcomMessageThread::getPlatformOrderNo, no).eq(EcomMessageThread::getId, id));
    }

    /**
     * 回复买家：recordOnly 时只登记（已在平台后台回复）；否则调用平台接口发送（不在事务中），返回保存的消息（失败时状态为发送失败并带原因）
     */
    public EcomMessage reply(Long id, CsRequests.Reply req) {
        EcomMessageThread t = require(id);
        String content = req.content().trim();
        EcomMessage m = new EcomMessage();
        m.setThreadId(id);
        m.setDirection(CsDirection.SELLER);
        m.setMsgType("TEXT");
        m.setContent(content);
        m.setSentTime(LocalDateTime.now());
        m.setTemplateId(req.templateId());
        m.setSenderId(SecurityUtils.getUserIdOrNull());
        if (Boolean.TRUE.equals(req.recordOnly())) {
            m.setSendStatus(CsSendStatus.SENT);
            transactionTemplate.executeWithoutResult(s -> {
                messageMapper.insert(m);
                refreshState(t, replyHours(t), false, null);
            });
            return m;
        }
        MessageConnector c = sender(t);
        ConnectorContext ctx = support.prepare(storeService.get(t.getStoreId()));
        m.setSendStatus(CsSendStatus.SENDING);
        messageMapper.insert(m);
        return send(t, c, ctx, m);
    }

    /** 重新发送发送失败的回复 */
    public EcomMessage resend(Long messageId) {
        EcomMessage m = messageMapper.selectById(messageId);
        if (m == null || m.getDirection() != CsDirection.SELLER) {
            throw new BizException("ecom.error.cs.messageNotFound");
        }
        if (m.getSendStatus() != CsSendStatus.FAILED) {
            throw new BizException("ecom.error.cs.messageNotFailed");
        }
        EcomMessageThread t = require(m.getThreadId());
        MessageConnector c = sender(t);
        ConnectorContext ctx = support.prepare(storeService.get(t.getStoreId()));
        messageMapper.update(Wrappers.<EcomMessage>lambdaUpdate().set(EcomMessage::getSendStatus, CsSendStatus.SENDING)
                .set(EcomMessage::getErrorMessage, null).eq(EcomMessage::getId, messageId));
        return send(t, c, ctx, m);
    }

    private MessageConnector sender(EcomMessageThread t) {
        MessageConnector c = registry.find(t.getPlatform()).orElse(null);
        if (c == null || !c.supports(CsCapability.SEND_MESSAGE)) {
            throw new BizException("ecom.error.cs.sendUnsupported", t.getPlatform().name());
        }
        return c;
    }

    private EcomMessage send(EcomMessageThread t, MessageConnector c, ConnectorContext ctx, EcomMessage m) {
        try {
            SentMessage sent = c.sendMessage(ctx, ref(t), m.getContent());
            m.setPlatformMessageId(sent.messageId());
            if (sent.sentTime() != null) {
                m.setSentTime(sent.sentTime());
            }
            m.setSendStatus(CsSendStatus.SENT);
            m.setErrorMessage(null);
        } catch (RuntimeException e) {
            log.warn("Sending reply to conversation {} failed: {}", t.getConversationId(), e.getMessage());
            m.setSendStatus(CsSendStatus.FAILED);
            m.setErrorMessage(HttpJson.abbreviate(e.getMessage()));
        }
        transactionTemplate.executeWithoutResult(s -> {
            messageMapper.update(Wrappers.<EcomMessage>lambdaUpdate().set(EcomMessage::getSendStatus, m.getSendStatus())
                    .set(EcomMessage::getPlatformMessageId, m.getPlatformMessageId()).set(EcomMessage::getSentTime, m.getSentTime())
                    .set(EcomMessage::getErrorMessage, m.getErrorMessage()).eq(EcomMessage::getId, m.getId()));
            if (m.getSendStatus() == CsSendStatus.SENT) {
                refreshState(t, replyHours(t), false, null);
            }
        });
        return m;
    }

    // ---------------------------------------------------------------- 同步写入

    /** 一个会话写入的结果：新增的消息数、是否新会话 */
    public record SaveResult(int newMessages, boolean newThread) {
    }

    /**
     * 写入同步到的会话（调用方每个会话一个事务）：新会话的负责人为店铺负责人；消息按平台消息 ID 去重；
     * 关联订单取消息中的订单号，没有时按买家名称取最近一张订单；未读数取平台值，没有时累加新的买家消息；最后重算状态与回复截止
     */
    @Transactional
    public SaveResult saveThread(EcomStore store, ThreadDraft d, int replyHours) {
        EcomMessageThread t = threadMapper.selectOne(Wrappers.<EcomMessageThread>lambdaQuery().eq(EcomMessageThread::getStoreId, store.getId())
                .eq(EcomMessageThread::getConversationId, d.conversationId()));
        boolean created = t == null;
        if (created) {
            t = new EcomMessageThread();
            t.setStoreId(store.getId());
            t.setPlatform(store.getPlatform());
            t.setConversationId(d.conversationId());
            t.setOwnerId(store.getOwnerId());
            t.setStatus(CsThreadStatus.REPLIED);
            t.setUnread(0);
        }
        if (StringUtils.hasText(d.buyerId())) {
            t.setBuyerId(d.buyerId());
        }
        if (StringUtils.hasText(d.buyerName())) {
            t.setBuyerName(cut(d.buyerName(), 128));
        }
        if (StringUtils.hasText(d.subject())) {
            t.setSubject(cut(d.subject(), 255));
        }
        if (t.getOrderId() == null) {
            EcomOrder o = support.findOrder(store.getId(), d.platformOrderNo());
            if (o == null) {
                o = support.latestOrderOfBuyer(store.getId(), t.getBuyerName());
            }
            if (o != null) {
                t.setOrderId(o.getId());
                t.setPlatformOrderNo(o.getPlatformOrderNo());
            } else if (StringUtils.hasText(d.platformOrderNo())) {
                t.setPlatformOrderNo(cut(d.platformOrderNo(), 64));
            }
        }
        if (created) {
            threadMapper.insert(t);
        } else {
            threadMapper.updateById(t);
        }
        // 去重：平台消息 ID 已存在的跳过（同一批中重复的也只写一次）
        Set<String> ids = new HashSet<>();
        d.messages().stream().map(MessageDraft::messageId).filter(Objects::nonNull).forEach(ids::add);
        Set<String> existing = new HashSet<>();
        if (!created && !ids.isEmpty()) {
            messageMapper.selectList(Wrappers.<EcomMessage>lambdaQuery().select(EcomMessage::getPlatformMessageId)
                    .eq(EcomMessage::getThreadId, t.getId()).in(EcomMessage::getPlatformMessageId, ids))
                    .forEach(m -> existing.add(m.getPlatformMessageId()));
        }
        List<EcomMessage> fresh = new ArrayList<>();
        int newBuyer = 0;
        LocalDateTime firstNewBuyer = null;
        for (MessageDraft md : d.messages()) {
            if (md.messageId() == null || !existing.add(md.messageId())) {
                continue;
            }
            EcomMessage m = new EcomMessage();
            m.setThreadId(t.getId());
            m.setPlatformMessageId(cut(md.messageId(), 128));
            m.setDirection(md.direction() == null ? CsDirection.BUYER : md.direction());
            m.setMsgType(cut(md.type() == null ? "TEXT" : md.type(), 32));
            m.setContent(cut(md.content(), 4000));
            m.setAttachments(md.attachments() == null || md.attachments().isEmpty() ? null
                    : cut(HttpJson.MAPPER.writeValueAsString(md.attachments()), 2000));
            m.setSentTime(md.sentTime() == null ? LocalDateTime.now() : md.sentTime());
            m.setSendStatus(m.getDirection() == CsDirection.SELLER ? CsSendStatus.SENT : null);
            fresh.add(m);
            if (m.getDirection() == CsDirection.BUYER) {
                newBuyer++;
                firstNewBuyer = firstNewBuyer == null || m.getSentTime().isBefore(firstNewBuyer) ? m.getSentTime() : firstNewBuyer;
            }
        }
        if (!fresh.isEmpty()) {
            messageMapper.insertBatch(fresh);
        }
        int unread = d.unreadCount() != null ? d.unreadCount() : (t.getUnread() == null ? 0 : t.getUnread()) + newBuyer;
        threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getUnread, unread).eq(EcomMessageThread::getId, t.getId()));
        // 已关闭的会话收到新消息：重新打开，回复截止从新消息算起（关闭前的消息视为已处理）
        // 库里的时间只精确到秒，起点也截到秒，否则刚写入的消息会被判为早于起点
        LocalDateTime floor = t.getStatus() == CsThreadStatus.CLOSED && firstNewBuyer != null
                ? firstNewBuyer.truncatedTo(java.time.temporal.ChronoUnit.SECONDS) : null;
        refreshState(t, replyHours, newBuyer > 0, floor);
        return new SaveResult(fresh.size(), created);
    }

    /**
     * 按消息重算最近消息、状态与回复截止
     *
     * @param newBuyerMessages 本次有新的买家消息（已关闭的会话据此重新打开）
     * @param floor            未回复的买家消息只从这个时间算起（重新打开的会话），为空不限
     */
    void refreshState(EcomMessageThread t, int replyHours, boolean newBuyerMessages, LocalDateTime floor) {
        EcomMessage latest = Queries.first(messageMapper, Wrappers.<EcomMessage>lambdaQuery().eq(EcomMessage::getThreadId, t.getId())
                .and(w -> w.eq(EcomMessage::getDirection, CsDirection.BUYER).or().eq(EcomMessage::getSendStatus, CsSendStatus.SENT))
                .orderByDesc(EcomMessage::getSentTime).orderByDesc(EcomMessage::getId));
        EcomMessage lastSeller = Queries.first(messageMapper, Wrappers.<EcomMessage>lambdaQuery().eq(EcomMessage::getThreadId, t.getId())
                .eq(EcomMessage::getDirection, CsDirection.SELLER).eq(EcomMessage::getSendStatus, CsSendStatus.SENT)
                .orderByDesc(EcomMessage::getSentTime).orderByDesc(EcomMessage::getId));
        EcomMessage firstUnreplied = Queries.first(messageMapper, Wrappers.<EcomMessage>lambdaQuery().eq(EcomMessage::getThreadId, t.getId())
                .eq(EcomMessage::getDirection, CsDirection.BUYER)
                .gt(lastSeller != null, EcomMessage::getSentTime, lastSeller == null ? null : lastSeller.getSentTime())
                .ge(floor != null, EcomMessage::getSentTime, floor)
                .orderByAsc(EcomMessage::getSentTime).orderByAsc(EcomMessage::getId));
        CsThreadStatus status;
        LocalDateTime deadline = null;
        if (t.getStatus() == CsThreadStatus.CLOSED && !newBuyerMessages) {
            status = CsThreadStatus.CLOSED;
        } else if (firstUnreplied != null) {
            status = CsThreadStatus.PENDING;
            deadline = deadline(firstUnreplied.getSentTime(), replyHours);
        } else {
            status = CsThreadStatus.REPLIED;
        }
        threadMapper.update(Wrappers.<EcomMessageThread>lambdaUpdate().set(EcomMessageThread::getStatus, status)
                .set(EcomMessageThread::getReplyDeadline, deadline)
                .set(latest != null, EcomMessageThread::getLastMessage, latest == null ? null : cut(preview(latest), PREVIEW_MAX))
                .set(latest != null, EcomMessageThread::getLastMessageTime, latest == null ? null : latest.getSentTime())
                .eq(EcomMessageThread::getId, t.getId()));
    }

    /** 回复截止 = 最早一条未回复的买家消息时间 + 回复时限（小时） */
    public static LocalDateTime deadline(LocalDateTime firstUnreplied, int replyHours) {
        return firstUnreplied == null ? null : firstUnreplied.plusHours(replyHours);
    }

    // ---------------------------------------------------------------- 工具

    int replyHours(EcomMessageThread t) {
        return registry.find(t.getPlatform()).map(MessageConnector::replyHours).orElse(CsConstants.DEFAULT_REPLY_HOURS);
    }

    private ThreadRef ref(EcomMessageThread t) {
        EcomMessage lastBuyer = Queries.first(messageMapper, Wrappers.<EcomMessage>lambdaQuery().eq(EcomMessage::getThreadId, t.getId())
                .eq(EcomMessage::getDirection, CsDirection.BUYER).isNotNull(EcomMessage::getPlatformMessageId)
                .orderByDesc(EcomMessage::getSentTime).orderByDesc(EcomMessage::getId));
        return new ThreadRef(t.getConversationId(), t.getBuyerId(), t.getPlatformOrderNo(), lastBuyer == null ? null : lastBuyer.getPlatformMessageId());
    }

    private static String preview(EcomMessage m) {
        return StringUtils.hasText(m.getContent()) ? m.getContent() : "[" + m.getMsgType() + "]";
    }

    private static String normalizeTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (String s : tags.split("[,，]")) {
            String v = s.trim();
            if (!v.isEmpty() && !list.contains(v)) {
                list.add(v);
            }
        }
        return list.isEmpty() ? null : String.join(",", list);
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
