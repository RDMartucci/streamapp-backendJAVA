package com.streamapp.streamappbackend.service.streaming;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Emite tickets firmados para streaming. El ticket incluye el usuario que lo
 * solicitó y la ruta absoluta del archivo, con expiración corta. Como es un URL
 * firmado, funciona igual para el reproductor interno y para reproductores
 * externos (VLC) que no envían headers de autenticación: el ticket ES la
 * credencial.
 */
@Component
public class StreamTicketService {

    private static final String CLAIM_PATH = "path";

    private final SecretKey key;
    private final long ttlMs;

    public StreamTicketService(@Value("${app.jwt.secret}") String secret,
                               @Value("${app.streaming.ticket-ttl-ms:1800000}") long ttlMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMs = ttlMs;
    }

    public String generate(String username, String path) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_PATH, path)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Devuelve el contenido del ticket si es válido (firma + expiración).
     * Lanza {@link StreamTicketException} si no.
     */
    public Ticket parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new Ticket(claims.getSubject(), claims.get(CLAIM_PATH, String.class));
        } catch (Exception e) {
            throw new StreamTicketException("Ticket de streaming inválido o expirado");
        }
    }

    public record Ticket(String username, String path) {
    }
}