package com.campus.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WechatService {

    private static final Logger log = LoggerFactory.getLogger(WechatService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${wechat.dev-mode:false}")
    private boolean devMode;

    /**
     * 调用微信 jscode2session 接口，用code换取openid
     * 真机环境返回真实openid；开发工具模拟器code无效时，dev模式下用hash兜底
     */
    public String getOpenId(String code) {
        // 未配置AppID时直接走兜底（本地开发）
        if (appId == null || appId.isEmpty() || appId.startsWith("your_")) {
            log.warn("微信AppID未配置");
            return devFallback(code);
        }

        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid=" + appId
                    + "&secret=" + appSecret
                    + "&js_code=" + code
                    + "&grant_type=authorization_code";
            String resp = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(resp);

            // 检查微信API返回的错误码
            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                int errcode = json.get("errcode").asInt();
                String errmsg = json.has("errmsg") ? json.get("errmsg").asText() : "";
                log.warn("微信API返回错误: errcode={}, errmsg={}", errcode, errmsg);
                return devFallback(code);   // code 无效通常是开发者工具模拟器
            }

            if (json.has("openid") && !json.get("openid").asText().isEmpty()) {
                String openid = json.get("openid").asText();
                log.info("微信登录成功，openid={}", openid.substring(0, Math.min(8, openid.length())) + "...");
                return openid;
            }
        } catch (Exception e) {
            log.warn("调用微信API网络异常: {}", e.getMessage());
        }
        return devFallback(code);
    }

    /**
     * 兜底：只有 dev-mode=true（本地开发者工具模拟器）才用 code 的 hash 造一个稳定的假 openid，
     * 让模拟器能登录；dev-mode=false（真机/生产）一律返回 null，由调用方拒绝登录。
     */
    private String devFallback(String code) {
        if (!devMode) return null;
        log.info("dev-mode：code 换不到 openid，使用 hash 兜底");
        return "wx_" + Integer.toHexString(code.hashCode());
    }
}
