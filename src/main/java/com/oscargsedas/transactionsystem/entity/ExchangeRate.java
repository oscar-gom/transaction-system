package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {
	@Id
	@Column(name = "base_currency", length = 3)
	private String baseCurrency;

	@Column(name = "last_updated", nullable = false)
	private Instant lastUpdated;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "exchange_rate_values", joinColumns = @JoinColumn(name = "base_currency"))
	@MapKeyColumn(name = "target_currency", length = 3)
	@Column(name = "rate", precision = 20, scale = 10, nullable = false)
	private Map<String, BigDecimal> rates;
}
