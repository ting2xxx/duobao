package com.javeme.duobao.configuration;

import com.javeme.duobao.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/users/login",
                        "/api/users/register",
                        "/api/upload",
                        "/swagger-ui/**",  // <-- Allow Swagger UI
                        "/v3/api-docs/**", // <-- Allow Swagger API Docs
                        "/swagger-ui.html"
                );


        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/orders/submit")
                .addPathPatterns("/api/payments/pay/**");
    }
}
