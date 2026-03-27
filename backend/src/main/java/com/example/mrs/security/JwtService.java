package com.example.mrs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtProps props;
  private final SecretKey key;

  public JwtService(JwtProps props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createToken(long userId, String username, String role) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getExpireMinutes() * 60L);
    Map<String, Object> claims = new HashMap<String, Object>();
    claims.put("username", username);
    claims.put("role", role);
    return Jwts.builder()
        .issuer(props.getIssuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .subject(String.valueOf(userId))
        .claims(claims)
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer(props.getIssuer())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}

