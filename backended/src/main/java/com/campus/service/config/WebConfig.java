package com.campus.service.config;

import com.campus.service.entity.User;
import com.campus.service.mapper.UserMapper;
import com.campus.service.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public WebConfig(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    // 拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

            // 预处理请求
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("OPTIONS".equals(request.getMethod())) {
                    return true;
                }
                String path = request.getRequestURI();

                // 无需登录的公开接口，登录接口和 WebSocket 路径放行（不需要登录）
                if (path.startsWith("/api/user/login") || path.startsWith("/ws/")) {
                    return true;
                }

                // OSS上传需要登录（防止被滥用）
                if (path.startsWith("/api/oss/")) {
                    String token = request.getHeader("Authorization");
                    if (token == null || token.isEmpty() || jwtUtil.isTokenExpired(token)) {
                        response.setStatus(401);
                        return false;
                    }
                    setUserAttr(request, token);
                    return true;
                }

                // AI接口需要登录（防止 API 额度被盗刷）
                if (path.startsWith("/api/ai/")) {
                    String token = request.getHeader("Authorization");
                    if (token == null || token.isEmpty() || jwtUtil.isTokenExpired(token)) {
                        response.setStatus(401);
                        return false;
                    }
                    setUserAttr(request, token);
                    return true;
                }

                // GET 请求：解析token但不强制，各Controller自行判断是否需要登录
                /**
                 * GET 请求不强制登录的原因
                 * 因为前端有很多不登录也能看的页面（首页商品列表、跑腿广场、组局广场等）。
                 * 如果 GET 也强制登录，游客就打不开了
                 */
                if ("GET".equals(request.getMethod())) {
                    String token = request.getHeader("Authorization");
                    if (token != null && !token.isEmpty() && !jwtUtil.isTokenExpired(token)) {
                        setUserAttr(request, token);
                    }
                    return true;
                }

                // POST/PUT/DELETE  强制登录（写操作必须认证）
                String token = request.getHeader("Authorization");
                if (token == null || token.isEmpty()) {
                    response.setStatus(401);
                    return false;
                }
                if (jwtUtil.isTokenExpired(token)) {
                    response.setStatus(401);
                    return false;
                }
                // 检查用户是否被禁用
                try {
                    Long uid = jwtUtil.getUserIdFromToken(token);
                    User u = userMapper.selectById(uid);
                    if (u != null && u.getStatus() != null && u.getStatus() == 1) {
                        response.setStatus(403);// 禁用用户返回 403
                        return false;
                    }
                } catch (Exception ignored) {}
                setUserAttr(request, token);
                return true;
            }

            // 设置用户属性
            private void setUserAttr(HttpServletRequest request, String token) {
                request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
                request.setAttribute("role", jwtUtil.getRoleFromToken(token));
                request.setAttribute("token", token);
            }
        }).addPathPatterns("/api/**");
    }
}
