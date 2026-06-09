package tr.com.hacettepe.tams.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tr.com.hacettepe.tams.api_gateway.config.RateLimitProperties;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory sliding-window rate limiter keyed by client IP address.
 *
 * <p>Each IP is allowed up to {@code burstCapacity} concurrent tokens. One token is
 * restored every {@code 1000 / requestsPerSecond} milliseconds, capped at burst
 * capacity. Requests that find no available token receive {@code 429 Too Many Requests}.
 *
 * <p>This implementation is intentionally simple — suitable for a single-node MVP.
 * For multi-replica deployments a Redis-backed rate limiter (e.g. Bucket4j + Redis)
 * should replace this.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    /** Stores available token counts per client IP. */
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp,
                k -> new TokenBucket(rateLimitProperties.burstCapacity(),
                        rateLimitProperties.requestsPerSecond()));

        if (!bucket.tryConsume()) {
            writeTooManyRequests(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Too Many Requests");
        problem.setDetail("Rate limit exceeded. Please slow down.");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now().toString());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }

    /**
     * Token bucket implementation using a single atomic counter and refill-on-read approach.
     * Thread-safe for concurrent requests from the same IP.
     */
    static class TokenBucket {

        private final int capacity;
        private final long refillIntervalNs;
        private final AtomicInteger tokens;
        private volatile long lastRefillNs;

        TokenBucket(int capacity, int requestsPerSecond) {
            this.capacity = capacity;
            this.refillIntervalNs = 1_000_000_000L / requestsPerSecond;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillNs = System.nanoTime();
        }

        boolean tryConsume() {
            refill();
            int current;
            do {
                current = tokens.get();
                if (current <= 0) {
                    return false;
                }
            } while (!tokens.compareAndSet(current, current - 1));
            return true;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNs;
            if (elapsed >= refillIntervalNs) {
                long tokensToAdd = elapsed / refillIntervalNs;
                lastRefillNs = now;
                tokens.updateAndGet(t -> (int) Math.min(capacity, t + tokensToAdd));
            }
        }
    }
}
