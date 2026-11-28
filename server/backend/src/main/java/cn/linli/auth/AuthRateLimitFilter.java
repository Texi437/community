package cn.linli.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Single-instance, bounded, fixed-window protection for password endpoints. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final ObjectMapper json;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();
    private long nextCleanup;

    public AuthRateLimitFilter(ObjectMapper json, Clock clock) { this.json = json; this.clock = clock; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        // MockMvc and servlet containers may expose the mapped path differently.
        if (path.isEmpty()) { path = request.getRequestURI().substring(request.getContextPath().length()); }
        if ("POST".equals(request.getMethod()) && (path.equals("/api/auth/login") || path.equals("/api/auth/register"))) {
            int retry = acquire(path + ":" + request.getRemoteAddr(), path.endsWith("login") ? 20 : 10);
            if (retry > 0) {
                response.setHeader("Retry-After", Integer.toString(retry));
                SecurityErrors.write(json, response, 429, "TOO_MANY_REQUESTS", "操作过于频繁，请稍后重试");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private synchronized int acquire(String key, int limit) {
        long now = clock.instant().getEpochSecond();
        if (now >= nextCleanup) {
            windows.values().removeIf(w -> w.until <= now);
            nextCleanup = now + 60;
        }
        Window window = windows.get(key);
        if (window == null || window.until <= now) {
            if (window == null && windows.size() >= 10_000) { return 60; }
            window = new Window(now + 60);
            windows.put(key, window);
        }
        if (window.count >= limit) { return (int)(window.until - now); }
        window.count++;
        return 0;
    }

    private static class Window {
        final long until;
        int count;
        Window(long until) { this.until = until; }
    }
}
