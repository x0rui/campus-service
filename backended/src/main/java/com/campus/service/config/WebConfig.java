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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("OPTIONS".equals(request.getMethod())) {
                    return true;
                }
                String path = request.getRequestURI();

                // 无需登录的公开接口
                if (path.startsWith("/api/user/login") || path.startsWith("/ws/")) {
                    return true;
                }

                // OSS上传需要登录
                if (path.startsWith("/api/oss/")) {
                    String token = request.getHeader("Authorization");
                    if (token == null || token.isEmpty() || jwtUtil.isTokenExpired(token)) {
                        response.setStatus(401);
                        return false;
                    }
                    setUserAttr(request, token);
                    return true;
                }

                // AI接口需要登录
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
                if ("GET".equals(request.getMethod())) {
                    String token = request.getHeader("Authorization");
                    if (token != null && !token.isEmpty() && !jwtUtil.isTokenExpired(token)) {
                        setUserAttr(request, token);
                    }
                    return true;
                }

                // POST/PUT/DELETE 需要登录
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
                        response.setStatus(403);
                        return false;
                    }
                } catch (Exception ignored) {}
                setUserAttr(request, token);
                return true;
            }

            private void setUserAttr(HttpServletRequest request, String token) {
                request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
                request.setAttribute("role", jwtUtil.getRoleFromToken(token));
                request.setAttribute("token", token);
            }
        }).addPathPatterns("/api/**");
    }
}
