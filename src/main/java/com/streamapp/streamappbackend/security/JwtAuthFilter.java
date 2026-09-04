package com.streamapp.streamappbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        log.debug("JwtAuthFilter: Processing request {} {}, Authorization header: {}", request.getMethod(), request.getRequestURI(), header != null ? "present" : "missing");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            log.debug("JwtAuthFilter: Token extracted, length: {}", token.length());
            jwtUtil.debugSecret();
            if (jwtUtil.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                log.debug("JwtAuthFilter: Token valid, username: {}, role: {}", username, role);

                var authorities = List.of(new SimpleGrantedAuthority(role));
                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JwtAuthFilter: Authentication set for user: {}", username);
            } else {
                log.debug("JwtAuthFilter: Token invalid or authentication already set");
            }
        } else {
            log.debug("JwtAuthFilter: No Bearer token found");
        }

        filterChain.doFilter(request, response);
    }
}