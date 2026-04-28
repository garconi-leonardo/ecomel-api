package br.com.ecomel.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import br.com.ecomel.exception.GlobalExceptionHandler;

import javax.crypto.SecretKey;

import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationInMs;
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);


    private SecretKey key;

    // Inicializa a chave uma única vez no startup para performance e segurança
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Authentication authentication) {
        // Cast para UserDetails (mais genérico e seguro)
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String loginSucesso = userPrincipal.getUsername();

        return Jwts.builder()
                .setSubject(loginSucesso)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                // Agora usamos a chave preparada no @PostConstruct
                .signWith(key, SignatureAlgorithm.HS512) 
                .compact();
    }

    public String getUsernameDoJwt(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Falha na validação do token: " + e.getMessage());
            return false;
        }
    }
}
