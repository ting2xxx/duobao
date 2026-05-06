package com.javeme.duobao.configuration;

import com.javeme.duobao.common.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = BaseContext.getCurrentID();
        if (userId == null) {
            return true;
        }

        String key = "rate:user:" + userId + ":" + (System.currentTimeMillis());
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count  == 1) {
            stringRedisTemplate.expire(key, 2, TimeUnit.SECONDS);
        }

        if (count != null && count > 5) {
            response.setStatus(429);
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
        return true;
    }
}
