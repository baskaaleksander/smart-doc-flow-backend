package com.baskaaleksander.smartdocflowbackend.common.security;

import com.baskaaleksander.smartdocflowbackend.modules.auth.application.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = parseJwt(request);

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtUtil.validateAccessToken(jwt)) {
                String username = jwtUtil.getUsernameFromAccessToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);
                return;
            }

            unauthorized(response);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            unauthorized(response);
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
                    {"status":401,"message":"Unauthorized","details":"Invalid or missing access token"}
                """);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        return (headerAuth != null && headerAuth.startsWith("Bearer "))
                ? headerAuth.substring(7)
                : null;
    }
}