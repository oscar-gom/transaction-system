package com.oscargsedas.transactionsystem.service;

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

	public void createLedgerLinesForTransaction(Transaction transaction) {

		var senderLedgerLine = new LedgerLine();
		senderLedgerLine.setTransaction(transaction);
		senderLedgerLine.setAccount(transaction.getSenderAccount());
		senderLedgerLine.setAmount(transaction.getAmount().negate());

		var receiverLedgerLine = new LedgerLine();
		receiverLedgerLine.setTransaction(transaction);
		receiverLedgerLine.setAccount(transaction.getReceiverAccount());
		receiverLedgerLine.setAmount(transaction.getAmount());

		ledgerLineRepository.save(senderLedgerLine);
		ledgerLineRepository.save(receiverLedgerLine);
	}

	public BigDecimal getAccountBalance(UUID accountId) {
		if (!accountRepository.existsById(accountId)) {
			throw new ResourceNotFoundException("Account not found with id: " + accountId);
		}

		return ledgerLineRepository.getAccountBalance(accountId);
	}
}
