package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WelcomeBonusTreasuryService {
	private final SystemTreasuryAccountService systemTreasuryAccountService;
	private final TransactionRepository transactionRepository;
	private final LedgerLineService ledgerLineService;
	private final WelcomeBonusProperties welcomeBonusProperties;

	@Transactional
	public void applyWelcomeBonus(Account receiverAccount) {
		Account treasuryAccount = systemTreasuryAccountService.getOrCreateTreasuryAccount(receiverAccount.getCurrency());
		Transaction savedTransaction = transactionRepository.save(buildWelcomeTransaction(treasuryAccount, receiverAccount));
		ledgerLineService.createLedgerLinesForTransaction(savedTransaction);
	}

	private Transaction buildWelcomeTransaction(Account senderAccount, Account receiverAccount) {
		Transaction welcomeTransaction = new Transaction();
		welcomeTransaction.setSenderAccount(senderAccount);
		welcomeTransaction.setReceiverAccount(receiverAccount);
		welcomeTransaction.setIdempotencyKey(UUID.randomUUID());
		welcomeTransaction.setAmount(welcomeBonusProperties.getAmount());
		welcomeTransaction.setStatus(TransactionStatus.COMPLETED);
		return welcomeTransaction;
	}
}

