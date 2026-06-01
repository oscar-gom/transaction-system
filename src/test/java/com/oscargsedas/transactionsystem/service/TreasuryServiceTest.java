package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.WelcomeBonusProperties;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreasuryServiceTest {

	@Mock
	private SystemTreasuryAccountService systemTreasuryAccountService;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private LedgerLineService ledgerLineService;

	@Mock
	private WelcomeBonusProperties welcomeBonusProperties;

	@InjectMocks
	private TreasuryService treasuryService;

	@Test
	void applyWelcomeBonusCreatesCompletedTransactionAndLedgerLines() {
		User receiverUser = new User();
		receiverUser.setId(UUID.randomUUID());

		Account receiverAccount = new Account();
		receiverAccount.setId(UUID.randomUUID());
		receiverAccount.setUser(receiverUser);
		receiverAccount.setCurrency("EUR");

		Account treasuryAccount = new Account();
		treasuryAccount.setId(UUID.randomUUID());
		treasuryAccount.setCurrency("USD");

		when(systemTreasuryAccountService.getOrCreateTreasuryAccount("USD")).thenReturn(treasuryAccount);
		when(welcomeBonusProperties.getAmount()).thenReturn(new BigDecimal("5000.00"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction tx = invocation.getArgument(0);
			tx.setId(UUID.randomUUID());
			return tx;
		});

		treasuryService.applyWelcomeBonus(receiverAccount);

		ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(captor.capture());
		Transaction savedTx = captor.getValue();

		assertEquals(treasuryAccount, savedTx.getSenderAccount());
		assertEquals(receiverAccount, savedTx.getReceiverAccount());
		assertEquals(new BigDecimal("5000.00"), savedTx.getAmount());
		assertEquals(TransactionStatus.COMPLETED, savedTx.getStatus());
		verify(ledgerLineService).createLedgerLinesForTransaction(savedTx);
	}

	@Test
	void applyWelcomeBonusAlwaysUsesUSDTreasuryRegardlessOfReceiverCurrency() {
		User receiverUser = new User();
		receiverUser.setId(UUID.randomUUID());

		Account receiverAccount = new Account();
		receiverAccount.setId(UUID.randomUUID());
		receiverAccount.setUser(receiverUser);
		receiverAccount.setCurrency("GBP");

		Account treasuryAccount = new Account();
		treasuryAccount.setId(UUID.randomUUID());
		treasuryAccount.setCurrency("USD");

		when(systemTreasuryAccountService.getOrCreateTreasuryAccount("USD")).thenReturn(treasuryAccount);
		when(welcomeBonusProperties.getAmount()).thenReturn(new BigDecimal("5000.00"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction tx = invocation.getArgument(0);
			tx.setId(UUID.randomUUID());
			return tx;
		});

		treasuryService.applyWelcomeBonus(receiverAccount);

		verify(systemTreasuryAccountService).getOrCreateTreasuryAccount("USD");
	}

	@Test
	void createCompensationTransactionAlwaysUsesUSDTreasury() {
		User receiverUser = new User();
		receiverUser.setId(UUID.randomUUID());

		Account receiverAccount = new Account();
		receiverAccount.setId(UUID.randomUUID());
		receiverAccount.setUser(receiverUser);
		receiverAccount.setCurrency("JPY");

		Account treasuryAccount = new Account();
		treasuryAccount.setId(UUID.randomUUID());
		treasuryAccount.setCurrency("USD");

		when(systemTreasuryAccountService.getOrCreateTreasuryAccount("USD")).thenReturn(treasuryAccount);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction tx = invocation.getArgument(0);
			tx.setId(UUID.randomUUID());
			return tx;
		});

		treasuryService.createCompensationTransaction(receiverAccount, new BigDecimal("100.00"), UUID.randomUUID());

		verify(systemTreasuryAccountService).getOrCreateTreasuryAccount("USD");
	}
}

