package com.example.gateway;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Value("${RATE_LIMIT_PER_MINUTE:60}")
    private long limit;

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter(limit, Duration.ofMinutes(1)));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
