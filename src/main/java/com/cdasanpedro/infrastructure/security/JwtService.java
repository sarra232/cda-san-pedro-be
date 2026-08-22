package com.cdasanpedro.infrastructure.security;

import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${app.jwt.expiration-hours:24}")
    private long expirationHours;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UsuarioEntity usuario) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId().toString());
        extraClaims.put("rol", usuario.getRol().name());
        extraClaims.put("nombres", usuario.getNombresApellidos());
        extraClaims.put("tipoDoc", usuario.getTipoDocumento().name());

        return Jwts.builder()
                .claims(extraClaims)
                .subject(usuario.getNumeroDocumento())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (expirationHours * 3600 * 1000)))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractNumeroDocumento(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userIdStr);
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String numeroDocumento) {
        final String extractedDocumento = extractNumeroDocumento(token);
        return (extractedDocumento.equals(numeroDocumento) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
