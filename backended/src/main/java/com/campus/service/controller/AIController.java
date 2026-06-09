package com.campus.service.controller;

import com.campus.service.dto.Result;
import com.campus.service.service.DeepSeekService;
import com.campus.service.service.PosterService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final DeepSeekService deepSeekService;
    private final PosterService posterService;

    public AIController(DeepSeekService deepSeekService, PosterService posterService) {
        this.deepSeekService = deepSeekService;
        this.posterService = posterService;
    }

    // 统一AI生成接口
    @PostMapping("/generate")
    public Result<String> generate(@RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "goods");
        String title = body.getOrDefault("title", "");
        String category = body.getOrDefault("category", "");
        String price = body.getOrDefault("price", "0");
        String description = body.getOrDefault("description", "");
        if (title.isEmpty()) return Result.fail("请输入标题");
        return Result.ok(deepSeekService.generate(type, title, category, price, description));
    }

    // 生成海报背景图
    @PostMapping("/poster")
    public Result<String> poster(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return Result.fail("请输入提示词");
        }
        try {
            String imageUrl = posterService.generate(prompt.trim());
            if (imageUrl.isEmpty()) {
                return Result.fail("生成失败，请稍后重试");
            }
            return Result.ok(imageUrl);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
