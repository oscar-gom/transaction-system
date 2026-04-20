package com.oscargsedas.transactionsystem.dto;

import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EntityDtoMapper {
	UserDto toUserDto(User user);

	@Mapping(target = "userId", source = "user.id")
	AccountDto toAccountDto(Account account);

	TransactionDto toTransactionDto(Transaction transaction);
}

