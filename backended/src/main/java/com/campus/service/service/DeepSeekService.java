package com.campus.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DeepSeekService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 统一生成入口：goods/task/team
    public String generate(String type, String title, String category, String price, String description) {
        String prompt;
        if ("task".equals(type)) {
            prompt = String.format(
                "你是一个校园跑腿任务文案助手。请为以下跑腿任务写一段吸引人接单的简短描述（60字以内）：\n任务：%s\n类型：%s\n跑腿费：%s元\n备注：%s\n\n要求：简洁明了，突出跑腿轻松和报酬，符合校园风格。",
                title, category, price, description);
        } else if ("team".equals(type)) {
            prompt = String.format(
                "你是一个校园组局活动文案助手。请为以下组局写一段吸引人加入的文案（80字以内）：\n主题：%s\n标签：%s\n描述：%s\n\n要求：热情活泼，突出活动亮点，符合大学生社交风格。",
                title, category, description);
        } else {
            prompt = String.format(
                "你是一个闲置物品销售文案助手。请为以下闲置物品写一段吸引人的卖点推荐语（80字以内）：\n物品：%s\n分类：%s\n价格：%s元\n卖家的描述：%s\n\n要求：生动有趣，突出卖点，符合大学生二手交易风格。",
                title, category, price, description);
        }
        return callDeepSeek(prompt);
    }

    // 保留旧方法兼容
    public String generateGoodsRecommendation(String title, String category, String price, String description) {
        return generate("goods", title, category, price, description);
    }

    //第二步：调用DeepSeek API
    private String callDeepSeek(String prompt) {
        try {
            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);// 设置请求头
            headers.set("Authorization", "Bearer " + apiKey);// 1. 拼请求头（API Key），创建请求体
            // 创建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "deepseek-chat");// 2. 拼请求体（model + messages + max_tokens）
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.add(msg);
            requestBody.set("messages", messages);
            requestBody.put("max_tokens", 300);
            requestBody.put("temperature", 0.7);
            // 创建请求实体
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);// 3. 发送HTTP请求
            JsonNode json = objectMapper.readTree(response);
            // 4.解析返回的 JSON，拿到 AI 生成的文案
            return json.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            return "[AI生成失败，请手动填写]";
        }
    }
}
