package com.javeme.duobao.interceptor;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    /**
     * To preHandle for JTW Interceptor
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. Grab the token from the request header
        String token = request.getHeader("Authorization");

        //if token is null or empty, return false
        if (token == null  || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        //if token start with Bearer, use substring to skip the Bearer part
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

    /**
     * To afterCompletion for JTW Interceptor
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.clear(); //clear the threadLocal
    }
}
