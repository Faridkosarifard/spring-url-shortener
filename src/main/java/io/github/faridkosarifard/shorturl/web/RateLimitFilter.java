package io.github.faridkosarifard.shorturl.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.faridkosarifard.shorturl.config.ShortUrlProperties;
import io.github.faridkosarifard.shorturl.support.InMemoryRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ShortUrlProperties properties;
    private final InMemoryRateLimiter limiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ShortUrlProperties properties, Clock clock, ObjectMapper objectMapper) {
        this.properties = properties;
        this.limiter = new InMemoryRateLimiter(properties.rateLimit().requestsPerMinute(), WINDOW, clock);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.rateLimit().enabled()) {
            return true;
        }
        String method = request.getMethod();
        if (!"POST".equals(method) && !"DELETE".equals(method)) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        InMemoryRateLimiter.Result result = limiter.tryAcquire(clientKey(request));
        response.setHeader("X-RateLimit-Limit", Integer.toString(result.limit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(result.remaining()));
        if (!result.allowed()) {
            long retryAfterSeconds = Math.max(1, result.retryAfter().toSeconds());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again later.");
            problem.setTitle("Too many requests");
            objectMapper.writeValue(response.getWriter(), problem);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        if (properties.rateLimit().trustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                String first = comma >= 0 ? forwarded.substring(0, comma) : forwarded;
                return first.strip();
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
