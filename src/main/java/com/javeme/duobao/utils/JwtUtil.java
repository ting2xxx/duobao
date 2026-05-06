package com.javeme.duobao.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;


public class JwtUtil {

    // 1. The Secret Key (NEVER leak this in a real company! Usually hidden in application.yml)
    private static final String SECRET_KEY = "DuoBaoSuperSecretKeyForECommerceApp";

    // 2. Token Lifespan (e.g., 12 hours in milliseconds)
    private static final long EXPIRATION_TIME = 12 * 60 * 60 * 1000;


    /**
     * Generates the token using the User's ID
     * @param userId
     * @return
     */
    public static String createToken(Long userId) {
        Long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + EXPIRATION_TIME);

        JwtBuilder builder = Jwts.builder()
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        return builder.compact();
    }

    /**
     * Decrypts the token and extracts the User ID
     * (We pretend we used this in the JwtInterceptor earlier!)
     */
    public static Long parseToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                        .parseClaimsJws(token)
                        .getBody();

                return claims.get("userId", Long.class);
    }
}
