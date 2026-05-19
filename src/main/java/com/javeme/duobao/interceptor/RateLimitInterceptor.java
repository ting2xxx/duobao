package com.javeme.duobao.interceptor;

import com.javeme.duobao.common.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;


/**
 * Rate Limit Interceptor
 * Prevent single user form overwhelming your server by clicking a button too many times in one seconds
 * Allow a maximum of 5 clicks per second. Reset the count every second
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = BaseContext.getCurrentID();
        if (userId == null) {
            return true;
        }

        Long currentSecond = System.currentTimeMillis() / 1000;
        //if User ID 15 clicks three times at exactly 10:05:01 AM,
        // then all three clicks share the exact same key: "rate:user:15:1684321501"
        String key = "rate:user:" + userId + ":" + currentSecond;

        //save the count
        Long count = stringRedisTemplate.opsForValue().increment(key);

        //delete the key count every 2 seconds
        if (count != null && count  == 1) {
            stringRedisTemplate.expire(key, 2, TimeUnit.SECONDS);
        }

        //if the count is more than 5, for example 6, it will show error message
        if (count != null && count > 5) {
            response.setStatus(429);
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
        return true;
    }
}
