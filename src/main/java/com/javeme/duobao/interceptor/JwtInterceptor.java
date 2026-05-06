package com.javeme.duobao.interceptor;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. Grab the token from the request header
        String token = request.getHeader("Authorization");

        if (token == null  || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        if (token.startsWith("Bearer")) {
            token = token.substring(7);
        }

        try {
            // We decrypt the token to find out exactly who is making this request.
            Long userId = JwtUtil.parseToken(token);
            BaseContext.setUser(userId);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.clear();
    }
}
