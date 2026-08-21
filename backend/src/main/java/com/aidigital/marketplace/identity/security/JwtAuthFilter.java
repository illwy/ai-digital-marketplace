package com.aidigital.marketplace.identity.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aidigital.marketplace.identity.application.UserQueryService;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.JsonErrorWriter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserQueryService userQueryService;
    private final JsonErrorWriter jsonErrorWriter;

    public JwtAuthFilter(JwtService jwtService, UserQueryService userQueryService, JsonErrorWriter jsonErrorWriter) {
        this.jwtService = jwtService;
        this.userQueryService = userQueryService;
        this.jsonErrorWriter = jsonErrorWriter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtService.parse(token);
            if ("refresh".equals(claims.get("typ"))) {
                filterChain.doFilter(request, response);
                return;
            }
            Long userId = Long.valueOf(claims.getSubject());
            AuthUser user = userQueryService.loadEnabled(userId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ApiException ex) {
            SecurityContextHolder.clearContext();
            if ("USER_DISABLED".equals(ex.getCode())) {
                jsonErrorWriter.write(response, HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage());
                return;
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
