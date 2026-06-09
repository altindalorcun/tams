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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tr.com.hacettepe.tams.api_gateway.security.JwtUtil;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Global JWT authentication filter.
 *
 * <p>Runs before every request. Behaviour:
 * <ul>
 *   <li>Paths under {@code /api/v1/auth/**} and management endpoints pass through without a token.</li>
 *   <li>All other requests must carry a valid {@code Authorization: Bearer <token>} header.
 *       Invalid or absent tokens result in a {@code 401 Unauthorized} RFC 9457 problem response.</li>
 *   <li>Valid tokens are parsed and the user identity ({@code X-User-Id}, {@code X-User-Role})
 *       is injected as request headers so downstream services do not re-validate the JWT.</li>
 * </ul>
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final String ACTUATOR_PATH_PREFIX = "/actuator/";
    private static final String SWAGGER_UI_PATH = "/swagger-ui";
    private static final String API_DOCS_PATH = "/api-docs";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request);
        if (token == null || !jwtUtil.isValid(token)) {
            writeUnauthorized(response);
            return;
        }

        String userId = jwtUtil.extractSubject(token);
        String role = jwtUtil.extractRole(token);

        HttpServletRequest enrichedRequest = new HeaderEnrichedRequestWrapper(request, userId, role);
        chain.doFilter(enrichedRequest, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith(AUTH_PATH_PREFIX)
                || path.startsWith(ACTUATOR_PATH_PREFIX)
                || path.startsWith(SWAGGER_UI_PATH)
                || path.startsWith(API_DOCS_PATH);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Missing or invalid JWT token.");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now().toString());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
