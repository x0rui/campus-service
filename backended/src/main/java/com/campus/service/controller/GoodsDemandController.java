package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Goods;
import com.campus.service.entity.GoodsDemand;
import com.campus.service.service.GoodsDemandService;
import com.campus.service.service.GoodsService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

// 求购（闲置侧的"需求发布 → 匹配在售物品"）
@RestController
@RequestMapping("/api/goods-demand")
public class GoodsDemandController {

    private final GoodsDemandService demandService;
    private final GoodsService goodsService;

    public GoodsDemandController(GoodsDemandService demandService, GoodsService goodsService) {
        this.demandService = demandService;
        this.goodsService = goodsService;
    }

    // 1. 发布求购 POST /publish
    @OpLog("发布求购")
    @PostMapping("/publish")
    public Result<?> publish(HttpServletRequest request, @Valid @RequestBody GoodsDemand demand) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        demand.setUserId(userId);
        String text = demand.getTitle() + " " + (demand.getDescription() != null ? demand.getDescription() : "");
        if (!goodsService.checkSensitiveWords(text).isEmpty()) {
            return Result.fail("内容包含违规信息，请修改");
        }
        return Result.ok(demandService.publish(demand));
    }

    // 2. 求购广场 GET /list
    @GetMapping("/list")
    public Result<List<GoodsDemand>> list(@RequestParam(defaultValue = "0") int page) {
        return Result.ok(demandService.list(page));
    }

    // 3. 匹配结果 GET /match/{id}
    @GetMapping("/match/{demandId}")
    public Result<List<Goods>> match(@PathVariable Long demandId) {
        return Result.ok(demandService.matchGoods(demandId, 20));
    }

    // 4. 我发布的 GET /my
    @GetMapping("/my")
    public Result<List<GoodsDemand>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(demandService.getMy(userId));
    }

    // 5. 改状态 PUT /status/{id}  body: {status:1已找到|2已关闭}
    @PutMapping("/status/{demandId}")
    public Result<?> updateStatus(HttpServletRequest request, @PathVariable Long demandId,
                                  @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Object s = body.get("status");
        if (s == null) return Result.fail("缺少状态");
        Integer status = Integer.valueOf(s.toString());
        if (status != 1 && status != 2) return Result.fail("状态只能是1已找到或2已关闭");
        return demandService.updateStatus(demandId, userId, status) ? Result.ok() : Result.fail("无权操作");
    }
}
