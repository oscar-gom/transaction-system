package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
	User findByEmail(String email);

	boolean existsByEmail(String email);
}