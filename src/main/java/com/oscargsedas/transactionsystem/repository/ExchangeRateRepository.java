package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {
}
