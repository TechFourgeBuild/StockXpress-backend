package com.sxp.StockXpress.service;

import com.sxp.StockXpress.entity.RefreshToken;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.repository.RefreshTokenRepository;
import com.sxp.StockXpress.repository.UserRepository;
import com.sxp.StockXpress.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.sxp.StockXpress.exception.TokenRefreshException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class RefreshTokenService {

    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    // ============================================================
    // CONVERSION METHODS
    // ============================================================

    private Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    // ============================================================
    // STEP A: LOGIN-TIME — creating new session family
    // ============================================================
    public String createNewRefreshToken(User user) {
        String familyId = jwtUtil.generateSessionFamilyId();
        Date absoluteExpiry = new Date(System.currentTimeMillis() + refreshExpirationMs);

        String tokenString = jwtUtil.generateRefreshToken(user.getEmail(), familyId, convertToLocalDateTime(absoluteExpiry));

        RefreshToken entity = new RefreshToken();
        entity.setToken(tokenString);
        entity.setSessionFamilyId(familyId);
        entity.setUser(user);
        entity.setAbsoluteExpiry(convertToLocalDateTime(absoluteExpiry));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        return tokenString;
    }

    // =========================================================
    // STEP B: 🌟 ROTATION LOGIC — sabse critical method
    // =========================================================
    public RotationResult rotateRefreshToken(String oldTokenString) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenString).
                orElseThrow(() -> new TokenRefreshException("Refresh token not found - please login again"));

        if(oldToken.isRevoked()) {
            refreshTokenRepository.revokeAllBySessionFamily(oldToken.getSessionFamilyId());

            throw new TokenRefreshException("Refresh token reuse detected — all sessions revoked. Please login again.");
        }

        if(oldToken.getAbsoluteExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("Refresh token expired — please login again");
        }

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        User user = oldToken.getUser();

        String newTokenString = jwtUtil.generateRefreshToken(user.getEmail(), oldToken.getSessionFamilyId(), oldToken.getAbsoluteExpiry());

        RefreshToken newTokenEntity = new RefreshToken();
        newTokenEntity.setToken(newTokenString);
        newTokenEntity.setSessionFamilyId(oldToken.getSessionFamilyId());
        newTokenEntity.setUser(user);
        newTokenEntity.setAbsoluteExpiry(oldToken.getAbsoluteExpiry());
        newTokenEntity.setRevoked(false);
        refreshTokenRepository.save(newTokenEntity);
        return new RotationResult(newTokenString, user);
    }

    public void revokeToken(String tokenString) {
        refreshTokenRepository.findByToken(tokenString).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public static class RotationResult {
        public final String newRefreshToken;
        public final User user;
        public RotationResult(String newRefreshToken, User user) {
            this.newRefreshToken = newRefreshToken;
            this.user = user;
        }
    }
}
