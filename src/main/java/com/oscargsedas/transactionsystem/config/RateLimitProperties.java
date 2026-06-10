package com.oscargsedas.transactionsystem.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitProperties {
	private Limit authLoginLimit = new Limit(5, 5, 60);
	private Limit authRegisterLimit = new Limit(3, 3, 60);
	private Limit adminLimit = new Limit(10, 10, 60);
	private Limit searchLimit = new Limit(5, 5, 60);
	private Limit accountsLimit = new Limit(60, 60, 60);
	private Limit transactionsLimit = new Limit(60, 60, 60);
	private Limit defaultLimit = new Limit(60, 60, 60);

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Limit {
		private int capacity;
		private int refillTokens;
		private int refillPeriodSeconds;
	}
}
