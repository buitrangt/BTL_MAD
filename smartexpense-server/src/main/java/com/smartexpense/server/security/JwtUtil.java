package com.smartexpense.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
    Claims claims = getClaims(token);
    return claims.getSubject(); // Hoặc thử claims.get("sub", String.class);
}

    // QUAN TRỌNG: Kiểm tra lại hàm isTokenValid
public boolean isTokenValid(String token) {
    try {
        Claims claims = getClaims(token);
        // Phải kiểm tra thêm xem token đã hết hạn chưa
        return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
        // Log lỗi ra để biết tại sao nó false (sai key, hết hạn, hay format đểu)
        System.out.println("JWT Error: " + e.getMessage());
        return false;
    }
}

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
