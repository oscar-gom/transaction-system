package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.WelcomeBonusProperties;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemTreasuryAccountService {
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final WelcomeBonusProperties welcomeBonusProperties;

	Account getOrCreateTreasuryAccount(String currency) {
		User systemUser = getOrCreateSystemUser();
		return accountRepository.findByUserIdAndCurrency(systemUser.getId(), currency)
				.orElseGet(() -> {
					Account account = new Account();
					account.setUser(systemUser);
					account.setCurrency(currency);
					account.setAccountName("System Treasury");
					return accountRepository.save(account);
				});
	}

	private User getOrCreateSystemUser() {
		User existingSystemUser = userRepository.findByEmail(welcomeBonusProperties.getSystemUserEmail());
		if (existingSystemUser != null) {
			return existingSystemUser;
		}

		User systemUser = new User();
		systemUser.setEmail(welcomeBonusProperties.getSystemUserEmail());
		systemUser.setName(welcomeBonusProperties.getSystemUserName());
		systemUser.setSurname(welcomeBonusProperties.getSystemUserSurname());
		systemUser.setPassword(welcomeBonusProperties.getSystemUserPassword());
		return userRepository.save(systemUser);
	}
}
