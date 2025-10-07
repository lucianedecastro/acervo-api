// src/main/java/br/com/acervodaatletabrasileira.acervoapi.service/JwtService.java
package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // CORREÇÃO: Adiciona um valor padrão (fallback) para evitar falha de inicialização (UnsatisfiedDependencyException).
    @Value("${jwt.secret:defaultFakeKey}")
    private String secret;

    // CORREÇÃO: Adiciona um valor padrão (fallback) de 24 horas (86400 segundos).
    @Value("${jwt.expiration:86400}")
    private Long expiration;

    // Métodos antigos
    public String generateToken(UsuarioAdmin usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("nome", usuario.getNome());
        // Note: O username é o email, que é o Subject (assunto) do token.
        return createToken(claims, usuario.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L)) // Converte segundos para milissegundos
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Métodos novos para validação
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Usa o secret para validar o token.
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Key getSigningKey() {
        // Converte a string secret para um array de bytes para gerar a chave de criptografia.
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}