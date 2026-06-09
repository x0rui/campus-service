package com.campus.service.service;

import com.campus.service.entity.SensitiveWord;
import com.campus.service.mapper.SensitiveWordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class PosterService {

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.api-url}")
    private String apiUrl;

    @Value("${dashscope.model:qwen-image-plus}")
    private String modelName;

    private final OssService ossService;
    private final SensitiveWordService sensitiveWordService;
    private final RestTemplate restTemplate;

    public PosterService(OssService ossService, SensitiveWordService sensitiveWordService) {
        this.ossService = ossService;
        this.sensitiveWordService = sensitiveWordService;
        this.restTemplate = new RestTemplate();
    }

    public String generate(String prompt) {
        // 1. 敏感词检查（用内存 HashSet，不走 MySQL）
        List<String> hits = sensitiveWordService.check(prompt);
        if (!hits.isEmpty()) {
            throw new RuntimeException("提示词包含敏感词: " + String.join(",", hits));
        }

        // 2. 拼装质量词
        String fullPrompt = "竖版海报背景，色彩明亮，" + prompt + "，唯美风格";

        // 3. 调用 API
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);

            // qwen-image 系列使用 messages 格式
            Map<String, Object> input = new HashMap<>();
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            List<Map<String, String>> content = new ArrayList<>();
            Map<String, String> textContent = new HashMap<>();
            textContent.put("text", fullPrompt);
            content.add(textContent);
            userMsg.put("content", content);
            messages.add(userMsg);
            input.put("messages", messages);
            requestBody.put("input", input);

            Map<String, Object> params = new HashMap<>();
            params.put("size", "720*1280");
            params.put("n", 1);
            requestBody.put("parameters", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) response.getBody().get("output");
                // 异步任务
                String taskId = (String) output.get("task_id");
                if (taskId != null) {
                    return pollTask(taskId);
                }
                // 同步结果
                String result = extractImageUrl(output);
                if (result != null) return result;
            }

            return getDefaultPoster();

        } catch (Exception e) {
            System.err.println("===== 错误: " + e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException he = (org.springframework.web.client.HttpClientErrorException) e;
                System.err.println("===== 响应体: " + he.getResponseBodyAsString());
            }
            e.printStackTrace();
            return getDefaultPoster();
        }
    }

    private String extractImageUrl(Map<String, Object> output) {
        // 旧版 wanx 格式: output.results[0].url
        List<Map<String, Object>> results = (List<Map<String, Object>>) output.get("results");
        if (results != null && !results.isEmpty()) {
            Object url = results.get(0).get("url");
            if (url != null) return ossService.uploadFromUrl(url.toString());
        }
        // qwen-image 格式: output.choices[0].message.content[0].image
        List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message != null) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) message.get("content");
                if (content != null && !content.isEmpty()) {
                    // 直接 key 为 "image"
                    Object image = content.get(0).get("image");
                    if (image != null) return ossService.uploadFromUrl(image.toString());
                    // 也可能是 "image_url": {"url": "..."}
                    Object imageUrl = content.get(0).get("image_url");
                    if (imageUrl instanceof Map) {
                        Object urlStr = ((Map<String, Object>) imageUrl).get("url");
                        if (urlStr != null) return ossService.uploadFromUrl(urlStr.toString());
                    }
                }
            }
        }
        // output.images[0].url
        List<Map<String, Object>> images = (List<Map<String, Object>>) output.get("images");
        if (images != null && !images.isEmpty()) {
            Object url = images.get(0).get("url");
            if (url != null) return ossService.uploadFromUrl(url.toString());
        }
        return null;
    }

    private String pollTask(String taskId) {
        String taskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;
        try {
            for (int i = 0; i < 30; i++) {
                Thread.sleep(2000);
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                HttpEntity<?> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(taskUrl, HttpMethod.GET, entity, Map.class);

                if (response.getBody() != null && response.getBody().containsKey("output")) {
                    Map<String, Object> output = (Map<String, Object>) response.getBody().get("output");
                    String status = (String) output.get("task_status");
                    if ("SUCCEEDED".equals(status)) {
                        String url = extractImageUrl(output);
                        if (url != null) return url;
                    } else if ("FAILED".equals(status)) {
                        return getDefaultPoster();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getDefaultPoster();
    }

    private String getDefaultPoster() {
        return "";
    }
}
