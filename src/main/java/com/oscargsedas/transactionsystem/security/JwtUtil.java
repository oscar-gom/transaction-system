package com.oscargsedas.transactionsystem.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private int jwtExpirationMs;

	private SecretKey key;

	@PostConstruct
	public void init() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (256 bits) for HS256");
		}
		this.key = Keys.hmacShaKeyFor(keyBytes);
		log.info("JWT secret key initialized");
	}

	public String generateToken(String email, Long tokenVersion) {
		return Jwts.builder()
				.subject(email)
				.claim("tokenVersion", tokenVersion)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				.signWith(key)
				.compact();
	}

	public String getEmailFromToken(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public Long getTokenVersionFromToken(String token) {
		Number version = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.get("tokenVersion", Number.class);
		return version != null ? version.longValue() : 1L;
	}

	public boolean validateJwtToken(String token) {
		try {
			Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			log.error("Invalid JWT token: {}", e.getMessage());
			return false;
		}
	}
}
