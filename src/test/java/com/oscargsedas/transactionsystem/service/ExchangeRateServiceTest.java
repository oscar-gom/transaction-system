package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.ExchangeRateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

	@Mock
	private WebClient apiClient;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	private WebClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;


	@InjectMocks
	private ExchangeRateService exchangeRateService;

	@Test
	void getRateReturnsRateForSupportedCurrency() {
		ExchangeRateResponse response = new ExchangeRateResponse(
				"success", "docs", "terms", 0L, "", 0L, "", "USD",
				Map.of("USD", new BigDecimal("1.0"), "EUR", new BigDecimal("0.9"), "GBP", new BigDecimal("0.77"))
		);

		when(apiClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(ExchangeRateResponse.class)).thenReturn(Mono.just(response));

		BigDecimal rate = exchangeRateService.getRate("USD", "EUR");

		assertEquals(new BigDecimal("0.9000000000"), rate);
	}

	@Test
	void getRateThrowsForUnsupportedCurrency() {
		ExchangeRateResponse response = new ExchangeRateResponse(
				"success", "docs", "terms", 0L, "", 0L, "", "USD",
				Map.of("USD", new BigDecimal("1.0"), "EUR", new BigDecimal("0.9"))
		);

		when(apiClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(ExchangeRateResponse.class)).thenReturn(Mono.just(response));

		assertThrows(IllegalArgumentException.class, () -> exchangeRateService.getRate("USD", "XYZ"));
	}

	@Test
	void getRateThrowsWhenResponseIsNull() {
		when(apiClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(ExchangeRateResponse.class)).thenReturn(Mono.empty());

		assertThrows(IllegalStateException.class, () -> exchangeRateService.getRate("USD", "EUR"));
	}

	@Test
	void convertReturnsSameAmountWhenCurrenciesAreEqual() {
		BigDecimal result = exchangeRateService.convert(new BigDecimal("100.00"), "EUR", "EUR");

		assertEquals(new BigDecimal("100.00"), result);
	}

	@Test
	void convertMultipliesByRateWhenCurrenciesDiffer() {
		ExchangeRateResponse response = new ExchangeRateResponse(
				"success", "docs", "terms", 0L, "", 0L, "", "USD",
				Map.of("USD", new BigDecimal("1.0"), "EUR", new BigDecimal("0.9"))
		);

		when(apiClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(ExchangeRateResponse.class)).thenReturn(Mono.just(response));

		BigDecimal result = exchangeRateService.convert(new BigDecimal("100.00"), "USD", "EUR");

		assertEquals(new BigDecimal("90.00"), result);
	}

	@Test
	void convertReturnsZeroForZeroAmount() {
		BigDecimal result = exchangeRateService.convert(BigDecimal.ZERO, "USD", "EUR");

		assertEquals(new BigDecimal("0.00"), result);
	}

	@Test
	void convertThrowsForNullAmount() {
		assertThrows(IllegalArgumentException.class, () -> exchangeRateService.convert(null, "USD", "EUR"));
	}

	@Test
	void convertThrowsForNullCurrency() {
		assertThrows(IllegalArgumentException.class, () ->
				exchangeRateService.convert(new BigDecimal("10.00"), null, "EUR"));
	}

	@Test
	void convertNormalizesCurrencyCodes() {
		ExchangeRateResponse response = new ExchangeRateResponse(
				"success", "docs", "terms", 0L, "", 0L, "", "USD",
				Map.of("USD", new BigDecimal("1.0"), "EUR", new BigDecimal("0.85"))
		);

		when(apiClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(ExchangeRateResponse.class)).thenReturn(Mono.just(response));

		BigDecimal result = exchangeRateService.convert(new BigDecimal("200.00"), "usd", "eur");

		assertEquals(new BigDecimal("170.00"), result);
	}

	@Test
	void convertThrowsForBlankCurrency() {
		assertThrows(IllegalArgumentException.class, () ->
				exchangeRateService.convert(new BigDecimal("10.00"), "  ", "EUR"));
	}
}
