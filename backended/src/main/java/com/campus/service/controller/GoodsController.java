package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Goods;
import javax.validation.Valid;
import com.campus.service.service.GoodsService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {
    // 物品服务
    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    // 1. 发布物品 POST /publish
    @OpLog("发布闲置物品")
    @PostMapping("/publish")
    public Result<?> publish(HttpServletRequest request, @Valid @RequestBody Goods goods) {
        Long userId = (Long) request.getAttribute("userId");
        goods.setUserId(userId);

        String checkText = goods.getTitle() + " " + (goods.getDescription() != null ? goods.getDescription() : "");
        List<String> hits = goodsService.checkSensitiveWords(checkText);
        if (!hits.isEmpty()) {
            return Result.fail("发布内容包含违规信息，请修改");
        }
        if (goods.getPrice() != null && goods.getPrice().doubleValue() <= 0) {
            return Result.fail("请输入有效价格");
        }
        goodsService.publish(goods);
        return Result.ok(goods);
    }

    // 2. 物品列表 GET /list?page=0
    @GetMapping("/list")
    public Result<List<Goods>> list(@RequestParam(defaultValue = "0") int page) {
        return Result.ok(goodsService.getIndexList(page));
    }

    // 3. 分类列表 GET /category/{category}
    @GetMapping("/category/{category}")
    public Result<List<Goods>> byCategory(@PathVariable String category) {
        return Result.ok(goodsService.getByCategory(category));
    }

    // 4. 搜索物品 GET /search?keyword=xxx
    @GetMapping("/search")
    public Result<List<Goods>> search(@RequestParam String keyword) {
        return Result.ok(goodsService.search(keyword));
    }

    // 5. 物品详情 GET /detail/{goodsId}
    @GetMapping("/detail/{goodsId}")
    public Result<Goods> detail(HttpServletRequest request, @PathVariable Long goodsId,
                                @RequestParam(required = false) String poll) {
        Long userId = (Long) request.getAttribute("userId");
        boolean skipView = "1".equals(poll);
        Goods goods = goodsService.getDetail(goodsId, userId, skipView);
        if (goods == null) {
            return Result.fail("物品不存在");
        }
        return Result.ok(goods);
    }

    // 6. 我的物品 GET /my
    @GetMapping("/my")
    public Result<List<Goods>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(goodsService.getUserGoods(userId));
    }

    // 7. 我买的 GET /bought
    @GetMapping("/bought")
    public Result<List<Goods>> bought(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(goodsService.getBoughtGoods(userId));
    }

    // 7. 修改物品状态 PUT /status/{goodsId}
    @OpLog("更新物品状态")
    @PutMapping("/status/{goodsId}")
    public Result<?> updateStatus(HttpServletRequest request, @PathVariable Long goodsId, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Integer status = Integer.valueOf(body.get("status").toString());
        Object buyerIdObj = body.get("buyerId");
        Long buyerId = buyerIdObj != null ? Long.valueOf(buyerIdObj.toString()) : null;
        String err = goodsService.updateStatus(goodsId, status, buyerId, userId);
        return err == null ? Result.ok() : Result.fail(err);
    }

    // 8. 修改物品 PUT /{goodsId}
    @PutMapping("/{goodsId}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long goodsId, @RequestBody Goods goods) {
        Long userId = (Long) request.getAttribute("userId");
        boolean ok = goodsService.updateGoods(goodsId, userId, goods);
        return ok ? Result.ok("修改成功") : Result.fail("操作失败，仅可修改在售物品");
    }

    // 9. 检查敏感词 POST /check-words
    @PostMapping("/check-words")
    public Result<?> checkWords(@RequestBody Goods goods) {
        String text = goods.getTitle() + " " + (goods.getDescription() != null ? goods.getDescription() : "");
        List<String> hits = goodsService.checkSensitiveWords(text);
        if (!hits.isEmpty()) {
            return Result.fail("内容包含违规信息: " + String.join(",", hits));
        }
        return Result.ok();
    }
}
