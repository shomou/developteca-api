package com.developteca.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    //============= CLAVE PRIVADA PARA FIRMAR ================
    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    //============ EXTRAER USERNAME ================
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    // ============= EXTRAER UN CLAIM ESPECÍFICO =============
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    // ============= EXTRAER TODOS LOS CLAIMS =============
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ============= VERIFICAR SI ESTÁ EXPIRADO =============
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ============= EXTRAER FECHA DE EXPIRACIÓN =============
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ============= GENERAR TOKEN =============
    public String generateToken(String username) {
        Map<String, Object> extraClaims = new HashMap<>();
        return createToken(extraClaims, username);
    }

    // ============= CREAR TOKEN =============
    private String createToken(Map<String, Object> extraClaims, String username) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ============= VALIDAR TOKEN =============
    public Boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // ============= VALIDAR TOKEN (SIN USERDETAILS) =============
    public Boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

}
