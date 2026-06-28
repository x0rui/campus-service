package com.campus.service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 登录流程：
 * 前端 wx.login() → code → 后端换 openid → 查库登录/注册 →
 * 生成 JWT token（含 userId, role）→ 返回前端 → 前端存本地缓存 →
 * 每次请求放请求头 → 拦截器解析 token 拿 userId → Controller 直接用
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}") // 从application.yml读配置
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 生成token
    /**
     * JWT 令牌里面存了三样东西：
     * {
     *   "userId": 1,      ← 用户ID
     *   "openid": "xxx",  ← 微信身份标识
     *   "role": 0,        ← 角色（学生/管理员）
     *   "exp": "2026-..." ← 过期时间
     * }
     */
    public String generateToken(Long userId, String openid, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);// 用户ID
        claims.put("openid", openid);// 微信openid
        claims.put("role", role);// 角色 0学生/1社团管理员/2系统管理员
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())// 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration))// 过期时间
                .signWith(SignatureAlgorithm.HS256, secret) // 使用HS256算法和密钥生成JWT令牌
                .compact();
    }

    // 解析token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    // 判断token是否过期
    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // 获取用户ID
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    // 获取用户角色
    public Integer getRoleFromToken(String token) {
        return parseToken(token).get("role", Integer.class);
    }
}
