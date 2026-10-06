package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Orders;
import com.campus.service.entity.PaymentLog;
import com.campus.service.service.PayService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

// 闲置交易的站内模拟支付（三段式：下单 → 付款 → 服务端回调）
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    // ① 下单 POST /unified-order  body: {goodsId}
    @OpLog("创建闲置订单")
    @PostMapping("/unified-order")
    public Result<?> unifiedOrder(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Object gid = body.get("goodsId");
        if (gid == null) return Result.fail("缺少物品ID");
        Map<String, Object> res = payService.unifiedOrder(userId, Long.valueOf(gid.toString()));
        if (res.containsKey("error")) return Result.fail((String) res.get("error"));
        return Result.ok(res);
    }

    // ③ 模拟支付回调 POST /notify  body: {orderNo, channel}
    @OpLog("模拟支付回调")
    @PostMapping("/notify")
    public Result<?> notifyPay(@RequestBody Map<String, Object> body) {
        Object orderNo = body.get("orderNo");
        if (orderNo == null) return Result.fail("缺少订单号");
        String channel = body.get("channel") == null ? "" : body.get("channel").toString();
        boolean ok = payService.notifyPay(orderNo.toString(), channel);
        return ok ? Result.ok() : Result.fail("订单不存在");
    }

    // 我的订单（买 + 卖）GET /order-list
    @GetMapping("/order-list")
    public Result<List<Orders>> orderList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(payService.myOrders(userId));
    }

    // 订单详情 GET /order/{orderId}
    @GetMapping("/order/{orderId}")
    public Result<Orders> detail(@PathVariable Long orderId) {
        Orders o = payService.detail(orderId);
        return o == null ? Result.fail("订单不存在") : Result.ok(o);
    }

    // 支付流水 GET /log/{orderId}
    @GetMapping("/log/{orderId}")
    public Result<PaymentLog> log(@PathVariable Long orderId) {
        PaymentLog log = payService.getLog(orderId);
        return log == null ? Result.fail("流水不存在") : Result.ok(log);
    }

    // 卖家发货 POST /ship/{orderId}
    @PostMapping("/ship/{orderId}")
    public Result<?> ship(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        String err = payService.ship(orderId, userId);
        return err == null ? Result.ok() : Result.fail(err);
    }

    // 买家确认收货 POST /confirm/{orderId}
    @PostMapping("/confirm/{orderId}")
    public Result<?> confirm(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        String err = payService.confirm(orderId, userId);
        return err == null ? Result.ok() : Result.fail(err);
    }

    // 取消订单 POST /cancel/{orderId}
    @PostMapping("/cancel/{orderId}")
    public Result<?> cancel(HttpServletRequest request, @PathVariable Long orderId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        String err = payService.cancel(orderId, userId);
        return err == null ? Result.ok() : Result.fail(err);
    }
}
