package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.LedgerLine;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.LedgerLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerLineService {
	private final LedgerLineRepository ledgerLineRepository;
	private final AccountRepository accountRepository;
	private final ExchangeRateService exchangeRateService;

	void createLedgerLinesForTransaction(Transaction transaction) {
		BigDecimal amountInBase = exchangeRateService.convert(
				transaction.getAmount(),
				transaction.getSenderAccount().getCurrency(),
				ExchangeRateService.BASE_CURRENCY
		);

		var senderLedgerLine = new LedgerLine();
		senderLedgerLine.setTransaction(transaction);
		senderLedgerLine.setAccount(transaction.getSenderAccount());
		senderLedgerLine.setAmount(amountInBase.negate());

		var receiverLedgerLine = new LedgerLine();
		receiverLedgerLine.setTransaction(transaction);
		receiverLedgerLine.setAccount(transaction.getReceiverAccount());
		receiverLedgerLine.setAmount(amountInBase);

		ledgerLineRepository.save(senderLedgerLine);
		ledgerLineRepository.save(receiverLedgerLine);
	}

	BigDecimal getAccountBalance(UUID accountId) {
		if (!accountRepository.existsById(accountId)) {
			throw new ResourceNotFoundException("Account not found with id: " + accountId);
		}
		BigDecimal baseBalance = ledgerLineRepository.getAccountBalance(accountId);
		String accountCurrency = accountRepository.findById(accountId)
				.map(Account::getCurrency)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
		return exchangeRateService.convert(baseBalance, ExchangeRateService.BASE_CURRENCY, accountCurrency);
	}

	BigDecimal getTransactionBalance(UUID transactionId) {
		return ledgerLineRepository.getTransactionBalance(transactionId);
	}

	long countByTransactionId(UUID transactionId) {
		return ledgerLineRepository.countByTransactionId(transactionId);
	}
}
