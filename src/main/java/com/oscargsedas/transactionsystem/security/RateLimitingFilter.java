package com.oscargsedas.transactionsystem.security;

import com.oscargsedas.transactionsystem.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
	private final RateLimitProperties rateLimitProperties;
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public RateLimitingFilter(RateLimitProperties rateLimitProperties) {
		this.rateLimitProperties = rateLimitProperties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String key = buildKey(request);
		Bucket bucket = buckets.computeIfAbsent(key, ignored -> buildBucket(request));
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
			return;
		}

		long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
		response.setStatus(429);
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		response.setHeader("X-Rate-Limit-Remaining", "0");
	}

	private String buildKey(HttpServletRequest request) {
		String identity = resolveIdentity(request);
		return identity + ":" + request.getRequestURI();
	}

	private String resolveIdentity(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
			return "user:" + authentication.getName();
		}
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return "ip:" + forwardedFor.split(",")[0].trim();
		}
		return "ip:" + request.getRemoteAddr();
	}

	private Bucket buildBucket(HttpServletRequest request) {
		RateLimitProperties.Limit limit = resolveLimit(request.getRequestURI());
		Bandwidth bandwidth = Bandwidth.builder()
				.capacity(limit.getCapacity())
				.refillGreedy(limit.getRefillTokens(), Duration.ofSeconds(limit.getRefillPeriodSeconds()))
				.build();
		return Bucket.builder().addLimit(bandwidth).build();
	}

	private RateLimitProperties.Limit resolveLimit(String uri) {
		if (uri.startsWith("/api/v4/auth/login")) {
			return rateLimitProperties.getAuthLoginLimit();
		}
		if (uri.startsWith("/api/v4/auth/register")) {
			return rateLimitProperties.getAuthRegisterLimit();
		}
		if (uri.startsWith("/api/v4/admin")) {
			return rateLimitProperties.getAdminLimit();
		}
		if (uri.startsWith("/api/v4/accounts/search")) {
			return rateLimitProperties.getSearchLimit();
		}
		if (uri.startsWith("/api/v4/accounts")) {
			return rateLimitProperties.getAccountsLimit();
		}
		if (uri.startsWith("/api/v4/transactions")) {
			return rateLimitProperties.getTransactionsLimit();
		}
		return rateLimitProperties.getDefaultLimit();
	}
}
