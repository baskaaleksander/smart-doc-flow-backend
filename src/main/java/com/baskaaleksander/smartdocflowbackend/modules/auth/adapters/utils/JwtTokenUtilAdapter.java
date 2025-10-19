package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.utils;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidJwtTokenException;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.TokenResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RefreshTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRefreshTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Tokens;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.TokenUtilPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenUtilAdapter implements TokenUtilPort {

    private final UserDetailsService userDetailsService;
    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final SpringDataUserRepository userRepository;
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

    public JwtTokenUtilAdapter(UserDetailsService userDetailsService, SpringDataRefreshTokenRepository refreshTokenRepository, SpringDataUserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(jwtAccessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Tokens issueTokens(String username) {
        String jti = UUID.randomUUID().toString();
        String accessToken = generateAccessToken(username);
        String refreshToken = generateRefreshToken(username, jti);
        UserEntity user = userRepository.findByUsername(username).orElseThrow(() -> new InvalidJwtTokenException("User not found"));
        LocalDateTime now = LocalDateTime.now();

        RefreshTokenEntity token = new RefreshTokenEntity(
                jti,
                user,
                now.plusSeconds(jwtRefreshExpiration / 1000L),
                false,
                null,
                now
        );

        refreshTokenRepository.save(token);

        return new Tokens(accessToken, refreshToken);
    }

    @Override
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

    @Override
    public String generateRefreshToken(String username, String jti) {
        return Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtRefreshExpiration))
                .signWith(refreshKey)
                .compact();
    }

    @Override
    public Tokens refreshAccessToken(String refreshToken) {
        if (validateRefreshToken(refreshToken)) {
            String oldJti = getJtiFromRefreshToken(refreshToken);
            RefreshTokenEntity oldToken = refreshTokenRepository.findByJtiAndRevokedFalse(oldJti)
                    .orElseThrow(() -> new InvalidJwtTokenException("Refresh token not found or revoked"));
            oldToken.setRevoked(true);
            String username = getUsernameFromRefreshToken(refreshToken);
            Tokens tokensIssued = issueTokens(username);
            String newJti = getJtiFromRefreshToken(tokensIssued.getRefreshToken());
            oldToken.setReplacedBy(newJti);
            refreshTokenRepository.save(oldToken);
            return tokensIssued;
        } else {
            throw new InvalidJwtTokenException("Invalid refresh token");
        }
    }

    @Override
    public String getUsernameFromAccessToken(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey).build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getSubject();
    }

    @Override
    public String getUsernameFromRefreshToken(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey).build()
                .parseClaimsJws(refreshToken)
                .getBody()
                .getSubject();    }

    @Override
    public String getJtiFromRefreshToken(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey).build()
                .parseClaimsJws(refreshToken)
                .getBody()
                .getId();    }

    @Override
    public Boolean validateAccessToken(String accessToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(accessKey).build()
                    .parseClaimsJws(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtTokenException("Invalid access token");
        }
    }

    @Override
    public Boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(refreshKey).build()
                    .parseClaimsJws(refreshToken);

            RefreshTokenEntity token = refreshTokenRepository
                    .findByJtiAndRevokedFalse(getJtiFromRefreshToken(refreshToken))
                    .orElseThrow(() -> new InvalidJwtTokenException("Refresh token not found or revoked"));

            if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new InvalidJwtTokenException("Refresh token expired (DB)");
            }
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new InvalidJwtTokenException("Refresh token expired (JWT)");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new InvalidJwtTokenException("Refresh token signature mismatch");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new InvalidJwtTokenException("Malformed refresh token");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtTokenException("Invalid refresh token: " + e.getMessage());
        }
    }

    @Override
    public void invalidateRefreshToken(String refreshToken) {
        String jti = getJtiFromRefreshToken(refreshToken);
        refreshTokenRepository.findByJtiAndRevokedFalse(jti).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
