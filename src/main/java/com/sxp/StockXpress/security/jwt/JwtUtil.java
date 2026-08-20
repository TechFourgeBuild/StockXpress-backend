package com.sxp.StockXpress.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.access.secret}")
    private String accessSecret;

    @Value("${jwt.access.expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    private SecretKey getAccessSigninKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
    }

    private SecretKey getRefreshSigninKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
    }

    // ============================================================
    // ACCESS TOKEN — Generation
    // ============================================================

    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type","access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(getAccessSigninKey())
                .compact();
    }

    // ============================================================
    //  REFRESH TOKEN — Generation
    // =================================

    public Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public String generateRefreshToken(String username, String sessionFamilyId,LocalDateTime absoluteExpiry) {
        return Jwts.builder()
                .subject(username)
                .claim("type","refresh")
                .claim("familyId",sessionFamilyId)
                .issuedAt(new Date())
                .expiration(convertToDate(absoluteExpiry))
                .signWith(getRefreshSigninKey())
                .compact();
    }

    public String generateSessionFamilyId() {
        return UUID.randomUUID().toString();
    }

    // ============================================================
    // EXTRACTION METHODS
    // ============================================================

    public String extractUsername(String token, boolean isRefreshToken) {
        return extractClaims(token,isRefreshToken).getSubject();
    }

    private Claims extractClaims(String token, boolean isRefreshToken) {
        SecretKey key = isRefreshToken ? getRefreshSigninKey():getAccessSigninKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractFamilyId(String token) {
        return extractClaims(token,true).get("familyId",String.class);
    }

    public boolean isTokenExpired(String token, boolean isRefreshToken) {
        return extractClaims(token, isRefreshToken).getExpiration().before(new Date());
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token,false);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token,false);
        } catch(Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            extractClaims(token,true);
            return !isTokenExpired(token,true);
        } catch (Exception e) {
            return false;
        }
    }

    public Date extractExpiry(String token, boolean isRefreshToken) {
        return extractClaims(token,isRefreshToken).getExpiration();
    }
}
