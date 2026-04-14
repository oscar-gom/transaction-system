package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}