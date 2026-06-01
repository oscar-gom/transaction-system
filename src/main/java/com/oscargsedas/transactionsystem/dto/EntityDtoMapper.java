package com.oscargsedas.transactionsystem.dto;

import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Contact;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EntityDtoMapper {
	UserDto toUserDto(User user);

	@Mapping(target = "userId", source = "user.id")
	AccountDto toAccountDto(Account account);

	@Mapping(target = "ownerUserId", source = "owner.id")
	@Mapping(target = "contactAccountId", source = "contactAccount.id")
	@Mapping(target = "contactAccountName", source = "contactAccount.accountName")
	ContactDto toContactDto(Contact contact);

	TransactionDto toTransactionDto(Transaction transaction);
}
