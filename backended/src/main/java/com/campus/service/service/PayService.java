package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.Goods;
import com.campus.service.entity.Orders;
import com.campus.service.entity.PaymentLog;
import com.campus.service.mapper.GoodsMapper;
import com.campus.service.mapper.OrderMapper;
import com.campus.service.mapper.PaymentLogMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 闲置交易的站内模拟支付。
 * 照搬真实支付的三段式：① 下单拿 prepay_id → ② 用户在自绘支付页选渠道 → ③ 服务端回调确认后才改状态。
 * 个人主体小程序开不了微信支付，所以把"第三方支付服务"换成系统内的模拟服务，其余结构一致。
 */
@Service
public class PayService {

    private static final int ST_UNPAID = 0;
    private static final int ST_PAID = 1;
    private static final int ST_SHIPPED = 2;
    private static final int ST_DONE = 3;
    private static final int ST_CANCELED = 4;

    private static final int TIMEOUT_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final GoodsMapper goodsMapper;
    private final NotificationService notificationService;

    public PayService(OrderMapper orderMapper, PaymentLogMapper paymentLogMapper,
                      GoodsMapper goodsMapper, NotificationService notificationService) {
        this.orderMapper = orderMapper;
        this.paymentLogMapper = paymentLogMapper;
        this.goodsMapper = goodsMapper;
        this.notificationService = notificationService;
    }

    // ① 下单：建订单 + 建流水(已创建) + 生成模拟 prepay_id
    @Transactional
    public Map<String, Object> unifiedOrder(Long buyerId, Long goodsId) {
        Map<String, Object> res = new HashMap<>();
        Goods g = goodsMapper.selectById(goodsId);
        if (g == null) { res.put("error", "物品不存在"); return res; }
        if (g.getStatus() == null || g.getStatus() != 0) { res.put("error", "该物品已售出或已下架"); return res; }
        if (g.getUserId().equals(buyerId)) { res.put("error", "不能购买自己发布的物品"); return res; }

        // 幂等：同一买家对同一物品已有待付款订单，直接复用，不重复建单
        Orders exist = orderMapper.selectOne(new QueryWrapper<Orders>()
                .eq("goods_id", goodsId).eq("buyer_id", buyerId).eq("status", ST_UNPAID));
        if (exist != null) {
            res.put("order", exist);
            res.put("prepayId", prepayIdOf(exist.getOrderId()));
            return res;
        }

        Orders o = new Orders();
        o.setOrderNo(genOrderNo());
        o.setGoodsId(goodsId);
        o.setBuyerId(buyerId);
        o.setSellerId(g.getUserId());
        o.setAmount(g.getPrice());
        o.setStatus(ST_UNPAID);
        o.setPayChannel("");
        orderMapper.insert(o);

        PaymentLog log = new PaymentLog();
        log.setOrderId(o.getOrderId());
        log.setOrderNo(o.getOrderNo());
        log.setPrepayId("MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        log.setChannel("");
        log.setAmount(g.getPrice());
        log.setStatus(0);
        log.setTradeNo("");
        log.setNotifyBody("");
        paymentLogMapper.insert(log);

        res.put("order", o);
        res.put("prepayId", log.getPrepayId());
        return res;
    }

    private String prepayIdOf(Long orderId) {
        PaymentLog log = paymentLogMapper.selectOne(new QueryWrapper<PaymentLog>().eq("order_id", orderId));
        return log == null ? "" : log.getPrepayId();
    }

    // ③ 模拟支付回调：校验订单号 → 改订单状态 → 回写流水。重复回调不重复改状态（幂等）
    @Transactional
    public boolean notifyPay(String orderNo, String channel) {
        Orders o = orderMapper.selectOne(new QueryWrapper<Orders>().eq("order_no", orderNo));
        if (o == null) return false;
        if (o.getStatus() != ST_UNPAID) return true;

        o.setStatus(ST_PAID);
        o.setPayChannel(channel == null ? "" : channel);
        o.setPayTime(LocalDateTime.now());
        orderMapper.updateById(o);

        PaymentLog log = paymentLogMapper.selectOne(new QueryWrapper<PaymentLog>().eq("order_id", o.getOrderId()));
        if (log != null) {
            log.setStatus(1);
            log.setChannel(channel == null ? "" : channel);
            log.setTradeNo("MOCK_TRADE_" + System.currentTimeMillis());
            log.setNotifyBody("{\"orderNo\":\"" + orderNo + "\",\"channel\":\"" + channel + "\",\"result\":\"SUCCESS\"}");
            paymentLogMapper.updateById(log);
        }

        // 付款成功才把物品置为已售出并绑定买家
        Goods g = goodsMapper.selectById(o.getGoodsId());
        if (g != null && g.getStatus() != null && g.getStatus() == 0) {
            g.setStatus(1);
            g.setBuyerId(o.getBuyerId());
            goodsMapper.updateById(g);
        }

        notificationService.send(o.getSellerId(), 7, "订单已付款",
                "买家已付款，请尽快发货。订单号：" + o.getOrderNo(), o.getOrderId());
        return true;
    }

    // 卖家发货 1→2
    public String ship(Long orderId, Long userId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null) return "订单不存在";
        if (!o.getSellerId().equals(userId)) return "无权操作";
        if (o.getStatus() != ST_PAID) return "只有已付款的订单可以发货";
        o.setStatus(ST_SHIPPED);
        orderMapper.updateById(o);
        notificationService.send(o.getBuyerId(), 7, "卖家已发货",
                "订单 " + o.getOrderNo() + " 已发货，请及时确认收货", orderId);
        return null;
    }

    // 买家确认收货 2→3
    public String confirm(Long orderId, Long userId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null) return "订单不存在";
        if (!o.getBuyerId().equals(userId)) return "无权操作";
        if (o.getStatus() != ST_SHIPPED) return "只有已发货的订单可以确认收货";
        o.setStatus(ST_DONE);
        o.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(o);
        notificationService.send(o.getSellerId(), 7, "交易完成",
                "订单 " + o.getOrderNo() + " 买家已确认收货", orderId);
        return null;
    }

    // 取消：仅未付款的订单可取消
    public String cancel(Long orderId, Long userId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null) return "订单不存在";
        if (!o.getBuyerId().equals(userId) && !o.getSellerId().equals(userId)) return "无权操作";
        if (o.getStatus() != ST_UNPAID) return "已付款的订单不能直接取消";
        o.setStatus(ST_CANCELED);
        o.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return null;
    }

    public List<Orders> myOrders(Long userId) {
        List<Orders> list = orderMapper.selectMine(userId);
        // 补上物品标题与首图，省得前端再逐条查
        for (Orders o : list) {
            Goods g = goodsMapper.selectById(o.getGoodsId());
            if (g == null) continue;
            o.setGoodsTitle(g.getTitle());
            String images = g.getImages();
            if (images != null && images.length() > 2) {
                int end = images.indexOf('"', 1);
                int start = images.indexOf('"') + 1;
                if (start > 0 && end > start) o.setGoodsImage(images.substring(start, end));
            }
        }
        return list;
    }

    public Orders detail(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    public PaymentLog getLog(Long orderId) {
        return paymentLogMapper.selectOne(new QueryWrapper<PaymentLog>().eq("order_id", orderId));
    }

    // 超时未支付自动取消：每 5 分钟扫一次，超过 30 分钟未付款的置为已取消
    @Scheduled(fixedRate = 300000)
    public void autoCancelTimeoutOrders() {
        List<Orders> list = orderMapper.selectList(new QueryWrapper<Orders>()
                .eq("status", ST_UNPAID)
                .lt("create_time", LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES)));
        for (Orders o : list) {
            o.setStatus(ST_CANCELED);
            o.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(o);
        }
    }

    private String genOrderNo() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
