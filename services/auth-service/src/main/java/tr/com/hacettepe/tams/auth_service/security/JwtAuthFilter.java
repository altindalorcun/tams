package tr.com.hacettepe.tams.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tr.com.hacettepe.tams.auth_service.domain.Role;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads the Authorization header on every request, validates the JWT,
 * and populates the SecurityContext so that Spring Security can enforce
 * role-based access on protected endpoints (e.g. /api/v1/admin/**).
 * Requests without a valid Bearer token are left unauthenticated;
 * Spring Security's access rules decide whether to reject them.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isValid(token) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = jwtUtil.extractUserId(token);
        Role role = jwtUtil.extractRole(token);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
