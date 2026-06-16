package com.navisow.docusign.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple IP-based rate limiter using a sliding-window counter — no extra dependencies.
 *
 *   POST /api/documents           — 20 uploads / minute per IP
 *   POST /api/webhook/docusign    — 200 webhook deliveries / minute per IP
 *
 * For multi-node deployments replace with a Redis-backed distributed counter.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int UPLOAD_RPM = 20;
    private static final int WEBHOOK_RPM = 200;
    private static final long WINDOW_MS = 60_000L;

    /** Maps IP -> timestamps of recent requests within the window. */
    private final Map<String, Deque<Long>> uploadWindows = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> webhookWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = resolveIp(request);

        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/documents")) {
            if (!allow(ip, uploadWindows, UPLOAD_RPM)) {
                reject(response);
                return;
            }
        } else if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/webhook/docusign")) {
            if (!allow(ip, webhookWindows, WEBHOOK_RPM)) {
                reject(response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean allow(String ip, Map<String, Deque<Long>> registry, int limit) {
        long now = Instant.now().toEpochMilli();
        long cutoff = now - WINDOW_MS;

        Deque<Long> timestamps = registry.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        // Remove stale timestamps
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= limit) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Please slow down.\"}");
    }

    private static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
