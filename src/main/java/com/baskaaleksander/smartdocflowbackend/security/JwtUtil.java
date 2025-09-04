package com.baskaaleksander.smartdocflowbackend.security;

import com.baskaaleksander.smartdocflowbackend.exceptions.InvalidJwtTokenException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

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

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(jwtAccessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtAccessExpiration))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtRefreshExpiration))
                .signWith(refreshKey)
                .compact();
    }

    public String getUsernameFromAccessToken(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey).build()
                .parseClaimsJwt(accessToken)
                .getBody()
                .getSubject();
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

}
