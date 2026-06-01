package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.ExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
	public static final String BASE_CURRENCY = "USD";
	private static final int RATE_SCALE = 10;
	private static final int AMOUNT_SCALE = 2;
	private final WebClient apiClient;

	public ExchangeRateResponse getRates(String baseCurrency) {
		String normalizedBase = normalizeCurrency(baseCurrency);
		return apiClient.get()
				.uri("{base}", normalizedBase)
				.retrieve()
				.bodyToMono(ExchangeRateResponse.class)
				.block();
	}

	public BigDecimal getRate(String baseCurrency, String targetCurrency) {
		ExchangeRateResponse response = getRates(baseCurrency);
		if (response == null) {
			throw new IllegalStateException("Exchange rate API returned empty response");
		}
		Map<String, BigDecimal> rates = response.conversionRates();
		if (rates == null || rates.isEmpty()) {
			throw new IllegalStateException("Exchange rate API returned no conversion rates");
		}
		String normalizedTarget = normalizeCurrency(targetCurrency);
		BigDecimal rate = rates.get(normalizedTarget);
		if (rate == null) {
			throw new IllegalArgumentException("Unsupported currency: " + normalizedTarget);
		}
		return rate.setScale(RATE_SCALE, RoundingMode.HALF_EVEN);
	}

	public BigDecimal convert(BigDecimal amount, String baseCurrency, String targetCurrency) {
		if (amount == null) {
			throw new IllegalArgumentException("Amount must not be null");
		}
		if (amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_EVEN);
		}
		if (baseCurrency == null || targetCurrency == null) {
			throw new IllegalArgumentException("Currency must not be null");
		}
		String normalizedBase = normalizeCurrency(baseCurrency);
		String normalizedTarget = normalizeCurrency(targetCurrency);
		if (normalizedBase.equals(normalizedTarget)) {
			return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_EVEN);
		}
		BigDecimal rate = getRate(normalizedBase, normalizedTarget);
		return amount.multiply(rate).setScale(AMOUNT_SCALE, RoundingMode.HALF_EVEN);
	}

	private String normalizeCurrency(String currency) {
		if (currency == null || currency.isBlank()) {
			throw new IllegalArgumentException("Currency must not be blank");
		}
		return currency.trim().toUpperCase(Locale.ROOT);
	}
}
