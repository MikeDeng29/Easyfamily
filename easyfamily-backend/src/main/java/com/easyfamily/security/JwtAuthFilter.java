package com.easyfamily.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SseEmitter responses (e.g. /api/v1/chat/stream) re-dispatch on the async
        // completion thread, where SecurityContextHolder is empty by default.
        // Re-run auth on that dispatch too, otherwise AuthorizationFilter denies it.
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                if (tokenBlacklistService.isRevoked(token)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                Claims claims = jwtService.parse(token);
                String tokenType = claims.get("tokenType", String.class);
                if ("access".equals(tokenType)) {
                    UserPrincipal principal = new UserPrincipal(
                            claims.getSubject(),
                            claims.get("phone", String.class)
                    );
                    String rolesStr = claims.get("roles", String.class);
                    Collection<? extends GrantedAuthority> authorities = (rolesStr != null && !rolesStr.isBlank())
                            ? AuthorityUtils.commaSeparatedStringToAuthorityList(rolesStr)
                            : AuthorityUtils.NO_AUTHORITIES;
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
