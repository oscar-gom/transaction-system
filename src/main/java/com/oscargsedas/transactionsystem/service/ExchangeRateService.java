package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.ExchangeRateResponse;
import com.oscargsedas.transactionsystem.entity.ExchangeRate;
import com.oscargsedas.transactionsystem.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {
	public static final String BASE_CURRENCY = "USD";
	private static final int RATE_SCALE = 10;
	private static final int AMOUNT_SCALE = 2;
	private final WebClient apiClient;
	private final ExchangeRateRepository exchangeRateRepository;

	@Scheduled(fixedRate = 1800000) // 30 minutes
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void updateExchangeRates() {
		log.info("Fetching exchange rates from external API...");
		try {
			ExchangeRateResponse response = apiClient.get()
					.uri("{base}", BASE_CURRENCY)
					.retrieve()
					.bodyToMono(ExchangeRateResponse.class)
					.block();

			if (response != null && response.conversionRates() != null && !response.conversionRates().isEmpty()) {
				ExchangeRate exchangeRate = new ExchangeRate(
						BASE_CURRENCY,
						Instant.now(),
						response.conversionRates()
				);
				exchangeRateRepository.save(exchangeRate);
				log.info("Exchange rates successfully updated and saved in PostgreSQL.");
			} else {
				log.warn("Received empty or invalid response from exchange rate API");
			}
		} catch (Exception e) {
			log.error("Failed to update exchange rates from API: {}", e.getMessage());
		}
	}

	public ExchangeRateResponse fetchRatesFromApiFallback(String baseCurrency) {
		String normalizedBase = normalizeCurrency(baseCurrency);
		log.warn("No data found for base currency {}. Performing synchronous API call as fallback.", normalizedBase);
		ExchangeRateResponse response = apiClient.get()
				.uri("{base}", normalizedBase)
				.retrieve()
				.bodyToMono(ExchangeRateResponse.class)
				.block();
				
		if (response != null && response.conversionRates() != null && !response.conversionRates().isEmpty()) {
			ExchangeRate exchangeRate = new ExchangeRate(
					normalizedBase,
					Instant.now(),
					response.conversionRates()
			);
			exchangeRateRepository.save(exchangeRate);
		}
		return response;
	}

	public BigDecimal getRate(String baseCurrency, String targetCurrency) {
		String normalizedBase = normalizeCurrency(baseCurrency);
		String normalizedTarget = normalizeCurrency(targetCurrency);

		ExchangeRate exchangeRate = exchangeRateRepository.findById(normalizedBase).orElse(null);
		Map<String, BigDecimal> rates;

		if (exchangeRate != null && exchangeRate.getRates() != null && !exchangeRate.getRates().isEmpty()) {
			rates = exchangeRate.getRates();
		} else {
			ExchangeRateResponse response = fetchRatesFromApiFallback(normalizedBase);
			if (response == null || response.conversionRates() == null || response.conversionRates().isEmpty()) {
				throw new IllegalStateException("Exchange rate API returned no conversion rates");
			}
			rates = response.conversionRates();
		}

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
