package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.WelcomeBonusProperties;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.UUID;

@Validated
@Service
@RequiredArgsConstructor
public class TreasuryService {
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

	private Transaction buildCompensationTransaction(Account senderAccount, Account receiverAccount, BigDecimal amount, UUID idempotencyKey) {
		Transaction compensationTransaction = new Transaction();

		compensationTransaction.setSenderAccount(senderAccount);
		compensationTransaction.setReceiverAccount(receiverAccount);
		compensationTransaction.setIdempotencyKey(idempotencyKey);
		compensationTransaction.setAmount(amount);
		compensationTransaction.setStatus(TransactionStatus.COMPLETED);

		return compensationTransaction;
	}

	@Transactional
	public void createCompensationTransaction(
			@NotNull Account receiverAccount,
			@NotNull @Positive BigDecimal amount,
			@NotNull UUID idempotencyKey) {
		Account treasuryAccount = systemTreasuryAccountService.getOrCreateTreasuryAccount(receiverAccount.getCurrency());
		Transaction savedTransaction = transactionRepository.save(
				buildCompensationTransaction(treasuryAccount, receiverAccount, amount, idempotencyKey));

		ledgerLineService.createLedgerLinesForTransaction(savedTransaction);
	}
}
