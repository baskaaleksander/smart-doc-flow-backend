package com.baskaaleksander.smartdocflowbackend.security;

import com.baskaaleksander.smartdocflowbackend.exceptions.InvalidJwtTokenException;
import com.baskaaleksander.smartdocflowbackend.repository.RefreshTokenRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${jwt.access.secret}")
    private String jwtAccessSecret;
    @Value("${jwt.access.expiration}")
    private int jwtAccessExpiration;
    @Value("${jwt.refresh.secret}")
    private String jwtRefreshSecret;
    @Value("${jwt.refresh.expiration}")
    private int jwtRefreshExpiration;
    private SecretKey accessKey;
    private SecretKey refreshKey;

    public JwtUtil(UserDetailsService userDetailsService, RefreshTokenRepository refreshTokenRepository) {
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(jwtAccessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username) {
        var now = new Date();
        var exp = new Date(now.getTime() + jwtAccessExpiration);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        var roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();



        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(String username, String jti) {

        return Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtRefreshExpiration))
                .signWith(refreshKey)
                .compact();
    }

    public String refreshAccessToken(String refreshToken) {
        if (validateRefreshToken(refreshToken)) {
            return generateAccessToken(
                    getUsernameFromRefreshToken(refreshToken)
            );
        } else {
            throw new InvalidJwtTokenException("Invalid refresh token");
        }

    }

    public String getUsernameFromAccessToken(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey).build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getSubject();
    }

    public String getUsernameFromRefreshToken(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey).build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getSubject();
    }

    public String getJtiFromRefreshToken(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey).build()
                .parseClaimsJws(refreshToken)
                .getBody()
                .getId();
    }

    public boolean validateAccessToken(String accessToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(accessKey).build()
                    .parseClaimsJws(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtTokenException("Invalid access token");
        }
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(refreshKey).build()
                    .parseClaimsJws(refreshToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtTokenException("Invalid refresh token");
        }
    }

    public void invalidateRefreshToken(String refreshToken) {
        String jti = getJtiFromRefreshToken(refreshToken);
        refreshTokenRepository.findByJtiAndRevokedFalse(jti).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
