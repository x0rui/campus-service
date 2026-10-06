package com.campus.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 腾讯位置服务封装。
 * 目前用到：逆地址解析（经纬度 → 文字地址），用于地图选点后回填地址。
 * 距离排序不用它 —— 两边经纬度都在库里，本地按球面距离算更快、不耗额度。
 */
@Service
public class TencentMapService {

    @Value("${tencent.map.key}")
    private String key;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 坐标 → 文字地址
    public String reverseGeocode(double lat, double lng) {
        String url = "https://apis.map.qq.com/ws/geocoder/v1/?location=" + lat + "," + lng + "&key=" + key;
        try {
            String body = get(url);
            if (body == null) return "";
            JsonNode node = objectMapper.readTree(body);
            if (node.path("status").asInt(-1) != 0) return "";
            return node.path("result").path("address").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    // 步行距离（米）：给单条任务做精确距离时用，列表排序仍用本地球面距离
    public Integer walkingDistance(double fromLat, double fromLng, double toLat, double toLng) {
        String url = "https://apis.map.qq.com/ws/distance/v1/matrix?mode=walking"
                + "&from=" + fromLat + "," + fromLng
                + "&to=" + toLat + "," + toLng
                + "&key=" + key;
        try {
            String body = get(url);
            if (body == null) return null;
            JsonNode node = objectMapper.readTree(body);
            if (node.path("status").asInt(-1) != 0) return null;
            JsonNode el = node.path("result").path("rows").path(0).path("elements").path(0).path("distance");
            return el.isMissingNode() ? null : el.asInt();
        } catch (Exception e) {
            return null;
        }
    }

    private String get(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
