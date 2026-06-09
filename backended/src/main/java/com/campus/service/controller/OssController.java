package com.campus.service.controller;

import com.campus.service.dto.Result;
import com.campus.service.service.OssService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
/*
后端收文件 → 传阿里云 OSS → 返回 signedUrl → 前端显示 → 大文件用前端直传
 */
@RestController
@RequestMapping("/api/oss")
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    // 上传图片
    // 接收文件 → 调Service → 返回URL
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        String url = ossService.uploadImage(file); // 调用Service上传
        return Result.ok(url); // 返回图片URL
    }

    @GetMapping("/sign-url")
    public Result<String> signUrl(@RequestParam("key") String key) {
        String url = ossService.getSignedUrl(key);
        return Result.ok(url);
    }
}
