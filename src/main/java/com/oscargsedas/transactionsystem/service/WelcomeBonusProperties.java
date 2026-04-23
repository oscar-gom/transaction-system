package com.oscargsedas.transactionsystem.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "welcome-bonus")
public class WelcomeBonusProperties {
	private BigDecimal amount = new BigDecimal("5000.00");
	private String systemUserEmail = "system@transaction.local";
	private String systemUserName = "System";
	private String systemUserSurname = "Treasury";
	private String systemUserPassword = "SYSTEM_INTERNAL_ONLY";
}
