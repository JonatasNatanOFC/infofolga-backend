package com.infoway.infofolga.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    // 100 requisições por minuto (60000ms)
    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60000L;

    // Rastreia requisições por IP: IP -> [timestamps]
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);

        RequestCounter counter = counters.computeIfAbsent(clientIp, ip -> new RequestCounter());

        if (counter.allowRequest()) {
            return true;
        }

        // Muitas requisições
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        try {
            response.getWriter().write("{\"erro\":\"Muitas requisições. Máximo 100 por minuto.\",\"status\":429}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Contador simples por IP
    private static class RequestCounter {
        private long lastRequestTime = 0;
        private int requestCount = 0;

        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();

            // Se passou o intervalo de 1 minuto, resetar contador
            if (now - lastRequestTime > WINDOW_MS) {
                lastRequestTime = now;
                requestCount = 1;
                return true;
            }

            // Dentro da janela de 1 minuto
            requestCount++;
            return requestCount <= MAX_REQUESTS;
        }
    }
}
