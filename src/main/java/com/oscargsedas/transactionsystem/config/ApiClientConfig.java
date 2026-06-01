package com.oscargsedas.transactionsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApiClientConfig {
	private static final String EXCHANGE_API_BASE_URL = "https://v6.exchangerate-api.com/v6/";

	@Bean
	public WebClient apiClient(WebClient.Builder builder, @Value("${exchange.api.key}") String apiKey) {
		return builder
				.baseUrl(EXCHANGE_API_BASE_URL + apiKey + "/latest/")
				.build();
	}
}
