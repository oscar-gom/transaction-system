package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.entity.LedgerLine;
import com.oscargsedas.transactionsystem.repository.LedgerLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerLineService {
	private final TransactionService transactionService;
	private final LedgerLineRepository ledgerLineRepository;

	public void createLedgerLinesForTransaction(UUID transactionId) {
		var transaction = transactionService.getTransactionById(transactionId);

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
}
