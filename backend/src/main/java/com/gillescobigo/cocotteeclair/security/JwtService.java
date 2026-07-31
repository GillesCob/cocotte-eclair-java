package com.gillescobigo.cocotteeclair.security;

import com.gillescobigo.cocotteeclair.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes,
            @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays
    ) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes());
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    public String generateAccessToken(UUID userId) {
        return generate(userId, accessKey, accessTtl);
    }

    public String generateRefreshToken(UUID userId) {
        return generate(userId, refreshKey, refreshTtl);
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    private String generate(UUID userId, SecretKey key, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                // jti (id) aleatoire : sans lui, deux tokens emis pour le meme
                // utilisateur dans la meme seconde (iat/exp identiques a la seconde
                // pres) produisent le meme JWT signe, donc le meme hash stocke en
                // base -> collision sur la contrainte d'unicite de refresh_tokens.
                // Constate en verification manuelle (register puis refresh immediat).
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public UUID parseAccessToken(String token) {
        return parse(token, accessKey);
    }

    public UUID parseRefreshToken(String token) {
        return parse(token, refreshKey);
    }

    private UUID parse(String token, SecretKey key) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Token invalide ou expiré");
        }
    }
}
