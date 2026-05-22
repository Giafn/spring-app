package com.tecnicaltest.spring_app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnicaltest.spring_app.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final Map<String, Queue<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null) {
                String key = getClientIp(request) + ":" + request.getMethod() + ":" + request.getRequestURI().replaceAll("/\\d+", "/{id}");
                long now = System.currentTimeMillis();
                long windowStart = now - (rateLimit.windowSeconds() * 1000L);

                Queue<Long> timestamps = requestTimestamps.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

                synchronized (timestamps) {
                    while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                        timestamps.poll();
                    }

                    if (timestamps.size() >= rateLimit.maxRequests()) {
                        response.setContentType("application/json");
                        response.setStatus(429);
                        var apiResponse = ApiResponse.error(429, "Too Many Requests",
                                "Too many requests. Please try again later.");
                        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                        return false;
                    }

                    request.setAttribute("rateLimitKey", key);
                    request.setAttribute("rateLimitTimestamps", timestamps);
                    request.setAttribute("rateLimitNow", now);
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null && response.getStatus() < 400) {
                @SuppressWarnings("unchecked")
                Queue<Long> timestamps = (Queue<Long>) request.getAttribute("rateLimitTimestamps");
                Long now = (Long) request.getAttribute("rateLimitNow");
                if (timestamps != null && now != null) {
                    synchronized (timestamps) {
                        long windowStart = now - (rateLimit.windowSeconds() * 1000L);
                        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                            timestamps.poll();
                        }
                        timestamps.add(now);
                    }
                }
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
