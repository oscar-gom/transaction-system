package com.oscargsedas.transactionsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

public record ExchangeRateResponse(
		String result,
		String documentation,
		@JsonProperty("terms_of_use") String termsOfUse,
		@JsonProperty("time_last_update_unix") long timeLastUpdateUnix,
		@JsonProperty("time_last_update_utc") String timeLastUpdateUtc,
		@JsonProperty("time_next_update_unix") long timeNextUpdateUnix,
		@JsonProperty("time_next_update_utc") String timeNextUpdateUtc,
		@JsonProperty("base_code") String baseCode,
		@JsonProperty("conversion_rates") Map<String, BigDecimal> conversionRates
) {
}
