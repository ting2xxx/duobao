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
        //get current time in milliseconds
        Long nowMillis = System.currentTimeMillis();
        //create a date object with current time
        Date now = new Date(nowMillis);
        //create a date object with current time + expiration time
        Date exp = new Date(nowMillis + EXPIRATION_TIME);

        //Use JwtBuilder to build a jwt token based on userId, now time, expiry time and secret key
        JwtBuilder builder = Jwts.builder()
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        // return a compact URL-safe JWT string
        return builder.compact();
    }

    /**
     * Decrypts the token and extracts the User ID
     * (We pretend we used this in the JwtInterceptor earlier!)
     */
    public static Long parseToken(String token) {
        //Use Jwts.parser() to parse the token with the secret key
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                //parse the token to a claims, claims include the attribute when we build a jwtToken
                        .parseClaimsJws(token)
                        .getBody();

                //get userId from claims
                Object userId =  claims.get("userId");
                //return userId as Long
                return Long.valueOf(userId.toString());
    }
}
