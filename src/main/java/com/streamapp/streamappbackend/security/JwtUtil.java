package com.streamapp.streamappbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // Clave fija hardcodeada - 56 chars = 448 bits (válido para HS384)
    private static final String FIXED_SECRET = "StreamApp_Dev_Secret_Key_Change_This_In_Production_2024!";
    private final SecretKey key;
    private final long expirationMs = 3600000; // 1 hora

    public JwtUtil() {
        // Clave fija hardcodeada - 56 chars = 448 bits (válido para HS384)
        String secret = "StreamApp_Dev_Secret_Key_Change_This_In_Production_2024!";
        this.key = Keys.hmacShaKeyFor(FIXED_SECRET.getBytes(StandardCharsets.UTF_8));
        System.out.println("JwtUtil init: key length=" + this.key.getEncoded().length + ", first16=" + bytesToHex(this.key.getEncoded()).substring(0, 16));
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        System.out.println("JWT generate: now=" + new Date() + " expiry=" + new Date(System.currentTimeMillis() + 3600000));
        System.out.println("JWT generate: key length=" + key.getEncoded().length + ", first16=" + bytesToHex(key.getEncoded()).substring(0, 16));
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            System.out.println("JwtUtil.isValid: validating token length=" + token.length());
            parseClaims(token);
            System.out.println("JwtUtil.isValid: token VALID");
            return true;
        } catch (Exception e) {
            System.err.println("JWT invalid: " + e.getMessage() + " token=" + token.substring(0, Math.min(20, token.length())) + "...");
            e.printStackTrace();
            return false;
        }
    }

    public long getExpirationMs() {
        return 3600000;
    }

    public void debugSecret() {
        System.out.println("JWT Secret being used: length=" + (key != null ? key.getEncoded().length : 0) + " first16=" + (key != null ? bytesToHex(key.getEncoded()).substring(0, 16) : "null"));
    }

    private Claims parseClaims(String token) {
        System.out.println("JwtUtil.parseClaims: token length=" + token.length() + ", key length=" + key.getEncoded().length + ", first16=" + bytesToHex(key.getEncoded()).substring(0, 16));
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(864000)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}