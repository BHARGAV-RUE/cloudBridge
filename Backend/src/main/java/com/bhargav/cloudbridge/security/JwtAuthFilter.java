package com.bhargav.cloudbridge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filter for non-admin routes — let them pass through
        if (!path.startsWith("/api/admin/") ||
                path.equals("/api/admin/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 1 — Extract Authorization header
        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[JwtFilter] Missing or invalid Authorization header | path: {}",
                    path);
            sendUnauthorized(response, "Missing Authorization header");
            return;
        }

        // Step 2 — Extract raw token
        String token = authHeader.substring(BEARER_PREFIX.length());

        // Step 3 — Validate token
        if (!jwtService.isTokenValid(token)) {
            log.warn("[JwtFilter] Invalid or expired token | path: {}", path);
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        // Step 4 — Extract username
        String username = jwtService.extractUsername(token);

        // Step 5 — Set SecurityContext (only if not already set)
        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.info("[JwtFilter] Admin authenticated: {} | path: {}",
                    username, path);
        }

        // Step 6 — Continue filter chain
        filterChain.doFilter(request, response);
    }

    // ── UTILITY ────────────────────────────────────────────────────────────────

    private void sendUnauthorized(HttpServletResponse response,
                                  String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                """
                {
                  "success": false,
                  "error": "%s",
                  "timestamp": "%s"
                }
                """.formatted(message, java.time.LocalDateTime.now())
        );
    }
}